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

/** Bancos según los 3 primeros dígitos de la CLABE (los más comunes; mismos que la app). */
const BANCOS_CLABE: Record<string, string> = {
  '002': 'BANAMEX', '012': 'BBVA', '014': 'SANTANDER', '021': 'HSBC', '030': 'BAJÍO', '036': 'INBURSA',
  '042': 'MIFEL', '044': 'SCOTIABANK', '058': 'BANREGIO', '062': 'AFIRME', '072': 'BANORTE', '127': 'AZTECA',
  '130': 'COMPARTAMOS', '137': 'BANCOPPEL', '138': 'ABC CAPITAL', '143': 'CIBANCO', '152': 'BANCREA',
  '156': 'SABADELL', '166': 'BANCO DEL BIENESTAR', '638': 'NU MÉXICO', '646': 'STP', '722': 'MERCADO PAGO',
  '728': 'SPIN BY OXXO',
}

/** Dígito de control de la CLABE (pesos 3, 7, 1): atrapa un número mal tecleado antes de guardarlo. */
export function clabeDigitoOk(clabe: string): boolean {
  if (!/^\d{18}$/.test(clabe)) return false
  const suma = [...clabe.slice(0, 17)].reduce((s, d, i) => s + ((Number(d) * [3, 7, 1][i % 3]) % 10), 0)
  return (10 - (suma % 10)) % 10 === Number(clabe[17])
}

export const bancoDeClabe = (clabe: string): string | null => (clabe.length >= 3 ? BANCOS_CLABE[clabe.slice(0, 3)] ?? null : null)

/** "032 180 00011835971 9": más fácil de leer y comparar (banco, plaza, cuenta y control). */
export const clabeAgrupada = (clabe: string) =>
  clabe.length === 18 ? `${clabe.slice(0, 3)} ${clabe.slice(3, 6)} ${clabe.slice(6, 17)} ${clabe[17]}` : clabe

interface ConfigGuardada {
  precio: string
  dias: string
  banco: string
  clabe: string
}

/**
 * "Datos para recibir pagos": precio, días, banco y CLABE que ven los lotes al pedir un destacado
 * (config_pagos). Mientras falten banco o CLABE nadie puede pedir destacados. El banco se llena
 * solo con la CLABE, la CLABE se revisa con su dígito de control y se ve cómo lo verán los lotes.
 */
export function ConfigPagos() {
  const { toast } = useUi()
  const { t } = useI18n()
  // null: cargando o no se pudo leer.
  const [guardado, setGuardado] = useState<ConfigGuardada | null>(null)
  const [errorCarga, setErrorCarga] = useState(false)
  const [intento, setIntento] = useState(0)
  const [precioTxt, setPrecioTxt] = useState('')
  const [dias, setDias] = useState('')
  const [banco, setBanco] = useState('')
  const [clabe, setClabe] = useState('')
  const [guardando, setGuardando] = useState(false)

  const poner = (g: ConfigGuardada) => {
    setPrecioTxt(g.precio)
    setDias(g.dias)
    // El campo Banco solo se usa con un banco que no se reconoce por la CLABE: si el guardado salió
    // de la CLABE, empieza vacío (no se arrastra a una CLABE de otro banco).
    setBanco(bancoDeClabe(g.clabe) ? '' : g.banco)
    setClabe(g.clabe)
  }

  useEffect(() => {
    let vivo = true
    setErrorCarga(false)
    datosPagoDestacado()
      .then((d) => {
        if (!vivo) return
        const g = { precio: String(d.precio), dias: String(d.dias), banco: d.banco ?? '', clabe: d.clabe ?? '' }
        setGuardado(g)
        poner(g)
      })
      .catch(() => vivo && setErrorCarga(true))
    return () => {
      vivo = false
    }
  }, [intento])

  const clabeCompleta = clabe.length === 18
  const clabeOk = clabeDigitoOk(clabe)
  const clabeMal = clabe !== '' && !clabeOk
  // El banco sale de la CLABE; solo se escribe si su banco no está en la lista.
  const bancoDetectado = clabeOk ? bancoDeClabe(clabe) : null
  const bancoFinal = bancoDetectado ?? banco.trim()
  const precioNum = Number(precioTxt)
  const diasNum = Number(dias)
  const precioOk = precioTxt.trim() !== '' && precioNum > 0 && precioNum <= 100000
  const diasOk = Number.isInteger(diasNum) && diasNum >= 1 && diasNum <= 365
  const bancoOk = bancoFinal !== ''
  // Con CLABE, el banco no puede quedar vacío (los lotes no sabrían a cuál transferir).
  const valido = precioOk && diasOk && !clabeMal && (clabe === '' || bancoOk)
  // Se compara el valor, no el texto: "300" y "300.00" o "BBVA " y "BBVA" no son cambios.
  const cambios =
    !!guardado &&
    (precioNum !== Number(guardado.precio) || diasNum !== Number(guardado.dias) || bancoFinal !== guardado.banco || clabe !== guardado.clabe)
  const activo = !!guardado && guardado.banco !== '' && guardado.clabe !== ''

  const guardar = async () => {
    setGuardando(true)
    try {
      await guardarConfigPagos(precioNum, diasNum, bancoFinal, clabe)
      const g = { precio: precioTxt, dias, banco: bancoFinal, clabe }
      setGuardado(g)
      setBanco(bancoDeClabe(g.clabe) ? '' : g.banco)
      toast(t('admin.payCfgSaved'))
    } catch (e) {
      toast(mensajeUsuario(e))
    }
    setGuardando(false)
  }

  return (
    <div className="card card-pad col gap-4" style={{ marginBottom: 'var(--s-4)' }}>
      <div className="row gap-3" style={{ alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <div>
          <h2 className="card-title tight">{t('admin.payCfgTitle')}</h2>
          <p className="muted text-sm">{t('admin.payCfgDesc')}</p>
        </div>
        {guardado && <span className={`badge ${activo ? 'b-succ' : 'b-warn'}`}>{t(activo ? 'admin.payCfgActive' : 'admin.payCfgIncomplete')}</span>}
      </div>
      {errorCarga ? (
        <div className="note error ink" style={{ display: 'flex', gap: 'var(--s-3)', alignItems: 'center', flexWrap: 'wrap' }}>
          <span className="grow">{t('admin.payCfgLoadError')}</span>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setIntento((n) => n + 1)}>
            {t('common.retry')}
          </button>
        </div>
      ) : !guardado ? (
        <div className="skeleton" style={{ height: 220 }} />
      ) : (
        <>
          {!activo && <div className="note warn ink">{t('admin.payCfgMissing')}</div>}

          <section className="col gap-3" aria-labelledby="pago-precio-titulo">
            <h3 id="pago-precio-titulo" className="label">{t('admin.payCfgPriceGroup')}</h3>
            <div className="grid2">
              <div className="field">
                <label htmlFor="pago-precio">{t('admin.payCfgPriceLabel')}</label>
                <div className="affix">
                  <span aria-hidden="true">$</span>
                  <input id="pago-precio" className="input num" type="number" inputMode="decimal" min={1} max={100000} step="0.01" aria-invalid={!precioOk} value={precioTxt} onChange={(e) => setPrecioTxt(e.target.value)} />
                  <span>MXN</span>
                </div>
              </div>
              <div className="field">
                <label htmlFor="pago-dias">{t('admin.payCfgDuration')}</label>
                <div className="affix">
                  <input id="pago-dias" className="input num" type="number" inputMode="numeric" min={1} max={365} step="1" aria-invalid={!diasOk} value={dias} onChange={(e) => setDias(e.target.value)} />
                  <span>{t('admin.payCfgDaysUnit')}</span>
                </div>
              </div>
            </div>
            {precioOk && diasOk && (
              <p className="muted text-sm">{t('admin.payCfgSummary', { monto: montoPago(precioNum), dias: diasNum })}</p>
            )}
          </section>

          <section className="col gap-3" aria-labelledby="pago-cuenta-titulo">
            <h3 id="pago-cuenta-titulo" className="label">{t('admin.payCfgAccountGroup')}</h3>
            <div className="grid2">
              <div className="field">
                <label htmlFor="pago-clabe">{t('admin.payCfgClabe')}</label>
                <input
                  id="pago-clabe"
                  className={`input num${clabeCompleta && !clabeOk ? ' bad' : ''}`}
                  style={{ fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace', letterSpacing: '.04em' }}
                  inputMode="numeric"
                  autoComplete="off"
                  maxLength={18}
                  value={clabe}
                  aria-invalid={clabeCompleta && !clabeOk}
                  aria-describedby="pago-clabe-estado"
                  onChange={(e) => setClabe(e.target.value.replace(/\D/g, '').slice(0, 18))}
                />
                <div id="pago-clabe-estado" className="row gap-2" style={{ justifyContent: 'space-between' }}>
                  {clabeOk ? (
                    <span className="text-sm" style={{ color: 'var(--b-succ)', display: 'inline-flex', alignItems: 'center', gap: 'var(--s-1)' }}>
                      <Icon name="check" size={14} />
                      {t('admin.payCfgClabeOk')}
                      {bancoDetectado && ` · ${bancoDetectado}`}
                    </span>
                  ) : clabeCompleta ? (
                    <span className="ferr">{t('admin.payCfgClabeInvalid')}</span>
                  ) : (
                    <span />
                  )}
                  <span className="muted text-sm num" aria-label={t('admin.payCfgClabeCount', { n: clabe.length })}>
                    {clabe.length}/18
                  </span>
                </div>
              </div>
              {clabeOk && !bancoDetectado && (
                <div className="field">
                  <label htmlFor="pago-banco">{t('admin.payCfgBank')}</label>
                  <input id="pago-banco" className="input" maxLength={60} value={banco} aria-describedby="pago-banco-ayuda" onChange={(e) => setBanco(e.target.value.toUpperCase())} />
                  <span id="pago-banco-ayuda" className="muted text-sm">
                    {t('admin.payCfgBankHint')}
                  </span>
                </div>
              )}
            </div>
          </section>

          {clabeOk && bancoOk && precioOk && diasOk && (
            <div className="note info ink col gap-1">
              <b>{t('admin.payCfgPreview')}</b>
              <span>
                {t('admin.payCfgBank')}: <b>{bancoFinal}</b>
              </span>
              <span>
                CLABE: <b className="num" style={{ fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' }}>{clabeAgrupada(clabe)}</b>
              </span>
              <span>
                <b>{montoPago(precioNum)}</b> · {diasNum} {t('admin.payCfgDaysUnit')}
              </span>
            </div>
          )}

          <div className="row gap-2" style={{ justifyContent: 'flex-end', alignItems: 'center', flexWrap: 'wrap' }}>
            {cambios && (
              <>
                <span className="fwarn grow" role="status">
                  {t('admin.payCfgUnsaved')}
                </span>
                <button type="button" className="btn btn-ghost" disabled={guardando} onClick={() => poner(guardado)}>
                  {t('admin.payCfgDiscard')}
                </button>
              </>
            )}
            <button className="btn btn-primary" disabled={!cambios || !valido || guardando} onClick={() => void guardar()}>
              {guardando ? t('common.saving') : t('admin.payCfgSaveChanges')}
            </button>
          </div>
        </>
      )}
    </div>
  )
}
