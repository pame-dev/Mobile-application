/**
 * Traducción automática español → inglés con MyMemory (gratis, sin cuenta ni clave; la
 * llama directo el navegador). En inglés la usan el detalle de un auto (descripción e
 * imperfecciones) y los textos que escriben los usuarios y el admin (components/Traducido).
 * Igual que core/util/Traductor.kt.
 *
 * - MyMemory acepta hasta ~500 bytes por petición: el texto se parte por oraciones.
 * - Límite gratuito: unos 5,000 caracteres al día por conexión; si se acaba o falla,
 *   regresa null y se muestra el original. Si se acabó, no se vuelve a pedir en una hora.
 * - Cada traducción se guarda en el navegador para no pedirla dos veces.
 */

const API = 'https://api.mymemory.translated.net/get'
const MAX_BYTES = 450
/** Hasta cuándo (ms) no se pide nada porque se acabó el límite diario; se recuerda en el navegador. */
const AGOTADO = 'karsy-traduccion-agotada'
const ESPERA_SI_AGOTADO = 60 * 60 * 1000

class CuotaAgotada extends Error {}

function agotada(): boolean {
  try {
    return Date.now() < Number(localStorage.getItem(AGOTADO) ?? 0)
  } catch {
    return false
  }
}
const CACHE = 'karsy-traduccion:'

const bytes = (t: string) => new TextEncoder().encode(t).length

/** Parte el texto en pedazos de máximo MAX_BYTES, por oraciones (y por palabras si una oración es muy larga). */
function partes(texto: string): string[] {
  const oraciones = texto.match(/[^.!?\n]+[.!?]*\s*|\n+/g) ?? [texto]
  const salida: string[] = []
  let actual = ''
  const agregar = (pedazo: string) => {
    if (bytes(actual + pedazo) <= MAX_BYTES) actual += pedazo
    else {
      if (actual) salida.push(actual)
      actual = pedazo
    }
  }
  for (const o of oraciones) {
    if (bytes(o) <= MAX_BYTES) agregar(o)
    else for (const palabra of o.split(/(?<=\s)/)) agregar(palabra)
  }
  if (actual) salida.push(actual)
  return salida
}

async function traducirParte(texto: string): Promise<string> {
  if (!texto.trim()) return texto
  const r = await fetch(`${API}?q=${encodeURIComponent(texto.trim())}&langpair=es|en`)
  const d = (await r.json()) as { responseStatus: number | string; responseData?: { translatedText?: string } }
  const traducido = d.responseData?.translatedText
  // Con la cuota agotada responde 429 o un texto "MYMEMORY WARNING…".
  if (Number(d.responseStatus) === 429 || traducido?.startsWith('MYMEMORY WARNING')) throw new CuotaAgotada()
  if (Number(d.responseStatus) !== 200 || !traducido) throw new Error('sin traducción')
  // Conserva los espacios y saltos de línea de alrededor.
  return texto.replace(texto.trim(), traducido)
}

/**
 * Inicio del texto que cabe en una tarjeta (hasta `max` caracteres, cortado en la última
 * palabra completa) y si se cortó. Se traduce solo eso para gastar poco del límite diario.
 */
export function inicio(texto: string, max = 100): [string, boolean] {
  const limpio = texto.trim().replace(/\s+/g, ' ')
  if (limpio.length <= max) return [limpio, false]
  const espacio = limpio.lastIndexOf(' ', max)
  const corte = espacio > max / 2 ? espacio : max
  return [limpio.slice(0, corte).trimEnd(), true]
}

/**
 * El texto en inglés, o null si no se pudo traducir. Con `corto` solo se traduce su
 * `inicio` (listas y tarjetas) y se agrega "…" si se cortó.
 */
export async function traducirAlIngles(texto: string, corto = false): Promise<string | null> {
  if (corto) {
    const [ini, cortado] = inicio(texto)
    const tr = await traducirAlIngles(ini)
    return tr && cortado ? `${tr}…` : tr
  }
  const limpio = texto.trim()
  if (!limpio) return null
  const guardado = traduccionGuardada(limpio)
  if (guardado) return guardado
  if (agotada()) return null
  // Una sola petición por texto aunque la pidan varias tarjetas a la vez.
  let pedido = enCurso.get(limpio)
  if (!pedido) {
    pedido = pedir(limpio).finally(() => enCurso.delete(limpio))
    enCurso.set(limpio, pedido)
  }
  return pedido
}

const enCurso = new Map<string, Promise<string | null>>()

/** La traducción ya guardada en este navegador (sin pedirla), o null; `corto` igual que en traducirAlIngles. */
export function traduccionGuardada(texto: string, corto = false): string | null {
  if (corto) {
    const [ini, cortado] = inicio(texto)
    const tr = traduccionGuardada(ini)
    return tr && cortado ? `${tr}…` : tr
  }
  try {
    return localStorage.getItem(CACHE + texto.trim())
  } catch {
    return null // almacenamiento bloqueado: se traduce sin guardar
  }
}

async function pedir(limpio: string): Promise<string | null> {
  const clave = CACHE + limpio
  try {
    let resultado = ''
    for (const p of partes(limpio)) resultado += await traducirParte(p)
    resultado = resultado.trim()
    try {
      localStorage.setItem(clave, resultado)
    } catch {
      // sin espacio o bloqueado: no pasa nada
    }
    return resultado
  } catch (e) {
    if (e instanceof CuotaAgotada) {
      try {
        localStorage.setItem(AGOTADO, String(Date.now() + ESPERA_SI_AGOTADO))
      } catch {
        // bloqueado: se volverá a intentar
      }
    }
    return null
  }
}
