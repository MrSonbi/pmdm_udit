package com.example.filtro400.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Crea y versiona la base de datos SQLite.
 *
 * Usamos SQLiteOpenHelper "a pelo" (sin Room) porque el enunciado lo pide y
 * porque así el proyecto no depende de KSP/kapt ni de procesadores de anotaciones.
 *
 * En la Fase 2 el repositorio hace el CRUD; esta clase solo se encarga del esquema.
 */
class BaseDatos(contexto: Context) : SQLiteOpenHelper(
    contexto.applicationContext,
    NOMBRE_BD,
    null,
    VERSION_BD
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREAR_TABLA_REGLAS)
        sembrarReglaInicial(db)
    }

    /**
     * La Fase 3 migrará la tabla sin perder datos con ALTER TABLE. De momento
     * solo existe la versión 1, así que no hay nada que migrar.
     *
     * Ejemplo previsto:
     *   if (versionAnterior < 2) {
     *       db.execSQL("ALTER TABLE ... ADD COLUMN tipo TEXT NOT NULL DEFAULT 'PREFIJO'")
     *       ...
     *   }
     */
    override fun onUpgrade(db: SQLiteDatabase, versionAnterior: Int, versionNueva: Int) {
        // Intencionadamente vacío: la v1 es la primera versión.
        // No se debe borrar ni recrear la tabla porque se perderían las reglas.
    }

    /**
     * La regla 400 se siembra SOLO al crear la base de datos.
     * Si el usuario la borra, no se vuelve a crear (el CRUD es completo).
     */
    private fun sembrarReglaInicial(db: SQLiteDatabase) {
        val valores = ContentValues().apply {
            put(COL_PATRON, PATRON_INICIAL)
            put(COL_ETIQUETA, ETIQUETA_INICIAL)
            put(COL_ACTIVA, 1)
            put(COL_CREADA_EN, System.currentTimeMillis())
        }
        db.insertWithOnConflict(
            TABLA_REGLAS,
            null,
            valores,
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    companion object {
        const val VERSION_BD = 1
        const val NOMBRE_BD = "filtro400.db"

        const val TABLA_REGLAS = "reglas"

        const val COL_ID = "id"
        const val COL_PATRON = "patron"
        const val COL_ETIQUETA = "etiqueta"
        const val COL_ACTIVA = "activa"
        const val COL_CREADA_EN = "creada_en"

        private const val PATRON_INICIAL = "400"
        private const val ETIQUETA_INICIAL = "Llamadas comerciales España"

        private const val CREAR_TABLA_REGLAS = """
            CREATE TABLE $TABLA_REGLAS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PATRON TEXT NOT NULL UNIQUE,
                $COL_ETIQUETA TEXT,
                $COL_ACTIVA INTEGER NOT NULL DEFAULT 1,
                $COL_CREADA_EN INTEGER NOT NULL
            )
        """
    }
}
