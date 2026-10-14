import type Ocr from '@gutenye/ocr-browser'

/**
 * Pixela las placas que aparezcan en una foto antes de subirla (privacidad del vendedor).
 * PaddleOCR (onnxruntime-web, en el navegador y sin costo) encuentra el texto de la foto y
 * se tapa el que tiene forma de placa: 5 a 8 letras y números con al menos una letra y dos
 * números ("DQJ-0982", "SIY-150-B"), así no se tapan "MAZDA3", "Gol" ni el kilometraje.
 * La app hace lo mismo con ML Kit (core/util/TapaPlacas.kt).
 */

/** Mismo tamaño máximo con el que se suben las fotos (jpegDe). */
const MAX_LADO = 1600
const PLACA = /^(?=.*[A-Z])(?=(?:.*\d){2})[A-Z0-9]{5,8}$/
/** Modelos de PaddleOCR (~16 MB) del mismo paquete npm, servidos por jsDelivr. */
const MODELOS = 'https://cdn.jsdelivr.net/npm/@gutenye/ocr-models@1.4.2/assets/'

export const parecePlaca = (texto: string) => PLACA.test(texto.toUpperCase().replace(/[^A-Z0-9]/g, ''))

let ocr: Promise<Ocr> | null = null

/** Descarga el OCR una vez (solo al llegar al paso de fotos, no al abrir la página). */
export function precargarTapaPlacas(): Promise<Ocr> {
  ocr ??= (async () => {
    const [{ default: OcrBrowser }, ort] = await Promise.all([import('@gutenye/ocr-browser'), import('onnxruntime-web')])
    // El .wasm de onnxruntime se baja de jsDelivr (misma versión): Vite no lo sirve bien en desarrollo.
    ort.env.wasm.wasmPaths = `https://cdn.jsdelivr.net/npm/onnxruntime-web@${ort.env.versions.web}/dist/`
    return OcrBrowser.create({
      models: {
        detectionPath: MODELOS + 'ch_PP-OCRv4_det_infer.onnx',
        recognitionPath: MODELOS + 'ch_PP-OCRv4_rec_infer.onnx',
        dictionaryPath: MODELOS + 'ppocr_keys_v1.txt',
      },
    })
  })().catch((e) => {
    ocr = null // sin internet o falló la descarga: se reintenta la próxima vez
    throw e
  })
  return ocr
}

/** Foto derecha (según su EXIF) en un canvas de máximo MAX_LADO por lado. */
async function aCanvas(origen: Blob | string): Promise<HTMLCanvasElement> {
  const blob = typeof origen === 'string' ? await (await fetch(origen)).blob() : origen
  const img = await createImageBitmap(blob, { imageOrientation: 'from-image' })
  const escala = Math.min(1, MAX_LADO / Math.max(img.width, img.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(img.width * escala)
  canvas.height = Math.round(img.height * escala)
  canvas.getContext('2d')!.drawImage(img, 0, 0, canvas.width, canvas.height)
  img.close()
  return canvas
}

/**
 * Pixelado grueso (unos 6 cuadros a lo ancho): ya no se puede leer. PaddleOCR ya da la caja
 * con holgura, así que solo se agranda un poco (en la app, ML Kit la da justa).
 */
function pixelar(canvas: HTMLCanvasElement, x0: number, y0: number, x1: number, y1: number) {
  const mx = (x1 - x0) * 0.05 + 2
  const my = (y1 - y0) * 0.1 + 2
  const x = Math.max(0, Math.floor(x0 - mx))
  const y = Math.max(0, Math.floor(y0 - my))
  const w = Math.min(canvas.width, Math.ceil(x1 + mx)) - x
  const h = Math.min(canvas.height, Math.ceil(y1 + my)) - y
  if (w < 2 || h < 2) return
  const cuadros = 6
  const chico = document.createElement('canvas')
  chico.width = cuadros
  chico.height = Math.max(1, Math.round((cuadros * h) / w))
  chico.getContext('2d')!.drawImage(canvas, x, y, w, h, 0, 0, chico.width, chico.height)
  const ctx = canvas.getContext('2d')!
  ctx.imageSmoothingEnabled = false
  ctx.drawImage(chico, 0, 0, chico.width, chico.height, x, y, w, h)
  ctx.imageSmoothingEnabled = true
}

/**
 * JPEG de la foto con las placas pixeladas, o null si no encontró ninguna (o no se pudo
 * analizar: no se bloquea al usuario). Acepta un archivo elegido o la URL de una foto ya
 * publicada (al editar).
 */
export async function taparPlacas(origen: Blob | string): Promise<Blob | null> {
  try {
    const [modelo, canvas] = await Promise.all([precargarTapaPlacas(), aCanvas(origen)])
    const datos = canvas.getContext('2d')!.getImageData(0, 0, canvas.width, canvas.height)
    const res = await modelo.detect({ data: datos.data, width: datos.width, height: datos.height })
    // Las cajas vienen en el tamaño con el que el modelo analizó la foto.
    const sx = canvas.width / res.resizedImageWidth
    const sy = canvas.height / res.resizedImageHeight
    const placas = res.texts.filter((t) => parecePlaca(t.text))
    if (!placas.length) return null
    for (const { box } of placas) {
      if (!box?.length) continue
      const xs = box.map((p) => p[0] * sx)
      const ys = box.map((p) => p[1] * sy)
      pixelar(canvas, Math.min(...xs), Math.min(...ys), Math.max(...xs), Math.max(...ys))
    }
    return await new Promise((ok) => canvas.toBlob(ok, 'image/jpeg', 0.9))
  } catch {
    return null
  }
}
