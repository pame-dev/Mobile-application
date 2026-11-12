import type Ocr from '@gutenye/ocr-browser'
import { diag, tam } from './diagnostico'
import { esIOS } from './detectorAutos'

/**
 * Pixela las placas que aparezcan en una foto antes de subirla (privacidad del vendedor).
 * PaddleOCR (onnxruntime-web, en el navegador y sin costo) encuentra el texto de la foto y
 * se tapa el que tiene forma de placa: 5 a 8 letras y números con al menos una letra y dos
 * números ("DQJ-0982", "SIY-150-B"), así no se tapan "MAZDA3", "Gol" ni el kilometraje.
 * La app hace lo mismo con ML Kit (core/util/TapaPlacas.kt).
 */

/** Mismo tamaño máximo con el que se suben las fotos (jpegDe). */
const MAX_LADO = 1600
/**
 * Lado máximo con el que PaddleOCR busca el texto (la librería no pone límite). Con 1600 px
 * su memoria de WebAssembly llegaba a ~370 MB y en iPhone Safari cerraba y recargaba la
 * página; con 960 px son ~150 MB y sigue encontrando placas chicas (~150 px de ancho en la
 * foto de 1600). Las cajas se pasan después a la foto completa para pixelar.
 */
const LADO_OCR = 960
const PLACA = /^(?=.*[A-Z])(?=(?:.*\d){2})[A-Z0-9]{5,8}$/
/** Modelos de PaddleOCR (~16 MB) del mismo paquete npm, servidos por jsDelivr. */
const MODELOS = 'https://cdn.jsdelivr.net/npm/@gutenye/ocr-models@1.4.2/assets/'

export const parecePlaca = (texto: string) => PLACA.test(texto.toUpperCase().replace(/[^A-Z0-9]/g, ''))

let ocr: Promise<Ocr> | null = null

/**
 * En iPhone y iPad no se tapan placas: Safari cerraba la página justo al terminar el análisis
 * (tardaba ~18 s por foto). Ahí el formulario pide no mostrar la placa (publicar.platesAvoid)
 * y el equipo revisa cada publicación antes de aprobarla.
 */
export const TAPA_PLACAS_DISPONIBLE = !esIOS()

/** Descarga el OCR una vez (solo al llegar al paso de fotos, no al abrir la página). */
export function precargarTapaPlacas(): Promise<Ocr> {
  if (!TAPA_PLACAS_DISPONIBLE) return Promise.reject(new Error('Tapado de placas no disponible en iOS'))
  ocr ??= (async () => {
    diag('modelo de placas: descargando')
    const [{ default: OcrBrowser }, ort] = await Promise.all([import('@gutenye/ocr-browser'), import('onnxruntime-web')])
    // El .wasm de onnxruntime se baja de jsDelivr (misma versión): Vite no lo sirve bien en desarrollo.
    ort.env.wasm.wasmPaths = `https://cdn.jsdelivr.net/npm/onnxruntime-web@${ort.env.versions.web}/dist/`
    const creado = await OcrBrowser.create({
      models: {
        detectionPath: MODELOS + 'ch_PP-OCRv4_det_infer.onnx',
        recognitionPath: MODELOS + 'ch_PP-OCRv4_rec_infer.onnx',
        dictionaryPath: MODELOS + 'ppocr_keys_v1.txt',
      },
    })
    diag('modelo de placas: listo')
    return creado
  })().catch((e) => {
    diag(`modelo de placas: ERROR ${e}`)
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

/** Las fotos se reducen de una en una (ver reducirFoto). */
let colaReducir: Promise<unknown> = Promise.resolve()

/**
 * Copia de la foto elegida, derecha y de máximo MAX_LADO por lado (el tamaño con el que se
 * sube). Todo lo demás (vista previa, huellas, placas, "¿parece auto?") usa esta copia: en
 * iPhone, abrir varias veces la original (12–48 MP, ~200 MB cada una) llenaba la memoria y
 * Safari recargaba la página (el formulario regresaba al primer paso). Se hacen de una en
 * una para no abrir dos originales a la vez. Si no se puede, se usa la original.
 */
export function reducirFoto(file: File): Promise<File> {
  const trabajo = colaReducir.then(async () => {
    try {
      diag(`reducir: abriendo original (${tam(file.size)}, ${file.type})`)
      const canvas = await aCanvas(file)
      diag(`reducir: copia de ${canvas.width}x${canvas.height} lista`)
      // Fondo blanco detrás de un PNG o WebP transparente (en JPEG quedaría negro), como jpegDe.
      const ctx = canvas.getContext('2d')!
      ctx.globalCompositeOperation = 'destination-over'
      ctx.fillStyle = '#fff'
      ctx.fillRect(0, 0, canvas.width, canvas.height)
      const blob = await new Promise<Blob | null>((ok) => canvas.toBlob(ok, 'image/jpeg', 0.9))
      canvas.width = canvas.height = 0 // libera la memoria del canvas (Safari tiene un tope)
      diag(`reducir: listo (${blob ? tam(blob.size) : 'sin blob'})`)
      return blob ? new File([blob], 'foto.jpg', { type: 'image/jpeg' }) : file
    } catch (e) {
      diag(`reducir: ERROR ${e}`)
      return file
    }
  })
  colaReducir = trabajo
  return trabajo
}

/**
 * JPEG de la foto con las placas pixeladas, o null si no encontró ninguna (o no se pudo
 * analizar: no se bloquea al usuario). Acepta un archivo elegido o la URL de una foto ya
 * publicada (al editar).
 */
export async function taparPlacas(origen: Blob | string): Promise<Blob | null> {
  if (!TAPA_PLACAS_DISPONIBLE) return null
  try {
    diag('placas: abriendo foto y esperando modelo')
    const [modelo, canvas] = await Promise.all([precargarTapaPlacas(), aCanvas(origen)])
    diag('placas: modelo y foto listos, analizando')
    const escala = Math.min(1, LADO_OCR / Math.max(canvas.width, canvas.height))
    const chico = document.createElement('canvas')
    chico.width = Math.round(canvas.width * escala)
    chico.height = Math.round(canvas.height * escala)
    chico.getContext('2d')!.drawImage(canvas, 0, 0, chico.width, chico.height)
    const datos = chico.getContext('2d')!.getImageData(0, 0, chico.width, chico.height)
    chico.width = chico.height = 0
    const res = await modelo.detect({ data: datos.data, width: datos.width, height: datos.height })
    diag(`placas: análisis terminado (${res.texts.length} textos)`)
    // Las cajas vienen en el tamaño con el que el modelo analizó la foto (la copia chica);
    // al dividir entre ese tamaño quedan en la foto completa.
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
