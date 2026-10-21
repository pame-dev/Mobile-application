import { useEffect, useRef, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { AlertBox, AuthShell, BackButton, OtpInput, ResendRow, useCountdown } from '../../components/AuthParts'
import { Icon } from '../../components/Icon'
import { useUi } from '../../components/Ui'
import { EMAIL_CODE_LENGTH, mensajeUsuario, sendVerificationCode, uploadPendingAvatar, uploadPendingConstancia, uploadPendingCurp, verifyEmail } from '../../data/auth'
import { useI18n } from '../../i18n/I18nContext'
import { useSession } from '../../session/SessionContext'
import { destinoDe } from './Login'

interface VerifyState {
  email?: string
  /** Desde el login se pide un código nuevo; desde el registro Supabase ya lo mandó. */
  sendCode?: boolean
  from?: string
}

/** Verificación del correo con el código de Supabase (VerifyEmailScreen de la app). */
export function VerifyEmail() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = (location.state ?? {}) as VerifyState
  const email = state.email ?? ''
  const { reload, clearNotice } = useSession()
  const { toast } = useUi()
  const { t } = useI18n()
  const countdown = useCountdown()
  const [code, setCode] = useState('')
  const [verifying, setVerifying] = useState(false)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const started = useRef(false)

  const reenviar = async () => {
    if (sending || countdown.seconds > 0) return
    setSending(true)
    setError(null)
    try {
      await sendVerificationCode(email)
      setCode('')
      countdown.start()
    } catch (e) {
      setError(mensajeUsuario(e))
    }
    setSending(false)
  }

  useEffect(() => {
    if (!email || started.current) return
    started.current = true
    if (state.sendCode) void reenviar()
    else countdown.start()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Se entró directo a /verificar (sin venir del login o del registro).
  if (!email) return <Navigate to="/login" replace />

  const verificar = async (token = code) => {
    if (verifying) return
    if (token.length < EMAIL_CODE_LENGTH) {
      setError(t('auth.codeDigits', { n: EMAIL_CODE_LENGTH }))
      return
    }
    setVerifying(true)
    setError(null)
    try {
      await verifyEmail(email, token)
      // Ya hay sesión: se sube el logo que se eligió en el registro (si hubo).
      await uploadPendingAvatar(email).catch(() => undefined)
      // Y la constancia fiscal del lote (opcional); si falla, la cuenta sigue y se avisa.
      const constanciaFallo = await uploadPendingConstancia(email).then(() => false, () => true)
      // Y la CURP del particular (opcional); si falla, la puede mandar desde su perfil.
      const curpFallo = await uploadPendingCurp(email).then(() => false, () => true)
      const r = await reload()
      if (!r.account) {
        clearNotice()
        setError(r.notice ?? t('auth.loginFailed'))
        setVerifying(false)
        return
      }
      toast(t(constanciaFallo ? 'auth.csf.uploadFailed' : curpFallo ? 'auth.curp.sendFailed' : 'auth.verify.done'))
      navigate(destinoDe(state), { replace: true })
    } catch (e) {
      setError(mensajeUsuario(e))
      setVerifying(false)
    }
  }

  return (
    <AuthShell title={t('auth.verify.artTitle')} sub={t('auth.verify.artSub')} photo={8}>
      <BackButton onClick={() => navigate(-1)} />
      <span className="nic" style={{ width: 64, height: 64, borderRadius: 18, background: 'var(--teal-light)', color: 'var(--teal)' }}>
        <Icon name="mail" size={30} />
      </span>
      <h2>{t('auth.verify.title')}</h2>
      <p className="muted">
        {t('auth.verify.sub1', { n: EMAIL_CODE_LENGTH })} <b style={{ color: 'var(--ink)' }}>{email}</b>
        {t('auth.verify.sub2')}
      </p>
      <OtpInput value={code} onChange={setCode} onComplete={(c) => void verificar(c)} />
      <AlertBox message={error} />
      <ResendRow sending={sending} seconds={countdown.seconds} onResend={() => void reenviar()} />
      <button className="btn btn-primary btn-block" style={{ height: 50 }} disabled={verifying} onClick={() => void verificar()}>
        {verifying ? t('auth.verifying') : t('auth.verify')}
      </button>
    </AuthShell>
  )
}
