import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { AlertBox, AuthShell, BackButton, PasswordInput, PasswordRulesList } from '../../components/AuthParts'
import { Icon, type IconName } from '../../components/Icon'
import { Brand } from '../../components/TopNav'
import { useUi } from '../../components/Ui'
import { EmailNotVerifiedError, holdAvatarUntilVerified, MSG_EMAIL_EXISTS, mensajeUsuario, signUp, telefonoEnUso, uploadAvatar } from '../../data/auth'
import * as R from '../../lib/formRules'
import { estadosMunicipios, jpegDe, type EstadoMunicipios } from '../../lib/mexico'
import { useSession } from '../../session/SessionContext'

type Tipo = 'particular' | 'lote'

/** Registro: tipo de cliente (RegisterTypeScreen de la app). */
export function RegisterType() {
  const navigate = useNavigate()
  const { account } = useSession()
  const [tipo, setTipo] = useState<Tipo>('particular')
  if (account) return <Navigate to="/" replace />

  const opciones: [Tipo, string, string, IconName, string | undefined][] = [
    ['particular', 'Particular', 'Compra y vende vehículos como persona particular.', 'user', undefined],
    ['lote', 'Lote', 'Publica y administra vehículos de tu lote o agencia.', 'building', 'var(--teal)'],
  ]
  return (
    <AuthShell title="Únete a Karsy." sub="Crea tu cuenta gratis como particular o registra tu lote para publicar todo tu inventario." photo={11}>
      <BackButton onClick={() => navigate('/')} />
      <h2>Registro</h2>
      <p className="muted">Selecciona qué tipo de cliente eres</p>
      {opciones.map(([v, t, d, i, bg]) => (
        <button key={v} className={`typecard ${tipo === v ? 'on' : ''}`} aria-pressed={tipo === v} onClick={() => setTipo(v)}>
          <span className="nic" style={bg ? { background: bg } : undefined}>
            <Icon name={i} size={26} />
          </span>
          <span className="grow">
            <b style={{ fontFamily: 'var(--f-display)', fontSize: 18, color: 'var(--ink)' }}>{t}</b>
            <br />
            <span className="muted">{d}</span>
          </span>
          <Icon name="chev" />
        </button>
      ))}
      <button className="btn btn-primary btn-block" style={{ height: 50 }} onClick={() => navigate(`/registro/${tipo}`)}>
        Continuar
      </button>
      <p className="muted" style={{ textAlign: 'center', fontSize: 13 }}>
        Podrás completar tu perfil después.
      </p>
      <p className="muted" style={{ textAlign: 'center' }}>
        ¿Ya tienes una cuenta?{' '}
        <button className="linkbtn" onClick={() => navigate('/login')}>
          Inicia sesión aquí
        </button>
      </p>
    </AuthShell>
  )
}

// ── Formulario ─────────────────────────────────────────────────────────────

const LLAMADA = 'llamada'
const WHATSAPP = 'whatsapp'

interface Form {
  nombre: string
  apellido: string
  correo: string
  estado: string
  ciudad: string
  telefono: string
  mismoWhatsapp: boolean
  whatsapp: string
  medio: string | null
  nombreLote: string
  calle: string
  numero: string
  colonia: string
  codigoPostal: string
  descripcion: string
  password: string
  confirm: string
  terminos: boolean
}

const VACIO: Form = {
  nombre: '', apellido: '', correo: '', estado: '', ciudad: '', telefono: '', mismoWhatsapp: true, whatsapp: '',
  medio: null, nombreLote: '', calle: '', numero: '', colonia: '', codigoPostal: '', descripcion: '', password: '',
  confirm: '', terminos: false,
}

const REQUERIDO = 'Campo obligatorio'

/** Orden de los campos en pantalla (para llevar la vista al primer error). */
const CAMPOS_PARTICULAR = ['nombre', 'apellido', 'correo', 'estado', 'ciudad', 'telefono', 'whatsapp', 'medio', 'password']
const CAMPOS_LOTE = ['nombre', 'apellido', 'correo', 'telefono', 'whatsapp', 'medio', 'nombreLote', 'calle', 'numero', 'colonia', 'codigoPostal', 'estado', 'ciudad', 'password']

/** Error del campo con su valor actual, o null si está bien (validar() de RegisterViewModel). */
function validar(f: Form, campo: string, correoRegistrado: string | null): string | null {
  const obligatorio = (v: string, regla: (x: string) => string | null) => (!v.trim() ? REQUERIDO : regla(v))
  switch (campo) {
    case 'nombre':
      return obligatorio(f.nombre, R.nombrePersona)
    case 'apellido':
      return obligatorio(f.apellido, R.nombrePersona)
    case 'correo':
      return obligatorio(f.correo, R.correo) ?? (correoRegistrado && R.limpiarCorreo(f.correo) === correoRegistrado ? MSG_EMAIL_EXISTS : null)
    case 'telefono':
      return obligatorio(f.telefono, R.telefono)
    // Si no usa el mismo número, el WhatsApp es opcional (vacío = no tiene WhatsApp).
    case 'whatsapp':
      if (f.mismoWhatsapp) return null
      if (f.medio === WHATSAPP && !f.whatsapp.trim()) return 'Escribe tu WhatsApp o elige Llamadas como método principal.'
      return R.telefono(f.whatsapp)
    case 'medio':
      return f.medio ? null : 'Elige cómo prefieres que te contacten.'
    case 'estado':
      return f.estado ? null : REQUERIDO
    case 'ciudad':
      return f.ciudad ? null : REQUERIDO
    case 'password':
      return !f.password ? REQUERIDO : R.passwordError(f.password, f.correo)
    case 'nombreLote':
      return obligatorio(f.nombreLote, R.nombreLote)
    case 'calle':
      return obligatorio(f.calle, R.textoDireccion)
    case 'numero':
      return obligatorio(f.numero, R.numeroExterior)
    case 'colonia':
      return obligatorio(f.colonia, R.textoDireccion)
    case 'codigoPostal':
      return obligatorio(f.codigoPostal, R.codigoPostal)
  }
  return null
}

/** Campo con etiqueta y su mensaje de error debajo. */
function Field({ id, label, error, children, span }: { id: string; label: string; error?: string | null; children: ReactNode; span?: number }) {
  return (
    <div className="field" data-field={id} style={span ? { gridColumn: `span ${span}` } : undefined}>
      <label htmlFor={id}>{label}</label>
      {children}
      {error && <span className="ferr">{error}</span>}
    </div>
  )
}

/** Registro particular o de lote (RegisterParticularScreen / RegisterLoteScreen + RegisterViewModel). */
export function RegisterForm({ tipo }: { tipo: Tipo }) {
  const esLote = tipo === 'lote'
  const navigate = useNavigate()
  const { account, reload } = useSession()
  const { toast } = useUi()
  const [f, setF] = useState<Form>(VACIO)
  const [touched, setTouched] = useState<Set<string>>(new Set())
  const [submitted, setSubmitted] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [correoRegistrado, setCorreoRegistrado] = useState<string | null>(null)
  const [telefonoAviso, setTelefonoAviso] = useState<string | null>(null)
  const [catalogo, setCatalogo] = useState<EstadoMunicipios[]>([])
  const [logo, setLogo] = useState<{ blob: Blob; url: string } | null>(null)
  const [logoError, setLogoError] = useState<string | null>(null)
  const logoInput = useRef<HTMLInputElement>(null)
  const [scrollPedido, setScrollPedido] = useState(0)
  /** Teléfono actual, para descartar la consulta de "ya registrado" si cambió mientras tanto. */
  const telefonoActual = useRef(f.telefono)
  telefonoActual.current = f.telefono

  useEffect(() => {
    estadosMunicipios().then(setCatalogo).catch(() => setCatalogo([]))
  }, [])
  useEffect(
    () => () => {
      if (logo) URL.revokeObjectURL(logo.url)
    },
    [logo],
  )

  const campos = esLote ? CAMPOS_LOTE : CAMPOS_PARTICULAR
  const err = (campo: string) => (submitted || touched.has(campo) ? validar(f, campo, correoRegistrado) : null)
  const correoYaRegistrado = correoRegistrado != null && R.limpiarCorreo(f.correo) === correoRegistrado
  const correoSugerido = submitted || touched.has('correo') ? R.sugerenciaCorreo(f.correo) : null
  const cpAviso = submitted || touched.has('codigoPostal') ? R.avisoCodigoPostalEstado(f.codigoPostal.trim(), f.estado) : null
  const mismatch = f.confirm.length > 0 && f.password !== f.confirm
  const municipios = catalogo.find((e) => e.estado === f.estado)?.municipios ?? []

  // Después de un envío fallido, la vista baja al primer campo con error.
  useEffect(() => {
    if (!scrollPedido) return
    const conError = [
      ...campos.filter((c) => validar(f, c, correoRegistrado) != null),
      ...(f.password && f.password !== f.confirm ? ['confirm'] : []),
      ...(!f.terminos ? ['terminos'] : []),
    ]
    const el = conError.length ? document.querySelector(`[data-field="${conError[0]}"]`) : null
    ;(el ?? document.querySelector('.alertbox'))?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scrollPedido])

  if (account && !loading) return <Navigate to="/" replace />

  const set = <K extends keyof Form>(k: K, v: Form[K]) => {
    if (k === 'telefono') setTelefonoAviso(null)
    setF((x) => ({ ...x, [k]: v }))
  }

  /** El usuario salió del campo: desde ahora se muestra su error (y se actualiza al corregirlo). */
  const touch = (campo: string) => {
    setTouched((t) => (t.has(campo) ? t : new Set(t).add(campo)))
    // "juan pérez" → "Juan Pérez" al salir del campo.
    if (campo === 'nombre' || campo === 'apellido') setF((x) => ({ ...x, [campo]: R.capitalizarNombre(x[campo]) }))
    if (campo === 'telefono') void revisarTelefono(f.telefono)
  }

  const revisarTelefono = async (valor: string) => {
    setTelefonoAviso(null)
    if (R.telefono(valor) != null || !valor.trim()) return
    const numero = R.normalizarTelefono(valor)
    const enUso = await telefonoEnUso(numero).catch(() => false)
    // Solo si sigue siendo el mismo número (pudo cambiar mientras se consultaba).
    if (enUso && R.normalizarTelefono(telefonoActual.current) === numero) setTelefonoAviso('Este teléfono ya está registrado en otra cuenta.')
  }

  const elegirLogo = async (file: File | undefined) => {
    if (!file) return
    try {
      const { blob, ladoMenor } = await jpegDe(file, 800)
      if (ladoMenor < R.LOGO_MIN_PX) {
        setLogoError(`La imagen es muy pequeña (mínimo ${R.LOGO_MIN_PX} × ${R.LOGO_MIN_PX} px); se vería borrosa.`)
        return
      }
      setLogo({ blob, url: URL.createObjectURL(blob) })
      setLogoError(null)
    } catch {
      setLogoError('No pudimos leer esa imagen. Elige otra.')
    }
  }

  const crear = async () => {
    if (loading) return
    setSubmitted(true)
    const limpio = { ...f, nombre: R.capitalizarNombre(f.nombre), apellido: R.capitalizarNombre(f.apellido) }
    setF(limpio)
    const errores = campos.map((c) => validar(limpio, c, correoRegistrado)).filter(Boolean)
    const e = errores.includes(REQUERIDO)
      ? 'Completa los campos obligatorios.'
      : errores.length
        ? 'Revisa los datos marcados en rojo.'
        : limpio.password !== limpio.confirm
          ? 'Las contraseñas no coinciden.'
          : !limpio.terminos
            ? 'Acepta los términos y condiciones para continuar.'
            : null
    setError(e)
    if (e) {
      setScrollPedido((n) => n + 1)
      return
    }

    const tel = R.normalizarTelefono(limpio.telefono)
    const wa = limpio.mismoWhatsapp ? tel : R.normalizarTelefono(limpio.whatsapp)
    const nombreCompleto = `${R.limpiarEspacios(limpio.nombre)} ${R.limpiarEspacios(limpio.apellido)}`
    const datos: Record<string, string | null> = {
      tipo_cuenta: tipo,
      nombre_mostrar: esLote ? R.limpiarEspacios(limpio.nombreLote) : nombreCompleto,
      estado: limpio.estado.trim(),
      municipio: limpio.ciudad.trim(),
      // La BD guarda una sola fila si ambos números son iguales (ver fn_auth_crear_cuenta).
      telefono: tel,
      whatsapp: wa,
      medio_contacto: limpio.medio,
      descripcion: limpio.descripcion.trim(),
      ...(esLote
        ? {
            responsable: nombreCompleto,
            calle: R.limpiarEspacios(limpio.calle),
            numero: R.limpiarEspacios(limpio.numero),
            colonia: R.limpiarEspacios(limpio.colonia),
            codigo_postal: limpio.codigoPostal.trim(),
          }
        : {}),
    }
    const correo = R.limpiarCorreo(limpio.correo)

    setLoading(true)
    try {
      await signUp(correo, limpio.password, datos)
      // Sin verificación de correo: ya hay sesión. El logo es opcional; si falla la cuenta ya quedó creada.
      if (logo) await uploadAvatar(logo.blob).catch(() => undefined)
      await reload()
      toast('¡Cuenta creada! Bienvenido a Karsy')
      navigate('/', { replace: true })
    } catch (ex) {
      setLoading(false)
      if (ex instanceof EmailNotVerifiedError) {
        // Cuenta creada, pero sin sesión hasta escribir el código: el logo se sube al verificarlo.
        if (logo) holdAvatarUntilVerified(ex.email, logo.blob)
        navigate('/verificar', { replace: true, state: { email: ex.email, sendCode: false } })
        return
      }
      const msg = mensajeUsuario(ex)
      setError(msg)
      // Correo ya registrado: se marca el campo y se ofrece iniciar sesión.
      if (msg === MSG_EMAIL_EXISTS) {
        setCorreoRegistrado(correo)
        setScrollPedido((n) => n + 1)
      }
    }
  }

  const input = (campo: keyof Form & string, props: { placeholder?: string; filtro?: (v: string) => string; type?: string; autoComplete?: string; inputMode?: 'numeric' | 'tel' | 'email' } = {}) => (
    <input
      className={`input ${err(campo) ? 'bad' : ''}`}
      id={campo}
      type={props.type ?? 'text'}
      inputMode={props.inputMode}
      autoComplete={props.autoComplete}
      placeholder={props.placeholder}
      value={f[campo] as string}
      onChange={(e) => set(campo, (props.filtro ? props.filtro(e.target.value) : e.target.value) as never)}
      onBlur={() => touch(campo)}
    />
  )

  const ubicacion = (
    <div className="grid2">
      <Field id="estado" label="Estado" error={err('estado')}>
        <select
          className={`input ${err('estado') ? 'bad' : ''}`}
          id="estado"
          value={f.estado}
          onChange={(e) => setF((x) => ({ ...x, estado: e.target.value, ciudad: '' }))}
          onBlur={() => touch('estado')}
        >
          <option value="">Selecciona tu estado</option>
          {catalogo.map((e) => (
            <option key={e.estado}>{e.estado}</option>
          ))}
        </select>
      </Field>
      <Field id="ciudad" label="Ciudad" error={err('ciudad')}>
        <select
          className={`input ${err('ciudad') ? 'bad' : ''}`}
          id="ciudad"
          value={f.ciudad}
          disabled={!f.estado}
          onChange={(e) => set('ciudad', e.target.value)}
          onBlur={() => touch('ciudad')}
        >
          <option value="">{f.estado ? 'Selecciona tu ciudad' : 'Elige primero el estado'}</option>
          {municipios.map((m) => (
            <option key={m}>{m}</option>
          ))}
        </select>
      </Field>
    </div>
  )

  const correoCampo = (
    <Field id="correo" label="Correo electrónico" error={err('correo')}>
      {input('correo', { placeholder: 'correo@ejemplo.com', type: 'email', autoComplete: 'email', inputMode: 'email' })}
      {correoYaRegistrado && (
        <span className="ferr">
          <button onClick={() => navigate('/login')}>Iniciar sesión</button>
        </span>
      )}
      {correoSugerido && !err('correo') && (
        <span className="fwarn">
          ¿Quisiste decir{' '}
          <button onClick={() => set('correo', correoSugerido)}>{correoSugerido}</button>?
        </span>
      )}
    </Field>
  )

  const contacto = (
    <section className="card">
      <h3>
        <Icon name="phone" size={18} />
        Contacto
      </h3>
      <div className="grid2">
        <Field id="telefono" label={esLote ? 'Teléfono comercial' : 'Teléfono'} error={err('telefono')}>
          {input('telefono', { placeholder: '10 dígitos', type: 'tel', autoComplete: 'tel-national', inputMode: 'tel', filtro: R.telefonoAlEscribir })}
          {telefonoAviso && !err('telefono') && <span className="fwarn">{telefonoAviso}</span>}
        </Field>
        <div className="field">
          <label>¿Usas este mismo número para WhatsApp?</label>
          <div className="seg" style={{ alignSelf: 'flex-start' }}>
            <button type="button" className={f.mismoWhatsapp ? 'on' : ''} onClick={() => set('mismoWhatsapp', true)}>
              Sí
            </button>
            <button type="button" className={!f.mismoWhatsapp ? 'on' : ''} onClick={() => set('mismoWhatsapp', false)}>
              No
            </button>
          </div>
        </div>
      </div>
      <Field id="whatsapp" label="WhatsApp" error={err('whatsapp')}>
        {f.mismoWhatsapp ? (
          <input className="input" id="whatsapp" disabled value={f.telefono ? f.telefono : 'Igual a tu teléfono'} readOnly />
        ) : (
          input('whatsapp', { placeholder: 'Otro número (opcional)', type: 'tel', inputMode: 'tel', filtro: R.telefonoAlEscribir })
        )}
      </Field>
      <Field id="medio" label="Método de contacto principal" error={err('medio')}>
        <div className="row" style={{ gap: 8 }}>
          {(
            [
              [WHATSAPP, 'WhatsApp', 'chat'],
              [LLAMADA, 'Llamadas', 'phone'],
            ] as const
          ).map(([v, t, i]) => (
            <button
              key={v}
              type="button"
              className={`chip ${f.medio === v ? 'on' : ''} ${err('medio') ? 'bad' : ''}`}
              aria-pressed={f.medio === v}
              onClick={() => {
                set('medio', v)
                touch('medio')
              }}
            >
              <Icon name={i} size={16} />
              {t}
            </button>
          ))}
        </div>
      </Field>
    </section>
  )

  const datosPersona = (
    <div className="grid2">
      <Field id="nombre" label={esLote ? 'Nombre del responsable' : 'Nombre'} error={err('nombre')}>
        {input('nombre', { placeholder: esLote ? 'Ingresa el nombre' : 'Ingresa tu nombre', autoComplete: 'given-name', filtro: R.nombreAlEscribir })}
      </Field>
      <Field id="apellido" label={esLote ? 'Apellido del responsable' : 'Apellido'} error={err('apellido')}>
        {input('apellido', { placeholder: esLote ? 'Ingresa el apellido' : 'Ingresa tu apellido', autoComplete: 'family-name', filtro: R.nombreAlEscribir })}
      </Field>
    </div>
  )

  return (
    <>
      <header className="topnav">
        <div className="wrap">
          <Brand />
          <div style={{ marginLeft: 'auto' }} className="muted">
            ¿Ya tienes una cuenta?{' '}
            <button className="linkbtn" onClick={() => navigate('/login')}>
              Inicia sesión aquí
            </button>
          </div>
        </div>
      </header>
      <form
        className="regform"
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          void crear()
        }}
      >
        <button type="button" className="btn btn-ghost btn-sm" onClick={() => navigate('/registro')}>
          <Icon name="back" size={16} />
          Regresar
        </button>
        <h1 style={{ fontSize: 32, marginTop: 18 }}>{esLote ? 'Crear perfil de lote' : 'Registro'}</h1>
        <p className="muted">{esLote ? 'Registra tu lote para comenzar a publicar vehículos.' : 'Registra tus datos para comenzar a ver y vender vehículos'}</p>

        {esLote && (
          <section className="card">
            <h3>
              <Icon name="camera" size={18} />
              Foto de perfil
            </h3>
            <div className="row" style={{ gap: 18 }}>
              <button
                type="button"
                className={`photoslot ${logo ? 'filled' : ''}`}
                style={{ width: 110, height: 110, borderRadius: 24, flex: 'none' }}
                onClick={() => logoInput.current?.click()}
                aria-label={logo ? 'Cambiar logo' : 'Agregar logo'}
              >
                {logo ? (
                  <img src={logo.url} alt="Logo del lote" />
                ) : (
                  <>
                    <Icon name="camera" size={22} />
                    Agregar foto
                  </>
                )}
              </button>
              <div>
                <p className="muted">
                  Logo del lote o imagen de perfil (opcional). Mínimo {R.LOGO_MIN_PX} × {R.LOGO_MIN_PX} px.
                </p>
                {logo && (
                  <button type="button" className="linkbtn" style={{ color: 'var(--error)', marginTop: 6 }} onClick={() => setLogo(null)}>
                    Quitar logo
                  </button>
                )}
                {logoError && <p className="ferr">{logoError}</p>}
              </div>
              <input
                ref={logoInput}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                hidden
                onChange={(e) => {
                  void elegirLogo(e.target.files?.[0])
                  e.target.value = ''
                }}
              />
            </div>
          </section>
        )}

        {esLote ? (
          <section className="card">
            <h3>
              <Icon name="user" size={18} />
              Datos del responsable
            </h3>
            {datosPersona}
            {correoCampo}
          </section>
        ) : (
          <section className="card">
            <h3>
              <Icon name="user" size={18} />
              Información personal
            </h3>
            {datosPersona}
            {correoCampo}
            {ubicacion}
          </section>
        )}

        {contacto}

        {esLote && (
          <section className="card">
            <h3>
              <Icon name="building" size={18} />
              Información del lote
            </h3>
            <Field id="nombreLote" label="Nombre del lote" error={err('nombreLote')}>
              {input('nombreLote', { placeholder: 'Ej. Auto Premium', filtro: (v) => v.slice(0, R.LOTE_MAX) })}
            </Field>
            <div className="grid3">
              <Field id="calle" label="Calle" error={err('calle')} span={2}>
                {input('calle', { placeholder: 'Ej. Av. Juárez', filtro: (v) => v.slice(0, R.DIRECCION_MAX) })}
              </Field>
              <Field id="numero" label="Número" error={err('numero')}>
                {input('numero', { placeholder: '123', filtro: (v) => v.slice(0, R.NUMERO_MAX) })}
              </Field>
            </div>
            <div className="grid2">
              <Field id="colonia" label="Colonia" error={err('colonia')}>
                {input('colonia', { placeholder: 'Ej. Centro', filtro: (v) => v.slice(0, R.DIRECCION_MAX) })}
              </Field>
              <Field id="codigoPostal" label="C.P." error={err('codigoPostal')}>
                {input('codigoPostal', { placeholder: '27000', inputMode: 'numeric', autoComplete: 'postal-code', filtro: (v) => R.soloDigitos(v, 5) })}
                {cpAviso && !err('codigoPostal') && <span className="fwarn">{cpAviso}</span>}
              </Field>
            </div>
            {ubicacion}
            <Field id="descripcion" label="Descripción del lote (opcional)">
              <textarea
                className="input"
                id="descripcion"
                placeholder="Describe brevemente tu lote, los servicios que ofrece y el tipo de vehículos que maneja."
                value={f.descripcion}
                maxLength={R.DESCRIPCION_MAX}
                onChange={(e) => set('descripcion', e.target.value)}
              />
              <span className="muted" style={{ fontSize: 12, alignSelf: 'flex-end' }}>
                {f.descripcion.length}/{R.DESCRIPCION_MAX}
              </span>
            </Field>
          </section>
        )}

        <section className="card">
          <h3>
            <Icon name="lock" size={18} />
            Seguridad
          </h3>
          <div className="grid2">
            <Field id="password" label="Contraseña" error={err('password')}>
              <PasswordInput
                id="password"
                value={f.password}
                onChange={(v) => set('password', v)}
                onBlur={() => touch('password')}
                placeholder="Ingresa una contraseña"
                autoComplete="new-password"
                bad={!!err('password')}
              />
            </Field>
            <Field id="confirm" label="Confirmar contraseña" error={mismatch ? 'Las contraseñas no coinciden.' : null}>
              <PasswordInput id="confirm" value={f.confirm} onChange={(v) => set('confirm', v)} placeholder="Repite tu contraseña" autoComplete="new-password" bad={mismatch} />
            </Field>
          </div>
          <PasswordRulesList password={f.password} />
        </section>

        <label className="row" data-field="terminos" style={{ marginTop: 18, gap: 10, cursor: 'pointer', color: submitted && !f.terminos ? 'var(--error)' : undefined }}>
          <input
            type="checkbox"
            checked={f.terminos}
            onChange={(e) => set('terminos', e.target.checked)}
            style={{ width: 18, height: 18, accentColor: 'var(--teal)', outline: submitted && !f.terminos ? '2px solid var(--error)' : undefined }}
          />
          <span>
            Acepto los{' '}
            <a href="/terminos" target="_blank" rel="noreferrer">
              términos y condiciones
            </a>{' '}
            de KARSY.
          </span>
        </label>

        <div style={{ marginTop: 18 }}>
          <AlertBox message={error}>
            {correoYaRegistrado && (
              <>
                {' '}
                <button type="button" className="linkbtn" onClick={() => navigate('/login')}>
                  Iniciar sesión
                </button>
              </>
            )}
          </AlertBox>
        </div>

        <button type="submit" className="btn btn-primary btn-block" style={{ height: 52, marginTop: 18 }} disabled={loading}>
          {loading ? 'Creando perfil…' : 'Crear perfil'}
        </button>
      </form>
    </>
  )
}
