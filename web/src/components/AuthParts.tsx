import { useEffect, useRef, useState, type ReactNode } from 'react'
import { VolverButton } from './VolverButton'
import { EMAIL_CODE_LENGTH } from '../data/auth'
import { useI18n } from '../i18n/I18nContext'
import { PASSWORD_RULES } from '../lib/formRules'
import { Icon, type IconName } from './Icon'
import { Brand } from './TopNav'

// Piezas compartidas por las pantallas de acceso (login, registro, verificación, recuperación).

/**
 * Fotos del panel izquierdo: las mismas del carrusel de bienvenida de la app
 * (CarRepository.welcomeCarousel). Son material fijo, no publicaciones de usuarios.
 */
const CARRUSEL = ['1767749995450-7b63ab7cd4fd', '1580273916550-e323be2ae537', '1571987502227-9231b837d92a', '1758411898152-5fde4b5eef56'].map(
  (id) => `https://images.unsplash.com/photo-${id}?w=1200&h=1500&fit=crop&auto=format`,
)
const CAMBIO_MS = 5000

/** Pantalla dividida: carrusel de fotos con mensaje a la izquierda y formulario a la derecha. [photo] = foto inicial. */
export function AuthShell({ title, sub, photo = 0, children }: { title: string; sub: string; photo?: number; children: ReactNode }) {
  const { t } = useI18n()
  const [actual, setActual] = useState(photo % CARRUSEL.length)

  // Avanza cada 5 s (igual que la app); al elegir una foto, el conteo vuelve a empezar.
  useEffect(() => {
    const id = window.setTimeout(() => setActual((i) => (i + 1) % CARRUSEL.length), CAMBIO_MS)
    return () => window.clearTimeout(id)
  }, [actual])

  return (
    <div className="auth">
      <div className="authart">
        <div className="authslides" aria-hidden="true">
          {CARRUSEL.map((src, i) => (
            <img key={src} src={src} alt="" className={i === actual ? 'on' : ''} />
          ))}
        </div>
        <div className="grad" />
        <div>
          <Brand light />
        </div>
        <div className="col" style={{ gap: 12, display: 'flex', flexDirection: 'column' }}>
          <h1>{title}</h1>
          <p style={{ color: '#C9D6E2', maxWidth: 420 }}>{sub}</p>
          <div className="authdots">
            {CARRUSEL.map((src, i) => (
              <button key={src} className={i === actual ? 'on' : ''} aria-label={t('auth.slide', { n: i + 1 })} aria-current={i === actual} onClick={() => setActual(i)} />
            ))}
          </div>
        </div>
      </div>
      <div className="authform">
        <div className="inner">{children}</div>
      </div>
    </div>
  )
}

export function BackButton({ onClick }: { onClick: () => void }) {
  const { t } = useI18n()
  return (
    <div style={{ alignSelf: 'flex-start' }}>
      <VolverButton onClick={onClick} label={t('auth.back')} />
    </div>
  )
}

/** Error general del formulario. */
export function AlertBox({ message, children }: { message: string | null; children?: ReactNode }) {
  if (!message) return null
  return (
    <div className="alertbox" role="alert">
      <Icon name="alert" size={18} />
      <div>
        {message}
        {children}
      </div>
    </div>
  )
}

/** Campo de contraseña con botón para mostrarla u ocultarla. */
export function PasswordInput({
  id,
  value,
  onChange,
  placeholder,
  icon,
  bad,
  onBlur,
  autoComplete = 'current-password',
}: {
  id: string
  value: string
  onChange: (v: string) => void
  placeholder?: string
  icon?: IconName
  bad?: boolean
  onBlur?: () => void
  autoComplete?: string
}) {
  const { t } = useI18n()
  const [ver, setVer] = useState(false)
  const input = (
    <input
      className={`input ${bad ? 'bad' : ''}`}
      id={id}
      type={ver ? 'text' : 'password'}
      value={value}
      placeholder={placeholder}
      autoComplete={autoComplete}
      onChange={(e) => onChange(e.target.value)}
      onBlur={onBlur}
    />
  )
  return (
    <div className="pwdwrap">
      {icon ? (
        <div className="inputicon">
          <Icon name={icon} size={18} />
          {input}
        </div>
      ) : (
        input
      )}
      <button type="button" className="eyebtn" aria-label={ver ? t('auth.hidePassword') : t('auth.showPassword')} onClick={() => setVer((v) => !v)}>
        <Icon name={ver ? 'x' : 'eye'} size={18} />
      </button>
    </div>
  )
}

/** Requisitos de la contraseña, en verde los que ya cumple. */
export function PasswordRulesList({ password }: { password: string }) {
  const { t } = useI18n()
  return (
    <div className="rules">
      {PASSWORD_RULES.map(([clave, ok]) => {
        const cumple = ok(password)
        return (
          <span key={clave} className={cumple ? 'ok' : ''}>
            <Icon name={cumple ? 'check' : 'x'} size={14} />
            {t(clave)}
          </span>
        )
      })}
    </div>
  )
}

/** Cajitas del código de 6 dígitos: avanza solo, Retroceso regresa y acepta pegar el código completo. */
export function OtpInput({ value, onChange, onComplete }: { value: string; onChange: (v: string) => void; onComplete?: (v: string) => void }) {
  const { t } = useI18n()
  const refs = useRef<(HTMLInputElement | null)[]>([])
  const digits = Array.from({ length: EMAIL_CODE_LENGTH }, (_, i) => value[i] ?? '')

  /** Escribe [chars] desde la cajita [desde]; un solo dígito reemplaza solo esa cajita. */
  const set = (chars: string, desde: number) => {
    const inicio = Math.min(desde, value.length)
    const resto = chars.length === 1 ? value.slice(inicio + 1) : ''
    const next = (value.slice(0, inicio) + chars + resto).slice(0, EMAIL_CODE_LENGTH)
    onChange(next)
    refs.current[Math.min(inicio + chars.length, EMAIL_CODE_LENGTH - 1)]?.focus()
    if (next.length === EMAIL_CODE_LENGTH) onComplete?.(next)
  }

  useEffect(() => {
    refs.current[0]?.focus()
  }, [])

  return (
    <div className="otp">
      {digits.map((d, i) => (
        <input
          key={i}
          ref={(el) => {
            refs.current[i] = el
          }}
          className={d ? 'f' : ''}
          inputMode="numeric"
          autoComplete={i === 0 ? 'one-time-code' : 'off'}
          maxLength={EMAIL_CODE_LENGTH}
          aria-label={t('auth.otpDigit', { n: i + 1 })}
          value={d}
          onFocus={(e) => e.target.select()}
          onChange={(e) => {
            const v = e.target.value.replace(/\D/g, '')
            if (!v) return
            // Al escribir sobre una cajita llena llega su dígito anterior + el nuevo; un texto largo es el código pegado.
            set(v.length > 2 ? v : v.slice(-1), i)
          }}
          onKeyDown={(e) => {
            if (e.key === 'Backspace') {
              e.preventDefault()
              const hasta = d ? i : Math.max(0, i - 1)
              onChange(value.slice(0, hasta))
              refs.current[hasta]?.focus()
            } else if (e.key === 'ArrowLeft') refs.current[i - 1]?.focus()
            else if (e.key === 'ArrowRight') refs.current[i + 1]?.focus()
          }}
          onPaste={(e) => {
            e.preventDefault()
            const v = e.clipboardData.getData('text').replace(/\D/g, '')
            if (v) set(v, 0)
          }}
        />
      ))}
    </div>
  )
}

/** Espera para pedir otro código: Supabase no manda otro correo al mismo usuario antes de 60 s. */
export function useCountdown() {
  const [seconds, setSeconds] = useState(0)
  useEffect(() => {
    if (seconds <= 0) return
    const t = window.setTimeout(() => setSeconds((s) => s - 1), 1000)
    return () => window.clearTimeout(t)
  }, [seconds])
  return { seconds, start: () => setSeconds(60) }
}

/** "¿No recibiste el código? Reenviar código", o la espera mientras no se puede pedir otro. */
export function ResendRow({ sending, seconds, onResend }: { sending: boolean; seconds: number; onResend: () => void }) {
  const { t } = useI18n()
  return (
    <p className="muted" style={{ fontSize: 13, textAlign: 'center' }}>
      {sending ? (
        t('auth.sendingCode')
      ) : seconds > 0 ? (
        t('auth.resendWait', { s: seconds })
      ) : (
        <>
          {t('auth.noCode')}{' '}
          <button className="linkbtn" onClick={onResend}>
            {t('auth.resendCode')}
          </button>
        </>
      )}
    </p>
  )
}
