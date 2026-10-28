package com.example.filtro400.csv

data class ResumenPlan(
    val anyadidas: Int = 0,
    val actualizadas: Int = 0,
    val omitidas: Int = 0,
    val erroneas: Int = 0,
    val errores: List<ImportadorCsv.ErrorLinea> = emptyList()
)

object ResumidorPlan {
    fun resumenDe(plan: PlanImportacion): ResumenPlan {
        var anyadidas = 0
        var actualizadas = 0
        var omitidas = 0
        var erroneas = 0
        val errores = ArrayList<ImportadorCsv.ErrorLinea>()
        for (op in plan.operaciones) {
            when (op) {
                is OperacionCsv.Insertar -> anyadidas++
                is OperacionCsv.Actualizar -> actualizadas++
                is OperacionCsv.Omitir -> omitidas++
                is OperacionCsv.Error -> {
                    erroneas++
                    errores.add(ImportadorCsv.ErrorLinea(op.numeroLineaOriginal, op.motivo))
                }
            }
        }
        return ResumenPlan(anyadidas, actualizadas, omitidas, erroneas, errores.sortedBy { it.numeroLinea })
    }
}
