import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { AlertBox, AuthShell, BackButton, OtpInput, PasswordInput, PasswordRulesList, ResendRow, useCountdown } from '../../components/AuthParts'
import { Icon } from '../../components/Icon'
import { EMAIL_CODE_LENGTH, mensajeUsuario, resetPassword, sendPasswordResetCode, verifyPasswordResetCode } from '../../data/auth'
import { useI18n } from '../../i18n/I18nContext'
import type { Clave } from '../../i18n/es'
import { cerrarSesionLocal } from '../../lib/supabase'
import { passwordError } from '../../lib/formRules'
import { setRecoveryMode } from '../../session/SessionContext'

const PASOS: Clave[] = ['auth.recover.stepEmail', 'auth.recover.stepCode', 'auth.recover.stepPassword', 'auth.recover.stepDone']

/**
 * "¿Olvidaste tu contraseña?": correo → código → contraseña nueva (PasswordRecoveryViewModel).
 * Supabase genera, manda y valida el código; al validarlo abre una sesión temporal que
 * solo sirve para cambiar la contraseña y después se cierra.
 */
export function Recover() {
  const navigate = useNavigate()
  const location = useLocation()
  const { t } = useI18n()
  const [step, setStep] = useState(0)
  const [email, setEmail] = useState(((location.state as { email?: string } | null)?.email ?? '').trim())
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [sentTo, setSentTo] = useState<string | null>(null)
  const [sending, setSending] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const countdown = useCountdown()
  const mismatch = confirm.length > 0 && newPassword !== confirm

  /** El código ya se validó y la sesión temporal sigue abierta. */
  const sesionTemporal = useRef(false)

  // Si se sale a medio camino, la sesión temporal del código se cierra.
  useEffect(
    () => () => {
      if (sesionTemporal.current) {
        sesionTemporal.current = false
        void cerrarSesionLocal().finally(() => setRecoveryMode(false))
      } else setRecoveryMode(false)
    },
    [],
  )

  const enviar = async (onSent: () => void) => {
    if (sending) return
    setSending(true)
    setError(null)
    try {
      await sendPasswordResetCode(email)
      setSentTo(email.trim())
      setCode('')
      countdown.start()
      onSent()
    } catch (e) {
      setError(mensajeUsuario(e))
    }
    setSending(false)
  }

  const sendCode = () => {
    if (!email.includes('@')) {
      setError(t('auth.recover.accountEmail'))
      return
    }
    // Si regresó al paso 1 sin cambiar el correo, el código de hace unos segundos sigue sirviendo.
    if (countdown.seconds > 0 && sentTo === email.trim()) {
      setError(null)
      setStep(1)
      return
    }
    void enviar(() => setStep(1))
  }

  const verifyCode = async (token = code) => {
    if (loading) return
    if (token.length < EMAIL_CODE_LENGTH) {
      setError(t('auth.codeDigits', { n: EMAIL_CODE_LENGTH }))
      return
    }
    setLoading(true)
    setError(null)
    setRecoveryMode(true)
    try {
      await verifyPasswordResetCode(email, token)
      sesionTemporal.current = true
      setStep(2)
    } catch (e) {
      setRecoveryMode(false)
      setError(mensajeUsuario(e))
    }
    setLoading(false)
  }

  const guardar = async () => {
    if (loading) return
    if (!newPassword || !confirm) {
      setError(t('auth.recover.newRequired'))
      return
    }
    const err = passwordError(newPassword, email)
    if (err) {
      setError(err)
      return
    }
    if (mismatch) return // el aviso "no coinciden" ya está en pantalla
    setLoading(true)
    setError(null)
    try {
      await resetPassword(newPassword)
      sesionTemporal.current = false
      setRecoveryMode(false)
      setStep(3)
    } catch (e) {
      setError(mensajeUsuario(e))
    }
    setLoading(false)
  }

  /** Desde la contraseña nueva se cancela todo: el código ya se usó, así que se cierra la sesión temporal. */
  const atras = () => {
    setError(null)
    if (step === 0 || step === 2) navigate('/login', { replace: true })
    else setStep(step - 1)
  }

  const textos: [string, ReactNode][] = [
    [t('auth.recover.title0'), t('auth.recover.sub0')],
    [
      t('auth.recover.title1'),
      <>
        {t('auth.recover.sub1a', { n: EMAIL_CODE_LENGTH })} <b className="ink">{email.trim()}</b>
        {t('auth.recover.sub1b')}
      </>,
    ],
    [t('auth.recover.title2'), t('auth.recover.sub2')],
    [t('auth.recover.title3'), t('auth.recover.sub3')],
  ]

  return (
    <AuthShell title={t('auth.recover.artTitle')} sub={t('auth.recover.artSub')} photo={9}>
      {step < 3 && <BackButton onClick={atras} />}
      <div className="seg" style={{ alignSelf: 'flex-start', pointerEvents: 'none' }} aria-label={t('auth.recover.stepOf', { n: step + 1 })}>
        {PASOS.map((clave, i) => (
          <button key={clave} className={i === step ? 'on' : ''} tabIndex={-1}>
            {t(clave)}
          </button>
        ))}
      </div>
      {step === 3 && (
        <span className="nic done-icon">
          <Icon name="check" size={30} />
        </span>
      )}
      <h1>{textos[step][0]}</h1>
      <p className="muted">{textos[step][1]}</p>

      {step === 0 && (
        <form className="col gap-4"
          onSubmit={(e) => {
            e.preventDefault()
            sendCode()
          }}
        >
          <div className="field">
            <label className="label" htmlFor="re">
              {t('auth.email')}
            </label>
            <div className="inputicon">
              <Icon name="mail" size={18} />
              <input className="input" id="re" type="email" autoComplete="email" placeholder={t('auth.emailPlaceholder')} value={email} onChange={(e) => setEmail(e.target.value)} autoFocus />
            </div>
          </div>
          <AlertBox message={error} />
          <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={sending}>
            {sending ? t('auth.sendingCode') : t('auth.recover.send')}
          </button>
        </form>
      )}

      {step === 1 && (
        <>
          <OtpInput value={code} onChange={setCode} onComplete={(c) => void verifyCode(c)} />
          <AlertBox message={error} />
          <ResendRow sending={sending} seconds={countdown.seconds} onResend={() => countdown.seconds === 0 && void enviar(() => undefined)} />
          <button className="btn btn-primary btn-block btn-lg" disabled={loading} onClick={() => void verifyCode()}>
            {loading ? t('auth.verifying') : t('auth.verify')}
          </button>
        </>
      )}

      {step === 2 && (
        <form className="col gap-3"
          onSubmit={(e) => {
            e.preventDefault()
            void guardar()
          }}
        >
          <div className="field">
            <label className="label" htmlFor="rn">
              {t('auth.recover.newPassword')}
            </label>
            <PasswordInput id="rn" value={newPassword} onChange={setNewPassword} autoComplete="new-password" placeholder={t('auth.passwordPlaceholder')} />
          </div>
          <PasswordRulesList password={newPassword} />
          <div className="field">
            <label className="label" htmlFor="rc">
              {t('auth.confirmPassword')}
            </label>
            <PasswordInput id="rc" value={confirm} onChange={setConfirm} autoComplete="new-password" placeholder={t('auth.confirmPlaceholder')} bad={mismatch} />
            {mismatch && <span className="ferr">{t('auth.passwordMismatch')}</span>}
          </div>
          <AlertBox message={error} />
          <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={loading}>
            {loading ? t('common.saving') : t('auth.recover.reset')}
          </button>
        </form>
      )}

      {step === 3 && (
        <button className="btn btn-primary btn-block btn-lg" onClick={() => navigate('/login', { replace: true })}>
          {t('auth.login')}
        </button>
      )}
    </AuthShell>
  )
}
