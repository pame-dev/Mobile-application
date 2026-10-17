import { useState } from 'react'
import { createPortal, flushSync } from 'react-dom'
import { Icon } from '../../components/Icon'
import { ModalHead, useUi } from '../../components/Ui'
import { adminReport, adminStats, type AdminStats, type ReporteApp } from '../../data/admin'
import { mensajeUsuario } from '../../data/auth'
import { traducir, useI18n } from '../../i18n/I18nContext'
import { fraccion, libroAdmin, seccionesResumen } from '../../lib/exportarAdmin'
import { descargar } from '../../lib/exportarPanel'
import { entero } from '../../lib/format'
import { LineChart, PairBars } from './AdminParts'

/** Elegir formato: Excel (.xlsx) para trabajar los números o PDF para presentarlos. */
function ExportarModal({ onExcel, onPdf }: { onExcel: () => void; onPdf: () => void }) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  const elegir = (f: () => void) => {
    closeModal()
    f()
  }
  return (
    <div className="modal">
      <ModalHead title={t('admin.exportTitle')} sub={t('admin.exportSub')} />
      <div className="col" style={{ gap: 10, marginTop: 16 }}>
        <button className="btn btn-ghost btn-block" onClick={() => elegir(onExcel)}>
          <Icon name="doc" size={18} />
          {t('admin.exportExcel')}
        </button>
        <button className="btn btn-primary btn-block" onClick={() => elegir(onPdf)}>
          <Icon name="upload" size={18} />
          {t('admin.exportPdf')}
        </button>
      </div>
    </div>
  )
}

const pctTexto = (b: number, a: number) => `${Math.round(fraccion(b, a) * 100)}%`

/**
 * Reporte para imprimir o "Guardar como PDF". Solo se dibuja mientras se imprime y, en
 * ese momento, lo demás de la página se oculta (usa los estilos de .reportepanel).
 */
function ReportePdf({ stats, r, dias }: { stats: AdminStats; r: ReporteApp; dias: number }) {
  const { t } = useI18n()
  const locale = traducir('vehiculo.locale')
  const fecha = new Date().toLocaleDateString(locale, { day: 'numeric', month: 'long', year: 'numeric' })
  const kpis: [string, number, string][] = [
    [t('admin.xlsUsers'), r.usuarios.total, t('admin.reportNew', { n: entero(r.usuarios.nuevos) })],
    [t('admin.xlsActive'), r.publicaciones.activas, t('admin.reportNew', { n: entero(r.publicaciones.nuevas) })],
    [t('admin.views'), r.interes.vistas, t('admin.reportInRange', { n: dias })],
    [t('admin.contacts'), r.interes.contactos, pctTexto(r.interes.contactos, r.interes.vistas)],
  ]
  const maxFecha = Math.max(1, ...r.porFecha.map((f) => f.vistas))
  // Con muchas barras (30 días) no caben todas las fechas: se muestra una de cada tantas,
  // contando desde la última para que hoy siempre tenga su fecha.
  const cadaCuantas = Math.ceil(r.porFecha.length / 8)
  const maxVistas = Math.max(1, ...r.masVistas.map((m) => m.vistas))
  const c = stats.crecimiento
  return createPortal(
    <div className="reportepanel">
      <header>
        <img src="/logo.png" alt="Karsy" />
        <div className="grow">
          <h1>{t('admin.reportTitle')}</h1>
          <p>{t('admin.exportReportSub', { n: dias, fecha })}</p>
        </div>
        <b className="rmarca">Karsy</b>
      </header>
      <div className="rkpis">
        {kpis.map(([k, n, sub]) => (
          <div key={k}>
            <span>{k}</span>
            <b>{entero(n)}</b>
            <small>{sub}</small>
          </div>
        ))}
      </div>

      <div className="radmin">
        {seccionesResumen(r, t)
          .filter(([k]) => k !== 'admin.xlsActivity')
          .map(([titulo, items]) => (
            <div key={titulo}>
              <h3>{t(titulo)}</h3>
              {items.map(([k, v, p]) => (
                <p key={k}>
                  <span>{k}</span>
                  <b>
                    {typeof v === 'number' ? entero(v) : v}
                    {p != null && <small> ({Math.round(p * 100)}%)</small>}
                  </b>
                </p>
              ))}
            </div>
          ))}
      </div>

      {/* Las dos gráficas del panel, con el mismo rango (7, 30 o 90 días). */}
      <section className="rbloque">
        <h2>{t('admin.growthTitle')}</h2>
        <p className="rsub">{t('admin.growthSub')}</p>
        <div className="legend">
          <span>
            <i style={{ background: '#3B82F6' }} />
            {t('admin.legendUsers')}
          </span>
          <span>
            <i style={{ background: '#8B5CF6' }} />
            {t('admin.legendLots')}
          </span>
          <span>
            <i style={{ background: '#22C55E' }} />
            {t('admin.legendListings')}
          </span>
        </div>
        <LineChart
          labels={c.etiquetas}
          h={180}
          series={[
            ['#3B82F6', c.usuarios],
            ['#8B5CF6', c.lotes],
            ['#22C55E', c.publicaciones],
          ]}
        />
      </section>

      {/* Separada de Crecimiento para que las dos gráficas no se vean pegadas. */}
      <section className="rbloque" style={{ marginTop: 28 }}>
        <h2>{t('admin.interestTitle')}</h2>
        <p className="rsub">{t('admin.interestSub')}</p>
        <div className="legend">
          <span>
            <i style={{ background: '#3B82F6' }} />
            {t('admin.views')}
          </span>
          <span>
            <i style={{ background: 'var(--teal)' }} />
            {t('admin.contacts')}
          </span>
        </div>
        <PairBars labels={stats.interacciones.etiquetas} a={stats.interacciones.vistas} b={stats.interacciones.contactos} h={180} />
      </section>

      {r.masVistas.length > 0 && (
        <section className="rbloque">
          <h2>{t('admin.xlsSheetTop')}</h2>
          <table>
            <thead>
              <tr>
                <th>{t('admin.xlsListing')}</th>
                <th>{t('admin.seller')}</th>
                <th>{t('admin.views')}</th>
                <th>{t('admin.contacts')}</th>
                <th>{t('admin.conversion')}</th>
              </tr>
            </thead>
            <tbody>
              {r.masVistas.map((m) => (
                <tr key={m.id}>
                  <td>
                    <b className="rauto">{m.titulo}</b>
                    <span className="rbar">
                      <span style={{ width: `${Math.max(2, (m.vistas * 100) / maxVistas)}%` }} />
                    </span>
                  </td>
                  <td>{m.vendedor}</td>
                  <td>{entero(m.vistas)}</td>
                  <td>{entero(m.contactos)}</td>
                  <td>{pctTexto(m.contactos, m.vistas)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      <section className="rbloque">
        <h2>{t('admin.xlsActivity')}</h2>
        <div className="ractividad">
          <div>
            <span>{t('admin.reportByDate')}</span>
            <div className="rmeses" style={{ gridTemplateColumns: `repeat(${Math.max(1, r.porFecha.length)}, 1fr)`, gap: 2 }}>
              {r.porFecha.map((f, i) => (
                <div key={f.etiqueta + i} title={`${f.etiqueta}: ${f.vistas}`}>
                  <i style={{ height: `${Math.max(2, (f.vistas * 100) / maxFecha)}%` }} />
                  <small>{(r.porFecha.length - 1 - i) % cadaCuantas === 0 ? f.etiqueta : ''}</small>
                </div>
              ))}
            </div>
          </div>
          <div>
            {seccionesResumen(r, t)
              .find(([k]) => k === 'admin.xlsActivity')![1]
              .map(([k, v]) => (
                <div key={k} style={{ border: 0, padding: 0, display: 'flex', flexDirection: 'column' }}>
                  <span>{k}</span>
                  <b>{v}</b>
                </div>
              ))}
          </div>
        </div>
      </section>

      <div className="radmin rdos">
        <div>
          <h3>{t('admin.xlsTopBrands')}</h3>
          {r.marcas.length === 0 && <p className="rsub">{t('admin.xlsNoData')}</p>}
          {r.marcas.map((m) => (
            <p key={m.nombre}>
              <span>{m.nombre}</span>
              <b>{entero(m.publicaciones)}</b>
            </p>
          ))}
        </div>
        <div>
          <h3>{t('admin.xlsTopStates')}</h3>
          {r.estados.length === 0 && <p className="rsub">{t('admin.xlsNoData')}</p>}
          {r.estados.map((m) => (
            <p key={m.nombre}>
              <span>{m.nombre}</span>
              <b>{entero(m.publicaciones)}</b>
            </p>
          ))}
        </div>
      </div>

      {r.motivos.length > 0 && (
        <section className="rbloque">
          <h2>{t('admin.reportReasons')}</h2>
          <ul>
            {r.motivos.map((m) => (
              <li key={m.motivo}>
                <b>{entero(m.reportes)}</b> · {m.motivo}
              </li>
            ))}
          </ul>
        </section>
      )}
      <footer>{t('admin.reportFooter')}</footer>
    </div>,
    document.body,
  )
}

/**
 * Botón "Exportar" del Inicio del panel de administración: Excel (.xlsx) o PDF con los
 * números de toda la app en el rango elegido (7, 30 o 90 días).
 */
export function ExportarAdmin({ stats, dias }: { stats: AdminStats | null; dias: number }) {
  const { openModal, toast } = useUi()
  const { t } = useI18n()
  const [reporte, setReporte] = useState<{ r: ReporteApp; stats: AdminStats } | null>(null)
  const [imprimir, setImprimir] = useState(false)
  const [cargando, setCargando] = useState(false)

  const cargar = async () => {
    setCargando(true)
    try {
      // Todo del mismo rango: las estadísticas en pantalla pueden ser del rango anterior.
      const [r, st] = await Promise.all([adminReport(dias), adminStats(dias)])
      return { r, stats: st }
    } catch (e) {
      toast(mensajeUsuario(e))
      return null
    } finally {
      setCargando(false)
    }
  }
  const exportarExcel = async () => {
    const datos = await cargar()
    if (!datos) return
    const hoy = new Date()
    const libro = libroAdmin(datos.stats, datos.r, { fecha: hoy.toLocaleDateString(traducir('vehiculo.locale')), dias }, t)
    descargar(`karsy-admin-${hoy.toISOString().slice(0, 10)}.xlsx`, libro, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
  }
  const exportarPdf = async () => {
    const datos = await cargar()
    if (!datos) return
    flushSync(() => {
      setReporte(datos)
      setImprimir(true)
    })
    // Se espera a que cargue el logo (máx. 4 s); si no, sale un cuadro vacío.
    const imagenes = [...document.querySelectorAll<HTMLImageElement>('.reportepanel img')]
    await Promise.race([
      Promise.all(imagenes.map((img) => (img.complete ? null : new Promise((ok) => ((img.onload = ok), (img.onerror = ok)))))),
      new Promise((ok) => setTimeout(ok, 4000)),
    ])
    document.body.classList.add('imprimiendo')
    // En el celular window.print() no espera a que se cierre la vista previa y "afterprint" llega
    // antes de que se genere el PDF (salía la pantalla en vez del reporte). Por eso se limpia hasta
    // el siguiente toque o tecla; mientras tanto el reporte sigue oculto en pantalla.
    const fin = () => {
      window.removeEventListener('pointerdown', fin)
      window.removeEventListener('keydown', fin)
      document.body.classList.remove('imprimiendo')
      setImprimir(false)
    }
    window.print()
    window.addEventListener('pointerdown', fin)
    window.addEventListener('keydown', fin)
  }

  return (
    <>
      <button
        className="btn btn-ghost"
        disabled={!stats || cargando}
        onClick={() => openModal(<ExportarModal onExcel={() => void exportarExcel()} onPdf={() => void exportarPdf()} />)}
      >
        <Icon name="doc" size={18} />
        {cargando ? t('admin.exporting') : t('admin.export')}
      </button>
      {imprimir && reporte && <ReportePdf stats={reporte.stats} r={reporte.r} dias={dias} />}
    </>
  )
}
