import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { catalogs } from '../data/api'
import type { Catalogs } from '../data/types'
import { iniciales } from '../lib/format'
import { publicUrl } from '../lib/supabase'
import { useSession } from '../session/SessionContext'
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

/** Claves de búsqueda/filtros en la URL de Inicio. */
export const FILTER_KEYS = ['tipo', 'marca', 'modelo', 'anio', 'min', 'max'] as const

const anioActual = new Date().getFullYear()
const ANIOS = Array.from({ length: 35 }, (_, i) => String(anioActual + 1 - i))

/** Panel de filtros (mismos campos que el menú de filtros de la app). */
function FilterPanel({ onClose }: { onClose: () => void }) {
  const { mode } = useSession()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [cat, setCat] = useState<Catalogs | null>(null)
  const [draft, setDraft] = useState<Record<string, string>>(() =>
    Object.fromEntries(FILTER_KEYS.map((k) => [k, params.get(k) ?? ''])),
  )
  useEffect(() => {
    catalogs().then(setCat).catch(() => setCat(null))
  }, [])
  const set = (k: string, v: string) => setDraft((d) => ({ ...d, [k]: v, ...(k === 'marca' ? { modelo: '' } : {}) }))

  const aplicar = (valores: Record<string, string>) => {
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

  const marcaSel = cat?.marcas.find((m) => String(m.id) === draft.marca)
  return (
    <div className="filterpop">
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <h3>FILTROS</h3>
        <button
          style={{ background: 'none', border: 0, color: 'var(--teal)', fontWeight: 600 }}
          onClick={() => aplicar(Object.fromEntries(FILTER_KEYS.map((k) => [k, ''])))}
        >
          Limpiar
        </button>
      </div>
      <div className="field">
        <label>Tipo</label>
        <div className="row" style={{ flexWrap: 'wrap', gap: 6 }}>
          <button className={`chip ${!draft.tipo ? 'on' : ''}`} onClick={() => set('tipo', '')}>
            Todos
          </button>
          {cat?.carrocerias
            .filter((c) => !c.esOtro)
            .map((c) => (
              <button key={c.id} className={`chip ${draft.tipo === c.nombre ? 'on' : ''}`} onClick={() => set('tipo', c.nombre)}>
                {c.nombre}
              </button>
            ))}
        </div>
      </div>
      <div className="field">
        <label htmlFor="fb">Marca</label>
        <select className="input" id="fb" value={draft.marca} onChange={(e) => set('marca', e.target.value)}>
          <option value="">Todas</option>
          {cat?.marcas
            .filter((m) => !m.esOtro)
            .map((m) => (
              <option key={m.id} value={m.id}>
                {m.nombre}
              </option>
            ))}
        </select>
      </div>
      <div className="field">
        <label htmlFor="fm">Modelo</label>
        <select className="input" id="fm" value={draft.modelo} disabled={!marcaSel} onChange={(e) => set('modelo', e.target.value)}>
          <option value="">Todos</option>
          {cat?.modelos
            .filter((m) => m.idMarca === marcaSel?.id && !m.esOtro)
            .map((m) => (
              <option key={m.id} value={m.id}>
                {m.nombre}
              </option>
            ))}
        </select>
      </div>
      <div className="field">
        <label htmlFor="fy">Año</label>
        <select className="input" id="fy" value={draft.anio} onChange={(e) => set('anio', e.target.value)}>
          <option value="">Cualquiera</option>
          {ANIOS.map((a) => (
            <option key={a}>{a}</option>
          ))}
        </select>
      </div>
      <div className="field">
        <label>Rango de precio</label>
        <div className="grid2" style={{ gap: 8 }}>
          <input className="input" inputMode="numeric" placeholder="Mín. $0" aria-label="Precio mínimo" value={draft.min} onChange={(e) => set('min', e.target.value.replace(/\D/g, ''))} />
          <input className="input" inputMode="numeric" placeholder="Máx. $2M" aria-label="Precio máximo" value={draft.max} onChange={(e) => set('max', e.target.value.replace(/\D/g, ''))} />
        </div>
      </div>
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
              <button className="btn btn-ghost btn-sm" onClick={() => navigate('/login')}>
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
