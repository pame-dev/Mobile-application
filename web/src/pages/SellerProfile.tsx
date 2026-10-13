import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { Inventory, ProfileHeader } from '../components/ProfileParts'
import { ownerVisibleCars, sellerProfile } from '../data/api'
import type { Car, SellerProfile as Perfil } from '../data/types'

const desde = (fecha: string) => new Date(fecha).toLocaleDateString('es-MX', { month: 'long', year: 'numeric' })

/** Perfil público de un vendedor particular y sus publicaciones visibles (SellerProfileView de la app). */
export function SellerProfile() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState<Perfil | null | undefined>(undefined)
  const [cars, setCars] = useState<Car[]>([])
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(() => {
    setError(null)
    Promise.all([sellerProfile(id), ownerVisibleCars(id)])
      .then(([p, c]) => {
        setPerfil(p)
        setCars(c)
      })
      .catch(() => setError('No pudimos cargar este perfil. Revisa tu conexión e intenta de nuevo.'))
  }, [id])

  useEffect(() => {
    cargar()
  }, [cargar])

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
  if (perfil === undefined) {
    return (
      <div className="wrap" style={{ paddingTop: 24 }}>
        <div className="skeleton" style={{ height: 210, borderRadius: 22 }} />
        <div className="skeleton" style={{ height: 300, marginTop: 24 }} />
      </div>
    )
  }
  if (perfil === null) {
    return (
      <div className="wrap" style={{ maxWidth: 820 }}>
        <div className="card empty" style={{ marginTop: 40 }}>
          <h2>No encontramos este vendedor.</h2>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            Ir al inicio
          </button>
        </div>
      </div>
    )
  }
  // Los lotes tienen su propio perfil, con dirección y contacto.
  if (perfil.type === 'lote') return <Navigate to={`/lotes/${perfil.id}`} replace />

  const datos = ['Particular', perfil.city, perfil.since ? `En Karsy desde ${desde(perfil.since)}` : ''].filter(Boolean).join(' · ')

  return (
    <div className="wrap" style={{ paddingTop: 24 }}>
      <ProfileHeader name={perfil.name} avatarUrl={perfil.avatarUrl} coverUrl={perfil.coverUrl}>
        <h1 style={{ fontSize: 28 }}>{perfil.name}</h1>
        <p className="muted">{datos}</p>
      </ProfileHeader>
      {perfil.description && (
        <p className="muted" style={{ marginTop: 18, maxWidth: 720 }}>
          {perfil.description}
        </p>
      )}
      <div style={{ marginTop: 28 }}>
        <Inventory cars={cars} title="Publicaciones" emptyText="Este vendedor no tiene publicaciones activas por el momento." />
      </div>
    </div>
  )
}
