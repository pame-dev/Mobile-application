import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
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

  // Esc cierra el modal.
  useEffect(() => {
    if (!modal) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setModal(null)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [modal])

  return (
    <UiContext.Provider value={{ toast, openModal: setModal, closeModal }}>
      {children}
      {modal && (
        <div className="overlay" onClick={(e) => e.target === e.currentTarget && setModal(null)}>
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
  return (
    <div className="row" style={{ alignItems: 'flex-start' }}>
      <div className="grow">
        <h3>{title}</h3>
        {sub && <p className="muted" style={{ marginTop: 4 }}>{sub}</p>}
      </div>
      <button className="iconbtn mclose" aria-label="Cerrar" onClick={closeModal}>
        <Icon name="x" size={18} />
      </button>
    </div>
  )
}
