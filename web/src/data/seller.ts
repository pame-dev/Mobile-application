// Lo que hace el vendedor con sus publicaciones y su perfil: lo mismo que CarRepository,
// StatsRepository y UserRepository de la app (mismas tablas y funciones de la BD).
import { publicUrl, supabase, BUCKET } from '../lib/supabase'
import { traducir } from '../i18n/I18nContext'
import { carsByIds, toCar, UserError } from './api'
import type { AnuncioRow, Car } from './types'

function check<T>(res: { data: T; error: { message: string; code?: string } | null }): T {
  if (res.error) {
    // P0001 = RAISE EXCEPTION de las funciones de la BD, que ya vienen en español.
    if (res.error.code === 'P0001') throw new UserError(res.error.message)
    if (res.error.code === '42501') throw new UserError(traducir('publicar.errNoPermission'))
    throw new Error(res.error.message)
  }
  return res.data
}

async function uid(): Promise<string> {
  const { data } = await supabase.auth.getUser()
  if (!data.user) throw new UserError(traducir('publicar.errLoginFirst'))
  return data.user.id
}

// ── Publicaciones propias ───────────────────────────────────────────────────

/** Publicación propia con los datos que solo ve su dueño (Car.kt de la app). */
export interface OwnCar extends Car {
  featuredPending: boolean
  /** Fin del destacado vigente (dura 1 mes desde que el admin lo aprueba). */
  featuredUntil: string | null
  /** El destacado termina en 3 días o menos: se puede renovar. */
  featuredEndsSoon: boolean
  rejectedReason: string | null
  disabledReason: string | null
  inReview: boolean
  approved: boolean
  soldVia: string | null
}

export function toOwnCar(a: AnuncioRow): OwnCar {
  return {
    ...toCar(a),
    featuredPending: a.destacado_pendiente,
    featuredUntil: null,
    featuredEndsSoon: false,
    rejectedReason: a.estado_ultima_propuesta === 'rechazada' ? a.motivo_rechazo_propuesta : null,
    disabledReason: a.estado_administrativo === 'deshabilitada_administrador' ? a.motivo_deshabilitacion : null,
    inReview: a.estado_ultima_propuesta === 'pendiente',
    approved: a.aprobada,
    soldVia: a.medio_venta ?? null,
  }
}

/** Todas las publicaciones de la cuenta en sesión, incluidas pendientes y rechazadas. */
export async function myCars(ownerId: string): Promise<OwnCar[]> {
  const rows = check(await supabase.from('v_anuncios').select('*').eq('id_propietario', ownerId).order('fecha_creacion', { ascending: false })) as AnuncioRow[]
  const cars = rows.map(toOwnCar)
  const ids = cars.filter((c) => c.featured).map((c) => c.id)
  if (!ids.length) return cars
  // Fin del destacado vigente; si la consulta falla, las publicaciones se muestran igual.
  const { data } = await supabase.from('solicitudes_destacado').select('id_publicacion,fecha_fin_destacado').in('id_publicacion', ids).eq('estado_solicitud', 'aprobada')
  const fin = new Map<number, string>()
  for (const f of (data ?? []) as { id_publicacion: number; fecha_fin_destacado: string | null }[]) {
    if (f.fecha_fin_destacado && (!fin.has(f.id_publicacion) || f.fecha_fin_destacado > fin.get(f.id_publicacion)!)) fin.set(f.id_publicacion, f.fecha_fin_destacado)
  }
  const tresDias = 3 * 24 * 3600 * 1000
  return cars.map((c) => {
    const f = fin.get(c.id)
    return f ? { ...c, featuredUntil: f, featuredEndsSoon: new Date(f).getTime() - Date.now() <= tresDias } : c
  })
}

/** Datos de una publicación propia para precargar el formulario al editarla. */
export async function ownListing(id: number): Promise<AnuncioRow | null> {
  return check(await supabase.from('v_anuncios').select('*').eq('id_publicacion', id).maybeSingle()) as AnuncioRow | null
}

/** Sube una foto al bucket y devuelve su ruta (publicaciones/<id_cuenta>/<archivo>). */
export async function uploadPhoto(blob: Blob): Promise<string> {
  const ruta = `publicaciones/${await uid()}/${crypto.randomUUID()}.jpg`
  const { error } = await supabase.storage.from(BUCKET).upload(ruta, blob, { upsert: false, contentType: 'image/jpeg' })
  if (error) throw new Error(error.message)
  return ruta
}

/** Crea la publicación con su propuesta pendiente (función crear_publicacion). */
export async function crearPublicacion(datos: Record<string, unknown>, fotos: string[]): Promise<number> {
  return check(await supabase.rpc('crear_publicacion', { p_datos: datos, p_fotos: fotos })) as number
}

/** Edita una publicación propia o corrige una rechazada: queda en revisión (editar_publicacion). */
export async function editarPublicacion(id: number, datos: Record<string, unknown>, fotos: string[]): Promise<void> {
  check(await supabase.rpc('editar_publicacion', { p_id_publicacion: id, p_datos: datos, p_fotos: fotos }))
}

/** Pausa (deshabilita) o reactiva una publicación propia; no afecta lo que decida el admin. */
export async function setPaused(id: number, paused: boolean): Promise<void> {
  check(await supabase.from('publicaciones').update({ estado_publicacion: paused ? 'deshabilitada' : 'activa' }).eq('id_publicacion', id))
}

/** Marca como vendida ("dentro_app" / "fuera_app") o la regresa a disponible (marcar_publicacion_vendida). */
export async function setSold(id: number, sold: boolean, via: string | null = null): Promise<void> {
  check(await supabase.rpc('marcar_publicacion_vendida', { p_id_publicacion: id, p_vendida: sold, p_medio_venta: via }))
}

/** Solicita destacar la publicación; un admin la revisa. */
export async function requestFeatured(id: number): Promise<void> {
  const { error } = await supabase.from('solicitudes_destacado').insert({ id_publicacion: id })
  if (error) {
    if (error.message.includes('uq_solicitudes_pendiente') || error.code === '23505') throw new UserError(traducir('publicar.errFeaturedPending'))
    check({ data: null, error })
  }
}

/** Cuántos usuarios guardaron en favoritos cada publicación de la cuenta en sesión. */
export async function favoriteCounts(): Promise<Map<number, number>> {
  const rows = check(await supabase.rpc('favoritos_mis_publicaciones')) as { id_publicacion: number; total: number }[]
  return new Map((rows ?? []).map((r) => [r.id_publicacion, r.total]))
}

// ── Estadísticas del panel ──────────────────────────────────────────────────

export interface SellerStats {
  publicadas: number
  activas: number
  vendidas: number
  pendientes: number
  vistas: number
  contactos: number
  vistasMes: number
  vistasMesAnterior: number
  vistasPorMes: number[]
  favoritosSemana: number[]
  diasSemana: string[]
}

export async function sellerStats(): Promise<SellerStats> {
  const j = (check(await supabase.rpc('vendedor_estadisticas')) ?? {}) as Record<string, unknown>
  const n = (k: string) => Number(j[k] ?? 0)
  const arr = (k: string) => ((j[k] as unknown[]) ?? []).map(Number)
  return {
    publicadas: n('publicadas'),
    activas: n('activas'),
    vendidas: n('vendidas'),
    pendientes: n('pendientes'),
    vistas: n('vistas'),
    contactos: n('contactos'),
    vistasMes: n('vistas_mes'),
    vistasMesAnterior: n('vistas_mes_anterior'),
    vistasPorMes: arr('vistas_por_mes'),
    favoritosSemana: arr('favoritos_semana'),
    diasSemana: ((j.dias_semana as unknown[]) ?? []).map(String),
  }
}

// ── Historial ───────────────────────────────────────────────────────────────

/** Anuncios vistos por la cuenta en sesión (los últimos 200), con la fecha de la última vista. */
export async function history(userId: string): Promise<{ car: Car; seenAt: string }[]> {
  const rows = check(
    await supabase
      .from('interacciones_publicacion')
      .select('id_publicacion,fecha_hora')
      .eq('id_cuenta', userId)
      .eq('tipo_interaccion', 'ver_detalle')
      .order('fecha_hora', { ascending: false })
      .limit(200),
  ) as { id_publicacion: number; fecha_hora: string }[]
  const vistas = new Map<number, string>()
  for (const r of rows) if (!vistas.has(r.id_publicacion)) vistas.set(r.id_publicacion, r.fecha_hora)
  const cars = await carsByIds([...vistas.keys()])
  return cars.map((car) => ({ car, seenAt: vistas.get(car.id)! }))
}

// ── Perfil (UserRepository) ─────────────────────────────────────────────────

export const LLAMADA = 'llamada'
export const WHATSAPP = 'whatsapp'

export interface Perfil {
  id: string
  displayName: string
  email: string
  phone: string
  whatsapp: string
  medioPrincipal: string
  bio: string
  avatarUrl: string | null
  coverUrl: string | null
  memberSince: string
  accountType: 'particular' | 'lote'
  estado: string
  municipio: string
  responsable: string
  calle: string
  numero: string
  colonia: string
  codigoPostal: string
}

interface TelefonoRow {
  id_telefono: number
  numero_telefono: string
  permite_llamadas: boolean
  es_whatsapp: boolean
  es_principal: boolean
  activo: boolean
}

export async function currentProfile(): Promise<Perfil | null> {
  const { data: u } = await supabase.auth.getUser()
  const user = u.user
  if (!user) return null
  const cuenta = check(await supabase.from('cuentas').select('*').eq('id_cuenta', user.id).maybeSingle()) as Record<string, string | null> | null
  if (!cuenta) return null
  const lote =
    cuenta.tipo_cuenta === 'lote'
      ? (check(await supabase.from('perfiles_lote').select('*').eq('id_cuenta', user.id).maybeSingle()) as Record<string, string | null> | null)
      : null
  const tels = ((check(await supabase.from('telefonos_contacto').select('*').eq('id_cuenta', user.id)) ?? []) as TelefonoRow[])
    .filter((t) => t.activo)
    .sort((a, b) => Number(b.es_principal) - Number(a.es_principal))
  const telefono = tels.find((t) => t.permite_llamadas)
  const whatsapp = tels.find((t) => t.es_whatsapp)
  let correo = user.email ?? ''
  if (!correo) correo = ((await supabase.rpc('mi_correo')).data as string | null) ?? ''

  return {
    id: user.id,
    displayName: cuenta.nombre_mostrar ?? '',
    email: correo,
    phone: telefono?.numero_telefono ?? '',
    whatsapp: whatsapp?.numero_telefono ?? '',
    medioPrincipal: !telefono ? WHATSAPP : !whatsapp ? LLAMADA : cuenta.medio_contacto_principal ?? LLAMADA,
    bio: cuenta.descripcion_corta ?? '',
    avatarUrl: publicUrl(cuenta.foto_perfil),
    coverUrl: publicUrl(cuenta.foto_portada),
    memberSince: cuenta.fecha_creacion ?? '',
    accountType: cuenta.tipo_cuenta === 'lote' ? 'lote' : 'particular',
    estado: cuenta.estado_perfil_ubi ?? '',
    municipio: cuenta.municipio_perfil_ubi ?? '',
    // El responsable del lote se guardó en los metadatos de Auth al registrarse.
    responsable: String(user.user_metadata?.responsable ?? ''),
    calle: lote?.calle ?? '',
    numero: lote?.numero ?? '',
    colonia: lote?.colonia ?? '',
    codigoPostal: lote?.codigo_postal ?? '',
  }
}

/**
 * Guarda nombre, descripción, ubicación y teléfonos; en un lote también su dirección y
 * responsable. El correo no se cambia desde el perfil.
 */
export async function updateProfile(original: Perfil, p: Perfil): Promise<void> {
  check(
    await supabase
      .from('cuentas')
      .update({
        nombre_mostrar: p.displayName.trim(),
        descripcion_corta: p.bio.trim().slice(0, 300) || null,
        estado_perfil_ubi: p.estado.trim(),
        municipio_perfil_ubi: p.municipio.trim(),
      })
      .eq('id_cuenta', p.id),
  )
  if (p.accountType === 'lote') {
    check(
      await supabase
        .from('perfiles_lote')
        .update({ calle: p.calle.trim(), numero: p.numero.trim(), colonia: p.colonia.trim(), codigo_postal: p.codigoPostal.trim() })
        .eq('id_cuenta', p.id),
    )
    if (p.responsable.trim() !== original.responsable.trim()) {
      const { error } = await supabase.auth.updateUser({ data: { responsable: p.responsable.trim() } })
      if (error) throw new Error(error.message)
    }
  }
  if (p.phone.trim() !== original.phone.trim() || p.whatsapp.trim() !== original.whatsapp.trim() || p.medioPrincipal !== original.medioPrincipal) {
    await savePhones(p.id, p.phone.trim(), p.whatsapp.trim(), p.medioPrincipal)
  }
}

/**
 * Deja los teléfonos como los arma el registro (fn_auth_crear_cuenta): una fila si llamadas y
 * WhatsApp usan el mismo número, o una por número; es_principal marca el del medio preferido.
 * Un número quitado se desactiva (no se borra) y se reactiva si vuelve a agregarse.
 */
async function savePhones(id: string, llamadas: string, whatsapp: string, medio: string): Promise<void> {
  type Deseado = { numero: string; llamadas: boolean; whatsapp: boolean; principal: boolean }
  const deseados: Deseado[] =
    llamadas && llamadas === whatsapp
      ? [{ numero: llamadas, llamadas: true, whatsapp: true, principal: true }]
      : [
          ...(llamadas ? [{ numero: llamadas, llamadas: true, whatsapp: false, principal: medio === LLAMADA }] : []),
          ...(whatsapp ? [{ numero: whatsapp, llamadas: false, whatsapp: true, principal: medio === WHATSAPP }] : []),
        ]
  check(await supabase.from('cuentas').update({ medio_contacto_principal: medio }).eq('id_cuenta', id))
  const existentes = check(await supabase.from('telefonos_contacto').select('*').eq('id_cuenta', id)) as TelefonoRow[]
  // Primero nadie es principal (uq_telefonos_contacto_principal) y se apagan los que ya no van.
  check(await supabase.from('telefonos_contacto').update({ es_principal: false }).eq('id_cuenta', id))
  for (const e of existentes.filter((e) => e.activo && !deseados.some((d) => d.numero === e.numero_telefono))) {
    check(await supabase.from('telefonos_contacto').update({ activo: false }).eq('id_telefono', e.id_telefono))
  }
  // El principal al final, cuando los demás ya no lo son.
  for (const d of [...deseados].sort((a, b) => Number(a.principal) - Number(b.principal))) {
    const fila = existentes.find((e) => e.numero_telefono === d.numero)
    const valores = { permite_llamadas: d.llamadas, es_whatsapp: d.whatsapp, es_principal: d.principal }
    if (fila) check(await supabase.from('telefonos_contacto').update({ ...valores, activo: true }).eq('id_telefono', fila.id_telefono))
    else check(await supabase.from('telefonos_contacto').insert({ id_cuenta: id, numero_telefono: d.numero, ...valores }))
  }
}

/** Sube la portada (ya comprimida) y la guarda en la cuenta. */
export async function uploadCover(blob: Blob): Promise<string | null> {
  const id = await uid()
  const ruta = `perfiles/${id}/portada-${crypto.randomUUID()}.jpg`
  const { error } = await supabase.storage.from(BUCKET).upload(ruta, blob, { upsert: false, contentType: 'image/jpeg' })
  if (error) throw new Error(error.message)
  check(await supabase.from('cuentas').update({ foto_portada: ruta }).eq('id_cuenta', id))
  return publicUrl(ruta)
}

/** Quita la portada: el perfil vuelve al degradado de la marca. */
export async function removeCover(): Promise<void> {
  check(await supabase.from('cuentas').update({ foto_portada: null }).eq('id_cuenta', await uid()))
}

// ── Publicaciones ocultas (solo en este navegador, como en la app solo en ese teléfono) ──

const ESTADOS_OCULTABLES: Car['status'][] = ['Rechazado', 'Deshabilitado', 'Pausado']
export const sePuedeOcultar = (c: Car) => ESTADOS_OCULTABLES.includes(c.status)

function ocultasKey(userId: string) {
  return `karsy-ocultas-${userId}`
}
export function ocultas(userId: string): Set<number> {
  try {
    return new Set(JSON.parse(localStorage.getItem(ocultasKey(userId)) ?? '[]') as number[])
  } catch {
    return new Set()
  }
}
export function setOculta(userId: string, id: number, oculta: boolean) {
  const s = ocultas(userId)
  if (oculta) s.add(id)
  else s.delete(id)
  try {
    localStorage.setItem(ocultasKey(userId), JSON.stringify([...s]))
  } catch {
    /* sin almacenamiento: no se recuerda */
  }
}
