import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { CarPhoto } from '../components/CarPhoto'
import { Icon } from '../components/Icon'
import { VehicleCard } from '../components/VehicleCard'
import { heroStats, visibleCars } from '../data/api'
import type { Car } from '../data/types'
import { useI18n } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { FILTER_KEYS, ORDEN, filtrarCars, filtrosDeUrl } from '../lib/filtros'
import { entero, km, precio } from '../lib/format'
import { useSession } from '../session/SessionContext'

const POR_PAGINA_DESTACADOS = 3

export function Home() {
  const { mode } = useSession()
  const { t } = useI18n()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const [cars, setCars] = useState<Car[] | null>(null)
  const [error, setError] = useState<Clave | null>(null)
  const [stats, setStats] = useState<{ vehiculos: number; marcas: number; modelos: number } | null>(null)
  const [pagina, setPagina] = useState(0)

  const cargar = useCallback(() => {
    setError(null)
    visibleCars()
      .then((c) => {
        setCars(c)
        heroStats(c).then(setStats).catch(() => setStats({ vehiculos: c.length, marcas: 0, modelos: 0 }))
      })
      .catch(() => setError('home.loadError'))
  }, [])
  useEffect(() => {
    cargar()
  }, [cargar])

  const lista = useMemo(() => (cars ? filtrarCars(cars, filtrosDeUrl(params), params.get('q') ?? '', params.get('orden') ?? '') : []), [cars, params])
  const destacados = useMemo(() => (cars ?? []).filter((c) => c.featured), [cars])
  const hayFiltros = params.has('q') || FILTER_KEYS.some((k) => params.has(k))
  const paginas = Math.max(1, Math.ceil(destacados.length / POR_PAGINA_DESTACADOS))
  const verDestacados = destacados.slice(pagina * POR_PAGINA_DESTACADOS, (pagina + 1) * POR_PAGINA_DESTACADOS)

  const setOrden = (v: string) => {
    const next = new URLSearchParams(params)
    if (v === 'recientes') next.delete('orden')
    else next.set('orden', v)
    setParams(next, { replace: true })
  }

  return (
    <>
      <section className="hero">
        <div className="wrap heroc">
          <span className="eyebrow">{t('home.eyebrow')}</span>
          <h1>
            {t('home.title1')}
            <br />
            {t('home.title2')}
          </h1>
          <p>{t('home.lead')}</p>
          <div className="stats">
            <div>
              <b className="num">{stats ? entero(stats.vehiculos) : '—'}</b>
              <span>{t('home.statVehicles')}</span>
            </div>
            <div>
              <b className="num">{stats ? entero(stats.marcas) : '—'}</b>
              <span>{t('home.statBrands')}</span>
            </div>
            <div>
              <b className="num">{stats ? entero(stats.modelos) : '—'}</b>
              <span>{t('home.statModels')}</span>
            </div>
          </div>
        </div>
      </section>

      <div className="wrap">
        {mode === 'guest' && (
          <div className="visitor">
            <div className="nic" style={{ background: 'var(--navy)', color: '#fff' }}>
              <Icon name="user" />
            </div>
            <div className="grow">
              <b style={{ color: 'var(--ink)', fontFamily: 'var(--f-display)', fontSize: 16 }}>{t('home.visitorTitle')}</b>
              <p className="muted">{t('home.visitorText')}</p>
            </div>
            <button className="btn btn-primary" onClick={() => navigate('/registro')}>
              {t('home.visitorCta')}
            </button>
          </div>
        )}

        {/* Mientras se busca o filtra, los destacados se ocultan para que los resultados queden arriba (igual que la app). */}
        {!hayFiltros && destacados.length > 0 && (
          <>
            <div className="sechead">
              <div>
                <h2>{t('home.featured')}</h2>
                <p className="muted">{t('home.featuredSub')}</p>
              </div>
              {paginas > 1 && (
                <div className="row">
                  <button className="iconbtn" aria-label={t('home.prev')} onClick={() => setPagina((p) => (p - 1 + paginas) % paginas)}>
                    <Icon name="back" size={18} />
                  </button>
                  <button className="iconbtn" aria-label={t('home.next')} onClick={() => setPagina((p) => (p + 1) % paginas)}>
                    <Icon name="chev" size={18} />
                  </button>
                </div>
              )}
            </div>
            <div className="featrow">
              {verDestacados.map((c) => (
                <button key={c.id} className="fcard" onClick={() => navigate(`/vehiculo/${c.id}`)}>
                  <CarPhoto src={c.imageUrl} alt={`${c.brand} ${c.model}`} />
                  <div className="shade" />
                  <span className="badge b-star" style={{ position: 'absolute', left: 14, top: 14 }}>
                    {t('home.featuredBadge')}
                  </span>
                  <div className="info">
                    <div>
                      <b>
                        {c.brand} {c.model}
                      </b>
                      <span>
                        {c.year} · {km(c.km)}
                        {c.city ? ` · ${c.city}` : ''}
                      </span>
                    </div>
                    <div className="pr num">{precio(c.priceValue)}</div>
                  </div>
                </button>
              ))}
            </div>
          </>
        )}

        <div className="sechead">
          <div>
            <h2>{hayFiltros ? t('home.results') : t('home.all')}</h2>
            <p className="muted">
              {cars ? t(lista.length === 1 ? 'home.countOne' : 'home.countMany', { n: entero(lista.length) }) : t('common.loading')}
            </p>
          </div>
          <div className="row" style={{ flexWrap: 'wrap' }}>
            {hayFiltros && (
              <button className="chip" onClick={() => setParams(new URLSearchParams(), { replace: true })}>
                <Icon name="x" size={14} />
                {t('home.clearFilters')}
              </button>
            )}
            <select
              className="input"
              aria-label={t('sort.label')}
              value={params.get('orden') ?? 'recientes'}
              onChange={(e) => setOrden(e.target.value)}
              style={{ height: 36, width: 'auto', borderRadius: 999 }}
            >
              {ORDEN.map(([v, clave]) => (
                <option key={v} value={v}>
                  {t(clave)}
                </option>
              ))}
            </select>
          </div>
        </div>

        {error ? (
          <div className="card empty">
            <p className="muted">{t(error)}</p>
            <button className="btn btn-ghost" onClick={cargar}>
              {t('common.retry')}
            </button>
          </div>
        ) : !cars ? (
          <div className="vgrid">
            {Array.from({ length: 8 }, (_, i) => (
              <div key={i} className="skeleton" style={{ height: 300 }} />
            ))}
          </div>
        ) : lista.length === 0 ? (
          <div className="card empty">
            <p className="muted">{cars.length === 0 ? t('home.empty') : t('home.noMatch')}</p>
            {cars.length > 0 && (
              <button className="btn btn-ghost" onClick={() => setParams(new URLSearchParams(), { replace: true })}>
                {t('home.clearFilters')}
              </button>
            )}
          </div>
        ) : (
          <div className="vgrid">
            {lista.map((c) => (
              <VehicleCard key={c.id} car={c} />
            ))}
          </div>
        )}
      </div>
    </>
  )
}
