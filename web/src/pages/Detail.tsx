import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { VerifiedShield } from '../components/ConstanciaField'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { CarPhoto } from '../components/CarPhoto'
import { ConditionTag } from '../components/ConditionTag'
import { Icon } from '../components/Icon'
import { PhotoViewer } from '../components/PhotoViewer'
import { Traducido } from '../components/Traducido'
import { hayPantallaAnterior } from '../components/VolverButton'
import { OwnerButtons, statusLabel, useOwnerActions } from '../components/OwnerActions'
import { ModalHead, useUi } from '../components/Ui'
import { VehicleCard } from '../components/VehicleCard'
import { carDetail, cuentaVerificada, registerView, registrarContacto, reportListing, sellerContact, UserError, visibleCars, traduccionPublicacion } from '../data/api'
import { ocultas, ownCar, setOculta, type OwnCar } from '../data/seller'
import type { Car, CarDetail, SellerContact } from '../data/types'
import { useI18n } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { nombreCatalogo } from '../lib/catalogo'
import { construirTextoCompartir } from '../lib/compartir'
import { AdminReportBar, AdminReviewBar } from './admin/AdminParts'
import { iniciales, km, precio, whatsappUrl } from '../lib/format'
import { useFavorites } from '../session/FavoritesContext'
import { useSession } from '../session/SessionContext'
import { useRecargar } from '../lib/tiempoReal'
import { CargandoBloqueo } from '../components/CargandoBloqueo'

/**
 * Compartir (mismo mensaje que la app, ver construirTextoCompartir). Con Web Share (celular)
 * va todo en `text`, URL incluida: si se manda aparte en `url`, WhatsApp la mueve y rompe el
 * formato. Sin Web Share (escritorio) abre un menú: WhatsApp (sin número) o copiar el mensaje.
 * La vista previa con foto la pone la Edge Function vista-previa.
 */
function CompartirBoton({ c }: { c: Car }) {
  const { t } = useI18n()
  const { toast } = useUi()
  const [menu, setMenu] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!menu) return
    const fuera = (e: MouseEvent) => ref.current && !ref.current.contains(e.target as Node) && setMenu(false)
    const tecla = (e: KeyboardEvent) => e.key === 'Escape' && setMenu(false)
    document.addEventListener('mousedown', fuera)
    document.addEventListener('keydown', tecla)
    return () => {
      document.removeEventListener('mousedown', fuera)
      document.removeEventListener('keydown', tecla)
    }
  }, [menu])

  const compartir = async () => {
    const texto = construirTextoCompartir(c, t)
    if (!navigator.share) return setMenu((v) => !v)
    try {
      await navigator.share({ text: texto })
    } catch (e) {
      // Cancelar no es error; si el navegador no deja compartir, se ofrece el menú.
      if ((e as Error)?.name !== 'AbortError') setMenu(true)
    }
  }
  const copiar = async () => {
    setMenu(false)
    try {
      await navigator.clipboard.writeText(construirTextoCompartir(c, t))
      toast(t('vehiculo.share.copied'))
    } catch {
      /* sin permiso al portapapeles */
    }
  }

  return (
    <div className="sharewrap" ref={ref}>
      <button className="iconbtn" aria-label={t('vehiculo.detail.share')} aria-haspopup="menu" aria-expanded={menu} onClick={() => void compartir()}>
        <Icon name="share" size={18} />
      </button>
      {menu && (
        <div className="sharemenu" role="menu">
          <a role="menuitem" href={`https://wa.me/?text=${encodeURIComponent(construirTextoCompartir(c, t))}`} target="_blank" rel="noopener noreferrer" onClick={() => setMenu(false)}>
            <Icon name="chat" size={18} />
            {t('vehiculo.share.whatsapp')}
          </a>
          <button role="menuitem" onClick={() => void copiar()}>
            <Icon name="copy" size={18} />
            {t('vehiculo.share.copy')}
          </button>
        </div>
      )}
    </div>
  )
}

/**
 * Foto principal deslizable: las fotos van en una tira horizontal con scroll-snap, así el dedo y
 * el trackpad la arrastran de forma nativa y al soltar se acomoda en una foto (sin vuelta: en la
 * primera y la última solo rebota). Con el mouse se arrastra con pointer events y cambia de foto
 * a partir de 50 px o el 20 % del ancho. Lo que va encima (Volver, corazón, contador) se pasa en
 * [children] y queda fijo: no se mueve con la tira. Un toque sin arrastrar llama a [onAbrir].
 */
function FotoPrincipal({
  fotos,
  foto,
  onCambiar,
  onAbrir,
  alt,
  className,
  children,
}: {
  fotos: (string | null)[]
  foto: number
  onCambiar: (i: number) => void
  onAbrir?: () => void
  alt: string
  className: string
  children: ReactNode
}) {
  const tira = useRef<HTMLDivElement>(null)
  // La foto actual y onCambiar al momento (el temporizador del scroll lee los más recientes).
  const actual = useRef({ foto, onCambiar })
  actual.current = { foto, onCambiar }
  const espera = useRef(0)
  const raton = useRef<{ x: number; left: number; movio: boolean } | null>(null)
  const arrastro = useRef(false)
  /** El dedo sigue sobre la tira: aunque se quede quieto, todavía no terminó de deslizar. */
  const dedo = useRef(false)
  const varias = fotos.length > 1

  // Flechas, miniaturas o teclado → la tira se desliza a esa foto.
  useEffect(() => {
    const el = tira.current
    if (!el || !el.clientWidth) return
    // Un "terminó de deslizar" pendiente leería la posición vieja y regresaría a la foto anterior.
    window.clearTimeout(espera.current)
    const destino = foto * el.clientWidth
    if (Math.abs(el.scrollLeft - destino) > 2) el.scrollTo({ left: destino, behavior: 'smooth' })
  }, [foto])
  // Al cambiar el tamaño de la ventana, la tira se queda en la foto activa.
  useEffect(() => {
    const realinear = () => {
      const el = tira.current
      if (el) el.scrollLeft = actual.current.foto * el.clientWidth
    }
    window.addEventListener('resize', realinear)
    return () => {
      window.removeEventListener('resize', realinear)
      window.clearTimeout(espera.current)
    }
  }, [])

  // Al terminar de deslizar (dedo, trackpad o mouse), la foto que quedó es la activa.
  const alDeslizar = () => {
    window.clearTimeout(espera.current)
    espera.current = window.setTimeout(() => {
      const el = tira.current
      if (!el || raton.current || dedo.current || !el.clientWidth) return
      const i = Math.round(el.scrollLeft / el.clientWidth)
      if (i !== actual.current.foto) actual.current.onCambiar(i)
    }, 120)
  }

  // Arrastre con el mouse (el dedo y el trackpad ya los maneja el navegador).
  const soltar = (x: number) => {
    const el = tira.current
    const r = raton.current
    raton.current = null
    if (!el || !r) return
    arrastro.current = r.movio
    const ancho = el.clientWidth
    const dx = x - r.x
    const desde = Math.round(r.left / ancho)
    const umbral = Math.min(50, ancho * 0.2)
    const destino = Math.max(0, Math.min(fotos.length - 1, Math.abs(dx) > umbral ? desde + (dx < 0 ? 1 : -1) : desde))
    el.scrollTo({ left: destino * ancho, behavior: 'smooth' })
    // El snap vuelve cuando ya terminó la animación (si vuelve antes, salta).
    window.setTimeout(() => {
      if (!raton.current) el.style.scrollSnapType = ''
    }, 300)
  }

  return (
    <div
      className={`main ${className}`}
      role={onAbrir ? 'button' : undefined}
      tabIndex={onAbrir ? 0 : undefined}
      aria-label={onAbrir ? alt : undefined}
      onClick={() => {
        // Un arrastre con el mouse no abre el visor.
        if (arrastro.current) arrastro.current = false
        else onAbrir?.()
      }}
      onKeyDown={(e) => {
        if (!onAbrir || e.target !== e.currentTarget || (e.key !== 'Enter' && e.key !== ' ')) return
        e.preventDefault()
        onAbrir()
      }}
    >
      <div
        className="mainstrip"
        ref={tira}
        onScroll={alDeslizar}
        onTouchStart={() => (dedo.current = true)}
        onTouchEnd={() => {
          // Al soltar, el navegador acomoda la foto (snap) y alDeslizar fija la activa al terminar.
          dedo.current = false
          alDeslizar()
        }}
        onTouchCancel={() => {
          dedo.current = false
          alDeslizar()
        }}
        onDragStart={(e) => e.preventDefault()}
        onPointerDown={(e) => {
          arrastro.current = false
          if (!varias || e.pointerType !== 'mouse' || e.button !== 0 || !tira.current) return
          raton.current = { x: e.clientX, left: tira.current.scrollLeft, movio: false }
          tira.current.style.scrollSnapType = 'none'
        }}
        onPointerMove={(e) => {
          const r = raton.current
          if (!r || !tira.current) return
          const dx = e.clientX - r.x
          if (Math.abs(dx) > 4) r.movio = true
          tira.current.scrollLeft = r.left - dx
        }}
        onPointerUp={(e) => soltar(e.clientX)}
        onPointerLeave={(e) => soltar(e.clientX)}
        onPointerCancel={(e) => soltar(e.clientX)}
      >
        {fotos.map((src, i) => (
          <CarPhoto key={i} src={src} alt={i === foto ? alt : ''} className="slide" />
        ))}
      </div>
      {children}
    </div>
  )
}

/**
 * Fila de miniaturas con flechas a los lados (del ancho de la foto principal). Las flechas pasan a
 * la foto anterior/siguiente sin abrir el visor; la fila se desliza con el dedo o el trackpad sin
 * cambiar la foto. Si no caben todas, la orilla derecha se desvanece para que se note que hay más.
 */
function Miniaturas({ fotos, foto, onCambiar }: { fotos: (string | null)[]; foto: number; onCambiar: (i: number) => void }) {
  const { t } = useI18n()
  const fila = useRef<HTMLDivElement>(null)
  // Desvanecido en la orilla donde quedan miniaturas por ver (derecha: hay más; izquierda: ya no está al inicio).
  const [hayMas, setHayMas] = useState(false)
  const [hayAntes, setHayAntes] = useState(false)
  const revisarMas = useCallback(() => {
    const f = fila.current
    if (!f) return
    setHayMas(f.scrollLeft + f.clientWidth < f.scrollWidth - 2)
    setHayAntes(f.scrollLeft > 2)
  }, [])
  // La miniatura activa siempre a la vista (desplazamiento suave, solo dentro de la fila).
  useEffect(() => {
    const f = fila.current
    const activa = f?.children[foto] as HTMLElement | undefined
    if (f && activa) {
      // .thumbs tiene position:relative: offsetLeft es la posición dentro de la fila.
      const izq = activa.offsetLeft
      if (izq < f.scrollLeft) f.scrollTo({ left: izq, behavior: 'smooth' })
      else if (izq + activa.offsetWidth > f.scrollLeft + f.clientWidth) f.scrollTo({ left: izq + activa.offsetWidth - f.clientWidth, behavior: 'smooth' })
    }
    revisarMas()
  }, [foto, revisarMas])
  useEffect(() => {
    window.addEventListener('resize', revisarMas)
    return () => window.removeEventListener('resize', revisarMas)
  }, [revisarMas])

  return (
    <div className="thumbsrow">
      <button type="button" className="iconbtn thumbnav" aria-label={t('vehiculo.detail.prevPhoto')} disabled={foto === 0} onClick={() => onCambiar(foto - 1)}>
        <Icon name="back" size={18} />
      </button>
      <div className={`thumbs ${hayMas ? 'mas' : ''} ${hayAntes ? 'antes' : ''}`} ref={fila} onScroll={revisarMas} onDragStart={(e) => e.preventDefault()}>
        {fotos.map((src, i) => (
          <CarPhoto key={i} src={src} className={i === foto ? 'on' : ''} onClick={() => onCambiar(i)} alt={t('vehiculo.detail.photoAlt', { n: i + 1 })} />
        ))}
      </div>
      <button type="button" className="iconbtn thumbnav" aria-label={t('vehiculo.detail.nextPhoto')} disabled={foto === fotos.length - 1} onClick={() => onCambiar(foto + 1)}>
        <Icon name="chev" size={18} />
      </button>
    </div>
  )
}

const STATUS_BADGE: Record<Car['status'], string> = {
  Activo: 'b-succ',
  Pendiente: 'b-warn',
  Rechazado: 'b-dang',
  Deshabilitado: 'b-neu',
  Vendido: 'b-info',
  Pausado: 'b-neu',
}

/** Datos del vendedor. Abrirla no cuenta como contacto: cuenta al tocar WhatsApp o Llamar. */
function ContactModal({ detail }: { detail: CarDetail }) {
  const { account } = useSession()
  const { t } = useI18n()
  const [c, setC] = useState<SellerContact | null | undefined>(undefined)
  /** Veces que se tocó "Reintentar". */
  const [intento, setIntento] = useState(0)
  useEffect(() => {
    sellerContact(detail.car.id).then(setC).catch(() => setC(null))
  }, [detail.car.id, intento])
  // Una vez por ventana; además la base de datos guarda como máximo uno por persona y auto al día.
  const contado = useRef(false)
  const contar = () => {
    if (contado.current || !account) return
    contado.current = true
    void registrarContacto(detail.car.id, account.id).catch(() => undefined)
  }

  const car = detail.car
  // Mensaje de WhatsApp: saludo con el nombre del vendedor, quién escribe, el auto en negritas,
  // precio y km, la pregunta y el enlace (WhatsApp muestra la tarjeta con foto y precio).
  // Sin nombre de alguno de los dos, esa parte no aparece.
  const vendedor = c ? (c.tipo_cuenta === 'lote' ? c.nombre : c.nombre.split(' ')[0]).trim() : ''
  const yo = (account?.nombre ?? '').split(' ')[0].trim()
  const auto = `${car.brand} ${car.model} ${car.year || ''}`.trim()
  const mensaje = [
    vendedor ? t('vehiculo.contact.msgHello', { n: vendedor }) : t('vehiculo.contact.msgHelloAnon'),
    yo ? t('vehiculo.contact.msgIntro', { n: yo, v: auto }) : t('vehiculo.contact.msgIntroAnon', { v: auto }),
    [`${precio(car.priceValue)} ${car.currency}`, car.km ? km(car.km) : ''].filter(Boolean).join(' · '),
    t('vehiculo.contact.msgQuestion'),
    '',
    `${window.location.origin}/vehiculo/${car.id}`,
  ].join('\n')
  const whatsapp = c?.numero_whatsapp ?? (c?.whatsapp ? c.telefono : null)
  const prefiereWa = c?.medio_principal === 'whatsapp'
  return (
    <div className="modal">
      <ModalHead title={t('vehiculo.contact.title')} sub={c ? `${c.nombre} · ${t(c.tipo_cuenta === 'lote' ? 'nav.lot' : 'nav.individual')}` : undefined} />
      {c === undefined ? (
        <div className="skeleton" style={{ height: 120 }} />
      ) : c === null ? (
        <>
          <p className="muted">{t('vehiculo.contact.error')}</p>
          <button
            className="btn btn-ghost btn-block"
            onClick={() => {
              setC(undefined)
              setIntento((n) => n + 1)
            }}
          >
            {t('common.retry')}
          </button>
        </>
      ) : (
        <>
          {c.medio_principal && (
            <div className="note info row">
              <Icon name={prefiereWa ? 'chat' : 'phone'} size={16} />
              {prefiereWa ? t('vehiculo.contact.prefersWa') : t('vehiculo.contact.prefersCall')}
            </div>
          )}
          {c.telefono && (
            <div className="card card-pad-sm">
              <span className="label">{t('vehiculo.contact.phone')}</span>
              <b style={{ display: 'block', fontSize: 'var(--fs-lg)' }} className="num ink">
                {c.telefono}
              </b>
              {whatsapp && (
                <span className="mid text-xs">
                  {t('vehiculo.contact.whatsapp', { n: whatsapp })}
                </span>
              )}
            </div>
          )}
          {c.correo && (
            <div className="card card-pad-sm">
              <span className="label">{t('vehiculo.contact.email')}</span>
              <b className="ink" style={{ display: 'block' }}>{c.correo}</b>
            </div>
          )}
          {whatsapp && (
            <a className="btn btn-teal btn-block btn-lg" href={whatsappUrl(whatsapp, mensaje)} target="_blank" rel="noreferrer" onClick={contar}>
              <Icon name="chat" size={18} />
              {t('vehiculo.contact.sendWa')}
            </a>
          )}
          {c.telefono && (
            <a className="btn btn-ghost btn-block btn-lg" href={`tel:${c.telefono.replace(/[^\d+]/g, '')}`} onClick={contar}>
              <Icon name="phone" size={18} />
              {t('vehiculo.contact.call')}
            </a>
          )}
          {!c.telefono && !whatsapp && <p className="muted">{t('vehiculo.contact.noPhone')}</p>}
          <p className="mid text-xs" style={{ whiteSpace: 'pre-line' }}>
            {t('vehiculo.contact.suggested', { m: mensaje })}
          </p>
          <GuiaSeguridad />
        </>
      )}
    </div>
  )
}

const GUIA: [Clave, Clave[]][] = [
  ['vehiculo.safety.whereTitle', ['vehiculo.safety.where1', 'vehiculo.safety.where2']],
  ['vehiculo.safety.checkTitle', ['vehiculo.safety.check1', 'vehiculo.safety.check2', 'vehiculo.safety.check3', 'vehiculo.safety.check4']],
  ['vehiculo.safety.payTitle', ['vehiculo.safety.pay1', 'vehiculo.safety.pay2', 'vehiculo.safety.pay3']],
]

/** Al contactar: el consejo principal siempre visible y la guía completa para ver el auto en persona. */
function GuiaSeguridad() {
  const { t } = useI18n()
  const [abierta, setAbierta] = useState(false)
  return (
    <div className="note warn ink">
      <div className="row gap-2" style={{ alignItems: 'flex-start' }}>
        <span style={{ color: 'var(--warning)', flex: 'none' }}>
          <Icon name="shield" size={18} />
        </span>
        <b>{t('vehiculo.safety.short')}</b>
      </div>
      {/* Oscuro y subrayado: el teal no contrasta lo suficiente sobre el fondo ámbar claro. */}
      <button
        className="linkbtn text-sm ink" style={{ marginTop: 'var(--s-2)', textDecoration: 'underline', minHeight: 32 }}
        aria-expanded={abierta}
        onClick={() => setAbierta((a) => !a)}
      >
        {t(abierta ? 'vehiculo.safety.less' : 'vehiculo.safety.more')}
      </button>
      {abierta &&
        GUIA.map(([titulo, puntos]) => (
          <div key={titulo} style={{ marginTop: 'var(--s-3)' }}>
            <b className="text-sm">{t(titulo)}</b>
            <ul style={{ margin: 'var(--s-1) 0 0', paddingLeft: 'var(--s-5)' }}>
              {puntos.map((p) => (
                <li key={p} style={{ margin: 'var(--s-1) 0' }}>
                  {t(p)}
                </li>
              ))}
            </ul>
          </div>
        ))}
    </div>
  )
}

// [motivo que se guarda en la BD (siempre en español: lo lee el panel de admin), texto a mostrar]
const RAZONES: Record<'publicacion' | 'cuenta', [string, Clave][]> = {
  publicacion: [
    ['Información engañosa o falsa', 'vehiculo.reason.misleading'],
    ['Sospecha de fraude o estafa', 'vehiculo.reason.fraud'],
    ['Vehículo ya vendido o no disponible', 'vehiculo.reason.sold'],
    ['Fotos de mala calidad, explícitas o robadas', 'vehiculo.reason.photos'],
    ['Publicación duplicada', 'vehiculo.reason.duplicate'],
    ['Otra razón', 'vehiculo.reason.other'],
  ],
  cuenta: [
    ['Vendedor sospechoso o posible estafador', 'vehiculo.reason.suspicious'],
    ['Suplantación de identidad', 'vehiculo.reason.impersonation'],
    ['Lenguaje ofensivo o acoso en mensajes', 'vehiculo.reason.harassment'],
    ['Incumplimiento de tratos o spam', 'vehiculo.reason.spam'],
    ['Otra razón', 'vehiculo.reason.other'],
  ],
}

function ReportModal({ carId }: { carId: number }) {
  const { account } = useSession()
  const { closeModal } = useUi()
  const { t } = useI18n()
  const [tipo, setTipo] = useState<'publicacion' | 'cuenta'>('publicacion')
  const [razon, setRazon] = useState('')
  const [detalles, setDetalles] = useState('')
  const [estado, setEstado] = useState<'editando' | 'enviando' | 'enviado'>('editando')
  const [error, setError] = useState<string | null>(null)

  if (estado === 'enviado') {
    return (
      <div className="modal centrada">
        <div className="nic done-icon">
          <Icon name="check" size={30} />
        </div>
        <ModalHead title={t('vehiculo.report.sentTitle')} sub={t('vehiculo.report.sentMsg')} />
        <button className="btn btn-primary btn-block" onClick={closeModal}>
          {t('vehiculo.gotIt')}
        </button>
      </div>
    )
  }

  const enviar = async () => {
    if (!account || !razon) return
    setEstado('enviando')
    setError(null)
    try {
      await reportListing(carId, account.id, tipo, razon, detalles)
      setEstado('enviado')
    } catch (e) {
      setError(e instanceof UserError ? e.message : t('vehiculo.report.error'))
      setEstado('editando')
    }
  }

  return (
    <div className="modal wide">
      <CargandoBloqueo visible={estado === 'enviando'} />
      <ModalHead title={t('vehiculo.report.title')} />
      {(['publicacion', 'cuenta'] as const).map((tp) => (
        <button
          key={tp}
          className={`opt ${tipo === tp ? 'on' : ''}`}
          onClick={() => {
            setTipo(tp)
            setRazon('')
          }}
        >
          <span className="radio" />
          <span>
            <b className="ink">{tp === 'publicacion' ? t('vehiculo.report.listing') : t('vehiculo.report.account')}</b>
            <br />
            <span className="muted text-sm">
              {tp === 'publicacion' ? t('vehiculo.report.listingDesc') : t('vehiculo.report.accountDesc')}
            </span>
          </span>
        </button>
      ))}
      <div className="label" style={{ marginTop: 'var(--s-2)' }}>
        {tipo === 'publicacion' ? t('vehiculo.report.reasonsListing') : t('vehiculo.report.reasonsAccount')}
      </div>
      {RAZONES[tipo].map(([r, clave]) => (
        <button key={r} className={`opt ${razon === r ? 'on' : ''}`} style={{ padding: 'var(--s-3) var(--s-4)' }} onClick={() => setRazon(r)}>
          <span className="radio" />
          <span className="ink">{t(clave)}</span>
        </button>
      ))}
      <textarea
        className="input"
        placeholder={t('vehiculo.report.placeholder')}
        value={detalles}
        maxLength={500}
        onChange={(e) => setDetalles(e.target.value)}
      />
      {error && <p className="text-sm" style={{ color: 'var(--error)' }}>{error}</p>}
      <div className="grid2">
        <button className="btn btn-ghost" onClick={closeModal}>
          {t('common.cancel')}
        </button>
        <button className="btn btn-danger" disabled={!razon || estado === 'enviando'} onClick={enviar}>
          {estado === 'enviando' ? t('vehiculo.report.sending') : t('vehiculo.report.send')}
        </button>
      </div>
    </div>
  )
}

export function Detail() {
  const { id } = useParams()
  const carId = Number(id)
  const navigate = useNavigate()
  const { account, mode } = useSession()
  const location = useLocation()
  /** Abierto con "Ver publicación" desde la revisión de Vehículos del panel de administración. */
  const origen = location.state as { revisionAdmin?: boolean; revisionReporte?: number } | null
  /** Abierto con "Ver publicación" desde la revisión de un reporte (id del reporte). */
  const desdeReporte = !!account?.rolPanel && typeof origen?.revisionReporte === 'number' ? origen.revisionReporte : null
  const desdeRevision = !!account?.rolPanel && desdeReporte == null && origen?.revisionAdmin === true
  const { toast, openModal } = useUi()
  const { t } = useI18n()
  const fav = useFavorites()
  const [detail, setDetail] = useState<CarDetail | null | undefined>(undefined)
  const [foto, setFoto] = useState(0)
  /** Visor ampliado abierto (con la foto principal). */
  const [visor, setVisor] = useState(false)
  const [similares, setSimilares] = useState<Car[]>([])
  /** La publicación con los datos de Mi panel, solo si es del usuario en sesión. */
  const [propia, setPropia] = useState<OwnCar | null>(null)
  const [oculta, setOcultaState] = useState(false)
  /** En inglés: descripción e imperfecciones traducidas automáticamente (lib/traductor.ts). */
  const [traduccion, setTraduccion] = useState<{ descripcion: string | null; problemas: string | null } | null>(null)
  const [verTraduccion, setVerTraduccion] = useState(true)
  // Escudo de verificado junto al nombre del vendedor (lote o particular).
  const [vendedorVerificado, setVendedorVerificado] = useState(false)
  const idVendedor = detail?.car.ownerId
  useEffect(() => {
    setVendedorVerificado(false)
    if (!idVendedor) return
    let vivo = true
    cuentaVerificada(idVendedor)
      .then((v) => vivo && setVendedorVerificado(v))
      .catch(() => {})
    return () => {
      vivo = false
    }
  }, [idVendedor])
  const { lang } = useI18n()
  const textoOriginal = detail ? `${detail.car.description}\n${detail.row.descripcion_problemas ?? ''}` : ''
  useEffect(() => {
    setTraduccion(null)
    if (lang !== 'en' || !detail || !textoOriginal.trim()) return
    let vivo = true
    void traduccionPublicacion(detail.car.description, detail.row.tiene_problemas ? detail.row.descripcion_problemas ?? '' : '').then(
      (tr) => vivo && setTraduccion(tr),
    )
    return () => {
      vivo = false
    }
    // Se vuelve a pedir si cambia el texto (edición aprobada) o el idioma.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lang, carId, textoOriginal])

  useEffect(() => {
    let vivo = true
    setDetail(undefined)
    setFoto(0)
    carDetail(carId, desdeRevision)
      .then((d) => {
        if (!vivo) return
        setDetail(d)
        if (d) {
          void registerView(carId, account?.id ?? null)
          visibleCars()
            .then((cs) => vivo && setSimilares(cs.filter((c) => c.bodyType === d.car.bodyType && c.id !== d.car.id).slice(0, 4)))
            .catch(() => {})
        }
      })
      .catch(() => vivo && setDetail(null))
    return () => {
      vivo = false
    }
    // registerView solo al abrir: no depende de la sesión.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [carId])

  const esDuenoId = !!account && !!detail && account.id === detail.car.ownerId
  const cargarPropia = useCallback(() => {
    if (!account || !esDuenoId) return setPropia(null)
    ocultas(account.id)
      .then((s) => setOcultaState(s.has(carId)))
      .catch(() => undefined)
    ownCar(carId)
      .then(setPropia)
      .catch(() => setPropia(null))
  }, [account, esDuenoId, carId])
  useEffect(() => {
    cargarPropia()
  }, [cargarPropia])

  // Cambios hechos en otro lado (la app, el admin): se vuelve a leer sin mostrar "cargando".
  const recargarDetalle = useCallback(() => {
    carDetail(carId, desdeRevision)
      .then((d) => d && setDetail(d))
      .catch(() => undefined)
    cargarPropia()
  }, [carId, desdeRevision, cargarPropia])
  useRecargar(recargarDetalle, ['publicaciones', 'propuestas_publicacion', 'fotos_publicacion', 'solicitudes_destacado', 'publicaciones_ocultas'])

  // Tras una acción del dueño se recargan el estado del detalle y los botones.
  const accionesDueno = useOwnerActions(() => {
    carDetail(carId, desdeRevision)
      .then((d) => d && setDetail(d))
      .catch(() => undefined)
    cargarPropia()
  })

  const specs = useMemo(() => {
    if (!detail) return []
    const c = detail.car
    const seguro = detail.row.recuperado_por_seguro
    const siNo = (v: boolean | null) => (v == null ? '—' : v ? t('vehiculo.yes') : t('vehiculo.no'))
    const condicion = detail.row.condicion_vehiculo
    const filas: [Clave, string | number][] = [
      ['vehiculo.spec.brand', c.brand],
      ['vehiculo.spec.model', c.model || '—'],
      ['vehiculo.spec.year', c.year || '—'],
      ['vehiculo.spec.transmission', nombreCatalogo(detail.transmision)],
      ['vehiculo.spec.mileage', km(c.km)],
      ['vehiculo.spec.condition', condicion === 'seminuevo' ? t('vehiculo.conditionSeminuevo') : condicion === 'usado' ? t('vehiculo.conditionUsado') : '—'],
      ['vehiculo.spec.fuel', nombreCatalogo(detail.combustible)],
      ['vehiculo.spec.engine', detail.motor],
      ['vehiculo.spec.cylinders', detail.cilindros],
      ['vehiculo.spec.bodyType', nombreCatalogo(detail.tipoCarro)],
      ['vehiculo.spec.color', nombreCatalogo(detail.color)],
      ['vehiculo.spec.owners', detail.duenos],
      ['vehiculo.spec.location', c.city || '—'],
      // Se arma aquí (no en api.ts) para que cambie al cambiar el idioma.
      ['vehiculo.spec.insurance', seguro == null ? '—' : seguro ? t('vehiculo.yes') : t('vehiculo.no')],
      ['vehiculo.spec.originalInvoice', siNo(detail.row.factura_original)],
      ['vehiculo.spec.airConditioning', siNo(detail.row.aire_acondicionado)],
    ]
    return filas
  }, [detail, t])

  if (detail === undefined) {
    return (
      <div className="wrap" style={{ paddingTop: 'var(--s-5)' }}>
        <div className="detailgrid">
          <div className="skeleton" style={{ height: 440 }} />
          <div className="skeleton" style={{ height: 360 }} />
        </div>
      </div>
    )
  }
  if (detail === null) {
    return (
      <div className="wrap wrap-narrow">
        <div className="card empty empty-page">
          <h1 className="empty-title">{t('vehiculo.detail.unavailable')}</h1>
          <p className="muted">{t('vehiculo.detail.unavailableMsg')}</p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            {t('vehiculo.detail.seeMore')}
          </button>
        </div>
      </div>
    )
  }

  const c = detail.car
  const esDueno = !!account && account.id === c.ownerId
  // El dueño no ve favorito, contactar ni reportar en sus propias publicaciones.
  const acciones = !esDueno
  // Vendido: aviso y sin "Contactar" (igual que la app).
  const vendido = c.status === 'Vendido'
  // Pausado por el dueño o deshabilitado por el equipo: quien lo tenía en favoritos lo ve
  // como "no disponible" (sin decir por qué). Al dueño se le muestran sus propios avisos.
  const fueraDeVenta = acciones && (vendido || c.status === 'Pausado' || c.status === 'Deshabilitado')
  const verParecidos = () => {
    const seccion = document.getElementById('similares')
    if (seccion) seccion.scrollIntoView({ behavior: 'smooth', block: 'start' })
    else navigate('/')
  }
  const abrirContacto = () => openModal(<ContactModal detail={detail} />)
  const fotos = detail.gallery.length ? detail.gallery : [null]
  /** Foto anterior/siguiente (flechas, teclado o deslizar), sin dar la vuelta. */
  const irAFoto = (i: number) => setFoto(Math.max(0, Math.min(fotos.length - 1, i)))
  const on = fav.isFavorite(c.id)
  // Al terminar el registro se regresa a este auto.
  const pedirRegistro = (texto: string) => toast(texto, { label: t('nav.registerMe'), onClick: () => navigate('/registro', { state: { from: location.pathname } }) })

  return (
    <div className="wrap">
      {/* Desde la revisión del admin, la barra de abajo ya trae su propio botón de regresar. */}
      {/* 20 px bajo el menú; el botón de volver va sobre la foto principal. */}
      <div style={{ height: 20 }} />

      {desdeReporte != null && (
        <AdminReportBar
          idReporte={desdeReporte}
          ownerId={c.ownerId}
          onChanged={() =>
            carDetail(carId, desdeRevision)
              .then((d) => d && setDetail(d))
              .catch(() => undefined)
          }
        />
      )}
      {desdeRevision && (
        <AdminReviewBar
          car={c}
          row={detail.row}
          onChanged={() =>
            carDetail(carId, desdeRevision)
              .then((d) => d && setDetail(d))
              .catch(() => undefined)
          }
        />
      )}

      {/* Solo el dueño: dejar claro si el público ya la ve. Nueva en revisión = nadie más la ve;
          ya aprobada con cambios en revisión = el público sigue viendo la versión anterior. */}
      {esDueno && (c.status === 'Pendiente' || (c.status === 'Activo' && propia?.inReview)) && (
        <div className="revisionaviso" role="status">
          <Icon name="lock" size={20} />
          <div>
            <b>{t(c.status === 'Pendiente' ? 'vehiculo.detail.notPublicTitle' : 'vehiculo.detail.changesReviewTitle')}</b>
            <p>{t(c.status === 'Pendiente' ? 'vehiculo.detail.notPublicDesc' : 'vehiculo.detail.changesReviewDesc')}</p>
          </div>
        </div>
      )}

      {/* Rechazada (o rechazados sus cambios) o deshabilitada: el dueño ve el motivo, como en la
          notificación. "Corregir y reenviar" está en los botones de abajo. */}
      {esDueno && propia && (propia.rejectedReason || (c.status === 'Deshabilitado' && propia.disabledReason)) && (
        <div className="revisionaviso peligro" role="status">
          <Icon name="alert" size={20} />
          <div>
            <b>
              {t(
                propia.rejectedReason
                  ? propia.approved
                    ? 'vehiculo.detail.changesRejectedTitle'
                    : 'vehiculo.detail.rejectedTitle'
                  : 'vehiculo.detail.disabledTitle',
              )}
            </b>
            {/* Lo escribe el equipo en español: en inglés se muestra traducido. */}
            <Traducido texto={(propia.rejectedReason ?? propia.disabledReason)!}>{(m) => <p>{t('perfil.reason', { motivo: m })}</p>}</Traducido>
          </div>
        </div>
      )}

      {/* Quien no es el dueño: el auto ya no está a la venta (vendido, pausado o deshabilitado). */}
      {fueraDeVenta && (
        <div className="revisionaviso neutro" role="status">
          <Icon name="alert" size={20} />
          <div>
            <b>{t(vendido ? 'vehiculo.detail.soldTitle' : 'vehiculo.detail.notAvailableTitle')}</b>
            <p>{t(vendido ? 'vehiculo.detail.soldMsg' : 'vehiculo.detail.notAvailableDesc')}</p>
          </div>
        </div>
      )}

      {esDueno && (
        <div className="card ownerbar">
          <div className="row" style={{ flexWrap: 'wrap' }}>
            <span className={`badge ${STATUS_BADGE[c.status]}`}>{t(statusLabel(c.status))}</span>
            <div className="grow muted text-sm">
              {c.status === 'Activo' ? t('vehiculo.detail.ownerVisible') : t('vehiculo.detail.ownerPreview')}
            </div>
          </div>
          <div className="acts">
            {propia && (
              <OwnerButtons
                c={propia}
                acciones={accionesDueno}
                oculta={oculta}
                onOcultar={function ocultar(v: boolean) {
                  setOcultaState(v)
                  setOculta(account!.id, c.id, v)
                    // Al ocultar se ofrece "Deshacer" (la vuelve a mostrar en el perfil).
                    .then(() => v && toast(t('perfil.hidden'), { label: t('common.undo'), onClick: () => ocultar(false) }))
                    .catch(() => setOcultaState(!v))
                }}
              />
            )}
            <button className="btn btn-soft btn-sm" onClick={() => navigate('/panel')}>
              <Icon name="chart" size={14} />
              {t('nav.myDashboard')}
            </button>
          </div>
        </div>
      )}

      {visor && (
        <PhotoViewer
          fotos={detail.gallery}
          inicio={Math.min(foto, detail.gallery.length - 1)}
          alt={`${c.brand} ${c.model}`}
          onClose={(i) => {
            setFoto(i)
            setVisor(false)
          }}
        />
      )}

      <div className="detailgrid">
        {/* En 1000 px o menos, .detailmain se "deshace" (display: contents) y el bloque de precio y
            contacto (aside) sube justo debajo de la galería con order (styles.css). */}
        <div className="col detailmain" style={{ gap: 'var(--s-5)' }}>
          {/* ← y → cambian de foto mientras el foco está en la galería. */}
          <div
            className="gallery"
            onKeyDown={(e) => {
              if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return
              e.preventDefault()
              irAFoto(foto + (e.key === 'ArrowLeft' ? -1 : 1))
            }}
          >
            <FotoPrincipal
              fotos={fotos}
              foto={foto}
              onCambiar={irAFoto}
              alt={`${c.brand} ${c.model}`}
              className={`${detail.gallery.length ? 'zoomable' : ''} ${fueraDeVenta ? 'fueradeventa' : ''}`}
              onAbrir={detail.gallery.length ? () => setVisor(true) : undefined}
            >
              {/* Desde la revisión del admin, la barra de arriba ya trae su propio botón de regresar. */}
              {desdeReporte == null && !desdeRevision && (
                <button
                  type="button"
                  className="iconbtn photoback"
                  aria-label={t('vehiculo.detail.backToListings')}
                  title={t('vehiculo.detail.backToListings')}
                  onClick={(e) => {
                    // Que no abra el visor de la foto de abajo.
                    e.stopPropagation()
                    // Desde Karsy regresa a la pantalla anterior (con sus filtros y scroll); por enlace, al listado.
                    if (hayPantallaAnterior()) navigate(-1)
                    else navigate('/', { replace: true })
                  }}
                >
                  <Icon name="back" size={18} />
                  <span>{t('vehiculo.detail.back')}</span>
                </button>
              )}
              {fueraDeVenta && <span className="tag badge b-neu">{t(vendido ? 'vehiculo.status.Vendido' : 'vehiculo.detail.notAvailableBadge')}</span>}
              {fotos.length > 1 && (
                <span className="counter">
                  {t('vehiculo.detail.photoCounter', { i: foto + 1, n: fotos.length })}
                </span>
              )}
              {!esDueno && (
                <button
                  className={`heart ${on ? 'on' : ''}`}
                  style={{ width: 42, height: 42, top: 14, right: 14 }}
                  aria-label={on ? t('vehiculo.fav.remove') : t('vehiculo.fav.add')}
                  aria-pressed={on}
                  onClick={(e) => {
                    // Que no abra el visor de la foto de abajo.
                    e.stopPropagation()
                    fav.toggle(c.id)
                  }}
                >
                  <Icon name="heart" size={18} />
                </button>
              )}
            </FotoPrincipal>
            {fotos.length > 1 && <Miniaturas fotos={fotos} foto={foto} onCambiar={irAFoto} />}
          </div>
          <div className="detailrest">
            <h2 className="card-title">{t('vehiculo.detail.specs')}</h2>
            <div className="ficha cols4">
              {specs.map(([k, v]) => (
                <div key={k}>
                  <span>{t(k)}</span>
                  <b>{v}</b>
                </div>
              ))}
            </div>
          </div>
          <div className="card detailrest card-pad">
            <h2 className="card-title">{t('vehiculo.detail.description')}</h2>
            <p className="muted" style={{ lineHeight: 1.7, whiteSpace: 'pre-line' }}>
              {(verTraduccion && traduccion?.descripcion) || c.description || t('vehiculo.detail.noDescription')}
            </p>
            {traduccion?.descripcion && <AvisoTraduccion viendoTraduccion={verTraduccion} onCambiar={() => setVerTraduccion((v) => !v)} />}
          </div>
          <div className="card detailrest card-pad">
            <h2 className="card-title">{t('vehiculo.detail.carDetails')}</h2>
            <p className="muted" style={{ whiteSpace: 'pre-line' }}>
              {detail.row.tiene_problemas ? (verTraduccion && traduccion?.problemas) || detail.detalles : t('vehiculo.noProblems')}
            </p>
          </div>
        </div>

        <aside className="card sidebox">
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <div className="row gap-2">
              <CompartirBoton c={c} />
              {acciones && (
                <button
                  className="iconbtn"
                  aria-label={t('vehiculo.detail.report')}
                  onClick={() => (mode === 'guest' ? pedirRegistro(t('vehiculo.detail.guestReport')) : openModal(<ReportModal carId={c.id} />))}
                >
                  <Icon name="flag" size={18} />
                </button>
              )}
            </div>
          </div>
          <div>
            <div className="detail-title">
              <h1 className="h1-page">
                {c.brand} {c.model}
              </h1>
              <ConditionTag condicion={detail.row.condicion_vehiculo} />
            </div>
            <div className="loc" style={{ marginTop: 'var(--s-2)' }}>
              <Icon name="pin" />
              {[c.city, c.year, km(c.km)].filter(Boolean).join(' · ')}
            </div>
          </div>
          <div>
            <span className={`price-xl num ${fueraDeVenta ? 'tachado' : ''}`}>{precio(c.priceValue)}</span> <span className="mid">{c.currency}</span>
          </div>
          {/* Ya no está a la venta: en lugar de contactar, autos parecidos. */}
          {fueraDeVenta ? (
            <button className="btn btn-primary btn-block btn-lg" onClick={verParecidos}>
              <Icon name="search" size={18} />
              {t('vehiculo.detail.seeSimilar')}
            </button>
          ) : (
            acciones && (
              <button
                className="btn btn-primary btn-block btn-lg"
                onClick={() => (mode === 'guest' ? pedirRegistro(t('vehiculo.detail.guestContact')) : abrirContacto())}
              >
                <Icon name="chat" size={18} />
                {t('vehiculo.detail.contactSeller')}
              </button>
            )
          )}
          <div className="label" style={{ marginTop: 'var(--s-1)' }}>
            {t('vehiculo.detail.sellerContact')}
          </div>
          <button className="seller" onClick={() => navigate(c.sellerType === 'lote' ? `/lotes/${c.ownerId}` : `/vendedor/${c.ownerId}`)}>
            <span className="sellerlogo" style={c.sellerType === 'lote' ? undefined : { borderRadius: '50%', background: '#2F7A80' }}>
              {detail.sellerAvatar ? <img src={detail.sellerAvatar} alt="" /> : iniciales(c.ownerName)}
            </span>
            <span className="grow">
              <span className="verified-name" style={{ gap: 'var(--s-2)' }}>
                <b className="ink">{c.ownerName}</b>
                {vendedorVerificado && <VerifiedShield />}
              </span>
              <span className="muted text-xs">
                {t('vehiculo.detail.viewProfile', { tipo: t(c.sellerType === 'lote' ? 'nav.lot' : 'nav.individual') })}
              </span>
            </span>
            <Icon name="chev" />
          </button>
          <p className="mid text-xs">
            {t('vehiculo.detail.disclaimer')}
          </p>
        </aside>
      </div>

      {similares.length > 0 && (
        <>
          <div className="sechead" id="similares">
            <h2>{t('vehiculo.detail.similar')}</h2>
          </div>
          <div className="vgrid">
            {similares.map((s) => (
              <VehicleCard key={s.id} car={s} />
            ))}
          </div>
        </>
      )}
    </div>
  )
}

/** "Traducido automáticamente · Ver original" (o "Ver traducción"). */
function AvisoTraduccion({ viendoTraduccion, onCambiar }: { viendoTraduccion: boolean; onCambiar: () => void }) {
  const { t } = useI18n()
  return (
    <p className="mid text-xs" style={{ marginTop: 'var(--s-3)', display: 'flex', gap: 'var(--s-2)', alignItems: 'center', flexWrap: 'wrap' }}>
      <Icon name="globe" size={14} />
      {viendoTraduccion ? t('vehiculo.detail.translated') : t('vehiculo.detail.original')}
      <button className="linkbtn text-xs" onClick={onCambiar}>
        {viendoTraduccion ? t('vehiculo.detail.seeOriginal') : t('vehiculo.detail.seeTranslation')}
      </button>
    </p>
  )
}
