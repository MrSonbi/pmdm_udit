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
 * Versiones:
 *  - v1 (Fase 2): tabla `reglas` con patrón, etiqueta, activa y fecha.
 *  - v2 (Fase 3): reglas con tipo/acción/prioridad y tabla `registro` de actividad.
 *
 * El nombre de la base de datos se puede inyectar para que los tests
 * instrumentados trabajen sobre una copia y NUNCA toquen la BD real de la app.
 */
class BaseDatos(
    contexto: Context,
    nombreBd: String = NOMBRE_BD
) : SQLiteOpenHelper(
    contexto.applicationContext,
    nombreBd,
    null,
    VERSION_BD
) {

    /**
     * Instalación nueva: se crea directamente el esquema v2 completo.
     *
     * SQLiteOpenHelper NO llama a [onUpgrade] cuando la BD no existe, así que
     * `onCreate` tiene que dejar las dos tablas y los índices ya preparados.
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREAR_TABLA_REGLAS)
        db.execSQL(CREAR_TABLA_REGISTRO)
        db.execSQL(CREAR_INDICE_REGISTRO_FECHA)
        db.execSQL(CREAR_INDICE_REGISTRO_REGLA)
        sembrarReglaInicial(db)
    }

    /**
     * Migración v1 → v2 sin perder las reglas del usuario.
     *
     * Se usa ALTER TABLE (no se reconstruye la tabla) y los valores por defecto
     * dejan las reglas antiguas como prefijos que BLOQUEAN con prioridad 0,
     * que es exactamente el comportamiento de la Fase 2.
     *
     * Importante: aquí NO se siembra el 400. Si el usuario lo borró en v1 debe
     * seguir borrado; la semilla es cosa exclusiva de [onCreate].
     */
    override fun onUpgrade(db: SQLiteDatabase, versionAnterior: Int, versionNueva: Int) {
        if (versionAnterior < 2 && versionNueva >= 2) {
            db.execSQL(AGREGAR_TIPO)
            db.execSQL(AGREGAR_ACCION)
            db.execSQL(AGREGAR_PRIORIDAD)
            db.execSQL(CREAR_TABLA_REGISTRO)
            db.execSQL(CREAR_INDICE_REGISTRO_FECHA)
            db.execSQL(CREAR_INDICE_REGISTRO_REGLA)
        }
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
            // Explícitos aunque el esquema ya tenga valores por defecto: así
            // queda claro cómo nace la regla inicial de la Fase 3.
            put(COL_TIPO, TIPO_PREFIJO)
            put(COL_ACCION, ACCION_BLOQUEAR)
            put(COL_PRIORIDAD, 0)
        }
        db.insertWithOnConflict(
            TABLA_REGLAS,
            null,
            valores,
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    companion object {
        const val VERSION_BD = 2
        const val NOMBRE_BD = "filtro400.db"

        const val TABLA_REGLAS = "reglas"
        const val TABLA_REGISTRO = "registro"

        // Columnas de `reglas`.
        const val COL_ID = "id"
        const val COL_PATRON = "patron"
        const val COL_ETIQUETA = "etiqueta"
        const val COL_ACTIVA = "activa"
        const val COL_CREADA_EN = "creada_en"
        const val COL_TIPO = "tipo"
        const val COL_ACCION = "accion"
        const val COL_PRIORIDAD = "prioridad"

        // Columnas de `registro`.
        const val COL_REG_ID = "id"
        const val COL_REG_FECHA = "fecha"
        const val COL_REG_NUMERO = "numero_enmascarado"
        const val COL_REG_DECISION = "decision"
        const val COL_REG_REGLA_ID = "regla_id"
        const val COL_REG_REGLA_PATRON = "regla_patron"
        const val COL_REG_REGLA_TIPO = "regla_tipo"
        const val COL_REG_REGLA_PRIORIDAD = "regla_prioridad"
        const val COL_REG_MOTIVO = "motivo"
        const val COL_REG_ORIGEN = "origen"

        // Valores de texto compartidos con el futuro paquete `modelo/`.
        const val TIPO_PREFIJO = "PREFIJO"
        const val TIPO_NUMERO = "NUMERO"
        const val ACCION_BLOQUEAR = "BLOQUEAR"
        const val ACCION_PERMITIR = "PERMITIR"
        const val ORIGEN_REAL = "REAL"
        const val ORIGEN_SIMULADA = "SIMULADA"

        private const val PATRON_INICIAL = "400"
        private const val ETIQUETA_INICIAL = "Llamadas comerciales España"

        private const val CREAR_TABLA_REGLAS = """
            CREATE TABLE $TABLA_REGLAS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PATRON TEXT NOT NULL UNIQUE,
                $COL_ETIQUETA TEXT,
                $COL_ACTIVA INTEGER NOT NULL DEFAULT 1,
                $COL_CREADA_EN INTEGER NOT NULL,
                $COL_TIPO TEXT NOT NULL DEFAULT '$TIPO_PREFIJO',
                $COL_ACCION TEXT NOT NULL DEFAULT '$ACCION_BLOQUEAR',
                $COL_PRIORIDAD INTEGER NOT NULL DEFAULT 0
            )
        """

        // El registro guarda una copia del patrón/tipo/prioridad de la regla
        // para que el historial siga siendo legible aunque la regla se borre.
        private const val CREAR_TABLA_REGISTRO = """
            CREATE TABLE $TABLA_REGISTRO (
                $COL_REG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_REG_FECHA INTEGER NOT NULL,
                $COL_REG_NUMERO TEXT NOT NULL,
                $COL_REG_DECISION TEXT NOT NULL,
                $COL_REG_REGLA_ID INTEGER,
                $COL_REG_REGLA_PATRON TEXT,
                $COL_REG_REGLA_TIPO TEXT,
                $COL_REG_REGLA_PRIORIDAD INTEGER,
                $COL_REG_MOTIVO TEXT,
                $COL_REG_ORIGEN TEXT NOT NULL CHECK ($COL_REG_ORIGEN IN ('$ORIGEN_REAL', '$ORIGEN_SIMULADA'))
            )
        """

        private const val CREAR_INDICE_REGISTRO_FECHA =
            "CREATE INDEX idx_registro_fecha ON $TABLA_REGISTRO ($COL_REG_FECHA)"

        private const val CREAR_INDICE_REGISTRO_REGLA =
            "CREATE INDEX idx_registro_regla_id ON $TABLA_REGISTRO ($COL_REG_REGLA_ID)"

        private const val AGREGAR_TIPO =
            "ALTER TABLE $TABLA_REGLAS ADD COLUMN $COL_TIPO TEXT NOT NULL DEFAULT '$TIPO_PREFIJO'"

        private const val AGREGAR_ACCION =
            "ALTER TABLE $TABLA_REGLAS ADD COLUMN $COL_ACCION TEXT NOT NULL DEFAULT '$ACCION_BLOQUEAR'"

        private const val AGREGAR_PRIORIDAD =
            "ALTER TABLE $TABLA_REGLAS ADD COLUMN $COL_PRIORIDAD INTEGER NOT NULL DEFAULT 0"
    }
}
