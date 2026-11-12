import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import { useI18n } from '../i18n/I18nContext'

/**
 * Mientras se guarda o envía algo: velo encima de todo (también de los modales) que muestra
 * que está cargando y no deja hacer clic ni usar el teclado en nada más (tocar otra cosa a
 * medio guardar causaba errores). Igual que CargandoBloqueo.kt en la app.
 */
export function CargandoBloqueo({ visible, texto }: { visible: boolean; texto?: string }) {
  const { t } = useI18n()
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!visible) return
    const previo = document.activeElement as HTMLElement | null
    ref.current?.focus()
    // En captura: ni Escape ni Tab llegan al modal o la página de abajo.
    const onKey = (e: KeyboardEvent) => {
      e.preventDefault()
      e.stopPropagation()
    }
    window.addEventListener('keydown', onKey, true)
    return () => {
      window.removeEventListener('keydown', onKey, true)
      if (previo?.isConnected) previo.focus()
    }
  }, [visible])

  if (!visible) return null
  return createPortal(
    <div ref={ref} className="cargabloqueo" role="alert" aria-busy="true" aria-live="polite" tabIndex={-1}>
      <div className="cargabloqueo-card">
        <span className="spinner cargabloqueo-spin" aria-hidden="true" />
        <p>{texto ?? t('common.cargandoBloqueo')}</p>
      </div>
    </div>,
    document.body,
  )
}
