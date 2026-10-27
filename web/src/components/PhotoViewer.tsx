import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../i18n/I18nContext'
import { Icon } from './Icon'

/**
 * Visor de fotos a pantalla completa sobre la página (detalle de un vehículo).
 * Flechas del teclado o botones para cambiar de foto, deslizar en el celular, Escape o ✕ para cerrar.
 * [onClose] recibe la foto que se estaba viendo para dejarla como principal.
 */
export function PhotoViewer({ fotos, inicio, alt, onClose }: { fotos: string[]; inicio: number; alt: string; onClose: (i: number) => void }) {
  const { t } = useI18n()
  const [i, setI] = useState(inicio)
  const n = fotos.length
  const actual = useRef(i)
  actual.current = i
  const cerrar = useRef(onClose)
  cerrar.current = onClose
  const toque = useRef<number | null>(null)
  const ir = (d: number) => setI((x) => (x + d + n) % n)

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') cerrar.current(actual.current)
      else if (e.key === 'ArrowRight') setI((x) => (x + 1) % n)
      else if (e.key === 'ArrowLeft') setI((x) => (x - 1 + n) % n)
    }
    window.addEventListener('keydown', onKey)
    // La página de atrás no se desplaza mientras el visor está abierto.
    const antes = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = antes
    }
  }, [n])

  return (
    <div
      className="viewer"
      role="dialog"
      aria-modal="true"
      aria-label={t('vehiculo.viewer.label')}
      onClick={(e) => e.target === e.currentTarget && onClose(i)}
      onTouchStart={(e) => (toque.current = e.touches[0].clientX)}
      onTouchEnd={(e) => {
        if (toque.current == null || n < 2) return
        const dx = e.changedTouches[0].clientX - toque.current
        if (Math.abs(dx) > 50) ir(dx < 0 ? 1 : -1)
        toque.current = null
      }}
    >
      <img src={fotos[i]} alt={`${alt} · ${t('vehiculo.detail.photoCounter', { i: i + 1, n })}`} />
      <button className="iconbtn vclose" aria-label={t('common.close')} onClick={() => onClose(i)} autoFocus>
        <Icon name="x" size={20} />
      </button>
      {n > 1 && (
        <>
          <button className="vnav prev" aria-label={t('vehiculo.viewer.prev')} onClick={() => ir(-1)}>
            <Icon name="back" size={24} />
          </button>
          <button className="vnav next" aria-label={t('vehiculo.viewer.next')} onClick={() => ir(1)}>
            <Icon name="chev" size={24} />
          </button>
        </>
      )}
      <span className="vcount" aria-live="polite">
        {t('vehiculo.detail.photoCounter', { i: i + 1, n })}
      </span>
    </div>
  )
}
