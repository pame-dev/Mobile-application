/**
 * Excel (.xlsx) sin librerías: un .xlsx es un ZIP con archivos XML. Aquí se arman las
 * hojas con estilos (encabezados de color, moneda, %, fechas, filtros y fila fija) y se
 * empaqueta en un ZIP sin compresión. La app genera el mismo archivo (Xlsx.kt).
 */

/** Índices de los estilos definidos en styles.xml (mismo orden que en la app). */
export const ESTILO = {
  normal: 0,
  titulo: 1,
  subtitulo: 2,
  encabezado: 3,
  texto: 4,
  entero: 5,
  dinero: 6,
  pct: 7,
  fecha: 8,
  etiqueta: 9,
  seccion: 10,
  negrita: 11,
  decimal: 12,
} as const

export type Celda = null | string | number | { v: string | number | null; s: number }

export interface Hoja {
  nombre: string
  /** Ancho de cada columna (en caracteres). */
  anchos: number[]
  filas: Celda[][]
  /** Filas fijas arriba (no se mueven al bajar). */
  fijas?: number
  /** Rango con filtro, p. ej. "A4:K20". */
  filtro?: string
  /** Rangos combinados, p. ej. ["A1:F1"]. */
  unir?: string[]
}

/** Fecha ISO → número de fecha de Excel (días desde 1899-12-30); null si no es válida. */
export function fechaExcel(iso: string | null | undefined): number | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso ?? '')
  if (!m) return null
  return (Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3])) - Date.UTC(1899, 11, 30)) / 86_400_000
}

/** Letra(s) de columna: 0 → A, 26 → AA. */
export function columna(i: number): string {
  let s = ''
  for (let n = i + 1; n > 0; n = Math.floor((n - 1) / 26)) s = String.fromCharCode(65 + ((n - 1) % 26)) + s
  return s
}

const esc = (s: string) =>
  s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    // Caracteres de control que XML no permite.
    // eslint-disable-next-line no-control-regex
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '')

function celdaXml(c: Celda, ref: string): string {
  if (c === null) return ''
  const { v, s } = typeof c === 'object' ? c : { v: c, s: typeof c === 'number' ? ESTILO.entero : ESTILO.texto }
  if (v === null || v === '') return `<c r="${ref}" s="${s}"/>`
  if (typeof v === 'number') return Number.isFinite(v) ? `<c r="${ref}" s="${s}"><v>${v}</v></c>` : `<c r="${ref}" s="${s}"/>`
  return `<c r="${ref}" s="${s}" t="inlineStr"><is><t xml:space="preserve">${esc(v)}</t></is></c>`
}

function hojaXml(h: Hoja): string {
  const vista = h.fijas
    ? `<sheetViews><sheetView workbookViewId="0" showGridLines="0"><pane ySplit="${h.fijas}" topLeftCell="A${h.fijas + 1}" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>`
    : `<sheetViews><sheetView workbookViewId="0" showGridLines="0"/></sheetViews>`
  const cols = `<cols>${h.anchos.map((w, i) => `<col min="${i + 1}" max="${i + 1}" width="${w}" customWidth="1"/>`).join('')}</cols>`
  const filas = h.filas
    .map((f, r) => `<row r="${r + 1}">${f.map((c, i) => celdaXml(c, `${columna(i)}${r + 1}`)).join('')}</row>`)
    .join('')
  const filtro = h.filtro ? `<autoFilter ref="${h.filtro}"/>` : ''
  const unir = h.unir?.length ? `<mergeCells count="${h.unir.length}">${h.unir.map((u) => `<mergeCell ref="${u}"/>`).join('')}</mergeCells>` : ''
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">${vista}${cols}<sheetData>${filas}</sheetData>${filtro}${unir}</worksheet>`
}

const ESTILOS = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="3"><numFmt numFmtId="164" formatCode="&quot;$&quot;#,##0"/><numFmt numFmtId="165" formatCode="yyyy-mm-dd"/><numFmt numFmtId="166" formatCode="#,##0.0"/></numFmts>
<fonts count="6">
<font><sz val="11"/><color rgb="FF0D2B45"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><color rgb="FF0D2B45"/><name val="Calibri"/></font>
<font><b/><sz val="18"/><color rgb="FF0D2B45"/><name val="Calibri"/></font>
<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
<font><sz val="10"/><color rgb="FF5B6B7A"/><name val="Calibri"/></font>
<font><b/><sz val="13"/><color rgb="FF2C9AA0"/><name val="Calibri"/></font>
</fonts>
<fills count="4">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FF0D2B45"/><bgColor indexed="64"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFF3F7FA"/><bgColor indexed="64"/></patternFill></fill>
</fills>
<borders count="2">
<border><left/><right/><top/><bottom/><diagonal/></border>
<border><left style="thin"><color rgb="FFD7DEE5"/></left><right style="thin"><color rgb="FFD7DEE5"/></right><top style="thin"><color rgb="FFD7DEE5"/></top><bottom style="thin"><color rgb="FFD7DEE5"/></bottom><diagonal/></border>
</borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="13">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="3" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
<xf numFmtId="3" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="9" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="1" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="5" fillId="0" borderId="0" xfId="0" applyFont="1"/>
<xf numFmtId="0" fontId="1" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
<xf numFmtId="166" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>`

function libroXml(hojas: Hoja[]): Record<string, string> {
  const archivos: Record<string, string> = {
    '[Content_Types].xml':
      `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">` +
      `<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/>` +
      `<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>` +
      `<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>` +
      hojas.map((_, i) => `<Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>`).join('') +
      `</Types>`,
    '_rels/.rels':
      `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">` +
      `<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>`,
    'xl/workbook.xml':
      `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>` +
      hojas.map((h, i) => `<sheet name="${esc(h.nombre.slice(0, 31))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>`).join('') +
      `</sheets></workbook>`,
    'xl/_rels/workbook.xml.rels':
      `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">` +
      hojas.map((_, i) => `<Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${i + 1}.xml"/>`).join('') +
      `<Relationship Id="rId${hojas.length + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>`,
    'xl/styles.xml': ESTILOS,
  }
  hojas.forEach((h, i) => (archivos[`xl/worksheets/sheet${i + 1}.xml`] = hojaXml(h)))
  return archivos
}

// ── ZIP sin compresión ─────────────────────────────────────────────────────

const TABLA_CRC = (() => {
  const t = new Uint32Array(256)
  for (let n = 0; n < 256; n++) {
    let c = n
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
    t[n] = c >>> 0
  }
  return t
})()

function crc32(datos: Uint8Array): number {
  let c = 0xffffffff
  for (let i = 0; i < datos.length; i++) c = TABLA_CRC[(c ^ datos[i]) & 0xff] ^ (c >>> 8)
  return (c ^ 0xffffffff) >>> 0
}

function zip(archivos: Record<string, string>): Uint8Array<ArrayBuffer> {
  const enc = new TextEncoder()
  const partes: Uint8Array[] = []
  const central: Uint8Array[] = []
  let offset = 0
  for (const [nombre, texto] of Object.entries(archivos)) {
    const n = enc.encode(nombre)
    const d = enc.encode(texto)
    const crc = crc32(d)
    const local = new DataView(new ArrayBuffer(30))
    local.setUint32(0, 0x04034b50, true)
    local.setUint16(4, 20, true)
    local.setUint16(6, 0x0800, true) // nombres en UTF-8
    local.setUint16(8, 0, true) // sin compresión
    local.setUint16(10, 0, true)
    local.setUint16(12, 0x21, true) // fecha fija (1980-01-01)
    local.setUint32(14, crc, true)
    local.setUint32(18, d.length, true)
    local.setUint32(22, d.length, true)
    local.setUint16(26, n.length, true)
    local.setUint16(28, 0, true)
    partes.push(new Uint8Array(local.buffer), n, d)
    const cd = new DataView(new ArrayBuffer(46))
    cd.setUint32(0, 0x02014b50, true)
    cd.setUint16(4, 20, true)
    cd.setUint16(6, 20, true)
    cd.setUint16(8, 0x0800, true)
    cd.setUint16(10, 0, true)
    cd.setUint16(12, 0, true)
    cd.setUint16(14, 0x21, true)
    cd.setUint32(16, crc, true)
    cd.setUint32(20, d.length, true)
    cd.setUint32(24, d.length, true)
    cd.setUint16(28, n.length, true)
    cd.setUint32(42, offset, true)
    central.push(new Uint8Array(cd.buffer), n)
    offset += 30 + n.length + d.length
  }
  const tamCentral = central.reduce((a, b) => a + b.length, 0)
  const fin = new DataView(new ArrayBuffer(22))
  fin.setUint32(0, 0x06054b50, true)
  const total = Object.keys(archivos).length
  fin.setUint16(8, total, true)
  fin.setUint16(10, total, true)
  fin.setUint32(12, tamCentral, true)
  fin.setUint32(16, offset, true)
  const todo = [...partes, ...central, new Uint8Array(fin.buffer)]
  const out = new Uint8Array(todo.reduce((a, b) => a + b.length, 0))
  let p = 0
  for (const b of todo) {
    out.set(b, p)
    p += b.length
  }
  return out
}

/** Libro de Excel (.xlsx) con [hojas], listo para descargar. */
export function xlsx(hojas: Hoja[]): Uint8Array<ArrayBuffer> {
  return zip(libroXml(hojas))
}
