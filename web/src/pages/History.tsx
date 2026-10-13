import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { VehicleCard } from '../components/VehicleCard'
import { history } from '../data/seller'
import type { Car } from '../data/types'
import { haceCuanto } from '../lib/format'
import { useSession } from '../session/SessionContext'

/** "Hoy", "Ayer", "Esta semana" o "Antes", según la fecha de la última vista. */
function grupoDe(iso: string): string {
  const inicioHoy = new Date()
  inicioHoy.setHours(0, 0, 0, 0)
  const t = new Date(iso).getTime()
  const dia = 24 * 3600 * 1000
  if (t >= inicioHoy.getTime()) return 'Hoy'
  if (t >= inicioHoy.getTime() - dia) return 'Ayer'
  if (t >= inicioHoy.getTime() - 6 * dia) return 'Esta semana'
  return 'Antes'
}

/** Vehículos vistos por la cuenta en sesión (HistoryScreen de la app). */
export function History() {
  const navigate = useNavigate()
  const { account, loading: cargandoSesion } = useSession()
  const [vistos, setVistos] = useState<{ car: Car; seenAt: string }[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(() => {
    if (!account) return
    setError(null)
    history(account.id)
      .then(setVistos)
      .catch(() => setError('No pudimos cargar tu historial. Revisa tu conexión e intenta de nuevo.'))
  }, [account])

  useEffect(() => {
    cargar()
  }, [cargar])

  if (!cargandoSesion && !account) return <Navigate to="/login" replace state={{ from: '/historial' }} />

  const grupos = new Map<string, { car: Car; seenAt: string }[]>()
  for (const v of vistos ?? []) grupos.set(grupoDe(v.seenAt), [...(grupos.get(grupoDe(v.seenAt)) ?? []), v])

  return (
    <div className="wrap" style={{ paddingBottom: 40 }}>
      <div className="pagehead">
        <div className="row">
          <button className="iconbtn" aria-label="Regresar" onClick={() => navigate('/perfil')}>
            <Icon name="back" size={18} />
          </button>
          <h1>Historial</h1>
        </div>
      </div>
      {error ? (
        <div className="card empty">
          <p className="muted">{error}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            Reintentar
          </button>
        </div>
      ) : !vistos ? (
        <div className="vgrid">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="skeleton" style={{ height: 300 }} />
          ))}
        </div>
      ) : vistos.length === 0 ? (
        <div className="card empty">
          <span className="nic" style={{ width: 56, height: 56, borderRadius: 16, background: 'var(--teal-light)', color: 'var(--teal)' }}>
            <Icon name="history" size={26} />
          </span>
          <p className="muted">Aún no has visto ningún vehículo.</p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            Explorar vehículos
          </button>
        </div>
      ) : (
        [...grupos].map(([grupo, lista]) => (
          <div key={grupo}>
            <div className="label" style={{ margin: '18px 0 12px' }}>
              {grupo}
            </div>
            <div className="vgrid">
              {lista.map(({ car, seenAt }) => (
                <VehicleCard key={car.id} car={car} note={`Visto ${haceCuanto(seenAt).toLowerCase()}`} />
              ))}
            </div>
          </div>
        ))
      )}
    </div>
  )
}
