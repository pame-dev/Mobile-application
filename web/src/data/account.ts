// Notificaciones y seguridad de la cuenta (NotificationRepository y AuthRepository de la app).
import { supabase } from '../lib/supabase'
import { traducir } from '../i18n/I18nContext'
import { UserError } from './api'

export interface Notificacion {
  id_notificacion: number
  tipo: string
  id_publicacion: number | null
  titulo_vehiculo: string | null
  /** Comentario del admin (rechazo / deshabilitación); null si no aplica. */
  motivo: string | null
  leida: boolean
  fecha_creacion: string
  /** Números del aviso (precio, vistas, autos nuevos…); null si no los usa. */
  datos: Record<string, string | number> | null
}

/** Qué avisos opcionales recibe la cuenta (preferencias_notificacion; sin fila = todos). */
export interface PreferenciasAvisos {
  precios: boolean
  resumenes: boolean
  recomendaciones: boolean
  vendedor: boolean
}

export const PREFERENCIAS_POR_DEFECTO: PreferenciasAvisos = { precios: true, resumenes: true, recomendaciones: true, vendedor: true }

export async function notificationPreferences(userId: string): Promise<PreferenciasAvisos> {
  const { data, error } = await supabase
    .from('preferencias_notificacion')
    .select('precios,resumenes,recomendaciones,vendedor')
    .eq('id_cuenta', userId)
    .maybeSingle()
  if (error) throw new Error(error.message)
  return (data as PreferenciasAvisos | null) ?? PREFERENCIAS_POR_DEFECTO
}

export async function saveNotificationPreferences(userId: string, p: PreferenciasAvisos): Promise<void> {
  const { error } = await supabase
    .from('preferencias_notificacion')
    .upsert({ id_cuenta: userId, ...p, fecha_actualizacion: new Date().toISOString() })
  if (error) throw new Error(error.message)
}

/** Avisos de la cuenta, del más nuevo al más viejo. Los crean triggers en la BD. */
export async function notifications(userId: string): Promise<Notificacion[]> {
  const { data, error } = await supabase
    .from('notificaciones')
    .select('id_notificacion,tipo,id_publicacion,titulo_vehiculo,motivo,leida,fecha_creacion,datos')
    .eq('id_cuenta', userId)
    .order('fecha_creacion', { ascending: false })
    .limit(100)
  if (error) throw new Error(error.message)
  return (data ?? []) as Notificacion[]
}

export async function unreadCount(userId: string): Promise<number> {
  const { count } = await supabase.from('notificaciones').select('id_notificacion', { count: 'exact', head: true }).eq('id_cuenta', userId).eq('leida', false)
  return count ?? 0
}

/** Marca como leídos todos los avisos pendientes. */
export async function markAllRead(userId: string): Promise<void> {
  await supabase.from('notificaciones').update({ leida: true }).eq('id_cuenta', userId).eq('leida', false)
}

/**
 * Cambia la contraseña. Primero comprueba la actual iniciando sesión con ella
 * (Supabase no la pide para cambiarla); si no coincide no se cambia nada.
 */
export async function changePassword(email: string, current: string, next: string): Promise<void> {
  const r = await supabase.auth.signInWithPassword({ email, password: current })
  if (r.error) {
    if (r.error.code === 'invalid_credentials' || r.error.code === 'invalid_grant') throw new UserError(traducir('perfil.wrongPassword'))
    throw r.error
  }
  const { error } = await supabase.auth.updateUser({ password: next })
  if (error) throw error
}

/** Cierra la sesión en todos los dispositivos de la cuenta, incluido este. */
export async function signOutEverywhere(): Promise<void> {
  const { error } = await supabase.auth.signOut({ scope: 'global' })
  if (error) throw error
}
