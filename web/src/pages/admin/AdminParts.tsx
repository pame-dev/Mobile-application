import { useState, type ReactNode } from 'react'
import { AlertBox } from '../../components/AuthParts'
import { Icon } from '../../components/Icon'
import { ModalHead, useUi } from '../../components/Ui'
import { mensajeUsuario } from '../../data/auth'
import { traducir, useI18n } from '../../i18n/I18nContext'
import type { Clave } from '../../i18n/es'
import { iniciales } from '../../lib/format'

/** Mínimo de caracteres de un motivo del admin (MIN_ADMIN_REASON en la app). */
export const MIN_MOTIVO = 10

export const BADGE: Record<string, string> = {
  Activo: 'b-succ',
  Suspendido: 'b-dang',
  Pendiente: 'b-warn',
  'Cambios pendientes': 'b-warn',
  Rechazado: 'b-dang',
  Deshabilitado: 'b-neu',
  Pausado: 'b-neu',
  Vendido: 'b-info',
  Atendido: 'b-succ',
  Descartado: 'b-neu',
  Aprobada: 'b-succ',
  Rechazada: 'b-dang',
}

/** Clave de traducción de cada estado que se muestra (el valor interno sigue en español). */
const ESTADO_CLAVE: Record<string, Clave> = {
  Activo: 'admin.st.active',
  Suspendido: 'admin.st.suspended',
  Pendiente: 'admin.st.pending',
  'Cambios pendientes': 'admin.st.pendingChanges',
  Rechazado: 'admin.st.rejected',
  Deshabilitado: 'admin.st.disabled',
  Pausado: 'admin.st.paused',
  Vendido: 'admin.st.sold',
  Atendido: 'admin.st.resolved',
  Descartado: 'admin.st.dismissed',
  Aprobada: 'admin.st.approvedF',
  Rechazada: 'admin.st.rejectedF',
}

/** Estado traducido para mostrar; si no se conoce, se muestra tal cual. */
export const etiquetaEstado = (s: string) => (ESTADO_CLAVE[s] ? traducir(ESTADO_CLAVE[s]) : s)

export function Badge({ s }: { s: string }) {
  const { t } = useI18n()
  return <span className={`badge ${BADGE[s] ?? 'b-neu'}`}>{ESTADO_CLAVE[s] ? t(ESTADO_CLAVE[s]) : s}</span>
}

const COLORES = ['#52A3AA', '#8B5CF6', '#3B82F6', '#E65B5B', '#1A4A6E', '#F59E0B', '#0D2B45', '#22C55E']
/** Círculo con iniciales; el color sale del nombre para que siempre sea el mismo. */
export function Ini({ name, src, square = false }: { name: string; src?: string | null; square?: boolean }) {
  const color = COLORES[[...name].reduce((a, c) => a + c.charCodeAt(0), 0) % COLORES.length]
  return (
    <span className="ini" style={{ background: color, borderRadius: square ? 10 : undefined }}>
      {src ? <img src={src} alt="" /> : iniciales(name)}
    </span>
  )
}

export const fecha = (iso: string) => new Date(iso).toLocaleDateString(traducir('admin.locale'), { day: 'numeric', month: 'short', year: 'numeric' })

export function AdminHead({ title, desc, right }: { title: string; desc: string; right?: ReactNode }) {
  return (
    <div className="row" style={{ justifyContent: 'space-between', flexWrap: 'wrap', marginBottom: 20, gap: 12 }}>
      <div>
        <h1 style={{ fontSize: 28 }}>{title}</h1>
        <p className="muted">{desc}</p>
      </div>
      {right}
    </div>
  )
}

export function Tabs<T extends string>({ value, onChange, options }: { value: T; onChange: (v: T) => void; options: [T, string][] }) {
  return (
    <div className="seg" style={{ marginBottom: 16 }}>
      {options.map(([v, t]) => (
        <button key={v} className={value === v ? 'on' : ''} onClick={() => onChange(v)}>
          {t}
        </button>
      ))}
    </div>
  )
}

export function SearchBox({ value, onChange, placeholder }: { value: string; onChange: (v: string) => void; placeholder: string }) {
  return (
    <div className="inputicon" style={{ width: 300, maxWidth: '100%' }}>
      <Icon name="search" size={18} />
      <input className="input" placeholder={placeholder} aria-label={placeholder} value={value} onChange={(e) => onChange(e.target.value)} />
    </div>
  )
}

/**
 * Pide el motivo de una acción del admin (rechazar, deshabilitar, suspender, descartar).
 * Mínimo 10 caracteres; si la BD falla, el error se muestra aquí y no se cierra.
 */
export function ReasonModal({
  title,
  desc,
  hint,
  confirm,
  working,
  onConfirm,
}: {
  title: string
  desc: string
  hint: string
  confirm: string
  working: string
  onConfirm: (motivo: string) => Promise<void>
}) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  const [motivo, setMotivo] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const largo = motivo.trim().length
  return (
    <div className="modal">
      <ModalHead title={title} sub={desc} />
      <textarea className="input" placeholder={hint} value={motivo} maxLength={500} onChange={(e) => setMotivo(e.target.value)} autoFocus />
      <span className="mid" style={{ fontSize: 12, color: largo > 0 && largo < MIN_MOTIVO ? 'var(--error)' : undefined }}>
        {largo < MIN_MOTIVO ? t('admin.reasonMin', { min: MIN_MOTIVO, n: largo }) : t('admin.reasonCount', { n: largo })}
      </span>
      <AlertBox message={error} />
      <div className="grid2">
        <button className="btn btn-ghost" disabled={busy} onClick={closeModal}>
          {t('common.cancel')}
        </button>
        <button
          className="btn btn-danger"
          disabled={busy || largo < MIN_MOTIVO}
          onClick={async () => {
            setBusy(true)
            setError(null)
            try {
              await onConfirm(motivo.trim())
              closeModal()
            } catch (e) {
              setError(mensajeUsuario(e))
              setBusy(false)
            }
          }}
        >
          {busy ? working : confirm}
        </button>
      </div>
    </div>
  )
}

/** Gráfica de líneas acumuladas (crecimiento de la plataforma). */
export function LineChart({ labels, series, w = 620, h = 230 }: { labels: string[]; series: [string, number[]][]; w?: number; h?: number }) {
  const { t } = useI18n()
  const pl = 40
  const pb = 26
  const max = Math.max(5, ...series.flatMap(([, d]) => d))
  const paso = Math.pow(10, Math.floor(Math.log10(max / 4)))
  const tope = Math.ceil(max / 4 / paso) * paso * 4
  const n = Math.max(1, labels.length - 1)
  const X = (i: number) => pl + (i * (w - pl - 10)) / n
  const Y = (v: number) => h - pb - ((h - pb - 12) * v) / tope
  return (
    <svg className="chart" viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label={t('admin.chartGrowth')}>
      <g className="grid">
        {[0, 1, 2, 3, 4].map((k) => (
          <g key={k}>
            <line x1={pl} x2={w - 10} y1={Y((tope * k) / 4)} y2={Y((tope * k) / 4)} />
            <text x={0} y={Y((tope * k) / 4) + 4}>
              {Math.round((tope * k) / 4).toLocaleString(t('admin.locale'))}
            </text>
          </g>
        ))}
      </g>
      {labels.map((l, i) => (
        <text key={i} x={X(i)} y={h - 6} textAnchor="middle">
          {l}
        </text>
      ))}
      {series.map(([c, d]) => {
        if (!d.length) return null
        const pts = d.map((v, i) => `${X(i)},${Y(v)}`).join(' L')
        return (
          <g key={c}>
            <path d={`M${pts} L${X(d.length - 1)},${Y(0)} L${X(0)},${Y(0)}Z`} fill={c} opacity={0.08} />
            <path d={`M${pts}`} fill="none" stroke={c} strokeWidth={2.5} strokeLinejoin="round" />
            <circle cx={X(d.length - 1)} cy={Y(d[d.length - 1])} r={4.5} fill={c} />
          </g>
        )
      })}
    </svg>
  )
}

/** Barras dobles (vistas vs. contactos). */
export function PairBars({ labels, a, b, w = 620, h = 230 }: { labels: string[]; a: number[]; b: number[]; w?: number; h?: number }) {
  const { t } = useI18n()
  const pl = 36
  const pb = 26
  const max = Math.max(5, ...a, ...b)
  const tope = Math.ceil(max / 4) * 4
  const Y = (v: number) => h - pb - ((h - pb - 12) * v) / tope
  const bw = (w - pl) / Math.max(1, labels.length)
  return (
    <svg className="chart" viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label={t('admin.chartViewsContacts')}>
      <g className="grid">
        {[0, 1, 2, 3, 4].map((k) => (
          <g key={k}>
            <line x1={pl} x2={w} y1={Y((tope * k) / 4)} y2={Y((tope * k) / 4)} />
            <text x={0} y={Y((tope * k) / 4) + 4}>
              {Math.round((tope * k) / 4)}
            </text>
          </g>
        ))}
      </g>
      {labels.map((l, i) => {
        const x = pl + i * bw + bw * 0.18
        return (
          <g key={i}>
            <rect x={x} y={Y(a[i] ?? 0)} width={bw * 0.3} height={Y(0) - Y(a[i] ?? 0)} rx={4} fill="#3B82F6" />
            <rect x={x + bw * 0.34} y={Y(b[i] ?? 0)} width={bw * 0.3} height={Y(0) - Y(b[i] ?? 0)} rx={4} fill="var(--teal)" />
            <text x={x + bw * 0.32} y={h - 6} textAnchor="middle">
              {l}
            </text>
          </g>
        )
      })}
    </svg>
  )
}
