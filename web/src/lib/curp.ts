/**
 * Datos que vienen dentro de una CURP (para que el admin los compare con lo que muestra
 * gob.mx/curp). Igual que core/util/Curp.kt en la app.
 *
 * Posiciones (desde 0): 4–9 fecha AAMMDD, 10 sexo (H, M, X), 11–12 estado de nacimiento,
 * 16 siglo (dígito = 1900–1999, letra = 2000 en adelante).
 */

/** Claves de estado de la CURP (NE = nacido en el extranjero). */
export const ESTADOS_CURP: Record<string, string> = {
  AS: 'Aguascalientes', BC: 'Baja California', BS: 'Baja California Sur', CC: 'Campeche', CL: 'Coahuila',
  CM: 'Colima', CS: 'Chiapas', CH: 'Chihuahua', DF: 'Ciudad de México', DG: 'Durango', GT: 'Guanajuato',
  GR: 'Guerrero', HG: 'Hidalgo', JC: 'Jalisco', MC: 'Estado de México', MN: 'Michoacán', MS: 'Morelos',
  NT: 'Nayarit', NL: 'Nuevo León', OC: 'Oaxaca', PL: 'Puebla', QT: 'Querétaro', QR: 'Quintana Roo',
  SP: 'San Luis Potosí', SL: 'Sinaloa', SR: 'Sonora', TC: 'Tabasco', TS: 'Tamaulipas', TL: 'Tlaxcala',
  VZ: 'Veracruz', YN: 'Yucatán', ZS: 'Zacatecas',
}

export interface DatosCurp {
  anio: number
  /** 1–12 */
  mes: number
  dia: number
  sexo: 'H' | 'M' | 'X'
  /** Clave de dos letras (p. ej. "JC"); "NE" = nacido en el extranjero. */
  estado: string
}

/** null si no tiene el formato de una CURP. */
export function leerCurp(curp: string): DatosCurp | null {
  const c = curp.trim().toUpperCase()
  if (!/^[A-Z]{4}\d{6}[HMX][A-Z]{2}[A-Z]{3}[A-Z0-9]\d$/.test(c)) return null
  const aa = Number(c.slice(4, 6))
  const mes = Number(c.slice(6, 8))
  const dia = Number(c.slice(8, 10))
  const estado = c.slice(11, 13)
  const anio = (/\d/.test(c[16]) ? 1900 : 2000) + aa
  if (mes < 1 || mes > 12 || dia < 1 || dia > diasDelMes(anio, mes) || (estado !== 'NE' && !ESTADOS_CURP[estado])) return null
  return { anio, mes, dia, sexo: c[10] as DatosCurp['sexo'], estado }
}

/** Días que tiene el mes (con febrero bisiesto): un 31 de abril no es una fecha real. */
export function diasDelMes(anio: number, mes: number): number {
  if (mes === 2) return (anio % 4 === 0 && anio % 100 !== 0) || anio % 400 === 0 ? 29 : 28
  return [4, 6, 9, 11].includes(mes) ? 30 : 31
}
