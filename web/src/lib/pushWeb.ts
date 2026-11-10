import type { Lang } from '../i18n/I18nContext'
import { supabase } from './supabase'

// Push en el navegador para el equipo (administrador y moderadores): el service worker
// (src/sw.ts → /sw.js) recibe el aviso aunque Karsy esté cerrada. La suscripción se
// guarda en dispositivos_push (registrar_navegador) y la usa la Edge Function enviar-push.

/** Llave pública VAPID (la misma que el secreto VAPID_PUBLIC_KEY de Supabase). */
const VAPID = (import.meta.env.VITE_VAPID_PUBLIC_KEY as string | undefined)?.trim() ?? ''
/** En desarrollo Vite sirve el archivo TypeScript como módulo; al compilar sale /sw.js. */
const SCRIPT = import.meta.env.DEV ? '/src/sw.ts' : '/sw.js'
/** Caché que comparte con el service worker (mismo nombre en sw.ts): ahí lee el idioma. */
const CACHE_AJUSTES = 'karsy-ajustes'

export const soportaPushWeb = () => !!VAPID && 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window

const aBytes = (b64: string) => Uint8Array.from(atob(b64.replace(/-/g, '+').replace(/_/g, '/') + '==='.slice((b64.length + 3) % 4)), (c) => c.charCodeAt(0))
const aBase64url = (buf: ArrayBuffer) => btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')

/** subscribe() pide un service worker activo. */
async function esperarActivo(reg: ServiceWorkerRegistration): Promise<void> {
  const sw = reg.installing ?? reg.waiting
  if (reg.active || !sw) return
  await new Promise<void>((listo) => {
    sw.addEventListener('statechange', () => sw.state === 'activated' && listo())
  })
}

/** El service worker arma el texto en el idioma elegido en la web. */
async function guardarIdioma(lang: Lang): Promise<void> {
  if (!('caches' in window)) return
  const cache = await caches.open(CACHE_AJUSTES)
  await cache.put('/idioma', new Response(lang))
}

/** Suscribe este navegador a la cuenta en sesión (debe ser del equipo y con permiso dado). */
export async function activarPushWeb(lang: Lang): Promise<void> {
  if (!soportaPushWeb() || Notification.permission !== 'granted') return
  await guardarIdioma(lang)
  const reg = await navigator.serviceWorker.register(SCRIPT, { type: import.meta.env.DEV ? 'module' : 'classic' })
  await esperarActivo(reg)
  let sub = await reg.pushManager.getSubscription()
  // Si cambiaron las llaves VAPID, la suscripción vieja ya no sirve.
  const llave = sub?.options.applicationServerKey
  if (sub && llave && aBase64url(llave) !== VAPID) {
    await sub.unsubscribe().catch(() => false)
    sub = null
  }
  sub ??= await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: aBytes(VAPID) })
  const { keys } = sub.toJSON()
  const { error } = await supabase.rpc('registrar_navegador', { p_endpoint: sub.endpoint, p_p256dh: keys?.p256dh, p_auth: keys?.auth })
  if (error) throw error
}

/** Quita este navegador de la cuenta (al cerrar sesión o si la cuenta ya no es del equipo). */
export async function quitarPushWeb(): Promise<void> {
  if (!('serviceWorker' in navigator)) return
  for (const reg of await navigator.serviceWorker.getRegistrations()) {
    const sub = await reg.pushManager?.getSubscription()
    if (!sub) continue
    // Si falla (sin sesión o sin internet), al anular la suscripción enviar-push la borra sola.
    await supabase.rpc('quitar_dispositivo', { p_token: sub.endpoint })
    await sub.unsubscribe().catch(() => false)
  }
}
