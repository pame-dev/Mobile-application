import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { useI18n } from '../i18n/I18nContext'
import { Icon } from './Icon'

interface ToastData {
  text: string
  action?: { label: string; onClick: () => void }
}

interface UiValue {
  toast: (text: string, action?: ToastData['action']) => void
  openModal: (content: ReactNode) => void
  closeModal: () => void
}

const UiContext = createContext<UiValue | null>(null)

/** id del título del modal abierto (lo pone ModalHead; los modales sin ModalHead lo ponen en su título). */
export const MODAL_TITULO = 'modal-titulo'

/** Avisos flotantes y ventanas modales compartidas por todas las pantallas. */
export function UiProvider({ children }: { children: ReactNode }) {
  const [toastData, setToast] = useState<ToastData | null>(null)
  const [modal, setModal] = useState<ReactNode>(null)
  const timer = useRef<number | undefined>(undefined)

  const toast = useCallback((text: string, action?: ToastData['action']) => {
    window.clearTimeout(timer.current)
    setToast({ text, action })
    timer.current = window.setTimeout(() => setToast(null), action ? 5000 : 2600)
  }, [])
  const closeModal = useCallback(() => setModal(null), [])
  const overlayRef = useRef<HTMLDivElement>(null)
  /** Elemento que tenía el foco al abrir el modal; al cerrarlo, el foco regresa ahí. */
  const focoPrevio = useRef<HTMLElement | null>(null)
  const abierto = modal != null
  const abiertoRef = useRef(false)
  const openModal = useCallback((content: ReactNode) => {
    // Si ya había uno abierto (se reemplaza), se conserva el foco de origen.
    if (!abiertoRef.current) focoPrevio.current = document.activeElement as HTMLElement | null
    setModal(content)
  }, [])

  // Foco al abrir, Esc cierra y Tab no sale del modal (mismo patrón que MenuMovil de TopNav.tsx).
  useEffect(() => {
    if (!abierto) return
    abiertoRef.current = true
    const enfocables = () =>
      [...(overlayRef.current?.querySelectorAll<HTMLElement>('button, input, select, textarea, a[href], [tabindex]:not([tabindex="-1"])') ?? [])].filter((el) => !el.hasAttribute('disabled'))
    // Si el contenido ya enfocó algo (autoFocus), se respeta.
    if (!overlayRef.current?.contains(document.activeElement)) (enfocables()[0] ?? overlayRef.current)?.focus()
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') return setModal(null)
      if (e.key !== 'Tab') return
      const lista = enfocables()
      const primero = lista[0]
      const ultimo = lista[lista.length - 1]
      if (!primero) return e.preventDefault()
      if (e.shiftKey && (document.activeElement === primero || !overlayRef.current?.contains(document.activeElement))) {
        e.preventDefault()
        ultimo.focus()
      } else if (!e.shiftKey && (document.activeElement === ultimo || !overlayRef.current?.contains(document.activeElement))) {
        e.preventDefault()
        primero.focus()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => {
      window.removeEventListener('keydown', onKey)
      abiertoRef.current = false
      // Al cerrar, el foco regresa a donde estaba (si ese elemento sigue en la página).
      if (focoPrevio.current?.isConnected) focoPrevio.current.focus()
      focoPrevio.current = null
    }
  }, [abierto])

  return (
    <UiContext.Provider value={{ toast, openModal, closeModal }}>
      {children}
      {modal && (
        <div
          ref={overlayRef}
          className="overlay"
          role="dialog"
          aria-modal="true"
          aria-labelledby={MODAL_TITULO}
          tabIndex={-1}
          onClick={(e) => e.target === e.currentTarget && setModal(null)}
        >
          {modal}
        </div>
      )}
      {toastData && (
        <div className="toast" role="status">
          <Icon name="check" size={18} />
          <span>{toastData.text}</span>
          {toastData.action && (
            <button
              className="link"
              onClick={() => {
                setToast(null)
                toastData.action!.onClick()
              }}
            >
              {toastData.action.label}
            </button>
          )}
        </div>
      )}
    </UiContext.Provider>
  )
}

export function useUi(): UiValue {
  const ctx = useContext(UiContext)
  if (!ctx) throw new Error('useUi debe usarse dentro de <UiProvider>')
  return ctx
}

/** Encabezado de modal con título, subtítulo y botón de cerrar. */
export function ModalHead({ title, sub }: { title: string; sub?: string }) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  return (
    <div className="row" style={{ alignItems: 'flex-start' }}>
      <div className="grow">
        <h3 id={MODAL_TITULO}>{title}</h3>
        {sub && <p className="muted" style={{ marginTop: 'var(--s-1)' }}>{sub}</p>}
      </div>
      <button className="iconbtn mclose" aria-label={t('common.close')} onClick={closeModal}>
        <Icon name="x" size={18} />
      </button>
    </div>
  )
}
