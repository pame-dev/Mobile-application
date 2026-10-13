import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { Inventory, ProfileHeader } from '../components/ProfileParts'
import { useUi } from '../components/Ui'
import { lot as cargarLote, lotContact, ownerVisibleCars } from '../data/api'
import type { Car, Lot, LotContact } from '../data/types'
import { whatsappUrl } from '../lib/format'
import { useSession } from '../session/SessionContext'

/** Perfil de un lote: datos, contacto e inventario visible (LotProfileScreen de la app). */
export function LotProfile() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { account, mode } = useSession()
  const { toast } = useUi()
  const [lote, setLote] = useState<Lot | null | undefined>(undefined)
  const [cars, setCars] = useState<Car[]>([])
  const [contacto, setContacto] = useState<LotContact | null>(null)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(() => {
    setError(null)
    Promise.all([cargarLote(id), ownerVisibleCars(id)])
      .then(([l, inventario]) => {
        setLote(l ? { ...l, vehicleCount: inventario.length } : null)
        setCars(inventario)
      })
      .catch(() => setError('No pudimos cargar este lote. Revisa tu conexión e intenta de nuevo.'))
  }, [id])

  useEffect(() => {
    cargar()
  }, [cargar])

  // El contacto no bloquea la pantalla; si falla, los botones quedan deshabilitados.
  useEffect(() => {
    lotContact(id, account?.id ?? null)
      .then(setContacto)
      .catch(() => setContacto(null))
  }, [id, account?.id])

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
  if (lote === undefined) {
    return (
      <div className="wrap" style={{ paddingTop: 24 }}>
        <div className="skeleton" style={{ height: 210, borderRadius: 22 }} />
        <div className="skeleton" style={{ height: 300, marginTop: 24 }} />
      </div>
    )
  }
  if (lote === null) {
    return (
      <div className="wrap" style={{ maxWidth: 820 }}>
        <div className="card empty" style={{ marginTop: 40 }}>
          <h2>No encontramos este lote.</h2>
          <button className="btn btn-primary" onClick={() => navigate('/lotes')}>
            Ver lotes
          </button>
        </div>
      </div>
    )
  }

  const invitado = mode === 'guest'
  const pedirRegistro = () => toast('Regístrate para contactar al vendedor', { label: 'Registrarme', onClick: () => navigate('/registro') })
  // Con sesión y sin número registrado, el botón se ve deshabilitado.
  const puedeLlamar = invitado || !!contacto?.phone
  const puedeWhatsapp = invitado || !!contacto?.whatsapp
  // Google Maps busca el lote por nombre y dirección.
  const mapa = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent([lote.name, lote.address || lote.city].join(', '))}`

  return (
    <div className="wrap" style={{ paddingTop: 24 }}>
      <div className="crumbs" style={{ paddingTop: 0 }}>
        <button onClick={() => navigate('/lotes')}>Lotes</button>
        <Icon name="chev" size={14} />
        <span style={{ color: 'var(--ink)' }}>{lote.name}</span>
      </div>

      <ProfileHeader name={lote.name} avatarUrl={lote.logoUrl} coverUrl={lote.coverUrl} square>
        <div className="row" style={{ flexWrap: 'wrap' }}>
          <h1 style={{ fontSize: 28 }}>{lote.name}</h1>
          <span className="badge b-info">Lote</span>
        </div>
        <p className="muted">
          {lote.city}
          {lote.city && ' · '}
          {lote.vehicleCount} {lote.vehicleCount === 1 ? 'vehículo en venta' : 'vehículos en venta'}
        </p>
      </ProfileHeader>

      <div className="twocol" style={{ marginTop: 24 }}>
        <aside className="card" style={{ padding: 22, display: 'flex', flexDirection: 'column', gap: 14 }}>
          {lote.description && <p className="muted">{lote.description}</p>}
          <div className="row" style={{ flexWrap: 'wrap', gap: 8 }}>
            {invitado || !contacto?.phone ? (
              <button className="btn btn-primary btn-sm" aria-disabled={!puedeLlamar} onClick={pedirRegistro}>
                <Icon name="phone" size={16} />
                Llamar
              </button>
            ) : (
              <a className="btn btn-primary btn-sm" href={`tel:${contacto.phone.replace(/[^\d+]/g, '')}`}>
                <Icon name="phone" size={16} />
                Llamar
              </a>
            )}
            {invitado || !contacto?.whatsapp ? (
              <button className="btn btn-teal btn-sm" aria-disabled={!puedeWhatsapp} onClick={pedirRegistro}>
                <Icon name="chat" size={16} />
                WhatsApp
              </button>
            ) : (
              <a
                className="btn btn-teal btn-sm"
                href={whatsappUrl(contacto.whatsapp, `Hola, vi su lote ${lote.name} en Karsy y me gustaría más información.`)}
                target="_blank"
                rel="noreferrer"
              >
                <Icon name="chat" size={16} />
                WhatsApp
              </a>
            )}
            <a className="btn btn-ghost btn-sm" href={mapa} target="_blank" rel="noreferrer">
              <Icon name="map" size={16} />
              Cómo llegar
            </a>
          </div>
          {lote.address && (
            <div>
              <div className="label">Dirección</div>
              <p>{lote.address}</p>
            </div>
          )}
          {lote.hours && (
            <div>
              <div className="label">Horario</div>
              <p style={{ whiteSpace: 'pre-line' }}>{lote.hours}</p>
            </div>
          )}
          {contacto?.phone && (
            <div>
              <div className="label">Teléfono</div>
              <p className="num">{contacto.phone}</p>
            </div>
          )}
        </aside>
        <Inventory cars={cars} title="Vehículos en venta" emptyText="Este lote no tiene publicaciones activas por el momento." />
      </div>
    </div>
  )
}
