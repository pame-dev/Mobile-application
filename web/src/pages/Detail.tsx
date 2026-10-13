import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { CarPhoto } from '../components/CarPhoto'
import { Icon } from '../components/Icon'
import { ModalHead, useUi } from '../components/Ui'
import { VehicleCard } from '../components/VehicleCard'
import { carDetail, registerView, reportListing, sellerContact, UserError, visibleCars } from '../data/api'
import type { Car, CarDetail, SellerContact } from '../data/types'
import { iniciales, km, precio, whatsappUrl } from '../lib/format'
import { useFavorites } from '../session/FavoritesContext'
import { useSession } from '../session/SessionContext'

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
  const [c, setC] = useState<SellerContact | null | undefined>(undefined)
  useEffect(() => {
    sellerContact(detail.car.id, account?.id ?? null).then(setC).catch(() => setC(null))
  }, [detail.car.id, account?.id])

  const car = detail.car
  const mensaje = `Hola, me interesa tu ${car.brand} ${car.model} ${car.year} que vi en Karsy.`
  const whatsapp = c?.numero_whatsapp ?? (c?.whatsapp ? c.telefono : null)
  const prefiereWa = c?.medio_principal === 'whatsapp'
  return (
    <div className="modal">
      <ModalHead title="Información de contacto" sub={c ? `${c.nombre} · ${c.tipo_cuenta === 'lote' ? 'Lote' : 'Particular'}` : undefined} />
      {c === undefined ? (
        <div className="skeleton" style={{ height: 120 }} />
      ) : c === null ? (
        <p className="muted">No pudimos obtener los datos del vendedor. Intenta de nuevo.</p>
      ) : (
        <>
          {c.medio_principal && (
            <div style={{ padding: '12px 14px', borderRadius: 12, background: 'var(--teal-light)', fontSize: 13 }} className="row">
              <Icon name={prefiereWa ? 'chat' : 'phone'} size={16} />
              {prefiereWa ? 'Prefiere que le escribas por WhatsApp' : 'Prefiere que le llames'}
            </div>
          )}
          {c.telefono && (
            <div className="card" style={{ padding: '14px 16px' }}>
              <span className="label">Teléfono</span>
              <b style={{ display: 'block', fontSize: 18, color: 'var(--ink)' }} className="num">
                {c.telefono}
              </b>
              {whatsapp && (
                <span className="mid" style={{ fontSize: 12.5 }}>
                  WhatsApp: {whatsapp}
                </span>
              )}
            </div>
          )}
          {c.correo && (
            <div className="card" style={{ padding: '14px 16px' }}>
              <span className="label">Correo</span>
              <b style={{ display: 'block', color: 'var(--ink)' }}>{c.correo}</b>
            </div>
          )}
          {whatsapp && (
            <a className="btn btn-teal btn-block" style={{ height: 48 }} href={whatsappUrl(whatsapp, mensaje)} target="_blank" rel="noreferrer">
              <Icon name="chat" size={18} />
              Enviar WhatsApp
            </a>
          )}
          {c.telefono && (
            <a className="btn btn-ghost btn-block" style={{ height: 48 }} href={`tel:${c.telefono.replace(/[^\d+]/g, '')}`}>
              <Icon name="phone" size={18} />
              Llamar al vendedor
            </a>
          )}
          {!c.telefono && !whatsapp && <p className="muted">El vendedor no registró un teléfono.</p>}
          <p className="mid" style={{ fontSize: 12 }}>
            Mensaje sugerido: “{mensaje}”
          </p>
        </>
      )}
    </div>
  )
}

const RAZONES = {
  publicacion: [
    'Información engañosa o falsa',
    'Sospecha de fraude o estafa',
    'Vehículo ya vendido o no disponible',
    'Fotos de mala calidad, explícitas o robadas',
    'Publicación duplicada',
    'Otra razón',
  ],
  cuenta: [
    'Vendedor sospechoso o posible estafador',
    'Suplantación de identidad',
    'Lenguaje ofensivo o acoso en mensajes',
    'Incumplimiento de tratos o spam',
    'Otra razón',
  ],
}

function ReportModal({ carId }: { carId: number }) {
  const { account } = useSession()
  const { closeModal } = useUi()
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
        <h3>Reporte enviado</h3>
        <p className="muted">Gracias por tu reporte. Nuestro equipo de administración revisará la información a la brevedad.</p>
        <button className="btn btn-primary btn-block" onClick={closeModal}>
          Entendido
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
      setError(e instanceof UserError ? e.message : 'No se pudo enviar el reporte. Intenta de nuevo.')
      setEstado('editando')
    }
  }

  return (
    <div className="modal wide">
      <ModalHead title="¿Qué deseas reportar?" />
      {(['publicacion', 'cuenta'] as const).map((t) => (
        <button
          key={t}
          className={`opt ${tipo === t ? 'on' : ''}`}
          onClick={() => {
            setTipo(t)
            setRazon('')
          }}
        >
          <span className="radio" />
          <span>
            <b style={{ color: 'var(--ink)' }}>{t === 'publicacion' ? 'Reportar publicación' : 'Reportar cuenta'}</b>
            <br />
            <span className="muted" style={{ fontSize: 13 }}>
              {t === 'publicacion'
                ? 'Información falsa, fotos inapropiadas o posible fraude en este vehículo.'
                : 'Comportamiento sospechoso, suplantación o acoso por parte del vendedor.'}
            </span>
          </span>
        </button>
      ))}
      <div className="label" style={{ marginTop: 6 }}>
        {tipo === 'publicacion' ? 'Razones para reportar esta publicación' : 'Razones para reportar esta cuenta'}
      </div>
      {RAZONES[tipo].map((r) => (
        <button key={r} className={`opt ${razon === r ? 'on' : ''}`} style={{ padding: '10px 14px' }} onClick={() => setRazon(r)}>
          <span className="radio" />
          <span style={{ color: 'var(--ink)' }}>{r}</span>
        </button>
      ))}
      <textarea
        className="input"
        placeholder="Escribe detalles adicionales que ayuden a la revisión (opcional)..."
        value={detalles}
        maxLength={500}
        onChange={(e) => setDetalles(e.target.value)}
      />
      {error && <p style={{ color: 'var(--error)', fontSize: 13 }}>{error}</p>}
      <div className="grid2">
        <button className="btn btn-ghost" onClick={closeModal}>
          Cancelar
        </button>
        <button className="btn btn-danger" disabled={!razon || estado === 'enviando'} onClick={enviar}>
          {estado === 'enviando' ? 'Enviando…' : 'Enviar reporte'}
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
  const fav = useFavorites()
  const [detail, setDetail] = useState<CarDetail | null | undefined>(undefined)
  const [foto, setFoto] = useState(0)
  const [similares, setSimilares] = useState<Car[]>([])

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

  const specs = useMemo(() => {
    if (!detail) return []
    const c = detail.car
    return [
      ['Marca', c.brand],
      ['Modelo', c.model || '—'],
      ['Año', c.year || '—'],
      ['Transmisión', detail.transmision],
      ['Kilometraje', km(c.km)],
      ['Combustible', detail.combustible],
      ['Motor', detail.motor],
      ['Cilindros', detail.cilindros],
      ['Tipo de carro', detail.tipoCarro],
      ['Color', detail.color],
      ['Dueños anteriores', detail.duenos],
      ['Recuperado por el seguro', detail.recuperadoPorSeguro],
    ] as const
  }, [detail])

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
          <h2>Publicación no disponible</h2>
          <p className="muted">Puede que se haya vendido, que esté en revisión o que ya no exista.</p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            Ver más vehículos
          </button>
        </div>
      </div>
    )
  }

  const c = detail.car
  const esDueno = !!account && account.id === c.ownerId
  // Igual que la app: el dueño y el admin no ven favorito, contactar ni reportar.
  const acciones = mode !== 'admin' && !esDueno
  const fotos = detail.gallery.length ? detail.gallery : [null]
  const on = fav.isFavorite(c.id)
  const seminuevo = c.year >= new Date().getFullYear() - 3
  const pedirRegistro = (texto: string) => toast(texto, { label: 'Registrarme', onClick: () => navigate('/registro') })

  const compartir = async () => {
    const url = window.location.href
    try {
      if (navigator.share) await navigator.share({ title: `${c.brand} ${c.model} ${c.year}`, url })
      else {
        await navigator.clipboard.writeText(url)
        toast('Enlace copiado')
      }
    } catch {
      /* el usuario canceló */
    }
  }

  return (
    <div className="wrap">
      <div className="crumbs">
        <button onClick={() => navigate('/')}>Inicio</button>
        {c.bodyType && (
          <>
            <Icon name="chev" size={14} />
            <button onClick={() => navigate(`/?tipo=${encodeURIComponent(c.bodyType)}`)}>{c.bodyType}</button>
          </>
        )}
        <Icon name="chev" size={14} />
        <span style={{ color: 'var(--ink)' }}>
          {c.brand} {c.model}
        </span>
      </div>

      {esDueno && (
        <div className="card row" style={{ padding: '14px 18px', marginBottom: 18 }}>
          <span className={`badge ${STATUS_BADGE[c.status]}`}>{c.status}</span>
          <div className="grow muted" style={{ fontSize: 13 }}>
            {c.status === 'Activo' ? 'Tu publicación está visible para los compradores.' : 'Así se verá tu publicación para los compradores.'}
          </div>
        </div>
      )}

      <div className="detailgrid">
        <div className="col" style={{ gap: 22 }}>
          <div className="gallery">
            <CarPhoto src={fotos[foto]} alt={`${c.brand} ${c.model}`} className="main">
              {fotos.length > 1 && (
                <span className="counter">
                  {foto + 1} de {fotos.length}
                </span>
              )}
              {acciones && (
                <button
                  className={`heart ${on ? 'on' : ''}`}
                  style={{ width: 42, height: 42, top: 14, right: 14 }}
                  aria-label={on ? 'Quitar de favoritos' : 'Guardar en favoritos'}
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
                  <CarPhoto key={i} src={src} className={i === foto ? 'on' : ''} onClick={() => setFoto(i)} alt={`Foto ${i + 1}`} />
                ))}
              </div>
            )}
          </div>
          <div>
            <h3 style={{ fontSize: 19, marginBottom: 12 }}>Ficha técnica</h3>
            <div className="ficha">
              {specs.map(([k, v]) => (
                <div key={k}>
                  <span>{k}</span>
                  <b>{v}</b>
                </div>
              ))}
            </div>
          </div>
          <div className="card" style={{ padding: 22 }}>
            <h3 style={{ fontSize: 17, marginBottom: 8 }}>Descripción</h3>
            <p className="muted" style={{ lineHeight: 1.7, whiteSpace: 'pre-line' }}>
              {c.description || 'El vendedor no agregó una descripción.'}
            </p>
          </div>
          <div className="card" style={{ padding: 22 }}>
            <h3 style={{ fontSize: 17, marginBottom: 8 }}>Detalles del auto</h3>
            <p className="muted" style={{ whiteSpace: 'pre-line' }}>
              {detail.detalles}
            </p>
          </div>
        </div>

        <aside className="card sidebox">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <span className="badge b-info">{seminuevo ? 'Seminuevo' : 'Usado'}</span>
            <div className="row" style={{ gap: 8 }}>
              <button className="iconbtn" aria-label="Compartir" onClick={compartir}>
                <Icon name="share" size={18} />
              </button>
              {acciones && (
                <button
                  className="iconbtn"
                  aria-label="Reportar"
                  onClick={() => (mode === 'guest' ? pedirRegistro('Regístrate para realizar un reporte') : openModal(<ReportModal carId={c.id} />))}
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
              onClick={() => (mode === 'guest' ? pedirRegistro('Regístrate para contactar al vendedor') : openModal(<ContactModal detail={detail} />))}
            >
              <Icon name="chat" size={18} />
              Contactar vendedor
            </button>
          )}
          <div className="label" style={{ marginTop: 4 }}>
            Contacto del vendedor
          </div>
          <button className="seller" onClick={() => navigate(c.sellerType === 'lote' ? `/lotes/${c.ownerId}` : `/vendedor/${c.ownerId}`)}>
            <span className="sellerlogo" style={c.sellerType === 'lote' ? undefined : { borderRadius: '50%', background: 'var(--teal)' }}>
              {detail.sellerAvatar ? <img src={detail.sellerAvatar} alt="" /> : iniciales(c.ownerName)}
            </span>
            <span className="grow">
              <b style={{ color: 'var(--ink)' }}>{c.ownerName}</b>
              <br />
              <span className="muted" style={{ fontSize: 12.5 }}>
                {c.sellerType === 'lote' ? 'Lote' : 'Particular'} · Ver perfil
              </span>
            </span>
            <Icon name="chev" />
          </button>
          <p className="mid" style={{ fontSize: 12 }}>
            Los acuerdos, pagos y entregas se realizan directamente entre comprador y vendedor. Revisa el vehículo antes de pagar.
          </p>
        </aside>
      </div>

      {similares.length > 0 && (
        <>
          <div className="sechead">
            <h2>Vehículos similares</h2>
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
