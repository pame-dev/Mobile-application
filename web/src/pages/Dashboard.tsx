import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { CarPhoto } from '../components/CarPhoto'
import { Icon, type IconName } from '../components/Icon'
import { STATUS_BADGE, fechaCorta, statusLabel, useOwnerActions } from '../components/OwnerActions'
import { favoriteCounts, myCars, ocultas, sePuedeOcultar, sellerStats, setOculta, type OwnCar, type SellerStats } from '../data/seller'
import { traducir, useI18n } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { entero, precio } from '../lib/format'
import { useSession } from '../session/SessionContext'

/** Iniciales de los días que manda la BD (domingo a sábado). */
const DIA: Record<string, Clave> = {
  D: 'perfil.dayD',
  L: 'perfil.dayL',
  M: 'perfil.dayM',
  X: 'perfil.dayX',
  J: 'perfil.dayJ',
  V: 'perfil.dayV',
  S: 'perfil.dayS',
}

/** Gráfica de barras del diseño (la última barra resaltada). */
export function BarChart({ vals, labels, w = 460, h = 190 }: { vals: number[]; labels: string[]; w?: number; h?: number }) {
  const { t } = useI18n()
  const max = Math.max(5, Math.ceil(Math.max(0, ...vals) / 5) * 5)
  const pl = 30
  const pb = 24
  const bw = (w - pl) / Math.max(1, vals.length)
  return (
    <svg className="chart" viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label={t('perfil.chart')}>
      <g className="grid">
        {[0, 0.5, 1].map((f) => {
          const y = h - pb - (h - pb - 10) * f
          return (
            <g key={f}>
              <line x1={pl} x2={w} y1={y} y2={y} />
              <text x={0} y={y + 4}>
                {Math.round(max * f)}
              </text>
            </g>
          )
        })}
      </g>
      {vals.map((v, i) => {
        const bh = ((h - pb - 10) * v) / max
        const x = pl + i * bw + bw * 0.22
        const ultima = i === vals.length - 1
        return (
          <g key={i}>
            <rect x={x} y={h - pb - bh} width={bw * 0.56} height={bh} rx={6} fill={ultima ? 'var(--teal)' : 'var(--navy)'} opacity={ultima ? 1 : 0.85} />
            <text x={x + bw * 0.28} y={h - 6} textAnchor="middle">
              {labels[i]}
            </text>
            <text x={x + bw * 0.28} y={h - pb - bh - 6} textAnchor="middle" style={{ fill: 'var(--ink)', fontWeight: 600 }}>
              {v}
            </text>
          </g>
        )
      })}
    </svg>
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
        {c.status === 'Rechazado' && (
          <button className="btn btn-primary btn-sm" onClick={() => acciones.editar(c)}>
            {t('perfil.fixResubmit')}
          </button>
        )}
        {c.status === 'Activo' &&
          (c.featuredPending ? (
            <button className="btn btn-ghost btn-sm" disabled>
              {t('perfil.requestSent')}
            </button>
          ) : !c.featured || c.featuredEndsSoon ? (
            <button className="btn btn-soft btn-sm" onClick={() => acciones.destacar(c)}>
              <Icon name="star" size={14} />
              {c.featured ? t('perfil.renewFeatured') : t('perfil.feature')}
            </button>
          ) : null)}
        {(c.status === 'Activo' || c.status === 'Pausado' || c.status === 'Pendiente') && (
          <button className="btn btn-ghost btn-sm" onClick={() => acciones.editar(c)}>
            <Icon name="edit" size={14} />
            {t('perfil.edit')}
          </button>
        )}
        {c.status === 'Activo' && (
          <>
            <button className="btn btn-ghost btn-sm" onClick={() => acciones.vendido(c)}>
              <Icon name="sold" size={14} />
              {t('perfil.markSold')}
            </button>
            <button className="btn btn-ghost btn-sm" onClick={() => acciones.pausar(c)}>
              <Icon name="pause" size={14} />
              {t('perfil.disable')}
            </button>
          </>
        )}
        {c.status === 'Pausado' && (
          <button className="btn btn-primary btn-sm" onClick={() => acciones.habilitar(c)}>
            {t('perfil.enable')}
          </button>
        )}
        {c.status === 'Vendido' && (
          <button className="btn btn-ghost btn-sm" onClick={() => acciones.disponible(c)}>
            {t('perfil.markAvailable')}
          </button>
        )}
        {sePuedeOcultar(c) && (
          <button className="btn btn-ghost btn-sm" onClick={() => onOcultar(!oculta)}>
            <Icon name="eye" size={14} />
            {oculta ? t('perfil.showOnProfile') : t('perfil.hide')}
          </button>
        )}
      </div>
    </div>
  )
}

/** Mi panel: métricas, interés de la semana y publicaciones propias (DashboardScreen de la app). */
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
    setOcultasIds(ocultas(account.id))
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

  const acciones = useOwnerActions(cargar)

  if (!cargandoSesion && !account) return <Navigate to="/login" replace state={{ from: '/panel' }} />

  const tendencia = (() => {
    if (!stats) return ''
    if (stats.vistasMesAnterior === 0) return t('perfil.trendMonth', { n: entero(stats.vistasMes) })
    const pct = Math.round(((stats.vistasMes - stats.vistasMesAnterior) * 100) / stats.vistasMesAnterior)
    return pct >= 0 ? t('perfil.trendUp', { n: pct }) : t('perfil.trendDown', { n: pct })
  })()
  const favSemana = stats?.favoritosSemana.reduce((a, b) => a + b, 0) ?? 0
  const kpis: [string, string, string, IconName, string, string][] = [
    [t('perfil.kpiViews'), stats ? entero(stats.vistas) : '—', tendencia, 'eye', 'var(--teal-light)', 'var(--teal)'],
    [t('perfil.kpiListed'), stats ? entero(stats.publicadas) : '—', stats ? t('perfil.kpiListedSub', { activas: stats.activas, vendidas: stats.vendidas }) : '', 'car', 'var(--pill)', 'var(--ink)'],
    [t('perfil.kpiInquiries'), stats ? entero(stats.contactos) : '—', t('perfil.kpiContacts'), 'chat', 'var(--success-l)', 'var(--success)'],
    [t('nav.favorites'), stats ? entero(favSemana) : '—', t('perfil.kpiThisWeek'), 'heart', 'var(--error-l)', 'var(--error)'],
  ]
  const visibles = (cars ?? []).filter((c) => !(sePuedeOcultar(c) && ocultasIds.has(c.id)))
  const escondidas = (cars ?? []).filter((c) => sePuedeOcultar(c) && ocultasIds.has(c.id))
  const ocultar = (id: number, v: boolean) => {
    if (!account) return
    setOculta(account.id, id, v)
    setOcultasIds(ocultas(account.id))
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
          <h3 style={{ fontSize: 17 }}>{t('perfil.weekInterest')}</h3>
          <p className="muted" style={{ fontSize: 13, marginBottom: 12 }}>
            {t(favSemana === 1 ? 'perfil.weekSubOne' : 'perfil.weekSubMany', { n: favSemana })}
          </p>
          {stats ? <BarChart vals={stats.favoritosSemana} labels={stats.diasSemana.map((d) => (DIA[d] ? t(DIA[d]) : d))} /> : <div className="skeleton" style={{ height: 190 }} />}
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
