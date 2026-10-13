import { useEffect, useRef, useState, type ReactNode } from 'react'
import { visibleCars } from '../data/api'
import { EMAIL_CODE_LENGTH } from '../data/auth'
import { PASSWORD_RULES } from '../lib/formRules'
import { CarPhoto } from './CarPhoto'
import { Icon, type IconName } from './Icon'
import { Brand } from './TopNav'

// Piezas compartidas por las pantallas de acceso (login, registro, verificación, recuperación).

/** Fotos de vehículos publicados para el panel izquierdo (una sola consulta por visita). */
let fotos: Promise<string[]> | null = null
function fotosDeAutos(): Promise<string[]> {
  fotos ??= visibleCars()
    .then((cars) => cars.map((c) => c.imageUrl).filter((u): u is string => !!u))
    .catch(() => [])
  return fotos
}

/** Pantalla dividida: foto con mensaje a la izquierda y formulario a la derecha. */
export function AuthShell({ title, sub, photo = 0, children }: { title: string; sub: string; photo?: number; children: ReactNode }) {
  const [src, setSrc] = useState<string | null>(null)
  useEffect(() => {
    let vivo = true
    fotosDeAutos().then((f) => {
      if (vivo && f.length) setSrc(f[photo % f.length])
    })
    return () => {
      vivo = false
    }
  }, [photo])

  return (
    <div className="auth">
      <div className="authart">
        <CarPhoto src={src} />
        <div className="grad" />
        <div>
          <Brand light />
        </div>
        <div className="col" style={{ gap: 12, display: 'flex', flexDirection: 'column' }}>
          <h1>{title}</h1>
          <p style={{ color: '#C9D6E2', maxWidth: 420 }}>{sub}</p>
        </div>
      </div>
      <div className="authform">
        <div className="inner">{children}</div>
      </div>
    </div>
  )
}

export function BackButton({ onClick }: { onClick: () => void }) {
  return (
    <button className="btn btn-ghost btn-sm" style={{ alignSelf: 'flex-start' }} onClick={onClick}>
      <Icon name="back" size={16} />
      Regresar
    </button>
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
      <button type="button" className="eyebtn" aria-label={ver ? 'Ocultar contraseña' : 'Mostrar contraseña'} onClick={() => setVer((v) => !v)}>
        <Icon name={ver ? 'x' : 'eye'} size={18} />
      </button>
    </div>
  )
}

/** Requisitos de la contraseña, en verde los que ya cumple. */
export function PasswordRulesList({ password }: { password: string }) {
  return (
    <div className="rules">
      {PASSWORD_RULES.map(([t, ok]) => {
        const cumple = ok(password)
        return (
          <span key={t} className={cumple ? 'ok' : ''}>
            <Icon name={cumple ? 'check' : 'x'} size={14} />
            {t}
          </span>
        )
      })}
    </div>
  )
}

/** Cajitas del código de 6 dígitos: avanza solo, Retroceso regresa y acepta pegar el código completo. */
export function OtpInput({ value, onChange, onComplete }: { value: string; onChange: (v: string) => void; onComplete?: (v: string) => void }) {
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
          aria-label={`Dígito ${i + 1}`}
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
  return (
    <p className="muted" style={{ fontSize: 13, textAlign: 'center' }}>
      {sending ? (
        'Enviando código…'
      ) : seconds > 0 ? (
        `Puedes pedir otro código en ${seconds} s`
      ) : (
        <>
          ¿No recibiste el código?{' '}
          <button className="linkbtn" onClick={onResend}>
            Reenviar código
          </button>
        </>
      )}
    </p>
  )
}
