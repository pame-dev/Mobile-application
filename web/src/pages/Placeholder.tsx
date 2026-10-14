import { useNavigate } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { useI18n } from '../i18n/I18nContext'

/** Mensaje centrado con botón al inicio (pantallas pendientes, 404). */
export function Placeholder({ title, message }: { title: string; message?: string }) {
  const navigate = useNavigate()
  const { t } = useI18n()
  return (
    <div className="wrap" style={{ maxWidth: 820 }}>
      <div className="card empty" style={{ marginTop: 40 }}>
        <span className="nic" style={{ width: 56, height: 56, borderRadius: 16, background: 'var(--teal-light)', color: 'var(--teal)' }}>
          <Icon name="car" size={26} />
        </span>
        <h2>{title}</h2>
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
