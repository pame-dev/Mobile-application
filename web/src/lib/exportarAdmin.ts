import type { AdminStats, ReporteApp } from '../data/admin'
import type { Clave } from '../i18n/es'
import type { T } from '../i18n/I18nContext'
import { DIAS, pico, rangoHora } from './exportarPanel'
import { ESTILO, xlsx, type Celda, type Hoja } from './xlsx'

/**
 * Reporte del panel de administración (Excel y PDF): números de toda la app en el rango
 * elegido. Mismo contenido que la app (ExportarAdmin.kt).
 */

/** Fracción para Excel (0.25 = 25 %); 0 si no hay base. */
export const fraccion = (b: number, a: number) => (a > 0 ? b / a : 0)

const s = (v: string | number | null, estilo: number): Celda => ({ v, s: estilo })

/** Filas "etiqueta | valor (| extra)" del Resumen, agrupadas por sección. */
export function seccionesResumen(r: ReporteApp, t: T): [Clave, [string, number | string, (number | null)?][]][] {
  const e = r.interes
  return [
    [
      'admin.xlsUsers',
      [
        [t('admin.xlsTotal'), r.usuarios.total],
        [t('admin.xlsNewInRange'), r.usuarios.nuevos],
        [t('admin.xlsIndividuals'), r.usuarios.particulares],
        [t('admin.xlsLots'), r.usuarios.lotes],
        [t('admin.xlsSuspended'), r.usuarios.suspendidos],
      ],
    ],
    [
      'admin.xlsListings',
      [
        [t('admin.xlsPublished'), r.publicaciones.publicadas],
        [t('admin.xlsNewInRangeF'), r.publicaciones.nuevas],
        [t('admin.xlsActive'), r.publicaciones.activas],
        [t('admin.xlsPaused'), r.publicaciones.pausadas],
        [t('admin.xlsDisabled'), r.publicaciones.deshabilitadas],
        [t('admin.xlsSold'), r.publicaciones.vendidas],
        [t('admin.xlsSoldInside'), r.publicaciones.vendidasDentro],
        [t('admin.xlsSoldOutside'), r.publicaciones.vendidasFuera],
      ],
    ],
    [
      'admin.xlsInterest',
      [
        [t('admin.views'), e.vistas],
        [t('admin.xlsFavorites'), r.favoritos, fraccion(r.favoritos, e.vistas)],
        [t('admin.contacts'), e.contactos, fraccion(e.contactos, e.vistas)],
      ],
    ],
    [
      'admin.xlsReview',
      [
        [t('admin.xlsPending'), r.revision.pendientes],
        [t('admin.xlsApproved'), r.revision.aprobadas],
        [t('admin.xlsRejected'), r.revision.rechazadas],
        [t('admin.xlsAvgHours'), r.revision.horasPromedio ?? '—'],
      ],
    ],
    [
      'admin.sec.featured',
      [
        [t('admin.xlsPending'), r.destacados.pendientes],
        [t('admin.xlsCurrent'), r.destacados.vigentes],
        [t('admin.xlsApprovedM'), r.destacados.aprobados],
        [t('admin.xlsRejectedM'), r.destacados.rechazados],
      ],
    ],
    [
      'admin.sec.reports',
      [
        [t('admin.xlsPending'), r.reportes.pendientes],
        [t('admin.xlsReceived'), r.reportes.recibidos],
        [t('admin.xlsResolved'), r.reportes.resueltos],
      ],
    ],
    [
      'admin.xlsActivity',
      [
        [t('admin.xlsBestDay'), pico(r.porDia) >= 0 && DIAS[pico(r.porDia)] ? t(DIAS[pico(r.porDia)]) : '—'],
        [t('admin.xlsBestHour'), pico(r.porHora) >= 0 ? rangoHora(pico(r.porHora)) : '—'],
      ],
    ],
  ]
}

/**
 * Libro de Excel del panel de administración con 6 hojas: Resumen, Crecimiento, Más vistas,
 * Marcas y estados, Reportes y Actividad.
 */
export function libroAdmin(stats: AdminStats, r: ReporteApp, info: { fecha: string; dias: number }, t: T): Uint8Array<ArrayBuffer> {
  // 1. Resumen
  const filas: Celda[][] = [
    [s(t('admin.reportTitle'), ESTILO.titulo)],
    [s(t('admin.exportReportSub', { n: info.dias, fecha: info.fecha }), ESTILO.subtitulo)],
  ]
  for (const [titulo, items] of seccionesResumen(r, t)) {
    filas.push([], [s(t(titulo), ESTILO.seccion)])
    for (const [k, v, pctVal] of items) {
      const num = typeof v === 'number'
      filas.push([
        s(k, ESTILO.etiqueta),
        s(v, num ? (Number.isInteger(v) ? ESTILO.entero : ESTILO.decimal) : ESTILO.texto),
        ...(pctVal != null ? [s(pctVal, ESTILO.pct)] : []),
      ])
    }
  }
  const resumen: Hoja = { nombre: t('admin.xlsSheetSummary'), anchos: [34, 16, 12], unir: ['A1:C1', 'A2:C2'], filas }

  // 2. Crecimiento (acumulados) e interés (por periodo), lado a lado
  const c = stats.crecimiento
  const it = stats.interacciones
  const cab = (k: Clave) => s(t(k), ESTILO.encabezado)
  const crec: Celda[][] = [
    [cab('admin.xlsDate'), cab('admin.legendUsers'), cab('admin.legendLots'), cab('admin.legendListings'), null, cab('admin.xlsPeriod'), cab('admin.views'), cab('admin.contacts')],
  ]
  for (let i = 0; i < Math.max(c.etiquetas.length, it.etiquetas.length); i++) {
    crec.push([
      i < c.etiquetas.length ? s(c.etiquetas[i], ESTILO.texto) : null,
      i < c.etiquetas.length ? s(c.usuarios[i] ?? 0, ESTILO.entero) : null,
      i < c.etiquetas.length ? s(c.lotes[i] ?? 0, ESTILO.entero) : null,
      i < c.etiquetas.length ? s(c.publicaciones[i] ?? 0, ESTILO.entero) : null,
      null,
      i < it.etiquetas.length ? s(it.etiquetas[i], ESTILO.texto) : null,
      i < it.etiquetas.length ? s(it.vistas[i] ?? 0, ESTILO.entero) : null,
      i < it.etiquetas.length ? s(it.contactos[i] ?? 0, ESTILO.entero) : null,
    ])
  }
  const crecimiento: Hoja = { nombre: t('admin.xlsSheetGrowth'), anchos: [12, 11, 11, 13, 3, 12, 10, 11], fijas: 1, filas: crec }

  // 3. Más vistas
  const mv: Celda[][] = [[cab('admin.xlsListing'), cab('admin.seller'), cab('admin.views'), cab('admin.contacts'), cab('admin.conversion')]]
  for (const m of r.masVistas) {
    mv.push([s(m.titulo, ESTILO.negrita), s(m.vendedor, ESTILO.texto), s(m.vistas, ESTILO.entero), s(m.contactos, ESTILO.entero), s(fraccion(m.contactos, m.vistas), ESTILO.pct)])
  }
  if (mv.length === 1) mv.push([s(t('admin.xlsNoData'), ESTILO.texto)])
  const masVistas: Hoja = { nombre: t('admin.xlsSheetTop'), anchos: [32, 26, 10, 11, 12], fijas: 1, filas: mv }

  // 4. Marcas y estados (publicaciones activas)
  const me: Celda[][] = [[cab('admin.xlsBrand'), cab('admin.xlsActiveListings'), null, cab('admin.xlsState'), cab('admin.xlsActiveListings')]]
  for (let i = 0; i < Math.max(r.marcas.length, r.estados.length); i++) {
    const m = r.marcas[i]
    const e = r.estados[i]
    me.push([
      m ? s(m.nombre, ESTILO.texto) : null,
      m ? s(m.publicaciones, ESTILO.entero) : null,
      null,
      e ? s(e.nombre, ESTILO.texto) : null,
      e ? s(e.publicaciones, ESTILO.entero) : null,
    ])
  }
  const marcas: Hoja = { nombre: t('admin.xlsSheetBrands'), anchos: [22, 14, 3, 22, 14], fijas: 1, filas: me }

  // 5. Reportes (motivos más comunes del rango)
  const rep: Celda[][] = [[cab('admin.reasonCol'), cab('admin.sec.reports')]]
  for (const m of r.motivos) rep.push([s(m.motivo, ESTILO.texto), s(m.reportes, ESTILO.entero)])
  if (rep.length === 1) rep.push([s(t('admin.xlsNoData'), ESTILO.texto)])
  const reportes: Hoja = { nombre: t('admin.sec.reports'), anchos: [60, 10], fijas: 1, filas: rep }

  // 6. Actividad (vistas por día y por hora)
  const act: Celda[][] = [[cab('admin.xlsDay'), cab('admin.views'), null, cab('admin.xlsHour'), cab('admin.views')]]
  for (let i = 0; i < 24; i++) {
    act.push([
      i < 7 ? s(t(DIAS[i]), ESTILO.texto) : null,
      i < 7 ? s(r.porDia[i] ?? 0, ESTILO.entero) : null,
      null,
      s(rangoHora(i), ESTILO.texto),
      s(r.porHora[i] ?? 0, ESTILO.entero),
    ])
  }
  const actividad: Hoja = { nombre: t('admin.xlsActivity'), anchos: [14, 10, 3, 14, 10], fijas: 1, filas: act }

  return xlsx([resumen, crecimiento, masVistas, marcas, reportes, actividad])
}
