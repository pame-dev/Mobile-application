// Edge Function: envía cada aviso nuevo de `notificaciones` a los teléfonos (Firebase Cloud
// Messaging) y a los navegadores del equipo (Web Push con VAPID) de la cuenta.
//
// La llama un Database Webhook (INSERT en public.notificaciones). Del cuerpo solo se usa
// el id del aviso: el aviso se vuelve a leer de la BD y se marca push_enviado = TRUE en el
// mismo paso, así que llamarla a mano no permite mandar mensajes inventados ni repetidos.
//
// El mensaje va solo con datos (tipo, vehículo, motivo); la app arma el texto en el idioma
// del usuario con los mismos textos de la bandeja.
//
// Secretos (no van al repo):
//   FIREBASE_SERVICE_ACCOUNT = contenido del JSON de la cuenta de servicio de Firebase
//   VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY = par de llaves VAPID (base64url); la pública
//                                        también va en la web como VITE_VAPID_PUBLIC_KEY
//   VAPID_SUBJECT = contacto del remitente, p. ej. mailto:soporte@tudominio.com
// Sin los de Firebase no se avisa a teléfonos; sin los VAPID, no a navegadores.
// SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY los pone Supabase automáticamente.

import { createClient } from "jsr:@supabase/supabase-js@2";

const supabase = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
);

interface CuentaServicio {
  project_id: string;
  client_email: string;
  private_key: string;
}

const cuenta: CuentaServicio = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT") ?? "{}");

// ── Token de acceso de Google (OAuth con la cuenta de servicio) ─────────────────────────

let tokenCache: { valor: string; expira: number } | null = null;

function base64url(datos: ArrayBuffer | Uint8Array | string): string {
  const bytes = typeof datos === "string" ? new TextEncoder().encode(datos) : new Uint8Array(datos);
  let binario = "";
  for (const b of bytes) binario += String.fromCharCode(b);
  return btoa(binario).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function tokenDeAcceso(): Promise<string> {
  const ahora = Math.floor(Date.now() / 1000);
  if (tokenCache && tokenCache.expira - 60 > ahora) return tokenCache.valor;

  const encabezado = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const reclamos = base64url(JSON.stringify({
    iss: cuenta.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: ahora,
    exp: ahora + 3600,
  }));
  const pem = cuenta.private_key
    .replace(/-----(BEGIN|END) PRIVATE KEY-----/g, "")
    .replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const llave = await crypto.subtle.importKey(
    "pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
  const firma = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5", llave, new TextEncoder().encode(`${encabezado}.${reclamos}`),
  );
  const jwt = `${encabezado}.${reclamos}.${base64url(firma)}`;

  const respuesta = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: jwt }),
  });
  if (!respuesta.ok) throw new Error(`OAuth de Google falló: ${respuesta.status} ${await respuesta.text()}`);
  const { access_token, expires_in } = await respuesta.json();
  tokenCache = { valor: access_token, expira: ahora + expires_in };
  return access_token;
}

// ── Web Push (navegadores del equipo): RFC 8291 (cifrado) y RFC 8292 (VAPID) ─────────────

const vapidPublica = Deno.env.get("VAPID_PUBLIC_KEY") ?? "";
const vapidPrivada = Deno.env.get("VAPID_PRIVATE_KEY") ?? "";
const vapidContacto = Deno.env.get("VAPID_SUBJECT") ?? "";
const hayVapid = !!(vapidPublica && vapidPrivada && vapidContacto);

// Solo se envía a los servicios de push de los navegadores: Chrome, Firefox, Edge y Safari.
const SERVICIOS_PUSH = [
  /^(fcm|android)\.googleapis\.com$/,
  /^updates\.push\.services\.mozilla\.com$/,
  /^[a-z0-9-]+\.notify\.windows\.com$/,
  /^([a-z0-9-]+\.)*push\.apple\.com$/,
];

function deBase64url(texto: string): Uint8Array {
  const b64 = texto.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((texto.length + 3) % 4);
  return Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
}

function unir(...partes: Uint8Array[]): Uint8Array {
  const total = new Uint8Array(partes.reduce((n, p) => n + p.length, 0));
  let i = 0;
  for (const p of partes) {
    total.set(p, i);
    i += p.length;
  }
  return total;
}

async function hkdf(sal: Uint8Array, ikm: Uint8Array, info: Uint8Array, bytes: number): Promise<Uint8Array> {
  const llave = await crypto.subtle.importKey("raw", ikm, "HKDF", false, ["deriveBits"]);
  return new Uint8Array(await crypto.subtle.deriveBits({ name: "HKDF", hash: "SHA-256", salt: sal, info }, llave, bytes * 8));
}

let llaveVapid: CryptoKey | null = null;

/** JWT de VAPID para el servicio de push (aud = su origen), firmado con la llave privada. */
async function jwtVapid(audiencia: string): Promise<string> {
  if (!llaveVapid) {
    const publica = deBase64url(vapidPublica);
    llaveVapid = await crypto.subtle.importKey(
      "jwk",
      { kty: "EC", crv: "P-256", d: vapidPrivada, x: base64url(publica.slice(1, 33)), y: base64url(publica.slice(33, 65)) },
      { name: "ECDSA", namedCurve: "P-256" },
      false,
      ["sign"],
    );
  }
  const ahora = Math.floor(Date.now() / 1000);
  const encabezado = base64url(JSON.stringify({ typ: "JWT", alg: "ES256" }));
  const reclamos = base64url(JSON.stringify({ aud: audiencia, exp: ahora + 12 * 3600, sub: vapidContacto }));
  const firma = await crypto.subtle.sign(
    { name: "ECDSA", hash: "SHA-256" }, llaveVapid, new TextEncoder().encode(`${encabezado}.${reclamos}`),
  );
  return `${encabezado}.${reclamos}.${base64url(firma)}`;
}

/** Cifra el mensaje para la suscripción (aes128gcm, un solo registro). */
async function cifrar(p256dh: string, auth: string, mensaje: string): Promise<Uint8Array> {
  const texto = new TextEncoder();
  const clienteRaw = deBase64url(p256dh);
  const efimera = await crypto.subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, true, ["deriveBits"]);
  const servidorRaw = new Uint8Array(await crypto.subtle.exportKey("raw", efimera.publicKey));
  const cliente = await crypto.subtle.importKey("raw", clienteRaw, { name: "ECDH", namedCurve: "P-256" }, false, []);
  const compartido = new Uint8Array(await crypto.subtle.deriveBits({ name: "ECDH", public: cliente }, efimera.privateKey, 256));

  const ikm = await hkdf(deBase64url(auth), compartido, unir(texto.encode("WebPush: info\0"), clienteRaw, servidorRaw), 32);
  const sal = crypto.getRandomValues(new Uint8Array(16));
  const cek = await hkdf(sal, ikm, texto.encode("Content-Encoding: aes128gcm\0"), 16);
  const nonce = await hkdf(sal, ikm, texto.encode("Content-Encoding: nonce\0"), 12);
  const llave = await crypto.subtle.importKey("raw", cek, "AES-GCM", false, ["encrypt"]);
  // 0x02 = último (y único) registro.
  const cifrado = new Uint8Array(
    await crypto.subtle.encrypt({ name: "AES-GCM", iv: nonce }, llave, unir(texto.encode(mensaje), new Uint8Array([2]))),
  );

  // Encabezado: sal (16) · tamaño de registro (4) · largo de la llave (1) · llave del servidor.
  const encabezado = new Uint8Array(21);
  encabezado.set(sal);
  new DataView(encabezado.buffer).setUint32(16, 4096);
  encabezado[20] = servidorRaw.length;
  return unir(encabezado, servidorRaw, cifrado);
}

/** "ok", "borrar" (la suscripción ya no sirve) o "error". */
async function enviarNavegador(endpoint: string, p256dh: string, auth: string, datos: Record<string, string>) {
  const url = new URL(endpoint);
  if (url.protocol !== "https:" || url.port !== "" || !SERVICIOS_PUSH.some((r) => r.test(url.hostname))) return "borrar";
  const respuesta = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Authorization": `vapid t=${await jwtVapid(url.origin)}, k=${vapidPublica}`,
      "Content-Encoding": "aes128gcm",
      "Content-Type": "application/octet-stream",
      "TTL": "86400",
      "Urgency": "high",
    },
    body: await cifrar(p256dh, auth, JSON.stringify(datos)),
    // Sin seguir redirecciones: la petición no sale de los servicios de push.
    redirect: "manual",
  });
  if (respuesta.ok) return "ok";
  if (respuesta.status === 404 || respuesta.status === 410) return "borrar";
  console.error("Push web respondió", respuesta.status, await respuesta.text());
  return "error";
}

// ── Envío ────────────────────────────────────────────────────────────────────────────────

function json(cuerpo: unknown, status = 200): Response {
  return new Response(JSON.stringify(cuerpo), { status, headers: { "Content-Type": "application/json" } });
}

Deno.serve(async (req) => {
  if (!cuenta.project_id && !hayVapid) return json({ error: "Faltan los secretos de Firebase y de VAPID" }, 500);

  const cuerpo = await req.json().catch(() => null);
  const id = cuerpo?.record?.id_notificacion;
  if (typeof id !== "number") return json({ error: "Falta record.id_notificacion" }, 400);

  // Se marca como enviado solo si aún no lo estaba: un aviso nunca se envía dos veces.
  const { data: aviso, error } = await supabase
    .from("notificaciones")
    .update({ push_enviado: true })
    .eq("id_notificacion", id)
    .eq("push_enviado", false)
    .select("id_notificacion, id_cuenta, tipo, titulo_vehiculo, motivo, id_publicacion, datos")
    .maybeSingle();
  if (error) return json({ error: error.message }, 500);
  if (!aviso) return json({ omitido: "No existe o ya se envió" });

  const { data: dispositivos } = await supabase
    .from("dispositivos_push")
    .select("token, p256dh, auth")
    .eq("id_cuenta", aviso.id_cuenta);
  if (!dispositivos?.length) return json({ enviados: 0 });

  // Solo datos: la app y el service worker de la web arman el texto (español / inglés).
  const data = {
    id_notificacion: String(aviso.id_notificacion),
    tipo: aviso.tipo,
    titulo_vehiculo: aviso.titulo_vehiculo ?? "",
    motivo: aviso.motivo ?? "",
    id_publicacion: aviso.id_publicacion != null ? String(aviso.id_publicacion) : "",
    // Números del aviso (precio, vistas…) para armar el texto; JSON en texto.
    datos: aviso.datos ? JSON.stringify(aviso.datos) : "",
  };
  let enviados = 0;

  // Navegadores del equipo (Web Push).
  for (const { token, p256dh, auth } of dispositivos) {
    if (!p256dh || !auth || !hayVapid) continue;
    const resultado = await enviarNavegador(token, p256dh, auth, data).catch((e) => {
      console.error("Push web falló", e);
      return "error";
    });
    if (resultado === "ok") enviados++;
    else if (resultado === "borrar") await supabase.from("dispositivos_push").delete().eq("token", token);
  }

  // Teléfonos (Firebase).
  const telefonos = dispositivos.filter((d) => !d.p256dh);
  if (!telefonos.length || !cuenta.project_id) return json({ enviados });
  const acceso = await tokenDeAcceso();
  const url = `https://fcm.googleapis.com/v1/projects/${cuenta.project_id}/messages:send`;

  for (const { token } of telefonos) {
    const respuesta = await fetch(url, {
      method: "POST",
      headers: { "Authorization": `Bearer ${acceso}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          token,
          data,
          android: { priority: "HIGH" },
        },
      }),
    });

    if (respuesta.ok) {
      enviados++;
    } else if (respuesta.status === 404 || respuesta.status === 400) {
      // La app se desinstaló o el token ya no es válido: se borra.
      const detalle = await respuesta.text();
      if (detalle.includes("UNREGISTERED") || detalle.includes("INVALID_ARGUMENT")) {
        await supabase.from("dispositivos_push").delete().eq("token", token);
      }
    } else {
      console.error("FCM respondió", respuesta.status, await respuesta.text());
    }
  }

  return json({ enviados });
});
