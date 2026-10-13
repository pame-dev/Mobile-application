import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { AlertBox, PasswordInput, PasswordRulesList } from '../components/AuthParts'
import { ConfirmModal } from '../components/Confirm'
import { Icon, type IconName } from '../components/Icon'
import { LogoutModal } from '../components/TopNav'
import { ModalHead, useUi } from '../components/Ui'
import { changePassword, markAllRead, notifications, signOutEverywhere, type Notificacion } from '../data/account'
import { mensajeUsuario } from '../data/auth'
import { POLITICAS, TERMINOS, type LegalDoc } from '../data/legal'
import { passwordError } from '../lib/formRules'
import { haceCuanto } from '../lib/format'
import { useSession } from '../session/SessionContext'

// Notificaciones, Configuración, Privacidad y seguridad, Términos y Políticas de privacidad.

function PageHead({ title, back, children }: { title: string; back?: string; children?: ReactNode }) {
  const navigate = useNavigate()
  return (
    <div className="pagehead">
      <div className="row">
        {back && (
          <button className="iconbtn" aria-label="Regresar" onClick={() => navigate(back)}>
            <Icon name="back" size={18} />
          </button>
        )}
        <h1>{title}</h1>
      </div>
      {children}
    </div>
  )
}

function SetRow({ icon, title, desc, onClick, right }: { icon: IconName; title: string; desc: string; onClick?: () => void; right?: ReactNode }) {
  const contenido = (
    <>
      <span className="nic">
        <Icon name={icon} />
      </span>
      <span className="grow">
        <b style={{ color: 'var(--ink)' }}>{title}</b>
        <br />
        <span className="muted" style={{ fontSize: 12.5 }}>
          {desc}
        </span>
      </span>
      {right ?? (onClick && <Icon name="chev" />)}
    </>
  )
  return onClick ? (
    <button className="setrow" onClick={onClick}>
      {contenido}
    </button>
  ) : (
    <div className="setrow static">{contenido}</div>
  )
}

/** Solo con sesión; sin ella manda a iniciar sesión y regresa aquí. */
function useRequireAccount(from: string) {
  const { account, loading } = useSession()
  return { account, redirect: !loading && !account ? <Navigate to="/login" replace state={{ from }} /> : null }
}

// ── Notificaciones ─────────────────────────────────────────────────────────

const VEHICULO = 'tu vehículo'
const TEXTOS: Record<string, (v: string) => string> = {
  favorito: (v) => `A alguien le gustó tu publicación "${v}" y la agregó a sus favoritos.`,
  propuesta_enviada: (v) => `Tu publicación "${v}" se envió al administrador y la está revisando.`,
  publicacion_aprobada: (v) => `¡Tu publicación "${v}" fue aprobada! Ya está visible para todos.`,
  destacado_aprobado: (v) => `Tu publicación "${v}" ahora es destacada durante 1 mes.`,
  edicion_aprobada: (v) => `Se aprobaron los cambios a tu publicación "${v}". Ya se ven publicados.`,
  publicacion_rehabilitada: (v) => `Tu publicación "${v}" fue rehabilitada y vuelve a estar visible.`,
  publicacion_rechazada: (v) => `Tu publicación "${v}" fue rechazada. Corrígela y vuelve a enviarla.`,
  edicion_rechazada: (v) => `No se aprobaron los cambios a "${v}". Sigue visible la versión anterior.`,
  publicacion_deshabilitada: (v) => `Administración deshabilitó tu publicación "${v}". Ya no es visible para otros usuarios.`,
  destacado_rechazado: (v) => `Tu solicitud para destacar "${v}" fue rechazada.`,
  destacado_por_vencer: (v) => `Tu destacado de "${v}" termina en menos de 3 días. Renuévalo para seguir apareciendo en destacados.`,
  cuenta_suspendida: () => 'Tu cuenta de Karsy fue suspendida por administración. Mientras esté suspendida no podrás entrar a la app.',
  cuenta_reactivada: () => 'Tu cuenta de Karsy fue reactivada. Ya puedes volver a entrar.',
  reporte_atendido: (v) => `Gracias por tu reporte sobre "${v}". Lo revisamos y tomamos medidas.`,
  reporte_descartado: (v) => `Revisamos tu reporte sobre "${v}" y no encontramos un incumplimiento.`,
}

/** Ícono y colores de cada tipo de aviso (mismos que la app). */
function estiloDe(tipo: string): [IconName, string, string] {
  switch (tipo) {
    case 'favorito':
      return ['heart', 'var(--error-l)', 'var(--error)']
    case 'propuesta_enviada':
      return ['doc', 'var(--warning-l)', 'var(--warning)']
    case 'publicacion_aprobada':
    case 'edicion_aprobada':
    case 'publicacion_rehabilitada':
    case 'cuenta_reactivada':
      return ['check', 'var(--success-l)', 'var(--success)']
    case 'destacado_aprobado':
    case 'destacado_por_vencer':
    case 'destacado_rechazado':
      return ['star', 'var(--warning-l)', 'var(--warning)']
    case 'publicacion_rechazada':
    case 'edicion_rechazada':
    case 'publicacion_deshabilitada':
    case 'cuenta_suspendida':
      return ['alert', 'var(--error-l)', 'var(--error)']
    case 'reporte_atendido':
    case 'reporte_descartado':
      return ['flag', 'var(--pill)', 'var(--ink)']
    default:
      return ['bell', 'var(--teal-light)', 'var(--teal)']
  }
}

/** Bandeja de avisos. Al abrirla se marcan como leídos (los nuevos se ven resaltados esta vez). */
export function Notifications() {
  const navigate = useNavigate()
  const { account, redirect } = useRequireAccount('/notificaciones')
  const [lista, setLista] = useState<Notificacion[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(() => {
    if (!account) return
    setError(null)
    notifications(account.id)
      .then((n) => {
        setLista(n)
        if (n.some((x) => !x.leida)) void markAllRead(account.id).then(() => window.dispatchEvent(new Event('karsy:notificaciones')))
      })
      .catch(() => setError('No pudimos cargar tus notificaciones. Revisa tu conexión e intenta de nuevo.'))
  }, [account])

  useEffect(() => {
    cargar()
  }, [cargar])

  if (redirect) return redirect

  return (
    <div className="wrap" style={{ maxWidth: 820, paddingBottom: 40 }}>
      <PageHead title="Notificaciones" />
      {error ? (
        <div className="card empty">
          <p className="muted">{error}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            Reintentar
          </button>
        </div>
      ) : !lista ? (
        <div className="skeleton" style={{ height: 320 }} />
      ) : lista.length === 0 ? (
        <div className="card empty">
          <span className="nic" style={{ width: 56, height: 56, borderRadius: 16, background: 'var(--teal-light)', color: 'var(--teal)' }}>
            <Icon name="bell" size={26} />
          </span>
          <p className="muted">Aún no tienes notificaciones.</p>
        </div>
      ) : (
        <div className="card" style={{ overflow: 'hidden' }}>
          {lista.map((n) => {
            const [icono, bg, fg] = estiloDe(n.tipo)
            const vehiculo = n.titulo_vehiculo?.trim() || VEHICULO
            const texto = (TEXTOS[n.tipo] ?? ((v: string) => `Tienes un aviso sobre "${v}".`))(vehiculo)
            const contenido = (
              <>
                <span className="nic" style={{ background: bg, color: fg }}>
                  <Icon name={icono} />
                </span>
                <div className="grow">
                  <p style={{ color: 'var(--charcoal)', fontWeight: n.leida ? 400 : 600 }}>{texto}</p>
                  {n.motivo?.trim() && <p className="muted" style={{ fontSize: 13 }}>Motivo: {n.motivo}</p>}
                  <span className="mid" style={{ fontSize: 12 }}>
                    {haceCuanto(n.fecha_creacion)}
                  </span>
                </div>
                {!n.leida && <span style={{ width: 9, height: 9, borderRadius: '50%', background: 'var(--teal)', marginTop: 6, flex: 'none' }} />}
              </>
            )
            return n.id_publicacion ? (
              <button key={n.id_notificacion} className={`nrow ${n.leida ? '' : 'unread'}`} onClick={() => navigate(`/vehiculo/${n.id_publicacion}`)}>
                {contenido}
              </button>
            ) : (
              <div key={n.id_notificacion} className={`nrow ${n.leida ? '' : 'unread'}`}>
                {contenido}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

// ── Configuración ──────────────────────────────────────────────────────────

type Tema = 'light' | 'dark' | ''

function temaGuardado(): Tema {
  try {
    const t = localStorage.getItem('karsy-theme')
    return t === 'light' || t === 'dark' ? t : ''
  } catch {
    return ''
  }
}

/** Claro, oscuro o el del sistema; se recuerda en este navegador. */
function aplicarTema(t: Tema) {
  if (t) document.documentElement.setAttribute('data-theme', t)
  else document.documentElement.removeAttribute('data-theme')
  try {
    if (t) localStorage.setItem('karsy-theme', t)
    else localStorage.removeItem('karsy-theme')
  } catch {
    /* sin almacenamiento: solo dura esta visita */
  }
}

export function Settings() {
  const navigate = useNavigate()
  const { openModal } = useUi()
  const { redirect } = useRequireAccount('/configuracion')
  const [tema, setTema] = useState<Tema>(temaGuardado)
  if (redirect) return redirect

  const elegirTema = (t: Tema) => {
    setTema(t)
    aplicarTema(t)
  }

  return (
    <div className="wrap" style={{ maxWidth: 820, paddingBottom: 40 }}>
      <PageHead title="Configuración" back="/perfil" />
      <div className="card" style={{ overflow: 'hidden' }}>
        <SetRow icon="bell" title="Notificaciones" desc="Alertas y avisos de tu cuenta" onClick={() => navigate('/notificaciones')} />
        <SetRow icon="lock" title="Privacidad y seguridad" desc="Contraseña, sesiones y políticas de privacidad" onClick={() => navigate('/privacidad')} />
        <SetRow icon="globe" title="Idioma" desc="Español" right={<span className="badge b-neu">ES</span>} />
        <SetRow
          icon="moon"
          title="Tema"
          desc="Claro, oscuro o el del sistema"
          right={
            <span className="seg">
              {(
                [
                  ['light', 'Claro'],
                  ['dark', 'Oscuro'],
                  ['', 'Sistema'],
                ] as const
              ).map(([v, t]) => (
                <button key={t} className={tema === v ? 'on' : ''} onClick={() => elegirTema(v)}>
                  {t}
                </button>
              ))}
            </span>
          }
        />
        <SetRow icon="doc" title="Términos y condiciones" desc="Políticas de privacidad y uso" onClick={() => navigate('/terminos')} />
      </div>
      <button className="btn btn-ghost btn-block" style={{ marginTop: 18, color: 'var(--error)', borderColor: 'var(--error)' }} onClick={() => openModal(<LogoutModal />)}>
        <Icon name="logout" size={18} />
        Cerrar sesión
      </button>
    </div>
  )
}

// ── Privacidad y seguridad ─────────────────────────────────────────────────

/** Cambiar contraseña (ChangePasswordDialog de la app). */
function ChangePasswordModal({ email }: { email: string }) {
  const { closeModal, toast } = useUi()
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [confirmar, setConfirmar] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  const guardar = async () => {
    const e = !actual
      ? 'Escribe tu contraseña actual.'
      : passwordError(nueva, email) ?? (nueva !== confirmar ? 'Las contraseñas no coinciden.' : nueva === actual ? 'La nueva contraseña debe ser diferente a la anterior.' : null)
    setError(e)
    if (e) return
    setGuardando(true)
    try {
      await changePassword(email, actual, nueva)
      closeModal()
      toast('Contraseña actualizada.')
    } catch (ex) {
      setError(mensajeUsuario(ex))
      setGuardando(false)
    }
  }

  return (
    <form
      className="modal"
      onSubmit={(e) => {
        e.preventDefault()
        void guardar()
      }}
    >
      <ModalHead title="Cambiar contraseña" sub="Por seguridad, primero escribe tu contraseña actual." />
      <div className="field">
        <label htmlFor="pw-a">Contraseña actual</label>
        <PasswordInput id="pw-a" value={actual} onChange={setActual} />
      </div>
      <div className="field">
        <label htmlFor="pw-n">Contraseña nueva</label>
        <PasswordInput id="pw-n" value={nueva} onChange={setNueva} autoComplete="new-password" />
      </div>
      <PasswordRulesList password={nueva} />
      <div className="field">
        <label htmlFor="pw-c">Confirmar contraseña nueva</label>
        <PasswordInput id="pw-c" value={confirmar} onChange={setConfirmar} autoComplete="new-password" bad={!!confirmar && confirmar !== nueva} />
      </div>
      <AlertBox message={error} />
      <div className="grid2">
        <button type="button" className="btn btn-ghost" disabled={guardando} onClick={closeModal}>
          Cancelar
        </button>
        <button type="submit" className="btn btn-primary" disabled={guardando}>
          {guardando ? 'Guardando…' : 'Guardar'}
        </button>
      </div>
    </form>
  )
}

export function Privacy() {
  const navigate = useNavigate()
  const { openModal, toast } = useUi()
  const { account, redirect } = useRequireAccount('/privacidad')
  const { signOut } = useSession()
  const permiso = typeof Notification === 'undefined' ? 'unsupported' : Notification.permission
  if (redirect) return redirect

  return (
    <div className="wrap" style={{ maxWidth: 820, paddingBottom: 40 }}>
      <PageHead title="Privacidad y seguridad" back="/configuracion" />
      <div className="label" style={{ marginBottom: 10 }}>
        Seguridad
      </div>
      <div className="card" style={{ overflow: 'hidden' }}>
        <SetRow icon="lock" title="Cambiar contraseña" desc="Necesitas tu contraseña actual" onClick={() => account && openModal(<ChangePasswordModal email={account.correo} />)} />
        <SetRow
          icon="devices"
          title="Cerrar sesión en todos los dispositivos"
          desc="Útil si perdiste tu teléfono o crees que alguien entró a tu cuenta"
          onClick={() =>
            openModal(
              <ConfirmModal
                title="¿Cerrar sesión en todos lados?"
                message="Se cerrará tu sesión en este navegador y en todos los dispositivos donde hayas entrado. Tendrás que volver a iniciar sesión."
                confirm="Cerrar todas"
                danger
                onConfirm={async () => {
                  try {
                    await signOutEverywhere()
                  } catch (e) {
                    toast(mensajeUsuario(e))
                    return
                  }
                  await signOut()
                  navigate('/', { replace: true })
                }}
              />,
            )
          }
        />
      </div>
      <div className="label" style={{ margin: '22px 0 10px' }}>
        Privacidad
      </div>
      <div className="card" style={{ overflow: 'hidden' }}>
        <SetRow icon="shield" title="Políticas de privacidad" desc="Qué datos guardamos y para qué" onClick={() => navigate('/privacidad/politicas')} />
        <SetRow
          icon="bell"
          title="Notificaciones del navegador"
          desc={
            permiso === 'unsupported'
              ? 'Este navegador no las admite. Tus avisos siempre están en Notificaciones.'
              : 'Por ahora los avisos de Karsy se ven en Notificaciones (la campanita). Los avisos al teléfono llegan por la app.'
          }
        />
      </div>
    </div>
  )
}

// ── Textos legales ─────────────────────────────────────────────────────────

function Legal({ title, doc }: { title: string; doc: LegalDoc }) {
  return (
    <div className="wrap" style={{ padding: '28px 16px 40px' }}>
      <div className="card legaltext" style={{ margin: '0 auto' }}>
        <h1 style={{ fontSize: 30 }}>{title}</h1>
        <p className="mid" style={{ margin: '6px 0 16px' }}>
          {doc.update}
        </p>
        <p>{doc.intro}</p>
        {doc.sections.map(([h, b]) => (
          <div key={h}>
            <h3>{h}</h3>
            <p>{b}</p>
          </div>
        ))}
      </div>
    </div>
  )
}

export const Terms = () => <Legal title="Términos y condiciones" doc={TERMINOS} />
export const PrivacyPolicy = () => <Legal title="Políticas de privacidad" doc={POLITICAS} />
