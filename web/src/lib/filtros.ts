import type { Car } from '../data/types'
import type { Clave } from '../i18n/es'

/** Campos del panel de filtros (mismos que el menú de filtros de la app). */
export const FILTER_KEYS = ['tipo', 'marca', 'modelo', 'anio', 'min', 'max'] as const
export type FilterKey = (typeof FILTER_KEYS)[number]
export type Filtros = Record<FilterKey, string>

export const SIN_FILTROS: Filtros = { tipo: '', marca: '', modelo: '', anio: '', min: '', max: '' }

/** Opciones de orden: valor en la URL y clave de su texto (t()). */
export const ORDEN: readonly (readonly [string, Clave])[] = [
  ['recientes', 'sort.recent'],
  ['menor', 'sort.low'],
  ['mayor', 'sort.high'],
]

export const hayFiltros = (f: Filtros) => FILTER_KEYS.some((k) => f[k])

/**
 * Búsqueda + filtros + orden (filtrar() de HomeViewModel.kt).
 * Con [conservarOrden], "Más recientes" deja la lista como viene (Favoritos: orden en que se guardaron).
 */
export function filtrarCars(cars: Car[], f: Filtros, q: string, orden: string, conservarOrden = false): Car[] {
  const texto = q.trim().toLowerCase()
  const min = Number(f.min) || null
  const max = Number(f.max) || null
  const lista = cars.filter(
    (c) =>
      (!texto || `${c.brand} ${c.model} ${c.year}`.toLowerCase().includes(texto)) &&
      (!f.marca || String(c.brandId) === f.marca) &&
      (!f.modelo || String(c.modelId) === f.modelo) &&
      (!f.anio || String(c.year) === f.anio) &&
      (!f.tipo || c.bodyType === f.tipo) &&
      (min == null || c.priceValue >= min) &&
      (max == null || c.priceValue <= max),
  )
  switch (orden) {
    case 'menor':
      return lista.sort((a, b) => a.priceValue - b.priceValue)
    case 'mayor':
      return lista.sort((a, b) => b.priceValue - a.priceValue)
    default:
      return conservarOrden ? lista : lista.sort((a, b) => b.publishedAt.localeCompare(a.publishedAt))
  }
}

/** Filtros guardados en la URL (?tipo=…&marca=…). */
export function filtrosDeUrl(p: URLSearchParams): Filtros {
  return Object.fromEntries(FILTER_KEYS.map((k) => [k, p.get(k) ?? ''])) as Filtros
}
