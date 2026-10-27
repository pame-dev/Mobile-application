import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { Inventory, ProfileHeader } from '../components/ProfileParts'
import { Traducido } from '../components/Traducido'
import { VolverButton } from '../components/VolverButton'
import { ownerVisibleCars, sellerProfile } from '../data/api'
import type { Car, SellerProfile as Perfil } from '../data/types'
import { traducir, useI18n } from '../i18n/I18nContext'
import { useRecargar } from '../lib/tiempoReal'

const desde = (fecha: string) => new Date(fecha).toLocaleDateString(traducir('vehiculo.locale'), { month: 'long', year: 'numeric' })

/** Perfil público de un vendedor particular y sus publicaciones visibles (SellerProfileView de la app). */
export function SellerProfile() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState<Perfil | null | undefined>(undefined)
  const [cars, setCars] = useState<Car[]>([])
  const [error, setError] = useState<string | null>(null)
  const { t } = useI18n()

  const cargar = useCallback(() => {
    setError(null)
    Promise.all([sellerProfile(id), ownerVisibleCars(id)])
      .then(([p, c]) => {
        setPerfil(p)
        setCars(c)
      })
      .catch(() => setError(traducir('perfil.sellerError')))
  }, [id])

  useEffect(() => {
    cargar()
  }, [cargar])
  useRecargar(cargar, ['cuentas', 'publicaciones', 'fotos_publicacion'])

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
  if (perfil === undefined) {
    return (
      <div className="wrap page-top">
        <div className="skeleton" style={{ height: 210, borderRadius: 'var(--r-xl)' }} />
        <div className="skeleton" style={{ height: 300, marginTop: 'var(--s-6)' }} />
      </div>
    )
  }
  if (perfil === null) {
    return (
      <div className="wrap wrap-narrow">
        <div className="card empty empty-page">
          <h1 className="empty-title">{t('perfil.sellerNotFound')}</h1>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            {t('common.goHome')}
          </button>
        </div>
      </div>
    )
  }
  // Los lotes tienen su propio perfil, con dirección y contacto.
  if (perfil.type === 'lote') return <Navigate to={`/lotes/${perfil.id}`} replace />

  const datos = [perfil.city, perfil.since ? t('perfil.since', { fecha: desde(perfil.since) }) : ''].filter(Boolean).join(' · ')

  return (
    <div className="wrap page-top">
      <div style={{ marginBottom: 'var(--s-4)' }}>
        <VolverButton fallback="/" />
      </div>
      <ProfileHeader
        name={perfil.name}
        avatarUrl={perfil.avatarUrl}
        coverUrl={perfil.coverUrl}
        verified={perfil.verified}
        typeLabel={t('nav.individual')}
        info={datos}
      />
      {perfil.description && (
        <p className="muted" style={{ marginTop: 'var(--s-4)', maxWidth: 720 }}>
          <Traducido texto={perfil.description} />
        </p>
      )}
      <div style={{ marginTop: 'var(--s-6)' }}>
        <Inventory cars={cars} title={t('perfil.listings')} emptyText={t('perfil.sellerInventoryEmpty')} />
      </div>
    </div>
  )
}
