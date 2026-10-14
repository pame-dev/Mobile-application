import { useI18n } from '../i18n/I18nContext'
import { Icon } from './Icon'

/** Switch de idioma ES / EN (barra superior y pie de página); la cápsula blanca se desliza al idioma elegido. */
export function LangSwitch() {
  const { t, lang, setLang } = useI18n()
  return (
    <div className="langsw" role="group" aria-label={t('footer.language')}>
      <Icon name="globe" size={15} />
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
