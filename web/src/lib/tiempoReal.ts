import { useEffect, useRef } from 'react'
import type { RealtimeChannel } from '@supabase/supabase-js'
import { supabase } from './supabase'

/**
 * Cambios en vivo (Supabase Realtime): lo que se hace en la app (o en otra pestaña) se
 * ve en la web sin recargar. Un solo canal escucha las tablas que cambian y avisa con el
 * evento "karsy:cambios" (agrupado cada 400 ms); cada pantalla vuelve a leer sus datos
 * con useRecargar. Realtime respeta la seguridad por filas: cada quien solo recibe los
 * cambios de filas que puede leer. Las tablas se agregan a Realtime en la migración
 * 20261016140000_tiempo_real.sql.
 */

export type Tabla =
  | 'publicaciones'
  | 'propuestas_publicacion'
  | 'fotos_publicacion'
  | 'notificaciones'
  | 'favoritos'
  | 'reportes'
  | 'solicitudes_destacado'
  | 'cuentas'

const TABLAS: Tabla[] = [
  'publicaciones', 'propuestas_publicacion', 'fotos_publicacion', 'notificaciones',
  'favoritos', 'reportes', 'solicitudes_destacado', 'cuentas',
]

export const EVENTO_CAMBIOS = 'karsy:cambios'

export interface AvisoNuevo {
  id_notificacion: number
  tipo: string
  id_publicacion: number | null
  titulo_vehiculo: string | null
  motivo: string | null
}

/**
 * Abre el canal de cambios. Con [uid] además llama a [onAviso] con cada notificación
 * nueva de esa cuenta. Devuelve la función para cerrarlo.
 */
export function escucharCambios(uid: string | null, onAviso: (a: AvisoNuevo) => void): () => void {
  let pendientes = new Set<Tabla>()
  let timer: ReturnType<typeof setTimeout> | undefined
  const avisar = (tabla: Tabla) => {
    pendientes.add(tabla)
    clearTimeout(timer)
    timer = setTimeout(() => {
      window.dispatchEvent(new CustomEvent<Tabla[]>(EVENTO_CAMBIOS, { detail: [...pendientes] }))
      pendientes = new Set()
    }, 400)
  }

  let canal: RealtimeChannel | null = null
  let cerrado = false

  void (async () => {
    // Realtime revisa los permisos con el token del canal: con sesión hay que pasárselo
    // antes de suscribirse; si no, entra como visitante y la BD rechaza las notificaciones.
    if (uid) {
      const { data } = await supabase.auth.getSession()
      if (data.session) await supabase.realtime.setAuth(data.session.access_token)
    }
    if (cerrado) return
    let c = supabase.channel(`karsy-cambios-${uid ?? 'visitante'}`)
    for (const tabla of TABLAS) {
      // Las notificaciones solo interesan las propias.
      if (tabla === 'notificaciones' && !uid) continue
      c = c.on(
        'postgres_changes',
        { event: '*', schema: 'public', table: tabla, ...(tabla === 'notificaciones' ? { filter: `id_cuenta=eq.${uid}` } : {}) },
        (payload) => {
          avisar(tabla)
          if (tabla === 'notificaciones' && payload.eventType === 'INSERT') onAviso(payload.new as AvisoNuevo)
        },
      )
    }
    canal = c.subscribe()
  })()

  return () => {
    cerrado = true
    clearTimeout(timer)
    if (canal) void supabase.removeChannel(canal)
  }
}

/**
 * Vuelve a llamar a [cargar] (sin mostrar "cargando") cuando cambia alguna de [tablas] en
 * la BD, y al regresar a la pestaña. Máximo una vez por segundo.
 */
export function useRecargar(cargar: () => unknown, tablas: Tabla[]) {
  const ref = useRef(cargar)
  ref.current = cargar
  const clave = tablas.join(',')

  useEffect(() => {
    const interesa = clave.split(',')
    let ultima = 0
    const recargar = () => {
      const ahora = Date.now()
      if (ahora - ultima < 1000) return
      ultima = ahora
      void ref.current()
    }
    const onCambios = (e: Event) => {
      if ((e as CustomEvent<Tabla[]>).detail.some((t) => interesa.includes(t))) recargar()
    }
    const onVisible = () => {
      if (document.visibilityState === 'visible') recargar()
    }
    window.addEventListener(EVENTO_CAMBIOS, onCambios)
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      window.removeEventListener(EVENTO_CAMBIOS, onCambios)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [clave])
}
