import { useEffect } from 'react'
import { Route, Routes, useLocation } from 'react-router-dom'
import { Layout } from './components/Layout'
import { Detail } from './pages/Detail'
import { Home } from './pages/Home'
import { Login } from './pages/auth/Login'
import { Recover } from './pages/auth/Recover'
import { RegisterForm, RegisterType } from './pages/auth/Register'
import { VerifyEmail } from './pages/auth/VerifyEmail'
import { NotFound, Placeholder } from './pages/Placeholder'

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

        {/* Fase 3: comprador */}
        <Route path="lotes" element={<Placeholder title="Lotes" />} />
        <Route path="lotes/:id" element={<Placeholder title="Perfil de lote" />} />
        <Route path="vendedor/:id" element={<Placeholder title="Perfil del vendedor" />} />
        <Route path="favoritos" element={<Placeholder title="Favoritos" />} />

        {/* Fase 4: vendedor */}
        <Route path="publicar" element={<Placeholder title="Publicar vehículo" />} />
        <Route path="perfil" element={<Placeholder title="Mi perfil" />} />
        <Route path="panel" element={<Placeholder title="Mi panel" />} />
        <Route path="historial" element={<Placeholder title="Historial" />} />

        {/* Fase 5: cuenta */}
        <Route path="notificaciones" element={<Placeholder title="Notificaciones" />} />
        <Route path="configuracion" element={<Placeholder title="Configuración" />} />
        <Route path="privacidad" element={<Placeholder title="Privacidad y seguridad" />} />
        <Route path="privacidad/politicas" element={<Placeholder title="Políticas de privacidad" />} />
        <Route path="terminos" element={<Placeholder title="Términos y condiciones" />} />

        <Route path="*" element={<NotFound />} />
      </Route>

      {/* Acceso: pantalla completa, sin la barra de navegación */}
      <Route path="login" element={<Login />} />
      <Route path="registro" element={<RegisterType />} />
      <Route path="registro/particular" element={<RegisterForm key="particular" tipo="particular" />} />
      <Route path="registro/lote" element={<RegisterForm key="lote" tipo="lote" />} />
      <Route path="verificar" element={<VerifyEmail />} />
      <Route path="recuperar" element={<Recover />} />

      {/* Fase 6: administración (tiene su propio layout con panel lateral) */}
      <Route path="admin/*" element={<Placeholder title="Panel de administración" />} />
    </Routes>
  )
}
