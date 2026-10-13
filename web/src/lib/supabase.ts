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
