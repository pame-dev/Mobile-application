package com.pame.karsy.core.util

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Excel (.xlsx) sin librerías: un .xlsx es un ZIP con archivos XML. Aquí se arman las
 * hojas con estilos (encabezados de color, moneda, %, fechas, filtros y fila fija).
 * Genera el mismo archivo que la web (web/src/lib/xlsx.ts).
 */
object Xlsx {

    /** Índices de los estilos de styles.xml (mismo orden que en la web). */
    object Estilo {
        const val NORMAL = 0
        const val TITULO = 1
        const val SUBTITULO = 2
        const val ENCABEZADO = 3
        const val TEXTO = 4
        const val ENTERO = 5
        const val DINERO = 6
        const val PCT = 7
        const val FECHA = 8
        const val ETIQUETA = 9
        const val SECCION = 10
        const val NEGRITA = 11
        const val DECIMAL = 12
    }

    /** Una celda: texto, número o vacía, con su estilo. */
    data class Celda(val v: Any?, val s: Int)

    data class Hoja(
        val nombre: String,
        /** Ancho de cada columna (en caracteres). */
        val anchos: List<Int>,
        val filas: List<List<Celda?>>,
        /** Filas fijas arriba (no se mueven al bajar). */
        val fijas: Int = 0,
        /** Rango con filtro, p. ej. "A1:K20". */
        val filtro: String? = null,
        /** Rangos combinados, p. ej. "A1:C1". */
        val unir: List<String> = emptyList(),
    )

    /** Fecha ISO (AAAA-MM-DD…) → número de fecha de Excel (días desde 1899-12-30); null si no es válida. */
    fun fechaExcel(iso: String?): Long? {
        val m = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(iso ?: "") ?: return null
        val (a, mes, d) = m.destructured.toList().map { it.toInt() }
        if (mes !in 1..12 || d !in 1..diasDelMes(a, mes)) return null
        return diasDesdeEpoch(a, mes, d) + 25569
    }

    private fun diasDelMes(a: Int, m: Int) = when (m) {
        2 -> if (a % 4 == 0 && (a % 100 != 0 || a % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    /** Días desde 1970-01-01 (sin java.time, que pide Android 8; la app soporta desde el 7). */
    private fun diasDesdeEpoch(anio: Int, mes: Int, dia: Int): Long {
        val y = (if (mes <= 2) anio - 1 else anio).toLong()
        val era = Math.floorDiv(y, 400L)
        val yoe = y - era * 400
        val doy = (153 * (if (mes > 2) mes - 3 else mes + 9) + 2) / 5 + dia - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    /** Letra(s) de columna: 0 → A, 26 → AA. */
    fun columna(i: Int): String {
        var n = i + 1
        val sb = StringBuilder()
        while (n > 0) {
            sb.insert(0, ('A' + (n - 1) % 26))
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun esc(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        // Caracteres de control que XML no permite.
        .filter { it == '\t' || it == '\n' || it == '\r' || it >= ' ' }

    private fun celdaXml(c: Celda?, ref: String): String {
        if (c == null) return ""
        val v = c.v
        return when {
            v == null || v == "" -> """<c r="$ref" s="${c.s}"/>"""
            v is Number -> {
                val d = v.toDouble()
                if (d.isNaN() || d.isInfinite()) """<c r="$ref" s="${c.s}"/>"""
                else """<c r="$ref" s="${c.s}"><v>${if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()}</v></c>"""
            }
            else -> """<c r="$ref" s="${c.s}" t="inlineStr"><is><t xml:space="preserve">${esc(v.toString())}</t></is></c>"""
        }
    }

    private fun hojaXml(h: Hoja): String {
        val vista = if (h.fijas > 0)
            """<sheetViews><sheetView workbookViewId="0" showGridLines="0"><pane ySplit="${h.fijas}" topLeftCell="A${h.fijas + 1}" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>"""
        else """<sheetViews><sheetView workbookViewId="0" showGridLines="0"/></sheetViews>"""
        val cols = h.anchos.mapIndexed { i, w -> """<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""" }.joinToString("", "<cols>", "</cols>")
        val filas = h.filas.mapIndexed { r, f ->
            f.mapIndexed { i, c -> celdaXml(c, "${columna(i)}${r + 1}") }.joinToString("", """<row r="${r + 1}">""", "</row>")
        }.joinToString("")
        val filtro = h.filtro?.let { """<autoFilter ref="$it"/>""" } ?: ""
        val unir = if (h.unir.isEmpty()) "" else h.unir.joinToString("", """<mergeCells count="${h.unir.size}">""", "</mergeCells>") { """<mergeCell ref="$it"/>""" }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">$vista$cols<sheetData>$filas</sheetData>$filtro$unir</worksheet>"""
    }

    private val ESTILOS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<numFmts count="3"><numFmt numFmtId="164" formatCode="&quot;${'$'}&quot;#,##0"/><numFmt numFmtId="165" formatCode="yyyy-mm-dd"/><numFmt numFmtId="166" formatCode="#,##0.0"/></numFmts>
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
</styleSheet>"""

    /** Archivos XML del libro (nombre dentro del ZIP → contenido). */
    fun archivos(hojas: List<Hoja>): Map<String, String> {
        val mapa = linkedMapOf(
            "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
                """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/>""" +
                """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
                """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
                hojas.indices.joinToString("") { """<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" } +
                "</Types>",
            "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
                """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""" +
                hojas.mapIndexed { i, h -> """<sheet name="${esc(h.nombre.take(31))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""" }.joinToString("") +
                "</sheets></workbook>",
            "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
                hojas.indices.joinToString("") { """<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>""" } +
                """<Relationship Id="rId${hojas.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>""",
            "xl/styles.xml" to ESTILOS,
        )
        hojas.forEachIndexed { i, h -> mapa["xl/worksheets/sheet${i + 1}.xml"] = hojaXml(h) }
        return mapa
    }

    /** Escribe el libro .xlsx con [hojas] en [salida]. */
    fun escribir(hojas: List<Hoja>, salida: OutputStream) {
        ZipOutputStream(salida).use { zip ->
            archivos(hojas).forEach { (nombre, texto) ->
                zip.putNextEntry(ZipEntry(nombre))
                zip.write(texto.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }
}
