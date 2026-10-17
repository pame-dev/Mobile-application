import { useEffect, useState, type ReactNode } from 'react'
import { useI18n } from '../i18n/I18nContext'
import { traduccionGuardada, traducirAlIngles } from '../lib/traductor'

/**
 * Texto escrito por los usuarios o el admin (descripción de un auto en su tarjeta, de un
 * perfil o lote, motivos): en inglés se muestra traducido automáticamente (lib/traductor.ts);
 * mientras llega o si falla, el original. Si ya estaba guardado se muestra desde el inicio.
 * Con `corto` (tarjetas y listas) solo se traduce el inicio, para gastar poco del límite
 * diario. Con `children` recibe la traducción para armar otro texto. Igual que
 * rememberTraduccion en la app.
 */
export function Traducido({ texto, corto = false, children }: { texto: string; corto?: boolean; children?: (traducido: string) => ReactNode }) {
  const tr = useTraduccion(texto, corto)
  return <>{children ? children(tr) : tr}</>
}

/** Lo mismo que Traducido pero como texto, para meterlo en otro mensaje (p. ej. "Motivo: …"). */
export function useTraduccion(texto: string, corto = false): string {
  const { lang } = useI18n()
  const [traducido, setTraducido] = useState<{ de: string; texto: string } | null>(() => {
    const tr = lang === 'en' ? traduccionGuardada(texto, corto) : null
    return tr ? { de: texto, texto: tr } : null
  })
  useEffect(() => {
    if (lang !== 'en' || !texto.trim()) return
    let vivo = true
    void traducirAlIngles(texto, corto).then((tr) => vivo && tr && setTraducido({ de: texto, texto: tr }))
    return () => {
      vivo = false
    }
  }, [lang, texto, corto])
  return lang === 'en' && traducido?.de === texto ? traducido.texto : texto
}
