import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useI18n } from '../i18n/I18nContext'
import { textoAviso } from '../lib/avisos'
import { escucharCambios, EVENTO_CAMBIOS, type Tabla } from '../lib/tiempoReal'
import { useSession } from '../session/SessionContext'
import { useUi } from './Ui'

const PERMISO_PREGUNTADO = 'karsy-avisos-navegador-preguntado'

const soportaAvisos = () => typeof window !== 'undefined' && 'Notification' in window

/**
 * Cambios en vivo y avisos de la cuenta (montado en Layout). Con cada notificación nueva
 * muestra un aviso en la página; si la pestaña está en segundo plano, una notificación
 * del navegador (si el usuario dio permiso). El permiso se pide una vez con un botón,
 * porque el navegador solo lo pregunta tras un clic.
 */
export function TiempoReal() {
  const { account, reload } = useSession()
  const { toast } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const uid = account?.id ?? null

  useEffect(() => {
    return escucharCambios(uid, (a) => {
      const texto = textoAviso(t, a.tipo, a.titulo_vehiculo?.trim() || t('notif.yourVehicle'))
      const abrir = () => navigate(a.id_publicacion ? `/vehiculo/${a.id_publicacion}` : '/notificaciones')
      window.dispatchEvent(new Event('karsy:notificaciones'))
      if (document.visibilityState === 'visible') {
        toast(texto, { label: t('live.view'), onClick: abrir })
      } else if (soportaAvisos() && Notification.permission === 'granted') {
        const n = new Notification('Karsy', { body: texto, icon: '/logo.png', tag: `karsy-${a.id_notificacion}` })
        n.onclick = () => {
          window.focus()
          abrir()
          n.close()
        }
      }
    })
    // t y navigate cambian de identidad; el canal solo depende de la cuenta.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [uid])

  // Si cambia la propia cuenta (p. ej. el admin la suspende desde la app), se vuelve a leer.
  useEffect(() => {
    if (!uid) return
    const onCambios = (e: Event) => {
      if ((e as CustomEvent<Tabla[]>).detail.includes('cuentas')) void reload()
    }
    window.addEventListener(EVENTO_CAMBIOS, onCambios)
    return () => window.removeEventListener(EVENTO_CAMBIOS, onCambios)
  }, [uid, reload])

  // Una sola vez por navegador: ofrecer los avisos del navegador.
  useEffect(() => {
    if (!uid || !soportaAvisos() || Notification.permission !== 'default') return
    try {
      if (localStorage.getItem(PERMISO_PREGUNTADO)) return
      localStorage.setItem(PERMISO_PREGUNTADO, '1')
    } catch {
      return
    }
    const id = setTimeout(() => {
      toast(t('live.askPermission'), { label: t('live.enable'), onClick: () => void Notification.requestPermission() })
    }, 1500)
    return () => clearTimeout(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [uid])

  return null
}
