package com.example.filtro400.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

/**
 * Único punto de acceso a la tabla `reglas`.
 *
 * El servicio de llamadas y la interfaz comparten esta misma instancia (singleton)
 * porque viven en el mismo proceso. Así el servicio NO consulta SQLite en cada
 * llamada: lee una copia inmutable de las reglas activas que está en memoria.
 *
 * La copia se carga la primera vez que se pide (el proceso puede arrancar solo
 * porque entra una llamada) y se refresca después de cada alta, edición, baja
 * o cambio de estado. El acceso a la BD desde la UI se hace en Dispatchers.IO.
 */
class RepositorioReglas private constructor(contexto: Context) {

    private val baseDatos = BaseDatos(contexto.applicationContext)

    // @Volatile: el hilo del servicio y el de la UI ven siempre la última copia.
    @Volatile
    private var cacheActivas: List<Regla>? = null

    /** Todas las reglas, para la pantalla de gestión. */
    fun listarTodas(): List<Regla> {
        return baseDatos.readableDatabase.query(
            BaseDatos.TABLA_REGLAS,
            null,
            null,
            null,
            null,
            null,
            "${BaseDatos.COL_CREADA_EN} ASC, ${BaseDatos.COL_ID} ASC"
        ).use { cursor -> leerReglas(cursor) }
    }

    /**
     * Copia en memoria de las reglas activas. Es lo único que lee el servicio
     * durante una llamada, para que `onScreenCall` sea rápido y no haga SQL.
     */
    fun reglasActivas(): List<Regla> {
        val copiaActual = cacheActivas
        if (copiaActual != null) return copiaActual

        return synchronized(this) {
            val copiaDentro = cacheActivas
            if (copiaDentro != null) {
                copiaDentro
            } else {
                leerActivas().also { cacheActivas = it }
            }
        }
    }

    fun insertar(patron: String, etiqueta: String?): Long {
        val valores = ContentValues().apply {
            put(BaseDatos.COL_PATRON, patron)
            put(BaseDatos.COL_ETIQUETA, etiquetaLimpia(etiqueta))
            put(BaseDatos.COL_ACTIVA, 1)
            put(BaseDatos.COL_CREADA_EN, System.currentTimeMillis())
        }
        val id = baseDatos.writableDatabase.insertOrThrow(BaseDatos.TABLA_REGLAS, null, valores)
        refrescarCache()
        return id
    }

    fun actualizar(id: Long, patron: String, etiqueta: String?) {
        val valores = ContentValues().apply {
            put(BaseDatos.COL_PATRON, patron)
            put(BaseDatos.COL_ETIQUETA, etiquetaLimpia(etiqueta))
        }
        baseDatos.writableDatabase.update(
            BaseDatos.TABLA_REGLAS,
            valores,
            "${BaseDatos.COL_ID} = ?",
            arrayOf(id.toString())
        )
        refrescarCache()
    }

    fun cambiarActiva(id: Long, activa: Boolean) {
        val valores = ContentValues().apply {
            put(BaseDatos.COL_ACTIVA, if (activa) 1 else 0)
        }
        baseDatos.writableDatabase.update(
            BaseDatos.TABLA_REGLAS,
            valores,
            "${BaseDatos.COL_ID} = ?",
            arrayOf(id.toString())
        )
        refrescarCache()
    }

    fun eliminar(id: Long) {
        baseDatos.writableDatabase.delete(
            BaseDatos.TABLA_REGLAS,
            "${BaseDatos.COL_ID} = ?",
            arrayOf(id.toString())
        )
        refrescarCache()
    }

    @Synchronized
    private fun refrescarCache() {
        // Si la lectura falla, dejamos la caché a null para que la próxima
        // llamada vuelva a intentar cargarla, en vez de usar datos viejos.
        cacheActivas = null
        cacheActivas = leerActivas()
    }

    private fun leerActivas(): List<Regla> {
        return baseDatos.readableDatabase.query(
            BaseDatos.TABLA_REGLAS,
            null,
            "${BaseDatos.COL_ACTIVA} = 1",
            null,
            null,
            null,
            "${BaseDatos.COL_CREADA_EN} ASC, ${BaseDatos.COL_ID} ASC"
        ).use { cursor -> leerReglas(cursor) }
    }

    private fun leerReglas(cursor: Cursor): List<Regla> {
        val indiceId = cursor.getColumnIndexOrThrow(BaseDatos.COL_ID)
        val indicePatron = cursor.getColumnIndexOrThrow(BaseDatos.COL_PATRON)
        val indiceEtiqueta = cursor.getColumnIndexOrThrow(BaseDatos.COL_ETIQUETA)
        val indiceActiva = cursor.getColumnIndexOrThrow(BaseDatos.COL_ACTIVA)
        val indiceCreadaEn = cursor.getColumnIndexOrThrow(BaseDatos.COL_CREADA_EN)

        val reglas = ArrayList<Regla>(cursor.count)
        while (cursor.moveToNext()) {
            reglas += Regla(
                id = cursor.getLong(indiceId),
                patron = cursor.getString(indicePatron),
                etiqueta = if (cursor.isNull(indiceEtiqueta)) null else cursor.getString(indiceEtiqueta),
                activa = cursor.getInt(indiceActiva) == 1,
                creadaEn = cursor.getLong(indiceCreadaEn)
            )
        }
        return reglas
    }

    private fun etiquetaLimpia(etiqueta: String?): String? {
        return etiqueta?.trim()?.ifEmpty { null }
    }

    companion object {
        @Volatile
        private var instancia: RepositorioReglas? = null

        /** Devuelve la instancia única usando siempre el contexto de la aplicación. */
        fun obtener(contexto: Context): RepositorioReglas {
            return instancia ?: synchronized(this) {
                instancia ?: RepositorioReglas(contexto.applicationContext).also { instancia = it }
            }
        }
    }
}
