// Reglas de los formularios de cuenta: las mismas que FormRules.kt y PasswordRules.kt de la app.
// Cada función devuelve el mensaje de error o null si el valor es válido; un valor vacío
// se considera válido aquí (lo obligatorio se revisa aparte). Los máximos son los de la BD.

export const NOMBRE_MAX = 50
export const LOTE_MAX = 120
export const DIRECCION_MAX = 120
export const NUMERO_MAX = 20
export const DESCRIPCION_MAX = 300
/** Lado mínimo del logo del lote en píxeles (más chico se ve borroso). */
export const LOGO_MIN_PX = 200

const CORREO = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/
/** Letras (con acentos y ñ), espacios, guion, apóstrofe y punto ("Ma. José", "O'Neil"). */
const NOMBRE = /^\p{L}[\p{L} .'-]*$/u
/** El nombre de un lote puede llevar números y signos comunes ("Autos 4x4 & Cía."). */
const NOMBRE_LOTE = /^[\p{L}\p{N}][\p{L}\p{N} .,'&()-]*$/u
/** "123", "123-A", "123 B", "S/N". */
const NUMERO_EXT = /^([0-9]+[A-Za-z]?([ -]?[A-Za-z0-9]+)?|[Ss]\/?[Nn])$/

// ── Filtros al escribir ──

export const soloDigitos = (t: string, max = Infinity) => t.replace(/\D/g, '').slice(0, max)

/** Solo dígitos, máximo 10; se permite "+" al inicio para la lada ("+52" y 10 dígitos). */
export function telefonoAlEscribir(t: string): string {
  const d = t.replace(/\D/g, '')
  return t.trimStart().startsWith('+') ? '+' + d.slice(0, 12) : d.slice(0, 10)
}

/** Nombre o apellido al escribir: solo letras y máximo NOMBRE_MAX. */
export const nombreAlEscribir = (t: string) => t.replace(/[^\p{L} .'-]/gu, '').slice(0, NOMBRE_MAX)

// ── Limpieza ──

export const limpiarCorreo = (c: string) => c.trim().toLowerCase()
export const limpiarEspacios = (t: string) => t.trim().replace(/\s+/g, ' ')

/** "871 123 4567", "+52 871 123 4567" y "528711234567" → "8711234567". */
export function normalizarTelefono(v: string): string {
  const d = v.replace(/\D/g, '')
  return d.length === 12 && d.startsWith('52') ? d.slice(2) : d
}

/** "juan  carlos de la o" → "Juan Carlos de la O" (artículos y preposiciones en minúscula). */
export function capitalizarNombre(t: string): string {
  const menores = new Set(['de', 'del', 'la', 'las', 'los', 'y', 'e', 'van', 'von'])
  const mayus = (s: string) => (s ? s[0].toLocaleUpperCase('es') + s.slice(1) : s)
  return limpiarEspacios(t)
    .split(' ')
    .map((palabra, i) => {
      const baja = palabra.toLocaleLowerCase('es')
      if (i > 0 && menores.has(baja)) return baja
      return baja
        .split('-')
        .map((p) => p.split("'").map(mayus).join("'"))
        .join('-')
    })
    .join(' ')
}

// ── Sugerencia de correo ("gmil.com" → "gmail.com") ──

const DOMINIOS = [
  'gmail.com', 'hotmail.com', 'outlook.com', 'yahoo.com', 'yahoo.com.mx', 'live.com',
  'live.com.mx', 'icloud.com', 'hotmail.es', 'outlook.es', 'prodigy.net.mx', 'msn.com',
]

function distancia(a: string, b: string): number {
  let anterior = Array.from({ length: b.length + 1 }, (_, i) => i)
  for (let i = 1; i <= a.length; i++) {
    const actual = [i]
    for (let j = 1; j <= b.length; j++) {
      const costo = a[i - 1] === b[j - 1] ? 0 : 1
      actual[j] = Math.min(actual[j - 1] + 1, anterior[j] + 1, anterior[j - 1] + costo)
    }
    anterior = actual
  }
  return anterior[b.length]
}

export function sugerenciaCorreo(valor: string): string | null {
  const c = limpiarCorreo(valor)
  const at = c.indexOf('@')
  if (at <= 0) return null
  const usuario = c.slice(0, at)
  const dominio = c.slice(at + 1)
  if (dominio.length < 3 || DOMINIOS.includes(dominio)) return null
  let mejor = DOMINIOS[0]
  let dist = Infinity
  for (const d of DOMINIOS) {
    const x = distancia(dominio, d)
    if (x < dist) [mejor, dist] = [d, x]
  }
  // Dominios cortos con 2 cambios ya serían otro dominio (p. ej. "gmx.com").
  const maximo = dominio.length <= 6 ? 1 : 2
  return dist >= 1 && dist <= maximo ? `${usuario}@${mejor}` : null
}

// ── Validaciones ──

export function correo(v: string): string | null {
  const c = limpiarCorreo(v)
  return !c || CORREO.test(c) ? null : 'Escribe un correo válido (ej. nombre@gmail.com).'
}

export function nombrePersona(valor: string): string | null {
  const v = limpiarEspacios(valor)
  if (!v) return null
  if (!NOMBRE.test(v)) return 'Usa solo letras (sin números ni símbolos).'
  if ((v.match(/\p{L}/gu) ?? []).length < 2) return 'Escribe al menos 2 caracteres.'
  if (v.length > NOMBRE_MAX) return `Máximo ${NOMBRE_MAX} caracteres.`
  return null
}

export function nombreLote(valor: string): string | null {
  const v = limpiarEspacios(valor)
  if (!v) return null
  if (!NOMBRE_LOTE.test(v)) return 'Usa letras, números y signos comunes.'
  if (v.length < 3) return 'Escribe al menos 3 caracteres.'
  if (v.length > LOTE_MAX) return `Máximo ${LOTE_MAX} caracteres.`
  return null
}

/** Calle o colonia. */
export function textoDireccion(valor: string): string | null {
  const v = limpiarEspacios(valor)
  if (!v) return null
  if (v.length < 3) return 'Escribe al menos 3 caracteres.'
  if (v.length > DIRECCION_MAX) return `Máximo ${DIRECCION_MAX} caracteres.`
  return null
}

export function numeroExterior(valor: string): string | null {
  const v = limpiarEspacios(valor)
  if (!v) return null
  if (v.length > NUMERO_MAX) return `Máximo ${NUMERO_MAX} caracteres.`
  if (!NUMERO_EXT.test(v)) return 'Escribe un número válido (ej. 123, 123-A o S/N).'
  return null
}

export function codigoPostal(valor: string): string | null {
  const v = valor.trim()
  return !v || /^\d{5}$/.test(v) ? null : 'El código postal debe tener 5 dígitos.'
}

/**
 * Números que nadie tiene: todos iguales (5555555555), escaleras (0123456789,
 * 9876543210), dos dígitos repetidos (1212121212) o casi todos iguales (8888888881).
 */
function esTelefonoFalso(n: string): boolean {
  const d = [...n].map(Number)
  const escalera =
    d.every((x, i) => i === 0 || x === (d[i - 1] + 1) % 10) || d.every((x, i) => i === 0 || x === (d[i - 1] + 9) % 10)
  const pares = new Set(n.match(/.{1,2}/g))
  const conteo: Record<string, number> = {}
  for (const c of n) conteo[c] = (conteo[c] ?? 0) + 1
  return escalera || pares.size === 1 || Math.max(...Object.values(conteo)) >= 8
}

export function telefono(valor: string): string | null {
  if (!valor.trim()) return null
  const n = normalizarTelefono(valor)
  if (n.length !== 10) return 'El teléfono debe tener 10 dígitos.'
  if (esTelefonoFalso(n)) return 'Escribe un número de teléfono real.'
  return null
}

// ── Código postal ↔ estado (los 2 primeros dígitos, esquema de SEPOMEX) ──

const CP_ESTADO: [number, number, string][] = [
  [1, 16, 'Ciudad de México'], [20, 20, 'Aguascalientes'], [21, 22, 'Baja California'],
  [23, 23, 'Baja California Sur'], [24, 24, 'Campeche'], [25, 27, 'Coahuila'], [28, 28, 'Colima'],
  [29, 30, 'Chiapas'], [31, 33, 'Chihuahua'], [34, 35, 'Durango'], [36, 38, 'Guanajuato'],
  [39, 41, 'Guerrero'], [42, 43, 'Hidalgo'], [44, 49, 'Jalisco'], [50, 57, 'Estado de México'],
  [58, 61, 'Michoacán'], [62, 62, 'Morelos'], [63, 63, 'Nayarit'], [64, 67, 'Nuevo León'],
  [68, 71, 'Oaxaca'], [72, 75, 'Puebla'], [76, 76, 'Querétaro'], [77, 77, 'Quintana Roo'],
  [78, 79, 'San Luis Potosí'], [80, 82, 'Sinaloa'], [83, 85, 'Sonora'], [86, 86, 'Tabasco'],
  [87, 89, 'Tamaulipas'], [90, 90, 'Tlaxcala'], [91, 96, 'Veracruz'], [97, 97, 'Yucatán'],
  [98, 99, 'Zacatecas'],
]

const sinAcentos = (t: string) => t.normalize('NFD').replace(/\p{Mn}+/gu, '').toLowerCase()

/** "Este código postal es de Jalisco, no de Coahuila." (aviso; no bloquea el registro). */
export function avisoCodigoPostalEstado(cp: string, estado: string): string | null {
  if (!estado.trim() || !/^\d{5}$/.test(cp)) return null
  const prefijo = Number(cp.slice(0, 2))
  const delCp = CP_ESTADO.find(([a, b]) => prefijo >= a && prefijo <= b)?.[2]
  if (!delCp) return null
  const a = sinAcentos(delCp)
  const b = sinAcentos(estado)
  return a === b || b.startsWith(a) ? null : `Este código postal es de ${delCp}, no de ${estado}. Revísalo.`
}

// ── Contraseña (PasswordRules.kt) ──

export const PASSWORD_MIN = 8
/** Supabase (bcrypt) solo toma en cuenta los primeros 72 bytes. */
export const PASSWORD_MAX = 72

export const PASSWORD_RULES: [string, (p: string) => boolean][] = [
  ['Mínimo 8 caracteres', (p) => p.length >= PASSWORD_MIN],
  ['Una letra mayúscula', (p) => /\p{Lu}/u.test(p)],
  ['Una letra minúscula', (p) => /\p{Ll}/u.test(p)],
  ['Un número', (p) => /\d/.test(p)],
  ['Un signo (!@#*…)', (p) => /[^\p{L}\p{N}\s]/u.test(p)],
  ['Sin espacios', (p) => p.length > 0 && !/\s/.test(p)],
]

/** Primer problema de la contraseña, o null si es válida. [correo] evita que la contenga. */
export function passwordError(password: string, correo?: string): string | null {
  const usuario = (correo ?? '').split('@')[0].trim()
  if (password.length > PASSWORD_MAX) return `La contraseña puede tener máximo ${PASSWORD_MAX} caracteres.`
  if (PASSWORD_RULES.some(([, ok]) => !ok(password))) return 'La contraseña no cumple los requisitos de seguridad.'
  if (usuario.length >= 3 && password.toLowerCase().includes(usuario.toLowerCase())) return 'La contraseña no debe contener tu correo.'
  return null
}
