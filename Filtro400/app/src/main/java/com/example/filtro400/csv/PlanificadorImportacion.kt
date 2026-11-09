package com.example.filtro400.csv

import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.motor.ValidacionReglas

/**
 * Decide, SIN tocar la BD, qué hacer con cada fila del CSV: insertar,
 * actualizar, omitir (sin cambios) o marcarla como error.
 *
 * Es Kotlin puro: se prueba en JVM y [com.example.filtro400.data.RepositorioReglas.aplicarPlan]
 * solo ejecuta el plan resultante dentro de una transacción.
 */
object PlanificadorImportacion {

    fun planificarImportacion(existentes: List<Regla>, filas: List<FilaCsv>): PlanImportacion {
        val operaciones = ArrayList<OperacionCsv>()
        val mapaPorPatronNormalizado = HashMap<String, Regla>()
        for (e in existentes) {
            mapaPorPatronNormalizado[ReglaBloqueo.normalizar(e.patron)] = e
        }
        // Patrones de filas VÁLIDAS ya planificadas. Solo se añade tras validar:
        // una fila inválida no "ocupa" el patrón y no convierte en duplicada
        // a una fila válida posterior con el mismo patrón.
        val vistoEnFichero = HashSet<String>()

        for (fila in filas) {
            val numeroLinea = fila.numeroLineaOriginal
            try {
                val patronNormalizadoFila = ReglaBloqueo.normalizar(fila.patron)
                if (patronNormalizadoFila.isEmpty()) {
                    operaciones.add(OperacionCsv.Error(numeroLinea, "patrón vacío"))
                    continue
                }
                if (vistoEnFichero.contains(patronNormalizadoFila)) {
                    operaciones.add(OperacionCsv.Error(numeroLinea, "duplicado en el fichero"))
                    continue
                }

                if (fila.tipo == TipoRegla.PREFIJO) {
                    val digitos = patronNormalizadoFila.count { it.isDigit() }
                    if (digitos <= 2) {
                        operaciones.add(OperacionCsv.Error(numeroLinea, "prefijo demasiado corto para importar"))
                        continue
                    }
                }

                val prioridadFinal = fila.prioridad ?: ValidacionReglas.prioridadPorDefecto(fila.accion)
                if (prioridadFinal < 0 || prioridadFinal > 100) {
                    operaciones.add(OperacionCsv.Error(numeroLinea, "prioridad fuera de rango"))
                    continue
                }

                // Los duplicados los decide el planificador con su mapa
                val validacion = ValidacionReglas.validar(
                    entrada = fila.patron,
                    patronesExistentes = emptyList(),
                    tipo = fila.tipo,
                    accion = fila.accion,
                    prioridad = prioridadFinal
                )
                if (!validacion.esValida) {
                    operaciones.add(OperacionCsv.Error(numeroLinea, validacion.error ?: "inválido"))
                    continue
                }

                // A partir de aquí la fila es válida: ya cuenta como "vista".
                vistoEnFichero.add(patronNormalizadoFila)

                val patronParaGuardar = validacion.patron ?: patronNormalizadoFila
                val reglaExistente = mapaPorPatronNormalizado[patronNormalizadoFila]

                if (reglaExistente != null) {
                    // Etiqueta vacía o ausente en el CSV -> se conserva la existente.
                    val etiquetaCsvLimpia = fila.etiqueta?.trim()?.ifEmpty { null }
                    val etiquetaNueva = etiquetaCsvLimpia ?: reglaExistente.etiqueta
                    val activaNueva = fila.activa ?: reglaExistente.activa
                    // Prioridad vacía en el CSV:
                    //  - misma acción  -> se conserva la prioridad existente;
                    //  - acción nueva  -> prioridad por defecto de la NUEVA acción
                    //    (si no, un BLOQUEAR 0 pasado a PERMITIR seguiría en 0).
                    val prioridadParaActualizar = fila.prioridad
                        ?: if (fila.accion == reglaExistente.accion) {
                            reglaExistente.prioridad
                        } else {
                            ValidacionReglas.prioridadPorDefecto(fila.accion)
                        }
                    if (reglaExistente.tipo == fila.tipo &&
                        reglaExistente.accion == fila.accion &&
                        reglaExistente.prioridad == prioridadParaActualizar &&
                        reglaExistente.etiqueta == etiquetaNueva &&
                        reglaExistente.activa == activaNueva
                    ) {
                        operaciones.add(OperacionCsv.Omitir(numeroLinea, "sin cambios"))
                        continue
                    }
                    operaciones.add(
                        OperacionCsv.Actualizar(
                            numeroLineaOriginal = numeroLinea,
                            id = reglaExistente.id,
                            patron = patronParaGuardar,
                            etiqueta = etiquetaNueva,
                            tipo = fila.tipo,
                            accion = fila.accion,
                            prioridad = prioridadParaActualizar,
                            activa = activaNueva
                        )
                    )
                    mapaPorPatronNormalizado[patronNormalizadoFila] = reglaExistente.copy(
                        tipo = fila.tipo,
                        accion = fila.accion,
                        prioridad = prioridadParaActualizar,
                        etiqueta = etiquetaNueva,
                        activa = activaNueva
                    )
                } else {
                    val etiquetaNueva = fila.etiqueta?.trim()?.ifEmpty { null }
                    val activaNueva = fila.activa ?: true
                    // Alta: validar ya devolvió la prioridad del CSV o la de por defecto.
                    val prioridadNueva = validacion.prioridad
                    operaciones.add(
                        OperacionCsv.Insertar(
                            numeroLineaOriginal = numeroLinea,
                            patron = patronParaGuardar,
                            etiqueta = etiquetaNueva,
                            tipo = fila.tipo,
                            accion = fila.accion,
                            prioridad = prioridadNueva,
                            activa = activaNueva
                        )
                    )
                    mapaPorPatronNormalizado[patronNormalizadoFila] = Regla(
                        id = -1,
                        patron = patronParaGuardar,
                        etiqueta = etiquetaNueva,
                        activa = activaNueva,
                        creadaEn = 0,
                        tipo = fila.tipo,
                        accion = fila.accion,
                        prioridad = prioridadNueva
                    )
                }
            } catch (e: Exception) {
                operaciones.add(OperacionCsv.Error(numeroLinea, "inválido"))
            }
        }
        return PlanImportacion(operaciones)
    }
}
