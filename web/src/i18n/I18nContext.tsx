import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { EN } from './en'
import { ES, type Clave } from './es'

/**
 * Idioma de la web (español o inglés), como Idioma.kt de la app.
 * Se recuerda en este navegador; si nunca se eligió, se sigue el idioma del navegador
 * (inglés si está en inglés; si no, español).
 */
export type Lang = 'es' | 'en'

const KEY = 'karsy-lang'
const DICTS: Record<Lang, Record<Clave, string>> = { es: ES, en: EN }

function idiomaInicial(): Lang {
  try {
    const guardado = localStorage.getItem(KEY)
    if (guardado === 'es' || guardado === 'en') return guardado
  } catch {
    /* sin almacenamiento: se usa el del navegador */
  }
  return navigator.language?.toLowerCase().startsWith('en') ? 'en' : 'es'
}

/** Texto traducido; {nombre} en el texto se reemplaza con vars.nombre. */
export type T = (clave: Clave, vars?: Record<string, string | number>) => string

interface I18nValue {
  lang: Lang
  setLang: (l: Lang) => void
  t: T
}

const I18nContext = createContext<I18nValue | null>(null)

/** Idioma en uso, para textos que se arman fuera de React (data/, lib/). */
let langActual: Lang = idiomaInicial()

function traducirEn(lang: Lang, clave: Clave, vars?: Record<string, string | number>): string {
  const texto = DICTS[lang][clave] ?? ES[clave]
  return vars ? texto.replace(/\{(\w+)\}/g, (m, k: string) => (k in vars ? String(vars[k]) : m)) : texto
}

/** Idioma en uso, para código fuera de componentes. */
export const idiomaActual = (): Lang => langActual

/** Texto traducido para usar fuera de componentes (en componentes usa useI18n().t). */
export const traducir: T = (clave, vars) => traducirEn(langActual, clave, vars)

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(langActual)

  useEffect(() => {
    document.documentElement.lang = lang
  }, [lang])

  const setLang = useCallback((l: Lang) => {
    langActual = l
    setLangState(l)
    try {
      localStorage.setItem(KEY, l)
    } catch {
      /* sin almacenamiento: solo dura esta visita */
    }
  }, [])

  const t = useCallback<T>((clave, vars) => traducirEn(lang, clave, vars), [lang])

  const value = useMemo(() => ({ lang, setLang, t }), [lang, setLang, t])
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n(): I18nValue {
  const ctx = useContext(I18nContext)
  if (!ctx) throw new Error('useI18n debe usarse dentro de <I18nProvider>')
  return ctx
}
