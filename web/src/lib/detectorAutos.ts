import type { MobileNet } from '@tensorflow-models/mobilenet'

/**
 * ¿La foto parece de un auto? Lo revisa MobileNet (TensorFlow.js) en el navegador, sin
 * costo. Se usa en las 4 fotos de ángulos al publicar; el tablero y las fotos adicionales
 * no se revisan. La app hace lo mismo con ML Kit (core/util/DetectorAutos.kt).
 */

/** Fotos obligatorias que deben mostrar el auto: frontal, trasera y laterales. */
export const ANGULOS_CON_AUTO = 4

/** Clases de ImageNet que son autos o partes exteriores de un auto. */
const CLASES_AUTO = new Set([
  'sports car', 'convertible', 'limousine', 'grille', 'beach wagon', 'racer', 'car wheel', 'cab', 'jeep',
  'minivan', 'pickup', 'Model T', 'police van', 'moving van', 'ambulance', 'tow truck', 'trailer truck',
  'garbage truck', 'minibus', 'go-kart', 'golf cart', 'car mirror', 'recreational vehicle', 'fire engine',
])

/**
 * Suma mínima de probabilidad de esas clases (entre las 5 más probables). Calibrado con
 * fotos reales de publicaciones: los autos dan 0.13–0.9 y otras cosas (mascotas, comida,
 * paisajes, carreteras sin un auto al frente) dan 0.
 */
const UMBRAL = 0.1

let modelo: Promise<MobileNet> | null = null

/** iPhone o iPad (el iPad se presenta como Mac, pero con pantalla táctil). */
export const esIOS = () =>
  /iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.userAgent.includes('Macintosh') && navigator.maxTouchPoints > 1)

/** Descarga el modelo (~14 MB, una vez) sin hacer más pesada la carga inicial de la página. */
export function precargarDetector(): Promise<MobileNet> {
  modelo ??= (async () => {
    const [tf, mobilenet] = await Promise.all([import('@tensorflow/tfjs'), import('@tensorflow-models/mobilenet')])
    // En iPhone y iPad con el procesador en lugar de WebGL: usa menos memoria (Safari cierra
    // la página si se pasa) y da el mismo resultado; tarda ~1–2 s por foto.
    if (esIOS()) await tf.setBackend('cpu')
    await tf.ready()
    return mobilenet.load({ version: 2, alpha: 1.0 })
  })().catch((e) => {
    modelo = null // sin internet o falló la descarga: se reintenta la próxima vez
    throw e
  })
  return modelo
}

function cargarImagen(url: string): Promise<HTMLImageElement> {
  return new Promise((ok, mal) => {
    const img = new Image()
    img.onload = () => ok(img)
    img.onerror = mal
    img.src = url
  })
}

/** Lado máximo con el que se analiza: MobileNet la reduce a 224 px de todos modos. */
const LADO_ANALISIS = 448

/**
 * Copia chica de la foto. Las fotos del iPhone (12 a 48 MP) pasan el límite de textura de
 * Safari (4096 px): ahí el análisis sale vacío sin error y toda foto "no parecía un auto".
 */
function reducir(img: HTMLImageElement): HTMLCanvasElement {
  const escala = Math.min(1, LADO_ANALISIS / Math.max(img.naturalWidth, img.naturalHeight))
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(img.naturalWidth * escala))
  canvas.height = Math.max(1, Math.round(img.naturalHeight * escala))
  canvas.getContext('2d')!.drawImage(img, 0, 0, canvas.width, canvas.height)
  return canvas
}

/** true / false; null si no se pudo analizar (en ese caso no se bloquea al usuario). */
export async function pareceAuto(url: string): Promise<boolean | null> {
  try {
    const [m, img] = await Promise.all([precargarDetector(), cargarImagen(url)])
    const chica = reducir(img)
    const preds = await m.classify(chica, 5).finally(() => {
      // Libera la memoria del canvas (Safari tiene un tope de memoria para canvas).
      chica.width = chica.height = 0
    })
    const puntaje = preds
      .filter((p) => CLASES_AUTO.has(p.className.split(',')[0].trim()))
      .reduce((suma, p) => suma + p.probability, 0)
    return puntaje >= UMBRAL
  } catch {
    return null
  }
}
