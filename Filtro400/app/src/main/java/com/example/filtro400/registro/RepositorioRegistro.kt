package com.example.filtro400.registro

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.example.filtro400.data.AlmacenDatos
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.util.Enmascarar
import com.example.filtro400.util.Fechas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId

/**
 * Acceso a la tabla `registro`. Se usa desde el servicio (tras responder)
 * y desde la UI para listar, borrar y calcular estadísticas.
 *
 * La BD se inyecta por constructor:
 *  - producción: [obtener] usa la BD de [AlmacenDatos] (`filtro400.db`);
 *  - tests: `RepositorioRegistro(BaseDatos(contexto, "test_xxx.db"))`.
 *
 * Todas las consultas de lectura reciben `incluirSimulaciones` (false por
 * defecto): las simulaciones guardadas desde el Simulador (origen SIMULADA)
 * no deben inflar las cifras reales.
 */
class RepositorioRegistro(
    private val baseDatos: BaseDatos
) {

    /**
     * Resumen numérico para la pestaña de estadísticas. Bloqueadas y
     * permitidas van SIEMPRE por separado: un total mezclado no dice nada útil.
     *
     * - "hoy": desde las 00:00 locales de hoy.
     * - "7 días": hoy y los 6 días de calendario anteriores (desde las 00:00
     *   locales de hace 6 días).
     */
    data class Estadisticas(
        val total: Int,
        val bloqueadas: Int,
        val permitidas: Int,
        val bloqueadasHoy: Int,
        val permitidasHoy: Int,
        val bloqueadas7Dias: Int,
        val permitidas7Dias: Int
    ) {
        companion object {
            val VACIAS = Estadisticas(0, 0, 0, 0, 0, 0, 0)
        }
    }

    /** Una entrada del ranking: cuántas veces ha decidido una regla. */
    data class UsoRegla(
        val patron: String,
        val veces: Int
    )

    /**
     * Inserción síncrona para usarla desde el servicio tras responder a la llamada.
     * El enmascarado se aplica AQUÍ con [Enmascarar.numero]; la columna
     * [BaseDatos.COL_REG_NUMERO] nunca recibe el número completo.
     *
     * [fecha] es opcional: en producción es "ahora"; los tests la pasan para
     * sembrar filas antiguas sin escribir SQL a mano.
     */
    fun insertarSincrono(
        numero: String,
        decision: Accion,
        reglaId: Long?,
        reglaPatron: String?,
        reglaTipo: TipoRegla?,
        reglaPrioridad: Int?,
        motivo: String?,
        origen: OrigenLlamada,
        fecha: Long = System.currentTimeMillis()
    ): Long {
        val valores = ContentValues().apply {
            put(BaseDatos.COL_REG_FECHA, fecha)
            put(BaseDatos.COL_REG_NUMERO, Enmascarar.numero(numero))
            put(BaseDatos.COL_REG_DECISION, decision.name)
            put(BaseDatos.COL_REG_REGLA_ID, reglaId)
            put(BaseDatos.COL_REG_REGLA_PATRON, reglaPatron)
            put(BaseDatos.COL_REG_REGLA_TIPO, reglaTipo?.name)
            put(BaseDatos.COL_REG_REGLA_PRIORIDAD, reglaPrioridad)
            put(BaseDatos.COL_REG_MOTIVO, motivo)
            put(BaseDatos.COL_REG_ORIGEN, origen.name)
        }
        return baseDatos.writableDatabase.insert(BaseDatos.TABLA_REGISTRO, null, valores)
    }

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
        insertarSincrono(numero, decision, reglaId, reglaPatron, reglaTipo, reglaPrioridad, motivo, origen)
    }

    /**
     * Borra las filas con más de [dias] días respecto a [ahora].
     * Devuelve cuántas filas se han borrado.
     */
    suspend fun borrarMasAntiguosDe(
        dias: Int = 30,
        ahora: Long = System.currentTimeMillis()
    ): Int = withContext(Dispatchers.IO) {
        val limite = ahora - dias.toLong() * MILIS_DIA
        baseDatos.writableDatabase.delete(
            BaseDatos.TABLA_REGISTRO,
            "${BaseDatos.COL_REG_FECHA} < ?",
            arrayOf(limite.toString())
        )
    }

    suspend fun borrarTodos() = withContext(Dispatchers.IO) {
        baseDatos.writableDatabase.delete(BaseDatos.TABLA_REGISTRO, null, null)
    }

    /** Número total de filas (reales y simuladas). */
    suspend fun contar(): Int = withContext(Dispatchers.IO) {
        baseDatos.readableDatabase.rawQuery("SELECT COUNT(*) FROM ${BaseDatos.TABLA_REGISTRO}", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    /**
     * Totales en UNA sola consulta (un único recorrido de la tabla).
     *
     * - "hoy": desde las 00:00 de [zona] del día de [ahora] ([Fechas.inicioDelDia]).
     * - "últimos 7 días": hoy y los 6 días de calendario anteriores, desde las
     *   00:00 de [zona] ([Fechas.inicioDelDiaHaceDias] con 6).
     *
     * [ahora] y [zona] se inyectan para que los tests no dependan del reloj ni
     * de la zona del dispositivo. COALESCE: con la tabla vacía SUM devuelve
     * NULL; así devolvemos 0.
     */
    suspend fun estadisticas(
        incluirSimulaciones: Boolean = false,
        ahora: Long = System.currentTimeMillis(),
        zona: ZoneId = ZoneId.systemDefault()
    ): Estadisticas = withContext(Dispatchers.IO) {
        val inicioHoy = Fechas.inicioDelDia(ahora, zona)
        val inicio7Dias = Fechas.inicioDelDiaHaceDias(ahora, 6, zona)
        val (filtro, argsFiltro) = filtroOrigen(incluirSimulaciones)

        val decision = BaseDatos.COL_REG_DECISION
        val fecha = BaseDatos.COL_REG_FECHA
        val sql = """
            SELECT
                COUNT(*),
                COALESCE(SUM(CASE WHEN $decision = ? THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN $decision = ? THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN $decision = ? AND $fecha >= ? THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN $decision = ? AND $fecha >= ? THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN $decision = ? AND $fecha >= ? THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN $decision = ? AND $fecha >= ? THEN 1 ELSE 0 END), 0)
            FROM ${BaseDatos.TABLA_REGISTRO}
            $filtro
        """.trimIndent()
        val bloquear = Accion.BLOQUEAR.name
        val permitir = Accion.PERMITIR.name
        // El orden de los argumentos sigue el de los "?" de la consulta.
        val args = arrayOf(
            bloquear,
            permitir,
            bloquear, inicioHoy.toString(),
            permitir, inicioHoy.toString(),
            bloquear, inicio7Dias.toString(),
            permitir, inicio7Dias.toString()
        ) + argsFiltro

        baseDatos.readableDatabase.rawQuery(sql, args).use { cursor ->
            if (!cursor.moveToFirst()) {
                Estadisticas.VACIAS
            } else {
                Estadisticas(
                    total = cursor.getInt(0),
                    bloqueadas = cursor.getInt(1),
                    permitidas = cursor.getInt(2),
                    bloqueadasHoy = cursor.getInt(3),
                    permitidasHoy = cursor.getInt(4),
                    bloqueadas7Dias = cursor.getInt(5),
                    permitidas7Dias = cursor.getInt(6)
                )
            }
        }
    }

    /**
     * Reglas que más veces han decidido una llamada.
     *
     * Orden DETERMINISTA: primero por número de veces (desc) y, en empate, por
     * patrón alfabético (asc). Sin el segundo criterio SQLite puede devolver
     * los empatados en cualquier orden y la pantalla "bailaría".
     */
    suspend fun rankingReglas(
        incluirSimulaciones: Boolean = false,
        limite: Int = 10
    ): List<UsoRegla> = withContext(Dispatchers.IO) {
        val (filtro, argsFiltro) = filtroOrigen(incluirSimulaciones)
        val condicionPatron = "${BaseDatos.COL_REG_REGLA_PATRON} IS NOT NULL"
        val where = if (filtro.isEmpty()) "WHERE $condicionPatron" else "$filtro AND $condicionPatron"

        val sql = """
            SELECT ${BaseDatos.COL_REG_REGLA_PATRON}, COUNT(*) AS c
            FROM ${BaseDatos.TABLA_REGISTRO}
            $where
            GROUP BY ${BaseDatos.COL_REG_REGLA_PATRON}
            ORDER BY c DESC, ${BaseDatos.COL_REG_REGLA_PATRON} ASC
            LIMIT ?
        """.trimIndent()

        baseDatos.readableDatabase.rawQuery(sql, argsFiltro + limite.toString()).use { cursor ->
            val resultado = ArrayList<UsoRegla>(cursor.count)
            while (cursor.moveToNext()) {
                resultado += UsoRegla(patron = cursor.getString(0), veces = cursor.getInt(1))
            }
            resultado
        }
    }

    /** Últimas filas del registro, la más reciente primero. */
    suspend fun listarRecientes(
        incluirSimulaciones: Boolean = false,
        limite: Int = 100
    ): List<RegistroLlamada> = withContext(Dispatchers.IO) {
        val (filtro, argsFiltro) = filtroOrigen(incluirSimulaciones)
        val sql = """
            SELECT * FROM ${BaseDatos.TABLA_REGISTRO}
            $filtro
            ORDER BY ${BaseDatos.COL_REG_FECHA} DESC, ${BaseDatos.COL_REG_ID} DESC
            LIMIT ?
        """.trimIndent()

        baseDatos.readableDatabase.rawQuery(sql, argsFiltro + limite.toString()).use { cursor ->
            leerFilas(cursor)
        }
    }

    /** Cláusula WHERE para excluir las simulaciones (vacía si se incluyen). */
    private fun filtroOrigen(incluirSimulaciones: Boolean): Pair<String, Array<String>> {
        return if (incluirSimulaciones) {
            "" to emptyArray()
        } else {
            "WHERE ${BaseDatos.COL_REG_ORIGEN} = ?" to arrayOf(OrigenLlamada.REAL.name)
        }
    }

    private fun leerFilas(cursor: Cursor): List<RegistroLlamada> {
        val iId = cursor.getColumnIndexOrThrow(BaseDatos.COL_REG_ID)
        val iFecha = cursor.getColumnIndexOrThrow(BaseDatos.COL_REG_FECHA)
        val iNumero = cursor.getColumnIndexOrThrow(BaseDatos.COL_REG_NUMERO)
        val iDecision = cursor.getColumnIndexOrThrow(BaseDatos.COL_REG_DECISION)
        val iReglaId = cursor.getColumnIndexOrThrow(BaseDatos.COL_REG_REGLA_ID)
        val iPatron = cursor.getColumnIndexOrThrow(BaseDatos.COL_REG_REGLA_PATRON)
        val iTipo = cursor.getColumnIndexOrThrow(BaseDatos.COL_REG_REGLA_TIPO)
        val iPrioridad = cursor.getColumnIndexOrThrow(BaseDatos.COL_REG_REGLA_PRIORIDAD)
        val iMotivo = cursor.getColumnIndexOrThrow(BaseDatos.COL_REG_MOTIVO)
        val iOrigen = cursor.getColumnIndexOrThrow(BaseDatos.COL_REG_ORIGEN)

        val filas = ArrayList<RegistroLlamada>(cursor.count)
        while (cursor.moveToNext()) {
            // Igual que con las reglas: una fila con valores desconocidos se
            // omite en vez de romper la pantalla.
            val decision = enumONull<Accion>(cursor.getString(iDecision)) ?: continue
            val origen = enumONull<OrigenLlamada>(cursor.getString(iOrigen)) ?: continue
            filas += RegistroLlamada(
                id = cursor.getLong(iId),
                fecha = cursor.getLong(iFecha),
                numeroEnmascarado = cursor.getString(iNumero),
                decision = decision,
                reglaId = if (cursor.isNull(iReglaId)) null else cursor.getLong(iReglaId),
                reglaPatron = if (cursor.isNull(iPatron)) null else cursor.getString(iPatron),
                reglaTipo = if (cursor.isNull(iTipo)) null else enumONull<TipoRegla>(cursor.getString(iTipo)),
                reglaPrioridad = if (cursor.isNull(iPrioridad)) null else cursor.getInt(iPrioridad),
                motivo = if (cursor.isNull(iMotivo)) null else cursor.getString(iMotivo),
                origen = origen
            )
        }
        return filas
    }

    private inline fun <reified E : Enum<E>> enumONull(nombre: String?): E? {
        if (nombre == null) return null
        return enumValues<E>().firstOrNull { it.name == nombre }
    }

    companion object {
        private const val MILIS_DIA = 24L * 60L * 60L * 1000L

        @Volatile
        private var instancia: RepositorioRegistro? = null

        /** Instancia única de producción; la BD sale SIEMPRE de [AlmacenDatos]. */
        fun obtener(contexto: Context): RepositorioRegistro {
            return instancia ?: synchronized(this) {
                instancia ?: RepositorioRegistro(AlmacenDatos.obtener(contexto)).also { instancia = it }
            }
        }
    }
}
