import type { T } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'

/** Tipos de aviso con texto propio (notif.<tipo> en i18n); los demás usan notif.generic. */
const TIPOS = [
  'favorito', 'propuesta_enviada', 'publicacion_aprobada', 'destacado_aprobado', 'edicion_aprobada', 'publicacion_rehabilitada',
  'publicacion_rechazada', 'edicion_rechazada', 'publicacion_deshabilitada', 'destacado_rechazado', 'destacado_por_vencer',
  'cuenta_suspendida', 'cuenta_reactivada', 'reporte_atendido', 'reporte_descartado', 'busqueda_coincidencia',
] as const

export function textoAviso(t: T, tipo: string, vehiculo: string): string {
  const clave = (TIPOS as readonly string[]).includes(tipo) ? (`notif.${tipo}` as Clave) : 'notif.generic'
  return t(clave, { v: vehiculo })
}
