import { useState, type CSSProperties, type ReactNode } from 'react'

/** Silueta del diseño para cuando una publicación no tiene foto (o no carga). */
function CarIllustration() {
  return (
    <svg className="car" viewBox="0 0 400 240" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <defs>
        <linearGradient id="phbg" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#dfe7ee" />
          <stop offset="1" stopColor="#b9c8d6" />
        </linearGradient>
      </defs>
      <rect width="400" height="240" fill="url(#phbg)" />
      <ellipse cx="204" cy="184" rx="176" ry="12" fill="#000" opacity=".18" />
      <path
        d="M38,168 C36,148 52,138 86,132 L138,102 C152,93 172,88 198,88 L246,88 C272,88 292,99 308,114 L344,128 C362,133 370,144 368,160 L366,170 C366,174 362,176 358,176 L46,176 C41,176 38,173 38,168Z"
        fill="#8E979F"
      />
      <path d="M150,108 L176,96 L224,96 L224,126 L128,126 Z M234,96 L250,96 C270,98 286,108 298,126 L234,126Z" fill="#1d2a36" opacity=".85" />
      {[112, 300].map((x) => (
        <g key={x}>
          <circle cx={x} cy="176" r="26" fill="#1a1f25" />
          <circle cx={x} cy="176" r="14" fill="#9aa6b2" />
        </g>
      ))}
    </svg>
  )
}

/** Foto de un vehículo; si no hay o falla al cargar, muestra la ilustración. */
export function CarPhoto({
  src,
  alt = '',
  className = '',
  style,
  children,
  onClick,
}: {
  src: string | null | undefined
  alt?: string
  className?: string
  style?: CSSProperties
  children?: ReactNode
  onClick?: () => void
}) {
  const [failed, setFailed] = useState(false)
  return (
    <div className={`ph ${className}`} style={style} onClick={onClick}>
      {src && !failed ? <img src={src} alt={alt} loading="lazy" onError={() => setFailed(true)} /> : <CarIllustration />}
      {children}
    </div>
  )
}
