/**
 * Huella visual de una foto (dHash de 64 bits): sirve para reconocer la misma foto aunque se
 * haya vuelto a guardar, cambiado de tamaño o tapado las placas. Igual que
 * core/util/HuellaVisual.kt en la app (mismo cálculo, para comparar fotos de web y app).
 *
 * Cómo se calcula: la foto se dibuja a 576×512, se promedia en bloques de 8×8 a ANCHO×ALTO en
 * grises, otra vez en bloques de 8×8 (queda de 9×8) y cada bit dice si un cuadro es más claro
 * que el de su derecha. Los promedios por bloques hacen que web y app den la misma huella
 * aunque cada una reduzca la imagen con su propio filtro.
 */

export const ANCHO = 72
export const ALTO = 64
/** Bits distintos con los que dos huellas se consideran la misma foto (de 64). */
export const MAX_DISTANCIA = 4

/** Huella (16 caracteres hex) a partir de ANCHO×ALTO valores de gris (0–255), fila por fila. */
export function huellaDeGrises(grises: ArrayLike<number>): string {
  const celdas: number[][] = []
  for (let y = 0; y < 8; y++) {
    const fila: number[] = []
    for (let x = 0; x < 9; x++) {
      let suma = 0
      for (let j = 0; j < 8; j++) for (let i = 0; i < 8; i++) suma += grises[(y * 8 + j) * ANCHO + x * 8 + i]
      fila.push(suma / 64)
    }
    celdas.push(fila)
  }
  let hex = ''
  let nibble = 0
  let bits = 0
  for (let y = 0; y < 8; y++) {
    for (let x = 0; x < 8; x++) {
      nibble = (nibble << 1) | (celdas[y][x] > celdas[y][x + 1] ? 1 : 0)
      if (++bits % 4 === 0) {
        hex += nibble.toString(16)
        nibble = 0
      }
    }
  }
  return hex
}

/** Cuántos bits distintos hay entre dos huellas (0 = idénticas). */
export function distancia(a: string, b: string): number {
  let d = 0
  for (let i = 0; i < 16; i++) {
    let x = parseInt(a[i], 16) ^ parseInt(b[i], 16)
    while (x) {
      d += x & 1
      x >>= 1
    }
  }
  return d
}

export const mismaFoto = (a: string, b: string) => distancia(a, b) <= MAX_DISTANCIA

/** Huella visual de un archivo o URL de imagen; null si no se pudo leer. */
export async function huellaVisual(src: Blob | string): Promise<string | null> {
  try {
    const blob = typeof src === 'string' ? await (await fetch(src)).blob() : src
    // createImageBitmap respeta la rotación de las fotos del celular (EXIF).
    const img = await createImageBitmap(blob)
    const canvas = document.createElement('canvas')
    canvas.width = ANCHO * 8
    canvas.height = ALTO * 8
    const ctx = canvas.getContext('2d', { willReadFrequently: true })
    if (!ctx) return null
    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
    img.close()
    const { data } = ctx.getImageData(0, 0, canvas.width, canvas.height)
    return huellaDeGrises(grisesPorBloques(data, canvas.width))
  } catch {
    return null
  }
}

/** RGBA de (ANCHO·8)×(ALTO·8) → ANCHO×ALTO grises, promediando bloques de 8×8. */
export function grisesPorBloques(rgba: ArrayLike<number>, ancho = ANCHO * 8): number[] {
  const grises = new Array<number>(ANCHO * ALTO).fill(0)
  for (let y = 0; y < ALTO * 8; y++) {
    for (let x = 0; x < ancho; x++) {
      const p = (y * ancho + x) * 4
      grises[Math.floor(y / 8) * ANCHO + Math.floor(x / 8)] += (0.299 * rgba[p] + 0.587 * rgba[p + 1] + 0.114 * rgba[p + 2]) / 64
    }
  }
  return grises
}
