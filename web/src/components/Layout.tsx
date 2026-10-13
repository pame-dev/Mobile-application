import { useEffect } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useSession } from '../session/SessionContext'
import { TopNav } from './TopNav'
import { useUi } from './Ui'

export function Footer() {
  const navigate = useNavigate()
  const go = (to: string) => () => navigate(to)
  return (
    <footer className="footer">
      <div className="wrap">
        <div>
          <div className="brand" style={{ color: '#fff' }}>
            <img src="/logo.png" alt="" />
            Karsy
          </div>
          <p style={{ marginTop: 12, maxWidth: 300 }}>Marketplace automotriz para comprar y vender autos nuevos y seminuevos en todo México.</p>
        </div>
        <div>
          <h4>Explorar</h4>
          <button onClick={go('/')}>Vehículos</button>
          <button onClick={go('/lotes')}>Lotes y agencias</button>
          <button onClick={go('/publicar')}>Publicar vehículo</button>
        </div>
        <div>
          <h4>Tu cuenta</h4>
          <button onClick={go('/perfil')}>Mi perfil</button>
          <button onClick={go('/panel')}>Mi panel</button>
          <button onClick={go('/favoritos')}>Favoritos</button>
        </div>
        <div>
          <h4>Legal</h4>
          <button onClick={go('/terminos')}>Términos y condiciones</button>
          <button onClick={go('/privacidad/politicas')}>Políticas de privacidad</button>
        </div>
      </div>
      <div className="wrap legal">© {new Date().getFullYear()} Karsy · Hecho en Torreón, Coahuila</div>
    </footer>
  )
}

/** Barra superior + contenido + pie, para las pantallas del marketplace y de la cuenta. */
export function Layout() {
  const { pathname } = useLocation()
  const { notice, clearNotice } = useSession()
  const { toast } = useUi()

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  // Sesión cerrada sola (cuenta suspendida, etc.): se avisa una vez.
  useEffect(() => {
    if (notice) {
      toast(notice)
      clearNotice()
    }
  }, [notice, clearNotice, toast])

  return (
    <>
      <TopNav />
      <main>
        <Outlet />
      </main>
      <Footer />
    </>
  )
}
