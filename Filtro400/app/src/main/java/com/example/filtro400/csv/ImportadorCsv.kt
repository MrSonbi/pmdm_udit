package com.example.filtro400.csv

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
    /**
     * Resultado de una importación.
     *
     * - [erroneas]: número TOTAL de filas con error (de parseo y del plan),
     *   calculado antes de recortar la lista [errores] a 50 + "y N más".
     * - [mensaje]: solo si [fallo] es true; frase completa que explica la causa.
     */
    data class ResultadoImportacion(
        val anyadidas: Int = 0,
        val actualizadas: Int = 0,
        val omitidas: Int = 0,
        val errores: List<ErrorLinea> = emptyList(),
        val fallo: Boolean = false,
        val mensaje: String? = null,
        val erroneas: Int = 0
    )

    data class ErrorLinea(
        val numeroLinea: Int,
        val mensaje: String
    )

    suspend fun importarDesdeStream(inputStream: InputStream): ResultadoImportacion = withContext(Dispatchers.IO) {
        try {
            val resultadoParseo = CsvCodec.leerConResultado(inputStream)
            val erroresParseo = resultadoParseo.errores.map { ErrorLinea(it.numeroLineaOriginal, it.motivo) }
            val plan = PlanificadorImportacion.planificarImportacion(repositorioReglas.listarTodas(), resultadoParseo.filas)
            // aplicarPlan devuelve TODOS los errores del plan, sin recortar.
            val res = repositorioReglas.aplicarPlan(plan)
            // Se juntan errores de parseo y del plan, se ordenan por línea y se
            // recortan UNA sola vez ("y N más" sobre el total real).
            val todosErrores = (erroresParseo + res.errores).sortedBy { it.numeroLinea }
            val mensaje = if (res.fallo) MENSAJE_FALLO_BD else null
            return@withContext res.copy(
                errores = RecorteErrores.recortar(todosErrores),
                erroneas = todosErrores.size,
                mensaje = mensaje
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: LimiteTamanoExcedidoException) {
            return@withContext ResultadoImportacion(
                fallo = true,
                mensaje = "El fichero supera el límite de 1 MB."
            )
        } catch (e: LimiteFilasExcedidoException) {
            return@withContext ResultadoImportacion(
                fallo = true,
                mensaje = "El fichero supera el límite de 5000 filas."
            )
        } catch (e: Exception) {
            return@withContext ResultadoImportacion(
                fallo = true,
                mensaje = "No se pudo leer el fichero. Comprueba que es un CSV de texto en UTF-8."
            )
        }
    }

    companion object {
        /** Mensaje para el usuario cuando la transacción de [RepositorioReglas.aplicarPlan] falla. */
        const val MENSAJE_FALLO_BD = "No se pudieron guardar las reglas en la base de datos."
    }
}
