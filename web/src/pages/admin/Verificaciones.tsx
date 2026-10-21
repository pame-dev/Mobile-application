import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Icon } from '../../components/Icon'
import { useUi } from '../../components/Ui'
import { constanciaUrl, resolveVerification, type VerificacionAdmin } from '../../data/admin'
import { mensajeUsuario } from '../../data/auth'
import { useI18n } from '../../i18n/I18nContext'
import type { Clave } from '../../i18n/es'
import { haceCuanto } from '../../lib/format'
import { AdminHead, Ini, ReasonModal, Tabs } from './AdminParts'

type Estado = VerificacionAdmin['estado']

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

  const aprobar = async (v: VerificacionAdmin) => {
    setBusy(v.id)
    try {
      await resolveVerification(v.id, true, null)
      listo(t('admin.verifApproved'))
    } catch (e) {
      toast(mensajeUsuario(e))
    }
    setBusy(null)
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
                <td>{documento(v.tipo_documento)}</td>
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
                    <button className="btn btn-ghost btn-sm" onClick={() => void abrir(v)}>
                      <Icon name="doc" size={14} />
                      {t('admin.verifOpen')}
                    </button>
                    {v.estado === 'pendiente' && (
                      <>
                        <button className="btn btn-ghost btn-sm" disabled={busy === v.id} onClick={() => rechazar(v)}>
                          {t('admin.reject')}
                        </button>
                        <button className="btn btn-primary btn-sm" disabled={busy === v.id} onClick={() => void aprobar(v)}>
                          {t('admin.approve')}
                        </button>
                      </>
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
