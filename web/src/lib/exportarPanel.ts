import type { OwnCar, SellerStats } from '../data/seller'

/** Vistas, favoritos y contactos de una publicación (totales). */
export interface FilaRendimiento {
  id: number
  vistas: number
  favoritos: number
  contactos: number
}

/**
 * Una fila por cada publicación ya publicada (aunque aún no tenga vistas), la más vista
 * primero. Los números vienen de la BD por id y, si no hay, van en 0.
 */
export function filasRendimiento(stats: SellerStats, cars: OwnCar[]): FilaRendimiento[] {
  const datos = new Map(stats.rendimiento.map((r) => [r.id, r]))
  return cars
    .filter((c) => c.approved)
    .map((c) => datos.get(c.id) ?? { id: c.id, vistas: 0, favoritos: 0, contactos: 0 })
    .sort((a, b) => b.vistas - a.vistas || b.contactos - a.contactos)
}

/** % de b sobre a, sin decimales y hasta 100 (se puede guardar en favoritos sin abrir el detalle). */
export const pct = (b: number, a: number) => (a > 0 ? Math.min(100, Math.round((b * 100) / a)) : 0)

/** Días de calendario desde [iso] (solo AAAA-MM-DD, a medianoche local) hasta hoy; 0 si no hay fecha válida. Igual que la app. */
export function diasDesde(iso: string, hoy = Date.now()) {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso ?? '')
  if (!m) return 0
  const fecha = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
  if (fecha.getMonth() !== Number(m[2]) - 1 || fecha.getDate() !== Number(m[3])) return 0
  return Math.max(0, Math.floor((hoy - fecha.getTime()) / 86_400_000))
}

export interface Consejo {
  tipo: 'sinContacto' | 'pocasVistas'
  car: OwnCar
}

/**
 * Lo más destacado del reporte y hasta 3 recomendaciones:
 * - muchas vistas (20+) y ningún contacto: revisar precio o descripción;
 * - activa desde hace 7+ días con menos de 5 vistas: mejores fotos o destacarla.
 */
export function resumenReporte(stats: SellerStats, cars: OwnCar[], hoy = Date.now()) {
  const porId = new Map(cars.map((c) => [c.id, c]))
  const filas = filasRendimiento(stats, cars)
  const masVista = filas.find((r) => r.vistas > 0)
  const masContactada = [...filas].sort((a, b) => b.contactos - a.contactos)[0]
  const consejos: Consejo[] = []
  for (const r of filas) {
    const c = porId.get(r.id)!
    if (r.vistas >= 20 && r.contactos === 0) consejos.push({ tipo: 'sinContacto', car: c })
    else if (c.status === 'Activo' && r.vistas < 5 && diasDesde(c.publishedAt, hoy) >= 7) consejos.push({ tipo: 'pocasVistas', car: c })
  }
  return {
    masVista: masVista ? porId.get(masVista.id) : undefined,
    masContactada: masContactada && masContactada.contactos > 0 ? porId.get(masContactada.id) : undefined,
    sinVistas: filas.filter((r) => r.vistas === 0).length,
    consejos: consejos.slice(0, 3),
  }
}

/** Una celda de CSV: entre comillas si trae coma, comillas o salto de línea. */
const celda = (v: string | number) => {
  const s = String(v)
  return /[",\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
}

export interface TextosCsv {
  titulo: string
  vendedor: string
  fecha: string
  resumen: [string, number][]
  columnas: string[]
  estado: (c: OwnCar) => string
  fechaPublicacion: (c: OwnCar) => string
}

/** CSV de Mi panel: resumen arriba y una fila por publicación (se abre en Excel o Google Sheets). */
export function csvPanel(stats: SellerStats, cars: OwnCar[], tx: TextosCsv): string {
  const porId = new Map(cars.map((c) => [c.id, c]))
  const lineas: (string | number)[][] = [[tx.titulo], [tx.vendedor], [tx.fecha], [], ...tx.resumen, [], tx.columnas]
  for (const r of filasRendimiento(stats, cars)) {
    const c = porId.get(r.id)!
    lineas.push([
      `${c.brand} ${c.model}`.trim(),
      c.year || '',
      c.priceValue,
      c.currency,
      tx.estado(c),
      r.vistas,
      r.favoritos,
      r.contactos,
      `${pct(r.contactos, r.vistas)}%`,
      tx.fechaPublicacion(c),
    ])
  }
  // El BOM hace que Excel lea bien los acentos.
  return '\ufeff' + lineas.map((l) => l.map(celda).join(',')).join('\r\n')
}

/** Descarga [contenido] como archivo en el navegador. */
export function descargar(nombre: string, contenido: string, tipo: string) {
  const url = URL.createObjectURL(new Blob([contenido], { type: tipo }))
  const a = document.createElement('a')
  a.href = url
  a.download = nombre
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
