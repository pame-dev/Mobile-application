// Lo que hace el vendedor con sus publicaciones y su perfil: lo mismo que CarRepository,
// StatsRepository y UserRepository de la app (mismas tablas y funciones de la BD).
import { publicUrl, supabase, BUCKET } from '../lib/supabase'
import { traducir } from '../i18n/I18nContext'
import { EXT_CONSTANCIA, tipoConstancia } from './auth'
import { carsByIds, conPropuesta, sigueDisponible, toCar, UserError } from './api'
import type { AnuncioRow, Car, DocumentoVehiculo, EstadoVerificacion } from './types'

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
  /** Fin del destacado vigente (dura los días de config_pagos desde que el admin lo aprueba). */
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
  return conFinDestacado(rows.map(toOwnCar))
}

/** Una publicación propia con los datos de Mi panel (para sus acciones en el detalle); null si no existe. */
export async function ownCar(id: number): Promise<OwnCar | null> {
  const row = await ownListing(id)
  return row ? (await conFinDestacado([toOwnCar(row)]))[0] : null
}

/** Agrega la fecha de fin del destacado vigente. */
async function conFinDestacado(cars: OwnCar[]): Promise<OwnCar[]> {
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
  const a = check(await supabase.from('v_anuncios').select('*').eq('id_publicacion', id).maybeSingle()) as AnuncioRow | null
  // Con cambios en revisión o rechazados, el formulario parte de esa propuesta (la que
  // editar_publicacion vuelve a actualizar): en una ya aprobada, v_anuncios sigue
  // mostrando la versión aprobada. Igual que CarRepository.ownListing en la app.
  return a && conPropuesta(a, ['pendiente', 'rechazada'])
}

/** Sube una foto al bucket y devuelve su ruta (publicaciones/<id_cuenta>/<archivo>). */
export async function uploadPhoto(blob: Blob): Promise<string> {
  const ruta = `publicaciones/${await uid()}/${crypto.randomUUID()}.jpg`
  const { error } = await supabase.storage.from(BUCKET).upload(ruta, blob, { upsert: false, contentType: 'image/jpeg' })
  if (error) throw new Error(error.message)
  return ruta
}

/** Bucket privado de las fotos o PDF de tarjeta de circulación y documento de baja. */
const BUCKET_DOCUMENTOS = 'documentos-vehiculo'

/**
 * Sube la foto (JPG o PNG) o el PDF del documento del vehículo, tal cual, al bucket privado y devuelve
 * su ruta (<id_cuenta>/<uuid>.<jpg|png|pdf>). El archivo ya se revisó con errorConstancia.
 */
export async function uploadDocumento(f: File): Promise<string> {
  const tipo = tipoConstancia(f)
  const ruta = `${await uid()}/${crypto.randomUUID()}.${EXT_CONSTANCIA[tipo]}`
  const { error } = await supabase.storage.from(BUCKET_DOCUMENTOS).upload(ruta, f, { upsert: false, contentType: tipo })
  if (error) throw new Error(error.message)
  return ruta
}

/**
 * Documento del vehículo de la propuesta más reciente que lo tenga; null si nunca se dio.
 * Solo lo pueden leer el dueño y el admin (RLS de propuestas_publicacion). Igual que
 * CarRepository.documentoVehiculo en la app.
 */
export async function documentoVehiculo(idPublicacion: number): Promise<DocumentoVehiculo | null> {
  const rows = check(
    await supabase
      .from('propuestas_publicacion')
      .select('tipo_documento_vehiculo, documento_vehiculo')
      .eq('id_publicacion', idPublicacion)
      .not('tipo_documento_vehiculo', 'is', null)
      .order('fecha_envio_moderacion', { ascending: false })
      .order('id_propuesta', { ascending: false })
      .limit(1),
  ) as { tipo_documento_vehiculo: DocumentoVehiculo['tipo']; documento_vehiculo: string }[] | null
  const r = rows?.[0]
  return r ? { tipo: r.tipo_documento_vehiculo, valor: r.documento_vehiculo } : null
}

/** URL firmada de 10 minutos de la foto o el PDF de un documento del vehículo (el bucket es privado). */
export async function documentoVehiculoUrl(ruta: string): Promise<string> {
  const { data, error } = await supabase.storage.from(BUCKET_DOCUMENTOS).createSignedUrl(ruta, 600)
  if (error) throw new Error(error.message)
  return data.signedUrl
}

/**
 * Posiciones (desde 0) de las huellas visuales que ya están en una publicación de otra cuenta
 * (función fotos_ya_usadas). Igual que CarRepository.fotosYaUsadas en la app.
 */
export async function fotosYaUsadas(huellas: string[]): Promise<number[]> {
  if (!huellas.length) return []
  const r = check(await supabase.rpc('fotos_ya_usadas', { p_huellas: huellas })) as number[] | null
  return (r ?? []).map((p) => p - 1)
}

/** Guarda las huellas visuales de las fotos de una publicación propia (al publicar o editar). */
export async function registrarHuellas(idPublicacion: number, huellas: string[]): Promise<void> {
  check(await supabase.rpc('registrar_huellas_fotos', { p_id_publicacion: idPublicacion, p_huellas: huellas }))
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

// ── Destacar (solo lotes, pago por transferencia) ──────────────────────────

/** Precio, días y cuenta para pagar un destacado. Sin banco o CLABE todavía no se puede pedir. */
export interface DatosPago {
  precio: number
  dias: number
  banco: string | null
  clabe: string | null
}

/** Datos para pagar un destacado (datos_pago_destacado; también los lee el admin). */
export async function datosPagoDestacado(): Promise<DatosPago> {
  const rows = check(await supabase.rpc('datos_pago_destacado')) as { precio: number | string; dias: number; banco: string | null; clabe: string | null }[] | null
  const r = rows?.[0]
  return { precio: Number(r?.precio ?? 0), dias: Number(r?.dias ?? 30), banco: r?.banco ?? null, clabe: r?.clabe ?? null }
}

/** Referencia que va en el concepto de la transferencia (la misma que guarda solicitar_destacado). */
export const referenciaPago = (idPublicacion: number) => `KAR-${idPublicacion}`

/** Bucket privado de los comprobantes de pago de destacados. */
const BUCKET_COMPROBANTES = 'comprobantes-pago'

/**
 * Sube la foto (JPG o PNG) o el PDF del comprobante, tal cual, a <id_cuenta>/<uuid>.<ext> y devuelve
 * su ruta. El archivo ya se revisó con errorConstancia (mismo tipo y tamaño máximo).
 */
export async function subirComprobante(f: File): Promise<string> {
  const tipo = tipoConstancia(f)
  const ruta = `${await uid()}/${crypto.randomUUID()}.${EXT_CONSTANCIA[tipo]}`
  const { error } = await supabase.storage.from(BUCKET_COMPROBANTES).upload(ruta, f, { upsert: false, contentType: tipo })
  if (error) throw new Error(error.message)
  return ruta
}

/**
 * Pide destacar la publicación con el comprobante ya subido (solicitar_destacado); un admin
 * revisa el pago. Si no se envía, el archivo lo borra después la limpieza del panel admin.
 */
export async function requestFeatured(id: number, comprobante: string): Promise<void> {
  const { error } = await supabase.rpc('solicitar_destacado', { p_id_publicacion: id, p_comprobante: comprobante })
  if (error) {
    if (error.code === '23505') throw new UserError(traducir('publicar.errFeaturedPending'))
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
  /** Totales de las publicaciones ya publicadas. */
  embudo: { vistas: number; favoritos: number; contactos: number }
  /** Totales por publicación (la más vista primero). */
  rendimiento: { id: number; vistas: number; favoritos: number; contactos: number }[]
}

export async function sellerStats(): Promise<SellerStats> {
  const j = (check(await supabase.rpc('vendedor_estadisticas')) ?? {}) as Record<string, unknown>
  const n = (k: string) => Number(j[k] ?? 0)
  const arr = (k: string) => ((j[k] as unknown[]) ?? []).map(Number)
  const embudo = (j.embudo ?? {}) as Record<string, unknown>
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
    embudo: { vistas: Number(embudo.vistas ?? 0), favoritos: Number(embudo.favoritos ?? 0), contactos: Number(embudo.contactos ?? 0) },
    rendimiento: ((j.rendimiento as Record<string, unknown>[]) ?? []).map((r) => ({
      id: Number(r.id),
      vistas: Number(r.vistas ?? 0),
      favoritos: Number(r.favoritos ?? 0),
      contactos: Number(r.contactos ?? 0),
    })),
  }
}

/** Datos extra del reporte de Mi panel (vendedor_reporte); solo se piden al exportar. */
export interface ReporteExtra {
  /** Precio promedio de autos similares de otros vendedores (misma marca/modelo, año ±2, misma moneda). */
  mercado: { id: number; promedio: number; similares: number }[]
  /** Vistas por día de la semana (domingo = 0) y por hora (0–23), hora de México. */
  porDia: number[]
  porHora: number[]
  /** Último destacado de cada auto: vistas en los días antes vs. los días que lleva destacado. */
  destacados: { id: number; inicio: string; fin: string; dias: number; diasAntes: number; diasDurante: number; vistasAntes: number; vistasDurante: number }[]
}

export async function sellerReport(): Promise<ReporteExtra> {
  const j = (check(await supabase.rpc('vendedor_reporte')) ?? {}) as Record<string, unknown>
  const lista = (k: string) => ((j[k] as Record<string, unknown>[]) ?? [])
  const nums = (k: string) => ((j[k] as unknown[]) ?? []).map(Number)
  return {
    mercado: lista('mercado').map((m) => ({ id: Number(m.id), promedio: Number(m.promedio ?? 0), similares: Number(m.similares ?? 0) })),
    porDia: nums('por_dia'),
    porHora: nums('por_hora'),
    destacados: lista('destacados').map((d) => ({
      id: Number(d.id),
      inicio: String(d.inicio ?? ''),
      fin: String(d.fin ?? ''),
      dias: Number(d.dias ?? 1),
      diasAntes: Number(d.dias_antes ?? 0),
      diasDurante: Number(d.dias_durante ?? 0),
      vistasAntes: Number(d.vistas_antes ?? 0),
      vistasDurante: Number(d.vistas_durante ?? 0),
    })),
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
  const cars = (await carsByIds([...vistas.keys()])).filter(sigueDisponible)
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
  /** Insignia de verificado (cuentas.verificado). */
  verificado: boolean
  /** Estado del último documento enviado (verificaciones_usuario) y motivo si se rechazó. */
  verificacion: EstadoVerificacion
  motivoVerificacion: string
  /** Cuándo se envió el último documento ("Actualizado el …"). */
  fechaVerificacion: string | null
  /** Cuándo la revisó el admin ("Desde el …" si está verificada). */
  fechaRevision: string | null
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
  // Último documento de verificación enviado (si la consulta falla, se muestra como sin enviar).
  const { data: verif } = await supabase.from('verificaciones_usuario').select('estado,motivo_rechazo,fecha_envio,fecha_revision').eq('user_id', user.id).order('fecha_envio', { ascending: false }).limit(1)
  const ultimaVerificacion = (verif?.[0] ?? null) as { estado: string; motivo_rechazo: string | null; fecha_envio: string; fecha_revision: string | null } | null
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
    verificado: String(cuenta.verificado) === 'true',
    verificacion: (ultimaVerificacion?.estado as EstadoVerificacion | undefined) ?? 'sin_documento',
    motivoVerificacion: ultimaVerificacion?.motivo_rechazo ?? '',
    fechaVerificacion: ultimaVerificacion?.fecha_envio ?? null,
    fechaRevision: ultimaVerificacion?.fecha_revision ?? null,
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

/** Clave de la versión anterior, cuando las ocultas solo se guardaban en este navegador. */
function ocultasKey(userId: string) {
  return `karsy-ocultas-${userId}`
}

/**
 * Publicaciones propias que el dueño ocultó de su perfil (tabla publicaciones_ocultas: la
 * app y la web ven la misma lista). La primera vez se suben las que estaban guardadas solo
 * en este navegador.
 */
export async function ocultas(userId: string): Promise<Set<number>> {
  const filas = check(await supabase.from('publicaciones_ocultas').select('id_publicacion').eq('id_cuenta', userId)) as {
    id_publicacion: number
  }[]
  const ids = new Set(filas.map((f) => f.id_publicacion))
  let locales: number[] = []
  try {
    locales = JSON.parse(localStorage.getItem(ocultasKey(userId)) ?? '[]') as number[]
  } catch {
    /* sin almacenamiento */
  }
  const faltan = locales.filter((id) => !ids.has(id))
  if (faltan.length) {
    // Si alguna ya no es suya o se borró, la BD la rechaza y simplemente no se sube.
    for (const id of faltan) {
      const { error } = await supabase.from('publicaciones_ocultas').insert({ id_cuenta: userId, id_publicacion: id })
      if (!error) ids.add(id)
    }
  }
  if (locales.length) {
    try {
      localStorage.removeItem(ocultasKey(userId))
    } catch {
      /* sin almacenamiento */
    }
  }
  return ids
}

export async function setOculta(userId: string, id: number, oculta: boolean): Promise<void> {
  if (oculta) {
    const { error } = await supabase.from('publicaciones_ocultas').upsert({ id_cuenta: userId, id_publicacion: id }, { ignoreDuplicates: true })
    if (error) throw new Error(error.message)
  } else {
    check(await supabase.from('publicaciones_ocultas').delete().eq('id_cuenta', userId).eq('id_publicacion', id))
  }
}
