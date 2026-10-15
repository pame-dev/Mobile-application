import { createClient } from '@supabase/supabase-js'

// Mismo proyecto de Supabase que la app de Android (local.properties → SUPABASE_URL / SUPABASE_ANON_KEY).
const url = import.meta.env.VITE_SUPABASE_URL as string | undefined
const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined

if (!url || !anonKey) {
  throw new Error('Faltan VITE_SUPABASE_URL y VITE_SUPABASE_ANON_KEY. Copia web/.env.example como web/.env y llénalo.')
}

export const supabase = createClient(url, anonKey, {
  auth: { persistSession: true, autoRefreshToken: true },
})

/** Bucket público donde la app guarda fotos de publicaciones y perfiles. */
export const BUCKET = 'karsy'

/** URL pública de una ruta del bucket (igual que Supabase.publicUrl en la app). */
export function publicUrl(ruta: string | null | undefined): string | null {
  if (!ruta || !ruta.trim()) return null
  if (ruta.startsWith('http')) return ruta
  return supabase.storage.from(BUCKET).getPublicUrl(ruta).data.publicUrl
}

/**
 * Cierra la sesión solo en este navegador. Sin opciones, supabase-js la cierra en TODOS
 * los dispositivos de la cuenta (también en la app). Si Supabase no responde (sin
 * internet), la sesión guardada se borra igual para que no vuelva a aparecer al recargar.
 * "Cerrar sesión en todos los dispositivos" (Privacidad) usa scope 'global' a propósito.
 */
export async function cerrarSesionLocal(): Promise<void> {
  const { error } = await supabase.auth.signOut({ scope: 'local' }).catch((e: unknown) => ({ error: e }))
  if (!error) return
  const clave = (supabase.auth as unknown as { storageKey?: string }).storageKey
  if (!clave) return
  try {
    for (const sufijo of ['', '-user', '-code-verifier']) localStorage.removeItem(clave + sufijo)
  } catch {
    // almacenamiento bloqueado: no hay nada guardado que borrar
  }
}
