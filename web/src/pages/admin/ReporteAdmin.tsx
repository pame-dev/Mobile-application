import { useState } from 'react'
import { createPortal, flushSync } from 'react-dom'
import { Icon } from '../../components/Icon'
import { ModalHead, useUi } from '../../components/Ui'
import { adminReport, adminStats, type AdminStats, type ReporteApp } from '../../data/admin'
import { mensajeUsuario } from '../../data/auth'
import { traducir, useI18n } from '../../i18n/I18nContext'
import { fraccion, libroAdmin, seccionesResumen } from '../../lib/exportarAdmin'
import { DIAS, descargar } from '../../lib/exportarPanel'
import { entero } from '../../lib/format'

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
  const maxDia = Math.max(1, ...r.porDia)
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

      <section className="rbloque">
        <h2>{t('admin.growthTitle')}</h2>
        <table>
          <thead>
            <tr>
              <th>{t('admin.xlsDate')}</th>
              {c.etiquetas.map((e) => (
                <th key={e}>{e}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {(
              [
                [t('admin.legendUsers'), c.usuarios],
                [t('admin.legendLots'), c.lotes],
                [t('admin.legendListings'), c.publicaciones],
              ] as [string, number[]][]
            ).map(([k, v]) => (
              <tr key={k}>
                <td>
                  <b className="rauto">{k}</b>
                </td>
                {v.map((x, i) => (
                  <td key={i}>{entero(x)}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
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
            <span>{t('admin.reportByDay')}</span>
            <div className="rmeses" style={{ gridTemplateColumns: 'repeat(7, 1fr)' }}>
              {r.porDia.map((v, i) => (
                <div key={i}>
                  <i style={{ height: `${Math.max(2, (v * 100) / maxDia)}%` }} />
                  <small>{t(DIAS[i]).slice(0, 3)}</small>
                </div>
              ))}
            </div>
          </div>
          <div>
            {seccionesResumen(r, t)
              .find(([k]) => k === 'admin.xlsActivity')![1]
              .map(([k, v]) => (
                <div key={k} style={{ border: 0, padding: 0 }}>
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
    const fin = () => {
      document.body.classList.remove('imprimiendo')
      setImprimir(false)
    }
    window.addEventListener('afterprint', fin, { once: true })
    window.print()
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
