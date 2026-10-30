import { useEffect, useState } from 'react'
import { Icon } from '../components/Icon'
import { VolverButton } from '../components/VolverButton'
import { datosPagoDestacado } from '../data/seller'
import { useI18n } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { useSession } from '../session/SessionContext'

/** Correo de soporte de Karsy (también en HelpScreen de la app). */
export const CORREO_SOPORTE = 'karsypi5@gmail.com'

/** Preguntas frecuentes: [pregunta, respuesta]. Las mismas que en la app (HelpScreen). */
const PREGUNTAS: [Clave, Clave][] = [
  ['help.q.publish', 'help.a.publish'],
  ['help.q.review', 'help.a.review'],
  ['help.q.rejected', 'help.a.rejected'],
  ['help.q.featured', 'help.a.featured'],
  ['help.q.featuredPay', 'help.a.featuredPay'],
  ['help.q.verify', 'help.a.verify'],
  ['help.q.contact', 'help.a.contact'],
  ['help.q.safe', 'help.a.safe'],
  ['help.q.report', 'help.a.report'],
  ['help.q.language', 'help.a.language'],
]

/** Ayuda: preguntas frecuentes y cómo escribirle a soporte. Abierta para todos (con o sin sesión). */
export function Ayuda() {
  const { t } = useI18n()
  const { account } = useSession()
  // Días que dura un destacado (los cambia el admin). Sin sesión no se pueden leer: 30, los de siempre.
  const [dias, setDias] = useState(30)
  useEffect(() => {
    if (account) void datosPagoDestacado().then((d) => setDias(d.dias), () => undefined)
  }, [account])
  return (
    <div className="wrap wrap-narrow page-end">
      <div className="pagehead">
        <div className="row">
          <VolverButton fallback="/" />
          <h1>{t('help.title')}</h1>
        </div>
      </div>
      <p className="muted" style={{ marginBottom: 'var(--s-4)' }}>
        {t('help.sub')}
      </p>
      <div className="card" style={{ overflow: 'hidden', marginBottom: 'var(--s-4)' }}>
        {PREGUNTAS.map(([p, r]) => (
          <details key={p} className="faq">
            <summary>
              {t(p)}
              <Icon name="down" size={16} />
            </summary>
            <p>{t(r, { dias })}</p>
          </details>
        ))}
      </div>
      <div className="card" style={{ padding: 'var(--s-5)', display: 'flex', gap: 'var(--s-3)', alignItems: 'center', flexWrap: 'wrap' }}>
        <span className="nic" style={{ width: 44, height: 44, borderRadius: 'var(--r-md)', background: 'var(--teal-light)', color: 'var(--teal)' }}>
          <Icon name="mail" size={20} />
        </span>
        <div className="grow" style={{ minWidth: 200 }}>
          <b className="ink">{t('help.contactTitle')}</b>
          <p className="muted text-sm">
            {t('help.contactDesc')}
          </p>
        </div>
        <a className="btn btn-primary btn-sm" href={`mailto:${CORREO_SOPORTE}`}>
          {CORREO_SOPORTE}
        </a>
      </div>
    </div>
  )
}
