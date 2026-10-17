import { useEffect, useState } from 'react'
import { useI18n } from '../i18n/I18nContext'
import { traduccionGuardada, traducirAlIngles } from '../lib/traductor'

/**
 * Texto escrito por los usuarios (descripción de un perfil o lote, motivo de un reporte): en
 * inglés se muestra traducido automáticamente (lib/traductor.ts); mientras llega o si falla,
 * el original. Si ya estaba guardado se muestra desde el inicio. Igual que rememberTraduccion
 * en la app.
 */
export function Traducido({ texto }: { texto: string }) {
  const { lang } = useI18n()
  const [traducido, setTraducido] = useState<{ de: string; texto: string } | null>(() => {
    const tr = lang === 'en' ? traduccionGuardada(texto) : null
    return tr ? { de: texto, texto: tr } : null
  })
  useEffect(() => {
    if (lang !== 'en' || !texto.trim()) return
    let vivo = true
    void traducirAlIngles(texto).then((tr) => vivo && tr && setTraducido({ de: texto, texto: tr }))
    return () => {
      vivo = false
    }
  }, [lang, texto])
  return <>{lang === 'en' && traducido?.de === texto ? traducido.texto : texto}</>
}
