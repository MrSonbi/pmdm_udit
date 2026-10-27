package com.example.filtro400.csv

import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Codec para leer ficheros CSV de reglas.
 */
object CsvCodec {

    private const val MAX_LINEA_CARACTERES = 1000
    private const val MAX_FILAS = 5000
    private const val MAX_BYTES = 1024L * 1024L

    fun leerLineas(
        inputStream: InputStream,
        charset: Charset = StandardCharsets.UTF_8
    ): List<FilaCsv> {
        val res = leerConResultado(inputStream, charset)
        return res.filas
    }

    fun leerConResultado(
        inputStream: InputStream,
        charset: Charset = StandardCharsets.UTF_8
    ): ResultadoParseo {
        val filasValidas = ArrayList<FilaCsv>()
        val errores = ArrayList<ResultadoLinea.Error>()
        val streamLimitado = contarBytesHastaLimite(inputStream)
        val lector = BufferedReader(InputStreamReader(streamLimitado, charset))
        var numeroLinea = 0
        var filasProcesadas = 0
        try {
            while (true) {
                val l = lector.readLine() ?: break
                numeroLinea++
                var lineaProcesar = l
                if (numeroLinea == 1 && lineaProcesar.isNotEmpty() && lineaProcesar[0] == '\uFEFF') {
                    lineaProcesar = lineaProcesar.substring(1)
                }
                if (lineaProcesar.isBlank()) {
                    continue
                }
                if (lineaProcesar.length > MAX_LINEA_CARACTERES) {
                    errores.add(ResultadoLinea.Error(numeroLinea, "línea demasiado larga"))
                    continue
                }
                filasProcesadas++
                if (filasProcesadas > MAX_FILAS) {
                    throw LimiteFilasExcedidoException()
                }
                val resLinea = parsearSeguro(lineaProcesar, numeroLinea)
                when (resLinea) {
                    is ResultadoLinea.Ok -> filasValidas.add(resLinea.fila)
                    is ResultadoLinea.Error -> errores.add(resLinea)
                }
            }
        } catch (e: Throwable) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            if (e is LimiteTamanoExcedidoException) throw e
            if (e is LimiteFilasExcedidoException) throw e
            throw e
        }
        return ResultadoParseo(filasValidas, errores.sortedBy { it.numeroLineaOriginal })
    }

    private fun parsearSeguro(linea: String, numeroLinea: Int): ResultadoLinea {
        try {
            val fila = parsearLinea(linea, numeroLinea)
            return if (fila == null) {
                ResultadoLinea.Error(numeroLinea, "fila vacía")
            } else {
                ResultadoLinea.Ok(numeroLinea, fila)
            }
        } catch (e: FilaIncorrectaException) {
            return ResultadoLinea.Error(numeroLinea, "fila con menos columnas")
        } catch (e: TipoOAccionDesconocidoException) {
            return ResultadoLinea.Error(numeroLinea, "tipo o acción desconocidos")
        } catch (e: PrioridadInvalidaException) {
            return ResultadoLinea.Error(numeroLinea, "prioridad no numérica")
        } catch (e: PrioridadFueraDeRangoException) {
            return ResultadoLinea.Error(numeroLinea, "prioridad fuera de rango")
        } catch (e: Exception) {
            return ResultadoLinea.Error(numeroLinea, "inválido")
        }
    }

    private fun contarBytesHastaLimite(inputStream: InputStream): InputStream {
        return LimitadorBytesInputStream(inputStream, MAX_BYTES)
    }

    private fun parsearLinea(linea: String, numeroLinea: Int): FilaCsv? {
        if (linea.isBlank()) {
            return null
        }
        val delimitador = detectarDelimitador(linea)
        val campos = dividirCampos(linea, delimitador)
        if (campos.size < 3) {
            throw FilaIncorrectaException(numeroLinea, "Se esperan al menos 3 columnas")
        }
        val patron = campos[0].trim()
        val etiqueta = if (campos.size > 1) campos[1].trim().ifEmpty { null } else null
        val tipoStr = if (campos.size > 2) campos[2].trim() else ""
        val accionStr = if (campos.size > 3) campos[3].trim() else ""
        val prioridadStr = if (campos.size > 4) campos[4].trim() else ""
        val activaStr = if (campos.size > 5) campos[5].trim().lowercase() else ""

        val tipo = try {
            TipoRegla.valueOf(tipoStr.uppercase())
        } catch (e: Exception) {
            throw TipoOAccionDesconocidoException(numeroLinea)
        }
        val accion = try {
            Accion.valueOf(accionStr.uppercase())
        } catch (e: Exception) {
            throw TipoOAccionDesconocidoException(numeroLinea)
        }

        val prioridad = if (prioridadStr.isEmpty()) {
            null
        } else {
            val p = prioridadStr.toIntOrNull() ?: throw PrioridadInvalidaException(numeroLinea)
            if (p < 0 || p > 100) {
                throw PrioridadFueraDeRangoException(numeroLinea)
            }
            p
        }

        val activa = when (activaStr) {
            "1", "true", "si", "sí" -> true
            "0", "false", "no" -> false
            "" -> null
            else -> null
        }

        return FilaCsv(
            numeroLineaOriginal = numeroLinea,
            patron = patron,
            etiqueta = etiqueta,
            tipo = tipo,
            accion = accion,
            prioridad = prioridad,
            activa = activa
        )
    }

    fun parsear(linea: String, numeroLinea: Int): FilaCsv? = parsearLinea(linea, numeroLinea)

    private fun detectarDelimitador(linea: String): Char {
        val countComa = contarFueraDeComillas(linea, ',')
        val countPuntoComa = contarFueraDeComillas(linea, ';')
        return if (countPuntoComa > countComa) ';' else ','
    }

    private fun contarFueraDeComillas(linea: String, delimitador: Char): Int {
        var count = 0
        var dentroComillas = false
        var i = 0
        while (i < linea.length) {
            val c = linea[i]
            if (c == '"') {
                if (i + 1 < linea.length && linea[i + 1] == '"') {
                    i += 2
                    continue
                }
                dentroComillas = !dentroComillas
                i++
                continue
            }
            if (c == delimitador && !dentroComillas) {
                count++
            }
            i++
        }
        return count
    }

    private fun dividirCampos(linea: String, delimitador: Char): List<String> {
        val campos = ArrayList<String>()
        val actual = StringBuilder()
        var dentroComillas = false
        var i = 0
        while (i < linea.length) {
            val c = linea[i]
            if (c == '"') {
                if (dentroComillas && i + 1 < linea.length && linea[i + 1] == '"') {
                    actual.append('"')
                    i += 2
                    continue
                }
                dentroComillas = !dentroComillas
                i++
                continue
            }
            if (c == delimitador && !dentroComillas) {
                campos.add(actual.toString())
                actual.clear()
                i++
                continue
            }
            actual.append(c)
            i++
        }
        campos.add(actual.toString())
        return campos
    }
}

data class FilaCsv(
    val numeroLineaOriginal: Int,
    val patron: String,
    val etiqueta: String?,
    val tipo: TipoRegla,
    val accion: Accion,
    val prioridad: Int?,
    val activa: Boolean? = null
)

class LineaDemasiadoLargaException(val numeroLinea: Int) : Exception("línea demasiado larga")
class FilaIncorrectaException(val numeroLinea: Int, mensaje: String) : Exception(mensaje)
class TipoOAccionDesconocidoException(val numeroLinea: Int) : Exception("tipo o acción desconocidos")
class PrioridadInvalidaException(val numeroLinea: Int) : Exception("prioridad no numérica")
class PrioridadFueraDeRangoException(val numeroLinea: Int) : Exception("prioridad fuera de rango")
class LimiteTamanoExcedidoException : Exception("límite de 1 MB excedido")
class LimiteFilasExcedidoException : Exception("límite de 5000 filas excedido")

private class LimitadorBytesInputStream(
    private val entrada: InputStream,
    private val limiteBytes: Long
) : InputStream() {
    private var leidos: Long = 0

    override fun read(): Int {
        if (leidos >= limiteBytes) {
            throw LimiteTamanoExcedidoException()
        }
        val b = entrada.read()
        if (b >= 0) {
            leidos++
            if (leidos > limiteBytes) {
                throw LimiteTamanoExcedidoException()
            }
        }
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (leidos >= limiteBytes) {
            throw LimiteTamanoExcedidoException()
        }
        val maxLeer = minOf(len, (limiteBytes - leidos + 1).toInt())
        val n = entrada.read(b, off, maxLeer)
        if (n > 0) {
            leidos += n.toLong()
            if (leidos > limiteBytes) {
                throw LimiteTamanoExcedidoException()
            }
        }
        return n
    }
}
