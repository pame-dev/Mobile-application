import { useNavigate } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { useI18n } from '../i18n/I18nContext'

/** Mensaje centrado con botón al inicio (pantallas pendientes, 404). */
export function Placeholder({ title, message }: { title: string; message?: string }) {
  const navigate = useNavigate()
  const { t } = useI18n()
  return (
    <div className="wrap wrap-narrow">
      <div className="card empty empty-page">
        <span className="nic empty-icon">
          <Icon name="car" size={26} />
        </span>
        <h1 className="empty-title">{title}</h1>
        <p className="muted">{message ?? t('placeholder.message')}</p>
        <button className="btn btn-primary" onClick={() => navigate('/')}>
          {t('common.goHome')}
        </button>
      </div>
    </div>
  )
}

export function NotFound() {
  const { t } = useI18n()
  return <Placeholder title={t('notFound.title')} message={t('notFound.message')} />
}
