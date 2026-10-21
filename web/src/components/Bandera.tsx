import { useId } from 'react'
import type { Lang } from '../i18n/I18nContext'

/**
 * Bandera del idioma: España (español) y Reino Unido (inglés), igual que la app.
 * Se dibuja en SVG porque Windows no muestra las banderas emoji (salen solo letras).
 */
export function Bandera({ lang, ancho = 20 }: { lang: Lang; ancho?: number }) {
  // Id único por bandera: puede haber varias en la página (barra, menú, Configuración).
  const id = useId()
  const alto = Math.round((ancho * 2) / 3)
  const estilo = { width: ancho, height: alto, borderRadius: 3, flex: 'none', display: 'block' }
  if (lang === 'es') {
    return (
      <svg viewBox="0 0 3 2" style={estilo} aria-hidden="true" className="bandera">
        <rect width="3" height="2" fill="#C60B1E" />
        <rect y="0.5" width="3" height="1" fill="#FFC400" />
      </svg>
    )
  }
  return (
    <svg viewBox="0 0 60 30" preserveAspectRatio="xMidYMid slice" style={estilo} aria-hidden="true" className="bandera">
      <clipPath id={`${id}t`}>
        <path d="M30,15 h30 v15 z v15 h-30 z h-30 v-15 z v-15 h30 z" />
      </clipPath>
      <rect width="60" height="30" fill="#012169" />
      <path d="M0,0 L60,30 M60,0 L0,30" stroke="#fff" strokeWidth="6" />
      <path d="M0,0 L60,30 M60,0 L0,30" clipPath={`url(#${id}t)`} stroke="#C8102E" strokeWidth="4" />
      <path d="M30,0 v30 M0,15 h60" stroke="#fff" strokeWidth="10" />
      <path d="M30,0 v30 M0,15 h60" stroke="#C8102E" strokeWidth="6" />
    </svg>
  )
}
