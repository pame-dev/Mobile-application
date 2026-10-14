// Acceso a datos. Usa las MISMAS tablas, vista (v_anuncios) y funciones que la app de
// Android (data/repository/*.kt), así lo que se haga aquí se ve en la app y al revés.
import { traducir } from '../i18n/I18nContext'
import { publicUrl, supabase } from '../lib/supabase'
import type { AnuncioRow, Car, CarDetail, Catalogs, Lot, LotContact, SellerContact, SellerProfile } from './types'

const VISTA = 'v_anuncios'

/** Error con un mensaje listo para mostrar al usuario. */
export class UserError extends Error {}

function check<T>(res: { data: T; error: { message: string } | null }): T {
  if (res.error) throw new Error(res.error.message)
  return res.data
}

// ── Mapeo (igual que CarRepository.toCar / toDetail) ───────────────────────

function statusOf(a: AnuncioRow): Car['status'] {
  if (!a.aprobada && a.estado_ultima_propuesta === 'rechazada') return 'Rechazado'
  if (!a.aprobada) return 'Pendiente'
  if (a.estado_administrativo === 'deshabilitada_administrador') return 'Deshabilitado'
  if (a.estado_publicacion === 'vendida') return 'Vendido'
  if (a.estado_publicacion === 'deshabilitada') return 'Pausado'
  return 'Activo'
}

export function toCar(a: AnuncioRow): Car {
  return {
    id: a.id_publicacion,
    brand: a.marca ?? traducir('vehiculo.noBrand'),
    model: a.modelo ?? '',
    year: a.anio ?? 0,
    priceValue: a.precio ?? 0,
    currency: a.moneda ?? 'MXN',
    description: a.descripcion ?? '',
    imageUrl: publicUrl(a.fotos?.[0]),
    featured: a.destacado,
    ownerId: a.id_propietario,
    ownerName: a.propietario_nombre,
    sellerType: a.propietario_tipo,
    status: statusOf(a),
    km: a.kilometraje,
    brandId: a.id_marca,
    modelId: a.id_modelo,
    bodyType: a.carroceria ?? '',
    publishedAt: a.fecha_primera_publicacion ?? a.fecha_creacion,
    city: [a.municipio_ubicacion, a.estado_ubicacion].filter(Boolean).join(', '),
  }
}

const o = (v: unknown) => (v == null || v === '' ? '—' : String(v))

function toDetail(a: AnuncioRow): CarDetail {
  return {
    car: toCar(a),
    gallery: (a.fotos ?? []).map(publicUrl).filter((u): u is string => !!u),
    transmision: o(a.transmision),
    combustible: o(a.tipo_combustible),
    motor: o(a.cilindrada),
    cilindros: o(a.cilindros),
    tipoCarro: o(a.carroceria),
    color: o(a.color),
    duenos: o(a.numero_propietarios_anteriores),
    recuperadoPorSeguro: a.recuperado_por_seguro == null ? '—' : a.recuperado_por_seguro ? traducir('vehiculo.yes') : traducir('vehiculo.no'),
    detalles: a.tiene_problemas ? a.descripcion_problemas ?? '' : traducir('vehiculo.noProblems'),
    sellerAvatar: publicUrl(a.propietario_foto),
    row: a,
  }
}

// ── Lecturas ────────────────────────────────────────────────────────────────

/** Anuncios aprobados, activos y habilitados, del más reciente al más antiguo. */
export async function visibleCars(): Promise<Car[]> {
  const rows = check(
    await supabase.from(VISTA).select('*').eq('visible', true).order('fecha_primera_publicacion', { ascending: false }),
  ) as AnuncioRow[]
  return rows.map(toCar)
}

export async function carDetail(id: number): Promise<CarDetail | null> {
  const row = check(await supabase.from(VISTA).select('*').eq('id_publicacion', id).maybeSingle()) as AnuncioRow | null
  return row ? toDetail(row) : null
}

export async function carsByIds(ids: number[]): Promise<Car[]> {
  if (ids.length === 0) return []
  const rows = check(await supabase.from(VISTA).select('*').in('id_publicacion', ids)) as AnuncioRow[]
  const porId = new Map(rows.map((r) => [r.id_publicacion, r]))
  return ids.flatMap((id) => (porId.has(id) ? [toCar(porId.get(id)!)] : []))
}

/** Catálogos con "Otros" al final de cada lista (igual que CatalogRepository). */
let catalogCache: Promise<Catalogs> | null = null
export function catalogs(): Promise<Catalogs> {
  catalogCache ??= (async () => {
    const otrosAlFinal = <T extends { esOtro: boolean }>(l: T[]) => [...l].sort((a, b) => Number(a.esOtro) - Number(b.esOtro))
    const [marcas, modelos, colores, transmisiones, carrocerias] = await Promise.all([
      supabase.from('marcas').select('id_marca,nombre_marca,es_otro').order('nombre_marca'),
      supabase.from('modelos').select('id_modelo,id_marca,nombre_modelo,es_otro').order('nombre_modelo'),
      supabase.from('colores').select('id_color,nombre_color,es_otro').order('id_color'),
      supabase.from('transmisiones').select('id_transmision,nombre_transmision,es_otro').order('id_transmision'),
      supabase.from('carrocerias').select('id_carroceria,nombre_carroceria,es_otro').order('id_carroceria'),
    ])
    type R = Record<string, unknown>
    const items = (res: { data: unknown; error: { message: string } | null }, id: string, nombre: string) =>
      otrosAlFinal((check(res) as R[]).map((r) => ({ id: r[id] as number, nombre: r[nombre] as string, esOtro: r.es_otro as boolean })))
    return {
      marcas: items(marcas, 'id_marca', 'nombre_marca'),
      modelos: otrosAlFinal(
        (check(modelos) as R[]).map((r) => ({
          id: r.id_modelo as number,
          idMarca: r.id_marca as number,
          nombre: r.nombre_modelo as string,
          esOtro: r.es_otro as boolean,
        })),
      ),
      colores: items(colores, 'id_color', 'nombre_color'),
      transmisiones: items(transmisiones, 'id_transmision', 'nombre_transmision'),
      carrocerias: items(carrocerias, 'id_carroceria', 'nombre_carroceria'),
    }
  })()
  catalogCache.catch(() => (catalogCache = null))
  return catalogCache
}

/** Cantidades del encabezado de Inicio: vehículos visibles, marcas y modelos. */
export async function heroStats(cars: Car[]): Promise<{ vehiculos: number; marcas: number; modelos: number }> {
  const c = await catalogs()
  return {
    vehiculos: cars.length,
    marcas: c.marcas.filter((m) => !m.esOtro).length,
    modelos: c.modelos.filter((m) => !m.esOtro).length,
  }
}

// ── Favoritos ───────────────────────────────────────────────────────────────

export async function favoriteIds(userId: string | null): Promise<Set<number>> {
  if (!userId) return new Set()
  const rows = check(await supabase.from('favoritos').select('id_publicacion').eq('id_cuenta', userId)) as { id_publicacion: number }[]
  return new Set(rows.map((r) => r.id_publicacion))
}

/** Favoritos de la cuenta, del guardado más reciente al más antiguo (CarRepository.favoriteCars). */
export async function favoriteCars(userId: string): Promise<Car[]> {
  const rows = check(
    await supabase.from('favoritos').select('id_publicacion').eq('id_cuenta', userId).order('fecha_guardado', { ascending: false }),
  ) as { id_publicacion: number }[]
  return carsByIds(rows.map((r) => r.id_publicacion))
}

export async function setFavorite(userId: string, id: number, favorite: boolean): Promise<void> {
  if (favorite) check(await supabase.from('favoritos').insert({ id_cuenta: userId, id_publicacion: id }))
  else check(await supabase.from('favoritos').delete().eq('id_cuenta', userId).eq('id_publicacion', id))
}

// ── Interacciones ───────────────────────────────────────────────────────────

/** Registra que se abrió el detalle (alimenta estadísticas e historial). Si falla, no pasa nada. */
export async function registerView(id: number, userId: string | null): Promise<void> {
  await supabase.from('interacciones_publicacion').insert({ id_publicacion: id, id_cuenta: userId, tipo_interaccion: 'ver_detalle' })
}

/**
 * Reporta una publicación o la cuenta del vendedor. El motivo se guarda como en la app
 * ("Publicación: …" / "Cuenta del vendedor: …"): el panel de admin lo lee así.
 */
export async function reportListing(id: number, userId: string, tipo: 'publicacion' | 'cuenta', razon: string, detalles: string): Promise<void> {
  const prefijo = tipo === 'publicacion' ? 'Publicación' : 'Cuenta del vendedor'
  const motivo = `${prefijo}: ${razon}` + (detalles.trim() ? `. ${detalles.trim()}` : '')
  const { error } = await supabase.from('reportes').insert({ id_publicacion: id, id_reportante: userId, motivo_reporte: motivo })
  if (error) {
    if (error.message.includes('uq_reportes')) throw new UserError(traducir('vehiculo.alreadyReported'))
    throw new Error(error.message)
  }
}

/** Datos de contacto del vendedor (función contacto_vendedor); con sesión registra el contacto. */
export async function sellerContact(id: number, userId: string | null): Promise<SellerContact | null> {
  const data = check(await supabase.rpc('contacto_vendedor', { p_id_publicacion: id })) as SellerContact | null
  if (data && userId) {
    await supabase.from('interacciones_publicacion').insert({ id_publicacion: id, id_cuenta: userId, tipo_interaccion: 'contactar' })
  }
  return data
}

// ── Lotes y vendedores (LotRepository) ──────────────────────────────────────

interface CuentaRow {
  id_cuenta: string
  nombre_mostrar: string
  tipo_cuenta: 'particular' | 'lote'
  foto_perfil: string | null
  foto_portada: string | null
  descripcion_corta: string | null
  estado_perfil_ubi: string | null
  municipio_perfil_ubi: string | null
  estado_cuenta: string
  fecha_creacion: string
}

interface PerfilLoteRow {
  id_cuenta: string
  calle: string | null
  numero: string | null
  colonia: string | null
  codigo_postal: string | null
  horarios_atencion: string | null
}

const CUENTA_COLS =
  'id_cuenta,nombre_mostrar,tipo_cuenta,foto_perfil,foto_portada,descripcion_corta,estado_perfil_ubi,municipio_perfil_ubi,estado_cuenta,fecha_creacion'

const ciudadDe = (c: CuentaRow) => [c.municipio_perfil_ubi, c.estado_perfil_ubi].filter(Boolean).join(', ')

function toLot(c: CuentaRow, p: PerfilLoteRow | undefined, vehicleCount: number): Lot {
  const direccion = p
    ? [`${p.calle ?? ''} ${p.numero ?? ''}`.trim(), p.colonia ?? '', p.codigo_postal ? `C.P. ${p.codigo_postal}` : '', c.municipio_perfil_ubi ?? '', c.estado_perfil_ubi ?? '']
        .filter((x) => x.trim())
        .join(', ')
    : ''
  return {
    id: c.id_cuenta,
    name: c.nombre_mostrar,
    logoUrl: publicUrl(c.foto_perfil),
    coverUrl: publicUrl(c.foto_portada),
    description: c.descripcion_corta ?? '',
    city: ciudadDe(c),
    address: direccion,
    hours: p?.horarios_atencion ?? null,
    vehicleCount,
  }
}

/**
 * Lotes con cuenta activa, de los que tienen más vehículos a los que tienen menos.
 * No incluye el lote de la cuenta en sesión: el directorio es para ver a los demás.
 */
export async function lots(selfId: string | null): Promise<Lot[]> {
  const [cuentas, perfiles, cars] = await Promise.all([
    supabase.from('cuentas').select(CUENTA_COLS).eq('tipo_cuenta', 'lote').eq('estado_cuenta', 'activa'),
    supabase.from('perfiles_lote').select('*'),
    visibleCars(),
  ])
  const porId = new Map((check(perfiles) as PerfilLoteRow[]).map((p) => [p.id_cuenta, p]))
  const conteo = new Map<string, number>()
  for (const c of cars) conteo.set(c.ownerId, (conteo.get(c.ownerId) ?? 0) + 1)
  return (check(cuentas) as CuentaRow[])
    .filter((c) => c.id_cuenta !== selfId)
    .map((c) => toLot(c, porId.get(c.id_cuenta), conteo.get(c.id_cuenta) ?? 0))
    .sort((a, b) => b.vehicleCount - a.vehicleCount || a.name.localeCompare(b.name, 'es'))
}

export async function lot(id: string): Promise<Lot | null> {
  const [cuenta, perfil] = await Promise.all([
    supabase.from('cuentas').select(CUENTA_COLS).eq('id_cuenta', id).eq('tipo_cuenta', 'lote').maybeSingle(),
    supabase.from('perfiles_lote').select('*').eq('id_cuenta', id).maybeSingle(),
  ])
  const c = check(cuenta) as CuentaRow | null
  return c ? toLot(c, (check(perfil) as PerfilLoteRow | null) ?? undefined, 0) : null
}

/** Teléfono y WhatsApp del lote. Igual que contacto_vendedor, solo con sesión iniciada. */
export async function lotContact(id: string, userId: string | null): Promise<LotContact | null> {
  if (!userId) return null
  const tels = (
    check(await supabase.from('telefonos_contacto').select('*').eq('id_cuenta', id).eq('activo', true)) as {
      numero_telefono: string
      permite_llamadas: boolean
      es_whatsapp: boolean
      es_principal: boolean
    }[]
  ).sort((a, b) => Number(b.es_principal) - Number(a.es_principal))
  return {
    phone: tels.find((t) => t.permite_llamadas)?.numero_telefono ?? null,
    whatsapp: tels.find((t) => t.es_whatsapp)?.numero_telefono ?? null,
  }
}

/** Perfil público de cualquier vendedor. */
export async function sellerProfile(id: string): Promise<SellerProfile | null> {
  const c = check(await supabase.from('cuentas').select(CUENTA_COLS).eq('id_cuenta', id).maybeSingle()) as CuentaRow | null
  if (!c) return null
  return {
    id: c.id_cuenta,
    name: c.nombre_mostrar,
    type: c.tipo_cuenta,
    avatarUrl: publicUrl(c.foto_perfil),
    coverUrl: publicUrl(c.foto_portada),
    description: c.descripcion_corta ?? '',
    city: ciudadDe(c),
    since: c.fecha_creacion,
    active: c.estado_cuenta === 'activa',
  }
}

/** Publicaciones visibles de una cuenta (CarRepository.ownerCars con onlyVisible). */
export async function ownerVisibleCars(ownerId: string): Promise<Car[]> {
  const rows = check(
    await supabase.from(VISTA).select('*').eq('id_propietario', ownerId).eq('visible', true).order('fecha_creacion', { ascending: false }),
  ) as AnuncioRow[]
  return rows.map(toCar)
}
