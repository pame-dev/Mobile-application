import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { lots as cargarLotes } from '../data/api'
import type { Lot } from '../data/types'
import { iniciales } from '../lib/format'
import { useSession } from '../session/SessionContext'

/** Logo del lote o sus iniciales. */
export function LotLogo({ lot, className = 'lotlogo' }: { lot: Pick<Lot, 'name' | 'logoUrl'>; className?: string }) {
  const [fallo, setFallo] = useState(false)
  return <span className={className}>{lot.logoUrl && !fallo ? <img src={lot.logoUrl} alt="" onError={() => setFallo(true)} /> : iniciales(lot.name)}</span>
}

/** Directorio de lotes y agencias (pestaña "Lotes" de la app). */
export function Lots() {
  const navigate = useNavigate()
  const { account, loading: cargandoSesion } = useSession()
  const [params, setParams] = useSearchParams()
  const query = params.get('q') ?? ''
  const [lista, setLista] = useState<Lot[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(() => {
    setError(null)
    cargarLotes(account?.id ?? null)
      .then(setLista)
      .catch(() => setError('No pudimos cargar los lotes. Revisa tu conexión e intenta de nuevo.'))
  }, [account?.id])

  useEffect(() => {
    // Se espera a saber quién es: el lote propio no aparece en el directorio.
    if (!cargandoSesion) cargar()
  }, [cargar, cargandoSesion])

  // Búsqueda por nombre o ciudad.
  const q = query.trim().toLowerCase()
  const visibles = (lista ?? []).filter((l) => !q || l.name.toLowerCase().includes(q) || l.city.toLowerCase().includes(q))

  return (
    <div className="wrap" style={{ paddingTop: 28 }}>
      <div className="banner">
        <span className="eyebrow" style={{ color: '#8FD3D8' }}>
          Catálogo de negocios automotrices
        </span>
        <h1 style={{ fontSize: 36, margin: '6px 0 8px' }}>Lotes</h1>
        <p>Vehículos de lotes y agencias. Esta sección muestra exclusivamente publicaciones de vendedores tipo lote/agencia.</p>
        <div className="inputicon" style={{ marginTop: 20, maxWidth: 520 }}>
          <Icon name="search" size={18} />
          <input
            className="input"
            style={{ height: 48 }}
            placeholder="Buscar lote o ciudad…"
            aria-label="Buscar lote o ciudad"
            value={query}
            onChange={(e) => setParams(e.target.value ? { q: e.target.value } : {}, { replace: true })}
          />
        </div>
      </div>

      <div className="sechead">
        <h2>
          {!lista
            ? 'Lotes registrados'
            : q
              ? `${visibles.length} ${visibles.length === 1 ? 'resultado' : 'resultados'}`
              : `${lista.length} ${lista.length === 1 ? 'lote registrado' : 'lotes registrados'}`}
        </h2>
      </div>

      {error ? (
        <div className="card empty">
          <p className="muted">{error}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            Reintentar
          </button>
        </div>
      ) : !lista ? (
        <div className="lgrid">
          {Array.from({ length: 6 }, (_, i) => (
            <div key={i} className="skeleton" style={{ height: 150 }} />
          ))}
        </div>
      ) : visibles.length === 0 ? (
        <div className="card empty">
          <span className="nic" style={{ width: 56, height: 56, borderRadius: 16, background: 'var(--teal-light)', color: 'var(--teal)' }}>
            <Icon name="building" size={26} />
          </span>
          <p className="muted">{lista.length === 0 ? 'Aún no hay lotes registrados.' : 'Ningún lote coincide con tu búsqueda.'}</p>
        </div>
      ) : (
        <div className="lgrid">
          {visibles.map((l) => (
            <button key={l.id} className="card lotcard" onClick={() => navigate(`/lotes/${l.id}`)}>
              <LotLogo lot={l} />
              <span className="grow" style={{ display: 'flex', flexDirection: 'column', gap: 4, minWidth: 0 }}>
                <b style={{ fontFamily: 'var(--f-display)', fontSize: 17, color: 'var(--ink)' }}>{l.name}</b>
                {l.city && (
                  <span className="loc">
                    <Icon name="pin" />
                    {l.city}
                  </span>
                )}
                {l.description && (
                  <span className="muted" style={{ fontSize: 13, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                    {l.description}
                  </span>
                )}
                <span className="badge b-info" style={{ alignSelf: 'flex-start', marginTop: 6 }}>
                  {l.vehicleCount} {l.vehicleCount === 1 ? 'vehículo' : 'vehículos'}
                </span>
              </span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
