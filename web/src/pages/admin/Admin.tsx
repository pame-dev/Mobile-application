import { useCallback, useEffect, useMemo, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { CarPhoto } from '../../components/CarPhoto'
import { Icon, type IconName } from '../../components/Icon'
import { Brand, LogoutModal } from '../../components/TopNav'
import { ModalHead, useUi } from '../../components/Ui'
import {
  adminAccounts,
  adminCars,
  adminFeatured,
  adminReports,
  adminStats,
  moderateProposal,
  resolveFeatured,
  resolveReport,
  setAccountSuspended,
  setCarEnabled,
  type AdminStats,
  type CuentaAdmin,
  type DestacadoAdmin,
  type ReporteAdmin,
} from '../../data/admin'
import { toCar } from '../../data/api'
import { mensajeUsuario } from '../../data/auth'
import type { AnuncioRow, Car } from '../../data/types'
import { entero, haceCuanto, km, precio } from '../../lib/format'
import { publicUrl } from '../../lib/supabase'
import { useSession } from '../../session/SessionContext'
import { AdminHead, Badge, fecha, Ini, LineChart, PairBars, ReasonModal, SearchBox, Tabs } from './AdminParts'

type Seccion = 'inicio' | 'usuarios' | 'lotes' | 'vehiculos' | 'reportes' | 'destacados'

/** Todo lo que necesita el panel; se recarga después de cada acción. */
interface Datos {
  cuentas: CuentaAdmin[]
  autos: { car: Car; row: AnuncioRow }[]
  reportes: ReporteAdmin[]
  destacados: DestacadoAdmin[]
}

/** Vehículo como lo ve el admin: "Cambios pendientes" si está aprobado y tiene una edición sin revisar. */
function estadoAdmin(row: AnuncioRow, car: Car): string {
  return row.aprobada && row.estado_ultima_propuesta === 'pendiente' ? 'Cambios pendientes' : car.status
}
const propuestaPendiente = (row: AnuncioRow) => (row.estado_ultima_propuesta === 'pendiente' ? row.id_ultima_propuesta ?? null : null)
const esReporteDeCuenta = (r: ReporteAdmin) => r.motivo_reporte.startsWith('Cuenta del vendedor')
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
  const [datos, setDatos] = useState<Datos | null>(null)
  const [stats, setStats] = useState<AdminStats | null>(null)
  const [rango, setRango] = useState(7)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(async () => {
    setError(null)
    try {
      const [cuentas, filas, reportes, destacados] = await Promise.all([adminAccounts(), adminCars(), adminReports(), adminFeatured()])
      setDatos({ cuentas, autos: filas.map((row) => ({ car: toCar(row), row })), reportes, destacados })
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

  /** Después de una acción: aviso y todo se vuelve a leer de la BD. */
  const listo = (msg: string) => {
    toast(msg)
    void cargar()
    cargarStats()
  }

  if (cargandoSesion) return <div className="skeleton" style={{ height: '100vh', borderRadius: 0 }} />
  if (!account) return <Navigate to="/login" replace state={{ from: '/admin' }} />
  if (mode !== 'admin') return <Navigate to="/" replace />

  const pendientes = {
    vehiculos: datos?.autos.filter((a) => propuestaPendiente(a.row) != null).length ?? 0,
    reportes: datos?.reportes.filter((r) => r.estado_reporte === 'pendiente').length ?? 0,
    destacados: datos?.destacados.filter((d) => d.estado_solicitud === 'pendiente').length ?? 0,
  }
  const items: [Seccion, string, IconName, number?][] = [
    ['inicio', 'Inicio', 'home'],
    ['usuarios', 'Usuarios', 'users'],
    ['lotes', 'Lotes', 'building'],
    ['vehiculos', 'Vehículos', 'car', pendientes.vehiculos],
    ['reportes', 'Reportes', 'flag', pendientes.reportes],
    ['destacados', 'Destacados', 'star', pendientes.destacados],
  ]
  const ir = (s: Seccion) => navigate(s === 'inicio' ? '/admin' : `/admin/${s}`)

  let contenido
  if (error && !datos) {
    contenido = (
      <div className="card empty">
        <p className="muted">{error}</p>
        <button className="btn btn-ghost" onClick={() => void cargar()}>
          Reintentar
        </button>
      </div>
    )
  } else if (!datos) {
    contenido = <div className="skeleton" style={{ height: 420 }} />
  } else if (seccion === 'usuarios') contenido = <Usuarios datos={datos} selfId={account.id} listo={listo} />
  else if (seccion === 'lotes') contenido = <Lotes datos={datos} listo={listo} />
  else if (seccion === 'vehiculos') contenido = <Vehiculos datos={datos} listo={listo} />
  else if (seccion === 'reportes') contenido = <Reportes datos={datos} listo={listo} />
  else if (seccion === 'destacados') contenido = <Destacados datos={datos} listo={listo} />
  else contenido = <Inicio nombre={account.nombre} stats={stats} rango={rango} setRango={setRango} ir={ir} />

  return (
    <div className="admin">
      <aside className="side">
        <Brand />
        {items.map(([k, t, i, n]) => (
          <button key={k} className={`it ${seccion === k ? 'on' : ''}`} onClick={() => ir(k)}>
            <Icon name={i} />
            {t}
            {!!n && <span className="cnt">{n}</span>}
          </button>
        ))}
        <div className="sp" style={{ marginTop: 'auto', borderTop: '1px solid var(--border)', paddingTop: 14 }}>
          <div className="row" style={{ padding: '0 10px 10px' }}>
            <Ini name={account.nombre} src={publicUrl(account.fotoPerfil)} />
            <div style={{ minWidth: 0 }}>
              <b style={{ color: 'var(--ink)', fontSize: 13 }}>{account.nombre}</b>
              <div className="muted" style={{ fontSize: 11.5 }}>
                Administrador
              </div>
            </div>
          </div>
        </div>
        <button className="it" onClick={() => navigate('/')}>
          <Icon name="back" />
          Volver al inicio
        </button>
        <button className="it" style={{ color: 'var(--error)' }} onClick={() => openModal(<LogoutModal />)}>
          <Icon name="logout" />
          Cerrar sesión
        </button>
      </aside>
      <main className="adminmain">{contenido}</main>
    </div>
  )
}

// ── Inicio ─────────────────────────────────────────────────────────────────

function Inicio({ nombre, stats, rango, setRango, ir }: { nombre: string; stats: AdminStats | null; rango: number; setRango: (n: number) => void; ir: (s: Seccion) => void }) {
  const head = (
    <AdminHead
      title={`¡Hola, ${nombre.split(' ')[0]}!`}
      desc="Resumen de la actividad en la plataforma."
      right={<Tabs value={String(rango)} onChange={(v) => setRango(Number(v))} options={[7, 30, 90].map((d) => [String(d), `${d} días`] as [string, string])} />}
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
    ['Usuarios registrados', stats.usuarios, stats.usuariosNuevos, 'users', '#3B82F6'],
    ['Lotes registrados', stats.lotes, stats.lotesNuevos, 'building', '#8B5CF6'],
    ['Vehículos publicados', stats.publicadas, stats.publicadasNuevas, 'car', '#22C55E'],
  ]
  const plural = (n: number, uno: string, varios: string) => `${n} ${n === 1 ? uno : varios}`
  // Solo lo que realmente tiene pendientes.
  const alertas: [number, string, Seccion, string][] = (
    [
      [stats.propuestasPendientes, plural(stats.propuestasPendientes, 'publicación pendiente de revisión', 'publicaciones pendientes de revisión'), 'vehiculos', '#F59E0B'],
      [stats.reportesPendientes, plural(stats.reportesPendientes, 'reporte pendiente', 'reportes pendientes'), 'reportes', '#EF4444'],
      [stats.destacadosPendientes, plural(stats.destacadosPendientes, 'solicitud de destacado pendiente', 'solicitudes de destacado pendientes'), 'destacados', '#F59E0B'],
      [stats.lotesNuevos, `${plural(stats.lotesNuevos, 'lote nuevo', 'lotes nuevos')} en ${rango} días`, 'lotes', '#3B82F6'],
      [stats.suspendidas, plural(stats.suspendidas, 'cuenta suspendida', 'cuentas suspendidas'), 'usuarios', '#98A2B3'],
      [stats.deshabilitadas, plural(stats.deshabilitadas, 'publicación deshabilitada', 'publicaciones deshabilitadas'), 'vehiculos', '#98A2B3'],
    ] as [number, string, Seccion, string][]
  ).filter(([n]) => n > 0)

  return (
    <>
      {head}
      <div className="akpis">
        {kpis.map(([t, n, nuevos, i, c]) => (
          <div key={t} className="card kpi">
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <span className="ic" style={{ background: `${c}22`, color: c }}>
                <Icon name={i} />
              </span>
              <span className="badge b-succ">+{entero(nuevos)}</span>
            </div>
            <b className="num">{entero(n)}</b>
            <span className="muted" style={{ fontSize: 13 }}>
              {t}
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
            Publicaciones activas
          </span>
          <div style={{ height: 8, borderRadius: 9, background: 'var(--warning)', overflow: 'hidden', opacity: 0.9 }}>
            <div style={{ width: `${pct}%`, height: '100%', background: '#22C55E' }} />
          </div>
          <span className="mid" style={{ fontSize: 12 }}>
            {entero(stats.activas)} activas · {entero(stats.deshabilitadas)} deshabilitadas
          </span>
        </div>
      </div>

      <div className="twocol half" style={{ marginTop: 18 }}>
        <div className="card" style={{ padding: 22 }}>
          <h3 style={{ fontSize: 17 }}>Crecimiento de la plataforma</h3>
          <p className="muted" style={{ fontSize: 13 }}>
            Usuarios, lotes y publicaciones acumulados
          </p>
          <div className="legend" style={{ margin: '12px 0' }}>
            <span>
              <i style={{ background: '#3B82F6' }} />
              Usuarios
            </span>
            <span>
              <i style={{ background: '#8B5CF6' }} />
              Lotes
            </span>
            <span>
              <i style={{ background: '#22C55E' }} />
              Publicaciones
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
              <h3 style={{ fontSize: 17 }}>Interés en publicaciones</h3>
              <p className="muted" style={{ fontSize: 13 }}>
                Vistas de detalle vs. contactos al vendedor
              </p>
            </div>
            <span className="badge b-succ">{conversion}% de las vistas terminan en contacto</span>
          </div>
          <div className="legend" style={{ margin: '12px 0' }}>
            <span>
              <i style={{ background: '#3B82F6' }} />
              Vistas
            </span>
            <span>
              <i style={{ background: 'var(--teal)' }} />
              Contactos
            </span>
          </div>
          <PairBars labels={stats.interacciones.etiquetas} a={stats.interacciones.vistas} b={stats.interacciones.contactos} />
          <div className="grid3" style={{ marginTop: 12 }}>
            {(
              [
                ['Vistas', entero(vistas)],
                ['Contactos', entero(contactos)],
                ['Conversión', `${conversion}%`],
              ] as const
            ).map(([t, n]) => (
              <div key={t} style={{ background: 'var(--bg)', borderRadius: 12, padding: '10px 12px' }}>
                <span className="mid" style={{ fontSize: 12 }}>
                  {t}
                </span>
                <b className="num" style={{ display: 'block', color: 'var(--ink)', fontFamily: 'var(--f-display)', fontSize: 18 }}>
                  {n}
                </b>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="card" style={{ padding: 22, marginTop: 18 }}>
        <h3 style={{ fontSize: 17 }} className="row">
          <Icon name="alert" size={18} /> Requiere atención
        </h3>
        {alertas.length === 0 ? (
          <p className="muted" style={{ marginTop: 10 }}>
            Todo al día: no hay pendientes.
          </p>
        ) : (
          <div className="grid3" style={{ marginTop: 14 }}>
            {alertas.map(([, t, s, c]) => (
              <button key={t} className="card row" style={{ padding: '14px 16px', textAlign: 'left', width: '100%' }} onClick={() => ir(s)}>
                <span style={{ width: 10, height: 10, borderRadius: '50%', flex: 'none', background: c }} />
                <span className="grow" style={{ color: 'var(--ink)', fontWeight: 500 }}>
                  {t}
                </span>
                <span style={{ color: 'var(--admin-blue)', fontSize: 12.5, fontWeight: 600 }}>Ver todos →</span>
              </button>
            ))}
          </div>
        )}
      </div>
    </>
  )
}

// ── Usuarios y lotes ───────────────────────────────────────────────────────

const estadoCuenta = (c: CuentaAdmin) => (c.estado_cuenta === 'suspendida' ? 'Suspendido' : 'Activo')
const tipoCuenta = (c: CuentaAdmin) => (c.tipo_cuenta === 'lote' ? 'Lote' : 'Particular')

/** Suspende (con motivo) o reactiva una cuenta. */
function useSuspender(listo: (m: string) => void) {
  const { openModal } = useUi()
  return {
    suspender: (c: CuentaAdmin) =>
      openModal(
        <ReasonModal
          title={c.tipo_cuenta === 'lote' ? '¿Por qué suspendes este lote?' : '¿Por qué suspendes esta cuenta?'}
          desc="El motivo queda registrado en la bitácora de administración."
          hint="Ej. Publicó anuncios fraudulentos varias veces."
          confirm="Suspender"
          working="Suspendiendo…"
          onConfirm={async (m) => {
            await setAccountSuspended(c.id_cuenta, true, m)
            listo('Cuenta suspendida.')
          }}
        />,
      ),
    reactivar: async (c: CuentaAdmin) => {
      await setAccountSuspended(c.id_cuenta, false, 'Reactivada desde el panel de administración.')
      listo('Cuenta reactivada.')
    },
  }
}

function UserDetail({ c, selfId, listo }: { c: CuentaAdmin; selfId: string; listo: (m: string) => void }) {
  const { closeModal, toast } = useUi()
  const navigate = useNavigate()
  const { suspender, reactivar } = useSuspender(listo)
  const [busy, setBusy] = useState(false)
  const suspendida = c.estado_cuenta === 'suspendida'
  // No se puede suspender a otro administrador ni a uno mismo.
  const puede = !c.es_admin && c.id_cuenta !== selfId
  return (
    <div className="modal">
      <ModalHead title="Detalle del usuario" />
      <div className="row">
        <Ini name={c.nombre_mostrar} />
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
            ['Tipo de cuenta', tipoCuenta(c) + (c.es_admin ? ' · Administrador' : '')],
            ['Estado', estadoCuenta(c)],
            ['Fecha de registro', fecha(c.fecha_creacion)],
            ['Publicaciones', `${c.publicaciones} (${c.activas} activas)`],
            ['Ubicación', [c.municipio, c.estado].filter(Boolean).join(', ') || '—'],
            ...(c.tipo_cuenta === 'lote' ? [['Responsable', c.responsable || '—']] : []),
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
          Ver perfil
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
              Reactivar cuenta
            </button>
          ) : (
            <button className="btn btn-danger" onClick={() => suspender(c)}>
              {c.tipo_cuenta === 'lote' ? 'Suspender lote' : 'Suspender cuenta'}
            </button>
          ))}
      </div>
    </div>
  )
}

function Usuarios({ datos, selfId, listo }: { datos: Datos; selfId: string; listo: (m: string) => void }) {
  const { openModal } = useUi()
  const [q, setQ] = useState('')
  const [tab, setTab] = useState<'todos' | 'particular' | 'lote' | 'suspendido'>('todos')
  const c = datos.cuentas
  const lista = c
    .filter((x) => (tab === 'todos' ? true : tab === 'suspendido' ? x.estado_cuenta === 'suspendida' : x.tipo_cuenta === tab))
    .filter((x) => !q.trim() || `${x.nombre_mostrar} ${x.correo}`.toLowerCase().includes(q.trim().toLowerCase()))
  return (
    <>
      <AdminHead title="Usuarios" desc="Consulta y administra las cuentas registradas en Karsy." right={<SearchBox value={q} onChange={setQ} placeholder="Buscar por nombre o correo..." />} />
      <Tabs
        value={tab}
        onChange={setTab}
        options={[
          ['todos', `Todos (${c.length})`],
          ['particular', `Particular (${c.filter((x) => x.tipo_cuenta === 'particular').length})`],
          ['lote', `Lote (${c.filter((x) => x.tipo_cuenta === 'lote').length})`],
          ['suspendido', `Suspendido (${c.filter((x) => x.estado_cuenta === 'suspendida').length})`],
        ]}
      />
      <div className="card tablewrap">
        <table className="t">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Correo</th>
              <th>Tipo de cuenta</th>
              <th>Estado</th>
              <th>Fecha de registro</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {lista.map((x) => (
              <tr key={x.id_cuenta}>
                <td>
                  <div className="mini">
                    <Ini name={x.nombre_mostrar} />
                    <b style={{ color: 'var(--ink)' }}>{x.nombre_mostrar}</b>
                  </div>
                </td>
                <td>{x.correo}</td>
                <td>
                  {tipoCuenta(x)}
                  {x.es_admin ? ' · Administrador' : ''}
                </td>
                <td>
                  <Badge s={estadoCuenta(x)} />
                </td>
                <td className="num">{fecha(x.fecha_creacion)}</td>
                <td>
                  <button className="btn btn-ghost btn-sm" onClick={() => openModal(<UserDetail c={x} selfId={selfId} listo={listo} />)}>
                    Ver detalle
                  </button>
                </td>
              </tr>
            ))}
            {lista.length === 0 && (
              <tr>
                <td colSpan={6} className="muted" style={{ textAlign: 'center', padding: 30 }}>
                  No hay cuentas que coincidan.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  )
}

function Lotes({ datos, listo }: { datos: Datos; listo: (m: string) => void }) {
  const navigate = useNavigate()
  const { toast } = useUi()
  const { suspender, reactivar } = useSuspender(listo)
  const [q, setQ] = useState('')
  const [tab, setTab] = useState<'todos' | 'activo' | 'suspendido'>('todos')
  const lotes = datos.cuentas.filter((c) => c.tipo_cuenta === 'lote')
  const lista = lotes
    .filter((x) => (tab === 'todos' ? true : tab === 'suspendido' ? x.estado_cuenta === 'suspendida' : x.estado_cuenta !== 'suspendida'))
    .filter((x) => !q.trim() || `${x.nombre_mostrar} ${x.responsable} ${x.municipio} ${x.estado}`.toLowerCase().includes(q.trim().toLowerCase()))
  return (
    <>
      <AdminHead title="Lotes" desc="Administra los lotes y agencias que publican vehículos en Karsy." right={<SearchBox value={q} onChange={setQ} placeholder="Buscar lote, responsable o ciudad..." />} />
      <Tabs
        value={tab}
        onChange={setTab}
        options={[
          ['todos', `Todos (${lotes.length})`],
          ['activo', `Activo (${lotes.filter((x) => x.estado_cuenta !== 'suspendida').length})`],
          ['suspendido', `Suspendido (${lotes.filter((x) => x.estado_cuenta === 'suspendida').length})`],
        ]}
      />
      <div className="card tablewrap">
        <table className="t">
          <thead>
            <tr>
              <th>Lote</th>
              <th>Responsable</th>
              <th>Ubicación</th>
              <th>Vehículos activos</th>
              <th>Estado</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {lista.map((x) => (
              <tr key={x.id_cuenta}>
                <td>
                  <div className="mini">
                    <Ini name={x.nombre_mostrar} square />
                    <b style={{ color: 'var(--ink)' }}>{x.nombre_mostrar}</b>
                  </div>
                </td>
                <td>{x.responsable || '—'}</td>
                <td>{[x.municipio, x.estado].filter(Boolean).join(', ') || '—'}</td>
                <td className="num">{x.activas}</td>
                <td>
                  <Badge s={estadoCuenta(x)} />
                </td>
                <td>
                  <div className="row" style={{ gap: 6 }}>
                    <button className="btn btn-ghost btn-sm" onClick={() => navigate(`/lotes/${x.id_cuenta}`)}>
                      Ver lote
                    </button>
                    {!x.es_admin &&
                      (x.estado_cuenta === 'suspendida' ? (
                        <button className="btn btn-soft btn-sm" onClick={() => reactivar(x).catch((e) => toast(mensajeUsuario(e)))}>
                          Reactivar
                        </button>
                      ) : (
                        <button className="btn btn-ghost btn-sm" style={{ color: 'var(--error)' }} onClick={() => suspender(x)}>
                          Suspender
                        </button>
                      ))}
                  </div>
                </td>
              </tr>
            ))}
            {lista.length === 0 && (
              <tr>
                <td colSpan={6} className="muted" style={{ textAlign: 'center', padding: 30 }}>
                  No hay lotes que coincidan.
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
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const propuesta = propuestaPendiente(row)
  const habilitada = row.estado_administrativo === 'habilitada'
  const estado = estadoAdmin(row, car)

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
      <ModalHead title="Detalle del vehículo" sub={`Publicación #${car.id} · ${car.ownerName}`} />
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
            Motivo: {row.motivo_rechazo_propuesta}
          </span>
        )}
        {!habilitada && row.motivo_deshabilitacion && (
          <span className="muted" style={{ fontSize: 12.5 }}>
            Motivo: {row.motivo_deshabilitacion}
          </span>
        )}
      </div>
      <div className="ficha">
        {(
          [
            ['Transmisión', row.transmision ?? '—'],
            ['Kilometraje', km(row.kilometraje)],
            ['Carrocería', row.carroceria ?? '—'],
            ['Color', row.color ?? '—'],
            ['Combustible', row.tipo_combustible ?? '—'],
            ['Ubicación', car.city || '—'],
          ] as [string, string][]
        ).map(([k, v]) => (
          <div key={k}>
            <span>{k}</span>
            <b>{v}</b>
          </div>
        ))}
      </div>
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
              onClick={() =>
                openModal(
                  <ReasonModal
                    title="¿Por qué rechazas esta publicación?"
                    desc="El vendedor verá este comentario. Sé claro sobre qué debe corregir para volver a enviarla."
                    hint="Ej. Faltan fotos del interior y del tablero."
                    confirm="Rechazar"
                    working="Rechazando…"
                    onConfirm={async (m) => {
                      await moderateProposal(propuesta, false, m)
                      listo('Publicación rechazada.')
                    }}
                  />,
                )
              }
            >
              Rechazar
            </button>
            <button
              className="btn btn-ghost"
              onClick={() => {
                closeModal()
                navigate(`/vehiculo/${car.id}`)
              }}
            >
              Ver publicación
            </button>
            <button className="btn btn-primary" disabled={busy} onClick={() => correr(() => moderateProposal(propuesta, true, null), 'Publicación aprobada.')}>
              Aprobar
            </button>
          </>
        ) : (
          <>
            <button
              className="btn btn-ghost"
              onClick={() => {
                closeModal()
                navigate(`/vehiculo/${car.id}`)
              }}
            >
              Ver publicación
            </button>
            <span />
            {row.aprobada &&
              (habilitada ? (
                <button
                  className="btn btn-danger"
                  onClick={() =>
                    openModal(
                      <ReasonModal
                        title="¿Por qué deshabilitas esta publicación?"
                        desc="El dueño verá este comentario en su publicación. Sé claro sobre qué debe corregir."
                        hint="Ej. Las fotos no corresponden al vehículo anunciado."
                        confirm="Deshabilitar"
                        working="Deshabilitando…"
                        onConfirm={async (m) => {
                          await setCarEnabled(car.id, false, m)
                          listo('Publicación deshabilitada.')
                        }}
                      />,
                    )
                  }
                >
                  Deshabilitar
                </button>
              ) : (
                <button className="btn btn-primary" disabled={busy} onClick={() => correr(() => setCarEnabled(car.id, true, 'Habilitada desde el panel.'), 'Publicación habilitada.')}>
                  Habilitar
                </button>
              ))}
          </>
        )}
      </div>
    </div>
  )
}

const FILTROS_VEHICULO = ['Todos', 'Pendiente', 'Cambios pendientes', 'Activo', 'Rechazado', 'Deshabilitado', 'Pausado', 'Vendido'] as const

function Vehiculos({ datos, listo }: { datos: Datos; listo: (m: string) => void }) {
  const { openModal } = useUi()
  const [q, setQ] = useState('')
  const [tab, setTab] = useState<(typeof FILTROS_VEHICULO)[number]>('Todos')
  const conEstado = useMemo(() => datos.autos.map((a) => ({ ...a, estado: estadoAdmin(a.row, a.car) })), [datos.autos])
  const lista = conEstado
    .filter((a) => tab === 'Todos' || a.estado === tab)
    .filter((a) => !q.trim() || `${a.car.brand} ${a.car.model} ${a.car.year} ${a.car.ownerName}`.toLowerCase().includes(q.trim().toLowerCase()))
  const cuenta = (s: string) => conEstado.filter((a) => a.estado === s).length
  return (
    <>
      <AdminHead title="Vehículos" desc="Revisa las publicaciones de vehículos y su estado dentro de la plataforma." right={<SearchBox value={q} onChange={setQ} placeholder="Buscar marca, modelo o vendedor..." />} />
      <Tabs value={tab} onChange={setTab} options={FILTROS_VEHICULO.map((f) => [f, f === 'Todos' ? `Todos (${conEstado.length})` : `${f} (${cuenta(f)})`])} />
      <div className="card tablewrap">
        <table className="t">
          <thead>
            <tr>
              <th>Vehículo</th>
              <th>Vendedor</th>
              <th>Año</th>
              <th>Precio</th>
              <th>Estado</th>
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
                          Publicación #{car.id}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td>{car.ownerName}</td>
                  <td>{car.year || '—'}</td>
                  <td className="num">{precio(car.priceValue)}</td>
                  <td>
                    <Badge s={estado} />
                  </td>
                  <td>
                    <button className={`btn btn-sm ${revisar ? 'btn-primary' : 'btn-ghost'}`} onClick={() => openModal(<VehicleReview car={car} row={row} listo={listo} />)}>
                      Revisar
                    </button>
                  </td>
                </tr>
              )
            })}
            {lista.length === 0 && (
              <tr>
                <td colSpan={6} className="muted" style={{ textAlign: 'center', padding: 30 }}>
                  No hay publicaciones en esta categoría.
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

function ReportReview({ r, datos, listo }: { r: ReporteAdmin; datos: Datos; listo: (m: string) => void }) {
  const { openModal } = useUi()
  const auto = datos.autos.find((a) => a.car.id === r.id_publicacion)
  const deCuenta = esReporteDeCuenta(r)
  const pendiente = r.estado_reporte === 'pendiente'
  const dueno = auto ? datos.cuentas.find((c) => c.id_cuenta === auto.car.ownerId) : undefined

  const descartar = () =>
    openModal(
      <ReasonModal
        title="¿Por qué descartas este reporte?"
        desc={deCuenta ? 'La cuenta sigue activa. El motivo queda como resolución del reporte.' : 'La publicación sigue habilitada. El motivo queda como resolución del reporte.'}
        hint="Ej. Revisé las fotos y sí corresponden al vehículo."
        confirm="Descartar reporte"
        working="Descartando…"
        onConfirm={async (m) => {
          await resolveReport(r.id_reporte, 'descartado', m, false)
          listo('Reporte descartado.')
        }}
      />,
    )
  const proceder = () =>
    openModal(
      deCuenta ? (
        <ReasonModal
          title="¿Por qué suspendes esta cuenta?"
          desc="El motivo queda registrado en la bitácora de administración."
          hint="Ej. Publicó anuncios fraudulentos varias veces."
          confirm="Suspender"
          working="Suspendiendo…"
          onConfirm={async (m) => {
            // Si la cuenta ya estaba suspendida, solo se atiende el reporte.
            if (!dueno) throw new Error('No se encontró la cuenta del vendedor.')
            if (dueno.estado_cuenta !== 'suspendida') await setAccountSuspended(dueno.id_cuenta, true, m)
            await resolveReport(r.id_reporte, 'atendido', m, false)
            listo('Reporte atendido y cuenta suspendida.')
          }}
        />
      ) : (
        <ReasonModal
          title="¿Por qué deshabilitas esta publicación?"
          desc="El dueño verá este comentario en su publicación. Sé claro sobre qué debe corregir."
          hint="Ej. Las fotos no corresponden al vehículo anunciado."
          confirm="Deshabilitar"
          working="Deshabilitando…"
          onConfirm={async (m) => {
            await resolveReport(r.id_reporte, 'atendido', m, true)
            listo('Reporte atendido y publicación deshabilitada.')
          }}
        />
      ),
    )

  return (
    <div className="modal wide">
      <ModalHead title="Revisión del reporte" sub={`Reporte #R-${r.id_reporte} · ${haceCuanto(r.fecha_reporte)}`} />
      <div className="ficha" style={{ gridTemplateColumns: '1fr 1fr' }}>
        {(
          [
            ['Tipo', deCuenta ? 'Usuario' : 'Publicación'],
            ['Reportado por', r.reportante],
            ['Estado', capital(r.estado_reporte)],
            [deCuenta ? 'Cuenta reportada' : 'Publicación', deCuenta ? auto?.car.ownerName ?? '—' : r.publicacion],
          ] as [string, string][]
        ).map(([k, v]) => (
          <div key={k}>
            <span>{k}</span>
            <b style={{ whiteSpace: 'normal' }}>{v}</b>
          </div>
        ))}
      </div>
      <div className="note muted" style={{ background: 'var(--bg)' }}>
        {r.motivo_reporte}
      </div>
      {r.motivo_resolucion && (
        <div className="note" style={{ background: 'var(--success-l)' }}>
          <b>Resolución:</b> {r.motivo_resolucion}
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
      {pendiente && (
        <div className="grid2">
          <button className="btn btn-ghost" onClick={descartar}>
            Descartar
          </button>
          <button className="btn btn-danger" onClick={proceder}>
            {deCuenta ? 'Suspender cuenta' : 'Deshabilitar publicación'}
          </button>
        </div>
      )}
    </div>
  )
}

function Reportes({ datos, listo }: { datos: Datos; listo: (m: string) => void }) {
  const { openModal } = useUi()
  const [tab, setTab] = useState<'pendiente' | 'resueltos' | 'todos'>('pendiente')
  const pendientes = datos.reportes.filter((r) => r.estado_reporte === 'pendiente')
  const resueltos = datos.reportes.filter((r) => r.estado_reporte !== 'pendiente')
  const lista = tab === 'pendiente' ? pendientes : tab === 'resueltos' ? resueltos : datos.reportes
  return (
    <>
      <AdminHead
        title="Reportes"
        desc="Revisa incidencias y reportes enviados por la comunidad de Karsy."
        right={pendientes.length > 0 && <span className="badge b-dang">{pendientes.length} {pendientes.length === 1 ? 'reporte pendiente' : 'reportes pendientes'}</span>}
      />
      <Tabs
        value={tab}
        onChange={setTab}
        options={[
          ['pendiente', `Pendiente (${pendientes.length})`],
          ['resueltos', `Resueltos (${resueltos.length})`],
          ['todos', 'Todos'],
        ]}
      />
      <div className="card tablewrap">
        <table className="t">
          <thead>
            <tr>
              <th>Reporte</th>
              <th>Tipo</th>
              <th>Reportado</th>
              <th>Motivo</th>
              <th>Reportado por</th>
              <th>Fecha</th>
              <th>Estado</th>
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
                  <td>
                    <span className={`badge ${deCuenta ? 'b-info' : 'b-neu'}`}>{deCuenta ? 'Usuario' : 'Publicación'}</span>
                  </td>
                  <td>{deCuenta ? dueno ?? r.publicacion : r.publicacion}</td>
                  <td style={{ whiteSpace: 'normal', minWidth: 220 }}>{r.motivo_reporte.replace(/^(Publicación|Cuenta del vendedor): /, '')}</td>
                  <td>{r.reportante}</td>
                  <td>{haceCuanto(r.fecha_reporte)}</td>
                  <td>
                    <Badge s={capital(r.estado_reporte)} />
                  </td>
                  <td>
                    <button className="btn btn-ghost btn-sm" onClick={() => openModal(<ReportReview r={r} datos={datos} listo={listo} />)}>
                      Revisar
                    </button>
                  </td>
                </tr>
              )
            })}
            {lista.length === 0 && (
              <tr>
                <td colSpan={8} className="muted" style={{ textAlign: 'center', padding: 30 }}>
                  No hay reportes en esta categoría.
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

const MOTIVOS_DESTACADO: [string, string][] = [
  ['Pago no verificado', 'El comprobante de pago no coincide o no ha sido acreditado.'],
  ['Calidad de imágenes', 'Las fotos del vehículo no cumplen con la calidad o visibilidad requerida.'],
  ['Información incompleta', 'La publicación carece de detalles obligatorios o precisos.'],
  ['Vehículo no permitido', 'El vehículo no cumple con las políticas de publicación de Karsy.'],
  ['Otro motivo', 'Especifica el motivo en los comentarios adicionales.'],
]

function RejectFeatured({ d, listo }: { d: DestacadoAdmin; listo: (m: string) => void }) {
  const { closeModal, toast } = useUi()
  const [motivo, setMotivo] = useState('')
  const [comentario, setComentario] = useState('')
  const [busy, setBusy] = useState(false)
  return (
    <div className="modal wide">
      <ModalHead title="Rechazar solicitud para destacar" sub="Selecciona o escribe el motivo por el cual no se aprobó la solicitud para informarle al usuario." />
      {MOTIVOS_DESTACADO.map(([t, desc]) => (
        <button key={t} className={`opt ${motivo === t ? 'on' : ''}`} onClick={() => setMotivo(t)}>
          <span className="radio" />
          <span>
            <b style={{ color: 'var(--ink)' }}>{t}</b>
            <br />
            <span className="muted" style={{ fontSize: 12.5 }}>
              {desc}
            </span>
          </span>
        </button>
      ))}
      <textarea className="input" placeholder="Añade detalles adicionales para el usuario (opcional)..." maxLength={300} value={comentario} onChange={(e) => setComentario(e.target.value)} />
      <div className="grid2">
        <button className="btn btn-ghost" disabled={busy} onClick={closeModal}>
          Cancelar
        </button>
        <button
          className="btn btn-danger"
          disabled={busy || !motivo}
          onClick={async () => {
            setBusy(true)
            try {
              await resolveFeatured(d.id_solicitud_destacado, false, [motivo, comentario.trim()].filter(Boolean).join('. '))
              closeModal()
              listo('Solicitud rechazada.')
            } catch (e) {
              toast(mensajeUsuario(e))
              setBusy(false)
            }
          }}
        >
          Rechazar y Notificar
        </button>
      </div>
    </div>
  )
}

function Aprobada({ titulo }: { titulo: string }) {
  const { closeModal } = useUi()
  return (
    <div className="modal" style={{ textAlign: 'center', alignItems: 'center' }}>
      <span className="nic" style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--warning-l)', color: 'var(--star)' }}>
        <Icon name="star" size={30} />
      </span>
      <ModalHead title="¡Solicitud Aprobada!" sub={`La publicación de ${titulo} ahora aparece en la sección de Destacados de Karsy durante 1 mes. Se ha notificado al usuario solicitante.`} />
      <button className="btn btn-primary btn-block" onClick={closeModal}>
        Volver a solicitudes
      </button>
    </div>
  )
}

function Destacados({ datos, listo }: { datos: Datos; listo: (m: string) => void }) {
  const { openModal, toast } = useUi()
  const navigate = useNavigate()
  const [tab, setTab] = useState<'pendiente' | 'aprobada' | 'rechazada'>('pendiente')
  const [busy, setBusy] = useState<number | null>(null)
  const cuenta = (e: string) => datos.destacados.filter((d) => d.estado_solicitud === e).length
  const lista = datos.destacados.filter((d) => d.estado_solicitud === tab)

  const aprobar = async (d: DestacadoAdmin, titulo: string) => {
    setBusy(d.id_solicitud_destacado)
    try {
      await resolveFeatured(d.id_solicitud_destacado, true, null)
      listo('Publicación destacada por 1 mes.')
      openModal(<Aprobada titulo={titulo} />)
    } catch (e) {
      toast(mensajeUsuario(e))
    }
    setBusy(null)
  }

  return (
    <>
      <AdminHead title="Solicitudes para Destacar" desc="Revisa las solicitudes enviadas por usuarios para mostrar sus vehículos en la sección de Destacados." />
      <Tabs
        value={tab}
        onChange={setTab}
        options={[
          ['pendiente', `Pendientes (${cuenta('pendiente')})`],
          ['aprobada', `Aprobadas (${cuenta('aprobada')})`],
          ['rechazada', `Rechazadas (${cuenta('rechazada')})`],
        ]}
      />
      {lista.length === 0 ? (
        <div className="card empty">
          <p className="muted">No hay solicitudes en esta categoría.</p>
        </div>
      ) : (
        <div className="vgrid">
          {lista.map((d) => {
            const car = datos.autos.find((a) => a.car.id === d.id_publicacion)?.car
            const titulo = car ? `${car.brand} ${car.model} (${car.year})` : `Publicación #${d.id_publicacion}`
            return (
              <div key={d.id_solicitud_destacado} className="card" style={{ overflow: 'hidden' }}>
                <CarPhoto src={car?.imageUrl} style={{ height: 170, cursor: 'pointer' }} onClick={() => navigate(`/vehiculo/${d.id_publicacion}`)}>
                  <span className="tag badge" style={{ background: '#FFF4CC', color: '#8A6415' }}>
                    ★ {d.estado_solicitud === 'pendiente' ? 'Solicitado' : capital(d.estado_solicitud)}
                  </span>
                </CarPhoto>
                <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <b style={{ fontFamily: 'var(--f-display)', color: 'var(--ink)', fontSize: 16 }}>{titulo}</b>
                  {car && <span className="vprice num" style={{ textAlign: 'left' }}>{precio(car.priceValue)}</span>}
                  <div className="row" style={{ gap: 8 }}>
                    <Ini name={d.solicitante} src={publicUrl(d.solicitante_foto)} />
                    <p className="muted" style={{ fontSize: 12.5 }}>
                      <b>{d.solicitante}</b> lo solicitó {haceCuanto(d.fecha_solicitud).toLowerCase()}.
                    </p>
                  </div>
                  {d.motivo_rechazo && (
                    <p className="muted" style={{ fontSize: 12.5 }}>
                      Motivo: {d.motivo_rechazo}
                    </p>
                  )}
                  {tab === 'pendiente' && (
                    <div className="grid2" style={{ gap: 8, marginTop: 4 }}>
                      <button className="btn btn-ghost btn-sm" disabled={busy === d.id_solicitud_destacado} onClick={() => openModal(<RejectFeatured d={d} listo={listo} />)}>
                        Rechazar
                      </button>
                      <button className="btn btn-primary btn-sm" disabled={busy === d.id_solicitud_destacado} onClick={() => void aprobar(d, titulo)}>
                        Aprobar
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
