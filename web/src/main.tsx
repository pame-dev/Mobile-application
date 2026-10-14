import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { App } from './App'
import { UiProvider } from './components/Ui'
import { I18nProvider } from './i18n/I18nContext'
import { FavoritesProvider } from './session/FavoritesContext'
import { SessionProvider } from './session/SessionContext'
import './styles.css'

// Tema guardado (Configuración → Tema); si no hay, sigue al del sistema.
try {
  const tema = localStorage.getItem('karsy-theme')
  if (tema === 'light' || tema === 'dark') document.documentElement.setAttribute('data-theme', tema)
} catch {
  /* sin almacenamiento: tema del sistema */
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <I18nProvider>
      <BrowserRouter>
        <SessionProvider>
          <UiProvider>
            <FavoritesProvider>
              <App />
            </FavoritesProvider>
          </UiProvider>
        </SessionProvider>
      </BrowserRouter>
    </I18nProvider>
  </StrictMode>,
)
