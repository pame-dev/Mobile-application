import { useEffect, useRef, useState, type ReactNode } from 'react'
import { VolverButton } from '../../components/VolverButton'
import { Navigate, useNavigate } from 'react-router-dom'
import { AlertBox, AuthShell, BackButton, PasswordInput, PasswordRulesList } from '../../components/AuthParts'
import { ConstanciaField } from '../../components/ConstanciaField'
import { Icon, type IconName } from '../../components/Icon'
import { Brand } from '../../components/TopNav'
import { useUi } from '../../components/Ui'
import { EmailNotVerifiedError, enviarCurp, holdAvatarUntilVerified, holdConstanciaUntilVerified, holdCurpUntilVerified, mensajeUsuario, msgEmailExiste, signUp, subirConstancia, telefonoEnUso, uploadAvatar } from '../../data/auth'
import { docAceptado, limpiarAceptaciones, type DocLegal } from '../../data/legal'
import { traducir, useI18n } from '../../i18n/I18nContext'
import type { Clave } from '../../i18n/es'
import * as R from '../../lib/formRules'
import { estadosMunicipios, jpegDe, type EstadoMunicipios } from '../../lib/mexico'
import { useSession } from '../../session/SessionContext'

type Tipo = 'particular' | 'lote'

/** Registro: tipo de cliente (RegisterTypeScreen de la app). */
export function RegisterType() {
  const navigate = useNavigate()
  const { t } = useI18n()
  const { account } = useSession()
  const [tipo, setTipo] = useState<Tipo>('particular')
  if (account) return <Navigate to="/" replace />

  const opciones: [Tipo, Clave, Clave, IconName, string | undefined][] = [
    ['particular', 'auth.type.individual', 'auth.type.individualDesc', 'user', undefined],
    ['lote', 'auth.type.lot', 'auth.type.lotDesc', 'building', 'var(--teal)'],
  ]
  return (
    <AuthShell title={t('auth.type.artTitle')} sub={t('auth.type.artSub')} photo={11}>
      <BackButton onClick={() => navigate('/')} />
      <h1>{t('auth.type.title')}</h1>
      <p className="muted">{t('auth.type.sub')}</p>
      {opciones.map(([v, titulo, d, i, bg]) => (
        <button key={v} className={`typecard ${tipo === v ? 'on' : ''}`} aria-pressed={tipo === v} onClick={() => setTipo(v)}>
          <span className="nic" style={bg ? { background: bg } : undefined}>
            <Icon name={i} size={26} />
          </span>
          <span className="grow">
            <b className="ink" style={{ fontFamily: 'var(--f-display)', fontSize: 'var(--fs-lg)' }}>{t(titulo)}</b>
            <br />
            <span className="muted">{t(d)}</span>
          </span>
          <Icon name="chev" />
        </button>
      ))}
      <button className="btn btn-primary btn-block btn-lg" onClick={() => navigate(`/registro/${tipo}`)}>
        {t('auth.type.continue')}
      </button>
      <p className="muted text-sm" style={{ textAlign: 'center' }}>
        {t('auth.type.later')}
      </p>
      <p className="muted" style={{ textAlign: 'center' }}>
        {t('auth.haveAccount')}{' '}
        <button className="linkbtn" onClick={() => navigate('/login')}>
          {t('auth.loginHere')}
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
  /** CURP (opcional, solo particulares): con ella el admin puede verificar la cuenta. */
  curp: string
  password: string
  confirm: string
}

const VACIO: Form = {
  nombre: '', apellido: '', correo: '', estado: '', ciudad: '', telefono: '', mismoWhatsapp: true, whatsapp: '',
  medio: null, nombreLote: '', calle: '', numero: '', colonia: '', codigoPostal: '', descripcion: '', curp: '', password: '',
  confirm: '',
}

/**
 * Lo escrito en el formulario mientras se consultan los términos o las políticas de privacidad:
 * al volver con "← Volver" se restaura. Solo vive en memoria (incluye la contraseña, así que no se
 * guarda en el navegador) y se descarta al recargar la página.
 */
let borrador: { tipo: Tipo; f: Form; logo: Blob | null; constancia: File | null } | null = null

/** "Campo obligatorio" en el idioma actual (se compara contra los errores armados en el mismo momento). */
const requerido = () => traducir('auth.reg.required')

/** Orden de los campos en pantalla (para llevar la vista al primer error). */
const CAMPOS_PARTICULAR = ['nombre', 'apellido', 'correo', 'estado', 'ciudad', 'curp', 'telefono', 'whatsapp', 'medio', 'password']
const CAMPOS_LOTE = ['nombre', 'apellido', 'correo', 'telefono', 'whatsapp', 'medio', 'nombreLote', 'calle', 'numero', 'colonia', 'codigoPostal', 'estado', 'ciudad', 'password']

/** Error del campo con su valor actual, o null si está bien (validar() de RegisterViewModel). */
function validar(f: Form, campo: string, correoRegistrado: string | null): string | null {
  const obligatorio = (v: string, regla: (x: string) => string | null) => (!v.trim() ? requerido() : regla(v))
  switch (campo) {
    case 'nombre':
      return obligatorio(f.nombre, R.nombrePersona)
    case 'apellido':
      return obligatorio(f.apellido, R.nombrePersona)
    case 'correo':
      return obligatorio(f.correo, R.correo) ?? (correoRegistrado && R.limpiarCorreo(f.correo) === correoRegistrado ? msgEmailExiste() : null)
    case 'telefono':
      return obligatorio(f.telefono, R.telefono)
    // Si no usa el mismo número, el WhatsApp es opcional (vacío = no tiene WhatsApp).
    case 'whatsapp':
      if (f.mismoWhatsapp) return null
      if (f.medio === WHATSAPP && !f.whatsapp.trim()) return traducir('auth.reg.whatsappOrCalls')
      return R.telefono(f.whatsapp)
    case 'medio':
      return f.medio ? null : traducir('auth.reg.chooseMethod')
    case 'estado':
      return f.estado ? null : requerido()
    case 'ciudad':
      return f.ciudad ? null : requerido()
    case 'password':
      return !f.password ? requerido() : R.passwordError(f.password, f.correo)
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
    // Opcional: vacía no es error.
    case 'curp':
      return R.curp(f.curp)
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
  const { t } = useI18n()
  const { account, reload } = useSession()
  const { toast } = useUi()
  const [f, setF] = useState<Form>(() => (borrador?.tipo === tipo ? borrador.f : VACIO))
  const [touched, setTouched] = useState<Set<string>>(new Set())
  const [submitted, setSubmitted] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [correoRegistrado, setCorreoRegistrado] = useState<string | null>(null)
  const [telefonoAviso, setTelefonoAviso] = useState<string | null>(null)
  const [catalogo, setCatalogo] = useState<EstadoMunicipios[]>([])
  /** Constancia de Situación Fiscal (opcional, solo lotes): se sube después de crear la cuenta. */
  const [constancia, setConstancia] = useState<File | null>(() => (borrador?.tipo === tipo ? borrador.constancia : null))
  const [logo, setLogo] = useState<{ blob: Blob; url: string } | null>(() => {
    const blob = borrador?.tipo === tipo ? borrador.logo : null
    return blob ? { blob, url: URL.createObjectURL(blob) } : null
  })
  const [logoError, setLogoError] = useState<string | null>(null)
  const logoInput = useRef<HTMLInputElement>(null)
  const [scrollPedido, setScrollPedido] = useState(0)
  /** Teléfono actual, para descartar la consulta de "ya registrado" si cambió mientras tanto. */
  const telefonoActual = useRef(f.telefono)
  telefonoActual.current = f.telefono

  // El borrador ya se restauró en el estado inicial.
  useEffect(() => {
    borrador = null
  }, [])
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

  // Aceptaciones hechas al final de cada documento (se leen al volver de él).
  const terminosOk = docAceptado('terminos')
  const privacidadOk = docAceptado('privacidad')
  const formularioValido = campos.every((c) => validar(f, c, correoRegistrado) == null) && f.password === f.confirm
  const listoParaCrear = terminosOk && privacidadOk && formularioValido

  // Después de un envío fallido, la vista baja al primer campo con error.
  useEffect(() => {
    if (!scrollPedido) return
    const conError = [
      ...campos.filter((c) => validar(f, c, correoRegistrado) != null),
      ...(f.password && f.password !== f.confirm ? ['confirm'] : []),
      ...(!terminosOk || !privacidadOk ? ['legales'] : []),
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

  /** Antes de ir a los términos o las políticas: se guarda lo escrito para restaurarlo al volver. */
  const guardarBorrador = () => {
    borrador = { tipo, f, logo: logo?.blob ?? null, constancia }
  }

  const leer = (doc: DocLegal) => {
    guardarBorrador()
    navigate(doc === 'terminos' ? '/terminos' : '/privacidad/politicas', { state: { desdeRegistro: true } })
  }

  const revisarTelefono = async (valor: string) => {
    setTelefonoAviso(null)
    if (R.telefono(valor) != null || !valor.trim()) return
    const numero = R.normalizarTelefono(valor)
    const enUso = await telefonoEnUso(numero).catch(() => false)
    // Solo si sigue siendo el mismo número (pudo cambiar mientras se consultaba).
    if (enUso && R.normalizarTelefono(telefonoActual.current) === numero) setTelefonoAviso(t('auth.reg.phoneInUse'))
  }

  const elegirLogo = async (file: File | undefined) => {
    if (!file) return
    try {
      const { blob, ladoMenor } = await jpegDe(file, 800)
      if (ladoMenor < R.LOGO_MIN_PX) {
        setLogoError(t('auth.reg.logoSmall', { n: R.LOGO_MIN_PX }))
        return
      }
      setLogo({ blob, url: URL.createObjectURL(blob) })
      setLogoError(null)
    } catch {
      setLogoError(t('auth.reg.logoUnreadable'))
    }
  }

  const crear = async () => {
    if (loading) return
    setSubmitted(true)
    const limpio = { ...f, nombre: R.capitalizarNombre(f.nombre), apellido: R.capitalizarNombre(f.apellido) }
    setF(limpio)
    const errores = campos.map((c) => validar(limpio, c, correoRegistrado)).filter(Boolean)
    const e = errores.includes(requerido())
      ? t('auth.reg.completeRequired')
      : errores.length
        ? t('auth.reg.checkRed')
        : limpio.password !== limpio.confirm
          ? t('auth.passwordMismatch')
          : !docAceptado('terminos') || !docAceptado('privacidad')
            ? t('auth.reg.acceptDocs')
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
      limpiarAceptaciones()
      // Sin verificación de correo: ya hay sesión. El logo es opcional; si falla la cuenta ya quedó creada.
      if (logo) await uploadAvatar(logo.blob).catch(() => undefined)
      // La constancia también es opcional: si no se pudo subir, la cuenta queda 'sin_documento' y se avisa.
      const constanciaFallo = esLote && constancia ? await subirConstancia(constancia).then(() => false, () => true) : false
      // La CURP del particular (opcional) va a revisión; si falla, la puede mandar desde su perfil.
      const curpFallo = !esLote && limpio.curp ? await enviarCurp(limpio.curp).then(() => false, () => true) : false
      await reload()
      toast(t(constanciaFallo ? 'auth.csf.uploadFailed' : curpFallo ? 'auth.curp.sendFailed' : 'auth.reg.created'))
      navigate('/', { replace: true })
    } catch (ex) {
      setLoading(false)
      if (ex instanceof EmailNotVerifiedError) {
        limpiarAceptaciones()
        // Cuenta creada, pero sin sesión hasta escribir el código: el logo se sube al verificarlo.
        if (logo) holdAvatarUntilVerified(ex.email, logo.blob)
        if (esLote && constancia) holdConstanciaUntilVerified(ex.email, constancia)
        if (!esLote && limpio.curp) holdCurpUntilVerified(ex.email, limpio.curp)
        navigate('/verificar', { replace: true, state: { email: ex.email, sendCode: false } })
        return
      }
      const msg = mensajeUsuario(ex)
      setError(msg)
      // Correo ya registrado: se marca el campo y se ofrece iniciar sesión.
      if (msg === msgEmailExiste()) {
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
      <Field id="estado" label={t('auth.reg.state')} error={err('estado')}>
        <select
          className={`input ${err('estado') ? 'bad' : ''}`}
          id="estado"
          value={f.estado}
          onChange={(e) => setF((x) => ({ ...x, estado: e.target.value, ciudad: '' }))}
          onBlur={() => touch('estado')}
        >
          <option value="">{t('auth.reg.chooseState')}</option>
          {catalogo.map((e) => (
            <option key={e.estado}>{e.estado}</option>
          ))}
        </select>
      </Field>
      <Field id="ciudad" label={t('auth.reg.city')} error={err('ciudad')}>
        <select
          className={`input ${err('ciudad') ? 'bad' : ''}`}
          id="ciudad"
          value={f.ciudad}
          disabled={!f.estado}
          onChange={(e) => set('ciudad', e.target.value)}
          onBlur={() => touch('ciudad')}
        >
          <option value="">{f.estado ? t('auth.reg.chooseCity') : t('auth.reg.stateFirst')}</option>
          {municipios.map((m) => (
            <option key={m}>{m}</option>
          ))}
        </select>
      </Field>
    </div>
  )

  const correoCampo = (
    <Field id="correo" label={t('auth.email')} error={err('correo')}>
      {input('correo', { placeholder: t('auth.reg.emailPlaceholder'), type: 'email', autoComplete: 'email', inputMode: 'email' })}
      {correoYaRegistrado && (
        <span className="ferr">
          <button onClick={() => navigate('/login')}>{t('auth.login')}</button>
        </span>
      )}
      {correoSugerido && !err('correo') && (
        <span className="fwarn">
          {t('auth.reg.didYouMean')}{' '}
          <button onClick={() => set('correo', correoSugerido)}>{correoSugerido}</button>?
        </span>
      )}
    </Field>
  )

  const contacto = (
    <section className="card">
      <h2>
        <Icon name="phone" size={18} />
        {t('auth.reg.contact')}
      </h2>
      <div className="grid2">
        <Field id="telefono" label={esLote ? t('auth.reg.businessPhone') : t('auth.reg.phone')} error={err('telefono')}>
          {input('telefono', { placeholder: t('auth.reg.phonePlaceholder'), type: 'tel', autoComplete: 'tel-national', inputMode: 'tel', filtro: R.telefonoAlEscribir })}
          {telefonoAviso && !err('telefono') && <span className="fwarn">{telefonoAviso}</span>}
        </Field>
        <div className="field">
          <label>{t('auth.reg.sameWhatsapp')}</label>
          <div className="seg" style={{ alignSelf: 'flex-start' }}>
            <button type="button" className={f.mismoWhatsapp ? 'on' : ''} aria-pressed={f.mismoWhatsapp} onClick={() => set('mismoWhatsapp', true)}>
              {t('auth.reg.yes')}
            </button>
            <button type="button" className={!f.mismoWhatsapp ? 'on' : ''} aria-pressed={!f.mismoWhatsapp} onClick={() => set('mismoWhatsapp', false)}>
              {t('auth.reg.no')}
            </button>
          </div>
        </div>
      </div>
      <Field id="whatsapp" label={t('auth.reg.whatsapp')} error={err('whatsapp')}>
        {f.mismoWhatsapp ? (
          <input className="input" id="whatsapp" disabled value={f.telefono ? f.telefono : t('auth.reg.sameAsPhone')} readOnly />
        ) : (
          input('whatsapp', { placeholder: t('auth.reg.otherNumber'), type: 'tel', inputMode: 'tel', filtro: R.telefonoAlEscribir })
        )}
      </Field>
      <Field id="medio" label={t('auth.reg.mainMethod')} error={err('medio')}>
        <div className="row gap-2">
          {(
            [
              [WHATSAPP, 'auth.reg.whatsapp', 'chat'],
              [LLAMADA, 'auth.reg.calls', 'phone'],
            ] as const
          ).map(([v, etiqueta, i]) => (
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
              {t(etiqueta)}
            </button>
          ))}
        </div>
      </Field>
    </section>
  )

  const datosPersona = (
    <div className="grid2">
      <Field id="nombre" label={esLote ? t('auth.reg.managerName') : t('auth.reg.name')} error={err('nombre')}>
        {input('nombre', { placeholder: esLote ? t('auth.reg.managerNamePlaceholder') : t('auth.reg.namePlaceholder'), autoComplete: 'given-name', filtro: R.nombreAlEscribir })}
      </Field>
      <Field id="apellido" label={esLote ? t('auth.reg.managerLast') : t('auth.reg.last')} error={err('apellido')}>
        {input('apellido', { placeholder: esLote ? t('auth.reg.managerLastPlaceholder') : t('auth.reg.lastPlaceholder'), autoComplete: 'family-name', filtro: R.nombreAlEscribir })}
      </Field>
    </div>
  )

  return (
    <>
      <header className="topnav">
        <div className="wrap">
          <Brand />
          <div style={{ marginLeft: 'auto' }} className="muted">
            {t('auth.haveAccount')}{' '}
            <button className="linkbtn" onClick={() => navigate('/login')}>
              {t('auth.loginHere')}
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
        <VolverButton to="/registro" label={t('auth.back')} />
        <h1 className="h1-page" style={{ marginTop: 'var(--s-4)' }}>{esLote ? t('auth.reg.lotTitle') : t('auth.reg.title')}</h1>
        <p className="muted">{esLote ? t('auth.reg.lotSub') : t('auth.reg.sub')}</p>

        {esLote && (
          <section className="card">
            <h2>
              <Icon name="camera" size={18} />
              {t('auth.reg.photo')}
            </h2>
            <div className="row gap-4">
              <button
                type="button"
                className={`photoslot ${logo ? 'filled' : ''}`} style={{ width: 110, height: 110, borderRadius: 'var(--r-xl)', flex: 'none' }}
                onClick={() => logoInput.current?.click()}
                aria-label={logo ? t('auth.reg.changeLogo') : t('auth.reg.addLogo')}
              >
                {logo ? (
                  <img src={logo.url} alt={t('auth.reg.logoAlt')} />
                ) : (
                  <>
                    <Icon name="camera" size={24} />
                    {t('auth.reg.addPhoto')}
                  </>
                )}
              </button>
              <div>
                <p className="muted">
                  {t('auth.reg.logoHint', { n: R.LOGO_MIN_PX })}
                </p>
                {logo && (
                  <button type="button" className="linkbtn" style={{ color: 'var(--error)', marginTop: 'var(--s-2)' }} onClick={() => setLogo(null)}>
                    {t('auth.reg.removeLogo')}
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
            <h2>
              <Icon name="user" size={18} />
              {t('auth.reg.managerData')}
            </h2>
            {datosPersona}
            {correoCampo}
          </section>
        ) : (
          <section className="card">
            <h2>
              <Icon name="user" size={18} />
              {t('auth.reg.personal')}
            </h2>
            {datosPersona}
            {correoCampo}
            {ubicacion}
            <Field id="curp" label={t('auth.curp.label')} error={err('curp')}>
              {input('curp', { placeholder: 'GODE561231HDFRRN09', filtro: R.curpAlEscribir })}
              {!err('curp') && <span className="muted text-xs">{t('auth.curp.hint')}</span>}
            </Field>
          </section>
        )}

        {contacto}

        {esLote && (
          <section className="card">
            <h2>
              <Icon name="building" size={18} />
              {t('auth.reg.lotInfo')}
            </h2>
            <Field id="nombreLote" label={t('auth.reg.lotName')} error={err('nombreLote')}>
              {input('nombreLote', { placeholder: t('auth.reg.lotNamePlaceholder'), filtro: (v) => v.slice(0, R.LOTE_MAX) })}
            </Field>
            <div className="grid3">
              <Field id="calle" label={t('auth.reg.street')} error={err('calle')} span={2}>
                {input('calle', { placeholder: t('auth.reg.streetPlaceholder'), filtro: (v) => v.slice(0, R.DIRECCION_MAX) })}
              </Field>
              <Field id="numero" label={t('auth.reg.number')} error={err('numero')}>
                {input('numero', { placeholder: '123', filtro: (v) => v.slice(0, R.NUMERO_MAX) })}
              </Field>
            </div>
            <div className="grid2">
              <Field id="colonia" label={t('auth.reg.colony')} error={err('colonia')}>
                {input('colonia', { placeholder: t('auth.reg.colonyPlaceholder'), filtro: (v) => v.slice(0, R.DIRECCION_MAX) })}
              </Field>
              <Field id="codigoPostal" label={t('auth.reg.zip')} error={err('codigoPostal')}>
                {input('codigoPostal', { placeholder: '27000', inputMode: 'numeric', autoComplete: 'postal-code', filtro: (v) => R.soloDigitos(v, 5) })}
                {cpAviso && !err('codigoPostal') && <span className="fwarn">{cpAviso}</span>}
              </Field>
            </div>
            {ubicacion}
            <ConstanciaField file={constancia} onChange={setConstancia} />
            <Field id="descripcion" label={t('auth.reg.description')}>
              <textarea
                className="input"
                id="descripcion"
                placeholder={t('auth.reg.descriptionPlaceholder')}
                value={f.descripcion}
                maxLength={R.DESCRIPCION_MAX}
                onChange={(e) => set('descripcion', e.target.value)}
              />
              <span className="muted text-xs" style={{ alignSelf: 'flex-end' }}>
                {f.descripcion.length}/{R.DESCRIPCION_MAX}
              </span>
            </Field>
          </section>
        )}

        <section className="card">
          <h2>
            <Icon name="lock" size={18} />
            {t('auth.reg.security')}
          </h2>
          <div className="grid2">
            <Field id="password" label={t('auth.reg.password')} error={err('password')}>
              <PasswordInput
                id="password"
                value={f.password}
                onChange={(v) => set('password', v)}
                onBlur={() => touch('password')}
                placeholder={t('auth.passwordPlaceholder')}
                autoComplete="new-password"
                bad={!!err('password')}
              />
            </Field>
            <Field id="confirm" label={t('auth.confirmPassword')} error={mismatch ? t('auth.passwordMismatch') : null}>
              <PasswordInput id="confirm" value={f.confirm} onChange={(v) => set('confirm', v)} placeholder={t('auth.confirmPlaceholder')} autoComplete="new-password" bad={mismatch} />
            </Field>
          </div>
          <PasswordRulesList password={f.password} />
        </section>

        <section className="card" data-field="legales">
          <h2>
            <Icon name="doc" size={18} />
            {t('auth.reg.legalTitle')}
          </h2>
          {(
            [
              ['terminos', 'doc', 'auth.reg.termsCard', 'auth.reg.termsCardDesc', terminosOk],
              ['privacidad', 'shield', 'auth.reg.privacyCard', 'auth.reg.privacyCardDesc', privacidadOk],
            ] as const
          ).map(([doc, icono, titulo, desc, ok]) => (
            <div key={doc} className="legaldoc">
              <span className="nic">
                <Icon name={icono} size={20} />
              </span>
              <div className="grow" style={{ minWidth: 0 }}>
                <b className="ink">{t(titulo)}</b>
                {ok && (
                  <span className="badge b-succ" style={{ marginLeft: 'var(--s-2)' }}>
                    <Icon name="check" size={14} />
                    {t('auth.reg.accepted')}
                  </span>
                )}
                <p className="muted text-sm">
                  {t(desc)}
                </p>
              </div>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => leer(doc)}>
                {t('auth.reg.read')}
              </button>
            </div>
          ))}
        </section>

        <div style={{ marginTop: 'var(--s-4)' }}>
          <AlertBox message={error}>
            {correoYaRegistrado && (
              <>
                {' '}
                <button type="button" className="linkbtn" onClick={() => navigate('/login')}>
                  {t('auth.login')}
                </button>
              </>
            )}
          </AlertBox>
        </div>

        <button type="submit" className="btn btn-primary btn-block btn-lg" style={{ marginTop: 'var(--s-4)' }} disabled={loading || !listoParaCrear}>
          {loading ? t('auth.reg.creating') : t('auth.reg.create')}
        </button>
        {!loading && !listoParaCrear && (
          <p className="muted text-sm" style={{ textAlign: 'center', marginTop: 'var(--s-3)' }}>
            {!terminosOk || !privacidadOk ? (
              t('auth.reg.docsPending')
            ) : (
              <>
                {t('auth.reg.fieldsPending')}{' '}
                <button
                  type="button"
                  className="linkbtn"
                  onClick={() => {
                    setSubmitted(true)
                    setScrollPedido((n) => n + 1)
                  }}
                >
                  {t('auth.reg.showPending')}
                </button>
              </>
            )}
          </p>
        )}
      </form>
    </>
  )
}
