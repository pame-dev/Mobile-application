import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { mensajeUsuario } from '../data/auth'
import { requestFeatured, sePuedeOcultar, setPaused, setSold, type OwnCar } from '../data/seller'
import { traducir, useI18n } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
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

/** Clave del texto para un estado de publicación (el estado es un valor interno en español). */
export const statusLabel = (status: string): Clave =>
  status in STATUS_BADGE ? (`vehiculo.status.${status}` as Clave) : 'vehiculo.status.Activo'

export const fechaCorta = (iso: string) =>
  new Date(iso).toLocaleDateString(traducir('vehiculo.locale'), { day: 'numeric', month: 'short' })

/** "¿Dónde lo vendiste?" (dentro o fuera de la app). */
function SoldViaModal({ onDone }: { onDone: (via: string) => Promise<void> }) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  const [busy, setBusy] = useState(false)
  const elegir = async (via: string) => {
    setBusy(true)
    await onDone(via)
    closeModal()
  }
  return (
    <div className="modal">
      <ModalHead title={t('vehiculo.soldVia.title')} sub={t('vehiculo.soldVia.sub')} />
      <button className="btn btn-primary btn-block" disabled={busy} onClick={() => void elegir('dentro_app')}>
        {t('vehiculo.soldVia.inside')}
      </button>
      <button className="btn btn-ghost btn-block" disabled={busy} onClick={() => void elegir('fuera_app')}>
        {t('vehiculo.soldVia.outside')}
      </button>
    </div>
  )
}

/** "¿Quieres destacar tu vehículo?" */
function FeatureModal({ onConfirm }: { onConfirm: () => Promise<void> }) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  const [busy, setBusy] = useState(false)
  return (
    <div className="modal">
      <span className="nic" style={{ width: 56, height: 56, borderRadius: 16, background: 'var(--warning-l)', color: 'var(--star)' }}>
        <Icon name="star" size={26} />
      </span>
      <ModalHead title={t('vehiculo.feature.title')} sub={t('vehiculo.feature.sub')} />
      <div className="note" style={{ background: 'var(--teal-light)' }}>
        {t('vehiculo.feature.note')}
      </div>
      <div className="grid2">
        <button className="btn btn-ghost" disabled={busy} onClick={closeModal}>
          {t('common.cancel')}
        </button>
        <button
          className="btn btn-primary"
          disabled={busy}
          onClick={async () => {
            setBusy(true)
            await onConfirm()
          }}
        >
          {t('vehiculo.feature.confirm')}
        </button>
      </div>
    </div>
  )
}

function SentModal({ titulo }: { titulo: string }) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  return (
    <div className="modal" style={{ textAlign: 'center', alignItems: 'center' }}>
      <span className="nic" style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--success-l)', color: 'var(--success)' }}>
        <Icon name="check" size={30} />
      </span>
      <ModalHead title={t('vehiculo.feature.sentTitle')} sub={t('vehiculo.feature.sentSub', { v: titulo })} />
      <button className="btn btn-primary btn-block" onClick={closeModal}>
        {t('vehiculo.gotIt')}
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
  const { t } = useI18n()
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
          title={t('vehiculo.pause.title')}
          message={t('vehiculo.pause.msg')}
          confirm={t('vehiculo.pause.confirm')}
          danger
          onConfirm={() => correr(() => setPaused(c.id, true), t('vehiculo.pause.done'))}
        />,
      ),
    habilitar: (c: OwnCar) => void correr(() => setPaused(c.id, false), t('vehiculo.enable.done')),
    vendido: (c: OwnCar) =>
      openModal(
        <ConfirmModal
          title={t('vehiculo.sold.title')}
          message={t('vehiculo.sold.msg')}
          confirm={t('vehiculo.sold.confirm')}
          onConfirm={() => {
            // Se abre después de que se cierre esta ventana.
            setTimeout(() => openModal(<SoldViaModal onDone={(via) => correr(() => setSold(c.id, true, via), t('vehiculo.sold.done'))} />), 0)
          }}
        />,
      ),
    disponible: (c: OwnCar) =>
      openModal(
        <ConfirmModal
          title={t('vehiculo.available.title')}
          message={t('vehiculo.available.msg')}
          confirm={t('vehiculo.available.confirm')}
          onConfirm={() => correr(() => setSold(c.id, false), t('vehiculo.available.done'))}
        />,
      ),
  }
}

/**
 * Botones de gestión de una publicación propia según su estado (destacar, editar, vendido,
 * deshabilitar/habilitar, ocultar). Se usan en Mi panel y en el detalle de la publicación.
 */
export function OwnerButtons({ c, acciones, oculta, onOcultar }: { c: OwnCar; acciones: ReturnType<typeof useOwnerActions>; oculta: boolean; onOcultar: (v: boolean) => void }) {
  const { t } = useI18n()
  return (
    <>
      {c.status === 'Rechazado' && (
        <button className="btn btn-primary btn-sm" onClick={() => acciones.editar(c)}>
          {t('perfil.fixResubmit')}
        </button>
      )}
      {c.status === 'Activo' &&
        (c.featuredPending ? (
          <button className="btn btn-ghost btn-sm" disabled>
            {t('perfil.requestSent')}
          </button>
        ) : !c.featured || c.featuredEndsSoon ? (
          <button className="btn btn-soft btn-sm" onClick={() => acciones.destacar(c)}>
            <Icon name="star" size={14} />
            {c.featured ? t('perfil.renewFeatured') : t('perfil.feature')}
          </button>
        ) : null)}
      {(c.status === 'Activo' || c.status === 'Pausado' || c.status === 'Pendiente') && (
        <button className="btn btn-ghost btn-sm" onClick={() => acciones.editar(c)}>
          <Icon name="edit" size={14} />
          {t('perfil.edit')}
        </button>
      )}
      {c.status === 'Activo' && (
        <>
          <button className="btn btn-ghost btn-sm" onClick={() => acciones.vendido(c)}>
            <Icon name="sold" size={14} />
            {t('perfil.markSold')}
          </button>
          <button className="btn btn-ghost btn-sm" onClick={() => acciones.pausar(c)}>
            <Icon name="pause" size={14} />
            {t('perfil.disable')}
          </button>
        </>
      )}
      {c.status === 'Pausado' && (
        <button className="btn btn-primary btn-sm" onClick={() => acciones.habilitar(c)}>
          {t('perfil.enable')}
        </button>
      )}
      {c.status === 'Vendido' && (
        <button className="btn btn-ghost btn-sm" onClick={() => acciones.disponible(c)}>
          {t('perfil.markAvailable')}
        </button>
      )}
      {sePuedeOcultar(c) && (
        <button className="btn btn-ghost btn-sm" onClick={() => onOcultar(!oculta)}>
          <Icon name="eye" size={14} />
          {oculta ? t('perfil.showOnProfile') : t('perfil.hide')}
        </button>
      )}
    </>
  )
}
