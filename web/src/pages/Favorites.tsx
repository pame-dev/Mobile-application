import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { FiltersButton, aniosDe } from '../components/Filters'
import { Icon } from '../components/Icon'
import { VolverButton } from '../components/VolverButton'
import { VehicleCard } from '../components/VehicleCard'
import { favoriteCars } from '../data/api'
import type { Car } from '../data/types'
import { traducir, useI18n } from '../i18n/I18nContext'
import { ORDEN, SIN_FILTROS, filtrarCars, hayFiltros, type Filtros } from '../lib/filtros'
import { useFavorites } from '../session/FavoritesContext'
import { useRecargar } from '../lib/tiempoReal'
import { useSession } from '../session/SessionContext'

/** [principal]: sin otro título en la pantalla (visitante), el aviso lleva el h1; si no, es un h2 bajo el h1 de Favoritos. */
function Aviso({ titulo, texto, principal = false, children }: { titulo: string; texto: string; principal?: boolean; children?: ReactNode }) {
  const Titulo = principal ? 'h1' : 'h2'
  return (
    <div className="card col gap-3" style={{ padding: 60, textAlign: 'center', alignItems: 'center' }}>
      <div style={{ color: 'var(--teal)' }}>
        <Icon name="heart" size={40} />
      </div>
      <Titulo className="empty-title">{titulo}</Titulo>
      <p className="muted">{texto}</p>
      {children}
    </div>
  )
}

/** Favoritos de la cuenta en sesión (FavoritesScreen de la app). Al quitar uno sale de la lista al instante. */
export function Favorites() {
  const navigate = useNavigate()
  const { account, mode, loading: cargandoSesion } = useSession()
  const fav = useFavorites()
  const [cars, setCars] = useState<Car[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [q, setQ] = useState('')
  const [filtros, setFiltros] = useState<Filtros>(SIN_FILTROS)
  const [orden, setOrden] = useState('recientes')
  const { t } = useI18n()

  const cargar = useCallback(() => {
    if (!account) return
    setError(null)
    favoriteCars(account.id)
      .then(setCars)
      .catch(() => setError(traducir('perfil.favError')))
  }, [account])

  useEffect(() => {
    cargar()
  }, [cargar])
  useRecargar(cargar, ['favoritos', 'publicaciones', 'fotos_publicacion'])

  // Solo los que siguen marcados (quitar el corazón los saca de aquí al instante).
  const guardados = useMemo(() => (cars ?? []).filter((c) => fav.ids.has(c.id)), [cars, fav.ids])
  // "Más recientes" = orden en que se guardaron.
  const lista = useMemo(() => filtrarCars(guardados, filtros, q, orden, true), [guardados, filtros, q, orden])
  const filtrando = !!q.trim() || hayFiltros(filtros)
  const guardadosTexto = t(guardados.length === 1 ? 'perfil.savedOne' : 'perfil.savedMany', { n: guardados.length })

  if (cargandoSesion) return <div className="wrap page-top"><div className="skeleton" style={{ height: 300 }} /></div>

  if (mode === 'guest') {
    return (
      <div className="wrap wrap-narrow page-top">
        <div style={{ marginBottom: 'var(--s-4)' }}>
          <VolverButton fallback="/" />
        </div>
        <Aviso titulo={t('perfil.favGuestTitle')} texto={t('perfil.favGuestText')} principal>
          <div className="row" style={{ marginTop: 'var(--s-2)' }}>
            <button className="btn btn-ghost" onClick={() => navigate('/login', { state: { from: '/favoritos' } })}>
              {t('nav.login')}
            </button>
            <button className="btn btn-primary" onClick={() => navigate('/registro')}>
              {t('perfil.createFreeAccount')}
            </button>
          </div>
        </Aviso>
      </div>
    )
  }

  return (
    <div className="wrap">
      <div className="pagehead">
        <div className="row" style={{ alignItems: 'flex-start' }}>
          <VolverButton fallback="/" />
          <div>
            <h1>{t('nav.favorites')}</h1>
            <p className="muted">
              {cars ? (filtrando ? t('perfil.ofTotal', { n: lista.length, total: guardadosTexto }) : guardadosTexto) : t('common.loading')}
            </p>
          </div>
        </div>
        {guardados.length > 0 && (
          <div className="row gap-2" style={{ flexWrap: 'wrap' }}>
            <div className="inputicon" style={{ width: 280, maxWidth: '100%' }}>
              <Icon name="search" size={18} />
              <input className="input" placeholder={t('perfil.favSearchPlaceholder')} aria-label={t('perfil.favSearchLabel')} value={q} onChange={(e) => setQ(e.target.value)} />
            </div>
            <FiltersButton value={filtros} onApply={setFiltros} anios={aniosDe(guardados)} />
            <select className="input" aria-label={t('sort.label')} value={orden} onChange={(e) => setOrden(e.target.value)} style={{ width: 'auto' }}>
              {ORDEN.map(([v, clave]) => (
                <option key={v} value={v}>
                  {t(clave)}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {error ? (
        <div className="card empty">
          <p className="muted">{error}</p>
          <button className="btn btn-ghost" onClick={cargar}>
            {t('common.retry')}
          </button>
        </div>
      ) : !cars ? (
        <div className="vgrid">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="skeleton" style={{ height: 300 }} />
          ))}
        </div>
      ) : guardados.length === 0 ? (
        <Aviso titulo={t('perfil.favEmptyTitle')} texto={t('perfil.favEmptyText')}>
          <button className="btn btn-primary" style={{ marginTop: 'var(--s-2)' }} onClick={() => navigate('/')}>
            {t('perfil.exploreVehicles')}
          </button>
        </Aviso>
      ) : lista.length === 0 ? (
        <div className="card empty">
          <p className="muted">{t('perfil.favNoMatch')}</p>
          <button
            className="btn btn-ghost"
            onClick={() => {
              setQ('')
              setFiltros(SIN_FILTROS)
            }}
          >
            {t('home.clearFilters')}
          </button>
        </div>
      ) : (
        <div className="vgrid">
          {lista.map((c) => (
            <VehicleCard key={c.id} car={c} />
          ))}
        </div>
      )}
    </div>
  )
}
