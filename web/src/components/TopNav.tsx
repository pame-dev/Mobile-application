import { useCallback, useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { unreadCount } from '../data/account'
import { useI18n } from '../i18n/I18nContext'
import { iniciales } from '../lib/format'
import { publicUrl } from '../lib/supabase'
import { useSession } from '../session/SessionContext'
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
  const [menuOpen, setMenuOpen] = useState(false)
  /** Menú de hamburguesa (pantallas de 1000 px o menos). */
  const [menuMovil, setMenuMovil] = useState(false)
  const cerrarMenuMovil = useCallback(() => setMenuMovil(false), [])
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

  useEffect(() => {
    setMenuOpen(false)
    setMenuMovil(false)
  }, [location.pathname])

  // Clic fuera cierra el menú de la cuenta.
  useEffect(() => {
    const onDown = (e: MouseEvent) => {
      if (menuOpen && menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [menuOpen])

  const links: [string, string, IconName][] = [
    ['/', t('nav.home'), 'home'],
    ['/lotes', t('nav.lots'), 'building'],
    ['/favoritos', t('nav.favorites'), 'heart'],
  ]
  const activo = (to: string) => (to === '/' ? location.pathname === '/' : location.pathname.startsWith(to))
  const irA = (to: string) =>
    to === '/favoritos' && mode === 'guest'
      ? toast(t('nav.favoritesGuest'), { label: t('nav.registerMe'), onClick: () => navigate('/registro', { state: { from: location.pathname + location.search } }) })
      : navigate(to)
  /** Opciones de la cuenta: las usa el menú del avatar y el de hamburguesa. */
  const cuentaItems: [string, string, IconName][] = [
    // Mismo orden que el menú de la cuenta en la app: perfil, paneles y después lo demás.
    ['/perfil', t('nav.myProfile'), 'user'],
    ['/panel', t('nav.myDashboard'), 'chart'],
    ...(account?.rolPanel ? [['/admin', t('nav.adminPanel'), 'shield'] as [string, string, IconName]] : []),
    ['/historial', t('nav.history'), 'history'],
    ['/configuracion', t('nav.settings'), 'settings'],
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
        <div className="row" style={{ marginLeft: 'auto' }}>
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
              <button className="iconbtn" aria-label={sinLeer ? t('nav.notificationsUnread', { n: sinLeer }) : t('nav.notifications')} onClick={() => navigate('/notificaciones')}>
                <Icon name="bell" size={18} />
                {sinLeer > 0 && <span className="dot">{sinLeer > 99 ? '99+' : sinLeer}</span>}
              </button>
              <div className="navdesk" style={{ position: 'relative' }} ref={menuRef}>
                <button
                  className="avatar"
                  aria-label={t('nav.myAccount')}
                  aria-expanded={menuOpen}
                  onClick={() => {
                    setMenuOpen((v) => !v)
                  }}
                >
                  {foto ? <img src={foto} alt="" /> : iniciales(account!.nombre)}
                </button>
                {menuOpen && (
                  <div className="menu">
                    <div style={{ padding: 'var(--s-3) var(--s-3) var(--s-2)' }}>
                      <b className="ink">{account!.nombre}</b>
                      <div className="muted text-xs">
                        {account!.correo} · {account!.esAdmin ? t('nav.admin') : account!.tipoCuenta === 'lote' ? t('nav.lot') : t('nav.individual')}
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
          {/* Computadora: el idioma al final, dentro del mismo contenedor centrado que la página. */}
          <span className="navdesk navlangdesk">
            <LangSwitch />
          </span>
          {/* Pantallas chicas: todo lo demás del encabezado va en el menú (la campana se queda afuera). */}
          <button className="iconbtn navburger" aria-label={t('nav.openMenu')} aria-expanded={menuMovil} onClick={() => setMenuMovil(true)}>
            <Icon name="menu" size={20} />
          </button>
        </div>
      </div>
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
 * lo del encabezado: Inicio / Lotes / Favoritos, entrar o registrarse
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
            <div className="row" style={{ minWidth: 0 }}>
              <span className="avatar" aria-hidden>
                {foto ? <img src={foto} alt="" /> : iniciales(account.nombre)}
              </span>
              <div style={{ minWidth: 0 }}>
                <b style={{ display: 'block' }} className="ellipsis ink">
                  {account.nombre}
                </b>
                <span className="muted ellipsis text-xs" style={{ display: 'block' }}>
                  {account.esAdmin ? t('nav.admin') : account.tipoCuenta === 'lote' ? t('nav.lot') : t('nav.individual')}
                </span>
              </div>
            </div>
          ) : (
            <b className="ink">{t('nav.menu')}</b>
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
          <div className="col gap-3">
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
