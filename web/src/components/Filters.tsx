import { useEffect, useState } from 'react'
import { catalogs } from '../data/api'
import type { Car, Catalogs } from '../data/types'
import { FILTER_KEYS, SIN_FILTROS, type Filtros } from '../lib/filtros'
import { Icon } from './Icon'
import { ModalHead, useUi } from './Ui'

const anioActual = new Date().getFullYear()
const ANIOS = Array.from({ length: 35 }, (_, i) => String(anioActual + 1 - i))

/** Campos del panel de filtros: tipo, marca, modelo, año y rango de precio. */
export function FilterFields({ draft, onChange, anios = ANIOS }: { draft: Filtros; onChange: (f: Filtros) => void; anios?: string[] }) {
  const [cat, setCat] = useState<Catalogs | null>(null)
  useEffect(() => {
    catalogs().then(setCat).catch(() => setCat(null))
  }, [])
  // Al cambiar la marca, el modelo elegido ya no aplica.
  const set = (k: keyof Filtros, v: string) => onChange({ ...draft, [k]: v, ...(k === 'marca' ? { modelo: '' } : {}) })
  const marcaSel = cat?.marcas.find((m) => String(m.id) === draft.marca)

  return (
    <>
      <div className="field">
        <label>Tipo</label>
        <div className="row" style={{ flexWrap: 'wrap', gap: 6 }}>
          <button type="button" className={`chip ${!draft.tipo ? 'on' : ''}`} onClick={() => set('tipo', '')}>
            Todos
          </button>
          {cat?.carrocerias
            .filter((c) => !c.esOtro)
            .map((c) => (
              <button type="button" key={c.id} className={`chip ${draft.tipo === c.nombre ? 'on' : ''}`} onClick={() => set('tipo', c.nombre)}>
                {c.nombre}
              </button>
            ))}
        </div>
      </div>
      <div className="field">
        <label htmlFor="fb">Marca</label>
        <select className="input" id="fb" value={draft.marca} onChange={(e) => set('marca', e.target.value)}>
          <option value="">Todas</option>
          {cat?.marcas
            .filter((m) => !m.esOtro)
            .map((m) => (
              <option key={m.id} value={m.id}>
                {m.nombre}
              </option>
            ))}
        </select>
      </div>
      <div className="field">
        <label htmlFor="fm">Modelo</label>
        <select className="input" id="fm" value={draft.modelo} disabled={!marcaSel} onChange={(e) => set('modelo', e.target.value)}>
          <option value="">Todos</option>
          {cat?.modelos
            .filter((m) => m.idMarca === marcaSel?.id && !m.esOtro)
            .map((m) => (
              <option key={m.id} value={m.id}>
                {m.nombre}
              </option>
            ))}
        </select>
      </div>
      <div className="field">
        <label htmlFor="fy">Año</label>
        <select className="input" id="fy" value={draft.anio} onChange={(e) => set('anio', e.target.value)}>
          <option value="">Cualquiera</option>
          {anios.map((a) => (
            <option key={a}>{a}</option>
          ))}
        </select>
      </div>
      <div className="field">
        <label>Rango de precio</label>
        <div className="grid2" style={{ gap: 8 }}>
          <input className="input" inputMode="numeric" placeholder="Mín. $0" aria-label="Precio mínimo" value={draft.min} onChange={(e) => set('min', e.target.value.replace(/\D/g, ''))} />
          <input className="input" inputMode="numeric" placeholder="Máx. $2M" aria-label="Precio máximo" value={draft.max} onChange={(e) => set('max', e.target.value.replace(/\D/g, ''))} />
        </div>
      </div>
    </>
  )
}

/** Años de una lista de vehículos, del más nuevo al más viejo (filterOptionsOf de la app). */
export const aniosDe = (cars: Car[]) => [...new Set(cars.map((c) => String(c.year)))].sort((a, b) => b.localeCompare(a))

function FiltersModal({ value, anios, onApply }: { value: Filtros; anios?: string[]; onApply: (f: Filtros) => void }) {
  const { closeModal } = useUi()
  const [draft, setDraft] = useState(value)
  return (
    <div className="modal" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <ModalHead title="Filtros" />
      <FilterFields draft={draft} onChange={setDraft} anios={anios} />
      <div className="grid2">
        <button
          className="btn btn-ghost"
          onClick={() => {
            onApply(SIN_FILTROS)
            closeModal()
          }}
        >
          Limpiar
        </button>
        <button
          className="btn btn-primary"
          onClick={() => {
            onApply(draft)
            closeModal()
          }}
        >
          Aplicar
        </button>
      </div>
    </div>
  )
}

/** Botón de filtros junto a una búsqueda (perfil de lote, favoritos): abre el panel en un modal. */
export function FiltersButton({ value, onApply, anios }: { value: Filtros; onApply: (f: Filtros) => void; anios?: string[] }) {
  const { openModal } = useUi()
  const activos = FILTER_KEYS.filter((k) => value[k] && k !== 'modelo').length
  return (
    <button
      className={`btn filterbtn solo ${activos ? 'on' : ''}`}
      style={{ height: 46 }}
      onClick={() => openModal(<FiltersModal value={value} anios={anios} onApply={onApply} />)}
    >
      <Icon name="filter" size={18} />
      <span>Filtros{activos ? ` (${activos})` : ''}</span>
    </button>
  )
}
