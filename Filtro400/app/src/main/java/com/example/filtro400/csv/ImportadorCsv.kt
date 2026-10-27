package com.example.filtro400.csv

import com.example.filtro400.data.BaseDatos
import com.example.filtro400.data.RepositorioReglas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Importador CSV con SAF.
 */
class ImportadorCsv(
    private val repositorioReglas: RepositorioReglas
) {
    data class ResultadoImportacion(
        val anyadidas: Int = 0,
        val actualizadas: Int = 0,
        val omitidas: Int = 0,
        val errores: List<ErrorLinea> = emptyList(),
        val fallo: Boolean = false,
        val mensaje: String? = null
    )

    data class ErrorLinea(
        val numeroLinea: Int,
        val mensaje: String
    )

    suspend fun importar(filas: List<FilaCsv>): ResultadoImportacion = withContext(Dispatchers.IO) {
        procesarLineas(filas)
    }

    fun procesarLineas(filas: List<FilaCsv>): ResultadoImportacion {
        val plan = PlanificadorImportacion.planificarImportacion(repositorioReglas.listarTodas(), filas)
        return repositorioReglas.aplicarPlan(plan)
    }

    suspend fun importarDesdeStream(inputStream: InputStream): ResultadoImportacion = withContext(Dispatchers.IO) {
        try {
            val resultadoParseo = CsvCodec.leerConResultado(inputStream)
            val erroresParseo = resultadoParseo.errores.map { ErrorLinea(it.numeroLineaOriginal, it.motivo) }
            val plan = PlanificadorImportacion.planificarImportacion(repositorioReglas.listarTodas(), resultadoParseo.filas)
            val res = repositorioReglas.aplicarPlan(plan)
            // fusionar errores de parseo y del plan, ordenados por nº de línea
            val todosErrores = (erroresParseo + res.errores).sortedBy { it.numeroLinea }
            return@withContext res.copy(errores = todosErrores.take(50) + if (todosErrores.size > 50) listOf(ErrorLinea(0, "y ${todosErrores.size - 50} más")) else emptyList())
        } catch (e: LimiteTamanoExcedidoException) {
            return@withContext ResultadoImportacion(fallo = true, mensaje = "límite de 1 MB excedido")
        } catch (e: LimiteFilasExcedidoException) {
            return@withContext ResultadoImportacion(fallo = true, mensaje = "límite de 5000 filas excedido")
        } catch (e: Exception) {
            if (e.message == "no se ha cambiado nada") {
                return@withContext ResultadoImportacion(fallo = true, mensaje = "no se ha cambiado nada")
            }
            return@withContext ResultadoImportacion(fallo = true, mensaje = "no se ha cambiado nada")
        }
    }
}
