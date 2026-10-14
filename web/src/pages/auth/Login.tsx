import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { AlertBox, AuthShell, BackButton, PasswordInput } from '../../components/AuthParts'
import { Icon } from '../../components/Icon'
import { useUi } from '../../components/Ui'
import { EmailNotVerifiedError, mensajeUsuario, signIn } from '../../data/auth'
import { useI18n } from '../../i18n/I18nContext'
import { useSession } from '../../session/SessionContext'

/** A dónde volver después de entrar (la pantalla que pidió iniciar sesión) o al inicio. */
export function destinoDe(state: unknown): string {
  const from = (state as { from?: string } | null)?.from
  return from && from.startsWith('/') && !from.startsWith('/login') ? from : '/'
}

/** Inicio de sesión (LoginScreen + LoginViewModel de la app). */
export function Login() {
  const navigate = useNavigate()
  const location = useLocation()
  const { account, reload, clearNotice } = useSession()
  const { toast } = useUi()
  const { t } = useI18n()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Ya hay sesión: no tiene caso mostrar el formulario.
  if (account && !loading) return <Navigate to={destinoDe(location.state)} replace />

  const entrar = async () => {
    if (loading) return
    if (!email.trim() || !password) {
      setError(t('auth.login.missing'))
      return
    }
    setLoading(true)
    setError(null)
    try {
      await signIn(email, password)
      // El rol y el estado de la cuenta se leen de public.cuentas (sin cuenta o suspendida → se cierra).
      const r = await reload()
      if (!r.account) {
        clearNotice()
        setError(r.notice ?? t('auth.loginFailed'))
        setLoading(false)
        return
      }
      toast(t('auth.login.hello', { nombre: r.account.nombre.split(' ')[0] }))
      navigate(destinoDe(location.state), { replace: true })
    } catch (e) {
      setLoading(false)
      if (e instanceof EmailNotVerifiedError) {
        navigate('/verificar', { state: { email: e.email, sendCode: true, from: destinoDe(location.state) } })
      } else setError(mensajeUsuario(e))
    }
  }

  return (
    <AuthShell title={t('auth.login.artTitle')} sub={t('auth.login.artSub')}>
      <BackButton onClick={() => navigate('/')} />
      <h2>{t('auth.login.title')}</h2>
      <form
        style={{ display: 'flex', flexDirection: 'column', gap: 18 }}
        onSubmit={(e) => {
          e.preventDefault()
          void entrar()
        }}
      >
        <div className="field">
          <label className="label" htmlFor="le">
            {t('auth.email')}
          </label>
          <div className="inputicon">
            <Icon name="mail" size={18} />
            <input
              className="input"
              id="le"
              type="email"
              autoComplete="email"
              placeholder={t('auth.emailPlaceholder')}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoFocus
            />
          </div>
        </div>
        <div className="field">
          <label className="label" htmlFor="lp">
            {t('auth.login.password')}
          </label>
          <PasswordInput id="lp" icon="lock" value={password} onChange={setPassword} placeholder={t('auth.login.passwordPlaceholder')} />
        </div>
        <button
          type="button"
          className="linkbtn"
          style={{ alignSelf: 'flex-end' }}
          onClick={() => navigate('/recuperar', { state: { email: email.trim() } })}
        >
          {t('auth.login.forgot')}
        </button>
        <AlertBox message={error} />
        <button type="submit" className="btn btn-primary btn-block" style={{ height: 50 }} disabled={loading}>
          {loading ? t('auth.login.loading') : t('auth.login.submit')}
        </button>
      </form>
      <p className="muted" style={{ textAlign: 'center' }}>
        {t('auth.login.noAccount')}{' '}
        <button className="linkbtn" onClick={() => navigate('/registro')}>
          {t('auth.login.register')}
        </button>
      </p>
    </AuthShell>
  )
}
