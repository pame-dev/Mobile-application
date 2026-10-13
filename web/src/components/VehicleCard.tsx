import { useNavigate } from 'react-router-dom'
import type { Car } from '../data/types'
import { km, precio } from '../lib/format'
import { useFavorites } from '../session/FavoritesContext'
import { useSession } from '../session/SessionContext'
import { CarPhoto } from './CarPhoto'
import { Icon } from './Icon'
import { STATUS_BADGE } from './OwnerActions'

/** Tarjeta de la cuadrícula de vehículos (vcard del diseño). */
export function VehicleCard({ car, showStatus = false, note }: { car: Car; showStatus?: boolean; note?: string }) {
  const navigate = useNavigate()
  const { mode, account } = useSession()
  const fav = useFavorites()
  const on = fav.isFavorite(car.id)
  const abrir = () => navigate(`/vehiculo/${car.id}`)

  return (
    <div
      className="vcard"
      role="link"
      tabIndex={0}
      onClick={abrir}
      onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && (e.preventDefault(), abrir())}
    >
      <CarPhoto src={car.imageUrl} alt={`${car.brand} ${car.model}`}>
        {/* Los administradores no guardan favoritos (igual que la app). */}
        {mode !== 'admin' && account?.id !== car.ownerId && (
          <button
            className={`heart ${on ? 'on' : ''}`}
            aria-label={on ? 'Quitar de favoritos' : 'Guardar en favoritos'}
            aria-pressed={on}
            onClick={(e) => {
              e.stopPropagation()
              fav.toggle(car.id)
            }}
          >
            <Icon name="heart" size={17} />
          </button>
        )}
        {car.featured && <span className="tag badge b-star">★ Destacado</span>}
      </CarPhoto>
      <div className="body">
        {showStatus && <span className={`badge ${STATUS_BADGE[car.status]}`} style={{ alignSelf: 'flex-start' }}>{car.status}</span>}
        <div className="row" style={{ alignItems: 'flex-start', gap: 8 }}>
          <div className="grow">
            <div className="vtitle">
              {car.brand} {car.model}
            </div>
            <div className="mid" style={{ fontSize: 12 }}>
              {car.year} · {km(car.km)}
            </div>
          </div>
          <div>
            <div className="vprice num">{precio(car.priceValue)}</div>
            <div className="mid" style={{ fontSize: 10, textAlign: 'right' }}>
              {car.currency}
            </div>
          </div>
        </div>
        <div className="vdesc">{car.description}</div>
        {note && (
          <div className="mid" style={{ fontSize: 11.5 }}>
            {note}
          </div>
        )}
        {car.city && (
          <div className="loc">
            <Icon name="pin" />
            {car.city}
          </div>
        )}
      </div>
    </div>
  )
}
