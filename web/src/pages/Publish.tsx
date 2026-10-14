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
import { jpegDe } from '../lib/mexico'
import { publicUrl } from '../lib/supabase'
import { useSession } from '../session/SessionContext'
import { traducir, useI18n } from '../i18n/I18nContext'
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
  const lugaresExtra = MAX_FOTOS - fotos.length

  const validar = (paso: number): string | null => {
    if (paso === 0) {
      const anio = Number(d.anio)
      const max = new Date().getFullYear() + 1
      if (!marcaSel || (marcaSel.esOtro && !d.marcaOtra.trim())) return t('publicar.errBrand')
      if (!modeloSel || (modeloSel.esOtro && !d.modeloOtro.trim())) return t('publicar.errModel')
      if (!Number.isInteger(anio) || anio < 1900 || anio > max) return t('publicar.errYear')
      if (!(Number(d.precio) > 0)) return t('publicar.errPrice')
      if (!d.idTransmision) return t('publicar.errTransmission')
      if (!d.kilometraje) return t('publicar.errMileage')
      if (Number(d.cilindros || 0) > MAX_CILINDROS) return t('publicar.errCylinders', { n: MAX_CILINDROS })
      if (!d.idCarroceria) return t('publicar.errBodyType')
      if (!d.idColor) return t('publicar.errColor')
      return null
    }
    if (paso === 1) return angulos.some((f) => !f) ? t('publicar.errRequiredPhotos') : null
    if (paso === 2) return d.descripcion.trim() ? null : t('publicar.errDescription')
    return null
  }

  const irA = (paso: number) => {
    setError(null)
    setStep(paso)
    arriba.current?.scrollIntoView({ behavior: 'smooth' })
  }

  /** Paso 2 → 3: deben estar las 5 fotos obligatorias y ninguna puede repetirse (por contenido). */
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
      setError(t('publicar.errDuplicatePhotos'))
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

  const nuevaFoto = (file: File): Foto => ({ key: 'f:' + crypto.randomUUID(), url: URL.createObjectURL(file), file })
  const esImagen = (f: File) => /^image\/(jpeg|png|webp)$/.test(f.type)

  const ponerAngulo = (file: File | undefined) => {
    if (!file || !esImagen(file)) return
    setTocado(true)
    setRepetidas(new Set())
    setAngulos((a) => a.map((f, i) => (i === anguloElegido.current ? nuevaFoto(file) : f)))
  }
  const agregarExtras = (files: File[]) => {
    const validas = files.filter(esImagen).slice(0, lugaresExtra)
    if (!validas.length) return
    setTocado(true)
    setRepetidas(new Set())
    setExtras((x) => [...x, ...validas.map(nuevaFoto)])
  }

  const publicar = async () => {
    if (publicando || cargandoEdicion) return
    const e = [0, 1, 2].map(validar).find(Boolean) ?? null
    if (e) return setError(e)
    setPublicando(true)
    setError(null)
    try {
      const rutas: string[] = []
      for (const f of fotos) rutas.push(f.ruta ?? (await uploadPhoto((await jpegDe(f.file!, 1600, 0.82)).blob)))
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
          {x.nombre}
        </option>
      ))}
    </select>
  )

  const fotoSlot = (f: Foto | null, etiqueta: string, onPick: () => void, onRemove?: () => void) => {
    const mal = f ? repetidas.has(f.key) : intentoFotos
    return f ? (
      <div className={`photoslot filled ${mal ? 'bad' : ''}`} onClick={onPick} role="button" tabIndex={0} aria-label={t('publicar.changePhoto', { foto: etiqueta })}>
        <img src={f.url} alt={etiqueta} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
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
          {t('publicar.rejectedReason', { motivo: rechazo })}
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
                <Campo id="precio" label={t('publicar.price')}>
                  <input className="input" id="precio" inputMode="numeric" placeholder="250,000" value={d.precio ? entero(Number(d.precio)) : ''} onChange={(e) => set('precio', digitos(e.target.value, 10))} />
                </Campo>
                <Campo label={t('publicar.transmission')}>
                  <div className="seg" style={{ alignSelf: 'flex-start' }}>
                    {cat.transmisiones.map((t) => (
                      <button type="button" key={t.id} className={d.idTransmision === String(t.id) ? 'on' : ''} onClick={() => set('idTransmision', String(t.id))}>
                        {t.nombre}
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
              <div className="photogrid">
                {angulos.map((f, i) => (
                  <div key={i}>
                    {fotoSlot(f, t(ANGULOS[i]), () => {
                      anguloElegido.current = i
                      anguloInput.current?.click()
                    })}
                  </div>
                ))}
              </div>
              <div className="label">{t('publicar.extraPhotos')}</div>
              <div className="photogrid extra">
                {extras.map((f, i) =>
                  <div key={f.key}>
                    {fotoSlot(f, t('publicar.photoN', { n: ANGULOS.length + i + 1 }), () => undefined, () => {
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
                {revisando ? t('publicar.checkingPhotos') : t('publicar.nextTo', { paso: t(step === 2 ? 'publicar.stepConfirmation' : PASOS[step + 1]) })}
              </button>
            ) : (
              <button className="btn btn-teal" disabled={publicando} onClick={confirmarPublicar}>
                {publicando
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
