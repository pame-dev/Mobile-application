// Service worker de Karsy: solo muestra las notificaciones push del equipo (administrador y
// moderadores) cuando la web está cerrada o en segundo plano. No guarda páginas en caché.
// Se compila aparte a /sw.js (vite.sw.config.ts). El push trae solo datos (como en la app)
// y el texto se arma aquí con los mismos textos de la bandeja.
import { EN } from './i18n/en'
import { ES } from './i18n/es'
import type { T } from './i18n/I18nContext'
import { destinoAviso, textoAviso } from './lib/avisos'

/** Lo que manda enviar-push (todo en texto). */
interface DatosPush {
  id_notificacion: string
  tipo: string
  titulo_vehiculo: string
  id_publicacion: string
  datos: string
}

// Tipos mínimos del service worker (el proyecto compila con la librería DOM, no WebWorker).
interface EventoExtensible extends Event {
  waitUntil(promesa: Promise<unknown>): void
}
interface EventoPush extends EventoExtensible {
  data: { json(): unknown } | null
}
interface EventoClic extends EventoExtensible {
  notification: Notification
}
interface Ventana {
  visibilityState: DocumentVisibilityState
  focus(): Promise<Ventana>
  navigate(url: string): Promise<Ventana | null>
}
const sw = self as unknown as {
  registration: ServiceWorkerRegistration
  location: Location
  clients: {
    matchAll(opciones: { type: 'window'; includeUncontrolled: boolean }): Promise<Ventana[]>
    openWindow(url: string): Promise<unknown>
    claim(): Promise<void>
  }
  skipWaiting(): Promise<void>
  addEventListener(tipo: 'install' | 'activate', f: (e: EventoExtensible) => void): void
  addEventListener(tipo: 'push', f: (e: EventoPush) => void): void
  addEventListener(tipo: 'notificationclick', f: (e: EventoClic) => void): void
}

/** Idioma que eligió la web (lo guarda pushWeb.ts en la caché "karsy-ajustes"). */
async function diccionario() {
  try {
    const guardado = await (await (await caches.open('karsy-ajustes')).match('/idioma'))?.text()
    if (guardado === 'en') return EN
    if (guardado === 'es') return ES
  } catch {
    /* sin caché: se usa el idioma del navegador */
  }
  return navigator.language?.toLowerCase().startsWith('en') ? EN : ES
}

sw.addEventListener('install', (e) => e.waitUntil(sw.skipWaiting()))
sw.addEventListener('activate', (e) => e.waitUntil(sw.clients.claim()))

sw.addEventListener('push', (e) => {
  let d: DatosPush | null = null
  try {
    d = (e.data?.json() ?? null) as DatosPush | null
  } catch {
    return
  }
  if (!d?.tipo) return
  const aviso = d
  e.waitUntil(
    (async () => {
      // Con Karsy a la vista, la página ya muestra el aviso (TiempoReal).
      const ventanas = await sw.clients.matchAll({ type: 'window', includeUncontrolled: true })
      if (ventanas.some((v) => v.visibilityState === 'visible')) return

      const dict = await diccionario()
      const t: T = (clave, vars) => {
        const texto = dict[clave] ?? ES[clave]
        return vars ? texto.replace(/\{(\w+)\}/g, (m, k: string) => (k in vars ? String(vars[k]) : m)) : texto
      }
      let datos: Record<string, string | number> | null = null
      try {
        datos = aviso.datos ? JSON.parse(aviso.datos) : null
      } catch {
        /* sin números: texto simple */
      }
      const idPublicacion = aviso.id_publicacion ? Number(aviso.id_publicacion) : null
      await sw.registration.showNotification('Karsy', {
        body: textoAviso(t, aviso.tipo, aviso.titulo_vehiculo.trim() || t('notif.yourVehicle'), datos),
        icon: '/logo.png',
        // Mismo tag que el aviso de TiempoReal: si salen los dos, uno reemplaza al otro.
        tag: `karsy-${aviso.id_notificacion}`,
        data: { url: destinoAviso(aviso.tipo, idPublicacion) ?? '/notificaciones' },
      })
    })(),
  )
})

// Al tocarla: lleva a la sección del panel (o a la bandeja) en una pestaña de Karsy.
sw.addEventListener('notificationclick', (e) => {
  e.notification.close()
  const ruta = (e.notification.data as { url?: string } | null)?.url ?? '/notificaciones'
  const url = new URL(ruta, sw.location.origin).href
  e.waitUntil(
    (async () => {
      const [ventana] = await sw.clients.matchAll({ type: 'window', includeUncontrolled: true })
      if (ventana) {
        await ventana.focus()
        // navigate solo funciona en pestañas que controla este service worker.
        if (await ventana.navigate(url).catch(() => null)) return
      }
      await sw.clients.openWindow(url)
    })(),
  )
})
