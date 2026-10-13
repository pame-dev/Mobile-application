/** Estados y municipios de México (mismo archivo que assets/mexico_estados_municipios.json de la app). */
export interface EstadoMunicipios {
  estado: string
  municipios: string[]
}

let cache: Promise<EstadoMunicipios[]> | null = null

export function estadosMunicipios(): Promise<EstadoMunicipios[]> {
  cache ??= fetch('/mexico_estados_municipios.json')
    .then((r) => {
      if (!r.ok) throw new Error(String(r.status))
      return r.json() as Promise<EstadoMunicipios[]>
    })
    .catch((e) => {
      cache = null
      throw e
    })
  return cache
}

/**
 * Comprime una imagen a JPEG con el lado mayor de [maxLado] px (Imagenes.jpegBytes de la app).
 * Devuelve también el lado menor original, para validar el tamaño mínimo.
 */
export async function jpegDe(file: Blob, maxLado = 800, calidad = 0.85): Promise<{ blob: Blob; ladoMenor: number }> {
  const url = URL.createObjectURL(file)
  try {
    const img = await new Promise<HTMLImageElement>((ok, fail) => {
      const i = new Image()
      i.onload = () => ok(i)
      i.onerror = fail
      i.src = url
    })
    const { naturalWidth: w, naturalHeight: h } = img
    const escala = Math.min(1, maxLado / Math.max(w, h))
    const canvas = document.createElement('canvas')
    canvas.width = Math.round(w * escala)
    canvas.height = Math.round(h * escala)
    const ctx = canvas.getContext('2d')!
    ctx.fillStyle = '#fff' // los PNG transparentes quedan con fondo blanco en JPEG
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
    const blob = await new Promise<Blob>((ok, fail) => canvas.toBlob((b) => (b ? ok(b) : fail(new Error('toBlob'))), 'image/jpeg', calidad))
    return { blob, ladoMenor: Math.min(w, h) }
  } finally {
    URL.revokeObjectURL(url)
  }
}
