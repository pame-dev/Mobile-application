import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { CarPhoto } from '../components/CarPhoto'
import { Icon } from '../components/Icon'
import { OwnerButtons, statusLabel, useOwnerActions } from '../components/OwnerActions'
import { ModalHead, useUi } from '../components/Ui'
import { VehicleCard } from '../components/VehicleCard'
import { carDetail, registerView, reportListing, sellerContact, UserError, visibleCars } from '../data/api'
import { ocultas, ownCar, setOculta, type OwnCar } from '../data/seller'
import type { Car, CarDetail, SellerContact } from '../data/types'
import { useI18n } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { nombreCatalogo } from '../lib/catalogo'
import { iniciales, km, precio, whatsappUrl } from '../lib/format'
import { useFavorites } from '../session/FavoritesContext'
import { useSession } from '../session/SessionContext'
import { useRecargar } from '../lib/tiempoReal'

const STATUS_BADGE: Record<Car['status'], string> = {
  Activo: 'b-succ',
  Pendiente: 'b-warn',
  Rechazado: 'b-dang',
  Deshabilitado: 'b-neu',
  Vendido: 'b-info',
  Pausado: 'b-neu',
}

function ContactModal({ detail }: { detail: CarDetail }) {
  const { account } = useSession()
  const { t } = useI18n()
  const [c, setC] = useState<SellerContact | null | undefined>(undefined)
  useEffect(() => {
    sellerContact(detail.car.id, account?.id ?? null).then(setC).catch(() => setC(null))
  }, [detail.car.id, account?.id])

  const car = detail.car
  const mensaje = t('vehiculo.contact.message', { v: `${car.brand} ${car.model} ${car.year}` })
  const whatsapp = c?.numero_whatsapp ?? (c?.whatsapp ? c.telefono : null)
  const prefiereWa = c?.medio_principal === 'whatsapp'
  return (
    <div className="modal">
      <ModalHead title={t('vehiculo.contact.title')} sub={c ? `${c.nombre} · ${t(c.tipo_cuenta === 'lote' ? 'nav.lot' : 'nav.individual')}` : undefined} />
      {c === undefined ? (
        <div className="skeleton" style={{ height: 120 }} />
      ) : c === null ? (
        <p className="muted">{t('vehiculo.contact.error')}</p>
      ) : (
        <>
          {c.medio_principal && (
            <div style={{ padding: '12px 14px', borderRadius: 12, background: 'var(--teal-light)', fontSize: 13 }} className="row">
              <Icon name={prefiereWa ? 'chat' : 'phone'} size={16} />
              {prefiereWa ? t('vehiculo.contact.prefersWa') : t('vehiculo.contact.prefersCall')}
            </div>
          )}
          {c.telefono && (
            <div className="card" style={{ padding: '14px 16px' }}>
              <span className="label">{t('vehiculo.contact.phone')}</span>
              <b style={{ display: 'block', fontSize: 18, color: 'var(--ink)' }} className="num">
                {c.telefono}
              </b>
              {whatsapp && (
                <span className="mid" style={{ fontSize: 12.5 }}>
                  {t('vehiculo.contact.whatsapp', { n: whatsapp })}
                </span>
              )}
            </div>
          )}
          {c.correo && (
            <div className="card" style={{ padding: '14px 16px' }}>
              <span className="label">{t('vehiculo.contact.email')}</span>
              <b style={{ display: 'block', color: 'var(--ink)' }}>{c.correo}</b>
            </div>
          )}
          {whatsapp && (
            <a className="btn btn-teal btn-block" style={{ height: 48 }} href={whatsappUrl(whatsapp, mensaje)} target="_blank" rel="noreferrer">
              <Icon name="chat" size={18} />
              {t('vehiculo.contact.sendWa')}
            </a>
          )}
          {c.telefono && (
            <a className="btn btn-ghost btn-block" style={{ height: 48 }} href={`tel:${c.telefono.replace(/[^\d+]/g, '')}`}>
              <Icon name="phone" size={18} />
              {t('vehiculo.contact.call')}
            </a>
          )}
          {!c.telefono && !whatsapp && <p className="muted">{t('vehiculo.contact.noPhone')}</p>}
          <p className="mid" style={{ fontSize: 12 }}>
            {t('vehiculo.contact.suggested', { m: mensaje })}
          </p>
        </>
      )}
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
      <div className="modal" style={{ textAlign: 'center', alignItems: 'center' }}>
        <div className="nic" style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--success-l)', color: 'var(--success)' }}>
          <Icon name="check" size={30} />
        </div>
        <h3>{t('vehiculo.report.sentTitle')}</h3>
        <p className="muted">{t('vehiculo.report.sentMsg')}</p>
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
            <b style={{ color: 'var(--ink)' }}>{tp === 'publicacion' ? t('vehiculo.report.listing') : t('vehiculo.report.account')}</b>
            <br />
            <span className="muted" style={{ fontSize: 13 }}>
              {tp === 'publicacion' ? t('vehiculo.report.listingDesc') : t('vehiculo.report.accountDesc')}
            </span>
          </span>
        </button>
      ))}
      <div className="label" style={{ marginTop: 6 }}>
        {tipo === 'publicacion' ? t('vehiculo.report.reasonsListing') : t('vehiculo.report.reasonsAccount')}
      </div>
      {RAZONES[tipo].map(([r, clave]) => (
        <button key={r} className={`opt ${razon === r ? 'on' : ''}`} style={{ padding: '10px 14px' }} onClick={() => setRazon(r)}>
          <span className="radio" />
          <span style={{ color: 'var(--ink)' }}>{t(clave)}</span>
        </button>
      ))}
      <textarea
        className="input"
        placeholder={t('vehiculo.report.placeholder')}
        value={detalles}
        maxLength={500}
        onChange={(e) => setDetalles(e.target.value)}
      />
      {error && <p style={{ color: 'var(--error)', fontSize: 13 }}>{error}</p>}
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
  const { toast, openModal } = useUi()
  const { t } = useI18n()
  const fav = useFavorites()
  const [detail, setDetail] = useState<CarDetail | null | undefined>(undefined)
  const [foto, setFoto] = useState(0)
  const [similares, setSimilares] = useState<Car[]>([])
  /** La publicación con los datos de Mi panel, solo si es del usuario en sesión. */
  const [propia, setPropia] = useState<OwnCar | null>(null)
  const [oculta, setOcultaState] = useState(false)

  useEffect(() => {
    let vivo = true
    setDetail(undefined)
    setFoto(0)
    carDetail(carId)
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
    setOcultaState(ocultas(account.id).has(carId))
    ownCar(carId)
      .then(setPropia)
      .catch(() => setPropia(null))
  }, [account, esDuenoId, carId])
  useEffect(() => {
    cargarPropia()
  }, [cargarPropia])

  // Cambios hechos en otro lado (la app, el admin): se vuelve a leer sin mostrar "cargando".
  const recargarDetalle = useCallback(() => {
    carDetail(carId)
      .then((d) => d && setDetail(d))
      .catch(() => undefined)
    cargarPropia()
  }, [carId, cargarPropia])
  useRecargar(recargarDetalle, ['publicaciones', 'propuestas_publicacion', 'fotos_publicacion', 'solicitudes_destacado'])

  // Tras una acción del dueño se recargan el estado del detalle y los botones.
  const accionesDueno = useOwnerActions(() => {
    carDetail(carId)
      .then((d) => d && setDetail(d))
      .catch(() => undefined)
    cargarPropia()
  })

  const specs = useMemo(() => {
    if (!detail) return []
    const c = detail.car
    const seguro = detail.row.recuperado_por_seguro
    const filas: [Clave, string | number][] = [
      ['vehiculo.spec.brand', c.brand],
      ['vehiculo.spec.model', c.model || '—'],
      ['vehiculo.spec.year', c.year || '—'],
      ['vehiculo.spec.transmission', nombreCatalogo(detail.transmision)],
      ['vehiculo.spec.mileage', km(c.km)],
      ['vehiculo.spec.fuel', nombreCatalogo(detail.combustible)],
      ['vehiculo.spec.engine', detail.motor],
      ['vehiculo.spec.cylinders', detail.cilindros],
      ['vehiculo.spec.bodyType', nombreCatalogo(detail.tipoCarro)],
      ['vehiculo.spec.color', nombreCatalogo(detail.color)],
      ['vehiculo.spec.owners', detail.duenos],
      // Se arma aquí (no en api.ts) para que cambie al cambiar el idioma.
      ['vehiculo.spec.insurance', seguro == null ? '—' : seguro ? t('vehiculo.yes') : t('vehiculo.no')],
    ]
    return filas
  }, [detail, t])

  if (detail === undefined) {
    return (
      <div className="wrap" style={{ paddingTop: 20 }}>
        <div className="detailgrid">
          <div className="skeleton" style={{ height: 440 }} />
          <div className="skeleton" style={{ height: 360 }} />
        </div>
      </div>
    )
  }
  if (detail === null) {
    return (
      <div className="wrap" style={{ maxWidth: 820 }}>
        <div className="card empty" style={{ marginTop: 40 }}>
          <h2>{t('vehiculo.detail.unavailable')}</h2>
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
  const fotos = detail.gallery.length ? detail.gallery : [null]
  const on = fav.isFavorite(c.id)
  const seminuevo = c.year >= new Date().getFullYear() - 3
  const pedirRegistro = (texto: string) => toast(texto, { label: t('nav.registerMe'), onClick: () => navigate('/registro') })

  const compartir = async () => {
    // Enlace limpio: en WhatsApp muestra la foto y el precio (Edge Function vista-previa) y,
    // en un Android con Karsy instalada, abre la app en este auto.
    const url = `${window.location.origin}/vehiculo/${c.id}`
    try {
      if (navigator.share) await navigator.share({ title: `${c.brand} ${c.model} ${c.year}`, text: t('vehiculo.detail.shareText'), url })
      else {
        await navigator.clipboard.writeText(url)
        toast(t('vehiculo.detail.linkCopied'))
      }
    } catch {
      /* el usuario canceló */
    }
  }

  return (
    <div className="wrap">
      <div className="crumbs">
        <button onClick={() => navigate('/')}>{t('nav.home')}</button>
        {c.bodyType && (
          <>
            <Icon name="chev" size={14} />
            <button onClick={() => navigate(`/?tipo=${encodeURIComponent(c.bodyType)}`)}>{nombreCatalogo(c.bodyType)}</button>
          </>
        )}
        <Icon name="chev" size={14} />
        <span style={{ color: 'var(--ink)' }}>
          {c.brand} {c.model}
        </span>
      </div>

      {esDueno && (
        <div className="card ownerbar">
          <div className="row" style={{ flexWrap: 'wrap' }}>
            <span className={`badge ${STATUS_BADGE[c.status]}`}>{t(statusLabel(c.status))}</span>
            <div className="grow muted" style={{ fontSize: 13 }}>
              {c.status === 'Activo' ? t('vehiculo.detail.ownerVisible') : t('vehiculo.detail.ownerPreview')}
            </div>
          </div>
          <div className="acts">
            {propia && (
              <OwnerButtons
                c={propia}
                acciones={accionesDueno}
                oculta={oculta}
                onOcultar={(v) => {
                  setOculta(account!.id, c.id, v)
                  setOcultaState(v)
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

      <div className="detailgrid">
        <div className="col" style={{ gap: 22 }}>
          <div className="gallery">
            <CarPhoto src={fotos[foto]} alt={`${c.brand} ${c.model}`} className="main">
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
                  onClick={() => fav.toggle(c.id)}
                >
                  <Icon name="heart" size={19} />
                </button>
              )}
            </CarPhoto>
            {fotos.length > 1 && (
              <div className="thumbs">
                {fotos.map((src, i) => (
                  <CarPhoto key={i} src={src} className={i === foto ? 'on' : ''} onClick={() => setFoto(i)} alt={t('vehiculo.detail.photoAlt', { n: i + 1 })} />
                ))}
              </div>
            )}
          </div>
          <div>
            <h3 style={{ fontSize: 19, marginBottom: 12 }}>{t('vehiculo.detail.specs')}</h3>
            <div className="ficha">
              {specs.map(([k, v]) => (
                <div key={k}>
                  <span>{t(k)}</span>
                  <b>{v}</b>
                </div>
              ))}
            </div>
          </div>
          <div className="card" style={{ padding: 22 }}>
            <h3 style={{ fontSize: 17, marginBottom: 8 }}>{t('vehiculo.detail.description')}</h3>
            <p className="muted" style={{ lineHeight: 1.7, whiteSpace: 'pre-line' }}>
              {c.description || t('vehiculo.detail.noDescription')}
            </p>
          </div>
          <div className="card" style={{ padding: 22 }}>
            <h3 style={{ fontSize: 17, marginBottom: 8 }}>{t('vehiculo.detail.carDetails')}</h3>
            <p className="muted" style={{ whiteSpace: 'pre-line' }}>
              {detail.row.tiene_problemas ? detail.detalles : t('vehiculo.noProblems')}
            </p>
          </div>
        </div>

        <aside className="card sidebox">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <span className="badge b-info">{seminuevo ? t('vehiculo.detail.nearlyNew') : t('vehiculo.detail.used')}</span>
            <div className="row" style={{ gap: 8 }}>
              <button className="iconbtn" aria-label={t('vehiculo.detail.share')} onClick={compartir}>
                <Icon name="share" size={18} />
              </button>
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
            <h1 style={{ fontSize: 28 }}>
              {c.brand} {c.model}
            </h1>
            <div className="loc" style={{ marginTop: 6 }}>
              <Icon name="pin" />
              {[c.city, c.year, km(c.km)].filter(Boolean).join(' · ')}
            </div>
          </div>
          <div>
            <span className="price-xl num">{precio(c.priceValue)}</span> <span className="mid">{c.currency}</span>
          </div>
          {acciones && (
            <button
              className="btn btn-primary btn-block"
              style={{ height: 52 }}
              onClick={() => (mode === 'guest' ? pedirRegistro(t('vehiculo.detail.guestContact')) : openModal(<ContactModal detail={detail} />))}
            >
              <Icon name="chat" size={18} />
              {t('vehiculo.detail.contactSeller')}
            </button>
          )}
          <div className="label" style={{ marginTop: 4 }}>
            {t('vehiculo.detail.sellerContact')}
          </div>
          <button className="seller" onClick={() => navigate(c.sellerType === 'lote' ? `/lotes/${c.ownerId}` : `/vendedor/${c.ownerId}`)}>
            <span className="sellerlogo" style={c.sellerType === 'lote' ? undefined : { borderRadius: '50%', background: 'var(--teal)' }}>
              {detail.sellerAvatar ? <img src={detail.sellerAvatar} alt="" /> : iniciales(c.ownerName)}
            </span>
            <span className="grow">
              <b style={{ color: 'var(--ink)' }}>{c.ownerName}</b>
              <br />
              <span className="muted" style={{ fontSize: 12.5 }}>
                {t('vehiculo.detail.viewProfile', { tipo: t(c.sellerType === 'lote' ? 'nav.lot' : 'nav.individual') })}
              </span>
            </span>
            <Icon name="chev" />
          </button>
          <p className="mid" style={{ fontSize: 12 }}>
            {t('vehiculo.detail.disclaimer')}
          </p>
        </aside>
      </div>

      {similares.length > 0 && (
        <>
          <div className="sechead">
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
