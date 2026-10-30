import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { mensajeUsuario } from '../data/auth'
import { datosPagoDestacado, referenciaPago, requestFeatured, sePuedeOcultar, setPaused, setSold, subirComprobante, type DatosPago, type OwnCar } from '../data/seller'
import { traducir, useI18n } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { precio } from '../lib/format'
import { useSession } from '../session/SessionContext'
import { ConfirmModal } from './Confirm'
import { ConstanciaField } from './ConstanciaField'
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

/**
 * "Destacar tu auto" (solo lotes): precio y días, la cuenta (banco y CLABE) y la referencia para
 * transferir, el comprobante (foto o PDF) y la casilla de que el pago no es reembolsable. Sube el
 * comprobante y pide el destacado con solicitar_destacado. Renovar es lo mismo (también se paga).
 */
function FeatureModal({ c, onSent }: { c: OwnCar; onSent: () => void }) {
  const { closeModal, openModal, toast } = useUi()
  const { t } = useI18n()
  // undefined: cargando.
  const [datos, setDatos] = useState<DatosPago | undefined>(undefined)
  const [errorCarga, setErrorCarga] = useState(false)
  const [intento, setIntento] = useState(0)
  const [archivo, setArchivo] = useState<File | null>(null)
  const [acepto, setAcepto] = useState(false)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    let vivo = true
    setDatos(undefined)
    setErrorCarga(false)
    datosPagoDestacado()
      .then((d) => vivo && setDatos(d))
      .catch(() => vivo && setErrorCarga(true))
    return () => {
      vivo = false
    }
  }, [intento])

  const disponible = !!datos?.banco && !!datos?.clabe
  const referencia = referenciaPago(c.id)
  const monto = datos ? `${precio(datos.precio)} MXN` : ''
  const copiar = (texto: string) => {
    void navigator.clipboard
      .writeText(texto)
      .then(() => toast(t('vehiculo.feature.copied')))
      .catch(() => undefined)
  }
  const enviar = async () => {
    if (!archivo) return
    setEnviando(true)
    try {
      const ruta = await subirComprobante(archivo)
      await requestFeatured(c.id, ruta)
      onSent()
      openModal(<SentModal />)
    } catch (e) {
      // La ventana sigue abierta con lo que ya eligió, para volver a intentar.
      toast(mensajeUsuario(e))
      setEnviando(false)
    }
  }
  const copiable = (valor: string) => (
    <div className="row gap-2">
      <code className="grow num" style={{ fontSize: 'var(--fs-md)', fontWeight: 700, letterSpacing: '.04em', userSelect: 'all', wordBreak: 'break-all' }}>
        {valor}
      </code>
      <button type="button" className="btn btn-ghost btn-sm" onClick={() => copiar(valor)}>
        <Icon name="copy" size={14} />
        {t('vehiculo.feature.copy')}
      </button>
    </div>
  )

  return (
    <div className="modal wide">
      <span className="nic empty-icon warn">
        <Icon name="star" size={26} />
      </span>
      <ModalHead title={t('vehiculo.feature.title')} sub={`${c.brand} ${c.model} ${c.year || ''}`.trim()} />
      {errorCarga ? (
        <div className="note error ink" style={{ display: 'flex', gap: 'var(--s-3)', alignItems: 'center', flexWrap: 'wrap' }}>
          <span className="grow">{t('vehiculo.feature.loadError')}</span>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setIntento((n) => n + 1)}>
            {t('common.retry')}
          </button>
        </div>
      ) : !datos ? (
        <div className="skeleton" style={{ height: 240 }} />
      ) : (
        <>
          <p className="muted">{t('vehiculo.feature.sub', { dias: datos.dias })}</p>
          <div className="card card-pad-sm row" style={{ flexWrap: 'wrap', justifyContent: 'space-between' }}>
            <b className="ink num" style={{ fontFamily: 'var(--f-display)', fontSize: 'var(--fs-3xl)', lineHeight: 'var(--lh-tight)' }}>
              {monto}
            </b>
            <span className="badge b-warn">{t('vehiculo.feature.days', { dias: datos.dias })}</span>
          </div>
          {!disponible ? (
            <div className="note warn ink">{t('vehiculo.feature.unavailable')}</div>
          ) : (
            <>
              <ol className="col gap-4" style={{ margin: 0, paddingLeft: 'var(--s-5)' }}>
                <li>
                  <p className="ink" style={{ marginBottom: 'var(--s-2)' }}>
                    {t('vehiculo.feature.step1', { precio: monto })}
                  </p>
                  <div className="card card-pad-sm col gap-2">
                    <p className="row" style={{ justifyContent: 'space-between' }}>
                      <span className="muted">{t('vehiculo.feature.bank')}</span>
                      <b style={{ textAlign: 'right' }}>{datos.banco}</b>
                    </p>
                    <span className="muted text-sm">{t('vehiculo.feature.clabe')}</span>
                    {copiable(datos.clabe ?? '')}
                    <span className="muted text-xs">{t('vehiculo.feature.holderNote')}</span>
                  </div>
                </li>
                <li>
                  <p className="ink" style={{ marginBottom: 'var(--s-2)' }}>
                    {t('vehiculo.feature.step2')}
                  </p>
                  {copiable(referencia)}
                </li>
                <li>
                  <p className="ink" style={{ marginBottom: 'var(--s-2)' }}>
                    {t('vehiculo.feature.step3')}
                  </p>
                  <ConstanciaField file={archivo} onChange={setArchivo} showLabel={false} help={t('vehiculo.feature.proofHelp')} />
                </li>
              </ol>
              <div className="note info ink">{t('vehiculo.feature.note', { dias: datos.dias })}</div>
              <div className="note warn ink">{t('vehiculo.feature.noRefund')}</div>
              <label className="row gap-2" style={{ alignItems: 'flex-start' }}>
                <input type="checkbox" checked={acepto} onChange={(e) => setAcepto(e.target.checked)} />
                <span>{t('vehiculo.feature.accept')}</span>
              </label>
            </>
          )}
        </>
      )}
      <div className="grid2">
        <button className="btn btn-ghost" disabled={enviando} onClick={closeModal}>
          {t('common.cancel')}
        </button>
        {disponible && (
          <button className="btn btn-primary" disabled={!archivo || !acepto || enviando} onClick={() => void enviar()}>
            {enviando ? t('vehiculo.feature.sending') : t('vehiculo.feature.confirm')}
          </button>
        )}
      </div>
    </div>
  )
}

function SentModal() {
  const { closeModal } = useUi()
  const { t } = useI18n()
  return (
    <div className="modal" style={{ textAlign: 'center', alignItems: 'center' }}>
      <span className="nic done-icon">
        <Icon name="check" size={30} />
      </span>
      <ModalHead title={t('vehiculo.feature.sentTitle')} sub={t('vehiculo.feature.sentSub')} />
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
  const { openModal, toast } = useUi()
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
    // Destacar y renovar: misma ventana (los dos se pagan).
    destacar: (c: OwnCar) => openModal(<FeatureModal c={c} onSent={onChanged} />),
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
  const navigate = useNavigate()
  // Destacar (y renovar) es solo para lotes; la BD también lo valida en solicitar_destacado.
  const esLote = useSession().account?.tipoCuenta === 'lote'
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
        ) : esLote && (!c.featured || c.featuredEndsSoon) ? (
          <button className="btn btn-soft btn-sm" title={t('perfil.featureHint')} onClick={() => acciones.destacar(c)}>
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
      {/* Publicación nueva con los mismos datos (útil para lotes con autos parecidos). */}
      <button className="btn btn-ghost btn-sm" title={t('perfil.duplicateHint')} onClick={() => navigate(`/publicar?duplicar=${c.id}`)}>
        <Icon name="copy" size={14} />
        {t('perfil.duplicate')}
      </button>
    </>
  )
}
