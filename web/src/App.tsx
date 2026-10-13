import { Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { Detail } from './pages/Detail'
import { Home } from './pages/Home'
import { NotFound, Placeholder } from './pages/Placeholder'

/**
 * Rutas de Karsy Web. A diferencia de la app (que abre en Bienvenida), la web abre
 * directo en Inicio como visitante; Iniciar sesión y Registrarse están en la barra.
 */
export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="vehiculo/:id" element={<Detail />} />

        {/* Fase 2: acceso */}
        <Route path="login" element={<Placeholder title="Iniciar sesión" />} />
        <Route path="registro/*" element={<Placeholder title="Registro" />} />
        <Route path="verificar" element={<Placeholder title="Verificar correo" />} />
        <Route path="recuperar" element={<Placeholder title="Recuperar contraseña" />} />

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

      {/* Fase 6: administración (tiene su propio layout con panel lateral) */}
      <Route path="admin/*" element={<Placeholder title="Panel de administración" />} />
    </Routes>
  )
}
