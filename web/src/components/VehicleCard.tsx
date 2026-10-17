import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { resumenEnIngles, resumenGuardado } from '../data/api'
import type { Car } from '../data/types'
import { useI18n } from '../i18n/I18nContext'
import { km, precio } from '../lib/format'
import { useFavorites } from '../session/FavoritesContext'
import { useSession } from '../session/SessionContext'
import { CarPhoto } from './CarPhoto'
import { Icon } from './Icon'
import { STATUS_BADGE, statusLabel } from './OwnerActions'

/** Tarjeta de la cuadrícula de vehículos (vcard del diseño). */
export function VehicleCard({ car, showStatus = false, note }: { car: Car; showStatus?: boolean; note?: string }) {
  const navigate = useNavigate()
  const { account } = useSession()
  const fav = useFavorites()
  const { t, lang } = useI18n()
  const on = fav.isFavorite(car.id)
  // En inglés: el inicio de la descripción traducido (como el detalle); mientras llega o si falla, el original.
  // Si ya estaba guardada se muestra desde el inicio (sin parpadeo).
  const [traducida, setTraducida] = useState<{ de: string; texto: string } | null>(() => {
    const tr = lang === 'en' ? resumenGuardado(car.description) : null
    return tr ? { de: car.description, texto: tr } : null
  })
  useEffect(() => {
    if (lang !== 'en' || !car.description.trim()) return
    let vivo = true
    void resumenEnIngles(car.description).then((tr) => vivo && tr && setTraducida({ de: car.description, texto: tr }))
    return () => {
      vivo = false
    }
  }, [lang, car.description])
  const descripcion = lang === 'en' && traducida?.de === car.description ? traducida.texto : car.description
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
        {/* Tus propias publicaciones no se guardan en favoritos. */}
        {account?.id !== car.ownerId && (
          <button
            className={`heart ${on ? 'on' : ''}`}
            aria-label={on ? t('vehiculo.fav.remove') : t('vehiculo.fav.add')}
            aria-pressed={on}
            onClick={(e) => {
              e.stopPropagation()
              fav.toggle(car.id)
            }}
          >
            <Icon name="heart" size={17} />
          </button>
        )}
        {car.featured && <span className="tag badge b-star">{t('home.featuredBadge')}</span>}
      </CarPhoto>
      <div className="body">
        {showStatus && <span className={`badge ${STATUS_BADGE[car.status]}`} style={{ alignSelf: 'flex-start' }}>{t(statusLabel(car.status))}</span>}
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
        <div className="vdesc">{descripcion}</div>
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
