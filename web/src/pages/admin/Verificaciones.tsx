import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Icon } from '../../components/Icon'
import { ModalHead, useUi } from '../../components/Ui'
import { constanciaUrl, resolveVerification, type VerificacionAdmin } from '../../data/admin'
import { mensajeUsuario } from '../../data/auth'
import { useI18n } from '../../i18n/I18nContext'
import type { Clave } from '../../i18n/es'
import { haceCuanto } from '../../lib/format'
import { AdminHead, Ini, ReasonModal, Tabs } from './AdminParts'

type Estado = VerificacionAdmin['estado']

/** Página oficial para consultar una CURP. */
const GOB_CURP = 'https://www.gob.mx/curp/'

/**
 * Revisar la CURP de un particular: pasos para consultarla en gob.mx, la CURP para copiar,
 * y el nombre con el que se registró para compararlo con el de gob.mx. Aprobar se activa hasta
 * marcar las dos casillas; Rechazar pide el motivo (como en los demás documentos).
 */
function RevisarCurpModal({ v, onAprobar, onRechazar }: { v: VerificacionAdmin; onAprobar: () => Promise<boolean>; onRechazar: () => void }) {
  const { closeModal, toast } = useUi()
  const { t } = useI18n()
  const [existe, setExiste] = useState(false)
  const [nombre, setNombre] = useState(false)
  const [aprobando, setAprobando] = useState(false)
  const curp = v.curp ?? ''
  const copiar = () => {
    void navigator.clipboard
      .writeText(curp)
      .then(() => toast(t('admin.curpCopied')))
      .catch(() => undefined)
  }
  const fila = (etiqueta: string, valor: string) => (
    <p className="row" style={{ justifyContent: 'space-between', gap: 12, margin: 0 }}>
      <span className="muted">{etiqueta}</span>
      <b style={{ textAlign: 'right' }}>{valor}</b>
    </p>
  )
  return (
    <div className="modal" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <ModalHead title={t('admin.curpReviewTitle', { nombre: v.nombre_mostrar })} sub={t('admin.curpReviewSub')} />
      <div className="note" style={{ background: 'var(--teal-light)', color: 'var(--ink)' }}>
        <ol style={{ margin: 0, paddingLeft: 18, display: 'flex', flexDirection: 'column', gap: 4 }}>
          <li>{t('admin.curpStep1')}</li>
          <li>{t('admin.curpStep2')}</li>
          <li>{t('admin.curpStep3')}</li>
          <li>{t('admin.curpStep4')}</li>
        </ol>
      </div>
      <div className="row" style={{ gap: 8, flexWrap: 'wrap' }}>
        <code style={{ fontSize: 17, fontWeight: 700, letterSpacing: '.04em', userSelect: 'all', flex: '1 1 auto' }}>{curp}</code>
        <button type="button" className="btn btn-ghost btn-sm" disabled={!curp} onClick={copiar}>
          <Icon name="doc" size={14} />
          {t('admin.curpCopy')}
        </button>
        <a className="btn btn-primary btn-sm" href={GOB_CURP} target="_blank" rel="noopener noreferrer">
          {t('admin.curpOpenGob')}
        </a>
      </div>
      {/* Solo lo que dio en el registro: el nombre (gob.mx lo muestra para compararlo). */}
      <div className="card" style={{ padding: 14, display: 'flex', flexDirection: 'column', gap: 8 }}>
        {fila(t('admin.curpProfileName'), v.nombre_mostrar)}
      </div>
      <label className="row" style={{ gap: 8, alignItems: 'flex-start' }}>
        <input type="checkbox" checked={existe} onChange={(e) => setExiste(e.target.checked)} />
        <span>{t('admin.curpCheckExists')}</span>
      </label>
      <label className="row" style={{ gap: 8, alignItems: 'flex-start' }}>
        <input type="checkbox" checked={nombre} onChange={(e) => setNombre(e.target.checked)} />
        <span>{t('admin.curpCheckName')}</span>
      </label>
      <div className="row" style={{ gap: 8, justifyContent: 'flex-end' }}>
        <button className="btn btn-ghost" disabled={aprobando} onClick={onRechazar}>
          {t('admin.reject')}
        </button>
        <button
          className="btn btn-primary"
          disabled={!existe || !nombre || aprobando}
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

/**
 * Revisar la constancia de situación fiscal de un lote: guía de qué revisar, botón para ver
 * el documento (foto o PDF) y el nombre del lote para comparar. Aprobar se activa hasta marcar
 * las dos casillas; Rechazar pide el motivo (como en la CURP).
 */
function RevisarConstanciaModal({ v, onVer, onAprobar, onRechazar }: { v: VerificacionAdmin; onVer: () => void; onAprobar: () => Promise<boolean>; onRechazar: () => void }) {
  const { closeModal } = useUi()
  const { t } = useI18n()
  const [documento, setDocumento] = useState(false)
  const [nombre, setNombre] = useState(false)
  const [aprobando, setAprobando] = useState(false)
  return (
    <div className="modal" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <ModalHead title={t('admin.csfReviewTitle', { nombre: v.nombre_mostrar })} sub={t('admin.csfReviewSub')} />
      <div className="note" style={{ background: 'var(--teal-light)', color: 'var(--ink)' }}>
        <ol style={{ margin: 0, paddingLeft: 18, display: 'flex', flexDirection: 'column', gap: 4 }}>
          <li>{t('admin.csfStep1')}</li>
          <li>{t('admin.csfStep2')}</li>
          <li>{t('admin.csfStep3')}</li>
          <li>{t('admin.csfStep4')}</li>
          <li>{t('admin.csfStep5')}</li>
          <li>{t('admin.csfStep6')}</li>
        </ol>
      </div>
      <div className="row" style={{ gap: 12, flexWrap: 'wrap', justifyContent: 'space-between' }}>
        <p style={{ margin: 0 }}>
          <span className="muted">{t('admin.csfLotName')}</span>
          <br />
          <b>{v.nombre_mostrar}</b>
        </p>
        <button type="button" className="btn btn-primary btn-sm" disabled={!v.archivo_path} onClick={onVer}>
          <Icon name="doc" size={14} />
          {t('admin.verifOpen')}
        </button>
      </div>
      <label className="row" style={{ gap: 8, alignItems: 'flex-start' }}>
        <input type="checkbox" checked={documento} onChange={(e) => setDocumento(e.target.checked)} />
        <span>{t('admin.csfCheckDocument')}</span>
      </label>
      <label className="row" style={{ gap: 8, alignItems: 'flex-start' }}>
        <input type="checkbox" checked={nombre} onChange={(e) => setNombre(e.target.checked)} />
        <span>{t('admin.csfCheckName')}</span>
      </label>
      <div className="row" style={{ gap: 8, justifyContent: 'flex-end' }}>
        <button className="btn btn-ghost" disabled={aprobando} onClick={onRechazar}>
          {t('admin.reject')}
        </button>
        <button
          className="btn btn-primary"
          disabled={!documento || !nombre || aprobando}
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

const ESTADO: Record<Estado, [Clave, string]> = {
  pendiente: ['admin.verifTabPending', 'b-warn'],
  verificado: ['admin.verifTabVerified', 'b-succ'],
  rechazado: ['admin.verifTabRejected', 'b-dang'],
}

/**
 * Verificación de usuarios: documentos de lotes y particulares (verificaciones_usuario).
 * Ver el documento (URL firmada), aprobar o rechazar con motivo. Un tipo de documento nuevo
 * solo necesita su texto en admin.doc.<tipo>; mientras tanto se muestra el valor tal cual.
 */
export function Verificaciones({ verificaciones, listo }: { verificaciones: VerificacionAdmin[]; listo: (m: string) => void }) {
  const { openModal, toast } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const [estado, setEstado] = useState<Estado>('pendiente')
  const [busy, setBusy] = useState<string | null>(null)
  const cuenta = (e: Estado) => verificaciones.filter((v) => v.estado === e).length
  const lista = verificaciones.filter((v) => v.estado === estado)
  const documento = (d: string) => {
    // Un tipo sin texto en i18n da undefined: se muestra el valor de la BD.
    const texto = t(`admin.doc.${d}` as Clave) as string | undefined
    return texto || d
  }

  const abrir = async (v: VerificacionAdmin) => {
    if (!v.archivo_path) return
    // La pestaña se abre antes de pedir la URL para que el navegador no la bloquee.
    const w = window.open('', '_blank')
    try {
      const url = await constanciaUrl(v.archivo_path)
      if (w) {
        w.opener = null
        w.location.href = url
      } else window.location.href = url
    } catch {
      w?.close()
      toast(t('admin.verifOpenError'))
    }
  }

  /** true si se aprobó (las ventanas de revisión solo se cierran entonces). */
  const aprobar = async (v: VerificacionAdmin): Promise<boolean> => {
    setBusy(v.id)
    try {
      await resolveVerification(v.id, true, null)
      listo(t('admin.verifApproved'))
      return true
    } catch (e) {
      toast(mensajeUsuario(e))
      return false
    } finally {
      setBusy(null)
    }
  }

  const rechazar = (v: VerificacionAdmin) =>
    openModal(
      <ReasonModal
        title={t('admin.verifRejectTitle')}
        desc={t('admin.verifRejectDesc')}
        hint={t('admin.verifRejectHint')}
        confirm={t('admin.reject')}
        working={t('admin.rejecting')}
        onConfirm={async (m) => {
          await resolveVerification(v.id, false, m)
          listo(t('admin.verifRejected'))
        }}
      />,
    )

  return (
    <>
      <AdminHead title={t('admin.sec.verification')} desc={t('admin.verifDesc')} />
      <Tabs
        value={estado}
        onChange={setEstado}
        options={(['pendiente', 'verificado', 'rechazado'] as const).map((e) => [e, t('admin.withCount', { label: t(ESTADO[e][0]), n: cuenta(e) })])}
      />
      <div className="card tablewrap">
        <table className="t">
          <thead>
            <tr>
              <th>{t('admin.verifColUser')}</th>
              <th>{t('admin.verifColType')}</th>
              <th>{t('admin.verifColDoc')}</th>
              <th>{t('admin.verifColSent')}</th>
              <th>{t('admin.verifColStatus')}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {lista.map((v) => (
              <tr key={v.id}>
                <td>
                  <div className="row" style={{ gap: 10 }}>
                    <Ini name={v.nombre_mostrar} square={v.tipo_usuario === 'lote'} />
                    <div style={{ minWidth: 0 }}>
                      <button
                        className="linkbtn"
                        style={{ color: 'var(--ink)', fontWeight: 700 }}
                        onClick={() => navigate(v.tipo_usuario === 'lote' ? `/lotes/${v.user_id}` : `/vendedor/${v.user_id}`)}
                      >
                        {v.nombre_mostrar}
                      </button>
                      <div className="muted" style={{ fontSize: 12 }}>
                        {v.correo}
                      </div>
                    </div>
                  </div>
                </td>
                <td>
                  <span className={`badge ${v.tipo_usuario === 'lote' ? 'b-info' : 'b-neu'}`}>{t(v.tipo_usuario === 'lote' ? 'admin.lot' : 'admin.individual')}</span>
                </td>
                <td>
                  {documento(v.tipo_documento)}
                  {/* La CURP no tiene archivo: se muestra para compararla con el nombre del usuario. */}
                  {v.curp && (
                    <div style={{ fontFamily: 'monospace', fontSize: 13, marginTop: 2, userSelect: 'all' }}>
                      {v.curp}
                    </div>
                  )}
                </td>
                <td>{haceCuanto(v.fecha_envio)}</td>
                <td style={{ whiteSpace: 'normal', minWidth: 160 }}>
                  <span className={`badge ${ESTADO[v.estado][1]}`}>{t(ESTADO[v.estado][0])}</span>
                  {v.motivo_rechazo && (
                    <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
                      {t('admin.reason', { m: v.motivo_rechazo })}
                    </div>
                  )}
                  {v.revisado_por && (
                    <div className="mid" style={{ fontSize: 12, marginTop: 2 }}>
                      {t('admin.verifReviewedBy', { nombre: v.revisado_por })}
                    </div>
                  )}
                </td>
                <td>
                  <div className="row" style={{ gap: 8, justifyContent: 'flex-end' }}>
                    {v.archivo_path && (
                      <button className="btn btn-ghost btn-sm" onClick={() => void abrir(v)}>
                        <Icon name="doc" size={14} />
                        {t('admin.verifOpen')}
                      </button>
                    )}
                    {/* CURP: se revisa en una ventana con la guía para comparar en gob.mx. */}
                    {v.estado === 'pendiente' && v.tipo_documento === 'curp' && (
                      <button
                        className="btn btn-primary btn-sm"
                        disabled={busy === v.id}
                        onClick={() => openModal(<RevisarCurpModal v={v} onAprobar={() => aprobar(v)} onRechazar={() => rechazar(v)} />)}
                      >
                        {t('admin.curpReview')}
                      </button>
                    )}
                    {/* Constancia (y cualquier otro documento con archivo): ventana con la guía de qué revisar. */}
                    {v.estado === 'pendiente' && v.tipo_documento !== 'curp' && (
                      <button
                        className="btn btn-primary btn-sm"
                        disabled={busy === v.id}
                        onClick={() => openModal(<RevisarConstanciaModal v={v} onVer={() => void abrir(v)} onAprobar={() => aprobar(v)} onRechazar={() => rechazar(v)} />)}
                      >
                        {t('admin.csfReview')}
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {lista.length === 0 && (
              <tr>
                <td colSpan={6} className="muted" style={{ textAlign: 'center', padding: 30 }}>
                  {t('admin.verifEmpty')}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  )
}
