import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { AlertBox, AuthShell, BackButton, OtpInput, PasswordInput, PasswordRulesList, ResendRow, useCountdown } from '../../components/AuthParts'
import { Icon } from '../../components/Icon'
import { EMAIL_CODE_LENGTH, mensajeUsuario, resetPassword, sendPasswordResetCode, verifyPasswordResetCode } from '../../data/auth'
import { supabase } from '../../lib/supabase'
import { passwordError } from '../../lib/formRules'
import { setRecoveryMode } from '../../session/SessionContext'

const PASOS = ['Correo', 'Código', 'Contraseña', 'Listo']

/**
 * "¿Olvidaste tu contraseña?": correo → código → contraseña nueva (PasswordRecoveryViewModel).
 * Supabase genera, manda y valida el código; al validarlo abre una sesión temporal que
 * solo sirve para cambiar la contraseña y después se cierra.
 */
export function Recover() {
  const navigate = useNavigate()
  const location = useLocation()
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
        void supabase.auth.signOut().finally(() => setRecoveryMode(false))
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
      setError('Escribe el correo de tu cuenta.')
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
      setError(`Escribe los ${EMAIL_CODE_LENGTH} dígitos del código.`)
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
      setError('Escribe tu nueva contraseña y confírmala.')
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
    ['Recuperar contraseña', 'Ingresa tu correo electrónico asociado a tu cuenta para enviarte un código de verificación.'],
    [
      'Código de verificación',
      <>
        Ingresa el código de {EMAIL_CODE_LENGTH} dígitos enviado a tu correo <b style={{ color: 'var(--ink)' }}>{email.trim()}</b>. Si no lo ves,
        revisa tu carpeta de spam.
      </>,
    ],
    ['Nueva contraseña', 'Crea una contraseña segura y fácil de recordar para acceder a tu cuenta.'],
    ['¡Contraseña actualizada!', 'Tu contraseña se ha cambiado con éxito. Ya puedes iniciar sesión con tu nueva credencial.'],
  ]

  return (
    <AuthShell title="Recupera el acceso a tu cuenta." sub="Te enviaremos un código a tu correo para que crees una contraseña nueva." photo={9}>
      {step < 3 && <BackButton onClick={atras} />}
      <div className="seg" style={{ alignSelf: 'flex-start', pointerEvents: 'none' }} aria-label={`Paso ${step + 1} de 4`}>
        {PASOS.map((t, i) => (
          <button key={t} className={i === step ? 'on' : ''} tabIndex={-1}>
            {t}
          </button>
        ))}
      </div>
      {step === 3 && (
        <span className="nic" style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--success-l)', color: 'var(--success)' }}>
          <Icon name="check" size={30} />
        </span>
      )}
      <h2>{textos[step][0]}</h2>
      <p className="muted">{textos[step][1]}</p>

      {step === 0 && (
        <form
          style={{ display: 'flex', flexDirection: 'column', gap: 18 }}
          onSubmit={(e) => {
            e.preventDefault()
            sendCode()
          }}
        >
          <div className="field">
            <label className="label" htmlFor="re">
              Correo electrónico
            </label>
            <div className="inputicon">
              <Icon name="mail" size={18} />
              <input className="input" id="re" type="email" autoComplete="email" placeholder="ejemplo@correo.com" value={email} onChange={(e) => setEmail(e.target.value)} autoFocus />
            </div>
          </div>
          <AlertBox message={error} />
          <button type="submit" className="btn btn-primary btn-block" style={{ height: 50 }} disabled={sending}>
            {sending ? 'Enviando código…' : 'Enviar código'}
          </button>
        </form>
      )}

      {step === 1 && (
        <>
          <OtpInput value={code} onChange={setCode} onComplete={(c) => void verifyCode(c)} />
          <AlertBox message={error} />
          <ResendRow sending={sending} seconds={countdown.seconds} onResend={() => countdown.seconds === 0 && void enviar(() => undefined)} />
          <button className="btn btn-primary btn-block" style={{ height: 50 }} disabled={loading} onClick={() => void verifyCode()}>
            {loading ? 'Verificando…' : 'Verificar'}
          </button>
        </>
      )}

      {step === 2 && (
        <form
          style={{ display: 'flex', flexDirection: 'column', gap: 14 }}
          onSubmit={(e) => {
            e.preventDefault()
            void guardar()
          }}
        >
          <div className="field">
            <label className="label" htmlFor="rn">
              Nueva contraseña
            </label>
            <PasswordInput id="rn" value={newPassword} onChange={setNewPassword} autoComplete="new-password" placeholder="Ingresa una contraseña" />
          </div>
          <PasswordRulesList password={newPassword} />
          <div className="field">
            <label className="label" htmlFor="rc">
              Confirmar contraseña
            </label>
            <PasswordInput id="rc" value={confirm} onChange={setConfirm} autoComplete="new-password" placeholder="Repite tu contraseña" bad={mismatch} />
            {mismatch && <span className="ferr">Las contraseñas no coinciden.</span>}
          </div>
          <AlertBox message={error} />
          <button type="submit" className="btn btn-primary btn-block" style={{ height: 50 }} disabled={loading}>
            {loading ? 'Guardando…' : 'Restablecer contraseña'}
          </button>
        </form>
      )}

      {step === 3 && (
        <button className="btn btn-primary btn-block" style={{ height: 50 }} onClick={() => navigate('/login', { replace: true })}>
          Iniciar sesión
        </button>
      )}
    </AuthShell>
  )
}
