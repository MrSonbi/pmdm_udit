package com.example.filtro400.registro

import android.content.ContentValues
import android.content.Context
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.util.Enmascarar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Acceso a la tabla `registro`. Se usa desde el servicio (tras responder)
 * y desde la UI para listar/borrar.
 */
class RepositorioRegistro private constructor(contexto: Context) {

    private val baseDatos = BaseDatos(contexto.applicationContext)

    suspend fun insertar(
        numero: String,
        decision: Accion,
        reglaId: Long?,
        reglaPatron: String?,
        reglaTipo: TipoRegla?,
        reglaPrioridad: Int?,
        motivo: String?,
        origen: OrigenLlamada
    ) = withContext(Dispatchers.IO) {
        val valores = ContentValues().apply {
            put(BaseDatos.COL_REG_FECHA, System.currentTimeMillis())
            put(BaseDatos.COL_REG_NUMERO, Enmascarar.numero(numero))
            put(BaseDatos.COL_REG_DECISION, decision.name)
            put(BaseDatos.COL_REG_REGLA_ID, reglaId)
            put(BaseDatos.COL_REG_REGLA_PATRON, reglaPatron)
            put(BaseDatos.COL_REG_REGLA_TIPO, reglaTipo?.name)
            put(BaseDatos.COL_REG_REGLA_PRIORIDAD, reglaPrioridad)
            put(BaseDatos.COL_REG_MOTIVO, motivo)
            put(BaseDatos.COL_REG_ORIGEN, origen.name)
        }
        baseDatos.writableDatabase.insert(BaseDatos.TABLA_REGISTRO, null, valores)
    }

    suspend fun borrarMasAntiguosDe(dias: Int = 30) = withContext(Dispatchers.IO) {
        val limite = System.currentTimeMillis() - dias.toLong() * 24L * 60L * 60L * 1000L
        baseDatos.writableDatabase.delete(
            BaseDatos.TABLA_REGISTRO,
            "${BaseDatos.COL_REG_FECHA} < ?",
            arrayOf(limite.toString())
        )
    }

    suspend fun borrarTodos() = withContext(Dispatchers.IO) {
        baseDatos.writableDatabase.delete(BaseDatos.TABLA_REGISTRO, null, null)
    }

    suspend fun contar(): Int = withContext(Dispatchers.IO) {
        baseDatos.readableDatabase.rawQuery("SELECT COUNT(*) FROM ${BaseDatos.TABLA_REGISTRO}", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    companion object {
        @Volatile
        private var instancia: RepositorioRegistro? = null

        fun obtener(contexto: Context): RepositorioRegistro {
            return instancia ?: synchronized(this) {
                instancia ?: RepositorioRegistro(contexto.applicationContext).also { instancia = it }
            }
        }
    }
}
