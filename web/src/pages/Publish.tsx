import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { AlertBox } from '../components/AuthParts'
import { CarPhoto } from '../components/CarPhoto'
import { ConfirmModal } from '../components/Confirm'
import { Icon } from '../components/Icon'
import { ModalHead, useUi } from '../components/Ui'
import { mensajeUsuario } from '../data/auth'
import { catalogs as cargarCatalogos } from '../data/api'
import { crearPublicacion, editarPublicacion, ownListing, uploadPhoto } from '../data/seller'
import type { Catalogs } from '../data/types'
import { entero, precio as formatoPrecio } from '../lib/format'
import { ANGULOS_CON_AUTO, pareceAuto, precargarDetector } from '../lib/detectorAutos'
import { jpegDe } from '../lib/mexico'
import { precargarTapaPlacas, taparPlacas } from '../lib/tapaPlacas'
import { publicUrl } from '../lib/supabase'
import { useSession } from '../session/SessionContext'

/** Máximo de fotos por publicación (editar_publicacion valida lo mismo). */
const MAX_FOTOS = 15
/** Máximo de publicaciones.cilindros (chk_publicaciones_cilindros). */
const MAX_CILINDROS = 16
/** Largo máximo de publicaciones.cilindrada (VARCHAR(30)). */
const MOTOR_MAX = 30
/** Se guardan tal cual en tipo_combustible. */
const COMBUSTIBLES = ['Gasolina', 'Diésel', 'Híbrido', 'Eléctrico']
const ANGULOS = ['Foto frontal', 'Foto trasera', 'Foto lateral izquierda', 'Foto lateral derecha', 'Foto del tablero']
const PASOS = ['Datos', 'Fotos', 'Detalles', 'Confirmar']
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

  const [cat, setCat] = useState<Catalogs | null>(null)
  const [d, setD] = useState<Datos>(VACIO)
  const [angulos, setAngulos] = useState<(Foto | null)[]>([null, null, null, null, null])
  const [extras, setExtras] = useState<Foto[]>([])
  const [step, setStep] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [cargandoEdicion, setCargandoEdicion] = useState(editId != null)
  const [rechazo, setRechazo] = useState<string | null>(null)
  const [editandoAprobada, setEditandoAprobada] = useState(false)
  const [intentoFotos, setIntentoFotos] = useState(false)
  const [repetidas, setRepetidas] = useState<Set<string>>(new Set())
  const [revisando, setRevisando] = useState(false)
  const [publicando, setPublicando] = useState(false)
  const [listo, setListo] = useState(false)
  const [tocado, setTocado] = useState(false)
  const [arrastrando, setArrastrando] = useState(false)
  const anguloInput = useRef<HTMLInputElement>(null)
  const anguloElegido = useRef(0)
  const extrasInput = useRef<HTMLInputElement>(null)
  const huellas = useRef(new Map<string, string>())
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
          setError('No encontramos la publicación que quieres corregir.')
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
  const nombre = (lista: { id: number; nombre: string }[] | undefined, v: string) => lista?.find((x) => String(x.id) === v)?.nombre ?? ''
  const marcaNombre = marcaSel?.esOtro ? d.marcaOtra.trim() : marcaSel?.nombre ?? ''
  const modeloNombre = modeloSel?.esOtro ? d.modeloOtro.trim() : modeloSel?.nombre ?? ''
  const titulo = [marcaNombre, modeloNombre, d.anio.trim()].filter(Boolean).join(' ')
  const fotos = [...angulos.filter((f): f is Foto => !!f), ...extras]
  fotosActuales.current = fotos
  const lugaresExtra = MAX_FOTOS - fotos.length

  const validar = (paso: number): string | null => {
    if (paso === 0) {
      const anio = Number(d.anio)
      const max = new Date().getFullYear() + 1
      if (!marcaSel || (marcaSel.esOtro && !d.marcaOtra.trim())) return 'Elige la marca.'
      if (!modeloSel || (modeloSel.esOtro && !d.modeloOtro.trim())) return 'Elige el modelo.'
      if (!Number.isInteger(anio) || anio < 1900 || anio > max) return 'Escribe un año válido.'
      if (!(Number(d.precio) > 0)) return 'Escribe el precio.'
      if (!d.idTransmision) return 'Elige la transmisión.'
      if (!d.kilometraje) return 'Escribe el kilometraje.'
      if (Number(d.cilindros || 0) > MAX_CILINDROS) return `Los cilindros no pueden ser más de ${MAX_CILINDROS}.`
      if (!d.idCarroceria) return 'Elige el tipo de carro.'
      if (!d.idColor) return 'Elige el color.'
      return null
    }
    if (paso === 1) return angulos.some((f) => !f) ? 'Debes subir las 5 fotos obligatorias para continuar.' : null
    if (paso === 2) return d.descripcion.trim() ? null : 'Escribe una descripción del vehículo.'
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
      setError('Las fotos frontal, trasera y laterales deben mostrar el auto. Cambia las marcadas en rojo.')
      arriba.current?.scrollIntoView({ behavior: 'smooth' })
      return
    }
    const porHuella = new Map<string, string[]>()
    for (const f of fotos) {
      let h = huellas.current.get(f.key)
      if (!h) {
        h = await huella(f).catch(() => 'key:' + f.key)
        huellas.current.set(f.key, h)
      }
      porHuella.set(h, [...(porHuella.get(h) ?? []), f.key])
    }
    const rep = new Set([...porHuella.values()].filter((k) => k.length > 1).flat())
    setRepetidas(rep)
    setRevisando(false)
    if (rep.size) {
      setError('Se detectaron fotos repetidas. Todas las fotos deben ser diferentes.')
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

  const nuevaFoto = (file: File): Foto => ({ key: 'f:' + crypto.randomUUID(), url: URL.createObjectURL(file), file })
  const esImagen = (f: File) => /^image\/(jpeg|png|webp)$/.test(f.type)

  const ponerAngulo = (file: File | undefined) => {
    if (!file || !esImagen(file)) return
    setTocado(true)
    setRepetidas(new Set())
    const foto = nuevaFoto(file)
    if (anguloElegido.current < ANGULOS_CON_AUTO) void revisarAuto(foto)
    if (anguloElegido.current !== ANGULOS.length - 1) prepararFoto(foto)
    setAngulos((a) => a.map((f, i) => (i === anguloElegido.current ? foto : f)))
  }
  const agregarExtras = (files: File[]) => {
    const validas = files.filter(esImagen).slice(0, lugaresExtra)
    if (!validas.length) return
    setTocado(true)
    setRepetidas(new Set())
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
      // Las de fotosActuales ya tienen las placas tapadas.
      for (const f of fotosActuales.current) rutas.push(f.ruta ?? (await uploadPhoto((await jpegDe(f.file!, 1600, 0.82)).blob)))
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
      if (editId != null) await editarPublicacion(editId, datos, rutas)
      else await crearPublicacion(datos, rutas)
      setListo(true)
      openModal(<Exito titulo={titulo} editando={editId != null} aprobada={editandoAprobada} editId={editId} />)
    } catch (ex) {
      setError(mensajeUsuario(ex))
    }
    setPublicando(false)
  }

  const salir = () => {
    if (!tocado) return navigate(-1)
    openModal(
      <ConfirmModal
        title="¿Salir sin publicar?"
        message={editId ? 'Si sales ahora, se perderán los cambios que hayas hecho.' : 'Si sales ahora, se borrarán los datos y las fotos que hayas agregado.'}
        confirm="Sí, salir"
        danger
        onConfirm={() => navigate(-1)}
      />,
    )
  }

  const confirmarPublicar = () =>
    openModal(
      <ConfirmModal
        title={editId ? '¿Enviar cambios?' : '¿Publicar tu vehículo?'}
        message={
          editId
            ? 'Tu publicación se enviará de nuevo a revisión con los cambios que hiciste.'
            : 'Revisa que los datos y las fotos sean correctos. Tu publicación se enviará a revisión antes de aparecer en el inicio.'
        }
        confirm={editId ? 'Sí, enviar' : 'Sí, publicar'}
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
          {x.nombre}
        </option>
      ))}
    </select>
  )

  const fotoSlot = (f: Foto | null, etiqueta: string, onPick: () => void, onRemove?: () => void, debeSerAuto = false) => {
    const revisandoAuto =
      !!f && (tapando.has(f.key) || (debeSerAuto && !!f.file && !f.key.startsWith('r:') && !revisionAuto.has(f.key)))
    const noEsAuto = debeSerAuto && !!f && revisionAuto.get(f.key) === false
    const mal = f ? repetidas.has(f.key) || noEsAuto : intentoFotos
    return f ? (
      <div className={`photoslot filled ${mal ? 'bad' : ''}`} onClick={onPick} role="button" tabIndex={0} aria-label={`Cambiar ${etiqueta}`}>
        <img src={f.url} alt={etiqueta} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        {(revisandoAuto || noEsAuto) && (
          <span className={`autocheck ${noEsAuto ? 'bad' : ''}`}>{noEsAuto ? 'No parece un auto. Elige otra foto.' : 'Revisando foto…'}</span>
        )}
        <span className="tag">{etiqueta}</span>
        {onRemove && (
          <button
            className="rm"
            aria-label="Quitar foto"
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

  const specs: [string, string][] = [
    ['Marca', marcaNombre],
    ['Modelo', modeloNombre],
    ['Año', d.anio],
    ['Transmisión', nombre(cat?.transmisiones, d.idTransmision)],
    ['Kilometraje', d.kilometraje ? `${entero(Number(d.kilometraje))} km` : '—'],
    ['Recuperado por el seguro', d.recuperado ? 'Sí' : 'No'],
    ['Cilindros', d.cilindros || '—'],
    ['Caballos de fuerza', d.caballos || '—'],
    ['Motor', d.motor.trim() || '—'],
    ['Combustible', d.combustible || '—'],
    ['Tipo de carro', nombre(cat?.carrocerias, d.idCarroceria)],
    ['Color', nombre(cat?.colores, d.idColor)],
    ['Dueños anteriores', d.duenos || '0'],
  ]

  return (
    <div className="wrap" style={{ maxWidth: 980 }} ref={arriba}>
      <div className="pagehead">
        <div>
          <h1>{editId ? 'Editar publicación' : 'Publicar vehículo'}</h1>
          <p className="muted">
            Paso {step + 1} de 4 · {PASOS[step]}
          </p>
        </div>
        <button className="btn btn-ghost" onClick={salir}>
          <Icon name="x" size={16} />
          Salir
        </button>
      </div>

      <div className="stepper">
        {PASOS.map((t, i) => (
          <span key={t} style={{ display: 'contents' }}>
            {i > 0 && <span className="stepline" />}
            <button className={`step ${i === step ? 'on' : i < step ? 'done' : ''}`} disabled={i >= step} onClick={() => irA(i)}>
              <i>{i < step ? <Icon name="check" size={16} /> : i + 1}</i>
              <span>{t}</span>
            </button>
          </span>
        ))}
      </div>

      {rechazo && (
        <div className="note" style={{ background: 'var(--error-l)', color: 'var(--error)', marginBottom: 16 }}>
          <b>Administración rechazó tu publicación</b>
          <br />
          Motivo: {rechazo}
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
              <div className="label">Información del vehículo</div>
              <div className="grid3">
                <Campo id="idMarca" label="Marca">
                  {select('idMarca', cat.marcas, 'Selecciona una marca')}
                  {marcaSel?.esOtro && <input className="input" placeholder="Escribe la marca" maxLength={60} value={d.marcaOtra} onChange={(e) => set('marcaOtra', e.target.value)} />}
                </Campo>
                <Campo id="idModelo" label="Modelo">
                  {select('idModelo', cat.modelos.filter((m) => String(m.idMarca) === d.idMarca), marcaSel ? 'Selecciona un modelo' : 'Primero elige la marca', !marcaSel)}
                  {modeloSel?.esOtro && <input className="input" placeholder="Escribe el modelo" maxLength={60} value={d.modeloOtro} onChange={(e) => set('modeloOtro', e.target.value)} />}
                </Campo>
                <Campo id="anio" label="Año">
                  <input className="input" id="anio" inputMode="numeric" placeholder={String(new Date().getFullYear())} value={d.anio} onChange={(e) => set('anio', digitos(e.target.value, 4))} />
                </Campo>
              </div>
              <div className="grid3">
                <Campo id="precio" label="Precio ($)">
                  <input className="input" id="precio" inputMode="numeric" placeholder="250,000" value={d.precio ? entero(Number(d.precio)) : ''} onChange={(e) => set('precio', digitos(e.target.value, 10))} />
                </Campo>
                <Campo label="Transmisión">
                  <div className="seg" style={{ alignSelf: 'flex-start' }}>
                    {cat.transmisiones.map((t) => (
                      <button type="button" key={t.id} className={d.idTransmision === String(t.id) ? 'on' : ''} onClick={() => set('idTransmision', String(t.id))}>
                        {t.nombre}
                      </button>
                    ))}
                  </div>
                </Campo>
                <Campo id="kilometraje" label="Kilometraje">
                  <input className="input" id="kilometraje" inputMode="numeric" placeholder="45,000" value={d.kilometraje ? entero(Number(d.kilometraje)) : ''} onChange={(e) => set('kilometraje', digitos(e.target.value, 7))} />
                </Campo>
              </div>
              <div className="label" style={{ marginTop: 8 }}>
                Clasificación
              </div>
              <div className="grid3">
                <Campo id="idCarroceria" label="Tipo de carro">
                  {select('idCarroceria', cat.carrocerias, 'Selecciona')}
                </Campo>
                <Campo id="idColor" label="Color">
                  {select('idColor', cat.colores, 'Selecciona')}
                </Campo>
                <Campo id="duenos" label="Dueños anteriores">
                  <input className="input" id="duenos" inputMode="numeric" placeholder="0" value={d.duenos} onChange={(e) => set('duenos', digitos(e.target.value, 2))} />
                </Campo>
              </div>
              <div className="label" style={{ marginTop: 8 }}>
                Información adicional
              </div>
              <div className="grid3">
                <Campo id="cilindros" label="Cilindros">
                  <input className="input" id="cilindros" inputMode="numeric" placeholder="4" value={d.cilindros} onChange={(e) => set('cilindros', digitos(e.target.value, 2))} />
                </Campo>
                <Campo id="motor" label="Motor">
                  <input className="input" id="motor" placeholder="2.0 L Turbo" maxLength={MOTOR_MAX} value={d.motor} onChange={(e) => set('motor', e.target.value)} />
                </Campo>
                <Campo id="combustible" label="Combustible">
                  <select className="input" id="combustible" value={d.combustible} onChange={(e) => set('combustible', e.target.value)}>
                    <option value="">Selecciona</option>
                    {COMBUSTIBLES.map((c) => (
                      <option key={c}>{c}</option>
                    ))}
                  </select>
                </Campo>
              </div>
              <div className="grid3">
                <Campo id="caballos" label="Caballos de fuerza">
                  <input className="input" id="caballos" inputMode="numeric" placeholder="150" value={d.caballos} onChange={(e) => set('caballos', digitos(e.target.value, 4))} />
                </Campo>
                <Campo label="Recuperado por el seguro">
                  <div className="seg" style={{ alignSelf: 'flex-start' }}>
                    <button type="button" className={d.recuperado ? 'on' : ''} onClick={() => set('recuperado', true)}>
                      Sí
                    </button>
                    <button type="button" className={!d.recuperado ? 'on' : ''} onClick={() => set('recuperado', false)}>
                      No
                    </button>
                  </div>
                </Campo>
              </div>
            </section>
          )}

          {step === 1 && (
            <section className="card" style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <div className="label">Fotos obligatorias</div>
                <span className={`badge ${angulos.every(Boolean) ? 'b-succ' : 'b-neu'}`}>
                  {fotos.length}/{MAX_FOTOS} fotos
                </span>
              </div>
              <p className="muted" style={{ margin: 0, fontSize: 13 }}>
                Por tu seguridad, las placas que aparezcan en las fotos se tapan automáticamente.
              </p>
              <div className="photogrid">
                {angulos.map((f, i) => (
                  <div key={i}>
                    {fotoSlot(
                      f,
                      ANGULOS[i],
                      () => {
                        anguloElegido.current = i
                        anguloInput.current?.click()
                      },
                      undefined,
                      i < ANGULOS_CON_AUTO,
                    )}
                  </div>
                ))}
              </div>
              <div className="label">Fotos adicionales</div>
              <div className="photogrid extra">
                {extras.map((f, i) =>
                  <div key={f.key}>
                    {fotoSlot(f, `Foto ${ANGULOS.length + i + 1}`, () => undefined, () => {
                      setRepetidas(new Set())
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
                {lugaresExtra > 0 ? `Agregar más fotos (máx. ${lugaresExtra})` : `Llegaste al máximo de ${MAX_FOTOS} fotos`}
                <span className="muted" style={{ fontSize: 12 }}>
                  Arrastra tus imágenes aquí o elige de tu computadora
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
              <Campo id="desc" label="Descripción del auto">
                <textarea className="input" id="desc" placeholder="Una buena descripción aumenta las visitas" maxLength={2000} value={d.descripcion} onChange={(e) => set('descripcion', e.target.value)} />
              </Campo>
              <Campo id="imp" label="Detalles o imperfecciones">
                <textarea className="input" id="imp" placeholder="La transparencia genera confianza en los compradores" maxLength={1000} value={d.imperfecciones} onChange={(e) => set('imperfecciones', e.target.value)} />
              </Campo>
              <div className="note" style={{ background: 'var(--teal-light)' }}>
                💡 Los anuncios con descripción completa reciben <b>40% más contactos</b> que los incompletos.
              </div>
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
                    {d.anio} · {d.kilometraje ? `${entero(Number(d.kilometraje))} km` : ''} · {fotos.length} fotos
                  </span>
                  <div className="note" style={{ background: 'var(--warning-l)', marginTop: 8 }}>
                    Tu publicación se revisará antes de aparecer en el inicio.
                  </div>
                </div>
              </div>
              <div className="label">Ficha técnica</div>
              <div className="ficha">
                {specs.map(([k, v]) => (
                  <div key={k}>
                    <span>{k}</span>
                    <b>{v || '—'}</b>
                  </div>
                ))}
              </div>
              <div className="label">Descripción</div>
              <p className="muted" style={{ whiteSpace: 'pre-line' }}>
                {d.descripcion.trim()}
                {d.caballos ? `\n\nPotencia: ${d.caballos} hp` : ''}
              </p>
              <div className="label">Detallado o imperfecciones</div>
              <p className="muted" style={{ whiteSpace: 'pre-line' }}>
                {d.imperfecciones.trim() || 'Sin imperfecciones reportadas.'}
              </p>
            </section>
          )}

          <div className="row" style={{ justifyContent: 'space-between', margin: '20px 0 40px' }}>
            <button className="btn btn-ghost" disabled={step === 0} style={step === 0 ? { opacity: 0.4 } : undefined} onClick={() => irA(step - 1)}>
              <Icon name="back" size={16} />
              Anterior
            </button>
            {step < 3 ? (
              <button className="btn btn-primary" disabled={revisando} onClick={siguiente}>
                {revisando ? 'Revisando fotos…' : `Siguiente: ${step === 2 ? 'Confirmación' : PASOS[step + 1]} →`}
              </button>
            ) : (
              <button className="btn btn-teal" disabled={publicando} onClick={confirmarPublicar}>
                {publicando
                  ? editId
                    ? 'Reenviando…'
                    : 'Publicando…'
                  : editId
                    ? rechazo
                      ? 'Reenviar a revisión'
                      : 'Enviar cambios a revisión'
                    : 'Publicar'}
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
  const navigate = useNavigate()
  const ir = (to: string) => {
    closeModal()
    navigate(to, { replace: true })
  }
  const mensaje = !editando
    ? `Tu ${titulo} se envió a revisión. Aparecerá para los compradores en cuanto un administrador lo apruebe.`
    : aprobada
      ? `Los cambios de tu ${titulo} se enviaron a revisión. Mientras tanto, los compradores siguen viendo la versión anterior.`
      : `Tu ${titulo} se envió de nuevo a revisión. Aparecerá para los compradores en cuanto un administrador la apruebe.`
  return (
    <div className="modal" style={{ textAlign: 'center', alignItems: 'center' }}>
      <span className="nic" style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--success-l)', color: 'var(--success)' }}>
        <Icon name="check" size={30} />
      </span>
      <ModalHead title={editando && !aprobada ? '¡Reenviada!' : '¡Publicado!'} sub={mensaje} />
      <div className="grid2" style={{ width: '100%' }}>
        <button className="btn btn-ghost" onClick={() => ir('/panel')}>
          Ver mi panel
        </button>
        {editId != null ? (
          <button className="btn btn-primary" onClick={() => ir(`/vehiculo/${editId}`)}>
            Ver publicación
          </button>
        ) : (
          <button className="btn btn-primary" onClick={() => ir('/')}>
            Ir al inicio
          </button>
        )}
      </div>
    </div>
  )
}
