import { useEffect, useState } from 'react'
import { Icon } from '../../components/Icon'
import { ModalHead, useUi } from '../../components/Ui'
import { comprobanteUrl, guardarConfigPagos, type DestacadoAdmin } from '../../data/admin'
import { mensajeUsuario } from '../../data/auth'
import { datosPagoDestacado } from '../../data/seller'
import { useI18n } from '../../i18n/I18nContext'
import { precio } from '../../lib/format'

/** Monto con el formato de precio de Karsy: "$300 MXN". */
export const montoPago = (monto: number) => `${precio(monto)} MXN`

/**
 * Abre el comprobante de un pago en otra pestaña (URL firmada de 10 minutos). La pestaña se abre
 * antes de pedir la URL para que el navegador no la bloquee.
 */
export function useVerComprobante() {
  const { toast } = useUi()
  const { t } = useI18n()
  return async (ruta: string) => {
    const w = window.open('', '_blank')
    try {
      const url = await comprobanteUrl(ruta)
      if (w) {
        w.opener = null
        w.location.href = url
      } else window.location.href = url
    } catch {
      w?.close()
      toast(t('admin.verifOpenError'))
    }
  }
}

/**
 * Revisar el pago de una solicitud de destacado antes de aprobarla (igual que "Revisar en REPUVE"):
 * ver el comprobante, el monto y la referencia esperados para copiar, el lote y el auto, una guía
 * y tres casillas que activan Aprobar. Rechazar pide el motivo (como siempre). Si se aprueba,
 * quien la abrió muestra la ventana de aprobada; si falla, esta se queda abierta.
 */
export function RevisarPagoModal({ d, auto, onAprobar, onRechazar }: { d: DestacadoAdmin; auto: string; onAprobar: () => Promise<boolean>; onRechazar: () => void }) {
  const { toast } = useUi()
  const { t } = useI18n()
  const verComprobante = useVerComprobante()
  const [monto, setMonto] = useState(false)
  const [encontrado, setEncontrado] = useState(false)
  const [recibido, setRecibido] = useState(false)
  const [aprobando, setAprobando] = useState(false)
  const textoMonto = d.monto != null ? montoPago(d.monto) : '—'
  const referencia = d.referencia ?? ''

  const copiar = (texto: string) => {
    void navigator.clipboard
      .writeText(texto)
      .then(() => toast(t('admin.payCopied')))
      .catch(() => undefined)
  }
  const fila = (etiqueta: string, valor: string, copiable = false) => (
    <div className="row gap-2" style={{ justifyContent: 'space-between', flexWrap: 'wrap' }}>
      <span className="muted">{etiqueta}</span>
      <span className="row gap-2">
        <b className="num" style={{ textAlign: 'right', userSelect: copiable ? 'all' : undefined }}>{valor || '—'}</b>
        {copiable && valor && (
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => copiar(valor)}>
            <Icon name="copy" size={14} />
            {t('admin.curpCopy')}
          </button>
        )}
      </span>
    </div>
  )
  const casilla = (marcada: boolean, cambiar: (v: boolean) => void, texto: string) => (
    <label className="row gap-2" style={{ alignItems: 'flex-start' }}>
      <input type="checkbox" checked={marcada} onChange={(e) => cambiar(e.target.checked)} />
      <span>{texto}</span>
    </label>
  )

  return (
    <div className="modal">
      <ModalHead title={t('admin.payReviewTitle', { auto })} sub={t('admin.payReviewSub')} />
      <div className="note info ink">
        <ol className="col" style={{ margin: 0, paddingLeft: 'var(--s-5)', gap: 'var(--s-1)' }}>
          <li>{t('admin.payStep1')}</li>
          <li>{t('admin.payStep2', { monto: textoMonto, ref: referencia })}</li>
          <li>{t('admin.payStep3')}</li>
          <li>{t('admin.payStep4')}</li>
        </ol>
      </div>
      <button type="button" className="btn btn-primary btn-sm" style={{ alignSelf: 'flex-start' }} disabled={!d.comprobante_path} onClick={() => d.comprobante_path && void verComprobante(d.comprobante_path)}>
        <Icon name="doc" size={14} />
        {t('admin.payViewProof')}
      </button>
      <div className="card col card-pad-sm gap-2">
        {fila(t('admin.payAmount'), textoMonto, true)}
        {fila(t('admin.payReference'), referencia, true)}
        {fila(t('admin.payLot'), d.solicitante)}
        {fila(t('admin.payCar'), auto)}
      </div>
      {casilla(monto, setMonto, t('admin.payCheckAmount'))}
      {casilla(encontrado, setEncontrado, t('admin.payCheckFound'))}
      {casilla(recibido, setRecibido, t('admin.payCheckReceived'))}
      <div className="row gap-2" style={{ justifyContent: 'flex-end' }}>
        <button className="btn btn-ghost" disabled={aprobando} onClick={onRechazar}>
          {t('admin.reject')}
        </button>
        <button
          className="btn btn-primary"
          disabled={!monto || !encontrado || !recibido || aprobando}
          onClick={() => {
            setAprobando(true)
            // Si falla, la ventana se queda abierta (con las casillas marcadas) y sale el aviso.
            void onAprobar().then((ok) => {
              if (!ok) setAprobando(false)
            })
          }}
        >
          {t('admin.approve')}
        </button>
      </div>
    </div>
  )
}

/**
 * "Datos para recibir pagos": precio, días, banco y CLABE que ven los lotes al pedir un destacado
 * (config_pagos). Mientras falten banco o CLABE nadie puede pedir destacados.
 */
export function ConfigPagos() {
  const { toast } = useUi()
  const { t } = useI18n()
  // null: cargando o no se pudo leer.
  const [guardado, setGuardado] = useState<{ banco: string | null; clabe: string | null } | null>(null)
  const [errorCarga, setErrorCarga] = useState(false)
  const [intento, setIntento] = useState(0)
  const [precioTxt, setPrecioTxt] = useState('')
  const [dias, setDias] = useState('')
  const [banco, setBanco] = useState('')
  const [clabe, setClabe] = useState('')
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    let vivo = true
    setErrorCarga(false)
    datosPagoDestacado()
      .then((d) => {
        if (!vivo) return
        setGuardado({ banco: d.banco, clabe: d.clabe })
        setPrecioTxt(String(d.precio))
        setDias(String(d.dias))
        setBanco(d.banco ?? '')
        setClabe(d.clabe ?? '')
      })
      .catch(() => vivo && setErrorCarga(true))
    return () => {
      vivo = false
    }
  }, [intento])

  const clabeMal = clabe !== '' && !/^\d{18}$/.test(clabe)
  const precioNum = Number(precioTxt)
  const diasNum = Number(dias)
  const valido = precioNum > 0 && Number.isInteger(diasNum) && diasNum >= 1 && diasNum <= 365 && !clabeMal

  const guardar = async () => {
    setGuardando(true)
    try {
      await guardarConfigPagos(precioNum, diasNum, banco, clabe)
      setGuardado({ banco: banco.trim() || null, clabe: clabe || null })
      toast(t('admin.payCfgSaved'))
    } catch (e) {
      toast(mensajeUsuario(e))
    }
    setGuardando(false)
  }

  return (
    <div className="card card-pad col gap-3" style={{ marginBottom: 'var(--s-4)' }}>
      <div>
        <h2 className="card-title tight">{t('admin.payCfgTitle')}</h2>
        <p className="muted text-sm">{t('admin.payCfgDesc')}</p>
      </div>
      {errorCarga ? (
        <div className="note error ink" style={{ display: 'flex', gap: 'var(--s-3)', alignItems: 'center', flexWrap: 'wrap' }}>
          <span className="grow">{t('admin.payCfgLoadError')}</span>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setIntento((n) => n + 1)}>
            {t('common.retry')}
          </button>
        </div>
      ) : !guardado ? (
        <div className="skeleton" style={{ height: 140 }} />
      ) : (
        <>
          {(!guardado.banco || !guardado.clabe) && <div className="note warn ink">{t('admin.payCfgMissing')}</div>}
          <div className="grid2">
            <div className="field">
              <label htmlFor="pago-precio">{t('admin.payCfgPrice')}</label>
              <input id="pago-precio" className="input num" type="number" inputMode="decimal" min={1} max={100000} step="0.01" value={precioTxt} onChange={(e) => setPrecioTxt(e.target.value)} />
            </div>
            <div className="field">
              <label htmlFor="pago-dias">{t('admin.payCfgDays')}</label>
              <input id="pago-dias" className="input num" type="number" inputMode="numeric" min={1} max={365} step="1" value={dias} onChange={(e) => setDias(e.target.value)} />
            </div>
            <div className="field">
              <label htmlFor="pago-banco">{t('admin.payCfgBank')}</label>
              <input id="pago-banco" className="input" maxLength={60} value={banco} onChange={(e) => setBanco(e.target.value)} />
            </div>
            <div className="field">
              <label htmlFor="pago-clabe">{t('admin.payCfgClabe')}</label>
              <input
                id="pago-clabe"
                className="input num"
                inputMode="numeric"
                maxLength={18}
                value={clabe}
                aria-invalid={clabeMal}
                onChange={(e) => setClabe(e.target.value.replace(/\D/g, '').slice(0, 18))}
              />
              {clabeMal && <span className="ferr">{t('admin.payCfgClabeError')}</span>}
            </div>
          </div>
          <button className="btn btn-primary" style={{ alignSelf: 'flex-end' }} disabled={!valido || guardando} onClick={() => void guardar()}>
            {guardando ? t('common.saving') : t('common.save')}
          </button>
        </>
      )}
    </div>
  )
}
