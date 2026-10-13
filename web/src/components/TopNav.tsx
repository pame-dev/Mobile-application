import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { FILTER_KEYS, SIN_FILTROS, filtrosDeUrl, type Filtros } from '../lib/filtros'
import { iniciales } from '../lib/format'
import { publicUrl } from '../lib/supabase'
import { useSession } from '../session/SessionContext'
import { FilterFields } from './Filters'
import { Icon, type IconName } from './Icon'
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
        <h3>FILTROS</h3>
        <div style={{ padding: 14, borderRadius: 12, background: 'var(--teal-light)', fontSize: 13 }}>
          Crea una cuenta para acceder a los filtros y funciones exclusivas.
          <br />
          <a
            href="/registro"
            onClick={(e) => {
              e.preventDefault()
              onClose()
              navigate('/registro')
            }}
          >
            <b>Regístrate gratis →</b>
          </a>
        </div>
      </div>
    )
  }

  return (
    <div className="filterpop">
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <h3>FILTROS</h3>
        <button style={{ background: 'none', border: 0, color: 'var(--teal)', fontWeight: 600 }} onClick={() => aplicar(SIN_FILTROS)}>
          Limpiar
        </button>
      </div>
      <FilterFields draft={draft} onChange={setDraft} />
      <button className="btn btn-primary btn-block" onClick={() => aplicar(draft)}>
        Aplicar
      </button>
    </div>
  )
}

function LogoutModal() {
  const { closeModal } = useUi()
  const { signOut } = useSession()
  const navigate = useNavigate()
  return (
    <div className="modal">
      <ModalHead title="¿Cerrar sesión?" sub="¿Estás seguro de que deseas salir de tu cuenta de Karsy? Tendrás que iniciar sesión de nuevo para gestionar tus vehículos." />
      <div className="grid2">
        <button className="btn btn-ghost" onClick={closeModal}>
          Cancelar
        </button>
        <button
          className="btn btn-danger"
          onClick={async () => {
            closeModal()
            await signOut()
            navigate('/')
          }}
        >
          Sí, salir
        </button>
      </div>
    </div>
  )
}

export function TopNav() {
  const { account, mode } = useSession()
  const { toast, openModal } = useUi()
  const navigate = useNavigate()
  const location = useLocation()
  const [params] = useSearchParams()
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [q, setQ] = useState(params.get('q') ?? '')
  const searchRef = useRef<HTMLDivElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)

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
    ['/', 'Inicio', 'home'],
    ['/lotes', 'Lotes', 'building'],
    ['/favoritos', 'Favoritos', 'heart'],
  ]
  const activo = (to: string) => (to === '/' ? location.pathname === '/' : location.pathname.startsWith(to))
  const foto = publicUrl(account?.fotoPerfil)

  return (
    <header className="topnav">
      <div className="wrap">
        <Brand />
        <nav className="navlinks">
          {links.map(([to, t, i]) => (
            <button
              key={to}
              className={activo(to) ? 'on' : ''}
              onClick={() =>
                to === '/favoritos' && mode === 'guest'
                  ? toast('Regístrate para guardar tus favoritos', { label: 'Registrarme', onClick: () => navigate('/registro') })
                  : navigate(to)
              }
            >
              <Icon name={i} size={18} />
              <span>{t}</span>
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
            <input className="input" id="navq" placeholder="Buscar marca, modelo o año..." value={q} onChange={(e) => setQ(e.target.value)} aria-label="Buscar vehículos" />
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
            <span>Filtros</span>
          </button>
          {filtersOpen && <FilterPanel onClose={() => setFiltersOpen(false)} />}
        </div>
        <div className="row" style={{ marginLeft: 'auto', gap: 10 }}>
          {mode === 'guest' ? (
            <>
              <button className="btn btn-ghost btn-sm" onClick={() => navigate('/login', { state: { from: location.pathname + location.search } })}>
                Iniciar sesión
              </button>
              <button className="btn btn-primary btn-sm" onClick={() => navigate('/registro')}>
                Regístrate
              </button>
            </>
          ) : (
            <>
              <button className="btn btn-primary btn-sm" onClick={() => navigate('/publicar')}>
                <Icon name="plus" size={18} />
                Publicar
              </button>
              <button className="iconbtn" aria-label="Notificaciones" onClick={() => navigate('/notificaciones')}>
                <Icon name="bell" size={19} />
              </button>
              <div style={{ position: 'relative' }} ref={menuRef}>
                <button
                  className="avatar"
                  aria-label="Mi cuenta"
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
                        {account!.correo} · {mode === 'admin' ? 'Administrador' : account!.tipoCuenta === 'lote' ? 'Lote' : 'Particular'}
                      </div>
                    </div>
                    <hr />
                    <button onClick={() => navigate('/perfil')}>
                      <Icon name="user" size={18} />
                      Mi perfil
                    </button>
                    <button onClick={() => navigate('/panel')}>
                      <Icon name="chart" size={18} />
                      Mi panel
                    </button>
                    <button onClick={() => navigate('/historial')}>
                      <Icon name="history" size={18} />
                      Historial
                    </button>
                    <button onClick={() => navigate('/configuracion')}>
                      <Icon name="settings" size={18} />
                      Configuración
                    </button>
                    {mode === 'admin' && (
                      <button onClick={() => navigate('/admin')}>
                        <Icon name="shield" size={18} />
                        Panel de administración
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
                      Cerrar sesión
                    </button>
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
