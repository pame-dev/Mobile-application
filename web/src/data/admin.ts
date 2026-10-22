// Panel de administración (AdminRepository y StatsRepository.admin de la app). Las escrituras
// pasan por las funciones admin_* de la BD, que validan el rol y registran la bitácora.
import { traducir } from '../i18n/I18nContext'
import { BUCKET, supabase } from '../lib/supabase'
import { UserError } from './api'
import type { AnuncioRow } from './types'

function check<T>(res: { data: T; error: { message: string; code?: string } | null }): T {
  if (res.error) {
    if (res.error.code === 'P0001') throw new UserError(res.error.message)
    if (res.error.code === '42501') throw new UserError(traducir('admin.noPermission'))
    throw new Error(res.error.message)
  }
  return res.data
}

export interface AdminStats {
  usuarios: number
  usuariosNuevos: number
  lotes: number
  lotesNuevos: number
  publicadas: number
  publicadasNuevas: number
  activas: number
  deshabilitadas: number
  /** Para "Requiere atención": las que este admin aún no ha visto en Vehículos. */
  deshabilitadasSinVer: number
  pausadasSinVer: number
  suspendidasSinVer: number
  suspendidas: number
  propuestasPendientes: number
  reportesPendientes: number
  destacadosPendientes: number
  crecimiento: { etiquetas: string[]; usuarios: number[]; lotes: number[]; publicaciones: number[] }
  interacciones: { etiquetas: string[]; vistas: number[]; contactos: number[] }
}

export async function adminStats(dias: number): Promise<AdminStats> {
  const j = (check(await supabase.rpc('admin_estadisticas', { p_dias: dias })) ?? {}) as Record<string, unknown>
  const n = (k: string) => Number(j[k] ?? 0)
  const obj = (k: string) => (j[k] ?? {}) as Record<string, unknown[]>
  const nums = (o: Record<string, unknown[]>, k: string) => (o[k] ?? []).map(Number)
  const strs = (o: Record<string, unknown[]>, k: string) => (o[k] ?? []).map(String)
  const c = obj('crecimiento')
  const i = obj('interacciones')
  return {
    usuarios: n('usuarios'),
    usuariosNuevos: n('usuarios_nuevos'),
    lotes: n('lotes'),
    lotesNuevos: n('lotes_nuevos'),
    publicadas: n('publicadas'),
    publicadasNuevas: n('publicadas_nuevas'),
    activas: n('activas'),
    deshabilitadas: n('deshabilitadas'),
    deshabilitadasSinVer: n('deshabilitadas_sin_ver'),
    pausadasSinVer: n('pausadas_sin_ver'),
    suspendidasSinVer: n('suspendidas_sin_ver'),
    suspendidas: n('suspendidas'),
    propuestasPendientes: n('propuestas_pendientes'),
    reportesPendientes: n('reportes_pendientes'),
    destacadosPendientes: n('destacados_pendientes'),
    crecimiento: { etiquetas: strs(c, 'etiquetas'), usuarios: nums(c, 'usuarios'), lotes: nums(c, 'lotes'), publicaciones: nums(c, 'publicaciones') },
    interacciones: { etiquetas: strs(i, 'etiquetas'), vistas: nums(i, 'vistas'), contactos: nums(i, 'contactos') },
  }
}

/** Datos del reporte del panel de administración (admin_reporte); solo se piden al exportar. */
export interface ReporteApp {
  usuarios: { total: number; nuevos: number; particulares: number; lotes: number; suspendidos: number }
  publicaciones: { publicadas: number; nuevas: number; activas: number; pausadas: number; deshabilitadas: number; vendidas: number; vendidasDentro: number; vendidasFuera: number }
  interes: { vistas: number; contactos: number }
  favoritos: number
  destacados: { pendientes: number; vigentes: number; aprobados: number; rechazados: number }
  reportes: { pendientes: number; recibidos: number; resueltos: number }
  revision: { pendientes: number; aprobadas: number; rechazadas: number; horasPromedio: number | null }
  marcas: { nombre: string; publicaciones: number }[]
  estados: { nombre: string; publicaciones: number }[]
  masVistas: { id: number; titulo: string; vendedor: string; vistas: number; contactos: number }[]
  /** Vistas por fecha del rango: por día hasta 31 días, por semana si es más (etiqueta = primer día). */
  porFecha: { etiqueta: string; vistas: number }[]
  porDia: number[]
  porHora: number[]
  motivos: { motivo: string; reportes: number }[]
}

export async function adminReport(dias: number): Promise<ReporteApp> {
  const j = (check(await supabase.rpc('admin_reporte', { p_dias: dias })) ?? {}) as Record<string, unknown>
  const o = (k: string) => (j[k] ?? {}) as Record<string, unknown>
  const n = (v: unknown) => Number(v ?? 0)
  const lista = (k: string) => ((j[k] as Record<string, unknown>[]) ?? [])
  const u = o('usuarios'), p = o('publicaciones'), i = o('interes'), d = o('destacados'), r = o('reportes'), rv = o('revision')
  return {
    usuarios: { total: n(u.total), nuevos: n(u.nuevos), particulares: n(u.particulares), lotes: n(u.lotes), suspendidos: n(u.suspendidos) },
    publicaciones: {
      publicadas: n(p.publicadas), nuevas: n(p.nuevas), activas: n(p.activas), pausadas: n(p.pausadas), deshabilitadas: n(p.deshabilitadas),
      vendidas: n(p.vendidas), vendidasDentro: n(p.vendidas_dentro), vendidasFuera: n(p.vendidas_fuera),
    },
    interes: { vistas: n(i.vistas), contactos: n(i.contactos) },
    favoritos: n(j.favoritos),
    destacados: { pendientes: n(d.pendientes), vigentes: n(d.vigentes), aprobados: n(d.aprobados), rechazados: n(d.rechazados) },
    reportes: { pendientes: n(r.pendientes), recibidos: n(r.recibidos), resueltos: n(r.resueltos) },
    revision: { pendientes: n(rv.pendientes), aprobadas: n(rv.aprobadas), rechazadas: n(rv.rechazadas), horasPromedio: rv.horas_promedio == null ? null : Number(rv.horas_promedio) },
    marcas: lista('marcas').map((m) => ({ nombre: String(m.nombre ?? '—'), publicaciones: n(m.publicaciones) })),
    estados: lista('estados').map((m) => ({ nombre: String(m.nombre ?? '—'), publicaciones: n(m.publicaciones) })),
    masVistas: lista('mas_vistas').map((m) => ({ id: n(m.id), titulo: String(m.titulo ?? ''), vendedor: String(m.vendedor ?? ''), vistas: n(m.vistas), contactos: n(m.contactos) })),
    porFecha: lista('por_fecha').map((m) => ({ etiqueta: String(m.etiqueta ?? ''), vistas: n(m.vistas) })),
    porDia: ((j.por_dia as unknown[]) ?? []).map(Number),
    porHora: ((j.por_hora as unknown[]) ?? []).map(Number),
    motivos: lista('motivos').map((m) => ({ motivo: String(m.motivo ?? ''), reportes: n(m.reportes) })),
  }
}

export interface CuentaAdmin {
  id_cuenta: string
  nombre_mostrar: string
  correo: string
  tipo_cuenta: string
  estado_cuenta: string
  fecha_creacion: string
  es_admin: boolean
  municipio: string
  estado: string
  responsable: string
  publicaciones: number
  activas: number
  /** Ruta de la foto de perfil en el bucket público (null = iniciales). */
  foto_perfil: string | null
  /** Cuenta verificada (cuentas.verificado): escudo azul junto al nombre en Usuarios. */
  verificado: boolean
}

export interface ReporteAdmin {
  id_reporte: number
  id_publicacion: number
  publicacion: string
  reportante: string
  motivo_reporte: string
  estado_reporte: string
  motivo_resolucion: string | null
  fecha_reporte: string
}

export interface DestacadoAdmin {
  id_solicitud_destacado: number
  id_publicacion: number
  estado_solicitud: string
  fecha_solicitud: string
  motivo_rechazo: string | null
  solicitante: string
  solicitante_foto: string | null
}

export const adminAccounts = async () => (check(await supabase.rpc('admin_listar_cuentas')) ?? []) as CuentaAdmin[]
export const adminReports = async () => (check(await supabase.rpc('admin_listar_reportes')) ?? []) as ReporteAdmin[]
export const adminFeatured = async () => (check(await supabase.rpc('admin_listar_destacados')) ?? []) as DestacadoAdmin[]

/** Publicaciones que todavía no tienen huellas de fotos (las que ya existían), con sus fotos. */
export async function publicacionesSinHuellas(): Promise<{ id: number; fotos: string[] }[]> {
  const rows = (check(await supabase.rpc('admin_publicaciones_sin_huellas')) ?? []) as { id_publicacion: number; fotos: string[] | null }[]
  return rows.map((r) => ({ id: r.id_publicacion, fotos: r.fotos ?? [] }))
}

/** Guarda las huellas visuales de las fotos de una publicación ya existente. */
export async function registrarHuellasAdmin(idPublicacion: number, huellas: string[]) {
  check(await supabase.rpc('admin_registrar_huellas', { p_id_publicacion: idPublicacion, p_huellas: huellas }))
}

/** El admin abrió el filtro "Deshabilitado" o "Pausado": dejan de salir en "Requiere atención". */
export async function marcarVistas(estado: 'deshabilitada' | 'pausada') {
  check(await supabase.rpc('admin_marcar_vistas', { p_estado: estado }))
}

/** El admin abrió el filtro "Suspendido" en Usuarios: dejan de salir en "Requiere atención". */
export async function marcarSuspendidasVistas() {
  check(await supabase.rpc('admin_marcar_suspendidas_vistas'))
}

/** Todos los anuncios (el admin ve también pendientes y deshabilitados). */
export const adminCars = async () =>
  check(await supabase.from('v_anuncios').select('*').order('fecha_creacion', { ascending: false })) as AnuncioRow[]

export async function moderateProposal(idPropuesta: number, aprobar: boolean, motivo: string | null) {
  check(await supabase.rpc('admin_moderar_propuesta', { p_id_propuesta: idPropuesta, p_aprobar: aprobar, p_motivo: motivo }))
  if (aprobar) await borrarFotosRetiradas()
}

/**
 * Borra de Storage las fotos que ya nadie usa (p. ej. la original con placa que una
 * edición aprobada reemplazó por la tapada). Si falla, la aprobación ya quedó: se
 * reintenta en la siguiente.
 */
async function borrarFotosRetiradas() {
  try {
    const { data } = await supabase.rpc('admin_fotos_retiradas_sin_uso')
    const rutas = (data ?? []) as string[]
    if (rutas.length) await supabase.storage.from(BUCKET).remove(rutas)
  } catch {
    // no bloquea la aprobación
  }
}

export async function setCarEnabled(idPublicacion: number, habilitar: boolean, motivo: string | null) {
  check(await supabase.rpc('admin_cambiar_estado_publicacion', { p_id_publicacion: idPublicacion, p_habilitar: habilitar, p_motivo: motivo }))
}

export async function setAccountSuspended(idCuenta: string, suspender: boolean, motivo: string | null) {
  check(await supabase.rpc('admin_cambiar_estado_cuenta', { p_id_cuenta: idCuenta, p_suspender: suspender, p_motivo: motivo }))
}

/** estado: "atendido" | "descartado". */
export async function resolveReport(idReporte: number, estado: 'atendido' | 'descartado', motivo: string, deshabilitar: boolean) {
  check(await supabase.rpc('admin_resolver_reporte', { p_id_reporte: idReporte, p_estado: estado, p_motivo: motivo, p_deshabilitar: deshabilitar }))
}

export async function resolveFeatured(idSolicitud: number, aprobar: boolean, motivo: string | null) {
  check(await supabase.rpc('admin_resolver_destacado', { p_id_solicitud: idSolicitud, p_aprobar: aprobar, p_motivo: motivo }))
}

// ── Verificación de usuarios (documentos de lotes y particulares) ──

export interface VerificacionAdmin {
  id: string
  user_id: string
  nombre_mostrar: string
  correo: string
  tipo_usuario: 'lote' | 'particular'
  tipo_documento: string
  /** Ruta del archivo en el bucket; null en la CURP (no tiene archivo). */
  archivo_path: string | null
  /** CURP del particular (tipo_documento 'curp'). */
  curp: string | null
  estado: 'pendiente' | 'verificado' | 'rechazado'
  motivo_rechazo: string | null
  /** Nombre del admin que lo revisó. */
  revisado_por: string | null
  fecha_envio: string
  fecha_revision: string | null
}

/** Documentos de verificación de todos los usuarios (pendientes primero). */
export async function adminVerifications(): Promise<VerificacionAdmin[]> {
  const { data, error } = await supabase.rpc('admin_listar_verificaciones_usuario')
  if (error) throw error
  // De paso se borran los archivos que ya nadie usa (sin esperar ni bloquear la lista).
  void borrarDocumentosSinUso()
  return (data ?? []) as VerificacionAdmin[]
}

/** Aprueba (estado verificado + insignia en la cuenta) o rechaza con motivo un documento pendiente. */
export async function resolveVerification(id: string, aprobar: boolean, motivo: string | null) {
  const { error } = await supabase.rpc('admin_resolver_verificacion_usuario', { p_id: id, p_aprobar: aprobar, p_motivo: motivo })
  if (error) throw error
  await borrarDocumentosSinUso()
}

/**
 * Borra del bucket los documentos que ningún registro usa (reenvíos viejos, repetidos que se
 * limpiaron). Si falla, la revisión ya quedó: se reintenta en la siguiente.
 */
async function borrarDocumentosSinUso() {
  try {
    const { data } = await supabase.rpc('admin_documentos_verificacion_sin_uso')
    const rutas = (data ?? []) as string[]
    if (rutas.length) await supabase.storage.from('constancias-fiscales').remove(rutas)
  } catch {
    // no bloquea la revisión
  }
}

/** URL firmada de 10 minutos para ver un documento de verificación (el bucket es privado). */
export async function constanciaUrl(ruta: string): Promise<string> {
  const { data, error } = await supabase.storage.from('constancias-fiscales').createSignedUrl(ruta, 600)
  if (error) throw error
  return data.signedUrl
}
