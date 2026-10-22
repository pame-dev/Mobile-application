import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Navigate, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { CarPhoto } from '../../components/CarPhoto'
import { VerifiedShield } from '../../components/ConstanciaField'
import { Icon, type IconName } from '../../components/Icon'
import { Traducido } from '../../components/Traducido'
import { Brand, LogoutModal } from '../../components/TopNav'
import { TiempoReal } from '../../components/TiempoReal'
import { ModalHead, useUi } from '../../components/Ui'
import {
  adminAccounts,
  adminCars,
  adminFeatured,
  adminReports,
  adminStats,
  adminVerifications,
  type VerificacionAdmin,
  marcarSuspendidasVistas,
  marcarVistas,
  moderateProposal,
  resolveFeatured,
  setAccountSuspended,
  setCarEnabled,
  type AdminStats,
  type CuentaAdmin,
  type DestacadoAdmin,
  type ReporteAdmin,
  publicacionesSinHuellas,
  registrarHuellasAdmin,
} from '../../data/admin'
import { unreadCount } from '../../data/account'
import { toCar } from '../../data/api'
import { mensajeUsuario } from '../../data/auth'
import { traducir, useI18n } from '../../i18n/I18nContext'
import { nombreCatalogo } from '../../lib/catalogo'
import type { Clave } from '../../i18n/es'
import type { AnuncioRow, Car } from '../../data/types'
import { entero, haceCuanto, km, precio } from '../../lib/format'
import { publicUrl } from '../../lib/supabase'
import { huellaVisual } from '../../lib/huellaVisual'
import { useSession } from '../../session/SessionContext'
import { EVENTO_CAMBIOS, useRecargar, type Tabla } from '../../lib/tiempoReal'
import { ExportarAdmin } from './ReporteAdmin'
import { Verificaciones } from './Verificaciones'
import { RevisarRepuveModal } from './RevisarRepuve'
import { AdminHead, Badge, DocumentoVehiculoAdmin, estadoAdmin, etiquetaEstado, fecha, Ini, LineChart, PairBars, propuestaPendiente, ReasonModal, SearchBox, Tabs, useAccionesReporte, useModeracion, esReporteDeCuenta, type TabReportes } from './AdminParts'

type Seccion = 'inicio' | 'usuarios' | 'vehiculos' | 'reportes' | 'destacados' | 'verificacion'

/** Todo lo que necesita el panel; se recarga después de cada acción. */
interface Datos {
  cuentas: CuentaAdmin[]
  autos: { car: Car; row: AnuncioRow }[]
  reportes: ReporteAdmin[]
  destacados: DestacadoAdmin[]
  verificaciones: VerificacionAdmin[]
}

const capital = (s: string) => s.charAt(0).toUpperCase() + s.slice(1)

/**
 * Panel de administración (AdminDashboardScreen de la app). Solo para administradores;
 * las acciones llaman a las funciones admin_* de la BD.
 */
export function Admin() {
  const { sec = 'inicio' } = useParams()
  const seccion = sec as Seccion
  const navigate = useNavigate()
  const { account, mode, loading: cargandoSesion } = useSession()
  const { openModal, toast } = useUi()
  const { t } = useI18n()
  const [datos, setDatos] = useState<Datos | null>(null)
  const [stats, setStats] = useState<AdminStats | null>(null)
  const [rango, setRango] = useState(7)
  const [error, setError] = useState<string | null>(null)
  // En pantallas chicas la barra lateral se abre como panel desde el botón ☰.
  const [menu, setMenu] = useState(false)
  const cerrarMenu = useCallback(() => setMenu(false), [])
  const sideRef = useRef<HTMLElement>(null)
  const [sinLeer, setSinLeer] = useState(0)

  const cargar = useCallback(async () => {
    setError(null)
    try {
      const [cuentas, filas, reportes, destacados, verificaciones] = await Promise.all([adminAccounts(), adminCars(), adminReports(), adminFeatured(), adminVerifications()])
      setDatos({ cuentas, autos: filas.map((row) => ({ car: toCar(row), row })), reportes, destacados, verificaciones })
    } catch (e) {
      setError(mensajeUsuario(e))
    }
  }, [])

  const cargarStats = useCallback(() => {
    adminStats(rango)
      .then(setStats)
      .catch((e) => setError(mensajeUsuario(e)))
  }, [rango])

  useEffect(() => {
    if (mode === 'admin') void cargar()
  }, [cargar, mode])
  useEffect(() => {
    if (mode === 'admin') cargarStats()
  }, [cargarStats, mode])
  // Lo que pasa en la app (publicaciones nuevas, reportes, ediciones) aparece sin recargar.
  useRecargar(
    () => {
      if (mode !== 'admin') return
      void cargar()
      cargarStats()
    },
    // notificaciones: así llegan las solicitudes de verificación nuevas.
    ['publicaciones', 'propuestas_publicacion', 'fotos_publicacion', 'reportes', 'solicitudes_destacado', 'cuentas', 'notificaciones'],
  )

  // Número en la campana: avisos sin leer del admin (como en TopNav).
  const uid = account?.id
  useEffect(() => {
    if (!uid) return setSinLeer(0)
    const revisar = () => void unreadCount(uid).then(setSinLeer).catch(() => undefined)
    revisar()
    const onCambios = (e: Event) => {
      if ((e as CustomEvent<Tabla[]>).detail.includes('notificaciones')) revisar()
    }
    window.addEventListener('karsy:notificaciones', revisar)
    window.addEventListener(EVENTO_CAMBIOS, onCambios)
    return () => {
      window.removeEventListener('karsy:notificaciones', revisar)
      window.removeEventListener(EVENTO_CAMBIOS, onCambios)
    }
  }, [uid])

  /** Después de una acción: aviso y todo se vuelve a leer de la BD. */
  const listo = (msg: string) => {
    toast(msg)
    void cargar()
    cargarStats()
  }

  useEffect(() => {
    if (!menu) return
    sideRef.current?.querySelector<HTMLButtonElement>('.sideclose')?.focus()
    // Mientras está abierto, la página de atrás no se desplaza.
    const antes = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') return cerrarMenu()
      // Tab no sale del panel mientras está abierto.
      if (e.key !== 'Tab' || !sideRef.current) return
      const enfocables = [...sideRef.current.querySelectorAll<HTMLElement>('button, input, select, a[href]')].filter((el) => !el.hasAttribute('disabled'))
      const primero = enfocables[0]
      const ultimo = enfocables[enfocables.length - 1]
      if (e.shiftKey && document.activeElement === primero) {
        e.preventDefault()
        ultimo?.focus()
      } else if (!e.shiftKey && document.activeElement === ultimo) {
        e.preventDefault()
        primero?.focus()
      }
    }
    // Si la ventana se agranda, la barra lateral vuelve a verse fija.
    const ancho = window.matchMedia('(min-width: 1001px)')
    const onAncho = () => ancho.matches && cerrarMenu()
    window.addEventListener('keydown', onKey)
    ancho.addEventListener('change', onAncho)
    return () => {
      document.body.style.overflow = antes
      window.removeEventListener('keydown', onKey)
      ancho.removeEventListener('change', onAncho)
      document.querySelector<HTMLButtonElement>('.adminburger')?.focus()
    }
  }, [menu, cerrarMenu])

  if (cargandoSesion) return <div className="skeleton" style={{ height: '100vh', borderRadius: 0 }} />
  if (!account) return <Navigate to="/login" replace state={{ from: '/admin' }} />
  if (mode !== 'admin') return <Navigate to="/" replace />
  // La sección Lotes se quitó: los lotes se ven en Usuarios con el filtro "Lote".
  if (sec === 'lotes') return <Navigate to="/admin/usuarios?tipo=lote" replace />

  const pendientes = {
    vehiculos: datos?.autos.filter((a) => propuestaPendiente(a.row) != null).length ?? 0,
    reportes: datos?.reportes.filter((r) => r.estado_reporte === 'pendiente').length ?? 0,
    destacados: datos?.destacados.filter((d) => d.estado_solicitud === 'pendiente').length ?? 0,
    verificacion: datos?.verificaciones.filter((v) => v.estado === 'pendiente').length ?? 0,
  }
  const items: [Seccion, Clave, IconName, number?][] = [
    ['inicio', 'admin.sec.home', 'home'],
    ['usuarios', 'admin.sec.users', 'users'],
    ['vehiculos', 'admin.sec.vehicles', 'car', pendientes.vehiculos],
    ['reportes', 'admin.sec.reports', 'flag', pendientes.reportes],
    ['destacados', 'admin.sec.featured', 'star', pendientes.destacados],
    ['verificacion', 'admin.sec.verification', 'shield', pendientes.verificacion],
  ]
  const ir = (s: Seccion, filtro?: string) => navigate((s === 'inicio' ? '/admin' : `/admin/${s}`) + (filtro ? `?${filtro}` : ''))
  /** Navega y, si el panel móvil estaba abierto, lo cierra. */
  const elegir = (accion: () => void) => {
    setMenu(false)
    accion()
  }
  const totalPendientes = pendientes.vehiculos + pendientes.reportes + pendientes.destacados + pendientes.verificacion

  let contenido
  if (error && !datos) {
    contenido = (
      <div className="card empty">
        <p className="muted">{error}</p>
        <button className="btn btn-ghost" onClick={() => void cargar()}>
          {t('common.retry')}
        </button>
      </div>
    )
  } else if (!datos) {
    contenido = <div className="skeleton" style={{ height: 420 }} />
  } else if (seccion === 'usuarios') contenido = <Usuarios datos={datos} selfId={account.id} listo={listo} onVisto={cargarStats} />
  else if (seccion === 'vehiculos') contenido = <Vehiculos datos={datos} listo={listo} onVisto={cargarStats} />
  else if (seccion === 'reportes') contenido = <Reportes datos={datos} listo={listo} />
  else if (seccion === 'destacados') contenido = <Destacados datos={datos} listo={listo} />
  else if (seccion === 'verificacion') contenido = <Verificaciones verificaciones={datos.verificaciones} listo={listo} />
  else contenido = <Inicio nombre={account.nombre} stats={stats} verificaciones={pendientes.verificacion} rango={rango} setRango={setRango} ir={ir} />

  return (
    <div className={`admin ${menu ? 'menuabierto' : ''}`}>
      {/* Avisos en vivo (solicitudes nuevas, etc.): el panel no usa el Layout que lo monta. */}
      <TiempoReal />
      {/* Solo en pantallas chicas: logo y botón ☰ en lugar de la barra lateral. */}
      <header className="admintop">
        <Brand />
        <div className="row" style={{ gap: 8 }}>
          <button className="iconbtn" style={{ position: 'relative' }} aria-label={sinLeer ? t('nav.notificationsUnread', { n: sinLeer }) : t('nav.notifications')} onClick={() => navigate('/notificaciones')}>
            <Icon name="bell" size={19} />
            {sinLeer > 0 && <span className="dot">{sinLeer > 99 ? '99+' : sinLeer}</span>}
          </button>
          <button className="iconbtn adminburger" aria-label={t('nav.openMenu')} aria-expanded={menu} onClick={() => setMenu(true)}>
            <Icon name="menu" size={20} />
            {totalPendientes > 0 && <span className="cnt">{totalPendientes}</span>}
          </button>
        </div>
      </header>
      <div className="adminfondo" onClick={cerrarMenu} />
      <aside ref={sideRef} className="side" aria-label={t('nav.menu')} {...(menu ? { role: 'dialog', 'aria-modal': true } : {})}>
        <Brand />
        <button className="iconbtn sideclose" aria-label={t('nav.closeMenu')} onClick={cerrarMenu}>
          <Icon name="x" size={18} />
        </button>
        {items.map(([k, clave, i, n]) => (
          <button key={k} className={`it ${seccion === k ? 'on' : ''}`} onClick={() => elegir(() => ir(k))}>
            <Icon name={i} />
            {t(clave)}
            {!!n && <span className="cnt">{n}</span>}
          </button>
        ))}
        {/* Minimenú del administrador: siempre visible al fondo de la barra. */}
        <nav className="adminmenu" aria-label={t('nav.myAccount')}>
          <div className="amhead">
            <Ini name={account.nombre} src={publicUrl(account.fotoPerfil)} />
            <div style={{ minWidth: 0 }}>
              <b>{account.nombre}</b>
              <span>{t('admin.administrator')}</span>
            </div>
          </div>
          <hr />
          <button className="it" onClick={() => elegir(() => navigate('/'))}>
            <Icon name="home" />
            {t('admin.backHome')}
          </button>
          <button className="it" onClick={() => elegir(() => navigate('/notificaciones'))}>
            <Icon name="bell" />
            {t('nav.notifications')}
            {sinLeer > 0 && <span className="cnt">{sinLeer > 99 ? '99+' : sinLeer}</span>}
          </button>
          <button className="it" onClick={() => elegir(() => navigate('/perfil'))}>
            <Icon name="user" />
            {t('nav.myProfile')}
          </button>
          <button className="it" onClick={() => elegir(() => navigate('/configuracion'))}>
            <Icon name="settings" />
            {t('nav.settings')}
          </button>
          <hr />
          <button className="it danger" onClick={() => elegir(() => openModal(<LogoutModal />))}>
            <Icon name="logout" />
            {t('nav.logout')}
          </button>
        </nav>
      </aside>
      <main className="adminmain">{contenido}</main>
    </div>
  )
}

// ── Inicio ─────────────────────────────────────────────────────────────────

function Inicio({ nombre, stats, verificaciones, rango, setRango, ir }: { nombre: string; stats: AdminStats | null; verificaciones: number; rango: number; setRango: (n: number) => void; ir: (s: Seccion, filtro?: string) => void }) {
  const { t } = useI18n()
  const head = (
    <AdminHead
      title={t('admin.greeting', { name: nombre.split(' ')[0] })}
      desc={t('admin.homeDesc')}
      right={
        // Alineados arriba: el selector de días trae margen abajo y desfasaba el botón.
        <div className="row" style={{ gap: 10, flexWrap: 'wrap', alignItems: 'flex-start' }}>
          <ExportarAdmin stats={stats} dias={rango} />
          <Tabs value={String(rango)} onChange={(v) => setRango(Number(v))} options={[7, 30, 90].map((d) => [String(d), t('admin.rangeDays', { n: d })] as [string, string])} />
        </div>
      }
    />
  )
  if (!stats)
    return (
      <>
        {head}
        <div className="skeleton" style={{ height: 420 }} />
      </>
    )

  const total = stats.activas + stats.deshabilitadas
  const pct = total ? Math.round((stats.activas * 100) / total) : 0
  const vistas = stats.interacciones.vistas.reduce((a, b) => a + b, 0)
  const contactos = stats.interacciones.contactos.reduce((a, b) => a + b, 0)
  const conversion = vistas ? Math.round((contactos * 100) / vistas) : 0
  const kpis: [string, number, number, IconName, string][] = [
    [t('admin.kpiUsers'), stats.usuarios, stats.usuariosNuevos, 'users', '#3B82F6'],
    [t('admin.kpiLots'), stats.lotes, stats.lotesNuevos, 'building', '#8B5CF6'],
    [t('admin.kpiPublished'), stats.publicadas, stats.publicadasNuevas, 'car', '#22C55E'],
  ]
  const plural = (n: number, uno: Clave, varios: Clave, vars: Record<string, number> = {}) => t(n === 1 ? uno : varios, { n, ...vars })
  // Solo lo que realmente tiene pendientes.
  const alertas: [number, string, Seccion, string, string?][] = (
    [
      [stats.propuestasPendientes, plural(stats.propuestasPendientes, 'admin.alertReviewOne', 'admin.alertReviewMany'), 'vehiculos', '#F59E0B'],
      [stats.reportesPendientes, plural(stats.reportesPendientes, 'admin.alertReportsOne', 'admin.alertReportsMany'), 'reportes', '#EF4444'],
      [stats.destacadosPendientes, plural(stats.destacadosPendientes, 'admin.alertFeaturedOne', 'admin.alertFeaturedMany'), 'destacados', '#F59E0B'],
      // Solicitudes de verificación sin revisar: se quitan al aprobarlas o rechazarlas.
      [verificaciones, plural(verificaciones, 'admin.alertVerificationOne', 'admin.alertVerificationMany'), 'verificacion', '#14B8A6'],
      [stats.lotesNuevos, plural(stats.lotesNuevos, 'admin.alertLotsOne', 'admin.alertLotsMany', { d: rango }), 'usuarios', '#3B82F6', 'tipo=lote'],
      [stats.suspendidasSinVer, plural(stats.suspendidasSinVer, 'admin.alertSuspendedOne', 'admin.alertSuspendedMany'), 'usuarios', '#98A2B3', 'tipo=suspendido'],
      // Se quitan cuando el admin abre ese filtro en Vehículos (admin_marcar_vistas).
      [stats.deshabilitadasSinVer, plural(stats.deshabilitadasSinVer, 'admin.alertDisabledOne', 'admin.alertDisabledMany'), 'vehiculos', '#98A2B3', 'estado=Deshabilitado'],
      [stats.pausadasSinVer, plural(stats.pausadasSinVer, 'admin.alertPausedOne', 'admin.alertPausedMany'), 'vehiculos', '#98A2B3', 'estado=Pausado'],
    ] as [number, string, Seccion, string, string?][]
  ).filter(([n]) => n > 0)

  return (
    <>
      {head}
      {/* Pendientes primero: es lo que el admin tiene que atender (igual que la app). */}
      <div className="card" style={{ padding: 22, marginBottom: 18 }}>
        <h3 style={{ fontSize: 17 }} className="row">
          <Icon name="alert" size={18} /> {t('admin.needsAttention')}
        </h3>
        {alertas.length === 0 ? (
          <p className="muted" style={{ marginTop: 10 }}>
            {t('admin.allCaughtUp')}
          </p>
        ) : (
          <div className="grid3" style={{ marginTop: 14 }}>
            {alertas.map(([, txt, s, c, filtro]) => (
              <button key={txt} className="card row" style={{ padding: '14px 16px', textAlign: 'left', width: '100%' }} onClick={() => ir(s, filtro)}>
                <span style={{ width: 10, height: 10, borderRadius: '50%', flex: 'none', background: c }} />
                <span className="grow" style={{ color: 'var(--ink)', fontWeight: 500 }}>
                  {txt}
                </span>
                <span style={{ color: 'var(--admin-blue)', fontSize: 12.5, fontWeight: 600 }}>{t('admin.seeAll')}</span>
              </button>
            ))}
          </div>
        )}
      </div>
      <div className="akpis">
        {kpis.map(([k, n, nuevos, i, c]) => (
          <div key={k} className="card kpi">
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <span className="ic" style={{ background: `${c}22`, color: c }}>
                <Icon name={i} />
              </span>
              <span className="badge b-succ">+{entero(nuevos)}</span>
            </div>
            <b className="num">{entero(n)}</b>
            <span className="muted" style={{ fontSize: 13 }}>
              {k}
            </span>
          </div>
        ))}
        <div className="card kpi">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <span className="ic" style={{ background: '#22C55E22', color: '#22C55E' }}>
              <Icon name="check" />
            </span>
            <b style={{ fontSize: 22 }}>{pct}%</b>
          </div>
          <span className="muted" style={{ fontSize: 13 }}>
            {t('admin.kpiActive')}
          </span>
          <div style={{ height: 8, borderRadius: 9, background: 'var(--warning)', overflow: 'hidden', opacity: 0.9 }}>
            <div style={{ width: `${pct}%`, height: '100%', background: '#22C55E' }} />
          </div>
          <span className="mid" style={{ fontSize: 12 }}>
            {t('admin.kpiStatusDetail', { a: entero(stats.activas), d: entero(stats.deshabilitadas) })}
          </span>
        </div>
      </div>

      <div className="twocol half" style={{ marginTop: 18 }}>
        <div className="card" style={{ padding: 22 }}>
          <h3 style={{ fontSize: 17 }}>{t('admin.growthTitle')}</h3>
          <p className="muted" style={{ fontSize: 13 }}>
            {t('admin.growthSub')}
          </p>
          <div className="legend" style={{ margin: '12px 0' }}>
            <span>
              <i style={{ background: '#3B82F6' }} />
              {t('admin.legendUsers')}
            </span>
            <span>
              <i style={{ background: '#8B5CF6' }} />
              {t('admin.legendLots')}
            </span>
            <span>
              <i style={{ background: '#22C55E' }} />
              {t('admin.legendListings')}
            </span>
          </div>
          <LineChart
            labels={stats.crecimiento.etiquetas}
            series={[
              ['#3B82F6', stats.crecimiento.usuarios],
              ['#8B5CF6', stats.crecimiento.lotes],
              ['#22C55E', stats.crecimiento.publicaciones],
            ]}
          />
        </div>
        <div className="card" style={{ padding: 22 }}>
          <div className="row" style={{ justifyContent: 'space-between', flexWrap: 'wrap' }}>
            <div>
              <h3 style={{ fontSize: 17 }}>{t('admin.interestTitle')}</h3>
              <p className="muted" style={{ fontSize: 13 }}>
                {t('admin.interestSub')}
              </p>
            </div>
            {/* Totales del rango elegido, junto al título (antes eran tres cajas bajo la gráfica). */}
            <div className="row" style={{ gap: 6, flexWrap: 'wrap' }}>
              <span className="badge b-info">
                {t('admin.views')} <b className="num">{entero(vistas)}</b>
              </span>
              <span className="badge b-neu">
                {t('admin.contacts')} <b className="num">{entero(contactos)}</b>
              </span>
              <span className="badge b-succ">
                {t('admin.conversion')} <b className="num">{conversion}%</b>
              </span>
            </div>
          </div>
          <div className="legend" style={{ margin: '12px 0' }}>
            <span>
              <i style={{ background: '#3B82F6' }} />
              {t('admin.views')}
            </span>
            <span>
              <i style={{ background: 'var(--teal)' }} />
              {t('admin.contacts')}
            </span>
          </div>
          <PairBars labels={stats.interacciones.etiquetas} a={stats.interacciones.vistas} b={stats.interacciones.contactos} />
        </div>
      </div>
    </>
  )
}

// ── Usuarios y lotes ───────────────────────────────────────────────────────

const estadoCuenta = (c: CuentaAdmin) => (c.estado_cuenta === 'suspendida' ? 'Suspendido' : 'Activo')
const tipoCuenta = (c: CuentaAdmin) => traducir(c.tipo_cuenta === 'lote' ? 'admin.lot' : 'admin.individual')

/** Suspende (con motivo) o reactiva una cuenta. */
function useSuspender(listo: (m: string) => void) {
  const { openModal } = useUi()
  const { t } = useI18n()
  return {
    suspender: (c: CuentaAdmin) =>
      openModal(
        <ReasonModal
          title={t(c.tipo_cuenta === 'lote' ? 'admin.suspendLotTitle' : 'admin.suspendTitle')}
          desc={t('admin.suspendDesc')}
          hint={t('admin.suspendHint')}
          confirm={t('admin.suspend')}
          working={t('admin.suspending')}
          onConfirm={async (m) => {
            await setAccountSuspended(c.id_cuenta, true, m)
            listo(t('admin.msgSuspended'))
          }}
        />,
      ),
    reactivar: async (c: CuentaAdmin) => {
      await setAccountSuspended(c.id_cuenta, false, 'Reactivada desde el panel de administración.')
      listo(t('admin.msgReactivated'))
    },
  }
}

function UserDetail({ c, selfId, listo }: { c: CuentaAdmin; selfId: string; listo: (m: string) => void }) {
  const { closeModal, toast } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const { suspender, reactivar } = useSuspender(listo)
  const [busy, setBusy] = useState(false)
  const suspendida = c.estado_cuenta === 'suspendida'
  // No se puede suspender a otro administrador ni a uno mismo.
  const puede = !c.es_admin && c.id_cuenta !== selfId
  return (
    <div className="modal">
      <ModalHead title={t('admin.userDetail')} />
      <div className="row">
        <Ini name={c.nombre_mostrar} src={publicUrl(c.foto_perfil)} />
        <div>
          <b style={{ color: 'var(--ink)' }}>{c.nombre_mostrar}</b>
          <div className="muted" style={{ fontSize: 12.5 }}>
            {c.correo}
          </div>
        </div>
      </div>
      <div className="ficha" style={{ gridTemplateColumns: '1fr 1fr' }}>
        {(
          [
            [t('admin.accountType'), c.es_admin ? t('admin.typeAdmin', { type: tipoCuenta(c) }) : tipoCuenta(c)],
            [t('admin.status'), etiquetaEstado(estadoCuenta(c))],
            [t('admin.registrationDate'), fecha(c.fecha_creacion)],
            [t('admin.listings'), t('admin.listingsCount', { n: c.publicaciones, a: c.activas })],
            [t('admin.location'), [c.municipio, c.estado].filter(Boolean).join(', ') || '—'],
            ...(c.tipo_cuenta === 'lote' ? [[t('admin.manager'), c.responsable || '—']] : []),
          ] as [string, string][]
        ).map(([k, v]) => (
          <div key={k}>
            <span>{k}</span>
            <b>{v}</b>
          </div>
        ))}
      </div>
      <div className="grid2">
        <button
          className="btn btn-ghost"
          onClick={() => {
            closeModal()
            navigate(c.tipo_cuenta === 'lote' ? `/lotes/${c.id_cuenta}` : `/vendedor/${c.id_cuenta}`)
          }}
        >
          {t('admin.viewProfile')}
        </button>
        {puede &&
          (suspendida ? (
            <button
              className="btn btn-primary"
              disabled={busy}
              onClick={async () => {
                setBusy(true)
                try {
                  await reactivar(c)
                  closeModal()
                } catch (e) {
                  toast(mensajeUsuario(e))
                  setBusy(false)
                }
              }}
            >
              {t('admin.reactivateAccount')}
            </button>
          ) : (
            <button className="btn btn-danger" onClick={() => suspender(c)}>
              {t(c.tipo_cuenta === 'lote' ? 'admin.suspendLot' : 'admin.suspendAccount')}
            </button>
          ))}
      </div>
    </div>
  )
}

function Usuarios({ datos, selfId, listo, onVisto }: { datos: Datos; selfId: string; listo: (m: string) => void; onVisto: () => void }) {
  const { openModal, toast } = useUi()
  const { t } = useI18n()
  const [q, setQ] = useState('')
  // Desde "Requiere atención" (o el enlace viejo /admin/lotes) llega con ?tipo=lote o ?tipo=suspendido.
  const [params] = useSearchParams()
  const [tab, setTab] = useState<'todos' | 'particular' | 'lote' | 'suspendido'>(() => {
    const tipo = params.get('tipo')
    return tipo === 'lote' || tipo === 'suspendido' ? tipo : 'todos'
  })
  // Al ver "Suspendido", esas cuentas dejan de salir en "Requiere atención".
  useEffect(() => {
    if (tab === 'suspendido') marcarSuspendidasVistas().then(onVisto).catch((e) => toast(mensajeUsuario(e)))
  }, [tab, onVisto, toast])
  const c = datos.cuentas
  const lista = c
    .filter((x) => (tab === 'todos' ? true : tab === 'suspendido' ? x.estado_cuenta === 'suspendida' : x.tipo_cuenta === tab))
    .filter((x) => !q.trim() || `${x.nombre_mostrar} ${x.correo}`.toLowerCase().includes(q.trim().toLowerCase()))
  return (
    <>
      <AdminHead title={t('admin.sec.users')} desc={t('admin.usersDesc')} right={<SearchBox value={q} onChange={setQ} placeholder={t('admin.usersSearch')} />} />
      <Tabs
        value={tab}
        onChange={setTab}
        options={[
          ['todos', t('admin.withCount', { label: t('admin.all'), n: c.length })],
          ['particular', t('admin.withCount', { label: t('admin.individual'), n: c.filter((x) => x.tipo_cuenta === 'particular').length })],
          ['lote', t('admin.withCount', { label: t('admin.lot'), n: c.filter((x) => x.tipo_cuenta === 'lote').length })],
          ['suspendido', t('admin.withCount', { label: t('admin.st.suspended'), n: c.filter((x) => x.estado_cuenta === 'suspendida').length })],
        ]}
      />
      <div className="card tablewrap">
        <table className="t">
          <thead>
            <tr>
              <th>{t('admin.name')}</th>
              <th>{t('admin.email')}</th>
              <th>{t('admin.accountType')}</th>
              <th>{t('admin.status')}</th>
              <th>{t('admin.registrationDate')}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {lista.map((x) => (
              <tr key={x.id_cuenta}>
                <td>
                  <div className="mini">
                    <Ini name={x.nombre_mostrar} src={publicUrl(x.foto_perfil)} />
                    <b style={{ color: 'var(--ink)' }}>{x.nombre_mostrar}</b>
                    {x.verificado && <VerifiedShield />}
                  </div>
                </td>
                <td data-label={t('admin.email')}>{x.correo}</td>
                <td data-label={t('admin.accountType')}>{x.es_admin ? t('admin.typeAdmin', { type: tipoCuenta(x) }) : tipoCuenta(x)}</td>
                <td data-label={t('admin.status')}>
                  <Badge s={estadoCuenta(x)} />
                </td>
                <td className="num" data-label={t('admin.registrationDate')}>{fecha(x.fecha_creacion)}</td>
                <td>
                  <button className="btn btn-ghost btn-sm" onClick={() => openModal(<UserDetail c={x} selfId={selfId} listo={listo} />)}>
                    {t('admin.viewDetail')}
                  </button>
                </td>
              </tr>
            ))}
            {lista.length === 0 && (
              <tr>
                <td colSpan={6} className="muted" style={{ textAlign: 'center', padding: 30 }}>
                  {t('admin.usersEmpty')}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  )
}

// ── Vehículos ──────────────────────────────────────────────────────────────

function VehicleReview({ car, row, listo }: { car: Car; row: AnuncioRow; listo: (m: string) => void }) {
  const { openModal, closeModal, toast } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const propuesta = propuestaPendiente(row)
  const habilitada = row.estado_administrativo === 'habilitada'
  const estado = estadoAdmin(row, car)
  const moderacion = useModeracion()

  const correr = async (accion: () => Promise<void>, msg: string) => {
    setBusy(true)
    try {
      await accion()
      closeModal()
      listo(msg)
    } catch (e) {
      toast(mensajeUsuario(e))
      setBusy(false)
    }
  }

  return (
    <div className="modal wide">
      <ModalHead title={t('admin.vehicleDetail')} sub={`${t('admin.listingNumber', { n: car.id })} · ${car.ownerName}`} />
      <CarPhoto src={car.imageUrl} alt={`${car.brand} ${car.model}`} style={{ height: 220, borderRadius: 14 }} />
      <div className="row" style={{ justifyContent: 'space-between', flexWrap: 'wrap' }}>
        <b style={{ fontFamily: 'var(--f-display)', fontSize: 19, color: 'var(--ink)' }}>
          {car.brand} {car.model} {car.year}
        </b>
        <span className="vprice num">{precio(car.priceValue)}</span>
      </div>
      <div className="row" style={{ gap: 8 }}>
        <Badge s={estado} />
        {row.estado_ultima_propuesta === 'rechazada' && row.motivo_rechazo_propuesta && (
          <span className="muted" style={{ fontSize: 12.5 }}>
            <Traducido texto={row.motivo_rechazo_propuesta}>{(m) => t('admin.reason', { m })}</Traducido>
          </span>
        )}
        {!habilitada && row.motivo_deshabilitacion && (
          <span className="muted" style={{ fontSize: 12.5 }}>
            <Traducido texto={row.motivo_deshabilitacion}>{(m) => t('admin.reason', { m })}</Traducido>
          </span>
        )}
      </div>
      <div className="ficha">
        {(
          [
            [t('admin.transmission'), row.transmision ? nombreCatalogo(row.transmision) : '—'],
            [t('admin.mileage'), km(row.kilometraje)],
            [t('admin.bodyType'), row.carroceria ? nombreCatalogo(row.carroceria) : '—'],
            [t('admin.color'), row.color ? nombreCatalogo(row.color) : '—'],
            [t('admin.fuel'), row.tipo_combustible ? nombreCatalogo(row.tipo_combustible) : '—'],
            [t('admin.location'), car.city || '—'],
          ] as [string, string][]
        ).map(([k, v]) => (
          <div key={k}>
            <span>{k}</span>
            <b>{v}</b>
          </div>
        ))}
      </div>
      <DocumentoVehiculoAdmin idPublicacion={car.id} recargar={row.id_ultima_propuesta} />
      {car.description && (
        <p className="muted" style={{ whiteSpace: 'pre-line', maxHeight: 140, overflow: 'auto' }}>
          {car.description}
        </p>
      )}
      <div className="grid3">
        {propuesta != null ? (
          <>
            <button
              className="btn btn-ghost"
              disabled={busy}
              onClick={() => moderacion.rechazar(propuesta, listo)}
            >
              {t('admin.reject')}
            </button>
            <button
              className="btn btn-ghost"
              onClick={() => {
                closeModal()
                navigate(`/vehiculo/${car.id}`, { state: { revisionAdmin: true } })
              }}
            >
              {t('admin.viewListing')}
            </button>
            {/* Se aprueba desde la ventana con la guía para consultar el NIV o la placa en REPUVE. */}
            <button
              className="btn btn-primary"
              disabled={busy}
              onClick={() =>
                openModal(
                  <RevisarRepuveModal
                    car={car}
                    recargar={row.id_ultima_propuesta}
                    onAprobar={async () => {
                      try {
                        await moderateProposal(propuesta, true, null)
                        listo(t('admin.msgApproved'))
                        return true
                      } catch (e) {
                        toast(mensajeUsuario(e))
                        return false
                      }
                    }}
                    onRechazar={() => moderacion.rechazar(propuesta, listo)}
                  />,
                )
              }
            >
              {t('admin.repuveReview')}
            </button>
          </>
        ) : (
          <>
            <button
              className="btn btn-ghost"
              onClick={() => {
                closeModal()
                navigate(`/vehiculo/${car.id}`, { state: { revisionAdmin: true } })
              }}
            >
              {t('admin.viewListing')}
            </button>
            <span />
            {row.aprobada &&
              (habilitada ? (
                <button
                  className="btn btn-danger"
                  onClick={() =>
                    openModal(
                      <ReasonModal
                        title={t('admin.disableTitle')}
                        desc={t('admin.disableDesc')}
                        hint={t('admin.disableHint')}
                        confirm={t('admin.disable')}
                        working={t('admin.disabling')}
                        onConfirm={async (m) => {
                          await setCarEnabled(car.id, false, m)
                          listo(t('admin.msgDisabled'))
                        }}
                      />,
                    )
                  }
                >
                  {t('admin.disable')}
                </button>
              ) : (
                <button className="btn btn-primary" disabled={busy} onClick={() => correr(() => setCarEnabled(car.id, true, 'Habilitada desde el panel.'), t('admin.msgEnabled'))}>
                  {t('admin.enable')}
                </button>
              ))}
          </>
        )}
      </div>
    </div>
  )
}

const FILTROS_VEHICULO = ['Todos', 'Pendiente', 'Cambios pendientes', 'Activo', 'Rechazado', 'Deshabilitado', 'Pausado', 'Vendido'] as const

/**
 * Una sola vez: registra la huella visual de las fotos de las publicaciones que ya existían,
 * para que tampoco se puedan volver a publicar en otra cuenta. Solo aparece si faltan.
 */
function RegistrarHuellas() {
  const { t } = useI18n()
  const { toast } = useUi()
  const [faltan, setFaltan] = useState<{ id: number; fotos: string[] }[] | null>(null)
  const [avance, setAvance] = useState<[number, number] | null>(null)
  useEffect(() => {
    publicacionesSinHuellas().then(setFaltan).catch(() => setFaltan([]))
  }, [])
  if (!faltan?.length) return null
  const registrar = async () => {
    setAvance([0, faltan.length])
    let hechas = 0
    for (const p of faltan) {
      const huellas = await Promise.all(p.fotos.map((r) => (publicUrl(r) ? huellaVisual(publicUrl(r)!) : Promise.resolve(null))))
      await registrarHuellasAdmin(p.id, huellas.filter((h): h is string => !!h)).catch(() => undefined)
      setAvance([++hechas, faltan.length])
    }
    setFaltan([])
    setAvance(null)
    toast(t('admin.fingerprintsDone', { n: hechas }))
  }
  return (
    <div className="note row" style={{ background: 'var(--teal-light)', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10, marginBottom: 14 }}>
      <span>{t('admin.fingerprintsPending', { n: faltan.length })}</span>
      <button className="btn btn-primary btn-sm" disabled={!!avance} onClick={() => void registrar()}>
        {avance ? t('admin.fingerprintsProgress', { n: avance[0], total: avance[1] }) : t('admin.fingerprintsButton')}
      </button>
    </div>
  )
}

function Vehiculos({ datos, listo, onVisto }: { datos: Datos; listo: (m: string) => void; onVisto: () => void }) {
  const { openModal, toast } = useUi()
  const { t } = useI18n()
  const [q, setQ] = useState('')
  // Desde "Requiere atención" llega con ?estado=Deshabilitado o ?estado=Pausado.
  const [params] = useSearchParams()
  const [tab, setTab] = useState<(typeof FILTROS_VEHICULO)[number]>(() => FILTROS_VEHICULO.find((f) => f === params.get('estado')) ?? 'Todos')
  // Al ver ese filtro, sus publicaciones dejan de salir en "Requiere atención".
  useEffect(() => {
    const estado = tab === 'Deshabilitado' ? 'deshabilitada' : tab === 'Pausado' ? 'pausada' : null
    if (estado) marcarVistas(estado).then(onVisto).catch((e) => toast(mensajeUsuario(e)))
  }, [tab, onVisto, toast])
  const conEstado = useMemo(() => datos.autos.map((a) => ({ ...a, estado: estadoAdmin(a.row, a.car) })), [datos.autos])
  const lista = conEstado
    .filter((a) => tab === 'Todos' || a.estado === tab)
    .filter((a) => !q.trim() || `${a.car.brand} ${a.car.model} ${a.car.year} ${a.car.ownerName}`.toLowerCase().includes(q.trim().toLowerCase()))
  const cuenta = (s: string) => conEstado.filter((a) => a.estado === s).length
  return (
    <>
      <AdminHead title={t('admin.sec.vehicles')} desc={t('admin.vehiclesDesc')} right={<SearchBox value={q} onChange={setQ} placeholder={t('admin.vehiclesSearch')} />} />
      <RegistrarHuellas />
      <Tabs
        value={tab}
        onChange={setTab}
        options={FILTROS_VEHICULO.map((f) => [f, f === 'Todos' ? t('admin.withCount', { label: t('admin.all'), n: conEstado.length }) : t('admin.withCount', { label: etiquetaEstado(f), n: cuenta(f) })])}
      />
      <div className="card tablewrap">
        <table className="t">
          <thead>
            <tr>
              <th>{t('admin.vehicle')}</th>
              <th>{t('admin.seller')}</th>
              <th>{t('admin.year')}</th>
              <th>{t('admin.price')}</th>
              <th>{t('admin.status')}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {lista.map(({ car, row, estado }) => {
              const revisar = estado === 'Pendiente' || estado === 'Cambios pendientes'
              return (
                <tr key={car.id}>
                  <td>
                    <div className="mini">
                      <CarPhoto src={car.imageUrl} />
                      <div>
                        <b style={{ color: 'var(--ink)' }}>
                          {car.brand} {car.model}
                        </b>
                        <div className="mid" style={{ fontSize: 12 }}>
                          {t('admin.listingNumber', { n: car.id })}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td data-label={t('admin.seller')}>{car.ownerName}</td>
                  <td data-label={t('admin.year')}>{car.year || '—'}</td>
                  <td className="num" data-label={t('admin.price')}>{precio(car.priceValue)}</td>
                  <td data-label={t('admin.status')}>
                    <Badge s={estado} />
                  </td>
                  <td>
                    <button className={`btn btn-sm ${revisar ? 'btn-primary' : 'btn-ghost'}`} onClick={() => openModal(<VehicleReview car={car} row={row} listo={listo} />)}>
                      {t('admin.review')}
                    </button>
                  </td>
                </tr>
              )
            })}
            {lista.length === 0 && (
              <tr>
                <td colSpan={6} className="muted" style={{ textAlign: 'center', padding: 30 }}>
                  {t('admin.vehiclesEmpty')}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  )
}

// ── Reportes ───────────────────────────────────────────────────────────────

function ReportReview({ r, datos, listo, tab }: { r: ReporteAdmin; datos: Datos; listo: (m: string) => void; tab: TabReportes }) {
  const { closeModal } = useUi()
  const navigate = useNavigate()
  const location = useLocation()
  const { t } = useI18n()
  const auto = datos.autos.find((a) => a.car.id === r.id_publicacion)
  const deCuenta = esReporteDeCuenta(r)
  const pendiente = r.estado_reporte === 'pendiente'
  const dueno = auto ? datos.cuentas.find((c) => c.id_cuenta === auto.car.ownerId) : undefined

  const acciones = useAccionesReporte()
  const descartar = () => acciones.descartar(r, listo)
  const proceder = () => acciones.proceder(r, dueno, listo)

  return (
    <div className="modal wide">
      <ModalHead title={t('admin.reportReview')} sub={t('admin.reportSub', { id: r.id_reporte, when: haceCuanto(r.fecha_reporte) })} />
      <div className="ficha" style={{ gridTemplateColumns: '1fr 1fr' }}>
        {(
          [
            [t('admin.type'), t(deCuenta ? 'admin.user' : 'admin.listing')],
            [t('admin.reportedBy'), r.reportante],
            [t('admin.status'), etiquetaEstado(capital(r.estado_reporte))],
            [t(deCuenta ? 'admin.reportedAccount' : 'admin.listing'), deCuenta ? auto?.car.ownerName ?? '—' : r.publicacion],
          ] as [string, string][]
        ).map(([k, v]) => (
          <div key={k}>
            <span>{k}</span>
            <b style={{ whiteSpace: 'normal' }}>{v}</b>
          </div>
        ))}
      </div>
      <div className="note muted" style={{ background: 'var(--bg)' }}>
        <Traducido texto={r.motivo_reporte} />
      </div>
      {r.motivo_resolucion && (
        <div className="note" style={{ background: 'var(--success-l)' }}>
          <b>{t('admin.resolution')}</b> <Traducido texto={r.motivo_resolucion} />
        </div>
      )}
      {auto && (
        <div className="listing" style={{ gridTemplateColumns: '120px 1fr' }}>
          <CarPhoto src={auto.car.imageUrl} />
          <div>
            <b style={{ color: 'var(--ink)' }}>
              {auto.car.brand} {auto.car.model} {auto.car.year}
            </b>
            <div className="muted" style={{ fontSize: 12.5 }}>
              {auto.car.ownerName} · {km(auto.car.km)}
            </div>
          </div>
        </div>
      )}
      {auto ? (
        <button
          className="btn btn-ghost btn-block"
          onClick={() => {
            closeModal()
            // El panel recuerda el reporte abierto y el filtro: al volver se restauran.
            navigate(location.pathname, { replace: true, state: { reporte: r.id_reporte, tabReportes: tab } })
            navigate(`/vehiculo/${auto.car.id}`, { state: { revisionReporte: r.id_reporte } })
          }}
        >
          {t('admin.viewListing')}
        </button>
      ) : (
        <div className="note muted" style={{ background: 'var(--bg)' }}>
          {t('admin.listingGone')}
        </div>
      )}
      {pendiente && (
        <div className="grid2">
          <button className="btn btn-ghost" onClick={descartar}>
            {t('admin.dismiss')}
          </button>
          <button className="btn btn-danger" onClick={proceder}>
            {t(deCuenta ? 'admin.suspendAccount' : 'admin.disableListing')}
          </button>
        </div>
      )}
    </div>
  )
}

function Reportes({ datos, listo }: { datos: Datos; listo: (m: string) => void }) {
  const { openModal } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const location = useLocation()
  const volviendo = location.state as { reporte?: number; tabReportes?: TabReportes } | null
  const [tab, setTab] = useState<TabReportes>(volviendo?.tabReportes ?? 'pendiente')

  // De regreso del detalle ("Volver al reporte"): se vuelve a abrir el reporte que se revisaba.
  useEffect(() => {
    if (!volviendo?.reporte) return
    const r = datos.reportes.find((x) => x.id_reporte === volviendo.reporte)
    navigate(location.pathname, { replace: true, state: null })
    if (r) openModal(<ReportReview r={r} datos={datos} listo={listo} tab={volviendo.tabReportes ?? tab} />)
    // Solo al regresar.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  const pendientes = datos.reportes.filter((r) => r.estado_reporte === 'pendiente')
  const resueltos = datos.reportes.filter((r) => r.estado_reporte !== 'pendiente')
  const lista = tab === 'pendiente' ? pendientes : tab === 'resueltos' ? resueltos : datos.reportes
  return (
    <>
      <AdminHead
        title={t('admin.sec.reports')}
        desc={t('admin.reportsDesc')}
        right={pendientes.length > 0 && <span className="badge b-dang">{t(pendientes.length === 1 ? 'admin.pendingReportsOne' : 'admin.pendingReportsMany', { n: pendientes.length })}</span>}
      />
      <Tabs
        value={tab}
        onChange={setTab}
        options={[
          ['pendiente', t('admin.withCount', { label: t('admin.tabPending'), n: pendientes.length })],
          ['resueltos', t('admin.withCount', { label: t('admin.tabResolved'), n: resueltos.length })],
          ['todos', t('admin.all')],
        ]}
      />
      <div className="card tablewrap">
        <table className="t">
          <thead>
            <tr>
              <th>{t('admin.report')}</th>
              <th>{t('admin.type')}</th>
              <th>{t('admin.reported')}</th>
              <th>{t('admin.reasonCol')}</th>
              <th>{t('admin.reportedBy')}</th>
              <th>{t('admin.date')}</th>
              <th>{t('admin.status')}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {lista.map((r) => {
              const deCuenta = esReporteDeCuenta(r)
              const dueno = datos.autos.find((a) => a.car.id === r.id_publicacion)?.car.ownerName
              return (
                <tr key={r.id_reporte}>
                  <td className="num">
                    <b style={{ color: 'var(--ink)' }}>#R-{r.id_reporte}</b>
                  </td>
                  <td data-label={t('admin.type')}>
                    <span className={`badge ${deCuenta ? 'b-info' : 'b-neu'}`}>{t(deCuenta ? 'admin.user' : 'admin.listing')}</span>
                  </td>
                  <td data-label={t('admin.reported')}>{deCuenta ? dueno ?? r.publicacion : r.publicacion}</td>
                  <td data-label={t('admin.reasonCol')} style={{ whiteSpace: 'normal', minWidth: 220 }}><Traducido texto={r.motivo_reporte.replace(/^(Publicación|Cuenta del vendedor): /, '')} corto /></td>
                  <td data-label={t('admin.reportedBy')}>{r.reportante}</td>
                  <td data-label={t('admin.date')}>{haceCuanto(r.fecha_reporte)}</td>
                  <td data-label={t('admin.status')}>
                    <Badge s={capital(r.estado_reporte)} />
                  </td>
                  <td>
                    <button className="btn btn-ghost btn-sm" onClick={() => openModal(<ReportReview r={r} datos={datos} listo={listo} tab={tab} />)}>
                      {t('admin.review')}
                    </button>
                  </td>
                </tr>
              )
            })}
            {lista.length === 0 && (
              <tr>
                <td colSpan={8} className="muted" style={{ textAlign: 'center', padding: 30 }}>
                  {t('admin.reportsEmpty')}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  )
}

// ── Destacados ─────────────────────────────────────────────────────────────

/** [valor que se guarda en la BD (en español, como la app), título, descripción]. */
const MOTIVOS_DESTACADO: [string, Clave, Clave][] = [
  ['Pago no verificado', 'admin.featPayment', 'admin.featPaymentDesc'],
  ['Calidad de imágenes', 'admin.featImages', 'admin.featImagesDesc'],
  ['Información incompleta', 'admin.featIncomplete', 'admin.featIncompleteDesc'],
  ['Vehículo no permitido', 'admin.featNotAllowed', 'admin.featNotAllowedDesc'],
  ['Otro motivo', 'admin.featOther', 'admin.featOtherDesc'],
]

function RejectFeatured({ d, listo }: { d: DestacadoAdmin; listo: (m: string) => void }) {
  const { closeModal, toast } = useUi()
  const { t } = useI18n()
  const [motivo, setMotivo] = useState('')
  const [comentario, setComentario] = useState('')
  const [busy, setBusy] = useState(false)
  return (
    <div className="modal wide">
      <ModalHead title={t('admin.featRejectTitle')} sub={t('admin.featRejectSub')} />
      {MOTIVOS_DESTACADO.map(([valor, titulo, desc]) => (
        <button key={valor} className={`opt ${motivo === valor ? 'on' : ''}`} onClick={() => setMotivo(valor)}>
          <span className="radio" />
          <span>
            <b style={{ color: 'var(--ink)' }}>{t(titulo)}</b>
            <br />
            <span className="muted" style={{ fontSize: 12.5 }}>
              {t(desc)}
            </span>
          </span>
        </button>
      ))}
      <textarea className="input" placeholder={t('admin.featCommentHint')} maxLength={300} value={comentario} onChange={(e) => setComentario(e.target.value)} />
      <div className="grid2">
        <button className="btn btn-ghost" disabled={busy} onClick={closeModal}>
          {t('common.cancel')}
        </button>
        <button
          className="btn btn-danger"
          disabled={busy || !motivo}
          onClick={async () => {
            setBusy(true)
            try {
              await resolveFeatured(d.id_solicitud_destacado, false, [motivo, comentario.trim()].filter(Boolean).join('. '))
              closeModal()
              listo(t('admin.msgFeatRejected'))
            } catch (e) {
              toast(mensajeUsuario(e))
              setBusy(false)
            }
          }}
        >
          {t('admin.featRejectNotify')}
        </button>
      </div>
    </div>
  )
}

function Aprobada({ titulo }: { titulo: string }) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  return (
    <div className="modal" style={{ textAlign: 'center', alignItems: 'center' }}>
      <span className="nic" style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--warning-l)', color: 'var(--star)' }}>
        <Icon name="star" size={30} />
      </span>
      <ModalHead title={t('admin.featApprovedTitle')} sub={t('admin.featApprovedSub', { title: titulo })} />
      <button className="btn btn-primary btn-block" onClick={closeModal}>
        {t('admin.backToRequests')}
      </button>
    </div>
  )
}

function Destacados({ datos, listo }: { datos: Datos; listo: (m: string) => void }) {
  const { openModal, toast } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const [tab, setTab] = useState<'pendiente' | 'aprobada' | 'rechazada'>('pendiente')
  const [busy, setBusy] = useState<number | null>(null)
  const cuenta = (e: string) => datos.destacados.filter((d) => d.estado_solicitud === e).length
  const lista = datos.destacados.filter((d) => d.estado_solicitud === tab)

  const aprobar = async (d: DestacadoAdmin, titulo: string) => {
    setBusy(d.id_solicitud_destacado)
    try {
      await resolveFeatured(d.id_solicitud_destacado, true, null)
      listo(t('admin.msgFeatApproved'))
      openModal(<Aprobada titulo={titulo} />)
    } catch (e) {
      toast(mensajeUsuario(e))
    }
    setBusy(null)
  }

  return (
    <>
      <AdminHead title={t('admin.featTitle')} desc={t('admin.featDesc')} />
      <Tabs
        value={tab}
        onChange={setTab}
        options={[
          ['pendiente', t('admin.withCount', { label: t('admin.tabPendingF'), n: cuenta('pendiente') })],
          ['aprobada', t('admin.withCount', { label: t('admin.tabApprovedF'), n: cuenta('aprobada') })],
          ['rechazada', t('admin.withCount', { label: t('admin.tabRejectedF'), n: cuenta('rechazada') })],
        ]}
      />
      {lista.length === 0 ? (
        <div className="card empty">
          <p className="muted">{t('admin.featEmpty')}</p>
        </div>
      ) : (
        <div className="vgrid">
          {lista.map((d) => {
            const car = datos.autos.find((a) => a.car.id === d.id_publicacion)?.car
            const titulo = car ? `${car.brand} ${car.model} (${car.year})` : t('admin.listingNumber', { n: d.id_publicacion })
            return (
              <div key={d.id_solicitud_destacado} className="card" style={{ overflow: 'hidden' }}>
                <CarPhoto src={car?.imageUrl} style={{ height: 170, cursor: 'pointer' }} onClick={() => navigate(`/vehiculo/${d.id_publicacion}`)}>
                  <span className="tag badge" style={{ background: '#FFF4CC', color: '#8A6415' }}>
                    ★ {d.estado_solicitud === 'pendiente' ? t('admin.st.requested') : etiquetaEstado(capital(d.estado_solicitud))}
                  </span>
                </CarPhoto>
                <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <b style={{ fontFamily: 'var(--f-display)', color: 'var(--ink)', fontSize: 16 }}>{titulo}</b>
                  {car && <span className="vprice num" style={{ textAlign: 'left' }}>{precio(car.priceValue)}</span>}
                  <div className="row" style={{ gap: 8 }}>
                    <Ini name={d.solicitante} src={publicUrl(d.solicitante_foto)} />
                    <p className="muted" style={{ fontSize: 12.5 }}>
                      <b>{d.solicitante}</b> {t('admin.requestedWhen', { when: haceCuanto(d.fecha_solicitud).toLowerCase() })}
                    </p>
                  </div>
                  {d.motivo_rechazo && (
                    <p className="muted" style={{ fontSize: 12.5 }}>
                      <Traducido texto={d.motivo_rechazo}>{(m) => t('admin.reason', { m })}</Traducido>
                    </p>
                  )}
                  {tab === 'pendiente' && (
                    <div className="grid2" style={{ gap: 8, marginTop: 4 }}>
                      <button className="btn btn-ghost btn-sm" disabled={busy === d.id_solicitud_destacado} onClick={() => openModal(<RejectFeatured d={d} listo={listo} />)}>
                        {t('admin.reject')}
                      </button>
                      <button className="btn btn-primary btn-sm" disabled={busy === d.id_solicitud_destacado} onClick={() => void aprobar(d, titulo)}>
                        {t('admin.approve')}
                      </button>
                    </div>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </>
  )
}
