import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { CarPhoto } from '../components/CarPhoto'
import { Icon, type IconName } from '../components/Icon'
import { STATUS_BADGE, fechaCorta, useOwnerActions } from '../components/OwnerActions'
import { favoriteCounts, myCars, ocultas, sePuedeOcultar, sellerStats, setOculta, type OwnCar, type SellerStats } from '../data/seller'
import { entero, precio } from '../lib/format'
import { useSession } from '../session/SessionContext'

/** Gráfica de barras del diseño (la última barra resaltada). */
export function BarChart({ vals, labels, w = 460, h = 190 }: { vals: number[]; labels: string[]; w?: number; h?: number }) {
  const max = Math.max(5, Math.ceil(Math.max(0, ...vals) / 5) * 5)
  const pl = 30
  const pb = 24
  const bw = (w - pl) / Math.max(1, vals.length)
  return (
    <svg className="chart" viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label="Gráfica">
      <g className="grid">
        {[0, 0.5, 1].map((t) => {
          const y = h - pb - (h - pb - 10) * t
          return (
            <g key={t}>
              <line x1={pl} x2={w} y1={y} y2={y} />
              <text x={0} y={y + 4}>
                {Math.round(max * t)}
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
  const nota =
    c.status === 'Rechazado' && c.rejectedReason
      ? `Motivo: ${c.rejectedReason}`
      : c.status === 'Deshabilitado' && c.disabledReason
        ? `Motivo: ${c.disabledReason}`
        : c.status === 'Vendido'
          ? c.soldVia === 'fuera_app'
            ? 'Vendido fuera de la app'
            : c.soldVia === 'dentro_app'
              ? 'Vendido dentro de la app'
              : 'Vendido'
          : c.status === 'Pendiente' || c.inReview
            ? 'En revisión'
            : c.featured && c.featuredUntil
              ? `★ Destacado hasta ${fechaCorta(c.featuredUntil)}`
              : c.featured
                ? '★ Destacado'
                : c.featuredPending
                  ? 'Solicitud para destacar enviada'
                  : ''
  const peligro = nota.startsWith('Motivo')

  return (
    <div className="listing">
      <CarPhoto src={c.imageUrl} alt={`${c.brand} ${c.model}`} onClick={() => navigate(`/vehiculo/${c.id}`)} />
      <div style={{ minWidth: 0 }}>
        <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
          <b style={{ color: 'var(--ink)', fontFamily: 'var(--f-display)' }}>
            {c.brand} {c.model} {c.year}
          </b>
          <span className={`badge ${STATUS_BADGE[c.status]}`}>{c.status}</span>
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
            {favs} {favs === 1 ? 'persona lo guardó' : 'personas lo guardaron'} en favoritos
          </div>
        )}
      </div>
      <div className="acts">
        {c.status === 'Rechazado' && (
          <button className="btn btn-primary btn-sm" onClick={() => acciones.editar(c)}>
            Corregir y reenviar
          </button>
        )}
        {c.status === 'Activo' &&
          (c.featuredPending ? (
            <button className="btn btn-ghost btn-sm" disabled>
              Solicitud enviada
            </button>
          ) : !c.featured || c.featuredEndsSoon ? (
            <button className="btn btn-soft btn-sm" onClick={() => acciones.destacar(c)}>
              <Icon name="star" size={14} />
              {c.featured ? 'Renovar destacado' : 'Destacar'}
            </button>
          ) : null)}
        {(c.status === 'Activo' || c.status === 'Pausado' || c.status === 'Pendiente') && (
          <button className="btn btn-ghost btn-sm" onClick={() => acciones.editar(c)}>
            <Icon name="edit" size={14} />
            Editar
          </button>
        )}
        {c.status === 'Activo' && (
          <>
            <button className="btn btn-ghost btn-sm" onClick={() => acciones.vendido(c)}>
              <Icon name="sold" size={14} />
              Marcar como vendido
            </button>
            <button className="btn btn-ghost btn-sm" onClick={() => acciones.pausar(c)}>
              <Icon name="pause" size={14} />
              Deshabilitar
            </button>
          </>
        )}
        {c.status === 'Pausado' && (
          <button className="btn btn-primary btn-sm" onClick={() => acciones.habilitar(c)}>
            Habilitar
          </button>
        )}
        {c.status === 'Vendido' && (
          <button className="btn btn-ghost btn-sm" onClick={() => acciones.disponible(c)}>
            Marcar como disponible
          </button>
        )}
        {sePuedeOcultar(c) && (
          <button className="btn btn-ghost btn-sm" onClick={() => onOcultar(!oculta)}>
            <Icon name="eye" size={14} />
            {oculta ? 'Mostrar en perfil' : 'Ocultar'}
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

  const cargar = useCallback(() => {
    if (!account) return
    setError(null)
    setOcultasIds(ocultas(account.id))
    sellerStats()
      .then(setStats)
      .catch(() => setError('No pudimos cargar tus estadísticas. Revisa tu conexión e intenta de nuevo.'))
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
    if (stats.vistasMesAnterior === 0) return `${entero(stats.vistasMes)} este mes`
    const pct = Math.round(((stats.vistasMes - stats.vistasMesAnterior) * 100) / stats.vistasMesAnterior)
    return pct >= 0 ? `▲ +${pct}% este mes` : `▼ ${pct}% este mes`
  })()
  const favSemana = stats?.favoritosSemana.reduce((a, b) => a + b, 0) ?? 0
  const kpis: [string, string, string, IconName, string, string][] = [
    ['Vistas totales', stats ? entero(stats.vistas) : '—', tendencia, 'eye', 'var(--teal-light)', 'var(--teal)'],
    ['Publicados', stats ? entero(stats.publicadas) : '—', stats ? `${stats.activas} activas · ${stats.vendidas} vendidas` : '', 'car', 'var(--pill)', 'var(--ink)'],
    ['Consultas', stats ? entero(stats.contactos) : '—', 'Contactos', 'chat', 'var(--success-l)', 'var(--success)'],
    ['Favoritos', stats ? entero(favSemana) : '—', 'esta semana', 'heart', 'var(--error-l)', 'var(--error)'],
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
          <button className="iconbtn" aria-label="Volver al perfil" onClick={() => navigate('/perfil')}>
            <Icon name="back" size={18} />
          </button>
          <h1>Mi Panel</h1>
        </div>
        <button className="btn btn-primary" onClick={() => navigate('/publicar')}>
          <Icon name="plus" size={18} />
          Publicar
        </button>
      </div>

      {error && (
        <div className="card empty" style={{ marginBottom: 18 }}>
          <p className="muted">{error}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            Reintentar
          </button>
        </div>
      )}

      <div className="kpis">
        {kpis.map(([t, n, s, i, bg, fg]) => (
          <div key={t} className="card kpi">
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <span className="label">{t}</span>
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
          <h3 style={{ fontSize: 17 }}>Interés en la semana</h3>
          <p className="muted" style={{ fontSize: 13, marginBottom: 12 }}>
            Favoritos agregados por día · {favSemana} {favSemana === 1 ? 'favorito' : 'favoritos'} esta semana
          </p>
          {stats ? <BarChart vals={stats.favoritosSemana} labels={stats.diasSemana} /> : <div className="skeleton" style={{ height: 190 }} />}
        </div>

        <div className="card" style={{ padding: 22 }}>
          <h3 style={{ fontSize: 17, marginBottom: 14 }}>Mis Vehículos Publicados</h3>
          {!cars ? (
            <div className="skeleton" style={{ height: 240 }} />
          ) : cars.length === 0 ? (
            <div className="empty">
              <p className="muted">Todavía no publicas ningún vehículo.</p>
              <button className="btn btn-primary" onClick={() => navigate('/publicar')}>
                Publicar mi primer vehículo
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
                      <b style={{ color: 'var(--ink)' }}>Publicaciones ocultas ({escondidas.length})</b>
                      <br />
                      <span className="muted" style={{ fontSize: 12.5 }}>
                        No aparecen en Mis publicaciones de tu perfil. Solo se ocultan en este navegador.
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
