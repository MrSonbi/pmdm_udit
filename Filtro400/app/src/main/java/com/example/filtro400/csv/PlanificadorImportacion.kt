package com.example.filtro400.csv

import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.motor.ValidacionReglas

object PlanificadorImportacion {

    fun planificarImportacion(existentes: List<Regla>, filas: List<FilaCsv>): PlanImportacion {
        val operaciones = ArrayList<OperacionCsv>()
        val mapaPorPatronNormalizado = HashMap<String, Regla>()
        for (e in existentes) {
            mapaPorPatronNormalizado[ReglaBloqueo.normalizar(e.patron)] = e
        }
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
                vistoEnFichero.add(patronNormalizadoFila)

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

                val existeEnBd = mapaPorPatronNormalizado.containsKey(patronNormalizadoFila)

                if (!validacion.esValida) {
                    operaciones.add(OperacionCsv.Error(numeroLinea, validacion.error ?: "inválido"))
                    continue
                }

                val patronParaGuardar = validacion.patron ?: patronNormalizadoFila
                val prioridadParaGuardar = if (validacion.esValida) validacion.prioridad else prioridadFinal
                val activa = fila.activa

                if (existeEnBd) {
                    val reglaExistente = mapaPorPatronNormalizado[patronNormalizadoFila]!!
                    val etiquetaCsvLimpia = fila.etiqueta?.trim()?.ifEmpty { null }
                    val etiquetaNueva = if (etiquetaCsvLimpia == null && fila.etiqueta?.trim()?.isEmpty() == true) {
                        reglaExistente.etiqueta
                    } else if (fila.etiqueta == null) {
                        reglaExistente.etiqueta
                    } else {
                        etiquetaCsvLimpia
                    }
                    val activaNueva = activa ?: reglaExistente.activa
                    val prioridadParaActualizar = if (fila.prioridad == null) reglaExistente.prioridad else prioridadParaGuardar
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
                    val prioridadAlta = fila.prioridad ?: ValidacionReglas.prioridadPorDefecto(fila.accion)
                    operaciones.add(
                        OperacionCsv.Insertar(
                            numeroLineaOriginal = numeroLinea,
                            patron = patronParaGuardar,
                            etiqueta = fila.etiqueta?.trim()?.ifEmpty { null },
                            tipo = fila.tipo,
                            accion = fila.accion,
                            prioridad = prioridadAlta,
                            activa = activa ?: true
                        )
                    )
                    val nuevaRegla = Regla(
                        id = -1,
                        patron = patronParaGuardar,
                        etiqueta = fila.etiqueta?.trim()?.ifEmpty { null },
                        activa = activa ?: true,
                        creadaEn = 0,
                        tipo = fila.tipo,
                        accion = fila.accion,
                        prioridad = prioridadAlta
                    )
                    mapaPorPatronNormalizado[patronNormalizadoFila] = nuevaRegla
                }
            } catch (e: Exception) {
                operaciones.add(OperacionCsv.Error(numeroLinea, "inválido"))
            }
        }
        return PlanImportacion(operaciones)
    }
}
