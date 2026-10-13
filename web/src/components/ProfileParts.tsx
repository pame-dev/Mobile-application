import { useMemo, useState, type ReactNode } from 'react'
import type { Car } from '../data/types'
import { ORDEN, SIN_FILTROS, filtrarCars, hayFiltros, type Filtros } from '../lib/filtros'
import { iniciales } from '../lib/format'
import { FiltersButton, aniosDe } from './Filters'
import { Icon } from './Icon'
import { useUi } from './Ui'
import { VehicleCard } from './VehicleCard'

/** Portada, foto (se amplía al hacer clic) y nombre de un perfil público. */
export function ProfileHeader({
  name,
  avatarUrl,
  coverUrl,
  square = false,
  children,
  actions,
}: {
  name: string
  avatarUrl: string | null
  coverUrl: string | null
  /** Los lotes usan logo cuadrado. */
  square?: boolean
  children?: ReactNode
  actions?: ReactNode
}) {
  const { openModal, closeModal } = useUi()
  const [portadaFallo, setPortadaFallo] = useState(false)
  const [fotoFallo, setFotoFallo] = useState(false)
  const foto = avatarUrl && !fotoFallo ? avatarUrl : null
  const estilo = square ? { borderRadius: 28, background: 'var(--navy)' } : undefined

  return (
    <>
      <div className="cover">{coverUrl && !portadaFallo && <img src={coverUrl} alt="" onError={() => setPortadaFallo(true)} />}</div>
      <div className="profilehead">
        {foto ? (
          <button
            className="bigavatar"
            style={estilo}
            aria-label={`Ver foto de ${name}`}
            onClick={() => openModal(<img className="lightbox" src={foto} alt={name} onClick={closeModal} />)}
          >
            <img src={foto} alt="" onError={() => setFotoFallo(true)} />
          </button>
        ) : (
          <div className="bigavatar" style={estilo}>
            {iniciales(name)}
          </div>
        )}
        <div className="grow" style={{ paddingBottom: 10, minWidth: 220 }}>
          {children}
        </div>
        {actions && (
          <div className="row" style={{ paddingBottom: 12, flexWrap: 'wrap' }}>
            {actions}
          </div>
        )}
      </div>
    </>
  )
}

/** Inventario de un vendedor con búsqueda, filtros y orden (perfil de lote / vendedor). */
export function Inventory({ cars, title, emptyText }: { cars: Car[]; title: string; emptyText: string }) {
  const [q, setQ] = useState('')
  const [filtros, setFiltros] = useState<Filtros>(SIN_FILTROS)
  const [orden, setOrden] = useState('recientes')
  const lista = useMemo(() => filtrarCars(cars, filtros, q, orden), [cars, filtros, q, orden])
  const filtrando = !!q.trim() || hayFiltros(filtros)

  return (
    <div>
      <div className="row" style={{ justifyContent: 'space-between', marginBottom: 16, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h2 style={{ fontSize: 22 }}>{title}</h2>
          <p className="muted">
            {filtrando ? `${lista.length} de ${cars.length}` : cars.length} {cars.length === 1 ? 'vehículo' : 'vehículos'}
          </p>
        </div>
        {cars.length > 0 && (
          <div className="row" style={{ flexWrap: 'wrap', gap: 8 }}>
            <div className="inputicon" style={{ width: 240, maxWidth: '100%' }}>
              <Icon name="search" size={18} />
              <input className="input" placeholder="Buscar marca o modelo…" aria-label="Buscar en el inventario" value={q} onChange={(e) => setQ(e.target.value)} />
            </div>
            <FiltersButton value={filtros} onApply={setFiltros} anios={aniosDe(cars)} />
            <select className="input" aria-label="Ordenar" value={orden} onChange={(e) => setOrden(e.target.value)} style={{ width: 'auto' }}>
              {ORDEN.map(([v, t]) => (
                <option key={v} value={v}>
                  {t}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>
      {lista.length === 0 ? (
        <div className="card empty">
          <p className="muted">{cars.length === 0 ? emptyText : 'Ningún vehículo coincide con tu búsqueda.'}</p>
          {filtrando && (
            <button
              className="btn btn-ghost"
              onClick={() => {
                setQ('')
                setFiltros(SIN_FILTROS)
              }}
            >
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
  )
}
