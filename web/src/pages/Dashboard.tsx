import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { CarPhoto } from '../components/CarPhoto'
import { Icon, type IconName } from '../components/Icon'
import { OwnerButtons, STATUS_BADGE, fechaCorta, statusLabel, useOwnerActions } from '../components/OwnerActions'
import { favoriteCounts, myCars, ocultas, sePuedeOcultar, sellerStats, setOculta, type OwnCar, type SellerStats } from '../data/seller'
import { traducir, useI18n } from '../i18n/I18nContext'
import { entero, precio } from '../lib/format'
import { useSession } from '../session/SessionContext'
import { useRecargar } from '../lib/tiempoReal'

/** % de b sobre a, sin decimales y hasta 100 (se puede guardar en favoritos sin abrir el detalle). */
const pct = (b: number, a: number) => (a > 0 ? Math.min(100, Math.round((b * 100) / a)) : 0)

/**
 * "Cómo van tus publicaciones" (totales): arriba el embudo vistas → favoritos →
 * contactos de todo el perfil; abajo una barra por publicación, la más vista primero.
 */
function Rendimiento({ stats, cars }: { stats: SellerStats; cars: OwnCar[] }) {
  const navigate = useNavigate()
  const { t } = useI18n()
  const [todas, setTodas] = useState(false)
  // Una fila por cada publicación ya publicada (aunque aún no tenga vistas); los números
  // vienen de la BD por id y, si no hay, van en 0.
  const datos = new Map(stats.rendimiento.map((r) => [r.id, r]))
  const porId = new Map(cars.map((c) => [c.id, c]))
  const filas = cars
    .filter((c) => c.approved)
    .map((c) => datos.get(c.id) ?? { id: c.id, vistas: 0, favoritos: 0, contactos: 0 })
    .sort((a, b) => b.vistas - a.vistas || b.contactos - a.contactos)
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

/** Fila de una publicación propia con su estado y lo que se puede hacer con ella. */
function ListingRow({ c, favs, acciones, oculta, onOcultar }: { c: OwnCar; favs: number; acciones: ReturnType<typeof useOwnerActions>; oculta: boolean; onOcultar: (v: boolean) => void }) {
  const navigate = useNavigate()
  const { t } = useI18n()
  const motivo = c.status === 'Rechazado' && c.rejectedReason ? c.rejectedReason : c.status === 'Deshabilitado' && c.disabledReason ? c.disabledReason : null
  const nota = motivo
    ? t('perfil.reason', { motivo })
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
          <button className="iconbtn" aria-label={t('perfil.backToProfile')} onClick={() => navigate('/perfil')}>
            <Icon name="back" size={18} />
          </button>
          <h1>{t('perfil.dashboardTitle')}</h1>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/publicar')}>
          <Icon name="plus" size={18} />
          {t('nav.publish')}
        </button>
      </div>

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
