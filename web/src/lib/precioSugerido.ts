/**
 * Precio sugerido al publicar: rango y precio típico de autos similares que ya están en
 * Karsy (mismo modelo y años parecidos). Igual que core/util/PrecioSugerido.kt en la app.
 */

export interface Rango {
  /** Cuántos autos similares se usaron. */
  n: number
  min: number
  max: number
  /** Mediana: el precio "del medio", sin que lo muevan los muy caros o muy baratos. */
  tipico: number
}

/** Mínimo de autos para sugerir algo; con menos, la sugerencia no sería confiable. */
export const MINIMO_SIMILARES = 2

const redondear = (n: number) => Math.round(n / 1000) * 1000

/** Valor en la posición p (0–1) de una lista ordenada, interpolando entre vecinos. */
function percentil(ordenados: number[], p: number): number {
  const i = (ordenados.length - 1) * p
  const abajo = Math.floor(i)
  const arriba = Math.ceil(i)
  return ordenados[abajo] + (ordenados[arriba] - ordenados[abajo]) * (i - abajo)
}

/**
 * Rango de precios de autos similares, redondeado a miles. Con 5 o más se usa del 20 % al
 * 80 % (para que un precio raro no estire el rango); con menos, del más barato al más caro.
 * null si hay menos de MINIMO_SIMILARES.
 */
export function calcularRango(precios: number[]): Rango | null {
  const ordenados = precios.filter((p) => Number.isFinite(p) && p > 0).sort((a, b) => a - b)
  if (ordenados.length < MINIMO_SIMILARES) return null
  const conPercentiles = ordenados.length >= 5
  return {
    n: ordenados.length,
    min: redondear(conPercentiles ? percentil(ordenados, 0.2) : ordenados[0]),
    max: redondear(conPercentiles ? percentil(ordenados, 0.8) : ordenados[ordenados.length - 1]),
    tipico: redondear(percentil(ordenados, 0.5)),
  }
}

/** Dónde queda el precio escrito respecto al rango. */
export function compararPrecio(precio: number, r: Rango): 'abajo' | 'dentro' | 'arriba' {
  if (precio < r.min) return 'abajo'
  if (precio > r.max) return 'arriba'
  return 'dentro'
}
