import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { VehicleCard } from '../components/VehicleCard'
import { history } from '../data/seller'
import type { Car } from '../data/types'
import { traducir, useI18n } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { haceCuanto } from '../lib/format'
import { useSession } from '../session/SessionContext'

/** "Hoy", "Ayer", "Esta semana" o "Antes", según la fecha de la última vista. */
function grupoDe(iso: string): Clave {
  const inicioHoy = new Date()
  inicioHoy.setHours(0, 0, 0, 0)
  const t = new Date(iso).getTime()
  const dia = 24 * 3600 * 1000
  if (t >= inicioHoy.getTime()) return 'perfil.today'
  if (t >= inicioHoy.getTime() - dia) return 'perfil.yesterday'
  if (t >= inicioHoy.getTime() - 6 * dia) return 'perfil.thisWeek'
  return 'perfil.earlier'
}

/** Vehículos vistos por la cuenta en sesión (HistoryScreen de la app). */
export function History() {
  const navigate = useNavigate()
  const { account, loading: cargandoSesion } = useSession()
  const [vistos, setVistos] = useState<{ car: Car; seenAt: string }[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const { t } = useI18n()

  const cargar = useCallback(() => {
    if (!account) return
    setError(null)
    history(account.id)
      .then(setVistos)
      .catch(() => setError(traducir('perfil.historyError')))
  }, [account])

  useEffect(() => {
    cargar()
  }, [cargar])

  if (!cargandoSesion && !account) return <Navigate to="/login" replace state={{ from: '/historial' }} />

  const grupos = new Map<Clave, { car: Car; seenAt: string }[]>()
  for (const v of vistos ?? []) grupos.set(grupoDe(v.seenAt), [...(grupos.get(grupoDe(v.seenAt)) ?? []), v])

  return (
    <div className="wrap" style={{ paddingBottom: 40 }}>
      <div className="pagehead">
        <div className="row">
          <button className="iconbtn" aria-label={t('settings.backLabel')} onClick={() => navigate('/perfil')}>
            <Icon name="back" size={18} />
          </button>
          <h1>{t('nav.history')}</h1>
        </div>
      </div>
      {error ? (
        <div className="card empty">
          <p className="muted">{error}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            {t('common.retry')}
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
          <p className="muted">{t('perfil.historyEmpty')}</p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            {t('perfil.exploreVehicles')}
          </button>
        </div>
      ) : (
        [...grupos].map(([grupo, lista]) => (
          <div key={grupo}>
            <div className="label" style={{ margin: '18px 0 12px' }}>
              {t(grupo)}
            </div>
            <div className="vgrid">
              {lista.map(({ car, seenAt }) => (
                <VehicleCard key={car.id} car={car} note={t('perfil.seen', { cuando: haceCuanto(seenAt).toLowerCase() })} />
              ))}
            </div>
          </div>
        ))
      )}
    </div>
  )
}
