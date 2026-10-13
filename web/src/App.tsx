import { useEffect } from 'react'
import { Route, Routes, useLocation } from 'react-router-dom'
import { Layout } from './components/Layout'
import { Notifications, Privacy, PrivacyPolicy, Settings, Terms } from './pages/Account'
import { Admin } from './pages/admin/Admin'
import { Dashboard } from './pages/Dashboard'
import { Detail } from './pages/Detail'
import { History } from './pages/History'
import { Profile } from './pages/Profile'
import { Publish } from './pages/Publish'
import { Favorites } from './pages/Favorites'
import { LotProfile } from './pages/LotProfile'
import { Lots } from './pages/Lots'
import { SellerProfile } from './pages/SellerProfile'
import { Home } from './pages/Home'
import { Login } from './pages/auth/Login'
import { Recover } from './pages/auth/Recover'
import { RegisterForm, RegisterType } from './pages/auth/Register'
import { VerifyEmail } from './pages/auth/VerifyEmail'
import { NotFound } from './pages/Placeholder'

/**
 * Rutas de Karsy Web. A diferencia de la app (que abre en Bienvenida), la web abre
 * directo en Inicio como visitante; Iniciar sesión y Registrarse están en la barra.
 */
export function App() {
  const { pathname } = useLocation()
  // Cada pantalla nueva empieza arriba.
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="vehiculo/:id" element={<Detail />} />

        <Route path="lotes" element={<Lots />} />
        <Route path="lotes/:id" element={<LotProfile />} />
        <Route path="vendedor/:id" element={<SellerProfile />} />
        <Route path="favoritos" element={<Favorites />} />

        <Route path="publicar" element={<Publish key="nueva" />} />
        <Route path="publicar/:id" element={<Publish />} />
        <Route path="perfil" element={<Profile />} />
        <Route path="panel" element={<Dashboard />} />
        <Route path="historial" element={<History />} />

        <Route path="notificaciones" element={<Notifications />} />
        <Route path="configuracion" element={<Settings />} />
        <Route path="privacidad" element={<Privacy />} />
        <Route path="privacidad/politicas" element={<PrivacyPolicy />} />
        <Route path="terminos" element={<Terms />} />

        <Route path="*" element={<NotFound />} />
      </Route>

      {/* Acceso: pantalla completa, sin la barra de navegación */}
      <Route path="login" element={<Login />} />
      <Route path="registro" element={<RegisterType />} />
      <Route path="registro/particular" element={<RegisterForm key="particular" tipo="particular" />} />
      <Route path="registro/lote" element={<RegisterForm key="lote" tipo="lote" />} />
      <Route path="verificar" element={<VerifyEmail />} />
      <Route path="recuperar" element={<Recover />} />

      {/* Administración: su propio layout con panel lateral */}
      <Route path="admin" element={<Admin />} />
      <Route path="admin/:sec" element={<Admin />} />
    </Routes>
  )
}
