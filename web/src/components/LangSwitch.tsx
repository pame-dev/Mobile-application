import { useI18n } from '../i18n/I18nContext'
import { Bandera } from './Bandera'

/**
 * Switch de idioma ES / EN (barra superior y menú); la cápsula blanca se desliza al idioma elegido.
 * A la izquierda, la bandera del idioma en uso (como en Configuración → Idioma).
 */
export function LangSwitch() {
  const { t, lang, setLang } = useI18n()
  return (
    <div className="langsw" role="group" aria-label={t('footer.language')}>
      <Bandera lang={lang} ancho={18} />
      <div className="langsw-opts" data-lang={lang}>
        <span className="thumb" aria-hidden="true" />
        {(['es', 'en'] as const).map((l) => (
          <button key={l} className={lang === l ? 'on' : ''} aria-pressed={lang === l} onClick={() => setLang(l)}>
            {l.toUpperCase()}
          </button>
        ))}
      </div>
    </div>
  )
}
