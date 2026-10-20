// Edge Function: "Cerrar sesión en todos los dispositivos" (web y app).
//
// La llama el usuario con su sesión (Authorization: Bearer <jwt>). En un solo paso:
//   1. Revoca todas las sesiones de su cuenta en Supabase Auth (scope global).
//   2. Quita sus teléfonos de dispositivos_push (ya no deben recibir avisos de la cuenta).
//   3. Manda a cada teléfono un push silencioso { tipo: "sesion_cerrada" }: la app lo
//      recibe al instante (aunque esté cerrada), comprueba con Supabase que su sesión ya
//      no existe y la cierra. Sin este aviso el teléfono tardaba hasta un minuto.
//
// Solo actúa sobre la cuenta del token: no se puede cerrar la sesión de otra persona.
//
// Secreto necesario (no va al repo), el mismo de enviar-push:
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

// La web la llama desde el navegador.
const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(cuerpo: unknown, status = 200): Response {
  return new Response(JSON.stringify(cuerpo), { status, headers: { ...CORS, "Content-Type": "application/json" } });
}

// ── Token de acceso de Google (OAuth con la cuenta de servicio, igual que enviar-push) ──

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

// ── Aviso a los teléfonos ───────────────────────────────────────────────────────────────

async function avisarTelefonos(tokens: string[]): Promise<number> {
  if (!tokens.length) return 0;
  if (!cuenta.project_id) {
    console.warn("Falta el secreto FIREBASE_SERVICE_ACCOUNT: no se avisó a los teléfonos");
    return 0;
  }
  const acceso = await tokenDeAcceso();
  const url = `https://fcm.googleapis.com/v1/projects/${cuenta.project_id}/messages:send`;
  // En paralelo: cada teléfono recibe el aviso sin esperar a los demás.
  const resultados = await Promise.all(tokens.map((token) =>
    fetch(url, {
      method: "POST",
      headers: { "Authorization": `Bearer ${acceso}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          token,
          // Solo datos: la app no muestra notificación, cierra la sesión.
          data: { tipo: "sesion_cerrada" },
          // Prioridad alta: se entrega de inmediato aunque el teléfono esté en reposo.
          // Si el teléfono está apagado más de 10 min el aviso ya no sirve: al abrir la app
          // ella misma comprueba la sesión.
          android: { priority: "HIGH", ttl: "600s" },
        },
      }),
    }).then(async (r) => {
      if (!r.ok) console.error("FCM respondió", r.status, await r.text());
      return r.ok;
    }).catch((e) => {
      console.error("FCM no respondió", e);
      return false;
    })
  ));
  return resultados.filter(Boolean).length;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS });
  if (req.method !== "POST") return json({ error: "Método no permitido" }, 405);

  const jwt = req.headers.get("Authorization")?.replace(/^Bearer\s+/i, "") ?? "";
  const { data: usuario, error: errorUsuario } = await supabase.auth.getUser(jwt);
  if (errorUsuario || !usuario.user) return json({ error: "Sesión no válida" }, 401);
  const idCuenta = usuario.user.id;

  // 1. Primero se revoca: cuando el teléfono reciba el aviso y lo compruebe, ya no existe.
  const { error: errorCierre } = await supabase.auth.admin.signOut(jwt, "global");
  if (errorCierre) {
    console.error("No se pudo cerrar la sesión", errorCierre.message);
    return json({ error: "No se pudo cerrar la sesión" }, 500);
  }

  // 2. Los teléfonos de la cuenta dejan de recibir avisos (ellos ya no se quitan: al
  //    recibir el aviso solo borran su sesión local).
  const { data: dispositivos, error: errorDispositivos } = await supabase
    .from("dispositivos_push")
    .delete()
    .eq("id_cuenta", idCuenta)
    .select("token");
  if (errorDispositivos) console.error("No se pudieron quitar los teléfonos", errorDispositivos.message);

  // 3. Aviso inmediato. Si falla, los teléfonos lo notan al volver a la app (o en un minuto).
  const avisados = await avisarTelefonos((dispositivos ?? []).map((d) => d.token as string))
    .catch((e) => {
      console.error("No se pudo avisar a los teléfonos", e);
      return 0;
    });

  return json({ cerrada: true, avisados });
});
