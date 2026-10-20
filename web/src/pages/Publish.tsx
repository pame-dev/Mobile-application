import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { AlertBox } from '../components/AuthParts'
import { CarPhoto } from '../components/CarPhoto'
import { ConfirmModal } from '../components/Confirm'
import { Icon } from '../components/Icon'
import { ModalHead, useUi } from '../components/Ui'
import { useTraduccion } from '../components/Traducido'
import { mensajeUsuario } from '../data/auth'
import { catalogs as cargarCatalogos, preciosSimilares } from '../data/api'
import { crearPublicacion, editarPublicacion, fotosYaUsadas, ownListing, registrarHuellas, uploadPhoto } from '../data/seller'
import type { Catalogs } from '../data/types'
import { entero, precio as formatoPrecio } from '../lib/format'
import { calcularRango, compararPrecio, type Rango } from '../lib/precioSugerido'
import { huellaVisual, mismaFoto } from '../lib/huellaVisual'
import { ANGULOS_CON_AUTO, pareceAuto, precargarDetector } from '../lib/detectorAutos'
import { jpegDe } from '../lib/mexico'
import { precargarTapaPlacas, taparPlacas } from '../lib/tapaPlacas'
import { publicUrl } from '../lib/supabase'
import { useSession } from '../session/SessionContext'
import { traducir, useI18n } from '../i18n/I18nContext'
import { nombreCatalogo } from '../lib/catalogo'
import type { Clave } from '../i18n/es'

/** Máximo de fotos por publicación (editar_publicacion valida lo mismo). */
const MAX_FOTOS = 15
/** Máximo de publicaciones.cilindros (chk_publicaciones_cilindros). */
const MAX_CILINDROS = 16
/** Largo máximo de publicaciones.cilindrada (VARCHAR(30)). */
const MOTOR_MAX = 30
/** El valor se guarda tal cual en tipo_combustible; la etiqueta se traduce. */
const COMBUSTIBLES: { valor: string; label: Clave }[] = [
  { valor: 'Gasolina', label: 'publicar.fuelGasolina' },
  { valor: 'Diésel', label: 'publicar.fuelDiesel' },
  { valor: 'Híbrido', label: 'publicar.fuelHibrido' },
  { valor: 'Eléctrico', label: 'publicar.fuelElectrico' },
]
const ANGULOS: Clave[] = ['publicar.photoFront', 'publicar.photoRear', 'publicar.photoLeft', 'publicar.photoRight', 'publicar.photoDashboard']
const PASOS: Clave[] = ['publicar.stepData', 'publicar.stepPhotos', 'publicar.stepDetails', 'publicar.stepConfirm']
/** Sufijo "Potencia: N hp" que se agrega a la descripción (no hay columna para los caballos). */
const POTENCIA = /\n\nPotencia: (\d+) hp$/

/** Una foto del formulario: nueva (archivo) o la que ya estaba en Storage (al editar). */
interface Foto {
  key: string
  url: string
  file?: File
  ruta?: string
}

interface Datos {
  idMarca: string
  marcaOtra: string
  idModelo: string
  modeloOtro: string
  anio: string
  precio: string
  idTransmision: string
  kilometraje: string
  recuperado: boolean
  cilindros: string
  caballos: string
  motor: string
  combustible: string
  idCarroceria: string
  idColor: string
  duenos: string
  descripcion: string
  imperfecciones: string
}

const VACIO: Datos = {
  idMarca: '', marcaOtra: '', idModelo: '', modeloOtro: '', anio: '', precio: '', idTransmision: '', kilometraje: '',
  recuperado: false, cilindros: '', caballos: '', motor: '', combustible: '', idCarroceria: '', idColor: '', duenos: '',
  descripcion: '', imperfecciones: '',
}

const digitos = (v: string, max = 12) => v.replace(/\D/g, '').slice(0, max)

/** SHA-256 del contenido: detecta la misma foto elegida dos veces aunque tenga otro nombre. */
async function huella(f: Foto): Promise<string> {
  if (!f.file) return 'ruta:' + f.ruta
  const hash = await crypto.subtle.digest('SHA-256', await f.file.arrayBuffer())
  return [...new Uint8Array(hash)].map((b) => b.toString(16).padStart(2, '0')).join('')
}

function Campo({ id, label, children }: { id?: string; label: string; children: ReactNode }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children}
    </div>
  )
}

/**
 * Publicar vehículo en 4 pasos (PublishFlowScreen + PublishViewModel de la app).
 * Con /publicar/:id se precarga una publicación propia y se mandan los cambios a revisión.
 */
export function Publish() {
  const { id } = useParams()
  const editId = id ? Number(id) : null
  const navigate = useNavigate()
  const { account, loading: cargandoSesion } = useSession()
  const { openModal, closeModal } = useUi()
  const { t } = useI18n()

  const [cat, setCat] = useState<Catalogs | null>(null)
  const [d, setD] = useState<Datos>(VACIO)
  const [angulos, setAngulos] = useState<(Foto | null)[]>([null, null, null, null, null])
  const [extras, setExtras] = useState<Foto[]>([])
  /** Foto del motor: recomendada, no obligatoria. Se sube después de los 5 ángulos. */
  const [motor, setMotor] = useState<Foto | null>(null)
  /** Fotos del tablero en las que el detector ve el exterior de un auto (solo avisa). */
  const [tableroExterior, setTableroExterior] = useState<Set<string>>(new Set())
  const [step, setStep] = useState(0)
  /** Precio sugerido (paso Detalles): 'cargando', el rango de autos similares o null si no hay suficientes. */
  const [sugerencia, setSugerencia] = useState<'cargando' | 'error' | { rango: Rango; desde?: number; hasta?: number } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargandoEdicion, setCargandoEdicion] = useState(editId != null)
  const [rechazo, setRechazo] = useState<string | null>(null)
  // Lo escribe el admin (en español): en inglés se muestra traducido.
  const rechazoTraducido = useTraduccion(rechazo ?? '')
  const [editandoAprobada, setEditandoAprobada] = useState(false)
  const [intentoFotos, setIntentoFotos] = useState(false)
  const [repetidas, setRepetidas] = useState<Set<string>>(new Set())
  /** Fotos que ya están en una publicación de otra cuenta (fotos_ya_usadas). */
  const [usadas, setUsadas] = useState<Set<string>>(new Set())
  const [revisando, setRevisando] = useState(false)
  const [publicando, setPublicando] = useState(false)
  /** Al publicar: [fotos subidas, total] mientras se suben a Storage. */
  const [subida, setSubida] = useState<[number, number] | null>(null)
  const [listo, setListo] = useState(false)
  const [tocado, setTocado] = useState(false)
  const [arrastrando, setArrastrando] = useState(false)
  const anguloInput = useRef<HTMLInputElement>(null)
  const anguloElegido = useRef(0)
  const extrasInput = useRef<HTMLInputElement>(null)
  /**
   * Huellas de cada foto ORIGINAL (antes de tapar placas), por key: la exacta (SHA-256) y la
   * visual. Se calculan al elegirla; así, aunque una copia se tape y la otra no, coinciden.
   */
  const huellas = useRef(new Map<string, Promise<string>>())
  const visuales = useRef(new Map<string, Promise<string | null>>())
  /** ¿Parece auto? por foto de ángulo (key de la Foto). Sin entrada = revisando. */
  const [revisionAuto, setRevisionAuto] = useState<Map<string, boolean>>(new Map())
  const revisionesAuto = useRef(new Map<string, Promise<boolean>>())
  /** Fotos a las que se les están tapando las placas (key de la Foto). */
  const [tapando, setTapando] = useState<Set<string>>(new Set())
  const trabajosPlacas = useRef<Promise<void>[]>([])
  const existentesRevisadas = useRef(false)
  /** Fotos más recientes (después de un await, las variables del render ya pueden ser viejas). */
  const fotosActuales = useRef<Foto[]>([])
  const arriba = useRef<HTMLDivElement>(null)

  // Catálogos primero: se usan para saber si la marca/modelo es "Otros" al precargar.
  useEffect(() => {
    let vivo = true
    cargarCatalogos()
      .then(async (c) => {
        if (!vivo) return
        setCat(c)
        if (editId == null) return
        const a = await ownListing(editId)
        if (!vivo) return
        if (!a) {
          setError(traducir('publicar.notFound'))
        } else {
          setRechazo(a.estado_ultima_propuesta === 'rechazada' ? a.motivo_rechazo_propuesta : null)
          setEditandoAprobada(a.aprobada)
          const esOtro = (lista: { id: number; esOtro: boolean }[], v: number | null) => lista.find((x) => x.id === v)?.esOtro === true
          const desc = a.descripcion ?? ''
          const pot = desc.match(POTENCIA)
          setD({
            idMarca: a.id_marca ? String(a.id_marca) : '',
            marcaOtra: esOtro(c.marcas, a.id_marca) ? a.marca ?? '' : '',
            idModelo: a.id_modelo ? String(a.id_modelo) : '',
            modeloOtro: esOtro(c.modelos, a.id_modelo) ? a.modelo ?? '' : '',
            anio: a.anio ? String(a.anio) : '',
            precio: a.precio ? String(Math.round(a.precio)) : '',
            idTransmision: a.id_transmision ? String(a.id_transmision) : '',
            kilometraje: a.kilometraje != null ? String(a.kilometraje) : '',
            recuperado: a.recuperado_por_seguro === true,
            cilindros: a.cilindros != null ? String(a.cilindros) : '',
            caballos: pot?.[1] ?? '',
            motor: a.cilindrada ?? '',
            combustible: a.tipo_combustible ?? '',
            idCarroceria: a.id_carroceria ? String(a.id_carroceria) : '',
            idColor: a.id_color ? String(a.id_color) : '',
            duenos: a.numero_propietarios_anteriores != null ? String(a.numero_propietarios_anteriores) : '',
            descripcion: pot ? desc.slice(0, pot.index) : desc,
            imperfecciones: a.descripcion_problemas ?? '',
          })
          // Las primeras van a los ángulos y el resto a fotos adicionales.
          const fotos: Foto[] = (a.fotos ?? []).slice(0, MAX_FOTOS).flatMap((ruta) => {
            const url = publicUrl(ruta)
            return url ? [{ key: 'r:' + ruta, url, ruta }] : []
          })
          setAngulos(ANGULOS.map((_, i) => fotos[i] ?? null))
          setExtras(fotos.slice(ANGULOS.length))
        }
        setCargandoEdicion(false)
      })
      .catch((e) => {
        if (vivo) {
          setError(mensajeUsuario(e))
          setCargandoEdicion(false)
        }
      })
    return () => {
      vivo = false
    }
  }, [editId])

  // Al llegar a Detalles se busca el precio de autos similares: mismo modelo y ±2 años; si hay
  // pocos, el mismo modelo de cualquier año. Con "Otros" (modelo escrito a mano) no se sugiere.
  useEffect(() => {
    const modelo = cat?.modelos.find((m) => String(m.id) === d.idModelo)
    const anio = Number(d.anio)
    if (step !== 2 || !modelo || modelo.esOtro || !Number.isInteger(anio) || anio < 1900 || anio > new Date().getFullYear() + 1) {
      setSugerencia(null)
      return
    }
    let vivo = true
    setSugerencia('cargando')
    void (async () => {
      const excluir = editId ?? undefined
      let precios = await preciosSimilares(modelo.id, anio - 2, anio + 2, excluir)
      let desde: number | undefined = anio - 2
      let hasta: number | undefined = anio + 2
      if (precios.length < 3) {
        precios = await preciosSimilares(modelo.id, undefined, undefined, excluir)
        desde = hasta = undefined
      }
      const rango = calcularRango(precios)
      if (vivo) setSugerencia(rango ? { rango, desde, hasta } : null)
      // Sin internet: no se muestra nada (no es que falten autos similares).
    })().catch(() => vivo && setSugerencia('error'))
    return () => {
      vivo = false
    }
  }, [step, d.idModelo, d.anio, cat, editId])

  // Avisa antes de cerrar o recargar la pestaña si ya se escribió algo (y no se publicó).
  useEffect(() => {
    if (!tocado || listo) return
    const onUnload = (e: BeforeUnloadEvent) => e.preventDefault()
    window.addEventListener('beforeunload', onUnload)
    return () => window.removeEventListener('beforeunload', onUnload)
  }, [tocado, listo])

  if (!cargandoSesion && !account) return <Navigate to="/login" replace state={{ from: editId ? `/publicar/${editId}` : '/publicar' }} />

  const set = <K extends keyof Datos>(k: K, v: Datos[K]) => {
    setTocado(true)
    setD((x) => ({ ...x, [k]: v, ...(k === 'idMarca' && v !== x.idMarca ? { idModelo: '', modeloOtro: '' } : {}) }))
  }

  const marcaSel = cat?.marcas.find((m) => String(m.id) === d.idMarca)
  const modeloSel = cat?.modelos.find((m) => String(m.id) === d.idModelo)
  const nombre = (lista: { id: number; nombre: string }[] | undefined, v: string) => nombreCatalogo(lista?.find((x) => String(x.id) === v)?.nombre ?? '')
  const marcaNombre = marcaSel?.esOtro ? d.marcaOtra.trim() : marcaSel?.nombre ?? ''
  const modeloNombre = modeloSel?.esOtro ? d.modeloOtro.trim() : modeloSel?.nombre ?? ''
  const titulo = [marcaNombre, modeloNombre, d.anio.trim()].filter(Boolean).join(' ')
  const fotos = [...angulos.filter((f): f is Foto => !!f), ...(motor ? [motor] : []), ...extras]
  /** "foto del tablero, foto frontal": nombres de los ángulos en minúscula para los avisos. */
  const nombresFotos = (indices: number[]) =>
    indices.map((i) => t(ANGULOS[i]).replace(/^./, (c) => c.toLocaleLowerCase())).join(', ')
  fotosActuales.current = fotos
  const lugaresExtra = MAX_FOTOS - fotos.length
  /** Ya no cabe la foto del motor: entre ángulos y adicionales se llegó a MAX_FOTOS. */
  const motorSinLugar = !motor && fotos.length >= MAX_FOTOS

  const validar = (paso: number): string | null => {
    if (paso === 0) {
      const anio = Number(d.anio)
      const max = new Date().getFullYear() + 1
      if (!marcaSel || (marcaSel.esOtro && !d.marcaOtra.trim())) return t('publicar.errBrand')
      if (!modeloSel || (modeloSel.esOtro && !d.modeloOtro.trim())) return t('publicar.errModel')
      if (!Number.isInteger(anio) || anio < 1900 || anio > max) return t('publicar.errYear')
      if (!d.idTransmision) return t('publicar.errTransmission')
      if (!d.kilometraje) return t('publicar.errMileage')
      if (Number(d.cilindros || 0) > MAX_CILINDROS) return t('publicar.errCylinders', { n: MAX_CILINDROS })
      if (!d.idCarroceria) return t('publicar.errBodyType')
      if (!d.idColor) return t('publicar.errColor')
      return null
    }
    if (paso === 1) {
      const faltan = angulos.flatMap((f, i) => (f ? [] : [i]))
      return faltan.length ? t('publicar.errRequiredPhotos', { fotos: nombresFotos(faltan) }) : null
    }
    if (paso === 2) {
      if (!d.descripcion.trim()) return t('publicar.errDescription')
      // El precio va al final del formulario, junto con la sugerencia de autos similares.
      if (!(Number(d.precio) > 0)) return t('publicar.errPrice')
      return null
    }
    return null
  }

  const irA = (paso: number) => {
    setError(null)
    setStep(paso)
    arriba.current?.scrollIntoView({ behavior: 'smooth' })
  }

  /**
   * Paso 2 → 3: deben estar las 5 fotos obligatorias, las 4 de ángulos deben parecer de un
   * auto (detectorAutos) y ninguna puede repetirse (por contenido).
   */
  const siguienteDesdeFotos = async () => {
    if (revisando) return
    setIntentoFotos(true)
    const e = validar(1)
    if (e) {
      setError(e)
      arriba.current?.scrollIntoView({ behavior: 'smooth' })
      return
    }
    setRevisando(true)
    // Primero terminan de taparse las placas.
    await Promise.all(trabajosPlacas.current)
    // Las fotos frontal, trasera y laterales deben mostrar el auto (espera las que se estén revisando).
    const autos = await Promise.all(angulos.slice(0, ANGULOS_CON_AUTO).map((f) => revisarAuto(f!)))
    if (autos.includes(false)) {
      setRevisando(false)
      setError(t('publicar.photosMustShowCar', { fotos: nombresFotos(autos.flatMap((ok, i) => (ok ? [] : [i]))) }))
      arriba.current?.scrollIntoView({ behavior: 'smooth' })
      return
    }
    // Repetidas: misma huella del original. Una foto ya publicada (al editar) contra otra se
    // compara por huella visual, porque la guardada ya se tapó y se volvió a guardar.
    const lista = fotosActuales.current
    const exactas = await Promise.all(lista.map(huellaExacta))
    const vis = await Promise.all(lista.map(huellaVis))
    const rep = new Set<string>()
    for (let i = 0; i < lista.length; i++) {
      for (let j = i + 1; j < lista.length; j++) {
        const yaPublicada = lista[i].key.startsWith('r:') || lista[j].key.startsWith('r:')
        const vi = vis[i]
        const vj = vis[j]
        if (exactas[i] === exactas[j] || (yaPublicada && vi && vj && mismaFoto(vi, vj))) rep.add(lista[i].key).add(lista[j].key)
      }
    }
    setRepetidas(rep)
    if (rep.size) {
      setRevisando(false)
      setError(t('publicar.errDuplicatePhotos'))
      arriba.current?.scrollIntoView({ behavior: 'smooth' })
      return
    }
    // Fotos que ya están en una publicación de otra cuenta (solo las nuevas). Si no se pudo
    // revisar (sin internet), no se bloquea: el admin revisa la publicación de todos modos.
    const nuevas = lista.flatMap((f, i) => (!f.key.startsWith('r:') && vis[i] ? [{ key: f.key, v: vis[i]! }] : []))
    const posiciones = await fotosYaUsadas(nuevas.map((n) => n.v)).catch(() => [] as number[])
    const ajenas = new Set(posiciones.flatMap((p) => (nuevas[p] ? [nuevas[p].key] : [])))
    setUsadas(ajenas)
    setRevisando(false)
    if (ajenas.size) {
      setError(t('publicar.errPhotosUsedElsewhere'))
      arriba.current?.scrollIntoView({ behavior: 'smooth' })
    } else irA(2)
  }

  const siguiente = () => {
    if (step === 1) return void siguienteDesdeFotos()
    const e = validar(step)
    setError(e)
    if (e) arriba.current?.scrollIntoView({ behavior: 'smooth' })
    else irA(step + 1)
  }

  /**
   * Revisa que la foto de un ángulo parezca de un auto (una sola vez por foto). Las que ya
   * estaban en Storage (al editar) ya fueron aprobadas; si el detector falla, no se bloquea.
   */
  const revisarAuto = (f: Foto): Promise<boolean> => {
    if (!f.file || f.key.startsWith('r:')) return Promise.resolve(true)
    let p = revisionesAuto.current.get(f.key)
    if (!p) {
      p = pareceAuto(f.url).then((r) => {
        const ok = r ?? true
        setRevisionAuto((m) => new Map(m).set(f.key, ok))
        return ok
      })
      revisionesAuto.current.set(f.key, p)
    }
    return p
  }

  // Los modelos se descargan al llegar al paso de fotos, para que la revisión sea inmediata.
  useEffect(() => {
    if (step !== 1) return
    precargarDetector().catch(() => undefined)
    precargarTapaPlacas().catch(() => undefined)
  }, [step])

  /**
   * Tapa las placas de una foto (menos la del tablero, que no tiene y donde se ve el
   * kilometraje). Si encontró alguna, la Foto pasa a ser la versión tapada; una foto ya
   * publicada (al editar) deja de usar su ruta y se sube de nuevo, ya censurada.
   */
  const prepararFoto = (f: Foto) => {
    setTapando((t) => new Set(t).add(f.key))
    const trabajo = taparPlacas(f.file ?? f.url).then((blob) => {
      if (blob) {
        const tapada: Foto = { key: f.key, url: URL.createObjectURL(blob), file: new File([blob], 'foto.jpg', { type: 'image/jpeg' }) }
        // Solo si la foto sigue en el formulario (no la quitaron o cambiaron mientras tanto).
        setAngulos((a) => a.map((x) => (x?.key === f.key ? tapada : x)))
        setMotor((m) => (m?.key === f.key ? tapada : m))
        setExtras((x) => x.map((y) => (y.key === f.key ? tapada : y)))
      }
      setTapando((t) => {
        const n = new Set(t)
        n.delete(f.key)
        return n
      })
    })
    trabajosPlacas.current.push(trabajo)
  }

  // Al editar, también se tapan las placas de las fotos que ya estaban publicadas: así, aunque
  // el dueño no cambie las fotos, el admin las ve censuradas al revisar la edición.
  useEffect(() => {
    if (step !== 1 || !editId || cargandoEdicion || existentesRevisadas.current) return
    existentesRevisadas.current = true
    angulos.forEach((f, i) => f?.ruta && i !== ANGULOS.length - 1 && prepararFoto(f))
    extras.forEach((f) => f.ruta && prepararFoto(f))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [step, editId, cargandoEdicion])

  /** Si en el recuadro del tablero se ve el auto por fuera, se avisa (sin bloquear: el detector puede fallar). */
  const revisarTablero = (f: Foto) => {
    void pareceAuto(f.url).then((r) => {
      if (r === true) setTableroExterior((s) => new Set(s).add(f.key))
    })
  }

  const nuevaFoto = (file: File): Foto => {
    const f: Foto = { key: 'f:' + crypto.randomUUID(), url: URL.createObjectURL(file), file }
    huellas.current.set(f.key, huella(f).catch(() => 'key:' + f.key))
    visuales.current.set(f.key, huellaVisual(file))
    return f
  }
  const huellaExacta = (f: Foto): Promise<string> => {
    let h = huellas.current.get(f.key)
    if (!h) huellas.current.set(f.key, (h = huella(f).catch(() => 'key:' + f.key)))
    return h
  }
  /** Las fotos ya publicadas (al editar) se leen de su URL la primera vez. */
  const huellaVis = (f: Foto): Promise<string | null> => {
    let h = visuales.current.get(f.key)
    if (!h) visuales.current.set(f.key, (h = huellaVisual(f.file ?? f.url)))
    return h
  }
  const esImagen = (f: File) => /^image\/(jpeg|png|webp)$/.test(f.type)

  const ponerAngulo = (file: File | undefined) => {
    if (!file || !esImagen(file)) return
    setTocado(true)
    setRepetidas(new Set())
    setUsadas(new Set())
    const foto = nuevaFoto(file)
    // La foto del motor no es un ángulo obligatorio (va en su propio recuadro) y solo si cabe.
    if (anguloElegido.current === ANGULOS.length) {
      if (motorSinLugar) return
      prepararFoto(foto)
      setMotor(foto)
      return
    }
    if (anguloElegido.current < ANGULOS_CON_AUTO) void revisarAuto(foto)
    if (anguloElegido.current !== ANGULOS.length - 1) prepararFoto(foto)
    else revisarTablero(foto)
    setAngulos((a) => a.map((f, i) => (i === anguloElegido.current ? foto : f)))
  }
  const agregarExtras = (files: File[]) => {
    const validas = files.filter(esImagen).slice(0, lugaresExtra)
    if (!validas.length) return
    setTocado(true)
    setRepetidas(new Set())
    setUsadas(new Set())
    const nuevas = validas.map(nuevaFoto)
    nuevas.forEach(prepararFoto)
    setExtras((x) => [...x, ...nuevas])
  }

  const publicar = async () => {
    if (publicando || cargandoEdicion) return
    const e = [0, 1, 2].map(validar).find(Boolean) ?? null
    if (e) return setError(e)
    setPublicando(true)
    setError(null)
    // Por si alguna foto todavía se está procesando (placas).
    await Promise.all(trabajosPlacas.current)
    try {
      const rutas: string[] = []
      // Las de fotosActuales ya tienen las placas tapadas. editar_publicacion solo acepta fotos
      // de la carpeta del dueño: las ya publicadas que están en otra (datos de prueba, links
      // externos) se suben de nuevo a la suya.
      const propia = `publicaciones/${account!.id}/`
      const fotos = fotosActuales.current
      const porSubir = fotos.filter((f) => !f.ruta?.startsWith(propia)).length
      let subidas = 0
      if (porSubir) setSubida([0, porSubir])
      for (const f of fotos) {
        if (f.ruta?.startsWith(propia)) {
          rutas.push(f.ruta)
          continue
        }
        const origen = f.file ?? (await (await fetch(f.url)).blob())
        rutas.push(await uploadPhoto((await jpegDe(origen, 1600, 0.82)).blob))
        setSubida([++subidas, porSubir])
      }
      // Fotos listas: lo que sigue es guardar la publicación ("Publicando…").
      setSubida(null)
      // "Caballos de fuerza" no tiene columna en la BD: se agrega a la descripción.
      const descripcionFinal = d.descripcion.trim() + (d.caballos ? `\n\nPotencia: ${d.caballos} hp` : '')
      const datos = {
        titulo,
        descripcion: descripcionFinal,
        precio: Number(d.precio),
        moneda: 'MXN',
        id_marca: Number(d.idMarca),
        marca_otra: marcaSel?.esOtro ? d.marcaOtra.trim() : null,
        id_modelo: Number(d.idModelo),
        modelo_otro: modeloSel?.esOtro ? d.modeloOtro.trim() : null,
        anio: Number(d.anio),
        kilometraje: Number(d.kilometraje),
        recuperado_por_seguro: d.recuperado,
        id_color: Number(d.idColor),
        id_transmision: Number(d.idTransmision),
        id_carroceria: Number(d.idCarroceria),
        numero_propietarios_anteriores: Number(d.duenos || 0),
        cilindros: d.cilindros ? Number(d.cilindros) : null,
        cilindrada: d.motor.trim() || null,
        tipo_combustible: d.combustible || null,
        descripcion_problemas: d.imperfecciones.trim(),
      }
      let id = editId
      if (id != null) await editarPublicacion(id, datos, rutas)
      else id = await crearPublicacion(datos, rutas)
      // Huellas visuales de las fotos, para que nadie más publique estas fotos. Si falla no
      // pasa nada (la publicación ya quedó guardada).
      const idGuardada = id
      // Solo si están todas: registrar reemplaza las anteriores de la publicación.
      void Promise.all(fotos.map(huellaVis))
        .then((vs) => (vs.every(Boolean) ? registrarHuellas(idGuardada, vs as string[]) : undefined))
        .catch(() => undefined)
      setListo(true)
      openModal(<Exito titulo={titulo} editando={editId != null} aprobada={editandoAprobada} editId={editId} />)
    } catch (ex) {
      setSubida(null)
      setError(mensajeUsuario(ex))
    }
    setPublicando(false)
  }

  const salir = () => {
    if (!tocado) return navigate(-1)
    openModal(
      <ConfirmModal
        title={t('publicar.exitTitle')}
        message={editId ? t('publicar.exitEditMessage') : t('publicar.exitMessage')}
        confirm={t('publicar.exitYes')}
        danger
        onConfirm={() => navigate(-1)}
      />,
    )
  }

  const confirmarPublicar = () =>
    openModal(
      <ConfirmModal
        title={editId ? t('publicar.confirmEditTitle') : t('publicar.confirmTitle')}
        message={
          editId
            ? t('publicar.confirmEditMessage')
            : t('publicar.confirmMessage')
        }
        confirm={editId ? t('publicar.confirmEditYes') : t('publicar.confirmYes')}
        onConfirm={() => {
          closeModal()
          void publicar()
        }}
      />,
    )

  const select = (k: keyof Datos, lista: { id: number; nombre: string }[] | undefined, placeholder: string, disabled = false) => (
    <select className="input" id={k} value={d[k] as string} disabled={disabled} onChange={(e) => set(k, e.target.value as never)}>
      <option value="">{placeholder}</option>
      {lista?.map((x) => (
        <option key={x.id} value={x.id}>
          {nombreCatalogo(x.nombre)}
        </option>
      ))}
    </select>
  )

  const fotoSlot = (f: Foto | null, etiqueta: string, onPick: () => void, onRemove?: () => void, debeSerAuto = false, opcional = false) => {
    const revisandoAuto =
      !!f && (tapando.has(f.key) || (debeSerAuto && !!f.file && !f.key.startsWith('r:') && !revisionAuto.has(f.key)))
    const noEsAuto = debeSerAuto && !!f && revisionAuto.get(f.key) === false
    const pareceExterior = !!f && tableroExterior.has(f.key)
    const ajena = !!f && usadas.has(f.key)
    const mal = f ? repetidas.has(f.key) || noEsAuto || ajena : intentoFotos && !opcional
    return f ? (
      <div className={`photoslot filled ${mal ? 'bad' : ''}`} onClick={onPick} role="button" tabIndex={0} aria-label={t('publicar.changePhoto', { foto: etiqueta })}>
        <img src={f.url} alt={etiqueta} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        {(revisandoAuto || noEsAuto) && (
          <span className={`autocheck ${noEsAuto ? 'bad' : ''}`}>{noEsAuto ? t('publicar.notACar') : t('publicar.checkingPhoto')}</span>
        )}
        {!revisandoAuto && !noEsAuto && pareceExterior && <span className="autocheck warn">{t('publicar.looksExterior')}</span>}
        {!revisandoAuto && !noEsAuto && ajena && <span className="autocheck bad">{t('publicar.usedElsewhere')}</span>}
        <span className="tag">{etiqueta}</span>
        {onRemove && (
          <button
            className="rm"
            aria-label={t('publicar.removePhoto')}
            onClick={(e) => {
              e.stopPropagation()
              onRemove()
            }}
          >
            <Icon name="x" size={14} />
          </button>
        )}
      </div>
    ) : (
      <button type="button" className={`photoslot ${mal ? 'bad' : ''}`} onClick={onPick}>
        <Icon name="camera" size={24} />
        {etiqueta}
      </button>
    )
  }

  const combustible = COMBUSTIBLES.find((c) => c.valor === d.combustible)
  const specs: [Clave, string][] = [
    ['publicar.brand', marcaNombre],
    ['publicar.model', modeloNombre],
    ['publicar.year', d.anio],
    ['publicar.transmission', nombre(cat?.transmisiones, d.idTransmision)],
    ['publicar.mileage', d.kilometraje ? `${entero(Number(d.kilometraje))} km` : '—'],
    ['publicar.insuranceRecovered', d.recuperado ? t('publicar.yes') : t('publicar.no')],
    ['publicar.cylinders', d.cilindros || '—'],
    ['publicar.horsepower', d.caballos || '—'],
    ['publicar.engine', d.motor.trim() || '—'],
    ['publicar.fuel', combustible ? t(combustible.label) : d.combustible || '—'],
    ['publicar.bodyType', nombre(cat?.carrocerias, d.idCarroceria)],
    ['publicar.color', nombre(cat?.colores, d.idColor)],
    ['publicar.previousOwners', d.duenos || '0'],
  ]

  return (
    <div className="wrap" style={{ maxWidth: 980 }} ref={arriba}>
      <div className="pagehead">
        <div>
          <h1>{editId ? t('publicar.editTitle') : t('publicar.title')}</h1>
          <p className="muted">{t('publicar.stepOf', { n: step + 1, paso: t(PASOS[step]) })}</p>
        </div>
        <button className="btn btn-ghost" onClick={salir}>
          <Icon name="x" size={16} />
          {t('publicar.exit')}
        </button>
      </div>

      <div className="stepper">
        {PASOS.map((p, i) => (
          <span key={p} style={{ display: 'contents' }}>
            {i > 0 && <span className="stepline" />}
            <button className={`step ${i === step ? 'on' : i < step ? 'done' : ''}`} disabled={i >= step} onClick={() => irA(i)}>
              <i>{i < step ? <Icon name="check" size={16} /> : i + 1}</i>
              <span>{t(p)}</span>
            </button>
          </span>
        ))}
      </div>

      {rechazo && (
        <div className="note" style={{ background: 'var(--error-l)', color: 'var(--error)', marginBottom: 16 }}>
          <b>{t('publicar.rejectedTitle')}</b>
          <br />
          {t('publicar.rejectedReason', { motivo: rechazoTraducido })}
        </div>
      )}
      <div style={{ marginBottom: error ? 16 : 0 }}>
        <AlertBox message={error} />
      </div>

      {cargandoEdicion || !cat ? (
        <div className="skeleton" style={{ height: 420 }} />
      ) : (
        <>
          {step === 0 && (
            <section className="card" style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div className="label">{t('publicar.sectionVehicleInfo')}</div>
              <div className="grid3">
                <Campo id="idMarca" label={t('publicar.brand')}>
                  {select('idMarca', cat.marcas, t('publicar.selectBrand'))}
                  {marcaSel?.esOtro && <input className="input" placeholder={t('publicar.enterBrand')} maxLength={60} value={d.marcaOtra} onChange={(e) => set('marcaOtra', e.target.value)} />}
                </Campo>
                <Campo id="idModelo" label={t('publicar.model')}>
                  {select('idModelo', cat.modelos.filter((m) => String(m.idMarca) === d.idMarca), marcaSel ? t('publicar.selectModel') : t('publicar.chooseBrandFirst'), !marcaSel)}
                  {modeloSel?.esOtro && <input className="input" placeholder={t('publicar.enterModel')} maxLength={60} value={d.modeloOtro} onChange={(e) => set('modeloOtro', e.target.value)} />}
                </Campo>
                <Campo id="anio" label={t('publicar.year')}>
                  <input className="input" id="anio" inputMode="numeric" placeholder={String(new Date().getFullYear())} value={d.anio} onChange={(e) => set('anio', digitos(e.target.value, 4))} />
                </Campo>
              </div>
              <div className="grid3">
                <Campo label={t('publicar.transmission')}>
                  <div className="seg" style={{ alignSelf: 'flex-start' }}>
                    {cat.transmisiones.map((t) => (
                      <button type="button" key={t.id} className={d.idTransmision === String(t.id) ? 'on' : ''} onClick={() => set('idTransmision', String(t.id))}>
                        {nombreCatalogo(t.nombre)}
                      </button>
                    ))}
                  </div>
                </Campo>
                <Campo id="kilometraje" label={t('publicar.mileage')}>
                  <input className="input" id="kilometraje" inputMode="numeric" placeholder="45,000" value={d.kilometraje ? entero(Number(d.kilometraje)) : ''} onChange={(e) => set('kilometraje', digitos(e.target.value, 7))} />
                </Campo>
              </div>
              <div className="label" style={{ marginTop: 8 }}>
                {t('publicar.sectionClassification')}
              </div>
              <div className="grid3">
                <Campo id="idCarroceria" label={t('publicar.bodyType')}>
                  {select('idCarroceria', cat.carrocerias, t('publicar.select'))}
                </Campo>
                <Campo id="idColor" label={t('publicar.color')}>
                  {select('idColor', cat.colores, t('publicar.select'))}
                </Campo>
                <Campo id="duenos" label={t('publicar.previousOwners')}>
                  <input className="input" id="duenos" inputMode="numeric" placeholder="0" value={d.duenos} onChange={(e) => set('duenos', digitos(e.target.value, 2))} />
                </Campo>
              </div>
              <div className="label" style={{ marginTop: 8 }}>
                {t('publicar.sectionAdditionalInfo')}
              </div>
              <div className="grid3">
                <Campo id="cilindros" label={t('publicar.cylinders')}>
                  <input className="input" id="cilindros" inputMode="numeric" placeholder="4" value={d.cilindros} onChange={(e) => set('cilindros', digitos(e.target.value, 2))} />
                </Campo>
                <Campo id="motor" label={t('publicar.engine')}>
                  <input className="input" id="motor" placeholder="2.0 L Turbo" maxLength={MOTOR_MAX} value={d.motor} onChange={(e) => set('motor', e.target.value)} />
                </Campo>
                <Campo id="combustible" label={t('publicar.fuel')}>
                  <select className="input" id="combustible" value={d.combustible} onChange={(e) => set('combustible', e.target.value)}>
                    <option value="">{t('publicar.select')}</option>
                    {COMBUSTIBLES.map((c) => (
                      <option key={c.valor} value={c.valor}>
                        {t(c.label)}
                      </option>
                    ))}
                  </select>
                </Campo>
              </div>
              <div className="grid3">
                <Campo id="caballos" label={t('publicar.horsepower')}>
                  <input className="input" id="caballos" inputMode="numeric" placeholder="150" value={d.caballos} onChange={(e) => set('caballos', digitos(e.target.value, 4))} />
                </Campo>
                <Campo label={t('publicar.insuranceRecovered')}>
                  <div className="seg" style={{ alignSelf: 'flex-start' }}>
                    <button type="button" className={d.recuperado ? 'on' : ''} onClick={() => set('recuperado', true)}>
                      {t('publicar.yes')}
                    </button>
                    <button type="button" className={!d.recuperado ? 'on' : ''} onClick={() => set('recuperado', false)}>
                      {t('publicar.no')}
                    </button>
                  </div>
                </Campo>
              </div>
            </section>
          )}

          {step === 1 && (
            <section className="card" style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <div className="label">{t('publicar.requiredPhotos')}</div>
                <span className={`badge ${angulos.every(Boolean) ? 'b-succ' : 'b-neu'}`}>
                  {t('publicar.photosCount', { n: fotos.length, max: MAX_FOTOS })}
                </span>
              </div>
              <p className="muted" style={{ margin: 0, fontSize: 13 }}>
                {t('publicar.platesHidden')}
              </p>
              <div className="photogrid">
                {angulos.map((f, i) => (
                  <div key={i}>
                    {fotoSlot(
                      f,
                      t(ANGULOS[i]),
                      () => {
                        anguloElegido.current = i
                        anguloInput.current?.click()
                      },
                      undefined,
                      i < ANGULOS_CON_AUTO,
                    )}
                  </div>
                ))}
                <div>
                  {fotoSlot(
                    motor,
                    `${t('publicar.photoEngine')} · ${t('publicar.recommended')}`,
                    () => {
                      if (motorSinLugar) return
                      anguloElegido.current = ANGULOS.length
                      anguloInput.current?.click()
                    },
                    motor
                      ? () => {
                          setRepetidas(new Set())
                          setUsadas(new Set())
                          setMotor(null)
                        }
                      : undefined,
                    false,
                    true,
                  )}
                </div>
              </div>
              {angulos[ANGULOS.length - 1] && tableroExterior.has(angulos[ANGULOS.length - 1]!.key) && (
                <p className="note" style={{ margin: 0, background: 'var(--warning-l)', color: 'var(--ink)' }}>
                  {t('publicar.dashLooksExterior')}
                </p>
              )}
              {!motor && (
                <p className="muted" style={{ margin: 0, fontSize: 13 }}>
                  {motorSinLugar ? t('publicar.engineNoRoom', { n: MAX_FOTOS }) : t('publicar.engineHint')}
                </p>
              )}
              <div className="label">{t('publicar.extraPhotos')}</div>
              <div className="photogrid extra">
                {extras.map((f, i) =>
                  <div key={f.key}>
                    {fotoSlot(f, t('publicar.photoN', { n: ANGULOS.length + (motor ? 1 : 0) + i + 1 }), () => undefined, () => {
                      setRepetidas(new Set())
                      setUsadas(new Set())
                      setExtras((x) => x.filter((y) => y.key !== f.key))
                    })}
                  </div>,
                )}
              </div>
              <button
                type="button"
                className={`photoslot ${arrastrando ? 'drop' : ''}`}
                style={{ aspectRatio: 'auto', height: 110 }}
                disabled={lugaresExtra <= 0}
                onClick={() => extrasInput.current?.click()}
                onDragOver={(e) => {
                  e.preventDefault()
                  setArrastrando(true)
                }}
                onDragLeave={() => setArrastrando(false)}
                onDrop={(e) => {
                  e.preventDefault()
                  setArrastrando(false)
                  agregarExtras([...e.dataTransfer.files])
                }}
              >
                <Icon name="upload" size={22} />
                {lugaresExtra > 0 ? t('publicar.addMorePhotos', { n: lugaresExtra }) : t('publicar.photosLimit', { n: MAX_FOTOS })}
                <span className="muted" style={{ fontSize: 12 }}>
                  {t('publicar.dropHint')}
                </span>
              </button>
              <input ref={anguloInput} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(e) => (ponerAngulo(e.target.files?.[0]), (e.target.value = ''))} />
              <input
                ref={extrasInput}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                hidden
                onChange={(e) => (agregarExtras([...(e.target.files ?? [])]), (e.target.value = ''))}
              />
            </section>
          )}

          {step === 2 && (
            <section className="card" style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
              <Campo id="desc" label={t('publicar.descriptionTitle')}>
                <textarea className="input" id="desc" placeholder={t('publicar.descriptionPlaceholder')} maxLength={2000} value={d.descripcion} onChange={(e) => set('descripcion', e.target.value)} />
              </Campo>
              <Campo id="imp" label={t('publicar.imperfectionsTitle')}>
                <textarea className="input" id="imp" placeholder={t('publicar.imperfectionsPlaceholder')} maxLength={1000} value={d.imperfecciones} onChange={(e) => set('imperfecciones', e.target.value)} />
              </Campo>
              <div className="note" style={{ background: 'var(--teal-light)' }}>
                {t('publicar.tipBefore')}
                <b>{t('publicar.tipBold')}</b>
                {t('publicar.tipAfter')}
              </div>
              <Campo id="precio" label={t('publicar.price')}>
                <input className="input" id="precio" inputMode="numeric" placeholder="250,000" value={d.precio ? entero(Number(d.precio)) : ''} onChange={(e) => set('precio', digitos(e.target.value, 10))} />
              </Campo>
              {sugerencia === 'cargando' ? (
                <p className="muted" style={{ margin: 0, fontSize: 13 }}>
                  {t('publicar.priceLoading')}
                </p>
              ) : sugerencia === 'error' ? null : sugerencia ? (
                <div className="note" style={{ background: 'var(--teal-light)', display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <span>
                    {t('publicar.priceSuggest', {
                      grupo:
                        sugerencia.desde != null
                          ? t('publicar.priceGroupYears', { modelo: `${marcaNombre} ${modeloNombre}`, desde: sugerencia.desde, hasta: sugerencia.hasta ?? '' })
                          : t('publicar.priceGroupAllYears', { modelo: `${marcaNombre} ${modeloNombre}` }),
                      n: sugerencia.rango.n,
                      min: formatoPrecio(sugerencia.rango.min),
                      max: formatoPrecio(sugerencia.rango.max),
                      tipico: formatoPrecio(sugerencia.rango.tipico),
                    })}
                  </span>
                  {Number(d.precio) > 0 && (
                    <b>
                      {t(
                        { abajo: 'publicar.priceBelow', dentro: 'publicar.priceWithin', arriba: 'publicar.priceAbove' }[
                          compararPrecio(Number(d.precio), sugerencia.rango)
                        ] as Clave,
                      )}
                    </b>
                  )}
                  {Number(d.precio) !== sugerencia.rango.tipico && (
                    <button type="button" className="btn btn-ghost btn-sm" style={{ alignSelf: 'flex-start' }} onClick={() => set('precio', String(sugerencia.rango.tipico))}>
                      {t('publicar.priceUseTypical', { tipico: formatoPrecio(sugerencia.rango.tipico) })}
                    </button>
                  )}
                </div>
              ) : (
                step === 2 &&
                modeloSel &&
                !modeloSel.esOtro && (
                  <p className="muted" style={{ margin: 0, fontSize: 13 }}>
                    {t('publicar.priceNoData')}
                  </p>
                )
              )}
            </section>
          )}

          {step === 3 && (
            <section className="card" style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 18 }}>
              <div className="grid2" style={{ gridTemplateColumns: 'minmax(0,1.2fr) minmax(0,1fr)', alignItems: 'start' }}>
                <CarPhoto src={fotos[0]?.url} alt={titulo} style={{ height: 260, borderRadius: 16 }} />
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <h2>
                    {marcaNombre} {modeloNombre}
                  </h2>
                  <span className="price-xl num">{formatoPrecio(Number(d.precio))}</span>
                  <span className="muted">
                    {d.anio} · {d.kilometraje ? `${entero(Number(d.kilometraje))} km` : ''} · {t('publicar.photosShort', { n: fotos.length })}
                  </span>
                  <div className="note" style={{ background: 'var(--warning-l)', marginTop: 8 }}>
                    {t('publicar.reviewNotice')}
                  </div>
                </div>
              </div>
              <div className="label">{t('publicar.specs')}</div>
              <div className="ficha">
                {specs.map(([k, v]) => (
                  <div key={k}>
                    <span>{t(k)}</span>
                    <b>{v || '—'}</b>
                  </div>
                ))}
              </div>
              <div className="label">{t('publicar.description')}</div>
              <p className="muted" style={{ whiteSpace: 'pre-line' }}>
                {d.descripcion.trim()}
                {d.caballos ? `\n\nPotencia: ${d.caballos} hp` : ''}
              </p>
              <div className="label">{t('publicar.imperfectionsBlock')}</div>
              <p className="muted" style={{ whiteSpace: 'pre-line' }}>
                {d.imperfecciones.trim() || t('publicar.noImperfections')}
              </p>
            </section>
          )}

          <div className="row" style={{ justifyContent: 'space-between', margin: '20px 0 40px' }}>
            <button className="btn btn-ghost" disabled={step === 0} style={step === 0 ? { opacity: 0.4 } : undefined} onClick={() => irA(step - 1)}>
              <Icon name="back" size={16} />
              {t('publicar.previous')}
            </button>
            {step < 3 ? (
              <button className="btn btn-primary" disabled={revisando} onClick={siguiente}>
                {revisando && <span className="spinner" aria-hidden />}
                {revisando ? t('publicar.checkingPhotos') : t('publicar.nextTo', { paso: t(step === 2 ? 'publicar.stepConfirmation' : PASOS[step + 1]) })}
              </button>
            ) : (
              <button className="btn btn-teal" disabled={publicando} onClick={confirmarPublicar}>
                {publicando && <span className="spinner" aria-hidden />}
                {subida
                  ? t('publicar.uploadingPhotos', { n: subida[0], total: subida[1] })
                  : publicando
                  ? editId
                    ? t('publicar.resubmitting')
                    : t('publicar.publishing')
                  : editId
                    ? rechazo
                      ? t('publicar.resubmit')
                      : t('publicar.sendChanges')
                    : t('publicar.publish')}
              </button>
            )}
          </div>
        </>
      )}
    </div>
  )
}

/** Aviso de éxito (SuccessOverlay de la app). */
function Exito({ titulo, editando, aprobada, editId }: { titulo: string; editando: boolean; aprobada: boolean; editId: number | null }) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const ir = (to: string) => {
    closeModal()
    navigate(to, { replace: true })
  }
  const mensaje = !editando
    ? t('publicar.successMessage', { titulo })
    : aprobada
      ? t('publicar.successEditMessage', { titulo })
      : t('publicar.successResubmitMessage', { titulo })
  return (
    <div className="modal" style={{ textAlign: 'center', alignItems: 'center' }}>
      <span className="nic" style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--success-l)', color: 'var(--success)' }}>
        <Icon name="check" size={30} />
      </span>
      <ModalHead title={editando && !aprobada ? t('publicar.successResubmitTitle') : t('publicar.successTitle')} sub={mensaje} />
      <div className="grid2" style={{ width: '100%' }}>
        <button className="btn btn-ghost" onClick={() => ir('/panel')}>
          {t('publicar.goDashboard')}
        </button>
        {editId != null ? (
          <button className="btn btn-primary" onClick={() => ir(`/vehiculo/${editId}`)}>
            {t('publicar.viewListing')}
          </button>
        ) : (
          <button className="btn btn-primary" onClick={() => ir('/')}>
            {t('common.goHome')}
          </button>
        )}
      </div>
    </div>
  )
}
