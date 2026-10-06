// Edge Function: envía por Firebase Cloud Messaging cada aviso nuevo de `notificaciones`.
//
// La llama un Database Webhook (INSERT en public.notificaciones). Del cuerpo solo se usa
// el id del aviso: el aviso se vuelve a leer de la BD y se marca push_enviado = TRUE en el
// mismo paso, así que llamarla a mano no permite mandar mensajes inventados ni repetidos.
//
// El mensaje va solo con datos (tipo, vehículo, motivo); la app arma el texto en el idioma
// del usuario con los mismos textos de la bandeja.
//
// Secreto necesario (no va al repo):
//   FIREBASE_SERVICE_ACCOUNT = contenido del JSON de la cuenta de servicio de Firebase
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

function base64url(datos: ArrayBuffer | string): string {
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

// ── Envío ────────────────────────────────────────────────────────────────────────────────

function json(cuerpo: unknown, status = 200): Response {
  return new Response(JSON.stringify(cuerpo), { status, headers: { "Content-Type": "application/json" } });
}

Deno.serve(async (req) => {
  if (!cuenta.project_id) return json({ error: "Falta el secreto FIREBASE_SERVICE_ACCOUNT" }, 500);

  const cuerpo = await req.json().catch(() => null);
  const id = cuerpo?.record?.id_notificacion;
  if (typeof id !== "number") return json({ error: "Falta record.id_notificacion" }, 400);

  // Se marca como enviado solo si aún no lo estaba: un aviso nunca se envía dos veces.
  const { data: aviso, error } = await supabase
    .from("notificaciones")
    .update({ push_enviado: true })
    .eq("id_notificacion", id)
    .eq("push_enviado", false)
    .select("id_notificacion, id_cuenta, tipo, titulo_vehiculo, motivo, id_publicacion")
    .maybeSingle();
  if (error) return json({ error: error.message }, 500);
  if (!aviso) return json({ omitido: "No existe o ya se envió" });

  const { data: dispositivos } = await supabase
    .from("dispositivos_push")
    .select("token")
    .eq("id_cuenta", aviso.id_cuenta);
  if (!dispositivos?.length) return json({ enviados: 0 });

  const acceso = await tokenDeAcceso();
  const url = `https://fcm.googleapis.com/v1/projects/${cuenta.project_id}/messages:send`;
  let enviados = 0;

  for (const { token } of dispositivos) {
    const respuesta = await fetch(url, {
      method: "POST",
      headers: { "Authorization": `Bearer ${acceso}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          token,
          // Solo datos: la app arma el texto (español / inglés).
          data: {
            id_notificacion: String(aviso.id_notificacion),
            tipo: aviso.tipo,
            titulo_vehiculo: aviso.titulo_vehiculo ?? "",
            motivo: aviso.motivo ?? "",
            id_publicacion: aviso.id_publicacion != null ? String(aviso.id_publicacion) : "",
          },
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
