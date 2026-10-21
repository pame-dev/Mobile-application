// Filas tal como las devuelve Supabase (mismos nombres que Dtos.kt de la app).

/** Fila de la vista public.v_anuncios. */
export interface AnuncioRow {
  id_publicacion: number
  id_propietario: string
  propietario_nombre: string
  propietario_tipo: string
  propietario_foto: string | null
  titulo: string | null
  descripcion: string | null
  precio: number | null
  moneda: string | null
  estado_ubicacion: string | null
  municipio_ubicacion: string | null
  id_marca: number | null
  marca: string | null
  id_modelo: number | null
  modelo: string | null
  anio: number | null
  kilometraje: number | null
  id_color: number | null
  color: string | null
  id_transmision: number | null
  transmision: string | null
  id_carroceria: number | null
  carroceria: string | null
  numero_propietarios_anteriores: number | null
  tiene_problemas: boolean | null
  descripcion_problemas: string | null
  recuperado_por_seguro: boolean | null
  tipo_combustible: string | null
  cilindros: number | null
  cilindrada: string | null
  estado_publicacion: string
  estado_administrativo: string
  fecha_creacion: string
  fecha_primera_publicacion: string | null
  aprobada: boolean
  visible: boolean
  estado_ultima_propuesta: string | null
  id_ultima_propuesta?: number | null
  motivo_rechazo_propuesta: string | null
  motivo_deshabilitacion: string | null
  destacado: boolean
  destacado_pendiente: boolean
  vendido?: boolean
  medio_venta?: string | null
  fotos: string[] | null
}

/** Vehículo como se muestra en listas y tarjetas (equivale a data/model/Car.kt). */
export interface Car {
  id: number
  brand: string
  model: string
  year: number
  priceValue: number
  currency: string
  description: string
  imageUrl: string | null
  featured: boolean
  ownerId: string
  ownerName: string
  sellerType: string
  status: 'Activo' | 'Pendiente' | 'Rechazado' | 'Deshabilitado' | 'Vendido' | 'Pausado'
  km: number | null
  brandId: number | null
  modelId: number | null
  bodyType: string
  publishedAt: string
  city: string
  /** Estado y municipio por separado (filtro "Cerca de mí"). */
  estado: string
  municipio: string
}

/** Detalle completo de una publicación (equivale a data/model/CarDetail.kt). */
export interface CarDetail {
  car: Car
  gallery: string[]
  transmision: string
  combustible: string
  motor: string
  cilindros: string
  tipoCarro: string
  color: string
  duenos: string
  recuperadoPorSeguro: string
  detalles: string
  sellerAvatar: string | null
  row: AnuncioRow
}

/** Respuesta de la función contacto_vendedor (teléfono y correo solo con sesión). */
export interface SellerContact {
  id_cuenta: string
  nombre: string
  tipo_cuenta: string
  descripcion: string | null
  ubicacion: string | null
  foto_perfil: string | null
  miembro_desde: string | null
  telefono: string | null
  whatsapp: boolean | null
  numero_whatsapp: string | null
  medio_principal: string | null
  correo: string | null
}

export interface CatalogItem {
  id: number
  nombre: string
  esOtro: boolean
}

export interface Catalogs {
  marcas: CatalogItem[]
  modelos: (CatalogItem & { idMarca: number })[]
  colores: CatalogItem[]
  transmisiones: CatalogItem[]
  carrocerias: CatalogItem[]
}

/** Cuenta con sesión iniciada (public.cuentas + rol), igual que SessionAccount en la app. */
export interface Account {
  id: string
  nombre: string
  tipoCuenta: 'particular' | 'lote'
  esAdmin: boolean
  correo: string
  fotoPerfil: string | null
}

/** Estado del último documento de verificación del usuario (verificaciones_usuario.estado, o sin enviar). */
export type EstadoVerificacion = 'sin_documento' | 'pendiente' | 'verificado' | 'rechazado'

/** Documentos de verificación (chk_verificaciones_usuario_tipo_documento). */
export type TipoDocumento = 'constancia_situacion_fiscal' | 'curp'

/** Documento del vehículo al publicar (propuestas_publicacion.tipo_documento_vehiculo). */
export type TipoDocumentoVehiculo = 'vin' | 'placa' | 'tarjeta_circulacion' | 'baja'

/**
 * Documento del vehículo de una propuesta. Privado: solo lo leen el dueño y el admin (no está
 * en v_anuncios). [valor] es el VIN o la placa, o la ruta de la foto en el bucket privado.
 */
export interface DocumentoVehiculo {
  tipo: TipoDocumentoVehiculo
  valor: string
}

/** Lote o agencia del directorio (cuentas tipo "lote" + perfiles_lote). */
export interface Lot {
  id: string
  name: string
  logoUrl: string | null
  coverUrl: string | null
  description: string
  /** "Municipio, Estado". */
  city: string
  /** Calle, número, colonia, CP, municipio y estado; vacío si el lote no la registró. */
  address: string
  hours: string | null
  /** Publicaciones visibles (aprobadas, activas y habilitadas). */
  vehicleCount: number
  /** Lote verificado con su constancia fiscal (insignia). */
  verified: boolean
}

/** Números del lote para "Llamar" y "WhatsApp" (solo se piden con sesión iniciada). */
export interface LotContact {
  phone: string | null
  whatsapp: string | null
}

/** Perfil público de un vendedor (cualquier tipo de cuenta). */
export interface SellerProfile {
  id: string
  name: string
  type: 'particular' | 'lote'
  avatarUrl: string | null
  coverUrl: string | null
  description: string
  city: string
  since: string
  active: boolean
}

