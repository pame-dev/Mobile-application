import { useNavigate } from 'react-router-dom'
import { useI18n } from '../i18n/I18nContext'
import { Icon } from './Icon'

/**
 * ¿Hay una pantalla anterior de Karsy a la cual regresar? React Router guarda la posición en el
 * historial (idx) en history.state: 0 si Karsy se abrió directo (enlace o recarga), y se conserva
 * al reemplazar la pantalla. (location.key cambia al reemplazar, así que no sirve para esto.)
 */
export const hayPantallaAnterior = () => ((window.history.state as { idx?: number } | null)?.idx ?? 0) > 0

/**
 * Botón cuadrado "‹" para regresar; todos los "volver" de la web usan este mismo diseño.
 * - [onClick]: lo que hace al tocarlo (p. ej. regresar un paso dentro de un formulario).
 * - [to]: va siempre a esa ruta.
 * - Sin ninguno: regresa a la pantalla anterior conservando su contexto; si la página se
 *   abrió directo (sin historial), va a [fallback].
 */
export function VolverButton({ fallback = '/', to, onClick, label }: { fallback?: string; to?: string; onClick?: () => void; label?: string }) {
  const navigate = useNavigate()
  const { t } = useI18n()
  const texto = label ?? t('settings.backLabel')
  const volver = () => {
    if (onClick) onClick()
    else if (to) navigate(to)
    else if (hayPantallaAnterior()) navigate(-1)
    else navigate(fallback, { replace: true })
  }
  return (
    <button type="button" className="iconbtn" style={{ flex: 'none' }} aria-label={texto} title={texto} onClick={volver}>
      <Icon name="back" size={18} />
    </button>
  )
}
