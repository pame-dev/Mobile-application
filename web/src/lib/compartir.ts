import type { Car } from '../data/types'
import type { T } from '../i18n/I18nContext'
import { km, precio } from './format'

/** Dirección pública de la web (BuildConfig.WEB_URL en la app): el enlace compartido siempre apunta aquí. */
export const SITIO = 'https://karsy.netlify.app'

/**
 * Mensaje para compartir un auto, igual que shareCar en la app (CarDetailScreen.kt):
 *
 *   🚗 *Kia Cerato 2003*
 *   💰 $215,000 MXN
 *   📍 Manzanillo, Colima · 45,000 km
 *
 *   https://karsy.netlify.app/vehiculo/42
 *
 *   Lo vi en Karsy, la app para comprar y vender autos.
 *
 * Los textos fijos vienen de vehiculo.share.* (mismos valores que detail_share_* de strings.xml).
 */
export function construirTextoCompartir(c: Pick<Car, 'id' | 'brand' | 'model' | 'year' | 'priceValue' | 'currency' | 'city' | 'km'>, t: T): string {
  // Ubicación y kilometraje: solo los que existan (sin "—"); si no hay ninguno, no va la línea.
  const lugar = [c.city, km(c.km)].filter((v) => v && v.trim() && v !== '—').join(' · ')
  return [
    t('vehiculo.share.title', { titulo: `${c.brand} ${c.model} ${c.year}`.replace(/\s+/g, ' ').trim() }),
    t('vehiculo.share.price', { precio: precio(c.priceValue), moneda: c.currency }),
    ...(lugar ? [t('vehiculo.share.place', { lugar })] : []),
    '',
    `${SITIO}/vehiculo/${c.id}`,
    '',
    t('vehiculo.share.footer'),
  ].join('\n')
}
