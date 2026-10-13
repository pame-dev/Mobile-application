import { useState } from 'react'
import { ModalHead, useUi } from './Ui'

/** Ventana de confirmación (ConfirmDialog de la app). [onConfirm] puede ser asíncrono: el botón espera. */
export function ConfirmModal({
  title,
  message,
  confirm,
  cancel = 'Cancelar',
  danger = false,
  onConfirm,
}: {
  title: string
  message: string
  confirm: string
  cancel?: string
  danger?: boolean
  onConfirm: () => void | Promise<void>
}) {
  const { closeModal } = useUi()
  const [busy, setBusy] = useState(false)
  return (
    <div className="modal">
      <ModalHead title={title} sub={message} />
      <div className="grid2">
        <button className="btn btn-ghost" disabled={busy} onClick={closeModal}>
          {cancel}
        </button>
        <button
          className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`}
          disabled={busy}
          onClick={async () => {
            setBusy(true)
            try {
              await onConfirm()
            } finally {
              setBusy(false)
              closeModal()
            }
          }}
        >
          {confirm}
        </button>
      </div>
    </div>
  )
}
