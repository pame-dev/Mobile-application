import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { AlertBox, PasswordInput, PasswordRulesList } from '../components/AuthParts'
import { Bandera } from '../components/Bandera'
import { ConfirmModal } from '../components/Confirm'
import { Icon, type IconName } from '../components/Icon'
import { LogoutModal } from '../components/TopNav'
import { VolverButton } from '../components/VolverButton'
import { ModalHead, useUi } from '../components/Ui'
import { Traducido } from '../components/Traducido'
import {
  changePassword,
  markAllRead,
  notificationPreferences,
  notifications,
  PREFERENCIAS_POR_DEFECTO,
  saveNotificationPreferences,
  signOutEverywhere,
  type Notificacion,
  type PreferenciasAvisos,
} from '../data/account'
import { mensajeUsuario } from '../data/auth'
import { borrarBusqueda, describirBusqueda, misBusquedas, type BusquedaGuardada } from '../data/busquedas'
import { aceptarDoc, POLICY_EN, POLITICAS, TERMINOS, TERMS_EN, type DocLegal, type LegalDoc } from '../data/legal'
import { useI18n, type Lang } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { passwordError } from '../lib/formRules'
import { destinoAviso, textoAviso } from '../lib/avisos'
import { haceCuanto } from '../lib/format'
import { aplicarTema, guardarTemaEnCuenta, temaGuardado, type Tema } from '../lib/tema'
import { useRecargar } from '../lib/tiempoReal'
import { useSession } from '../session/SessionContext'

// Notificaciones, Configuración, Privacidad y seguridad, Términos y Políticas de privacidad.

/** [back]: ruta fija, o 'historial' para regresar a la pantalla anterior (al Inicio si se abrió directo). */
function PageHead({ title, back, fallback = '/', children }: { title: string; back?: string; fallback?: string; children?: ReactNode }) {
  return (
    <div className="pagehead">
      <div className="row">
        {back === 'historial' ? (
          <VolverButton fallback={fallback} />
        ) : (
          back && <VolverButton to={back} />
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
        <b className="ink">{title}</b>
        <br />
        <span className="muted text-xs">
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
    case 'verificacion_aprobada':
      return ['check', 'var(--success-l)', 'var(--success)']
    case 'destacado_aprobado':
    case 'destacado_por_vencer':
    case 'destacado_rechazado':
    case 'pago_destacado':
      return ['star', 'var(--warning-l)', 'var(--warning)']
    case 'publicacion_rechazada':
    case 'edicion_rechazada':
    case 'publicacion_deshabilitada':
    case 'cuenta_suspendida':
    case 'verificacion_rechazada':
      return ['alert', 'var(--error-l)', 'var(--error)']
    case 'verificacion_pendiente':
      return ['shield', 'var(--warning-l)', 'var(--warning)']
    case 'reporte_atendido':
    case 'reporte_descartado':
      return ['flag', 'var(--pill)', 'var(--ink)']
    case 'precio_bajo':
      return ['chart', 'var(--success-l)', 'var(--success)']
    case 'favorito_no_disponible':
      return ['heart', 'var(--pill)', 'var(--mid)']
    case 'resumen_semanal':
    case 'auto_parecido':
      return ['car', 'var(--teal-light)', 'var(--teal)']
    case 'resumen_vendedor':
    case 'hito_vistas':
      return ['eye', 'var(--teal-light)', 'var(--teal)']
    case 'poca_actividad':
    case 'confirmar_venta':
      return ['sold', 'var(--warning-l)', 'var(--warning)']
    default:
      return ['bell', 'var(--teal-light)', 'var(--teal)']
  }
}

/** Bandeja de avisos. Al abrirla se marcan como leídos (los nuevos se ven resaltados esta vez). */
export function Notifications() {
  const navigate = useNavigate()
  const { account, redirect } = useRequireAccount('/notificaciones')
  const { t } = useI18n()
  const [lista, setLista] = useState<Notificacion[] | null>(null)
  const [error, setError] = useState(false)

  const cargar = useCallback(() => {
    if (!account) return
    setError(false)
    notifications(account.id)
      .then((n) => {
        setLista(n)
        if (n.some((x) => !x.leida)) void markAllRead(account.id).then(() => window.dispatchEvent(new Event('karsy:notificaciones')))
      })
      .catch(() => setError(true))
  }, [account])

  useEffect(() => {
    cargar()
  }, [cargar])
  useRecargar(cargar, ['notificaciones'])

  if (redirect) return redirect

  return (
    <div className="wrap wrap-narrow page-end">
      <PageHead title={t('notif.title')} back="historial" />
      <div className="card" style={{ overflow: 'hidden', marginBottom: 'var(--s-4)' }}>
        <SetRow icon="settings" title={t('notif.prefs.title')} desc={t('notif.prefs.linkDesc')} onClick={() => navigate('/notificaciones/preferencias')} />
      </div>
      {error ? (
        <div className="card empty">
          <p className="muted">{t('notif.loadError')}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            {t('common.retry')}
          </button>
        </div>
      ) : !lista ? (
        <div className="skeleton" style={{ height: 320 }} />
      ) : lista.length === 0 ? (
        <div className="card empty">
          <span className="nic empty-icon">
            <Icon name="bell" size={26} />
          </span>
          <p className="muted">{t('notif.empty')}</p>
        </div>
      ) : (
        <div className="card" style={{ overflow: 'hidden' }}>
          {lista.map((n) => {
            const [icono, bg, fg] = estiloDe(n.tipo)
            const texto = textoAviso(t, n.tipo, n.titulo_vehiculo?.trim() || t('notif.yourVehicle'), n.datos)
            const destino = destinoAviso(n.tipo, n.id_publicacion)
            const contenido = (
              <>
                <span className="nic" style={{ background: bg, color: fg }}>
                  <Icon name={icono} />
                </span>
                <div className="grow">
                  <p style={{ color: 'var(--charcoal)', fontWeight: n.leida ? 400 : 600 }}>{texto}</p>
                  {n.motivo?.trim() && (
                    <p className="muted text-sm">
                      <Traducido texto={n.motivo}>{(m) => t('notif.reason', { motivo: m })}</Traducido>
                    </p>
                  )}
                  <span className="mid text-xs">
                    {haceCuanto(n.fecha_creacion)}
                  </span>
                </div>
                {!n.leida && <span style={{ width: 9, height: 9, borderRadius: '50%', background: 'var(--teal)', marginTop: 'var(--s-2)', flex: 'none' }} />}
              </>
            )
            return destino ? (
              <button key={n.id_notificacion} className={`nrow ${n.leida ? '' : 'unread'}`} onClick={() => navigate(destino)}>
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

// ── Preferencias de notificaciones ─────────────────────────────────────────

type PermisoNavegador = NotificationPermission | 'sin-soporte'
const permisoNavegador = (): PermisoNavegador => ('Notification' in window ? Notification.permission : 'sin-soporte')

/** Qué avisos opcionales recibe la cuenta y el permiso de notificaciones del navegador. */
export function NotificationPreferences() {
  const { account, redirect } = useRequireAccount('/notificaciones/preferencias')
  const { t } = useI18n()
  const { toast } = useUi()
  const [prefs, setPrefs] = useState<PreferenciasAvisos | null>(null)
  const [permiso, setPermiso] = useState<PermisoNavegador>(permisoNavegador)

  useEffect(() => {
    if (!account) return
    notificationPreferences(account.id)
      .then(setPrefs)
      .catch(() => setPrefs(PREFERENCIAS_POR_DEFECTO))
  }, [account])

  if (redirect) return redirect

  /** Se guarda al momento; si falla, se regresa la casilla y se avisa. */
  const cambiar = (clave: keyof PreferenciasAvisos) => {
    if (!account || !prefs) return
    const antes = prefs
    const nuevo = { ...prefs, [clave]: !prefs[clave] }
    setPrefs(nuevo)
    saveNotificationPreferences(account.id, nuevo).catch(() => {
      setPrefs(antes)
      toast(t('notif.prefs.saveError'))
    })
  }

  const filas: [keyof PreferenciasAvisos, Clave, Clave, IconName][] = [
    ['precios', 'notif.prefs.prices', 'notif.prefs.pricesDesc', 'chart'],
    ['resumenes', 'notif.prefs.summaries', 'notif.prefs.summariesDesc', 'doc'],
    ['recomendaciones', 'notif.prefs.recommendations', 'notif.prefs.recommendationsDesc', 'car'],
    ['vendedor', 'notif.prefs.seller', 'notif.prefs.sellerDesc', 'eye'],
  ]

  return (
    <div className="wrap wrap-narrow page-end">
      {/* Regresa por el historial: ir directo a /notificaciones agregaba otra página y su
          botón de regresar volvía aquí (bucle entre las dos). */}
      <PageHead title={t('notif.prefs.title')} back="historial" fallback="/notificaciones" />
      <div className="card" style={{ overflow: 'hidden', marginBottom: 'var(--s-4)' }}>
        <SetRow
          icon="bell"
          title={t('notif.prefs.browser')}
          desc={t(
            permiso === 'granted'
              ? 'notif.prefs.browserOn'
              : permiso === 'denied'
                ? 'notif.prefs.browserBlocked'
                : permiso === 'sin-soporte'
                  ? 'notif.prefs.browserUnsupported'
                  : 'notif.prefs.browserOff',
          )}
          right={
            permiso === 'default' ? (
              <button className="btn btn-primary btn-sm" onClick={() => void Notification.requestPermission().then(setPermiso)}>
                {t('live.enable')}
              </button>
            ) : undefined
          }
        />
      </div>
      {!prefs ? (
        <div className="skeleton" style={{ height: 280 }} />
      ) : (
        <div className="card" style={{ overflow: 'hidden' }}>
          {filas.map(([clave, titulo, desc, icono]) => (
            <SetRow
              key={clave}
              icon={icono}
              title={t(titulo)}
              desc={t(desc)}
              onClick={() => cambiar(clave)}
              right={<input type="checkbox" checked={prefs[clave]} readOnly style={{ width: 20, height: 20, accentColor: 'var(--teal)' }} />}
            />
          ))}
        </div>
      )}
      <p className="muted text-sm" style={{ margin: 'var(--s-4) var(--s-1) 0' }}>
        {t('notif.prefs.footer')}
      </p>
    </div>
  )
}

// ── Búsquedas guardadas ────────────────────────────────────────────────────

/** Búsquedas guardadas: se crean en el inicio con "Avisarme de nuevos" y avisan de autos nuevos. */
export function SavedSearches() {
  const { account, redirect } = useRequireAccount('/busquedas')
  const { t } = useI18n()
  const { toast } = useUi()
  const [lista, setLista] = useState<BusquedaGuardada[] | null>(null)
  const [error, setError] = useState(false)

  const cargar = useCallback(() => {
    if (!account) return
    setError(false)
    misBusquedas(account.id).then(setLista).catch(() => setError(true))
  }, [account])

  useEffect(() => {
    cargar()
  }, [cargar])

  if (redirect) return redirect

  const borrar = (b: BusquedaGuardada) =>
    borrarBusqueda(b.id_busqueda)
      .then(() => setLista((l) => l?.filter((x) => x.id_busqueda !== b.id_busqueda) ?? null))
      .catch((e) => toast(mensajeUsuario(e)))

  return (
    <div className="wrap wrap-narrow page-end">
      <PageHead title={t('saved.title')} back="/configuracion" />
      <p className="muted" style={{ margin: '0 0 var(--s-4)' }}>
        {t('saved.hint')}
      </p>
      {error ? (
        <div className="card empty">
          <p className="muted">{t('saved.loadError')}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            {t('common.retry')}
          </button>
        </div>
      ) : !lista ? (
        <div className="skeleton" style={{ height: 200 }} />
      ) : lista.length === 0 ? (
        <div className="card empty">
          <span className="nic empty-icon">
            <Icon name="bell" size={26} />
          </span>
          <p className="muted">{t('saved.empty')}</p>
        </div>
      ) : (
        <div className="card" style={{ overflow: 'hidden' }}>
          {lista.map((b) => (
            <div key={b.id_busqueda} className="nrow" style={{ alignItems: 'center' }}>
              <span className="nic" style={{ background: 'var(--teal-light)', color: 'var(--teal)' }}>
                <Icon name="bell" />
              </span>
              <div className="grow">
                <p style={{ color: 'var(--charcoal)', fontWeight: 600 }}>{describirBusqueda(b)}</p>
                <span className="mid text-xs">
                  {t('saved.since', { fecha: haceCuanto(b.fecha_creacion) })}
                </span>
              </div>
              <button className="iconbtn" aria-label={t('saved.delete')} title={t('saved.delete')} onClick={() => void borrar(b)}>
                <Icon name="x" size={16} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Configuración ──────────────────────────────────────────────────────────

/** Selector de idioma (LanguageSheet de la app): español o inglés, se recuerda en este navegador. */
function LanguageModal() {
  const { closeModal } = useUi()
  const { t, lang, setLang } = useI18n()
  const opciones: Lang[] = ['es', 'en']
  return (
    <div className="modal">
      <ModalHead title={t('lang.title')} sub={t('lang.sub')} />
      {opciones.map((l) => (
        <button
          key={l}
          className={`langopt ${lang === l ? 'on' : ''}`}
          role="radio"
          aria-checked={lang === l}
          onClick={() => {
            setLang(l)
            closeModal()
          }}
        >
          <Bandera lang={l} ancho={28} />
          <b className="grow">{t(l === 'es' ? 'lang.es' : 'lang.en')}</b>
          <span className="badge b-neu">{l.toUpperCase()}</span>
          <span className="radio" />
        </button>
      ))}
    </div>
  )
}

const TEMAS: [Tema, 'settings.themeLight' | 'settings.themeDark' | 'settings.themeSystem', string][] = [
  ['light', 'settings.themeLight', '☀️'],
  ['dark', 'settings.themeDark', '🌙'],
  ['', 'settings.themeSystem', '📱'],
]

/** Selector de tema, igual que el de idioma: claro, oscuro o el del sistema. */
function ThemeModal({ tema, onElegir }: { tema: Tema; onElegir: (t: Tema) => void }) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  return (
    <div className="modal">
      <ModalHead title={t('settings.theme')} sub={t('settings.themeDesc')} />
      {TEMAS.map(([v, clave, icono]) => (
        <button
          key={clave}
          className={`langopt ${tema === v ? 'on' : ''}`}
          role="radio"
          aria-checked={tema === v}
          onClick={() => {
            onElegir(v)
            closeModal()
          }}
        >
          <span style={{ fontSize: 'var(--fs-xl)' }}>{icono}</span>
          <b className="grow">{t(clave)}</b>
          <span className="radio" />
        </button>
      ))}
    </div>
  )
}

export function Settings() {
  const navigate = useNavigate()
  const { openModal } = useUi()
  const { t, lang } = useI18n()
  const { account, redirect } = useRequireAccount('/configuracion')
  const [tema, setTema] = useState<Tema>(temaGuardado)
  if (redirect) return redirect

  const elegirTema = (t: Tema) => {
    setTema(t)
    aplicarTema(t)
    // También en la cuenta, para que sea el mismo en la app y en otros navegadores.
    if (account) void guardarTemaEnCuenta(account.id, t).catch(() => {})
  }

  return (
    <div className="wrap wrap-narrow page-end">
      <PageHead title={t('settings.title')} back="/perfil" />
      <div className="card" style={{ overflow: 'hidden' }}>
        <SetRow icon="bell" title={t('notif.title')} desc={t('settings.notificationsDesc')} onClick={() => navigate('/notificaciones')} />
        <SetRow icon="search" title={t('saved.title')} desc={t('saved.settingsDesc')} onClick={() => navigate('/busquedas')} />
        <SetRow icon="lock" title={t('settings.privacy')} desc={t('settings.privacyDesc')} onClick={() => navigate('/privacidad')} />
        <SetRow
          icon="globe"
          title={t('settings.language')}
          desc={t(lang === 'es' ? 'lang.es' : 'lang.en')}
          onClick={() => openModal(<LanguageModal />)}
          right={
            <span className="row gap-2">
              <span className="badge b-neu">{lang.toUpperCase()}</span>
              <Icon name="chev" />
            </span>
          }
        />
        <SetRow
          icon="moon"
          title={t('settings.theme')}
          desc={t(TEMAS.find(([v]) => v === tema)![1])}
          onClick={() => openModal(<ThemeModal tema={tema} onElegir={elegirTema} />)}
        />
        <SetRow icon="doc" title={t('settings.terms')} desc={t('settings.termsDesc')} onClick={() => navigate('/terminos')} />
        <SetRow icon="help" title={t('help.title')} desc={t('help.settingsDesc')} onClick={() => navigate('/ayuda')} />
      </div>
      <button className="btn btn-ghost btn-block" style={{ marginTop: 'var(--s-4)', color: 'var(--error)', borderColor: 'var(--error)' }} onClick={() => openModal(<LogoutModal />)}>
        <Icon name="logout" size={18} />
        {t('nav.logout')}
      </button>
    </div>
  )
}

// ── Privacidad y seguridad ─────────────────────────────────────────────────

/** Cambiar contraseña (ChangePasswordDialog de la app). */
function ChangePasswordModal({ email }: { email: string }) {
  const { closeModal, toast } = useUi()
  const { t } = useI18n()
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [confirmar, setConfirmar] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  const guardar = async () => {
    const e = !actual
      ? t('password.errCurrent')
      : passwordError(nueva, email) ?? (nueva !== confirmar ? t('password.errMismatch') : nueva === actual ? t('password.errSame') : null)
    setError(e)
    if (e) return
    setGuardando(true)
    try {
      await changePassword(email, actual, nueva)
      closeModal()
      toast(t('password.updated'))
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
      <ModalHead title={t('password.title')} sub={t('password.sub')} />
      <div className="field">
        <label htmlFor="pw-a">{t('password.current')}</label>
        <PasswordInput id="pw-a" value={actual} onChange={setActual} />
      </div>
      <div className="field">
        <label htmlFor="pw-n">{t('password.new')}</label>
        <PasswordInput id="pw-n" value={nueva} onChange={setNueva} autoComplete="new-password" />
      </div>
      <PasswordRulesList password={nueva} />
      <div className="field">
        <label htmlFor="pw-c">{t('password.confirm')}</label>
        <PasswordInput id="pw-c" value={confirmar} onChange={setConfirmar} autoComplete="new-password" bad={!!confirmar && confirmar !== nueva} />
      </div>
      <AlertBox message={error} />
      <div className="grid2">
        <button type="button" className="btn btn-ghost" disabled={guardando} onClick={closeModal}>
          {t('common.cancel')}
        </button>
        <button type="submit" className="btn btn-primary" disabled={guardando}>
          {guardando ? t('common.saving') : t('common.save')}
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
  const { t } = useI18n()
  if (redirect) return redirect

  return (
    <div className="wrap wrap-narrow page-end">
      <PageHead title={t('settings.privacy')} back="/configuracion" />
      <div className="label" style={{ marginBottom: 'var(--s-3)' }}>
        {t('privacy.security')}
      </div>
      <div className="card" style={{ overflow: 'hidden' }}>
        <SetRow icon="lock" title={t('privacy.changePassword')} desc={t('privacy.changePasswordDesc')} onClick={() => account && openModal(<ChangePasswordModal email={account.correo} />)} />
        <SetRow
          icon="devices"
          title={t('privacy.signOutAll')}
          desc={t('privacy.signOutAllDesc')}
          onClick={() =>
            openModal(
              <ConfirmModal
                title={t('privacy.signOutAllTitle')}
                message={t('privacy.signOutAllMsg')}
                confirm={t('privacy.signOutAllConfirm')}
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
      <div className="label" style={{ margin: 'var(--s-6) 0 var(--s-3)' }}>
        {t('privacy.privacy')}
      </div>
      <div className="card" style={{ overflow: 'hidden' }}>
        <SetRow icon="shield" title={t('privacy.policy')} desc={t('privacy.policyDesc')} onClick={() => navigate('/privacidad/politicas')} />
      </div>
    </div>
  )
}

// ── Textos legales ─────────────────────────────────────────────────────────

/** Términos o políticas en el idioma elegido, con "← Volver" a la pantalla de donde se llegó. */
function Legal({ title, doc, id }: { title: Clave; doc: Record<Lang, LegalDoc>; id: DocLegal }) {
  const navigate = useNavigate()
  const location = useLocation()
  const { t, lang } = useI18n()
  const d = doc[lang]
  /** Abierto con "Leer" desde el registro: al final se acepta el documento. */
  const desdeRegistro = (location.state as { desdeRegistro?: boolean } | null)?.desdeRegistro === true
  const [acepto, setAcepto] = useState(false)
  // Regresa en el historial (Registro, Configuración, pie de página…); si se abrió directo, al Inicio (o al registro).
  const volver = () => (location.key !== 'default' ? navigate(-1) : navigate(desdeRegistro ? '/registro' : '/', { replace: true }))
  return (
    <div className="wrap" style={{ padding: 'var(--s-6) var(--s-4) var(--s-10)' }}>
      <div className="card legaltext" style={{ margin: '0 auto' }}>
        <div style={{ marginBottom: 'var(--s-4)' }}>
          <VolverButton onClick={volver} label={desdeRegistro ? t('legal.backToRegister') : t('common.back')} />
        </div>
        <h1 className="h1-page">{t(title)}</h1>
        <p className="mid" style={{ margin: 'var(--s-2) 0 var(--s-4)' }}>
          {d.update}
        </p>
        <p>{d.intro}</p>
        {d.sections.map(([h, b]) => (
          <div key={h}>
            <h2>{h}</h2>
            <p>{b}</p>
          </div>
        ))}
        {desdeRegistro && (
          <div className="legalaccept">
            <label className="row ink" style={{ cursor: 'pointer', fontWeight: 600 }}>
              <input type="checkbox" checked={acepto} onChange={(e) => setAcepto(e.target.checked)} style={{ width: 18, height: 18, accentColor: 'var(--teal)', flex: 'none' }} />
              {t(id === 'terminos' ? 'legal.acceptTerms' : 'legal.acceptPrivacy')}
            </label>
            <button
              className="btn btn-primary"
              disabled={!acepto}
              onClick={() => {
                aceptarDoc(id)
                volver()
              }}
            >
              {t('legal.continue')}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export const Terms = () => <Legal id="terminos" title="legal.terms" doc={{ es: TERMINOS, en: TERMS_EN }} />
export const PrivacyPolicy = () => <Legal id="privacidad" title="legal.policy" doc={{ es: POLITICAS, en: POLICY_EN }} />
