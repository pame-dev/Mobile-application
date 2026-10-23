import { useCallback, useEffect, useRef, useState } from 'react'
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
import { recordarBusqueda, SugerenciasBusqueda } from './SugerenciasBusqueda'
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
  /** Menú de hamburguesa (pantallas de 1000 px o menos). */
  const [menuMovil, setMenuMovil] = useState(false)
  const cerrarMenuMovil = useCallback(() => setMenuMovil(false), [])
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
    setMenuMovil(false)
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
    recordarBusqueda(q, account?.id)
    navigate({ pathname: '/', search: next.toString() })
  }

  const links: [string, string, IconName][] = [
    ['/', t('nav.home'), 'home'],
    ['/lotes', t('nav.lots'), 'building'],
    ['/favoritos', t('nav.favorites'), 'heart'],
  ]
  const activo = (to: string) => (to === '/' ? location.pathname === '/' : location.pathname.startsWith(to))
  const irA = (to: string) =>
    to === '/favoritos' && mode === 'guest'
      ? toast(t('nav.favoritesGuest'), { label: t('nav.registerMe'), onClick: () => navigate('/registro') })
      : navigate(to)
  /** Opciones de la cuenta: las usa el menú del avatar y el de hamburguesa. */
  const cuentaItems: [string, string, IconName][] = [
    ['/perfil', t('nav.myProfile'), 'user'],
    ['/panel', t('nav.myDashboard'), 'chart'],
    ['/historial', t('nav.history'), 'history'],
    ['/configuracion', t('nav.settings'), 'settings'],
    ...(mode === 'admin' ? [['/admin', t('nav.adminPanel'), 'shield'] as [string, string, IconName]] : []),
  ]
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
              onClick={() => irA(to)}
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
            {/* Sugerencias: búsquedas recientes y marcas. */}
            <input className="input" id="navq" list="navq-sugerencias" autoComplete="off" placeholder={t('nav.searchPlaceholder')} value={q} onChange={(e) => setQ(e.target.value)} aria-label={t('nav.searchLabel')} />
            <SugerenciasBusqueda id="navq-sugerencias" />
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
              <button className="btn btn-ghost btn-sm navdesk" onClick={() => navigate('/login', { state: { from: location.pathname + location.search } })}>
                {t('nav.login')}
              </button>
              <button className="btn btn-primary btn-sm navdesk" onClick={() => navigate('/registro')}>
                {t('nav.register')}
              </button>
              {/* Pantallas chicas, visitante: el idioma siempre a la vista en la barra (no dentro del menú). */}
              <span className="navlang">
                <LangSwitch />
              </span>
            </>
          ) : (
            <>
              <button className="iconbtn" style={{ position: 'relative' }} aria-label={sinLeer ? t('nav.notificationsUnread', { n: sinLeer }) : t('nav.notifications')} onClick={() => navigate('/notificaciones')}>
                <Icon name="bell" size={19} />
                {sinLeer > 0 && <span className="dot">{sinLeer > 99 ? '99+' : sinLeer}</span>}
              </button>
              <div className="navdesk" style={{ position: 'relative' }} ref={menuRef}>
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
                    {cuentaItems.map(([to, texto, i]) => (
                      <button key={to} onClick={() => navigate(to)}>
                        <Icon name={i} size={18} />
                        {texto}
                      </button>
                    ))}
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
          {/* Pantallas chicas: todo lo demás del encabezado va en el menú (la campana se queda afuera). */}
          <button className="iconbtn navburger" aria-label={t('nav.openMenu')} aria-expanded={menuMovil} onClick={() => setMenuMovil(true)}>
            <Icon name="menu" size={20} />
          </button>
        </div>
      </div>
      {/* Fuera del contenido centrado: queda en el extremo derecho del encabezado. */}
      <LangSwitch />
      {menuMovil && (
        <MenuMovil
          links={links}
          cuentaItems={cuentaItems}
          activo={activo}
          irA={irA}
          onCerrar={cerrarMenuMovil}
        />
      )}
    </header>
  )
}

/**
 * Menú de hamburguesa (1000 px o menos): panel que se desliza desde la derecha con todo
 * lo del encabezado: buscador y filtros, Inicio / Lotes / Favoritos, entrar o registrarse
 * (visitante) o Publicar y las opciones de la cuenta, cerrar sesión e idioma. Se cierra
 * con la X, tocando afuera, con Esc, al cambiar de pantalla o si la ventana se agranda.
 */
function MenuMovil({
  links,
  cuentaItems,
  activo,
  irA,
  onCerrar,
}: {
  links: [string, string, IconName][]
  cuentaItems: [string, string, IconName][]
  activo: (to: string) => boolean
  irA: (to: string) => void
  onCerrar: () => void
}) {
  const { account, mode } = useSession()
  const { openModal } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const location = useLocation()
  const [params] = useSearchParams()
  const [q, setQ] = useState(params.get('q') ?? '')
  const [filtros, setFiltros] = useState(false)
  const cerrarRef = useRef<HTMLButtonElement>(null)
  const panelRef = useRef<HTMLElement>(null)
  /** Solo cierra si el clic empezó y terminó en el fondo (no al soltar ahí tras seleccionar texto). */
  const presionEnFondo = useRef(false)
  const foto = publicUrl(account?.fotoPerfil)

  useEffect(() => {
    cerrarRef.current?.focus()
    // Mientras está abierto, la página de atrás no se desplaza.
    const antes = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') return onCerrar()
      // Tab no sale del menú mientras está abierto (vuelve del último al primero y al revés).
      if (e.key !== 'Tab' || !panelRef.current) return
      const enfocables = [...panelRef.current.querySelectorAll<HTMLElement>('button, input, select, a[href]')].filter((el) => !el.hasAttribute('disabled'))
      const primero = enfocables[0]
      const ultimo = enfocables[enfocables.length - 1]
      if (e.shiftKey && document.activeElement === primero) {
        e.preventDefault()
        ultimo?.focus()
      } else if (!e.shiftKey && document.activeElement === ultimo) {
        e.preventDefault()
        primero?.focus()
      }
    }
    // Si la ventana se agranda, el menú ya no hace falta (el encabezado completo vuelve).
    const ancho = window.matchMedia('(min-width: 1001px)')
    const onAncho = () => ancho.matches && onCerrar()
    window.addEventListener('keydown', onKey)
    ancho.addEventListener('change', onAncho)
    return () => {
      document.body.style.overflow = antes
      window.removeEventListener('keydown', onKey)
      ancho.removeEventListener('change', onAncho)
      // Al cerrar, el foco regresa al botón ☰ (si sigue visible).
      document.querySelector<HTMLButtonElement>('.navburger')?.focus()
    }
  }, [onCerrar])

  const buscar = () => {
    const next = new URLSearchParams(location.pathname === '/' ? params : undefined)
    if (q.trim()) next.set('q', q.trim())
    else next.delete('q')
    recordarBusqueda(q, account?.id)
    navigate({ pathname: '/', search: next.toString() })
    onCerrar()
  }
  const ir = (to: string) => {
    irA(to)
    onCerrar()
  }

  return (
    <div
      className="drawerwrap"
      onMouseDown={(e) => (presionEnFondo.current = e.target === e.currentTarget)}
      onClick={(e) => e.target === e.currentTarget && presionEnFondo.current && onCerrar()}
    >
      <aside ref={panelRef} className="drawer" role="dialog" aria-modal="true" aria-label={t('nav.menu')}>
        <div className="row" style={{ justifyContent: 'space-between' }}>
          {account ? (
            <div className="row" style={{ gap: 10, minWidth: 0 }}>
              <span className="avatar" aria-hidden>
                {foto ? <img src={foto} alt="" /> : iniciales(account.nombre)}
              </span>
              <div style={{ minWidth: 0 }}>
                <b style={{ color: 'var(--ink)', display: 'block' }} className="ellipsis">
                  {account.nombre}
                </b>
                <span className="muted ellipsis" style={{ fontSize: 12, display: 'block' }}>
                  {mode === 'admin' ? t('nav.admin') : account.tipoCuenta === 'lote' ? t('nav.lot') : t('nav.individual')}
                </span>
              </div>
            </div>
          ) : (
            <b style={{ color: 'var(--ink)' }}>{t('nav.menu')}</b>
          )}
          <button ref={cerrarRef} className="iconbtn" aria-label={t('nav.closeMenu')} onClick={onCerrar}>
            <Icon name="x" size={18} />
          </button>
        </div>

        {/* Con sesión, el idioma va arriba y con su nombre (los usuarios no lo encontraban al fondo).
            El visitante lo tiene en la barra de arriba. */}
        {mode !== 'guest' && (
          <div className="drawerlang">
            <span>{t('footer.language')}</span>
            <LangSwitch />
          </div>
        )}

        <div className="drawersearch">
          <form
            className="inputicon grow"
            role="search"
            onSubmit={(e) => {
              e.preventDefault()
              buscar()
            }}
          >
            <Icon name="search" size={18} />
            <input className="input" list="drawerq-sugerencias" autoComplete="off" placeholder={t('nav.searchPlaceholder')} value={q} onChange={(e) => setQ(e.target.value)} aria-label={t('nav.searchLabel')} />
            <SugerenciasBusqueda id="drawerq-sugerencias" />
          </form>
          <button className={`btn filterbtn ${filtros ? 'on' : ''}`} aria-expanded={filtros} onClick={() => setFiltros((v) => !v)}>
            <Icon name="filter" size={18} />
            <span>{t('nav.filters')}</span>
          </button>
        </div>
        {filtros && <FilterPanel onClose={onCerrar} />}

        {/* Una sola lista con la misma separación: navegación, cuenta y cerrar sesión. */}
        <nav className="drawerlinks">
          {[...links, ...(mode === 'guest' ? [] : cuentaItems)].map(([to, texto, i]) => (
            <button key={to} className={activo(to) ? 'on' : ''} onClick={() => ir(to)}>
              <Icon name={i} size={18} />
              {texto}
            </button>
          ))}
          {mode !== 'guest' && (
            <button
              style={{ color: 'var(--error)' }}
              onClick={() => {
                onCerrar()
                openModal(<LogoutModal />)
              }}
            >
              <Icon name="logout" size={18} />
              {t('nav.logout')}
            </button>
          )}
        </nav>

        {mode === 'guest' && (
          <div className="col" style={{ gap: 10 }}>
            <button
              className="btn btn-ghost btn-block"
              onClick={() => {
                navigate('/login', { state: { from: location.pathname + location.search } })
                onCerrar()
              }}
            >
              {t('nav.login')}
            </button>
            <button className="btn btn-primary btn-block" onClick={() => ir('/registro')}>
              {t('nav.register')}
            </button>
          </div>
        )}
      </aside>
    </div>
  )
}
