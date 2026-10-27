import { useRef, useState, type DragEvent } from 'react'
import { errorConstancia } from '../data/auth'
import { useI18n } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { Icon } from './Icon'

const tamano = (b: number) => (b < 1024 * 1024 ? `${Math.max(1, Math.round(b / 1024))} KB` : `${(b / (1024 * 1024)).toFixed(1)} MB`)

/**
 * Campo para subir la Constancia de Situación Fiscal (registro de lote y perfil).
 * Acepta PDF, JPG o PNG de hasta 5 MB, con clic o arrastrando; un archivo no válido no se acepta.
 */
export function ConstanciaField({ file, onChange, showLabel = true }: { file: File | null; onChange: (f: File | null) => void; showLabel?: boolean }) {
  const { t } = useI18n()
  const input = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<Clave | null>(null)
  const [encima, setEncima] = useState(false)

  const elegir = (f: File | undefined) => {
    if (!f) return
    const e = errorConstancia(f)
    setError(e)
    onChange(e ? null : f)
  }
  const soltar = (e: DragEvent) => {
    e.preventDefault()
    setEncima(false)
    elegir(e.dataTransfer.files?.[0])
  }

  return (
    <div className="field" data-field="constancia">
      {showLabel && <label htmlFor="constancia">{t('auth.csf.label')}</label>}
      <span className="muted text-xs">
        {t('auth.csf.help')}
      </span>
      {file ? (
        <div className="filechip">
          <span className="nic">
            <Icon name="doc" size={20} />
          </span>
          <div className="grow" style={{ minWidth: 0 }}>
            <b className="ellipsis ink" style={{ display: 'block' }}>
              {file.name}
            </b>
            <span className="muted text-xs">
              {tamano(file.size)}
            </span>
          </div>
          <button type="button" className="linkbtn" onClick={() => input.current?.click()}>
            {t('auth.csf.change')}
          </button>
          <button type="button" className="linkbtn" style={{ color: 'var(--error)' }} onClick={() => onChange(null)}>
            {t('auth.csf.remove')}
          </button>
        </div>
      ) : (
        <button
          type="button"
          id="constancia"
          className={`dropzone ${encima ? 'over' : ''} ${error ? 'bad' : ''}`}
          onClick={() => input.current?.click()}
          onDragOver={(e) => {
            e.preventDefault()
            setEncima(true)
          }}
          onDragLeave={() => setEncima(false)}
          onDrop={soltar}
        >
          <Icon name="doc" size={24} />
          <span>
            <b>{t('auth.csf.drop')}</b>
            <br />
            <span className="muted text-xs">
              {t('auth.csf.formats')}
            </span>
          </span>
        </button>
      )}
      {error && <span className="ferr">{t(error)}</span>}
      <input
        ref={input}
        type="file"
        accept=".pdf,.jpg,.jpeg,.png"
        hidden
        onChange={(e) => {
          elegir(e.target.files?.[0])
          e.target.value = ''
        }}
      />
    </div>
  )
}

/**
 * Insignia "Verificado" (píldora azul con escudo blanco) a la derecha del nombre de un usuario
 * verificado (cuentas.verificado). Sirve igual para lotes y particulares. [compact] = versión
 * chica de las tarjetas de la lista de lotes. Para que un nombre largo se corte y la insignia
 * siempre se vea, van juntos en .verified-name.
 */
export function VerifiedBadge({ compact = false }: { compact?: boolean }) {
  const { t } = useI18n()
  return (
    <span className={`verified-badge ${compact ? 'compact' : ''}`} aria-label={t('perfil.verifiedA11y')}>
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 2l8 3v6c0 5-3.4 9.4-8 11-4.6-1.6-8-6-8-11V5z" fill="#FFFFFF" />
        <path d="M8 12l3 3 5-5.5" fill="none" stroke="#2F6DB0" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
      {t('perfil.verified')}
    </span>
  )
}

/** Solo el escudo de la insignia, sin la palabra "Verificado" (lista de Usuarios del panel de admin). */
export function VerifiedShield() {
  const { t } = useI18n()
  return (
    <span className="verified-badge solo" title={t('perfil.verifiedA11y')} aria-label={t('perfil.verifiedA11y')}>
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 2l8 3v6c0 5-3.4 9.4-8 11-4.6-1.6-8-6-8-11V5z" fill="#FFFFFF" />
        <path d="M8 12l3 3 5-5.5" fill="none" stroke="#2F6DB0" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </span>
  )
}
