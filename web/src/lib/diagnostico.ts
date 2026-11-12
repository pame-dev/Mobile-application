/**
 * TEMPORAL — diagnóstico del formulario de publicar en iPhone (Safari recarga la página al
 * poner la primera foto). Guarda en este navegador los pasos que se van haciendo con cada
 * foto; si la página se recarga, Publicar muestra los últimos pasos que alcanzó a hacer.
 * No se envía nada a ningún lado. Borrar este archivo y sus usos al encontrar la causa.
 */

const CLAVE = 'karsy-diagnostico-fotos'
const MAX = 25

/** Agrega un paso con la hora (segundos y milésimas). */
export function diag(paso: string): void {
  try {
    const lista = JSON.parse(localStorage.getItem(CLAVE) ?? '[]') as string[]
    const d = new Date()
    const hora = `${d.toLocaleTimeString('es-MX')}.${String(d.getMilliseconds()).padStart(3, '0')}`
    lista.push(`${hora} ${paso}`)
    localStorage.setItem(CLAVE, JSON.stringify(lista.slice(-MAX)))
  } catch {
    /* sin almacenamiento: no hay diagnóstico */
  }
}

/** Pasos guardados (de la prueba anterior si la página se recargó). Solo en iPhone y iPad. */
export function leerDiag(): string[] {
  const esIOS = /iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.userAgent.includes('Macintosh') && navigator.maxTouchPoints > 1)
  if (!esIOS) return []
  try {
    return JSON.parse(localStorage.getItem(CLAVE) ?? '[]') as string[]
  } catch {
    return []
  }
}

export function borrarDiag(): void {
  try {
    localStorage.removeItem(CLAVE)
  } catch {
    /* nada */
  }
}

/** Tamaño legible de un archivo. */
export const tam = (bytes: number) => `${(bytes / 1048576).toFixed(1)} MB`
