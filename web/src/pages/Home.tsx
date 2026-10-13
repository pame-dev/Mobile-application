import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { CarPhoto } from '../components/CarPhoto'
import { Icon } from '../components/Icon'
import { VehicleCard } from '../components/VehicleCard'
import { heroStats, visibleCars } from '../data/api'
import type { Car } from '../data/types'
import { FILTER_KEYS, ORDEN, filtrarCars, filtrosDeUrl } from '../lib/filtros'
import { entero, km, precio } from '../lib/format'
import { useSession } from '../session/SessionContext'

const POR_PAGINA_DESTACADOS = 3

export function Home() {
  const { mode } = useSession()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const [cars, setCars] = useState<Car[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [stats, setStats] = useState<{ vehiculos: number; marcas: number; modelos: number } | null>(null)
  const [pagina, setPagina] = useState(0)

  const cargar = useCallback(() => {
    setError(null)
    visibleCars()
      .then((c) => {
        setCars(c)
        heroStats(c).then(setStats).catch(() => setStats({ vehiculos: c.length, marcas: 0, modelos: 0 }))
      })
      .catch(() => setError('No pudimos cargar los vehículos. Revisa tu conexión e intenta de nuevo.'))
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
          <span className="eyebrow">Marketplace automotriz #1 en México</span>
          <h1>
            Encuentra tu próximo
            <br />
            vehículo ideal
          </h1>
          <p>Miles de vehículos verificados en toda la república mexicana. Compra, vende y negocia con confianza.</p>
          <div className="stats">
            <div>
              <b className="num">{stats ? entero(stats.vehiculos) : '—'}</b>
              <span>Vehículos</span>
            </div>
            <div>
              <b className="num">{stats ? entero(stats.marcas) : '—'}</b>
              <span>Marcas</span>
            </div>
            <div>
              <b className="num">{stats ? entero(stats.modelos) : '—'}</b>
              <span>Modelos</span>
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
              <b style={{ color: 'var(--ink)', fontFamily: 'var(--f-display)', fontSize: 16 }}>Estás en modo visitante</b>
              <p className="muted">Regístrate para guardar favoritos, contactar vendedores y publicar tu vehículo.</p>
            </div>
            <button className="btn btn-primary" onClick={() => navigate('/registro')}>
              Crear cuenta gratis →
            </button>
          </div>
        )}

        {/* Mientras se busca o filtra, los destacados se ocultan para que los resultados queden arriba (igual que la app). */}
        {!hayFiltros && destacados.length > 0 && (
          <>
            <div className="sechead">
              <div>
                <h2>Vehículos destacados</h2>
                <p className="muted">Selección especial de nuestra plataforma</p>
              </div>
              {paginas > 1 && (
                <div className="row">
                  <button className="iconbtn" aria-label="Anteriores" onClick={() => setPagina((p) => (p - 1 + paginas) % paginas)}>
                    <Icon name="back" size={18} />
                  </button>
                  <button className="iconbtn" aria-label="Siguientes" onClick={() => setPagina((p) => (p + 1) % paginas)}>
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
                    ★ Destacado
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
            <h2>{hayFiltros ? 'Resultados' : 'Todos los vehículos'}</h2>
            <p className="muted">
              {cars ? `${entero(lista.length)} ${lista.length === 1 ? 'vehículo disponible' : 'vehículos disponibles'}` : 'Cargando…'}
            </p>
          </div>
          <div className="row" style={{ flexWrap: 'wrap' }}>
            {hayFiltros && (
              <button className="chip" onClick={() => setParams(new URLSearchParams(), { replace: true })}>
                <Icon name="x" size={14} />
                Limpiar filtros
              </button>
            )}
            <select
              className="input"
              aria-label="Ordenar"
              value={params.get('orden') ?? 'recientes'}
              onChange={(e) => setOrden(e.target.value)}
              style={{ height: 36, width: 'auto', borderRadius: 999 }}
            >
              {ORDEN.map(([v, t]) => (
                <option key={v} value={v}>
                  {t}
                </option>
              ))}
            </select>
          </div>
        </div>

        {error ? (
          <div className="card empty">
            <p className="muted">{error}</p>
            <button className="btn btn-ghost" onClick={cargar}>
              Reintentar
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
            <p className="muted">{cars.length === 0 ? 'Todavía no hay vehículos publicados.' : 'Ningún vehículo coincide con tu búsqueda.'}</p>
            {cars.length > 0 && (
              <button className="btn btn-ghost" onClick={() => setParams(new URLSearchParams(), { replace: true })}>
                Limpiar filtros
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
