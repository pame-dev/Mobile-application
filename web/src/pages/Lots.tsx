import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Icon } from '../components/Icon'
import { Traducido } from '../components/Traducido'
import { esClicNormal } from '../components/VehicleCard'
import { VolverButton } from '../components/VolverButton'
import { lots as cargarLotes } from '../data/api'
import type { Lot } from '../data/types'
import { traducir, useI18n } from '../i18n/I18nContext'
import { iniciales } from '../lib/format'
import { useSession } from '../session/SessionContext'
import { useRecargar } from '../lib/tiempoReal'

/** Logo del lote o sus iniciales. */
export function LotLogo({ lot, className = 'lotlogo' }: { lot: Pick<Lot, 'name' | 'logoUrl'>; className?: string }) {
  const [fallo, setFallo] = useState(false)
  return <span className={className}>{lot.logoUrl && !fallo ? <img src={lot.logoUrl} alt="" onError={() => setFallo(true)} /> : iniciales(lot.name)}</span>
}

/** Directorio de lotes y agencias (pestaña "Lotes" de la app). */
export function Lots() {
  const navigate = useNavigate()
  const { account, loading: cargandoSesion } = useSession()
  const [params, setParams] = useSearchParams()
  const query = params.get('q') ?? ''
  const [lista, setLista] = useState<Lot[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const { t } = useI18n()

  const cargar = useCallback(() => {
    setError(null)
    cargarLotes(account?.id ?? null)
      .then(setLista)
      .catch(() => setError(traducir('perfil.lotsError')))
  }, [account?.id])

  useEffect(() => {
    // Se espera a saber quién es: el lote propio no aparece en el directorio.
    if (!cargandoSesion) cargar()
  }, [cargar, cargandoSesion])
  useRecargar(cargar, ['cuentas', 'publicaciones'])

  // Búsqueda por nombre o ciudad.
  const q = query.trim().toLowerCase()
  const visibles = (lista ?? []).filter((l) => !q || l.name.toLowerCase().includes(q) || l.city.toLowerCase().includes(q))

  return (
    <div className="wrap page-top">
      <div style={{ marginBottom: 'var(--s-4)' }}>
        <VolverButton fallback="/" />
      </div>
      <div className="banner">
        <span className="eyebrow" style={{ color: '#8FD3D8' }}>
          {t('perfil.lotsEyebrow')}
        </span>
        <h1 className="h1-page" style={{ margin: 'var(--s-2) 0' }}>{t('nav.lots')}</h1>
        <p>{t('perfil.lotsLead')}</p>
        <div className="inputicon" style={{ marginTop: 'var(--s-5)', maxWidth: 520 }}>
          <Icon name="search" size={18} />
          <input
            className="input"
            style={{ height: 48 }}
            placeholder={t('perfil.lotsSearchPlaceholder')}
            aria-label={t('perfil.lotsSearchLabel')}
            value={query}
            onChange={(e) => setParams(e.target.value ? { q: e.target.value } : {}, { replace: true })}
          />
        </div>
      </div>

      <div className="sechead">
        <h2>
          {!lista
            ? t('perfil.lotsRegistered')
            : q
              ? t(visibles.length === 1 ? 'perfil.resultsOne' : 'perfil.resultsMany', { n: visibles.length })
              : t(lista.length === 1 ? 'perfil.lotsRegisteredOne' : 'perfil.lotsRegisteredMany', { n: lista.length })}
        </h2>
      </div>

      {error ? (
        <div className="card empty">
          <p className="muted">{error}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            {t('common.retry')}
          </button>
        </div>
      ) : !lista ? (
        <div className="lgrid">
          {Array.from({ length: 6 }, (_, i) => (
            <div key={i} className="skeleton" style={{ height: 150 }} />
          ))}
        </div>
      ) : visibles.length === 0 ? (
        <div className="card empty">
          <span className="nic empty-icon">
            <Icon name="building" size={26} />
          </span>
          <p className="muted">{lista.length === 0 ? t('perfil.lotsEmpty') : t('perfil.lotsNoMatch')}</p>
        </div>
      ) : (
        <div className="lgrid">
          {visibles.map((l) => (
            <a
              key={l.id}
              className="card lotcard"
              href={`/lotes/${l.id}`}
              onClick={(e) => {
                if (!esClicNormal(e)) return
                e.preventDefault()
                navigate(`/lotes/${l.id}`)
              }}
            >
              <LotLogo lot={l} />
              <span className="grow col" style={{ gap: 'var(--s-1)', minWidth: 0 }}>
                <b className="ink" style={{ fontFamily: 'var(--f-display)', fontSize: 'var(--fs-lg)' }}>{l.name}</b>
                {l.city && (
                  <span className="loc">
                    <Icon name="pin" />
                    {l.city}
                  </span>
                )}
                {l.description && (
                  <span className="muted text-sm" style={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                    <Traducido texto={l.description} />
                  </span>
                )}
                <span className="badge b-info" style={{ alignSelf: 'flex-start', marginTop: 'var(--s-2)' }}>
                  {t(l.vehicleCount === 1 ? 'perfil.vehiclesOne' : 'perfil.vehiclesMany', { n: l.vehicleCount })}
                </span>
              </span>
            </a>
          ))}
        </div>
      )}
    </div>
  )
}
