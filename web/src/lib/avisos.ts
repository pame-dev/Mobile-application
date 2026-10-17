import type { T } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { precio } from './format'

/** Tipos de aviso con texto propio (notif.<tipo> en i18n); los demás usan notif.generic. */
const TIPOS = [
  'favorito', 'propuesta_enviada', 'publicacion_aprobada', 'destacado_aprobado', 'edicion_aprobada', 'publicacion_rehabilitada',
  'publicacion_rechazada', 'edicion_rechazada', 'publicacion_deshabilitada', 'destacado_rechazado', 'destacado_por_vencer',
  'cuenta_suspendida', 'cuenta_reactivada', 'reporte_atendido', 'reporte_descartado', 'busqueda_coincidencia',
  'favorito_no_disponible', 'auto_parecido', 'poca_actividad', 'confirmar_venta',
] as const

/**
 * Texto del aviso (igual que notificationText en la app). Los que llevan números
 * (precio, vistas, autos nuevos) los leen de [datos] (notificaciones.datos); si no
 * vienen, se usa una versión sin números.
 */
export function textoAviso(t: T, tipo: string, vehiculo: string, datos?: Record<string, string | number> | null): string {
  const d = datos ?? {}
  const num = (k: string) => (d[k] != null && !Number.isNaN(Number(d[k])) ? Math.round(Number(d[k])) : null)
  switch (tipo) {
    case 'precio_bajo': {
      const ahora = num('precio')
      const antes = num('precio_anterior')
      return ahora != null && antes != null
        ? t('notif.precio_bajo', { v: vehiculo, precio: precio(ahora), antes: precio(antes) })
        : t('notif.precio_bajo_simple', { v: vehiculo })
    }
    case 'resumen_semanal': {
      const nuevos = num('nuevos')
      const coinciden = num('coincidencias') ?? 0
      const estado = d.estado != null ? String(d.estado) : null
      if (nuevos == null || !estado) return t('notif.resumen_semanal_simple')
      return coinciden > 0
        ? t('notif.resumen_semanal_coincidencias', { estado, n: nuevos, m: coinciden })
        : t('notif.resumen_semanal', { estado, n: nuevos })
    }
    case 'resumen_vendedor': {
      const vistas = num('vistas')
      const favoritos = num('favoritos')
      return vistas != null && favoritos != null
        ? t('notif.resumen_vendedor', { vistas, favoritos })
        : t('notif.resumen_vendedor_simple')
    }
    case 'favorito_no_disponible':
      if (d.estado === 'vendida') return t('notif.favorito_vendido', { v: vehiculo })
      break
    case 'hito_vistas': {
      const vistas = num('vistas')
      return vistas != null ? t('notif.hito_vistas', { v: vehiculo, n: vistas }) : t('notif.hito_vistas_simple', { v: vehiculo })
    }
  }
  const clave = (TIPOS as readonly string[]).includes(tipo) ? (`notif.${tipo}` as Clave) : 'notif.generic'
  return t(clave, { v: vehiculo })
}

/** A dónde lleva el aviso al tocarlo (null = no lleva a ningún lado). */
export function destinoAviso(tipo: string, idPublicacion: number | null): string | null {
  if (tipo === 'resumen_semanal') return '/'
  if (tipo === 'resumen_vendedor') return '/panel'
  return idPublicacion ? `/vehiculo/${idPublicacion}` : null
}
