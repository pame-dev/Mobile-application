import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { AlertBox } from '../components/AuthParts'
import { Icon } from '../components/Icon'
import { ModalHead, useUi } from '../components/Ui'
import { VehicleCard } from '../components/VehicleCard'
import { mensajeUsuario, uploadAvatar } from '../data/auth'
import { currentProfile, LLAMADA, myCars, ocultas, removeCover, sePuedeOcultar, updateProfile, uploadCover, WHATSAPP, type OwnCar, type Perfil } from '../data/seller'
import * as R from '../lib/formRules'
import { iniciales } from '../lib/format'
import { estadosMunicipios, jpegDe, type EstadoMunicipios } from '../lib/mexico'
import { useSession } from '../session/SessionContext'

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
      {error ? <span className="ferr">{error}</span> : hint ? <span className="muted" style={{ fontSize: 12 }}>{hint}</span> : null}
    </div>
  )
}

/** Editar perfil (EditProfileDialog de la app): datos, teléfonos, foto y portada. */
function EditProfileModal({ original, onSaved }: { original: Perfil; onSaved: (p: Perfil) => void }) {
  const { closeModal } = useUi()
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
    displayName: !p.displayName.trim() ? 'Campo obligatorio' : esLote ? R.nombreLote(p.displayName) : R.nombrePersona(p.displayName),
    responsable: esLote && p.responsable.trim() ? R.nombrePersona(p.responsable) : null,
    phone: R.telefono(p.phone),
    whatsapp: R.telefono(p.whatsapp),
    estado: !p.estado ? 'Campo obligatorio' : null,
    municipio: !p.municipio ? 'Campo obligatorio' : null,
    calle: esLote ? (!p.calle.trim() ? 'Campo obligatorio' : R.textoDireccion(p.calle)) : null,
    numero: esLote ? (!p.numero.trim() ? 'Campo obligatorio' : R.numeroExterior(p.numero)) : null,
    colonia: esLote ? (!p.colonia.trim() ? 'Campo obligatorio' : R.textoDireccion(p.colonia)) : null,
    codigoPostal: esLote ? (!p.codigoPostal.trim() ? 'Campo obligatorio' : R.codigoPostal(p.codigoPostal)) : null,
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
      setError('No se pudo leer la imagen.')
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
    const msg = Object.values(errores).some(Boolean)
      ? 'Completa nombre, ciudad y estado (y la dirección del lote).'
      : !tel && !wa
        ? 'Escribe al menos un teléfono o un WhatsApp.'
        : p.medioPrincipal === LLAMADA && !tel
          ? 'Para que la llamada sea el principal, escribe un teléfono.'
          : p.medioPrincipal === WHATSAPP && !wa
            ? 'Para que WhatsApp sea el principal, escribe un número de WhatsApp.'
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
          title={guardarP ? '¿Guardar cambios?' : '¿Descartar cambios?'}
          sub={
            guardarP
              ? 'Se actualizará la información de tu perfil con los datos que ingresaste.'
              : 'Has realizado cambios en tu perfil. Si sales ahora, estos cambios se perderán. ¿Seguro que deseas descartar los cambios hechos?'
          }
        />
        <div className="grid2">
          <button className="btn btn-ghost" onClick={() => setPregunta(null)}>
            {guardarP ? 'Cancelar' : 'No, continuar editando'}
          </button>
          <button className={`btn ${guardarP ? 'btn-primary' : 'btn-danger'}`} onClick={guardarP ? () => void guardar() : closeModal}>
            {guardarP ? 'Sí, guardar' : 'Sí, descartar'}
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
    <div className="modal" style={{ maxWidth: 640 }}>
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <h3>Editar perfil</h3>
        <button className="iconbtn" aria-label="Cerrar" onClick={cerrar}>
          <Icon name="x" size={18} />
        </button>
      </div>

      <div className="cover" style={{ height: 130, borderRadius: 16 }}>
        {portada && <img src={portada} alt="" />}
        <div className="row" style={{ position: 'absolute', right: 10, bottom: 10, gap: 6 }}>
          {portada && (
            <button
              className="btn btn-sm coverbtn"
              style={{ position: 'static' }}
              aria-label="Quitar portada"
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
            Editar portada
          </button>
        </div>
      </div>
      <div className="row" style={{ marginTop: -48, paddingLeft: 16 }}>
        <div className="avatarwrap">
          <div className="bigavatar" style={{ width: 96, height: 96, fontSize: 32, ...(esLote ? { borderRadius: 24, background: 'var(--navy)' } : {}) }}>
            {foto ? <img src={foto} alt="" /> : iniciales(p.displayName)}
          </div>
          <button className="camb" aria-label="Cambiar foto" onClick={() => avatarInput.current?.click()}>
            <Icon name="camera" size={16} />
          </button>
        </div>
      </div>
      <input ref={avatarInput} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(ev) => (void elegir(ev.target.files?.[0], 'avatar'), (ev.target.value = ''))} />
      <input ref={coverInput} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(ev) => (void elegir(ev.target.files?.[0], 'cover'), (ev.target.value = ''))} />

      {input('displayName', esLote ? 'Nombre del lote' : 'Nombre completo', { filtro: esLote ? (v) => v.slice(0, R.LOTE_MAX) : (v) => v.replace(/[^\p{L} .'-]/gu, '').slice(0, 100) })}
      {esLote && input('responsable', 'Responsable', { filtro: (v) => v.replace(/[^\p{L} .'-]/gu, '').slice(0, 100) })}
      <Campo label="Correo electrónico" hint="El correo no se puede cambiar.">
        <input className="input" value={p.email} disabled readOnly />
      </Campo>
      <div className="grid2">
        <Campo id="ep-estado" label="Estado" error={e('estado')}>
          <select className={`input ${e('estado') ? 'bad' : ''}`} id="ep-estado" value={p.estado} onChange={(ev) => setP((x) => ({ ...x, estado: ev.target.value, municipio: '' }))}>
            <option value="">Selecciona</option>
            {/* Si el estado guardado no está en la lista (nombre antiguo), igual se muestra. */}
            {p.estado && !catalogo.some((x) => x.estado === p.estado) && <option>{p.estado}</option>}
            {catalogo.map((x) => (
              <option key={x.estado}>{x.estado}</option>
            ))}
          </select>
        </Campo>
        <Campo id="ep-municipio" label="Ciudad" error={e('municipio')}>
          <select className={`input ${e('municipio') ? 'bad' : ''}`} id="ep-municipio" value={p.municipio} disabled={!p.estado} onChange={(ev) => set('municipio', ev.target.value)}>
            <option value="">Selecciona</option>
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
            <div style={{ gridColumn: 'span 2' }}>{input('calle', 'Calle', { filtro: (v) => v.slice(0, R.DIRECCION_MAX) })}</div>
            {input('numero', 'Número', { filtro: (v) => v.slice(0, R.NUMERO_MAX) })}
          </div>
          <div className="grid2">
            {input('colonia', 'Colonia', { filtro: (v) => v.slice(0, R.DIRECCION_MAX) })}
            {input('codigoPostal', 'C.P.', { inputMode: 'numeric', filtro: (v) => R.soloDigitos(v, 5) })}
          </div>
        </>
      )}
      <div className="grid2">
        {input('phone', 'Teléfono', { inputMode: 'tel', placeholder: '10 dígitos', filtro: R.telefonoAlEscribir })}
        {input('whatsapp', 'WhatsApp', { inputMode: 'tel', placeholder: '10 dígitos', filtro: R.telefonoAlEscribir })}
      </div>
      <Campo label="Medio de contacto principal">
        <div className="row" style={{ gap: 8 }}>
          {(
            [
              [WHATSAPP, 'WhatsApp', 'chat'],
              [LLAMADA, 'Llamadas', 'phone'],
            ] as const
          ).map(([v, t, i]) => (
            <button key={v} type="button" className={`chip ${p.medioPrincipal === v ? 'on' : ''}`} onClick={() => set('medioPrincipal', v)}>
              <Icon name={i} size={16} />
              {t}
            </button>
          ))}
        </div>
      </Campo>
      <Campo id="ep-bio" label="Descripción">
        <textarea className="input" id="ep-bio" maxLength={R.DESCRIPCION_MAX} value={p.bio} onChange={(ev) => set('bio', ev.target.value)} />
        <span className="muted" style={{ fontSize: 12, alignSelf: 'flex-end' }}>
          {p.bio.length}/{R.DESCRIPCION_MAX}
        </span>
      </Campo>
      <AlertBox message={error} />
      <div className="grid2">
        <button className="btn btn-ghost" disabled={guardando} onClick={cerrar}>
          Cancelar
        </button>
        <button className="btn btn-primary" disabled={guardando} onClick={pedirGuardar}>
          {guardando ? 'Guardando…' : 'Guardar cambios'}
        </button>
      </div>
    </div>
  )
}

/** Mi perfil (ProfileScreen de la app). */
export function Profile() {
  const navigate = useNavigate()
  const { account, mode, loading: cargandoSesion, reload } = useSession()
  const { openModal, closeModal, toast } = useUi()
  const [perfil, setPerfil] = useState<Perfil | null>(null)
  const [cars, setCars] = useState<OwnCar[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [verOcultas, setVerOcultas] = useState(false)

  const cargar = useCallback(() => {
    if (!account) return
    setError(null)
    currentProfile()
      .then(setPerfil)
      .catch((e) => setError(mensajeUsuario(e)))
    myCars(account.id)
      .then(setCars)
      .catch(() => setCars([]))
  }, [account])

  useEffect(() => {
    cargar()
  }, [cargar])

  if (!cargandoSesion && !account) return <Navigate to="/login" replace state={{ from: '/perfil' }} />

  const editar = () =>
    perfil &&
    openModal(
      <EditProfileModal
        original={perfil}
        onSaved={(p) => {
          setPerfil(p)
          closeModal()
          toast('Perfil actualizado')
          void reload() // nombre y foto de la barra de arriba
        }}
      />,
    )

  if (error) {
    return (
      <div className="wrap" style={{ maxWidth: 820 }}>
        <div className="card empty" style={{ marginTop: 40 }}>
          <p className="muted">{error}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            Reintentar
          </button>
        </div>
      </div>
    )
  }
  if (!perfil || !account) {
    return (
      <div className="wrap" style={{ paddingTop: 24 }}>
        <div className="skeleton" style={{ height: 210, borderRadius: 22 }} />
        <div className="skeleton" style={{ height: 300, marginTop: 24 }} />
      </div>
    )
  }

  const esLote = perfil.accountType === 'lote'
  const ocultasIds = ocultas(account.id)
  const lista = cars ?? []
  const visibles = lista.filter((c) => !(sePuedeOcultar(c) && ocultasIds.has(c.id)))
  const escondidas = lista.filter((c) => sePuedeOcultar(c) && ocultasIds.has(c.id))
  const stats: [number, string][] = [
    [visibles.length, 'Publicaciones'],
    [lista.filter((c) => c.status === 'Vendido').length, 'Vendidos'],
    [lista.filter((c) => c.status === 'Activo').length, 'Activas'],
  ]
  const datos = [perfil.municipio && `${perfil.municipio}, ${perfil.estado}`, perfil.email].filter(Boolean).join(' · ')

  return (
    <div className="wrap" style={{ paddingTop: 24 }}>
      <div className="cover">
        {perfil.coverUrl && <img src={perfil.coverUrl} alt="" />}
        <button className="btn btn-sm coverbtn" onClick={editar}>
          <Icon name="camera" size={16} />
          Editar portada
        </button>
      </div>
      <div className="profilehead">
        {perfil.avatarUrl ? (
          <button
            className="bigavatar"
            style={esLote ? { borderRadius: 28, background: 'var(--navy)' } : undefined}
            aria-label="Ver foto de perfil"
            onClick={() => openModal(<img className="lightbox" src={perfil.avatarUrl!} alt={perfil.displayName} onClick={closeModal} />)}
          >
            <img src={perfil.avatarUrl} alt="" />
          </button>
        ) : (
          <div className="bigavatar" style={esLote ? { borderRadius: 28, background: 'var(--navy)' } : undefined}>
            {iniciales(perfil.displayName)}
          </div>
        )}
        <div className="grow" style={{ paddingBottom: 10, minWidth: 220 }}>
          <div className="row" style={{ flexWrap: 'wrap' }}>
            <h1 style={{ fontSize: 28 }}>{perfil.displayName}</h1>
            <span className="badge b-info">{mode === 'admin' ? 'Administrador' : esLote ? 'Lote' : 'Particular'}</span>
          </div>
          <p className="muted">{datos}</p>
        </div>
        <div className="row" style={{ paddingBottom: 12 }}>
          <button className="iconbtn" aria-label="Historial" onClick={() => navigate('/historial')}>
            <Icon name="history" size={18} />
          </button>
          <button className="iconbtn" aria-label="Configuración" onClick={() => navigate('/configuracion')}>
            <Icon name="settings" size={18} />
          </button>
          <button className="btn btn-primary" onClick={editar}>
            <Icon name="edit" size={16} />
            Editar perfil
          </button>
        </div>
      </div>

      <div className="twocol" style={{ margin: '26px 0 40px' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="card" style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)' }}>
            {stats.map(([n, t]) => (
              <div key={t} className="statbox">
                <b className="num">{n}</b>
                <span className="muted" style={{ fontSize: 12 }}>
                  {t}
                </span>
              </div>
            ))}
          </div>
          {perfil.bio && (
            <div className="card" style={{ padding: 20 }}>
              <div className="label" style={{ marginBottom: 8 }}>
                Descripción
              </div>
              <p className="muted">{perfil.bio}</p>
            </div>
          )}
          <button className="card row" style={{ padding: 18, width: '100%', textAlign: 'left' }} onClick={() => navigate('/panel')}>
            <span className="nic" style={{ background: 'var(--navy)', color: '#fff' }}>
              <Icon name="chart" />
            </span>
            <span className="grow">
              <b style={{ color: 'var(--ink)' }}>Mi panel</b>
              <br />
              <span className="muted" style={{ fontSize: 12.5 }}>
                Vistas, contactos y favoritos de tus publicaciones
              </span>
            </span>
            <Icon name="chev" />
          </button>
          {mode === 'admin' && (
            <button className="card row" style={{ padding: 18, width: '100%', textAlign: 'left' }} onClick={() => navigate('/admin')}>
              <span className="nic" style={{ background: 'var(--admin-blue)', color: '#fff' }}>
                <Icon name="shield" />
              </span>
              <span className="grow">
                <b style={{ color: 'var(--ink)' }}>Panel admin</b>
                <br />
                <span className="muted" style={{ fontSize: 12.5 }}>
                  Revisa publicaciones, reportes y cuentas
                </span>
              </span>
              <Icon name="chev" />
            </button>
          )}
        </div>

        <div>
          <div className="row" style={{ justifyContent: 'space-between', marginBottom: 16 }}>
            <h2 style={{ fontSize: 22 }}>Mis publicaciones</h2>
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/publicar')}>
              <Icon name="plus" size={16} />
              Publicar
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
              <p className="muted">Aún no tienes publicaciones.</p>
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
              <button className="card row" style={{ padding: '16px 18px', marginTop: 18, width: '100%', textAlign: 'left' }} onClick={() => setVerOcultas((v) => !v)}>
                <span className="mid">
                  <Icon name="eye" />
                </span>
                <span className="grow">
                  <b style={{ color: 'var(--ink)' }}>Publicaciones ocultas ({escondidas.length})</b>
                  <br />
                  <span className="muted" style={{ fontSize: 12.5 }}>
                    No aparecen en Mis publicaciones. Puedes volver a mostrarlas desde Mi panel.
                  </span>
                </span>
                <Icon name="down" />
              </button>
              {verOcultas && (
                <div className="vgrid" style={{ marginTop: 16 }}>
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
