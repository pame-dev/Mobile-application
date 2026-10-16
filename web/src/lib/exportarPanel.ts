import type { OwnCar, ReporteExtra, SellerStats } from '../data/seller'
import type { Clave } from '../i18n/es'
import type { T } from '../i18n/I18nContext'
import { ESTILO, columna, fechaExcel, xlsx, type Celda, type Hoja } from './xlsx'

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
  tipo: 'sinContacto' | 'pocasVistas' | 'precioAlto'
  car: OwnCar
}

/** Precio del auto vs el promedio de similares: 0.08 = 8 % arriba; null si no hay con qué comparar. */
export function difMercado(c: OwnCar, extra: ReporteExtra | null): number | null {
  const m = extra?.mercado.find((x) => x.id === c.id)
  return m && m.promedio > 0 && c.priceValue > 0 ? (c.priceValue - m.promedio) / m.promedio : null
}

/**
 * Lo más destacado del reporte y hasta 3 recomendaciones (igual que la app):
 * - muchas vistas (20+) y ningún contacto: revisar precio o descripción;
 * - activa desde hace 7+ días con menos de 5 vistas: mejores fotos o destacarla;
 * - activa con precio 10 %+ arriba del promedio de 2+ autos similares.
 */
export function resumenReporte(stats: SellerStats, cars: OwnCar[], hoy = Date.now(), extra: ReporteExtra | null = null) {
  const porId = new Map(cars.map((c) => [c.id, c]))
  const filas = filasRendimiento(stats, cars)
  const masVista = filas.find((r) => r.vistas > 0)
  const masContactada = [...filas].sort((a, b) => b.contactos - a.contactos)[0]
  const consejos: Consejo[] = []
  for (const r of filas) {
    const c = porId.get(r.id)!
    const similares = extra?.mercado.find((x) => x.id === c.id)?.similares ?? 0
    const dif = difMercado(c, extra)
    if (r.vistas >= 20 && r.contactos === 0) consejos.push({ tipo: 'sinContacto', car: c })
    else if (c.status === 'Activo' && r.vistas < 5 && diasDesde(c.publishedAt, hoy) >= 7) consejos.push({ tipo: 'pocasVistas', car: c })
    else if (c.status === 'Activo' && dif !== null && dif >= 0.1 && similares >= 2) consejos.push({ tipo: 'precioAlto', car: c })
  }
  return {
    masVista: masVista ? porId.get(masVista.id) : undefined,
    masContactada: masContactada && masContactada.contactos > 0 ? porId.get(masContactada.id) : undefined,
    sinVistas: filas.filter((r) => r.vistas === 0).length,
    consejos: consejos.slice(0, 3),
  }
}

/** Índice del valor más alto (el primero si empatan); -1 si todo es 0. */
export const pico = (v: number[]) => (v.some((x) => x > 0) ? v.indexOf(Math.max(...v)) : -1)

/** Publicaciones que necesitan algo: rechazadas, en revisión o deshabilitadas (con su motivo). */
export const pendientes = (cars: OwnCar[]) => cars.filter((c) => c.status === 'Rechazado' || c.status === 'Pendiente' || c.status === 'Deshabilitado')

/** Vendidas: total, por Karsy y fuera. */
export function vendidos(cars: OwnCar[]) {
  const v = cars.filter((c) => c.status === 'Vendido')
  return { total: v.length, dentro: v.filter((c) => c.soldVia === 'dentro_app').length, fuera: v.filter((c) => c.soldVia === 'fuera_app').length }
}

/** Vistas por día antes y durante el destacado, y el cambio (0.5 = +50 %). */
export function efectoDestacado(d: ReporteExtra['destacados'][number]) {
  // Cada ventana con sus días exactos (la de "antes" puede ser más corta si el auto se publicó hace poco).
  const antes = d.diasAntes > 0 ? d.vistasAntes / d.diasAntes : 0
  const durante = d.diasDurante > 0 ? d.vistasDurante / d.diasDurante : 0
  return { antes, durante, cambio: antes > 0 ? (durante - antes) / antes : null }
}

/** Llaves de los días (domingo = 0) y de los meses, para traducir. */
export const DIAS: Clave[] = ['perfil.daySun', 'perfil.dayMon', 'perfil.dayTue', 'perfil.dayWed', 'perfil.dayThu', 'perfil.dayFri', 'perfil.daySat']
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

/** Etiquetas de los últimos 12 meses (del más antiguo al actual), como vistas_por_mes. */
export function ultimosMeses(locale: string, hoy = new Date()): string[] {
  const nombres = locale.startsWith('en') ? MONTHS : MESES
  return Array.from({ length: 12 }, (_, i) => {
    const d = new Date(hoy.getFullYear(), hoy.getMonth() - 11 + i, 1)
    return `${nombres[d.getMonth()]} ${d.getFullYear()}`
  })
}

/** "19:00–20:00" */
export const rangoHora = (h: number) => `${String(h).padStart(2, '0')}:00–${String((h + 1) % 24).padStart(2, '0')}:00`

const s = (v: string | number | null, estilo: number): Celda => ({ v, s: estilo })

/**
 * Libro de Excel de Mi panel con 5 hojas: Resumen, Publicaciones, Actividad, Destacados
 * y Recomendaciones. Mismo contenido que el de la app (ExportarPanel.libro).
 */
export function libroPanel(
  stats: SellerStats,
  cars: OwnCar[],
  extra: ReporteExtra | null,
  info: { nombre: string; fecha: string; destacadas: number; locale: string; estado: (c: OwnCar) => string },
  t: T,
): Uint8Array<ArrayBuffer> {
  const porId = new Map(cars.map((c) => [c.id, c]))
  const filas = filasRendimiento(stats, cars)
  const res = resumenReporte(stats, cars, Date.now(), extra)
  const ven = vendidos(cars)
  const e = stats.embudo
  const titulo = (c: OwnCar) => `${c.brand} ${c.model} ${c.year || ''}`.trim()
  const porDia = extra?.porDia ?? []
  const porHora = extra?.porHora ?? []
  const dia = pico(porDia)
  const hora = pico(porHora)

  // 1. Resumen
  const resumen: Hoja = {
    nombre: t('perfil.xlsSheetSummary'),
    anchos: [34, 18, 36],
    unir: ['A1:C1', 'A2:C2'],
    filas: [
      [s(t('perfil.reportTitle'), ESTILO.titulo)],
      [s(`${info.nombre} · ${info.fecha}`, ESTILO.subtitulo)],
      [],
      [s(t('perfil.xlsListings'), ESTILO.seccion)],
      [s(t('perfil.kpiActive'), ESTILO.etiqueta), s(stats.activas, ESTILO.entero)],
      [s(t('perfil.kpiSold'), ESTILO.etiqueta), s(ven.total, ESTILO.entero), s(t('perfil.xlsSoldSplit', { dentro: ven.dentro, fuera: ven.fuera }), ESTILO.texto)],
      [s(t('perfil.kpiInReview'), ESTILO.etiqueta), s(stats.pendientes, ESTILO.entero)],
      [s(t('perfil.kpiFeatured'), ESTILO.etiqueta), s(info.destacadas, ESTILO.entero)],
      [],
      [s(t('perfil.performanceTitle'), ESTILO.seccion)],
      [s(t('perfil.funnelViews'), ESTILO.etiqueta), s(e.vistas, ESTILO.entero)],
      [s(t('perfil.funnelFavorites'), ESTILO.etiqueta), s(e.favoritos, ESTILO.entero), s(e.vistas ? e.favoritos / e.vistas : 0, ESTILO.pct)],
      [s(t('perfil.funnelContacts'), ESTILO.etiqueta), s(e.contactos, ESTILO.entero), s(e.vistas ? e.contactos / e.vistas : 0, ESTILO.pct)],
      [],
      [s(t('perfil.xlsHighlights'), ESTILO.seccion)],
      [s(t('perfil.reportTopViewed'), ESTILO.etiqueta), s(res.masVista ? titulo(res.masVista) : '—', ESTILO.texto)],
      [s(t('perfil.reportTopContacted'), ESTILO.etiqueta), s(res.masContactada ? titulo(res.masContactada) : '—', ESTILO.texto)],
      [s(t('perfil.reportNoViews'), ESTILO.etiqueta), s(res.sinVistas, ESTILO.entero)],
      [s(t('perfil.reportBestDay'), ESTILO.etiqueta), s(DIAS[dia] ? t(DIAS[dia]) : '—', ESTILO.texto)],
      [s(t('perfil.reportBestHour'), ESTILO.etiqueta), s(hora >= 0 ? rangoHora(hora) : '—', ESTILO.texto)],
    ],
  }

  // 2. Publicaciones
  const cab = [
    'perfil.exportColListing', 'perfil.exportColYear', 'perfil.exportColStatus', 'perfil.exportColPrice', 'perfil.exportColCurrency',
    'perfil.xlsMarketAvg', 'perfil.xlsVsMarket', 'perfil.exportColViews', 'perfil.exportColFavorites', 'perfil.xlsSaveRate',
    'perfil.exportColContacts', 'perfil.exportColRate', 'perfil.xlsDaysListed', 'perfil.exportColPublished', 'perfil.xlsFeaturedUntil',
    'perfil.xlsSoldVia', 'perfil.xlsReason',
  ] as Clave[]
  const pubs: Celda[][] = [cab.map((k) => s(t(k), ESTILO.encabezado))]
  for (const r of filas) {
    const c = porId.get(r.id)!
    const m = extra?.mercado.find((x) => x.id === c.id)
    pubs.push([
      s(titulo(c), ESTILO.negrita),
      s(c.year || null, ESTILO.texto),
      s(info.estado(c), ESTILO.texto),
      s(c.priceValue, ESTILO.dinero),
      s(c.currency, ESTILO.texto),
      s(m ? m.promedio : null, ESTILO.dinero),
      s(difMercado(c, extra), ESTILO.pct),
      s(r.vistas, ESTILO.entero),
      s(r.favoritos, ESTILO.entero),
      s(r.vistas ? r.favoritos / r.vistas : 0, ESTILO.pct),
      s(r.contactos, ESTILO.entero),
      s(r.vistas ? r.contactos / r.vistas : 0, ESTILO.pct),
      s(diasDesde(c.publishedAt), ESTILO.entero),
      s(fechaExcel(c.publishedAt), ESTILO.fecha),
      s(c.featured ? fechaExcel(c.featuredUntil) : null, ESTILO.fecha),
      s(c.soldVia === 'dentro_app' ? t('vehiculo.soldVia.inside') : c.soldVia === 'fuera_app' ? t('vehiculo.soldVia.outside') : '', ESTILO.texto),
      s(c.rejectedReason ?? c.disabledReason ?? '', ESTILO.texto),
    ])
  }
  // Las que aún no se publican (en revisión o rechazadas) también, para ver su motivo.
  for (const c of pendientes(cars).filter((c) => !c.approved)) {
    pubs.push([
      s(titulo(c), ESTILO.negrita), s(c.year || null, ESTILO.texto), s(info.estado(c), ESTILO.texto), s(c.priceValue, ESTILO.dinero),
      s(c.currency, ESTILO.texto), s(null, ESTILO.dinero), s(null, ESTILO.pct), s(null, ESTILO.entero), s(null, ESTILO.entero),
      s(null, ESTILO.pct), s(null, ESTILO.entero), s(null, ESTILO.pct), s(null, ESTILO.entero), s(null, ESTILO.fecha), s(null, ESTILO.fecha),
      s('', ESTILO.texto), s(c.rejectedReason ?? c.disabledReason ?? '', ESTILO.texto),
    ])
  }
  const publicaciones: Hoja = {
    nombre: t('perfil.xlsSheetListings'),
    anchos: [30, 8, 14, 14, 9, 16, 12, 9, 11, 11, 11, 12, 10, 13, 14, 16, 40],
    fijas: 1,
    filtro: `A1:${columna(cab.length - 1)}${pubs.length}`,
    filas: pubs,
  }

  // 3. Actividad
  const meses = ultimosMeses(info.locale)
  const act: Celda[][] = [
    [s(t('perfil.xlsMonth'), ESTILO.encabezado), s(t('perfil.exportColViews'), ESTILO.encabezado), null, s(t('perfil.xlsDay'), ESTILO.encabezado), s(t('perfil.exportColViews'), ESTILO.encabezado), null, s(t('perfil.xlsHour'), ESTILO.encabezado), s(t('perfil.exportColViews'), ESTILO.encabezado)],
  ]
  for (let i = 0; i < 24; i++) {
    act.push([
      i < 12 ? s(meses[i], ESTILO.texto) : null,
      i < 12 ? s(stats.vistasPorMes[i] ?? 0, ESTILO.entero) : null,
      null,
      i < 7 ? s(t(DIAS[i]), ESTILO.texto) : null,
      i < 7 ? s(porDia[i] ?? 0, ESTILO.entero) : null,
      null,
      s(rangoHora(i), ESTILO.texto),
      s(porHora[i] ?? 0, ESTILO.entero),
    ])
  }
  const actividad: Hoja = { nombre: t('perfil.xlsSheetActivity'), anchos: [14, 10, 3, 14, 10, 3, 14, 10], fijas: 1, filas: act }

  // 4. Destacados
  const des: Celda[][] = [
    (['perfil.exportColListing', 'perfil.xlsStart', 'perfil.xlsEnd', 'perfil.xlsDays', 'perfil.xlsViewsBefore', 'perfil.xlsViewsDuring', 'perfil.xlsChange'] as Clave[]).map((k) =>
      s(t(k), ESTILO.encabezado),
    ),
  ]
  for (const d of extra?.destacados ?? []) {
    const c = porId.get(d.id)
    if (!c) continue
    const ef = efectoDestacado(d)
    des.push([
      s(titulo(c), ESTILO.negrita),
      s(fechaExcel(d.inicio), ESTILO.fecha),
      s(fechaExcel(d.fin), ESTILO.fecha),
      s(d.dias, ESTILO.entero),
      s(Math.round(ef.antes * 10) / 10, ESTILO.decimal),
      s(Math.round(ef.durante * 10) / 10, ESTILO.decimal),
      s(ef.cambio, ESTILO.pct),
    ])
  }
  if (des.length === 1) des.push([s(t('perfil.xlsNoFeatured'), ESTILO.texto)])
  const destacados: Hoja = { nombre: t('perfil.xlsSheetFeatured'), anchos: [30, 13, 13, 8, 18, 18, 12], fijas: 1, filas: des }

  // 5. Recomendaciones
  const rec: Celda[][] = [[s(t('perfil.exportColListing'), ESTILO.encabezado), s(t('perfil.reportTips'), ESTILO.encabezado)]]
  for (const k of res.consejos) rec.push([s(titulo(k.car), ESTILO.negrita), s(t(TIP[k.tipo]), ESTILO.texto)])
  for (const c of pendientes(cars)) {
    const motivo = c.rejectedReason ?? c.disabledReason
    rec.push([s(titulo(c), ESTILO.negrita), s(`${info.estado(c)}${motivo ? `: ${motivo}` : ''}`, ESTILO.texto)])
  }
  if (rec.length === 1) rec.push([s(t('perfil.xlsAllGood'), ESTILO.texto)])
  const recomendaciones: Hoja = { nombre: t('perfil.reportTips'), anchos: [30, 90], fijas: 1, filas: rec }

  return xlsx([resumen, publicaciones, actividad, destacados, recomendaciones])
}

/** Texto de cada tipo de recomendación. */
export const TIP: Record<Consejo['tipo'], Clave> = {
  sinContacto: 'perfil.reportTipNoContact',
  pocasVistas: 'perfil.reportTipFewViews',
  precioAlto: 'perfil.reportTipHighPrice',
}

/** Descarga [contenido] como archivo en el navegador. */
export function descargar(nombre: string, contenido: BlobPart, tipo: string) {
  const url = URL.createObjectURL(new Blob([contenido], { type: tipo }))
  const a = document.createElement('a')
  a.href = url
  a.download = nombre
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
