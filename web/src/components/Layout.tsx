import { useEffect } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { useI18n } from '../i18n/I18nContext'
import { useSession } from '../session/SessionContext'
import { Icon } from './Icon'
import { TopNav } from './TopNav'
import { useUi } from './Ui'

export function Footer() {
  const navigate = useNavigate()
  const { t, lang, setLang } = useI18n()
  const go = (to: string) => () => navigate(to)
  return (
    <footer className="footer">
      <div className="wrap">
        <div>
          <div className="brand" style={{ color: '#fff' }}>
            <img src="/logo.png" alt="" />
            Karsy
          </div>
          <p style={{ marginTop: 12, maxWidth: 300 }}>{t('footer.tagline')}</p>
          <div className="footlang" role="group" aria-label={t('footer.language')}>
            <Icon name="globe" size={16} />
            {(['es', 'en'] as const).map((l) => (
              <button key={l} className={lang === l ? 'on' : ''} aria-pressed={lang === l} onClick={() => setLang(l)}>
                {l.toUpperCase()}
              </button>
            ))}
          </div>
        </div>
        <div>
          <h4>{t('footer.explore')}</h4>
          <button onClick={go('/')}>{t('footer.vehicles')}</button>
          <button onClick={go('/lotes')}>{t('footer.lots')}</button>
          <button onClick={go('/publicar')}>{t('footer.publish')}</button>
        </div>
        <div>
          <h4>{t('footer.account')}</h4>
          <button onClick={go('/perfil')}>{t('nav.myProfile')}</button>
          <button onClick={go('/panel')}>{t('nav.myDashboard')}</button>
          <button onClick={go('/favoritos')}>{t('nav.favorites')}</button>
        </div>
        <div>
          <h4>{t('footer.legal')}</h4>
          <button onClick={go('/terminos')}>{t('legal.terms')}</button>
          <button onClick={go('/privacidad/politicas')}>{t('legal.policy')}</button>
        </div>
      </div>
      <div className="wrap legal">© {new Date().getFullYear()} Karsy</div>
    </footer>
  )
}

/** Barra superior + contenido + pie, para las pantallas del marketplace y de la cuenta. */
export function Layout() {
  const { notice, clearNotice } = useSession()
  const { toast } = useUi()

  // Sesión cerrada sola (cuenta suspendida, etc.): se avisa una vez.
  useEffect(() => {
    if (notice) {
      toast(notice)
      clearNotice()
    }
  }, [notice, clearNotice, toast])

  return (
    <>
      <TopNav />
      <main>
        <Outlet />
      </main>
      <Footer />
    </>
  )
}
