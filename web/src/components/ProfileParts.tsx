import { useMemo, useState, type ReactNode } from 'react'
import type { Car } from '../data/types'
import { alFinalLosVendidos } from '../data/api'
import { useI18n } from '../i18n/I18nContext'
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
  const { t } = useI18n()
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
            aria-label={t('perfil.viewPhotoOf', { nombre: name })}
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
  const { t } = useI18n()
  const lista = useMemo(() => alFinalLosVendidos(filtrarCars(cars, filtros, q, orden)), [cars, filtros, q, orden])
  const filtrando = !!q.trim() || hayFiltros(filtros)

  return (
    <div>
      <div className="row" style={{ justifyContent: 'space-between', marginBottom: 16, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h2 style={{ fontSize: 22 }}>{title}</h2>
          <p className="muted">
            {filtrando
              ? t('perfil.ofTotal', { n: lista.length, total: t(cars.length === 1 ? 'perfil.vehiclesOne' : 'perfil.vehiclesMany', { n: cars.length }) })
              : t(cars.length === 1 ? 'perfil.vehiclesOne' : 'perfil.vehiclesMany', { n: cars.length })}
          </p>
        </div>
        {cars.length > 0 && (
          <div className="row" style={{ flexWrap: 'wrap', gap: 8 }}>
            <div className="inputicon" style={{ width: 240, maxWidth: '100%' }}>
              <Icon name="search" size={18} />
              <input className="input" placeholder={t('perfil.inventorySearchPlaceholder')} aria-label={t('perfil.inventorySearchLabel')} value={q} onChange={(e) => setQ(e.target.value)} />
            </div>
            <FiltersButton value={filtros} onApply={setFiltros} anios={aniosDe(cars)} />
            <select className="input" aria-label={t('sort.label')} value={orden} onChange={(e) => setOrden(e.target.value)} style={{ width: 'auto' }}>
              {ORDEN.map(([v, clave]) => (
                <option key={v} value={v}>
                  {t(clave)}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>
      {lista.length === 0 ? (
        <div className="card empty">
          <p className="muted">{cars.length === 0 ? emptyText : t('home.noMatch')}</p>
          {filtrando && (
            <button
              className="btn btn-ghost"
              onClick={() => {
                setQ('')
                setFiltros(SIN_FILTROS)
              }}
            >
              {t('home.clearFilters')}
            </button>
          )}
        </div>
      ) : (
        <div className="vgrid">
          {lista.map((c) => (
            <VehicleCard key={c.id} car={c} showStatus={c.status === 'Vendido'} />
          ))}
        </div>
      )}
    </div>
  )
}
