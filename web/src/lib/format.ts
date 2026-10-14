// Mismos formatos que core/util/Formato.kt de la app.
import { traducir } from '../i18n/I18nContext'

const numeros = new Intl.NumberFormat('es-MX', { maximumFractionDigits: 0 })

/** 329900 → "$329,900" */
export const precio = (v: number | null | undefined) => (v == null ? '—' : '$' + numeros.format(v))

/** 48000 → "48,000 km" */
export const km = (v: number | null | undefined) => (v == null ? '—' : numeros.format(v) + ' km')

export const entero = (v: number) => numeros.format(v)

/** "Hace 5 min", "Hace 3 horas", "Ayer", "Hace 4 días"… (en el idioma elegido) */
export function haceCuanto(iso: string | null | undefined): string {
  if (!iso) return ''
  const t = new Date(iso).getTime()
  if (Number.isNaN(t)) return ''
  const min = Math.floor((Date.now() - t) / 60000)
  if (min < 1) return traducir('vehiculo.time.justNow')
  if (min < 60) return traducir('vehiculo.time.minutes', { n: min })
  const horas = Math.floor(min / 60)
  if (horas < 24) return horas === 1 ? traducir('vehiculo.time.hour') : traducir('vehiculo.time.hours', { n: horas })
  const dias = Math.floor(horas / 24)
  if (dias === 1) return traducir('vehiculo.time.yesterday')
  if (dias < 7) return traducir('vehiculo.time.days', { n: dias })
  if (dias < 30) return Math.floor(dias / 7) === 1 ? traducir('vehiculo.time.week') : traducir('vehiculo.time.weeks', { n: Math.floor(dias / 7) })
  return new Date(iso).toLocaleDateString(traducir('vehiculo.locale'), { day: 'numeric', month: 'short', year: 'numeric' })
}

/** Iniciales para avatares sin foto: "Auto Premium" → "AP". */
export const iniciales = (nombre: string) =>
  nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]!.toUpperCase()).join('')

/** Números de 10 dígitos se toman como de México (+52), igual que la app. */
export function whatsappUrl(numero: string, mensaje: string) {
  const digitos = numero.replace(/\D/g, '')
  const internacional = digitos.length === 10 ? '52' + digitos : digitos
  return `https://wa.me/${internacional}?text=${encodeURIComponent(mensaje)}`
}
