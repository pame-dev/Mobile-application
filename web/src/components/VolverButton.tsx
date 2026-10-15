import { useLocation, useNavigate } from 'react-router-dom'
import { useI18n } from '../i18n/I18nContext'
import { Icon } from './Icon'

/**
 * Botón cuadrado "←" (el de Notificaciones): regresa a la pantalla anterior conservando su
 * contexto; si la página se abrió directo (sin historial), va a [fallback].
 */
export function VolverButton({ fallback }: { fallback: string }) {
  const navigate = useNavigate()
  const location = useLocation()
  const { t } = useI18n()
  return (
    <button
      className="iconbtn"
      style={{ flex: 'none' }}
      aria-label={t('settings.backLabel')}
      title={t('settings.backLabel')}
      onClick={() => (location.key !== 'default' ? navigate(-1) : navigate(fallback, { replace: true }))}
    >
      <Icon name="back" size={18} />
    </button>
  )
}
