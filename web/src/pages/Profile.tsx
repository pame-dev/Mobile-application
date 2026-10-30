import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { AlertBox } from '../components/AuthParts'
import { Icon } from '../components/Icon'
import { MODAL_TITULO, ModalHead, useUi } from '../components/Ui'
import { ProfileHeader } from '../components/ProfileParts'
import { VehicleCard } from '../components/VehicleCard'
import { VolverButton } from '../components/VolverButton'
import { Traducido } from '../components/Traducido'
import { useI18n } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { enviarCurp, errorConstancia, mensajeUsuario, subirConstancia, uploadAvatar } from '../data/auth'
import { currentProfile, LLAMADA, myCars, ocultas, removeCover, sePuedeOcultar, updateProfile, uploadCover, WHATSAPP, type OwnCar, type Perfil } from '../data/seller'
import * as R from '../lib/formRules'
import { iniciales } from '../lib/format'
import { estadosMunicipios, jpegDe, type EstadoMunicipios } from '../lib/mexico'
import { useSession } from '../session/SessionContext'
import { useRecargar } from '../lib/tiempoReal'

/** Foto elegida en el diálogo: se sube hasta "Guardar" para que "Descartar" no deje nada guardado. */
interface FotoNueva {
  blob: Blob
  url: string
}

function Campo({ id, label, error, children, hint }: { id?: string; label: string; error?: string | null; children: ReactNode; hint?: string }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children}
      {error ? <span className="ferr">{error}</span> : hint ? <span className="muted text-xs">{hint}</span> : null}
    </div>
  )
}

/** Editar perfil (EditProfileDialog de la app): datos, teléfonos, foto y portada. */
function EditProfileModal({ original, onSaved }: { original: Perfil; onSaved: (p: Perfil) => void }) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  const [p, setP] = useState<Perfil>(original)
  /** Confirmación en curso; se muestra dentro de esta misma ventana para no perder lo escrito. */
  const [pregunta, setPregunta] = useState<'guardar' | 'descartar' | null>(null)
  const [avatar, setAvatar] = useState<FotoNueva | null>(null)
  const [cover, setCover] = useState<FotoNueva | null>(null)
  const [quitarPortada, setQuitarPortada] = useState(false)
  const [catalogo, setCatalogo] = useState<EstadoMunicipios[]>([])
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [intento, setIntento] = useState(false)
  const avatarInput = useRef<HTMLInputElement>(null)
  const coverInput = useRef<HTMLInputElement>(null)
  const esLote = p.accountType === 'lote'

  useEffect(() => {
    estadosMunicipios().then(setCatalogo).catch(() => setCatalogo([]))
  }, [])

  const set = <K extends keyof Perfil>(k: K, v: Perfil[K]) => setP((x) => ({ ...x, [k]: v }))
  const cambiado = JSON.stringify(p) !== JSON.stringify(original) || !!avatar || !!cover || quitarPortada
  const municipios = catalogo.find((e) => e.estado === p.estado)?.municipios ?? []

  const errores = {
    displayName: !p.displayName.trim() ? t('perfil.required') : esLote ? R.nombreLote(p.displayName) : R.nombrePersona(p.displayName),
    responsable: esLote && p.responsable.trim() ? R.nombrePersona(p.responsable) : null,
    phone: R.telefono(p.phone),
    whatsapp: R.telefono(p.whatsapp),
    estado: !p.estado ? t('perfil.required') : null,
    municipio: !p.municipio ? t('perfil.required') : null,
    calle: esLote ? (!p.calle.trim() ? t('perfil.required') : R.textoDireccion(p.calle)) : null,
    numero: esLote ? (!p.numero.trim() ? t('perfil.required') : R.numeroExterior(p.numero)) : null,
    colonia: esLote ? (!p.colonia.trim() ? t('perfil.required') : R.textoDireccion(p.colonia)) : null,
    codigoPostal: esLote ? (!p.codigoPostal.trim() ? t('perfil.required') : R.codigoPostal(p.codigoPostal)) : null,
  }
  const e = (k: keyof typeof errores) => (intento ? errores[k] : null)

  const elegir = async (file: File | undefined, tipo: 'avatar' | 'cover') => {
    if (!file) return
    try {
      const { blob } = tipo === 'avatar' ? await jpegDe(file, 800) : await jpegDe(file, 1600, 0.78)
      const nueva = { blob, url: URL.createObjectURL(blob) }
      if (tipo === 'avatar') setAvatar(nueva)
      else {
        setCover(nueva)
        setQuitarPortada(false)
      }
    } catch {
      setError(t('perfil.imageReadError'))
    }
  }

  const guardar = async () => {
    setPregunta(null)
    setGuardando(true)
    setError(null)
    try {
      const limpio: Perfil = {
        ...p,
        displayName: esLote ? R.limpiarEspacios(p.displayName) : R.capitalizarNombre(p.displayName),
        responsable: R.capitalizarNombre(p.responsable),
        phone: R.normalizarTelefono(p.phone),
        whatsapp: R.normalizarTelefono(p.whatsapp),
      }
      await updateProfile(original, limpio)
      let guardado = limpio
      if (avatar) {
        await uploadAvatar(avatar.blob)
        guardado = { ...guardado, avatarUrl: avatar.url }
      }
      if (cover) guardado = { ...guardado, coverUrl: await uploadCover(cover.blob) }
      else if (quitarPortada) {
        await removeCover()
        guardado = { ...guardado, coverUrl: null }
      }
      onSaved(guardado)
    } catch (ex) {
      setError(mensajeUsuario(ex))
      setGuardando(false)
    }
  }

  const pedirGuardar = () => {
    setIntento(true)
    const tel = R.normalizarTelefono(p.phone)
    const wa = R.normalizarTelefono(p.whatsapp)
    // Particular: nombre, estado y ciudad. Lote: además su dirección.
    const msg = Object.values(errores).some(Boolean)
      ? t(esLote ? 'perfil.errMissingLote' : 'perfil.errMissing')
      : !tel && !wa
        ? t('perfil.errPhones')
        : p.medioPrincipal === LLAMADA && !tel
          ? t('perfil.errMainPhone')
          : p.medioPrincipal === WHATSAPP && !wa
            ? t('perfil.errMainWhatsapp')
            : null
    setError(msg)
    if (!msg) setPregunta('guardar')
  }

  const cerrar = () => {
    if (!cambiado) closeModal()
    else setPregunta('descartar')
  }

  if (pregunta) {
    const guardarP = pregunta === 'guardar'
    return (
      <div className="modal">
        <ModalHead
          title={guardarP ? t('perfil.saveTitle') : t('perfil.discardTitle')}
          sub={
            guardarP
              ? t('perfil.saveSub')
              : t('perfil.discardSub')
          }
        />
        <div className="grid2">
          <button className="btn btn-ghost" onClick={() => setPregunta(null)}>
            {guardarP ? t('common.cancel') : t('perfil.keepEditing')}
          </button>
          <button className={`btn ${guardarP ? 'btn-primary' : 'btn-danger'}`} onClick={guardarP ? () => void guardar() : closeModal}>
            {guardarP ? t('perfil.yesSave') : t('perfil.yesDiscard')}
          </button>
        </div>
      </div>
    )
  }

  const input = (k: keyof Perfil & keyof typeof errores, label: string, extra: { placeholder?: string; filtro?: (v: string) => string; inputMode?: 'numeric' | 'tel' } = {}) => (
    <Campo id={`ep-${k}`} label={label} error={e(k)}>
      <input
        className={`input ${e(k) ? 'bad' : ''}`}
        id={`ep-${k}`}
        inputMode={extra.inputMode}
        placeholder={extra.placeholder}
        value={p[k] as string}
        onChange={(ev) => set(k, (extra.filtro ? extra.filtro(ev.target.value) : ev.target.value) as never)}
      />
    </Campo>
  )

  const portada = cover?.url ?? (quitarPortada ? null : p.coverUrl)
  const foto = avatar?.url ?? p.avatarUrl

  return (
    <div className="modal editmodal">
      {/* Encabezado fijo arriba; solo el cuerpo hace scroll. */}
      <div className="editmodal-head">
        <h3 id={MODAL_TITULO}>{t('perfil.editProfile')}</h3>
        <button className="iconbtn" aria-label={t('common.close')} onClick={cerrar}>
          <Icon name="x" size={18} />
        </button>
      </div>

      <div className="editmodal-body">

        <div className="cover" style={{ height: 130, borderRadius: 'var(--r-lg)' }}>
          {portada && <img src={portada} alt="" />}
          <div className="row gap-2" style={{ position: 'absolute', right: 10, bottom: 10 }}>
            {portada && (
              <button
                className="btn btn-sm coverbtn"
                style={{ position: 'static' }}
                aria-label={t('perfil.removeCover')}
                onClick={() => {
                  setCover(null)
                  setQuitarPortada(true)
                }}
              >
                <Icon name="x" size={14} />
              </button>
            )}
            <button className="btn btn-sm coverbtn" style={{ position: 'static' }} onClick={() => coverInput.current?.click()}>
              <Icon name="camera" size={16} />
              {t('perfil.editCover')}
            </button>
          </div>
        </div>
        {/* Foto en su propio bloque, debajo de la portada (sin encimarse). */}
        <div className="editmodal-avatar">
          <div className="avatarwrap">
            <div className="bigavatar" style={esLote ? { borderRadius: 'var(--r-xl)', background: 'var(--navy)' } : undefined}>
              {foto ? <img src={foto} alt="" /> : iniciales(p.displayName)}
            </div>
            <button className="camb" aria-label={t('perfil.changePhoto')} onClick={() => avatarInput.current?.click()}>
              <Icon name="camera" size={16} />
            </button>
          </div>
        </div>
        <input ref={avatarInput} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(ev) => (void elegir(ev.target.files?.[0], 'avatar'), (ev.target.value = ''))} />
        <input ref={coverInput} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(ev) => (void elegir(ev.target.files?.[0], 'cover'), (ev.target.value = ''))} />

        {input('displayName', esLote ? t('perfil.lotName') : t('perfil.fullName'), { filtro: esLote ? (v) => v.slice(0, R.LOTE_MAX) : (v) => v.replace(/[^\p{L} .'-]/gu, '').slice(0, 100) })}
        {esLote && input('responsable', t('perfil.responsible'), { filtro: (v) => v.replace(/[^\p{L} .'-]/gu, '').slice(0, 100) })}
        <Campo label={t('perfil.email')} hint={t('perfil.emailLocked')}>
          <input className="input" value={p.email} disabled readOnly />
        </Campo>
        <div className="grid2">
          <Campo id="ep-estado" label={t('perfil.state')} error={e('estado')}>
            <select className={`input ${e('estado') ? 'bad' : ''}`} id="ep-estado" value={p.estado} onChange={(ev) => setP((x) => ({ ...x, estado: ev.target.value, municipio: '' }))}>
              <option value="">{t('perfil.select')}</option>
              {/* Si el estado guardado no está en la lista (nombre antiguo), igual se muestra. */}
              {p.estado && !catalogo.some((x) => x.estado === p.estado) && <option>{p.estado}</option>}
              {catalogo.map((x) => (
                <option key={x.estado}>{x.estado}</option>
              ))}
            </select>
          </Campo>
          <Campo id="ep-municipio" label={t('perfil.city')} error={e('municipio')}>
            <select className={`input ${e('municipio') ? 'bad' : ''}`} id="ep-municipio" value={p.municipio} disabled={!p.estado} onChange={(ev) => set('municipio', ev.target.value)}>
              <option value="">{t('perfil.select')}</option>
              {p.municipio && !municipios.includes(p.municipio) && <option>{p.municipio}</option>}
              {municipios.map((m) => (
                <option key={m}>{m}</option>
              ))}
            </select>
          </Campo>
        </div>
        {esLote && (
          <>
            <div className="grid3">
              <div style={{ gridColumn: 'span 2' }}>{input('calle', t('perfil.street'), { filtro: (v) => v.slice(0, R.DIRECCION_MAX) })}</div>
              {input('numero', t('perfil.number'), { filtro: (v) => v.slice(0, R.NUMERO_MAX) })}
            </div>
            <div className="grid2">
              {input('colonia', t('perfil.colony'), { filtro: (v) => v.slice(0, R.DIRECCION_MAX) })}
              {input('codigoPostal', t('perfil.zip'), { inputMode: 'numeric', filtro: (v) => R.soloDigitos(v, 5) })}
            </div>
          </>
        )}
        <div className="grid2">
          {input('phone', t('perfil.phone'), { inputMode: 'tel', placeholder: t('perfil.tenDigits'), filtro: R.telefonoAlEscribir })}
          {input('whatsapp', t('perfil.whatsapp'), { inputMode: 'tel', placeholder: t('perfil.tenDigits'), filtro: R.telefonoAlEscribir })}
        </div>
        <Campo label={t('perfil.mainContact')}>
          <div className="row gap-2">
            {(
              [
                [WHATSAPP, 'perfil.whatsapp', 'chat'],
                [LLAMADA, 'perfil.calls', 'phone'],
              ] as const
            ).map(([v, clave, i]) => (
              <button key={v} type="button" className={`chip ${p.medioPrincipal === v ? 'on' : ''}`} aria-pressed={p.medioPrincipal === v} onClick={() => set('medioPrincipal', v)}>
                <Icon name={i} size={16} />
                {t(clave)}
              </button>
            ))}
          </div>
        </Campo>
        <Campo id="ep-bio" label={t('perfil.description')}>
          <textarea className="input" id="ep-bio" maxLength={R.DESCRIPCION_MAX} value={p.bio} onChange={(ev) => set('bio', ev.target.value)} />
          <span className="muted text-xs" style={{ alignSelf: 'flex-end' }}>
            {p.bio.length}/{R.DESCRIPCION_MAX}
          </span>
        </Campo>
        <AlertBox message={error} />
      </div>

      {/* Botones fijos abajo. */}
      <div className="grid2 editmodal-foot">
        <button className="btn btn-ghost" disabled={guardando} onClick={cerrar}>
          {t('common.cancel')}
        </button>
        <button className="btn btn-primary" disabled={guardando} onClick={pedirGuardar}>
          {guardando ? t('common.saving') : t('perfil.saveChanges')}
        </button>
      </div>
    </div>
  )
}

/**
 * Particulares sin verificar: mandar (o volver a mandar, con el motivo del rechazo) su CURP.
 * Mientras está en revisión solo se avisa.
 */
function VerificarParticular({ perfil, onSent }: { perfil: Perfil; onSent: () => void }) {
  const { t } = useI18n()
  const { toast } = useUi()
  const [curp, setCurp] = useState('')
  const [enviando, setEnviando] = useState(false)

  if (perfil.verificacion === 'pendiente') {
    return (
      <div className="note info" style={{ marginTop: 'var(--s-6)', display: 'flex', gap: 'var(--s-3)', alignItems: 'center' }}>
        <Icon name="doc" size={18} />
        {t('perfil.curpPending')}
      </div>
    )
  }
  const rechazado = perfil.verificacion === 'rechazado'
  const error = curp.length === 18 ? R.curp(curp) : null
  const enviar = async () => {
    setEnviando(true)
    try {
      await enviarCurp(curp)
      toast(t('perfil.curpSent'))
      setCurp('')
      onSent()
    } catch (e) {
      toast(mensajeUsuario(e))
    }
    setEnviando(false)
  }
  return (
    <section className="card col gap-3" style={{ marginTop: 'var(--s-6)', padding: 'var(--s-5)' }}>
      <div>
        <h2 className="card-title tight">{t('perfil.curpTitle')}</h2>
        <p className="muted text-sm">
          {t('perfil.curpDesc')}
        </p>
      </div>
      {rechazado && perfil.motivoVerificacion && <AlertBox message={t('perfil.curpRejected', { motivo: perfil.motivoVerificacion })} />}
      <div className="field" style={{ maxWidth: 320 }}>
        <input
          className={`input ${error ? 'bad' : ''}`}
          aria-label="CURP"
          placeholder="GODE561231HDFRRN09"
          value={curp}
          onChange={(e) => setCurp(R.curpAlEscribir(e.target.value))}
        />
        {error && <span className="ferr">{error}</span>}
      </div>
      <button className="btn btn-primary" style={{ alignSelf: 'flex-start' }} disabled={curp.length !== 18 || !!error || enviando} onClick={() => void enviar()}>
        {enviando ? t('perfil.verifySending') : rechazado ? t('perfil.curpResend') : t('perfil.curpSend')}
      </button>
    </section>
  )
}

/** Estado de la constancia del lote: color del texto, fondo y color del ícono (sin documento: --mid, pasa AA en claro y oscuro). */
const ESTADO_CONSTANCIA = {
  sin_documento: { color: 'var(--mid)', fondo: '#1D3550', icono: '#9CC3EA' },
  pendiente: { color: '#E8C66A', fondo: '#3A3320', icono: '#E8C66A' },
  verificado: { color: '#7FD3A4', fondo: '#1F3B2C', icono: '#7FD3A4' },
  rechazado: { color: '#F08A8A', fondo: '#3D2224', icono: '#F08A8A' },
} as const

/**
 * Fila de la Constancia de Situación Fiscal en el perfil del propio lote. Se sube solo una vez:
 * sin documento o rechazada muestra el botón (abre el selector del registro y la sube enseguida);
 * en revisión o verificada no se puede cambiar (la BD también lo impide).
 */
function ConstanciaLote({ perfil, onSent }: { perfil: Perfil; onSent: () => void }) {
  const { t } = useI18n()
  const { toast } = useUi()
  const input = useRef<HTMLInputElement>(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Sin fila pero con la insignia (cuentas.verificado), se muestra como verificada.
  const estado = perfil.verificacion === 'sin_documento' && perfil.verificado ? 'verificado' : perfil.verificacion
  const colores = ESTADO_CONSTANCIA[estado]
  const fecha = (iso: string | null) =>
    iso ? new Date(iso).toLocaleDateString(t('vehiculo.locale'), { day: '2-digit', month: '2-digit', year: 'numeric' }) : null
  const enviada = fecha(perfil.fechaVerificacion)
  const desde = fecha(perfil.fechaRevision)
  const [etiqueta, detalle] =
    estado === 'pendiente'
      ? [t('perfil.csfPending'), enviada && t('perfil.csfSentOn', { fecha: enviada })]
      : estado === 'verificado'
        ? [t('perfil.csfVerified'), desde && t('perfil.csfSince', { fecha: desde })]
        : estado === 'rechazado'
          ? [t('perfil.csfRejected'), perfil.motivoVerificacion && t('perfil.csfReason', { motivo: perfil.motivoVerificacion })]
          : [t('perfil.csfNone'), t('perfil.csfNoneDetail')]
  const puedeSubir = estado === 'sin_documento' || estado === 'rechazado'

  const elegir = async (f: File | undefined) => {
    if (!f) return
    const e = errorConstancia(f)
    if (e) return setError(t(e))
    setError(null)
    setEnviando(true)
    try {
      await subirConstancia(f)
      toast(t('perfil.verifySent'))
      onSent()
    } catch (ex) {
      // Si falló, todo queda como estaba.
      setError(mensajeUsuario(ex))
    }
    setEnviando(false)
  }

  return (
    <section className="card csfrow" style={{ marginTop: 'var(--s-6)' }}>
      <span className="csfrow-icon" style={{ background: colores.fondo, color: colores.icono }}>
        <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
          <path d="M14 3v5h5" />
          <path d="M9 14.5l2 2 4-4" />
        </svg>
      </span>
      <div className="csfrow-text">
        <b>{t('perfil.csfTitle')}</b>
        <span className="csfrow-status">
          <span style={{ color: colores.color }}>● {etiqueta}</span>
          {detalle && ` · ${detalle}`}
        </span>
        {error && <span className="ferr">{error}</span>}
      </div>
      {puedeSubir && (
        <button className="btn csfrow-btn" disabled={enviando} onClick={() => input.current?.click()}>
          {enviando ? t('perfil.verifySending') : estado === 'rechazado' ? t('perfil.csfChange') : t('perfil.csfUpload')}
        </button>
      )}
      <input
        ref={input}
        type="file"
        accept=".pdf,.jpg,.jpeg,.png"
        hidden
        onChange={(e) => {
          void elegir(e.target.files?.[0])
          e.target.value = ''
        }}
      />
    </section>
  )
}

/** Mi perfil (ProfileScreen de la app). */
export function Profile() {
  const navigate = useNavigate()
  const { account, mode, loading: cargandoSesion, reload } = useSession()
  const { openModal, closeModal, toast } = useUi()
  const { t } = useI18n()
  const [perfil, setPerfil] = useState<Perfil | null>(null)
  const [cars, setCars] = useState<OwnCar[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [verOcultas, setVerOcultas] = useState(false)
  const [ocultasIds, setOcultasIds] = useState<Set<number>>(new Set())
  /** "Editar portada" abre directo el selector de imágenes (sin pasar por Editar perfil). */
  const portadaInput = useRef<HTMLInputElement>(null)
  const [subiendoPortada, setSubiendoPortada] = useState(false)

  const cargar = useCallback(() => {
    if (!account) return
    setError(null)
    currentProfile()
      .then(setPerfil)
      .catch((e) => setError(mensajeUsuario(e)))
    myCars(account.id)
      .then(setCars)
      .catch(() => setCars([]))
    ocultas(account.id)
      .then(setOcultasIds)
      .catch(() => undefined)
  }, [account])

  useEffect(() => {
    cargar()
  }, [cargar])
  useRecargar(cargar, ['cuentas', 'publicaciones', 'propuestas_publicacion', 'fotos_publicacion', 'publicaciones_ocultas'])

  if (!cargandoSesion && !account) return <Navigate to="/login" replace state={{ from: '/perfil' }} />

  /** Comprime la imagen elegida, la sube y la pone como portada al momento. */
  const cambiarPortada = async (file: File | undefined) => {
    if (!file || !perfil) return
    setSubiendoPortada(true)
    try {
      const { blob } = await jpegDe(file, 1600, 0.78)
      const coverUrl = await uploadCover(blob)
      setPerfil((p) => (p ? { ...p, coverUrl } : p))
      toast(t('perfil.coverUpdated'))
    } catch (ex) {
      toast(ex instanceof Error && ex.message ? mensajeUsuario(ex) : t('perfil.imageReadError'))
    } finally {
      setSubiendoPortada(false)
    }
  }

  const editar = () =>
    perfil &&
    openModal(
      <EditProfileModal
        original={perfil}
        onSaved={(p) => {
          setPerfil(p)
          closeModal()
          toast(t('perfil.updated'))
          void reload() // nombre y foto de la barra de arriba
        }}
      />,
    )

  if (error) {
    return (
      <div className="wrap wrap-narrow">
        <div className="card empty empty-page">
          <p className="muted">{error}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            {t('common.retry')}
          </button>
        </div>
      </div>
    )
  }
  if (!perfil || !account) {
    return (
      <div className="wrap page-top">
        <div className="skeleton" style={{ height: 210, borderRadius: 'var(--r-xl)' }} />
        <div className="skeleton" style={{ height: 300, marginTop: 'var(--s-6)' }} />
      </div>
    )
  }

  const esLote = perfil.accountType === 'lote'
  const lista = cars ?? []
  const visibles = lista.filter((c) => !(sePuedeOcultar(c) && ocultasIds.has(c.id)))
  const escondidas = lista.filter((c) => sePuedeOcultar(c) && ocultasIds.has(c.id))
  const stats: [number, Clave][] = [
    [visibles.length, 'perfil.listings'],
    [lista.filter((c) => c.status === 'Vendido').length, 'perfil.statSold'],
    [lista.filter((c) => c.status === 'Activo').length, 'perfil.statActive'],
  ]
  const datos = [perfil.municipio && `${perfil.municipio}, ${perfil.estado}`, perfil.email].filter(Boolean).join(' · ')

  return (
    <div className="wrap page-top">
      <div style={{ marginBottom: 'var(--s-4)' }}>
        <VolverButton fallback="/" />
      </div>
      <ProfileHeader
        name={perfil.displayName}
        avatarUrl={perfil.avatarUrl}
        coverUrl={perfil.coverUrl}
        lote={esLote}
        verified={perfil.verificado}
        typeLabel={account.esAdmin ? t('nav.admin') : esLote ? t('nav.lot') : t('nav.individual')}
        info={datos}
        coverAction={
          <>
            <button className="btn btn-sm coverbtn" onClick={() => portadaInput.current?.click()} disabled={subiendoPortada}>
              <Icon name="camera" size={16} />
              {subiendoPortada ? t('perfil.coverUploading') : t('perfil.editCover')}
            </button>
            <input
              ref={portadaInput}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              hidden
              onChange={(ev) => (void cambiarPortada(ev.target.files?.[0]), (ev.target.value = ''))}
            />
          </>
        }
        actions={
          <>
            <button className="iconbtn" aria-label={t('nav.history')} onClick={() => navigate('/historial')}>
              <Icon name="history" size={18} />
            </button>
            <button className="iconbtn" aria-label={t('nav.settings')} onClick={() => navigate('/configuracion')}>
              <Icon name="settings" size={18} />
            </button>
            <button className="btn btn-primary" onClick={editar}>
              <Icon name="edit" size={16} />
              {t('perfil.editProfile')}
            </button>
          </>
        }
      />

      {/* Solo el propio lote (no el admin) ve y sube su constancia. */}
      {esLote && mode !== 'admin' && <ConstanciaLote perfil={perfil} onSent={cargar} />}
      {!esLote && mode !== 'admin' && !perfil.verificado && <VerificarParticular perfil={perfil} onSent={cargar} />}

      <div className="twocol" style={{ margin: 'var(--s-6) 0 var(--s-10)' }}>
        <div className="col gap-4">
          <div className="card" style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)' }}>
            {stats.map(([n, clave]) => (
              <div key={clave} className="statbox">
                <b className="num">{n}</b>
                <span className="muted text-xs">
                  {t(clave)}
                </span>
              </div>
            ))}
          </div>
          {perfil.bio && (
            <div className="card" style={{ padding: 'var(--s-5)' }}>
              <div className="label" style={{ marginBottom: 'var(--s-2)' }}>
                {t('perfil.description')}
              </div>
              <p className="muted">
                <Traducido texto={perfil.bio} />
              </p>
            </div>
          )}
          <button className="card row" style={{ padding: 'var(--s-5)', width: '100%', textAlign: 'left' }} onClick={() => navigate('/panel')}>
            <span className="nic" style={{ background: 'var(--navy)', color: '#fff' }}>
              <Icon name="chart" />
            </span>
            <span className="grow">
              <b className="ink">{t('nav.myDashboard')}</b>
              <br />
              <span className="muted text-xs">
                {t('perfil.dashboardDesc')}
              </span>
            </span>
            <Icon name="chev" />
          </button>
          {account.rolPanel && (
            <button className="card row" style={{ padding: 'var(--s-5)', width: '100%', textAlign: 'left' }} onClick={() => navigate('/admin')}>
              <span className="nic" style={{ background: 'var(--admin-blue)', color: '#fff' }}>
                <Icon name="shield" />
              </span>
              <span className="grow">
                <b className="ink">{t('perfil.adminPanel')}</b>
                <br />
                <span className="muted text-xs">
                  {t('perfil.adminPanelDesc')}
                </span>
              </span>
              <Icon name="chev" />
            </button>
          )}
        </div>

        <div>
          <div className="row" style={{ justifyContent: 'space-between', marginBottom: 'var(--s-4)' }}>
            <h2 className="h2-section">{t('perfil.myListings')}</h2>
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/publicar')}>
              <Icon name="plus" size={16} />
              {t('nav.publish')}
            </button>
          </div>
          {!cars ? (
            <div className="vgrid">
              {Array.from({ length: 3 }, (_, i) => (
                <div key={i} className="skeleton" style={{ height: 300 }} />
              ))}
            </div>
          ) : visibles.length === 0 ? (
            <div className="card empty">
              <p className="muted">{t('perfil.noListings')}</p>
              {/* "Primer vehículo" solo si de verdad no tiene ninguna (ni ocultas). */}
              {escondidas.length === 0 && (
                <button className="btn btn-primary" onClick={() => navigate('/publicar')}>
                  {t('perfil.publishFirst')}
                </button>
              )}
            </div>
          ) : (
            <div className="vgrid">
              {visibles.map((c) => (
                <VehicleCard key={c.id} car={c} showStatus />
              ))}
            </div>
          )}
          {escondidas.length > 0 && (
            <>
              <button className="card row" style={{ padding: 'var(--s-4) var(--s-5)', marginTop: 'var(--s-4)', width: '100%', textAlign: 'left' }} onClick={() => setVerOcultas((v) => !v)}>
                <span className="mid">
                  <Icon name="eye" />
                </span>
                <span className="grow">
                  <b className="ink">{t('perfil.hiddenListings', { n: escondidas.length })}</b>
                  <br />
                  <span className="muted text-xs">
                    {t('perfil.hiddenDescProfile')}
                  </span>
                </span>
                <Icon name="down" />
              </button>
              {verOcultas && (
                <div className="vgrid" style={{ marginTop: 'var(--s-4)' }}>
                  {escondidas.map((c) => (
                    <VehicleCard key={c.id} car={c} showStatus />
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
