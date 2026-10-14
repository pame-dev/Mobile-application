import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { unreadCount } from '../data/account'
import { useI18n } from '../i18n/I18nContext'
import { FILTER_KEYS, SIN_FILTROS, filtrosDeUrl, type Filtros } from '../lib/filtros'
import { iniciales } from '../lib/format'
import { publicUrl } from '../lib/supabase'
import { useSession } from '../session/SessionContext'
import { FilterFields } from './Filters'
import { Icon, type IconName } from './Icon'
import { LangSwitch } from './LangSwitch'
import { EVENTO_CAMBIOS, type Tabla } from '../lib/tiempoReal'
import { ModalHead, useUi } from './Ui'

export function Brand({ light = false }: { light?: boolean }) {
  const navigate = useNavigate()
  return (
    <button className="brand" onClick={() => navigate('/')} style={light ? { color: '#fff' } : undefined}>
      <img src="/logo.png" alt="" />
      Karsy
    </button>
  )
}

/** Panel de filtros de la barra (mismos campos que el menú de filtros de la app); se aplica a Inicio. */
function FilterPanel({ onClose }: { onClose: () => void }) {
  const { mode } = useSession()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [draft, setDraft] = useState<Filtros>(() => filtrosDeUrl(params))
  const { t } = useI18n()

  const aplicar = (valores: Filtros) => {
    const next = new URLSearchParams(params)
    for (const k of FILTER_KEYS) {
      const v = valores[k]?.trim()
      if (v) next.set(k, v)
      else next.delete(k)
    }
    onClose()
    navigate({ pathname: '/', search: next.toString() })
  }

  // Igual que la app: los visitantes ven la invitación a registrarse en lugar de los filtros.
  if (mode === 'guest') {
    return (
      <div className="filterpop">
        <h3>{t('filters.title')}</h3>
        <div style={{ padding: 14, borderRadius: 12, background: 'var(--teal-light)', fontSize: 13 }}>
          {t('filters.guest')}
          <br />
          <a
            href="/registro"
            onClick={(e) => {
              e.preventDefault()
              onClose()
              navigate('/registro')
            }}
          >
            <b>{t('filters.registerFree')}</b>
          </a>
        </div>
      </div>
    )
  }

  return (
    <div className="filterpop">
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <h3>{t('filters.title')}</h3>
        <button style={{ background: 'none', border: 0, color: 'var(--teal)', fontWeight: 600 }} onClick={() => aplicar(SIN_FILTROS)}>
          {t('filters.clear')}
        </button>
      </div>
      <FilterFields draft={draft} onChange={setDraft} />
      <button className="btn btn-primary btn-block" onClick={() => aplicar(draft)}>
        {t('filters.apply')}
      </button>
    </div>
  )
}

export function LogoutModal() {
  const { closeModal } = useUi()
  const { signOut } = useSession()
  const navigate = useNavigate()
  const { t } = useI18n()
  return (
    <div className="modal">
      <ModalHead title={t('logout.title')} sub={t('logout.sub')} />
      <div className="grid2">
        <button className="btn btn-ghost" onClick={closeModal}>
          {t('common.cancel')}
        </button>
        <button
          className="btn btn-danger"
          onClick={async () => {
            closeModal()
            await signOut()
            navigate('/')
          }}
        >
          {t('logout.confirm')}
        </button>
      </div>
    </div>
  )
}

export function TopNav() {
  const { account, mode } = useSession()
  const { toast, openModal } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const location = useLocation()
  const [params] = useSearchParams()
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [q, setQ] = useState(params.get('q') ?? '')
  const searchRef = useRef<HTMLDivElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const [sinLeer, setSinLeer] = useState(0)

  // Puntito en la campana si hay avisos sin leer (se revisa al cambiar de pantalla).
  useEffect(() => {
    if (!account) return setSinLeer(0)
    const revisar = () => void unreadCount(account.id).then(setSinLeer).catch(() => undefined)
    revisar()
    // También cuando cambian las notificaciones en la BD (p. ej. se leyeron en la app).
    const onCambios = (e: Event) => {
      if ((e as CustomEvent<Tabla[]>).detail.includes('notificaciones')) revisar()
    }
    window.addEventListener('karsy:notificaciones', revisar)
    window.addEventListener(EVENTO_CAMBIOS, onCambios)
    return () => {
      window.removeEventListener('karsy:notificaciones', revisar)
      window.removeEventListener(EVENTO_CAMBIOS, onCambios)
    }
  }, [account, location.pathname])

  // La búsqueda refleja la URL (p. ej. al volver atrás).
  useEffect(() => {
    setQ(params.get('q') ?? '')
  }, [params])
  useEffect(() => {
    setFiltersOpen(false)
    setMenuOpen(false)
  }, [location.pathname])

  // Clic fuera cierra el panel de filtros y el menú de la cuenta.
  useEffect(() => {
    const onDown = (e: MouseEvent) => {
      if (filtersOpen && searchRef.current && !searchRef.current.contains(e.target as Node)) setFiltersOpen(false)
      if (menuOpen && menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [filtersOpen, menuOpen])

  const buscar = () => {
    const next = new URLSearchParams(location.pathname === '/' ? params : undefined)
    if (q.trim()) next.set('q', q.trim())
    else next.delete('q')
    navigate({ pathname: '/', search: next.toString() })
  }

  const links: [string, string, IconName][] = [
    ['/', t('nav.home'), 'home'],
    ['/lotes', t('nav.lots'), 'building'],
    ['/favoritos', t('nav.favorites'), 'heart'],
  ]
  const activo = (to: string) => (to === '/' ? location.pathname === '/' : location.pathname.startsWith(to))
  const foto = publicUrl(account?.fotoPerfil)

  return (
    <header className="topnav">
      <div className="wrap">
        <Brand />
        <nav className="navlinks">
          {links.map(([to, texto, i]) => (
            <button
              key={to}
              className={activo(to) ? 'on' : ''}
              onClick={() =>
                to === '/favoritos' && mode === 'guest'
                  ? toast(t('nav.favoritesGuest'), { label: t('nav.registerMe'), onClick: () => navigate('/registro') })
                  : navigate(to)
              }
            >
              <Icon name={i} size={18} />
              <span>{texto}</span>
            </button>
          ))}
        </nav>
        <div className="navsearch" ref={searchRef}>
          <form
            className="inputicon grow"
            role="search"
            onSubmit={(e) => {
              e.preventDefault()
              buscar()
            }}
          >
            <Icon name="search" size={18} />
            <input className="input" id="navq" placeholder={t('nav.searchPlaceholder')} value={q} onChange={(e) => setQ(e.target.value)} aria-label={t('nav.searchLabel')} />
          </form>
          <button
            className={`btn filterbtn ${filtersOpen ? 'on' : ''}`}
            aria-expanded={filtersOpen}
            onClick={() => {
              setFiltersOpen((v) => !v)
              setMenuOpen(false)
            }}
          >
            <Icon name="filter" size={18} />
            <span>{t('nav.filters')}</span>
          </button>
          {filtersOpen && <FilterPanel onClose={() => setFiltersOpen(false)} />}
        </div>
        <div className="row" style={{ marginLeft: 'auto', gap: 10 }}>
          {mode === 'guest' ? (
            <>
              <button className="btn btn-ghost btn-sm" onClick={() => navigate('/login', { state: { from: location.pathname + location.search } })}>
                {t('nav.login')}
              </button>
              <button className="btn btn-primary btn-sm" onClick={() => navigate('/registro')}>
                {t('nav.register')}
              </button>
            </>
          ) : (
            <>
              <button className="btn btn-primary btn-sm" onClick={() => navigate('/publicar')}>
                <Icon name="plus" size={18} />
                {t('nav.publish')}
              </button>
              <button className="iconbtn" style={{ position: 'relative' }} aria-label={sinLeer ? t('nav.notificationsUnread', { n: sinLeer }) : t('nav.notifications')} onClick={() => navigate('/notificaciones')}>
                <Icon name="bell" size={19} />
                {sinLeer > 0 && <span className="dot" />}
              </button>
              <div style={{ position: 'relative' }} ref={menuRef}>
                <button
                  className="avatar"
                  aria-label={t('nav.myAccount')}
                  aria-expanded={menuOpen}
                  onClick={() => {
                    setMenuOpen((v) => !v)
                    setFiltersOpen(false)
                  }}
                >
                  {foto ? <img src={foto} alt="" /> : iniciales(account!.nombre)}
                </button>
                {menuOpen && (
                  <div className="menu">
                    <div style={{ padding: '10px 12px 8px' }}>
                      <b style={{ color: 'var(--ink)' }}>{account!.nombre}</b>
                      <div className="muted" style={{ fontSize: 12 }}>
                        {account!.correo} · {mode === 'admin' ? t('nav.admin') : account!.tipoCuenta === 'lote' ? t('nav.lot') : t('nav.individual')}
                      </div>
                    </div>
                    <hr />
                    <button onClick={() => navigate('/perfil')}>
                      <Icon name="user" size={18} />
                      {t('nav.myProfile')}
                    </button>
                    <button onClick={() => navigate('/panel')}>
                      <Icon name="chart" size={18} />
                      {t('nav.myDashboard')}
                    </button>
                    <button onClick={() => navigate('/historial')}>
                      <Icon name="history" size={18} />
                      {t('nav.history')}
                    </button>
                    <button onClick={() => navigate('/configuracion')}>
                      <Icon name="settings" size={18} />
                      {t('nav.settings')}
                    </button>
                    {mode === 'admin' && (
                      <button onClick={() => navigate('/admin')}>
                        <Icon name="shield" size={18} />
                        {t('nav.adminPanel')}
                      </button>
                    )}
                    <hr />
                    <button
                      style={{ color: 'var(--error)' }}
                      onClick={() => {
                        setMenuOpen(false)
                        openModal(<LogoutModal />)
                      }}
                    >
                      <Icon name="logout" size={18} />
                      {t('nav.logout')}
                    </button>
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>
      {/* Fuera del contenido centrado: queda en el extremo derecho del encabezado. */}
      <LangSwitch />
    </header>
  )
}
