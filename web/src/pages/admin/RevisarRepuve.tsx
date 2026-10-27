import { useEffect, useState } from 'react'
import { Icon } from '../../components/Icon'
import { ModalHead, useUi } from '../../components/Ui'
import { documentoVehiculo, documentoVehiculoUrl } from '../../data/seller'
import type { Car, DocumentoVehiculo } from '../../data/types'
import { useI18n } from '../../i18n/I18nContext'

/** Consulta ciudadana del Registro Público Vehicular (por NIV o placa). */
const REPUVE = 'https://www2.repuve.gob.mx:8443/ciudadania/'

/**
 * Revisar una publicación pendiente en REPUVE antes de aprobarla (igual que "Revisar CURP"):
 * guía, el NIV o la placa para copiar (o el archivo de la tarjeta / baja para leerlos), la
 * marca, modelo y año de la publicación para comparar, y dos casillas que activan Aprobar.
 * Sin documento (publicaciones anteriores) se avisa y basta con una casilla.
 * Rechazar pide el motivo (como siempre). La ventana solo se cierra si se aprobó.
 */
export function RevisarRepuveModal({ car, recargar, onAprobar, onRechazar }: { car: Car; recargar?: number | null; onAprobar: () => Promise<boolean>; onRechazar: () => void }) {
  const { closeModal, toast } = useUi()
  const { t } = useI18n()
  // undefined: cargando; null: el vendedor no dio documento.
  const [doc, setDoc] = useState<DocumentoVehiculo | null | undefined>(undefined)
  // No se pudo leer: no es lo mismo que "sin documento" (no se deja aprobar con una sola casilla).
  const [errorCarga, setErrorCarga] = useState(false)
  const [intento, setIntento] = useState(0)
  const [coincide, setCoincide] = useState(false)
  const [sinRobo, setSinRobo] = useState(false)
  const [revisada, setRevisada] = useState(false)
  const [aprobando, setAprobando] = useState(false)

  useEffect(() => {
    let vivo = true
    setDoc(undefined)
    setErrorCarga(false)
    documentoVehiculo(car.id)
      .then((d) => vivo && setDoc(d))
      .catch(() => vivo && setErrorCarga(true))
    return () => {
      vivo = false
    }
  }, [car.id, recargar, intento])

  const escrito = doc?.tipo === 'vin' || doc?.tipo === 'placa'
  const listo = doc === null ? revisada : !!doc && coincide && sinRobo

  const copiar = () => {
    if (!doc) return
    void navigator.clipboard
      .writeText(doc.valor)
      .then(() => toast(t('admin.repuveCopied')))
      .catch(() => undefined)
  }
  const verArchivo = async () => {
    if (!doc) return
    // La pestaña se abre antes de pedir la URL para que el navegador no la bloquee.
    const w = window.open('', '_blank')
    try {
      const url = await documentoVehiculoUrl(doc.valor)
      if (w) {
        w.opener = null
        w.location.href = url
      } else window.location.href = url
    } catch {
      w?.close()
      toast(t('admin.verifOpenError'))
    }
  }
  const fila = (etiqueta: string, valor: string | number) => (
    <p className="row" style={{ justifyContent: 'space-between' }}>
      <span className="muted">{etiqueta}</span>
      <b style={{ textAlign: 'right' }}>{valor || '—'}</b>
    </p>
  )
  const casilla = (marcada: boolean, cambiar: (v: boolean) => void, texto: string) => (
    <label className="row gap-2" style={{ alignItems: 'flex-start' }}>
      <input type="checkbox" checked={marcada} onChange={(e) => cambiar(e.target.checked)} />
      <span>{texto}</span>
    </label>
  )

  return (
    <div className="modal">
      <ModalHead title={t('admin.repuveTitle', { auto: `${car.brand} ${car.model} ${car.year || ''}`.trim() })} sub={t('admin.repuveSub')} />
      {errorCarga ? (
        <div className="note error ink" style={{ display: 'flex', gap: 'var(--s-3)', alignItems: 'center', flexWrap: 'wrap' }}>
          <span className="grow">{t('admin.repuveLoadError')}</span>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setIntento((n) => n + 1)}>
            {t('common.retry')}
          </button>
        </div>
      ) : doc === undefined ? (
        <div className="skeleton" style={{ height: 160 }} />
      ) : doc === null ? (
        <div className="note warn ink">
          {t('admin.repuveNoDocument')}
        </div>
      ) : (
        <>
          <div className="note info ink">
            <ol className="col" style={{ margin: 0, paddingLeft: 'var(--s-5)', gap: 'var(--s-1)' }}>
              <li>{t(escrito ? 'admin.repuveStep1Copy' : 'admin.repuveStep1File')}</li>
              <li>{t('admin.repuveStep2')}</li>
              <li>{t('admin.repuveStep3')}</li>
              <li>{t('admin.repuveStep4')}</li>
              <li>{t('admin.repuveStep5')}</li>
            </ol>
          </div>
          <div className="row gap-2" style={{ flexWrap: 'wrap' }}>
            {escrito ? (
              <>
                <span className="muted text-sm">
                  {t(doc.tipo === 'vin' ? 'publicar.vin' : 'publicar.docPlate')}
                </span>
                <code style={{ fontSize: 'var(--fs-lg)', fontWeight: 700, letterSpacing: '.04em', userSelect: 'all', flex: '1 1 auto', wordBreak: 'break-all' }}>{doc.valor}</code>
                <button type="button" className="btn btn-ghost btn-sm" onClick={copiar}>
                  <Icon name="doc" size={14} />
                  {t('admin.curpCopy')}
                </button>
              </>
            ) : (
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => void verArchivo()}>
                <Icon name="eye" size={14} />
                {t(doc.tipo === 'baja' ? 'admin.repuveViewBaja' : 'admin.repuveViewCard')}
              </button>
            )}
            <a className="btn btn-primary btn-sm" href={REPUVE} target="_blank" rel="noopener noreferrer">
              {t('admin.repuveOpen')}
            </a>
          </div>
        </>
      )}
      <div className="card col card-pad-sm gap-2">
        <div className="label">{t('admin.repuveListingData')}</div>
        {fila(t('admin.repuveBrand'), car.brand)}
        {fila(t('admin.repuveModel'), car.model)}
        {fila(t('admin.repuveYear'), car.year)}
      </div>
      {doc === null
        ? casilla(revisada, setRevisada, t('admin.repuveCheckReviewed'))
        : doc && (
            <>
              {casilla(coincide, setCoincide, t('admin.repuveCheckMatch'))}
              {casilla(sinRobo, setSinRobo, t('admin.repuveCheckNoTheft'))}
            </>
          )}
      <div className="row gap-2" style={{ justifyContent: 'flex-end' }}>
        <button className="btn btn-ghost" disabled={aprobando} onClick={onRechazar}>
          {t('admin.reject')}
        </button>
        <button
          className="btn btn-primary"
          disabled={!listo || aprobando}
          onClick={() => {
            setAprobando(true)
            // Si falla, la ventana se queda abierta (con las casillas marcadas) y sale el aviso.
            void onAprobar().then((ok) => {
              setAprobando(false)
              if (ok) closeModal()
            })
          }}
        >
          {t('admin.approve')}
        </button>
      </div>
    </div>
  )
}
