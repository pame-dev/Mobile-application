import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { VolverButton } from '../../components/VolverButton'
import { useLocation, useNavigate } from 'react-router-dom'
import { AlertBox } from '../../components/AuthParts'
import { Icon } from '../../components/Icon'
import { Traducido } from '../../components/Traducido'
import { ModalHead, useUi } from '../../components/Ui'
import { adminAccounts, adminReports, moderateProposal, resolveReport, setAccountSuspended, type CuentaAdmin, type ReporteAdmin } from '../../data/admin'
import { mensajeUsuario } from '../../data/auth'
import type { AnuncioRow, Car } from '../../data/types'
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

// ── Revisión de publicaciones (panel de Vehículos y detalle de la publicación) ──

/** Vehículo como lo ve el admin: "Cambios pendientes" si está aprobado y tiene una edición sin revisar. */
export function estadoAdmin(row: AnuncioRow, car: Car): string {
  return row.aprobada && row.estado_ultima_propuesta === 'pendiente' ? 'Cambios pendientes' : car.status
}

/** Propuesta por revisar (publicación nueva o edición), o null si no hay. */
export const propuestaPendiente = (row: AnuncioRow) => (row.estado_ultima_propuesta === 'pendiente' ? row.id_ultima_propuesta ?? null : null)

/** Rechazar una propuesta pidiendo el motivo (misma ventana en el panel y en el detalle). */
export function useModeracion() {
  const { openModal } = useUi()
  const { t } = useI18n()
  return {
    rechazar: (propuesta: number, listo: (m: string) => void) =>
      openModal(
        <ReasonModal
          title={t('admin.rejectTitle')}
          desc={t('admin.rejectDesc')}
          hint={t('admin.rejectHint')}
          confirm={t('admin.reject')}
          working={t('admin.rejecting')}
          onConfirm={async (m) => {
            await moderateProposal(propuesta, false, m)
            listo(t('admin.msgRejected'))
          }}
        />,
      ),
  }
}

/**
 * Barra de administración en el detalle de una publicación abierta desde la revisión de Vehículos:
 * estado, Aprobar / Rechazar (solo si hay una propuesta pendiente) y Volver a revisión.
 * La BD vuelve a validar que sea admin (admin_moderar_propuesta → exigir_admin).
 */
export function AdminReviewBar({ car, row, onChanged }: { car: Car; row: AnuncioRow; onChanged: () => void }) {
  const { toast } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const location = useLocation()
  const moderacion = useModeracion()
  const [busy, setBusy] = useState(false)
  const propuesta = propuestaPendiente(row)

  const listo = (msg: string) => {
    toast(msg)
    onChanged()
  }
  const aprobar = async () => {
    if (propuesta == null) return
    setBusy(true)
    try {
      await moderateProposal(propuesta, true, null)
      listo(t('admin.msgApproved'))
    } catch (e) {
      toast(mensajeUsuario(e))
    }
    setBusy(false)
  }
  // Regresa al panel tal como estaba; si el detalle se abrió directo, a la sección de Vehículos.
  const volver = () => (location.key !== 'default' ? navigate(-1) : navigate('/admin/vehiculos'))

  return (
    <div className="card adminbar">
      <div className="row" style={{ flexWrap: 'wrap', gap: 10 }}>
        <span className="nic">
          <Icon name="shield" size={18} />
        </span>
        <b style={{ color: 'var(--ink)' }}>{t('admin.reviewBar')}</b>
        <Badge s={estadoAdmin(row, car)} />
        <span className="grow muted" style={{ fontSize: 13 }}>
          {propuesta != null ? t('admin.reviewBarPending') : t('admin.reviewBarDone')}
        </span>
      </div>
      <div className="acts">
        <VolverButton onClick={volver} label={t('admin.backToReview')} />
        {propuesta != null && (
          <>
            <button className="btn btn-danger btn-sm" disabled={busy} onClick={() => moderacion.rechazar(propuesta, listo)}>
              <Icon name="x" size={14} />
              {t('admin.reject')}
            </button>
            <button className="btn btn-success btn-sm" disabled={busy} onClick={() => void aprobar()}>
              <Icon name="check" size={14} />
              {t('admin.approve')}
            </button>
          </>
        )}
      </div>
    </div>
  )
}

// ── Revisión de reportes (panel de Reportes y detalle de la publicación) ──

export type TabReportes = 'pendiente' | 'resueltos' | 'todos'

export const esReporteDeCuenta = (r: ReporteAdmin) => r.motivo_reporte.startsWith('Cuenta del vendedor')

/** Descartar o atender un reporte (deshabilitar publicación o suspender cuenta), pidiendo el motivo. */
export function useAccionesReporte() {
  const { openModal } = useUi()
  const { t } = useI18n()
  return {
    descartar: (r: ReporteAdmin, listo: (m: string) => void) =>
      openModal(
        <ReasonModal
          title={t('admin.dismissTitle')}
          desc={t(esReporteDeCuenta(r) ? 'admin.dismissDescAccount' : 'admin.dismissDescListing')}
          hint={t('admin.dismissHint')}
          confirm={t('admin.dismissReport')}
          working={t('admin.dismissing')}
          onConfirm={async (m) => {
            await resolveReport(r.id_reporte, 'descartado', m, false)
            listo(t('admin.msgDismissed'))
          }}
        />,
      ),
    proceder: (r: ReporteAdmin, dueno: CuentaAdmin | undefined, listo: (m: string) => void) =>
      openModal(
        esReporteDeCuenta(r) ? (
          <ReasonModal
            title={t('admin.suspendTitle')}
            desc={t('admin.suspendDesc')}
            hint={t('admin.suspendHint')}
            confirm={t('admin.suspend')}
            working={t('admin.suspending')}
            onConfirm={async (m) => {
              // Si la cuenta ya estaba suspendida, solo se atiende el reporte.
              if (!dueno) throw new Error(t('admin.ownerNotFound'))
              if (dueno.estado_cuenta !== 'suspendida') await setAccountSuspended(dueno.id_cuenta, true, m)
              await resolveReport(r.id_reporte, 'atendido', m, false)
              listo(t('admin.msgResolvedSuspended'))
            }}
          />
        ) : (
          <ReasonModal
            title={t('admin.disableTitle')}
            desc={t('admin.disableDesc')}
            hint={t('admin.disableHint')}
            confirm={t('admin.disable')}
            working={t('admin.disabling')}
            onConfirm={async (m) => {
              await resolveReport(r.id_reporte, 'atendido', m, true)
              listo(t('admin.msgResolvedDisabled'))
            }}
          />
        ),
      ),
  }
}

/**
 * Barra de administración en el detalle de una publicación abierta desde la revisión de un reporte:
 * Volver al reporte, Descartar reporte y Deshabilitar publicación / Suspender cuenta (si sigue pendiente).
 * La BD vuelve a validar que sea admin en cada acción.
 */
export function AdminReportBar({ idReporte, ownerId, onChanged }: { idReporte: number; ownerId: string; onChanged: () => void }) {
  const { toast } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const location = useLocation()
  const acciones = useAccionesReporte()
  const [r, setR] = useState<ReporteAdmin | null | undefined>(undefined)
  const [dueno, setDueno] = useState<CuentaAdmin | undefined>(undefined)

  const cargar = useCallback(() => {
    adminReports()
      .then((rs) => setR(rs.find((x) => x.id_reporte === idReporte) ?? null))
      .catch(() => setR(null))
    adminAccounts()
      .then((cs) => setDueno(cs.find((c) => c.id_cuenta === ownerId)))
      .catch(() => undefined)
  }, [idReporte, ownerId])
  useEffect(() => {
    cargar()
  }, [cargar])

  const listo = (msg: string) => {
    toast(msg)
    cargar()
    onChanged()
  }
  // Regresa al panel, que vuelve a abrir el reporte; si el detalle se abrió directo, también lo abre.
  const volver = () => (location.key !== 'default' ? navigate(-1) : navigate('/admin/reportes', { state: { reporte: idReporte } }))
  const pendiente = r?.estado_reporte === 'pendiente'
  const deCuenta = r ? esReporteDeCuenta(r) : false

  return (
    <div className="card adminbar">
      <div className="row" style={{ flexWrap: 'wrap', gap: 10 }}>
        <span className="nic">
          <Icon name="flag" size={18} />
        </span>
        <b style={{ color: 'var(--ink)' }}>{t('admin.reportReviewBar', { id: idReporte })}</b>
        {r && <Badge s={r.estado_reporte.charAt(0).toUpperCase() + r.estado_reporte.slice(1)} />}
        <span className="grow muted" style={{ fontSize: 13 }}>
          {r === null ? t('admin.reportNotFound') : r ? <Traducido texto={r.motivo_reporte} /> : ''}
        </span>
      </div>
      <div className="acts">
        <VolverButton onClick={volver} label={t('admin.backToReport')} />
        {r && pendiente && (
          <>
            <button className="btn btn-ghost btn-sm" onClick={() => acciones.descartar(r, listo)}>
              {t('admin.dismissReport')}
            </button>
            <button className="btn btn-danger btn-sm" onClick={() => acciones.proceder(r, dueno, listo)}>
              {t(deCuenta ? 'admin.suspendAccount' : 'admin.disableListing')}
            </button>
          </>
        )}
      </div>
    </div>
  )
}
