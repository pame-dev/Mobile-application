import type { MouseEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Car } from '../data/types'
import { useI18n } from '../i18n/I18nContext'
import { km, precio } from '../lib/format'
import { useFavorites } from '../session/FavoritesContext'
import { useSession } from '../session/SessionContext'
import { CarPhoto } from './CarPhoto'
import { ConditionTag } from './ConditionTag'
import { Icon } from './Icon'
import { Traducido } from './Traducido'
import { STATUS_BADGE, statusLabel } from './OwnerActions'

/**
 * Clic "normal" (botón izquierdo, sin Ctrl/Cmd/Shift/Alt): ese se maneja con navigate para no recargar;
 * los demás (abrir en otra pestaña, etc.) se dejan al navegador porque la tarjeta es un enlace real.
 */
export const esClicNormal = (e: MouseEvent) => !e.defaultPrevented && e.button === 0 && !e.metaKey && !e.ctrlKey && !e.shiftKey && !e.altKey

/** Tarjeta de la cuadrícula de vehículos (vcard del diseño). */
export function VehicleCard({ car, showStatus = false, note }: { car: Car; showStatus?: boolean; note?: string }) {
  const navigate = useNavigate()
  const { account } = useSession()
  const fav = useFavorites()
  const { t } = useI18n()
  const on = fav.isFavorite(car.id)
  const ruta = `/vehiculo/${car.id}`

  return (
    // El corazón va fuera del enlace (un botón no puede ir dentro de <a>), encima de la foto.
    <div className="vcard">
      <a
        className="vcard-link"
        href={ruta}
        onClick={(e) => {
          if (!esClicNormal(e)) return
          e.preventDefault()
          navigate(ruta)
        }}
      >
        {/* Tu publicación nueva en revisión: foto atenuada (todavía nadie más la ve). */}
        <CarPhoto src={car.imageUrl} alt={`${car.brand} ${car.model} ${car.year || ''}`} className={showStatus && car.status === 'Pendiente' ? 'enrevision' : ''}>
          {car.featured && <span className="tag badge b-star">{t('home.featuredBadge')}</span>}
        </CarPhoto>
        <div className="body">
          {showStatus && <span className={`badge ${STATUS_BADGE[car.status]}`} style={{ alignSelf: 'flex-start' }}>{t(statusLabel(car.status))}</span>}
          <div className="row gap-2" style={{ alignItems: 'flex-start' }}>
            <div className="grow">
              <div className="vtitle-row">
                <div className="vtitle">
                  {car.brand} {car.model}
                </div>
                <ConditionTag condicion={car.condition} compact />
              </div>
              <div className="mid text-xs">
                {car.year} · {km(car.km)}
              </div>
            </div>
            <div>
              <div className="vprice num">{precio(car.priceValue)}</div>
              <div className="mid text-xs" style={{ textAlign: 'right' }}>
                {car.currency}
              </div>
            </div>
          </div>
          <div className="vdesc">
            <Traducido texto={car.description} corto />
          </div>
          {note && (
            <div className="mid text-xs">
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
      </a>
      {/* Tus propias publicaciones no se guardan en favoritos, ni un auto vendido (no saldría ahí). */}
      {account?.id !== car.ownerId && car.status !== 'Vendido' && (
        <button
          className={`heart ${on ? 'on' : ''}`}
          aria-label={on ? t('vehiculo.fav.remove') : t('vehiculo.fav.add')}
          aria-pressed={on}
          onClick={() => fav.toggle(car.id)}
        >
          <Icon name="heart" size={18} />
        </button>
      )}
    </div>
  )
}
