import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { mensajeUsuario } from '../data/auth'
import { requestFeatured, setPaused, setSold, type OwnCar } from '../data/seller'
import { ConfirmModal } from './Confirm'
import { Icon } from './Icon'
import { ModalHead, useUi } from './Ui'

export const STATUS_BADGE: Record<string, string> = {
  Activo: 'b-succ',
  Pendiente: 'b-warn',
  Rechazado: 'b-dang',
  Deshabilitado: 'b-dang',
  Vendido: 'b-neu',
  Pausado: 'b-neu',
}

export const fechaCorta = (iso: string) => new Date(iso).toLocaleDateString('es-MX', { day: 'numeric', month: 'short' })

/** "¿Dónde lo vendiste?" (dentro o fuera de la app). */
function SoldViaModal({ onDone }: { onDone: (via: string) => Promise<void> }) {
  const { closeModal } = useUi()
  const [busy, setBusy] = useState(false)
  const elegir = async (via: string) => {
    setBusy(true)
    await onDone(via)
    closeModal()
  }
  return (
    <div className="modal">
      <ModalHead title="¿Dónde lo vendiste?" sub="Cuéntanos si la venta se cerró por medio de Karsy o por otro lado." />
      <button className="btn btn-primary btn-block" disabled={busy} onClick={() => void elegir('dentro_app')}>
        Dentro de la app
      </button>
      <button className="btn btn-ghost btn-block" disabled={busy} onClick={() => void elegir('fuera_app')}>
        Fuera de la app
      </button>
    </div>
  )
}

/** "¿Quieres destacar tu vehículo?" */
function FeatureModal({ onConfirm }: { onConfirm: () => Promise<void> }) {
  const { closeModal } = useUi()
  const [busy, setBusy] = useState(false)
  return (
    <div className="modal">
      <span className="nic" style={{ width: 56, height: 56, borderRadius: 16, background: 'var(--warning-l)', color: 'var(--star)' }}>
        <Icon name="star" size={26} />
      </span>
      <ModalHead
        title="¿Quieres destacar tu vehículo?"
        sub="Haz que tu publicación tenga mayor visibilidad y aparezca en una sección destacada de la plataforma."
      />
      <div className="note" style={{ background: 'var(--teal-light)' }}>
        Antes de destacarla, un administrador deberá revisar y aprobar tu solicitud. Una vez aprobada, tu vehículo aparecerá como publicación destacada durante 1 mes.
      </div>
      <div className="grid2">
        <button className="btn btn-ghost" disabled={busy} onClick={closeModal}>
          Cancelar
        </button>
        <button
          className="btn btn-primary"
          disabled={busy}
          onClick={async () => {
            setBusy(true)
            await onConfirm()
          }}
        >
          Solicitar destacar
        </button>
      </div>
    </div>
  )
}

function SentModal({ titulo }: { titulo: string }) {
  const { closeModal } = useUi()
  return (
    <div className="modal" style={{ textAlign: 'center', alignItems: 'center' }}>
      <span className="nic" style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--success-l)', color: 'var(--success)' }}>
        <Icon name="check" size={30} />
      </span>
      <ModalHead
        title="Solicitud enviada"
        sub={`Tu solicitud para destacar ${titulo} fue enviada correctamente. Si se aprueba, estará destacada durante 1 mes. El equipo de Karsy la revisará pronto.`}
      />
      <button className="btn btn-primary btn-block" onClick={closeModal}>
        Entendido
      </button>
    </div>
  )
}

/**
 * Acciones del dueño sobre una publicación (OwnerActionsBar de la app): editar, destacar,
 * deshabilitar/habilitar y marcar como vendido/disponible. [onChanged] recarga la lista.
 */
export function useOwnerActions(onChanged: () => void) {
  const { openModal, closeModal, toast } = useUi()
  const navigate = useNavigate()

  const correr = async (accion: () => Promise<void>, ok: string) => {
    try {
      await accion()
      toast(ok)
      onChanged()
    } catch (e) {
      toast(mensajeUsuario(e))
    }
  }

  return {
    editar: (c: OwnCar) => navigate(`/publicar/${c.id}`),
    destacar: (c: OwnCar) => {
      const titulo = `${c.brand} ${c.model}`
      const enviar = async () => {
        try {
          await requestFeatured(c.id)
          onChanged()
          openModal(<SentModal titulo={titulo} />)
        } catch (e) {
          closeModal()
          toast(mensajeUsuario(e))
        }
      }
      // Renovar un destacado que está por terminar va directo.
      if (c.featured && c.featuredEndsSoon) void enviar()
      else openModal(<FeatureModal onConfirm={enviar} />)
    },
    pausar: (c: OwnCar) =>
      openModal(
        <ConfirmModal
          title="¿Deshabilitar publicación?"
          message="Dejará de aparecer para los compradores. Puedes volver a habilitarla cuando quieras."
          confirm="Deshabilitar"
          danger
          onConfirm={() => correr(() => setPaused(c.id, true), 'Publicación deshabilitada: ya no aparece para los compradores.')}
        />,
      ),
    habilitar: (c: OwnCar) => void correr(() => setPaused(c.id, false), 'Publicación habilitada de nuevo.'),
    vendido: (c: OwnCar) =>
      openModal(
        <ConfirmModal
          title="¿Marcar como vendido?"
          message="Dejará de aparecer para los compradores y quienes lo tengan en favoritos verán que ya se vendió. Puedes volver a marcarlo como disponible cuando quieras."
          confirm="Confirmar"
          onConfirm={() => {
            // Se abre después de que se cierre esta ventana.
            setTimeout(() => openModal(<SoldViaModal onDone={(via) => correr(() => setSold(c.id, true, via), 'Publicación marcada como vendida.')} />), 0)
          }}
        />,
      ),
    disponible: (c: OwnCar) =>
      openModal(
        <ConfirmModal
          title="¿Marcar como disponible?"
          message="Se quitará la marca de vendido y tu publicación volverá a aparecer para los compradores."
          confirm="Marcar como disponible"
          onConfirm={() => correr(() => setSold(c.id, false), 'Tu publicación vuelve a estar disponible.')}
        />,
      ),
  }
}
