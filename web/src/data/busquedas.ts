import { traducir } from '../i18n/I18nContext'
import { precio } from '../lib/format'
import { supabase } from '../lib/supabase'
import { UserError } from './api'

/**
 * Búsquedas guardadas (Configuración → Búsquedas guardadas), igual que SearchRepository
 * en la app. Cuando se publica un auto que coincide, la BD crea el aviso
 * "busqueda_coincidencia" (también llega por push al teléfono).
 */

export interface BusquedaGuardada {
  id_busqueda: number
  id_marca: number | null
  id_modelo: number | null
  anio: number | null
  id_carroceria: number | null
  precio_min: number | null
  precio_max: number | null
  estado_ubicacion: string | null
  fecha_creacion: string
  marcas: { nombre_marca: string } | null
  modelos: { nombre_modelo: string } | null
  carrocerias: { nombre_carroceria: string } | null
}

/** Filtros a guardar (ids del catálogo); null = cualquiera. */
export interface FiltrosBusqueda {
  id_marca: number | null
  id_modelo: number | null
  anio: number | null
  id_carroceria: number | null
  precio_min: number | null
  precio_max: number | null
}

export const busquedaVacia = (f: FiltrosBusqueda) => Object.values(f).every((v) => v == null)

export async function misBusquedas(uid: string): Promise<BusquedaGuardada[]> {
  const { data, error } = await supabase
    .from('busquedas_guardadas')
    .select('*, marcas(nombre_marca), modelos(nombre_modelo), carrocerias(nombre_carroceria)')
    .eq('id_cuenta', uid)
    .order('fecha_creacion', { ascending: false })
  if (error) throw new Error(error.message)
  return (data ?? []) as BusquedaGuardada[]
}

export async function guardarBusqueda(uid: string, f: FiltrosBusqueda): Promise<void> {
  const { error } = await supabase.from('busquedas_guardadas').insert({ id_cuenta: uid, ...f })
  if (!error) return
  // 23505: uq_busquedas_guardadas (ya estaba guardada). P0001: máximo 10 (mensaje de la BD).
  if (error.code === '23505') throw new UserError(traducir('saved.duplicate'))
  if (error.code === 'P0001') throw new UserError(error.message)
  throw new Error(error.message)
}

export async function borrarBusqueda(id: number): Promise<void> {
  const { error } = await supabase.from('busquedas_guardadas').delete().eq('id_busqueda', id)
  if (error) throw new Error(error.message)
}

/** "Toyota Hilux · 2019 · Pickup · Hasta $500,000 · Coahuila". */
export function describirBusqueda(b: BusquedaGuardada): string {
  const auto = [b.marcas?.nombre_marca, b.modelos?.nombre_modelo].filter(Boolean).join(' ')
  const rango =
    b.precio_min != null && b.precio_max != null
      ? `${precio(b.precio_min)} – ${precio(b.precio_max)}`
      : b.precio_max != null
        ? traducir('saved.priceMax', { p: precio(b.precio_max) })
        : b.precio_min != null
          ? traducir('saved.priceMin', { p: precio(b.precio_min) })
          : null
  return [auto || null, b.anio, b.carrocerias?.nombre_carroceria, rango, b.estado_ubicacion].filter((x) => x != null && x !== '').join(' · ')
}
