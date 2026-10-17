import { useCallback, useEffect, useState } from 'react'
import { createPortal, flushSync } from 'react-dom'
import { VolverButton } from '../components/VolverButton'
import { Navigate, useNavigate } from 'react-router-dom'
import { CarPhoto } from '../components/CarPhoto'
import { Icon, type IconName } from '../components/Icon'
import { OwnerButtons, STATUS_BADGE, fechaCorta, statusLabel, useOwnerActions } from '../components/OwnerActions'
import { favoriteCounts, myCars, ocultas, sePuedeOcultar, sellerReport, sellerStats, setOculta, type OwnCar, type ReporteExtra, type SellerStats } from '../data/seller'
import { traducir, useI18n } from '../i18n/I18nContext'
import { entero, precio } from '../lib/format'
import { useSession } from '../session/SessionContext'
import { useRecargar } from '../lib/tiempoReal'
import { ModalHead, useUi } from '../components/Ui'
import { Traducido, useTraduccion } from '../components/Traducido'
import { DIAS, TIP, descargar, diasDesde, difMercado, efectoDestacado, filasRendimiento, libroPanel, pct, pendientes, pico, rangoHora, resumenReporte, ultimosMeses, vendidos } from '../lib/exportarPanel'

/**
 * "Cómo van tus publicaciones" (totales): arriba el embudo vistas → favoritos →
 * contactos de todo el perfil; abajo una barra por publicación, la más vista primero.
 */
function Rendimiento({ stats, cars }: { stats: SellerStats; cars: OwnCar[] }) {
  const navigate = useNavigate()
  const { t } = useI18n()
  const [todas, setTodas] = useState(false)
  const porId = new Map(cars.map((c) => [c.id, c]))
  const filas = filasRendimiento(stats, cars)
  const visibles = todas ? filas : filas.slice(0, 6)
  const max = Math.max(1, ...filas.map((r) => r.vistas))
  const e = stats.embudo
  const pasos: [string, number, string, string][] = [
    [t('perfil.funnelViews'), e.vistas, '', 'var(--teal)'],
    [t('perfil.funnelFavorites'), e.favoritos, t('perfil.funnelOfViews', { n: pct(e.favoritos, e.vistas) }), 'var(--error)'],
    [t('perfil.funnelContacts'), e.contactos, t('perfil.funnelOfViews', { n: pct(e.contactos, e.vistas) }), 'var(--success)'],
  ]
  return (
    <>
      <div className="funnel">
        {pasos.map(([k, n, sub, color], i) => (
          <div key={k} className="funnelstep" style={{ width: `${100 - i * 12}%` }}>
            <span className="funnelbar" style={{ background: color }} />
            <span className="grow">{k}</span>
            <b className="num">{entero(n)}</b>
            {sub && <span className="muted funnelsub">{sub}</span>}
          </div>
        ))}
      </div>

      {filas.length === 0 ? (
        <p className="muted" style={{ marginTop: 16 }}>
          {t('perfil.performanceEmpty')}
        </p>
      ) : (
        <div className="perflist">
          {visibles.map((r) => {
            const c = porId.get(r.id)!
            return (
              <button key={r.id} className="perfrow" onClick={() => navigate(`/vehiculo/${r.id}`)}>
                <span className="perftitle">
                  {c.brand} {c.model} {c.year || ''}
                </span>
                <span className="perftrack" aria-hidden>
                  <span style={{ width: `${Math.max(2, (r.vistas * 100) / max)}%` }} />
                </span>
                <span className="muted perfnums">{t('perfil.performanceRow', { v: entero(r.vistas), f: entero(r.favoritos), c: entero(r.contactos) })}</span>
              </button>
            )
          })}
          {filas.length > 6 && (
            <button className="btn btn-ghost btn-sm" style={{ alignSelf: 'flex-start' }} onClick={() => setTodas((v) => !v)}>
              {todas ? t('perfil.performanceLess') : t('perfil.performanceAll', { n: filas.length })}
            </button>
          )}
        </div>
      )}
    </>
  )
}

/** Elegir formato: Excel (.xlsx) para trabajar los números o PDF para presentarlos. */
function ExportarModal({ onCsv, onPdf }: { onCsv: () => void; onPdf: () => void }) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  const elegir = (f: () => void) => {
    closeModal()
    f()
  }
  return (
    <div className="modal">
      <ModalHead title={t('perfil.exportTitle')} sub={t('perfil.exportSub')} />
      <div className="col" style={{ gap: 10, marginTop: 16 }}>
        <button className="btn btn-ghost btn-block" onClick={() => elegir(onCsv)}>
          <Icon name="doc" size={18} />
          {t('perfil.exportCsv')}
        </button>
        <button className="btn btn-primary btn-block" onClick={() => elegir(onPdf)}>
          <Icon name="upload" size={18} />
          {t('perfil.exportPdf')}
        </button>
      </div>
    </div>
  )
}

/**
 * Reporte para imprimir o "Guardar como PDF". Solo se dibuja mientras se imprime y, en
 * ese momento, lo demás de la página se oculta (ver .reportepanel en styles.css).
 */
function ReportePanel({ nombre, kpis, stats, cars, extra }: { nombre: string; kpis: [string, string, string][]; stats: SellerStats; cars: OwnCar[]; extra: ReporteExtra | null }) {
  const { t } = useI18n()
  const locale = traducir('vehiculo.locale')
  const porId = new Map(cars.map((c) => [c.id, c]))
  const filas = filasRendimiento(stats, cars)
  const max = Math.max(1, ...filas.map((r) => r.vistas))
  const e = stats.embudo
  const res = resumenReporte(stats, cars, Date.now(), extra)
  const ven = vendidos(cars)
  const atencion = pendientes(cars)
  const titulo = (c: OwnCar) => `${c.brand} ${c.model} ${c.year || ''}`.trim()
  const corta = (iso: string) => new Date(iso).toLocaleDateString(locale, { day: 'numeric', month: 'short' })
  const porDia = extra?.porDia ?? []
  const porHora = extra?.porHora ?? []
  const dia = pico(porDia)
  const hora = pico(porHora)
  const meses = ultimosMeses(locale)
  const maxMes = Math.max(1, ...stats.vistasPorMes)
  const destacados: [string, string][] = [
    [t('perfil.reportTopViewed'), res.masVista ? titulo(res.masVista) : '—'],
    [t('perfil.reportTopContacted'), res.masContactada ? titulo(res.masContactada) : '—'],
    // Solo si algún auto no tiene vistas: es un aviso para revisarlo.
    ...(res.sinVistas > 0 ? [[t('perfil.reportNoViews'), entero(res.sinVistas)] as [string, string]] : []),
  ]
  const conDestacado = (extra?.destacados ?? []).filter((d) => porId.has(d.id))
  return createPortal(
    <div className="reportepanel">
      <header>
        <img src="/logo.png" alt="Karsy" />
        <div className="grow">
          <h1>{t('perfil.reportTitle')}</h1>
          <p>
            {nombre} · {new Date().toLocaleDateString(locale, { day: 'numeric', month: 'long', year: 'numeric' })}
          </p>
        </div>
        <b className="rmarca">Karsy</b>
      </header>
      <div className="rkpis">
        {kpis.map(([k, n, sub]) => (
          <div key={k}>
            <span>{k}</span>
            <b>{n}</b>
            <small>{sub}</small>
          </div>
        ))}
      </div>
      {ven.total > 0 && <p className="rnota">{t('perfil.xlsSoldSplit', { dentro: ven.dentro, fuera: ven.fuera })}</p>}
      <h2>{t('perfil.performanceTitle')}</h2>
      <p className="rsub">{t('perfil.performanceSub')}</p>
      <div className="rembudo">
        <div>
          <span>{t('perfil.funnelViews')}</span>
          <b>{entero(e.vistas)}</b>
        </div>
        <div>
          <span>{t('perfil.funnelFavorites')}</span>
          <b>{entero(e.favoritos)}</b>
          <small>{t('perfil.funnelOfViews', { n: pct(e.favoritos, e.vistas) })}</small>
        </div>
        <div>
          <span>{t('perfil.funnelContacts')}</span>
          <b>{entero(e.contactos)}</b>
          <small>{t('perfil.funnelOfViews', { n: pct(e.contactos, e.vistas) })}</small>
        </div>
      </div>
      <div className="rdestacados" style={{ gridTemplateColumns: `repeat(${destacados.length}, 1fr)` }}>
        {destacados.map(([k, v]) => (
          <div key={k}>
            <span>{k}</span>
            <b>{v}</b>
          </div>
        ))}
      </div>
      <table>
        <thead>
          <tr>
            <th colSpan={2}>{t('perfil.exportColListing')}</th>
            <th>{t('perfil.exportColStatus')}</th>
            <th>{t('perfil.exportColViews')}</th>
            <th>{t('perfil.exportColFavorites')}</th>
            <th>{t('perfil.exportColContacts')}</th>
            <th>{t('perfil.exportColRate')}</th>
          </tr>
        </thead>
        <tbody>
          {filas.map((r) => {
            const c = porId.get(r.id)!
            const dif = difMercado(c, extra)
            const dias = diasDesde(c.publishedAt)
            return (
              <tr key={r.id}>
                <td className="rfoto">{c.imageUrl ? <img src={c.imageUrl} alt="" /> : <span />}</td>
                <td>
                  <b className="rauto">{titulo(c)}</b>
                  <small>
                    {precio(c.priceValue)} {c.currency} · {t(dias === 1 ? 'perfil.reportDaysOne' : 'perfil.reportDays', { n: dias })}
                    {c.featured && c.featuredUntil ? ` · ${t('perfil.reportFeaturedUntil', { fecha: corta(c.featuredUntil) })}` : ''}
                  </small>
                  {dif !== null && (
                    <small className={dif >= 0.1 ? 'ralto' : dif <= -0.1 ? 'rbajo' : ''}>
                      {t('perfil.reportVsMarket', { n: `${dif > 0 ? '+' : ''}${Math.round(dif * 100)}` })}
                    </small>
                  )}
                  <span className="rbar">
                    <span style={{ width: `${Math.max(2, (r.vistas * 100) / max)}%` }} />
                  </span>
                </td>
                <td>{t(statusLabel(c.status))}</td>
                <td>{entero(r.vistas)}</td>
                <td>
                  {entero(r.favoritos)} <small>({pct(r.favoritos, r.vistas)}%)</small>
                </td>
                <td>{entero(r.contactos)}</td>
                <td>{pct(r.contactos, r.vistas)}%</td>
              </tr>
            )
          })}
        </tbody>
      </table>

      <section className="rbloque">
        <h2>{t('perfil.reportActivity')}</h2>
        <div className="ractividad">
          <div>
            <span>{t('perfil.reportByMonth')}</span>
            <div className="rmeses">
              {stats.vistasPorMes.map((v, i) => (
                <div key={i} title={`${meses[i]}: ${v}`}>
                  <i style={{ height: `${Math.max(2, (v * 100) / maxMes)}%` }} />
                  <small>{meses[i].split(' ')[0]}</small>
                </div>
              ))}
            </div>
          </div>
          <div>
            <span>{t('perfil.reportBestDay')}</span>
            <b>{DIAS[dia] ? t(DIAS[dia]) : '—'}</b>
            <span>{t('perfil.reportBestHour')}</span>
            <b>{hora >= 0 ? rangoHora(hora) : '—'}</b>
          </div>
        </div>
      </section>

      {conDestacado.length > 0 && (
        <section className="rbloque">
          <h2>{t('perfil.reportFeaturedEffect')}</h2>
          <ul>
            {conDestacado.map((d) => {
              const ef = efectoDestacado(d)
              return (
                <li key={d.id}>
                  <b>{titulo(porId.get(d.id)!)}:</b>{' '}
                  {t('perfil.reportFeaturedLine', { antes: ef.antes.toFixed(1), durante: ef.durante.toFixed(1) })}
                  {ef.cambio !== null && ` (${ef.cambio >= 0 ? '+' : ''}${Math.round(ef.cambio * 100)}%)`}
                </li>
              )
            })}
          </ul>
        </section>
      )}

      {atencion.length > 0 && (
        <section className="rbloque">
          <h2>{t('perfil.reportAttention')}</h2>
          <ul>
            {atencion.map((c) => {
              const motivo = c.rejectedReason ?? c.disabledReason
              return (
                <li key={c.id}>
                  <b>{titulo(c)}:</b> {t(statusLabel(c.status))}
                  {motivo ? (
                    <>
                      {' — '}
                      <Traducido texto={motivo} />
                    </>
                  ) : (
                    ''
                  )}
                </li>
              )
            })}
          </ul>
        </section>
      )}

      {res.consejos.length > 0 && (
        <section className="rconsejos">
          <h2>{t('perfil.reportTips')}</h2>
          <ul>
            {res.consejos.map((k) => (
              <li key={k.car.id}>
                <b>{titulo(k.car)}:</b> {t(TIP[k.tipo])}
              </li>
            ))}
          </ul>
        </section>
      )}
      <footer>{t('perfil.reportFooter')}</footer>
    </div>,
    document.body,
  )
}

/** Fila de una publicación propia con su estado y lo que se puede hacer con ella. */
function ListingRow({ c, favs, acciones, oculta, onOcultar }: { c: OwnCar; favs: number; acciones: ReturnType<typeof useOwnerActions>; oculta: boolean; onOcultar: (v: boolean) => void }) {
  const navigate = useNavigate()
  const { t } = useI18n()
  const motivo = c.status === 'Rechazado' && c.rejectedReason ? c.rejectedReason : c.status === 'Deshabilitado' && c.disabledReason ? c.disabledReason : null
  // Lo escribe el admin (en español): en inglés se muestra traducido.
  const motivoTraducido = useTraduccion(motivo ?? '')
  const nota = motivo
    ? t('perfil.reason', { motivo: motivoTraducido })
    : c.status === 'Vendido'
      ? c.soldVia === 'fuera_app'
        ? t('perfil.soldOutside')
        : c.soldVia === 'dentro_app'
          ? t('perfil.soldInside')
          : t(statusLabel('Vendido'))
      : c.status === 'Pendiente' || c.inReview
        ? t('perfil.inReview')
        : c.featured && c.featuredUntil
          ? t('perfil.featuredUntil', { fecha: fechaCorta(c.featuredUntil) })
          : c.featured
            ? t('perfil.featured')
            : c.featuredPending
              ? t('perfil.featureRequested')
              : ''
  const peligro = !!motivo

  return (
    <div className="listing">
      <CarPhoto src={c.imageUrl} alt={`${c.brand} ${c.model}`} onClick={() => navigate(`/vehiculo/${c.id}`)} />
      <div style={{ minWidth: 0 }}>
        <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
          <b style={{ color: 'var(--ink)', fontFamily: 'var(--f-display)' }}>
            {c.brand} {c.model} {c.year}
          </b>
          <span className={`badge ${STATUS_BADGE[c.status]}`}>{t(statusLabel(c.status))}</span>
        </div>
        <div className="vprice num" style={{ textAlign: 'left', fontSize: 15 }}>
          {precio(c.priceValue)}
        </div>
        {nota && (
          <div className="muted" style={{ fontSize: 12, color: peligro ? 'var(--error)' : undefined }}>
            {nota}
          </div>
        )}
        {favs > 0 && (
          <div className="mid" style={{ fontSize: 12 }}>
            {t(favs === 1 ? 'perfil.savedByOne' : 'perfil.savedByMany', { n: favs })}
          </div>
        )}
      </div>
      <div className="acts">
        <OwnerButtons c={c} acciones={acciones} oculta={oculta} onOcultar={onOcultar} />
      </div>
    </div>
  )
}

/** Mi panel: métricas, cómo van las publicaciones y publicaciones propias (DashboardScreen de la app). */
export function Dashboard() {
  const navigate = useNavigate()
  const { account, loading: cargandoSesion } = useSession()
  const { openModal } = useUi()
  const [imprimir, setImprimir] = useState(false)
  const [extra, setExtra] = useState<ReporteExtra | null>(null)
  const [stats, setStats] = useState<SellerStats | null>(null)
  const [cars, setCars] = useState<OwnCar[] | null>(null)
  const [favs, setFavs] = useState<Map<number, number>>(new Map())
  const [error, setError] = useState<string | null>(null)
  const [ocultasIds, setOcultasIds] = useState<Set<number>>(new Set())
  const [verOcultas, setVerOcultas] = useState(false)
  const { t } = useI18n()

  const cargar = useCallback(() => {
    if (!account) return
    setError(null)
    ocultas(account.id)
      .then(setOcultasIds)
      .catch(() => undefined)
    sellerStats()
      .then(setStats)
      .catch(() => setError(traducir('perfil.statsError')))
    myCars(account.id)
      .then(setCars)
      .catch(() => setCars((c) => c ?? []))
    favoriteCounts()
      .then(setFavs)
      .catch(() => undefined)
  }, [account])

  useEffect(() => {
    cargar()
  }, [cargar])
  useRecargar(cargar, ['publicaciones', 'propuestas_publicacion', 'fotos_publicacion', 'favoritos', 'solicitudes_destacado', 'publicaciones_ocultas'])

  const acciones = useOwnerActions(cargar)

  if (!cargandoSesion && !account) return <Navigate to="/login" replace state={{ from: '/panel' }} />

  // Arriba, el estado de las publicaciones; vistas, favoritos y contactos van en el embudo.
  // Solo las que se ven en portada: un destacado vigente de un auto vendido o pausado no cuenta.
  const destacadas = (cars ?? []).filter((c) => c.featured && c.status === 'Activo').length
  const kpis: [string, string, string, IconName, string, string][] = [
    [t('perfil.kpiActive'), stats ? entero(stats.activas) : '—', t('perfil.kpiActiveSub'), 'car', 'var(--teal-light)', 'var(--teal)'],
    [t('perfil.kpiSold'), stats ? entero(stats.vendidas) : '—', t('perfil.kpiSoldSub'), 'sold', 'var(--success-l)', 'var(--success)'],
    [t('perfil.kpiInReview'), stats ? entero(stats.pendientes) : '—', t('perfil.kpiInReviewSub'), 'shield', 'var(--pill)', 'var(--ink)'],
    [t('perfil.kpiFeatured'), cars ? entero(destacadas) : '—', t('perfil.kpiFeaturedSub'), 'star', 'var(--b-warn-bg)', 'var(--b-warn)'],
  ]
  // Exportar: Excel (.xlsx) se descarga directo; PDF abre "Imprimir / Guardar como PDF" con el reporte.
  const listo = stats && cars && account
  const exportarExcel = async () => {
    if (!stats || !cars || !account) return
    const hoy = new Date()
    const locale = traducir('vehiculo.locale')
    // Si los datos extra fallan, el Excel sale igual (sin mercado, actividad ni destacados).
    const extra = await sellerReport().catch(() => null)
    const libro = libroPanel(stats, cars, extra, { nombre: account.nombre, fecha: hoy.toLocaleDateString(locale), destacadas, locale, estado: (c) => t(statusLabel(c.status)) }, t)
    descargar(`karsy-mi-panel-${hoy.toISOString().slice(0, 10)}.xlsx`, libro, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
  }
  const exportarPdf = async () => {
    const datos = await sellerReport().catch(() => null)
    flushSync(() => {
      setExtra(datos)
      setImprimir(true)
    })
    // Se espera a que carguen el logo y las fotos (máx. 4 s); si no, salen cuadros vacíos.
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

  const visibles = (cars ?? []).filter((c) => !(sePuedeOcultar(c) && ocultasIds.has(c.id)))
  const escondidas = (cars ?? []).filter((c) => sePuedeOcultar(c) && ocultasIds.has(c.id))
  /** Cambio inmediato en pantalla; si la BD falla se revierte. */
  const ocultar = (id: number, v: boolean) => {
    if (!account) return
    const cambiar = (s: Set<number>, poner: boolean) => {
      const n = new Set(s)
      if (poner) n.add(id)
      else n.delete(id)
      return n
    }
    setOcultasIds((s) => cambiar(s, v))
    setOculta(account.id, id, v).catch(() => setOcultasIds((s) => cambiar(s, !v)))
  }

  return (
    <div className="wrap">
      <div className="pagehead">
        <div className="row">
          <VolverButton to="/perfil" label={t('perfil.backToProfile')} />
          <h1>{t('perfil.dashboardTitle')}</h1>
        </div>
        <div className="row" style={{ gap: 10, flexWrap: 'wrap' }}>
          <button className="btn btn-ghost" disabled={!listo} onClick={() => openModal(<ExportarModal onCsv={() => void exportarExcel()} onPdf={() => void exportarPdf()} />)}>
            <Icon name="doc" size={18} />
            {t('perfil.export')}
          </button>
          <button className="btn btn-primary" onClick={() => navigate('/publicar')}>
            <Icon name="plus" size={18} />
            {t('nav.publish')}
          </button>
        </div>
      </div>
      {imprimir && stats && cars && account && <ReportePanel nombre={account.nombre} kpis={kpis.map(([k, n, sub]) => [k, n, sub])} stats={stats} cars={cars} extra={extra} />}

      {error && (
        <div className="card empty" style={{ marginBottom: 18 }}>
          <p className="muted">{error}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            {t('common.retry')}
          </button>
        </div>
      )}

      <div className="kpis">
        {kpis.map(([titulo, n, s, i, bg, fg]) => (
          <div key={titulo} className="card kpi">
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <span className="label">{titulo}</span>
              <span className="ic" style={{ background: bg, color: fg }}>
                <Icon name={i} />
              </span>
            </div>
            <b className="num">{n}</b>
            <span className="muted" style={{ fontSize: 12.5 }}>
              {s}
            </span>
          </div>
        ))}
      </div>

      <div className="twocol split" style={{ margin: '20px 0 40px' }}>
        <div className="card" style={{ padding: 22 }}>
          <h3 style={{ fontSize: 17 }}>{t('perfil.performanceTitle')}</h3>
          <p className="muted" style={{ fontSize: 13, marginBottom: 14 }}>
            {t('perfil.performanceSub')}
          </p>
          {stats && cars ? <Rendimiento stats={stats} cars={cars} /> : <div className="skeleton" style={{ height: 190 }} />}
        </div>

        <div className="card" style={{ padding: 22 }}>
          <h3 style={{ fontSize: 17, marginBottom: 14 }}>{t('perfil.myVehicles')}</h3>
          {!cars ? (
            <div className="skeleton" style={{ height: 240 }} />
          ) : cars.length === 0 ? (
            <div className="empty">
              <p className="muted">{t('perfil.noVehicles')}</p>
              <button className="btn btn-primary" onClick={() => navigate('/publicar')}>
                {t('perfil.publishFirst')}
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {visibles.map((c) => (
                <ListingRow key={c.id} c={c} favs={favs.get(c.id) ?? 0} acciones={acciones} oculta={false} onOcultar={(v) => ocultar(c.id, v)} />
              ))}
              {escondidas.length > 0 && (
                <>
                  <button className="card row" style={{ padding: '14px 16px', width: '100%', textAlign: 'left', marginTop: 8 }} onClick={() => setVerOcultas((v) => !v)}>
                    <span className="mid">
                      <Icon name="eye" />
                    </span>
                    <span className="grow">
                      <b style={{ color: 'var(--ink)' }}>{t('perfil.hiddenListings', { n: escondidas.length })}</b>
                      <br />
                      <span className="muted" style={{ fontSize: 12.5 }}>
                        {t('perfil.hiddenDescDashboard')}
                      </span>
                    </span>
                    <Icon name="down" />
                  </button>
                  {verOcultas &&
                    escondidas.map((c) => <ListingRow key={c.id} c={c} favs={favs.get(c.id) ?? 0} acciones={acciones} oculta onOcultar={(v) => ocultar(c.id, v)} />)}
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
