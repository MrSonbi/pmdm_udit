package com.example.filtro400

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.filtro400.data.BaseDatos
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de la migración v1 → v2 con SQLite real.
 *
 * Usa nombres de BD propios (`test_migracion_*.db`) para no tocar nunca la base
 * de datos real de la app. Los ficheros de prueba se borran antes y después de
 * cada test (solo esos dos nombres, no los datos del emulador).
 */
@RunWith(AndroidJUnit4::class)
class MigracionV1V2Test {

    private lateinit var contexto: Context

    private val nombreV1 = "test_migracion_v1.db"
    private val nombreV2 = "test_migracion_v2.db"

    @Before
    fun preparar() {
        contexto = InstrumentationRegistry.getInstrumentation().targetContext
        contexto.deleteDatabase(nombreV1)
        contexto.deleteDatabase(nombreV2)
    }

    @After
    fun limpiar() {
        contexto.deleteDatabase(nombreV1)
        contexto.deleteDatabase(nombreV2)
    }

    /** Fila mínima de la tabla `reglas` tal y como era en la versión 1. */
    private data class ReglaV1(
        val patron: String,
        val etiqueta: String?,
        val activa: Int,
        val creadaEn: Long
    )

    /**
     * Crea a mano una base de datos con el esquema EXACTO de la v1 y la marca
     * como versión 1. Al abrirla después con [BaseDatos] se dispara onUpgrade.
     */
    private fun crearBaseDatosV1(reglas: List<ReglaV1>): SQLiteDatabase {
        val db = contexto.openOrCreateDatabase(nombreV1, Context.MODE_PRIVATE, null)
        db.execSQL(
            """
            CREATE TABLE reglas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                patron TEXT NOT NULL UNIQUE,
                etiqueta TEXT,
                activa INTEGER NOT NULL DEFAULT 1,
                creada_en INTEGER NOT NULL
            )
            """.trimIndent()
        )
        reglas.forEach { regla ->
            db.execSQL(
                "INSERT INTO reglas (patron, etiqueta, activa, creada_en) VALUES (?, ?, ?, ?)",
                arrayOf(regla.patron, regla.etiqueta, regla.activa, regla.creadaEn)
            )
        }
        db.version = 1
        return db
    }

    private data class Columna(
        val nombre: String,
        val tipo: String,
        val noNula: Int,
        val porDefecto: String?,
        val clavePrimaria: Int
    )

    private fun columnasDe(db: SQLiteDatabase, tabla: String): List<Columna> {
        val columnas = mutableListOf<Columna>()
        db.rawQuery("PRAGMA table_info($tabla)", null).use { cursor ->
            while (cursor.moveToNext()) {
                val porDefecto: String? = if (cursor.isNull(4)) null else cursor.getString(4)
                columnas += Columna(
                    nombre = cursor.getString(1),
                    tipo = cursor.getString(2),
                    noNula = cursor.getInt(3),
                    porDefecto = porDefecto,
                    clavePrimaria = cursor.getInt(5)
                )
            }
        }
        return columnas
    }

    /**
     * Retrato lógico del esquema (tablas, columnas e índices), sin datos.
     * Sirve para comparar una BD migrada con una instalación nueva.
     */
    private fun esquemaLogico(db: SQLiteDatabase): Map<String, List<String>> {
        val esquema = linkedMapOf<String, List<String>>()

        val tablas = mutableListOf<String>()
        db.rawQuery(
            "SELECT name FROM sqlite_master " +
                "WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) tablas += cursor.getString(0)
        }

        tablas.forEach { tabla ->
            esquema["tabla:$tabla"] = columnasDe(db, tabla).map { columna ->
                "${columna.nombre}|${columna.tipo}|${columna.noNula}|" +
                    "${columna.porDefecto ?: ""}|${columna.clavePrimaria}"
            }
        }

        val indices = mutableListOf<String>()
        db.rawQuery(
            "SELECT name, tbl_name, \"unique\" FROM sqlite_master " +
                "WHERE type = 'index' AND sql IS NOT NULL ORDER BY name",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val nombre = cursor.getString(0)
                val tabla = cursor.getString(1)
                val unico = cursor.getInt(2)

                val columnasIndice = mutableListOf<String>()
                db.rawQuery("PRAGMA index_info($nombre)", null).use { info ->
                    while (info.moveToNext()) columnasIndice += info.getString(2)
                }
                indices += "$nombre|$tabla|$unico|${columnasIndice.joinToString(",")}"
            }
        }
        esquema["indices"] = indices

        return esquema
    }

    @Test
    fun migracionConservaDatosYAplicaLosValoresPorDefecto() {
        crearBaseDatosV1(
            listOf(
                ReglaV1("400", "Comerciales", 1, 1_000L),
                ReglaV1("911", "Emergencias", 1, 2_000L),
                ReglaV1("555", "Inactiva", 0, 3_000L)
            )
        ).close()

        val bd = BaseDatos(contexto, nombreV1)
        try {
            val db = bd.readableDatabase
            assertEquals(BaseDatos.VERSION_BD, db.version)

            val filas = mutableListOf<List<String>>()
            db.rawQuery(
                "SELECT patron, etiqueta, activa, creada_en, tipo, accion, prioridad " +
                    "FROM reglas ORDER BY id",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    filas += listOf(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getInt(2).toString(),
                        cursor.getLong(3).toString(),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getInt(6).toString()
                    )
                }
            }

            assertEquals(
                listOf(
                    listOf("400", "Comerciales", "1", "1000", "PREFIJO", "BLOQUEAR", "0"),
                    listOf("911", "Emergencias", "1", "2000", "PREFIJO", "BLOQUEAR", "0"),
                    listOf("555", "Inactiva", "0", "3000", "PREFIJO", "BLOQUEAR", "0")
                ),
                filas
            )
        } finally {
            bd.close()
        }
    }

    @Test
    fun migracionNoVuelveASembrarEl400SiSeBorro() {
        // El usuario borró el 400 en la Fase 2 y se quedó solo con el 911.
        crearBaseDatosV1(listOf(ReglaV1("911", "Emergencias", 1, 1_000L))).close()

        val bd = BaseDatos(contexto, nombreV1)
        try {
            val db = bd.readableDatabase
            db.rawQuery("SELECT COUNT(*) FROM reglas WHERE patron = '400'", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM reglas", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        } finally {
            bd.close()
        }
    }

    @Test
    fun migracionCreaLaTablaRegistroConSusColumnasEIndices() {
        crearBaseDatosV1(listOf(ReglaV1("400", "Comerciales", 1, 1_000L))).close()

        val bd = BaseDatos(contexto, nombreV1)
        try {
            val db = bd.readableDatabase

            val columnas = columnasDe(db, BaseDatos.TABLA_REGISTRO)
            assertEquals(
                listOf(
                    "id",
                    "fecha",
                    "numero_enmascarado",
                    "decision",
                    "regla_id",
                    "regla_patron",
                    "regla_tipo",
                    "regla_prioridad",
                    "motivo",
                    "origen"
                ),
                columnas.map { it.nombre }
            )

            // `regla_id` es la única columna que puede quedar a NULL además de
            // las copias informativas del patrón; fecha y decisión son obligatorias.
            val porNombre = columnas.associateBy { it.nombre }
            assertEquals(1, porNombre.getValue("fecha").noNula)
            assertEquals(1, porNombre.getValue("numero_enmascarado").noNula)
            assertEquals(1, porNombre.getValue("decision").noNula)
            assertEquals(0, porNombre.getValue("regla_id").noNula)
            assertEquals(1, porNombre.getValue("origen").noNula)

            val indices = mutableListOf<String>()
            db.rawQuery(
                "SELECT name FROM sqlite_master " +
                    "WHERE type = 'index' AND tbl_name = 'registro' ORDER BY name",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) indices += cursor.getString(0)
            }
            assertEquals(
                listOf("idx_registro_fecha", "idx_registro_regla_id"),
                indices
            )
        } finally {
            bd.close()
        }
    }

    @Test
    fun esquemaMigradoEsIgualAlDeUnaInstalacionNueva() {
        crearBaseDatosV1(listOf(ReglaV1("400", "Comerciales", 1, 1_000L))).close()

        val bdMigrada = BaseDatos(contexto, nombreV1)
        val esquemaMigrado = try {
            esquemaLogico(bdMigrada.readableDatabase)
        } finally {
            bdMigrada.close()
        }

        val bdNueva = BaseDatos(contexto, nombreV2)
        val esquemaNuevo = try {
            esquemaLogico(bdNueva.readableDatabase)
        } finally {
            bdNueva.close()
        }

        assertEquals(esquemaNuevo, esquemaMigrado)
    }
}
