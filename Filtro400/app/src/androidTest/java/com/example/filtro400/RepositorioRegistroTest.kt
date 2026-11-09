package com.example.filtro400

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.registro.OrigenLlamada
import com.example.filtro400.registro.RepositorioRegistro
import com.example.filtro400.registro.RepositorioRegistro.Estadisticas
import com.example.filtro400.registro.RepositorioRegistro.UsoRegla
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Prueba del CÓDIGO DE PRODUCCIÓN de [RepositorioRegistro] con SQLite real.
 *
 * Las filas se siembran con `insertarSincrono` (pasando la fecha cuando hace
 * falta una fila antigua) y se consultan con los métodos del repositorio,
 * no con SQL escrito a mano. La BD es `test_registro.db`, nunca `filtro400.db`.
 */
@RunWith(AndroidJUnit4::class)
class RepositorioRegistroTest {

    private lateinit var contexto: Context
    private val nombreBd = "test_registro.db"
    private lateinit var baseDatos: BaseDatos
    private lateinit var repositorio: RepositorioRegistro

    // Instante y zona FIJOS para que "hoy" y "hace N días" no dependan ni del
    // reloj ni de la zona del dispositivo: lunes 09/11/2026 a las 12:00 en Madrid.
    private val zona: ZoneId = ZoneId.of("Europe/Madrid")
    private val ahora = ZonedDateTime.of(2026, 11, 9, 12, 0, 0, 0, zona).toInstant().toEpochMilli()
    private val dia = 24L * 60L * 60L * 1000L

    /** Instante (ms) de una fecha y hora LOCAL en [zona]. */
    private fun local(anyo: Int, mes: Int, diaMes: Int, h: Int, min: Int): Long =
        ZonedDateTime.of(anyo, mes, diaMes, h, min, 0, 0, zona).toInstant().toEpochMilli()

    @Before
    fun preparar() {
        contexto = InstrumentationRegistry.getInstrumentation().targetContext
        contexto.deleteDatabase(nombreBd)
        baseDatos = BaseDatos(contexto, nombreBd)
        repositorio = RepositorioRegistro(baseDatos)
    }

    @After
    fun limpiar() {
        try {
            baseDatos.close()
        } catch (_: Exception) {
        }
        contexto.deleteDatabase(nombreBd)
    }

    /** Atajo para sembrar una fila con el método real del repositorio. */
    private fun registrar(
        fecha: Long,
        decision: Accion,
        patron: String?,
        origen: OrigenLlamada = OrigenLlamada.REAL,
        numero: String = "611000000"
    ): Long {
        return repositorio.insertarSincrono(
            numero = numero,
            decision = decision,
            reglaId = null,
            reglaPatron = patron,
            reglaTipo = patron?.let { TipoRegla.PREFIJO },
            reglaPrioridad = patron?.let { 0 },
            motivo = "test",
            origen = origen,
            fecha = fecha
        )
    }

    @Test
    fun usaSuPropiaBaseDeDatos() {
        assertEquals(nombreBd, baseDatos.databaseName)
        assertTrue(baseDatos.readableDatabase.path.endsWith(nombreBd))
    }

    // ---------- a) inserción enmascarada, borrado y recuento ----------

    @Test
    fun insertarSincronoGuardaElNumeroEnmascarado() = runBlocking<Unit> {
        // Número ficticio completo, escrito como lo entregaría Android.
        val numeroCompleto = "+34 611 223 344"
        val normalizado = ReglaBloqueo.normalizar(numeroCompleto) // "611223344"

        val id = repositorio.insertarSincrono(
            numero = numeroCompleto,
            decision = Accion.BLOQUEAR,
            reglaId = 1L,
            reglaPatron = "611",
            reglaTipo = TipoRegla.PREFIJO,
            reglaPrioridad = 0,
            motivo = "test",
            origen = OrigenLlamada.REAL
        )
        assertTrue(id > 0)

        val fila = repositorio.listarRecientes().single()
        assertNotEquals(numeroCompleto, fila.numeroEnmascarado)
        assertNotEquals(normalizado, fila.numeroEnmascarado)
        assertFalse(fila.numeroEnmascarado.contains(normalizado))
        assertTrue(fila.numeroEnmascarado.endsWith("***"))
        assertEquals("611223***", fila.numeroEnmascarado)
    }

    @Test
    fun borrarMasAntiguosDe30DiasEliminaSoloLosViejos() = runBlocking<Unit> {
        registrar(ahora - 40 * dia, Accion.BLOQUEAR, "400")
        registrar(ahora - 31 * dia, Accion.BLOQUEAR, "400", OrigenLlamada.SIMULADA)
        registrar(ahora - 5 * dia, Accion.BLOQUEAR, "400")
        registrar(ahora, Accion.PERMITIR, null, OrigenLlamada.SIMULADA)
        assertEquals(4, repositorio.contar())

        val borradas = repositorio.borrarMasAntiguosDe(30, ahora)

        assertEquals(2, borradas)
        assertEquals(2, repositorio.contar())
    }

    @Test
    fun contarIncluyeRealesYSimuladas() = runBlocking<Unit> {
        assertEquals(0, repositorio.contar())
        registrar(ahora, Accion.BLOQUEAR, "400")
        registrar(ahora, Accion.BLOQUEAR, "400", OrigenLlamada.SIMULADA)
        assertEquals(2, repositorio.contar())
        repositorio.borrarTodos()
        assertEquals(0, repositorio.contar())
    }

    // ---------- b) estadísticas, ranking y listado ----------

    @Test
    fun estadisticasConTablaVaciaSonCero() = runBlocking<Unit> {
        assertEquals(Estadisticas.VACIAS, repositorio.estadisticas(ahora = ahora, zona = zona))
        assertTrue(repositorio.rankingReglas().isEmpty())
        assertTrue(repositorio.listarRecientes().isEmpty())
    }

    @Test
    fun estadisticasYRankingSinEmpate() = runBlocking<Unit> {
        // Reales: 400 dos veces, 611 una vez, dos permitidas sin regla.
        registrar(ahora, Accion.BLOQUEAR, "400")
        registrar(ahora - 3 * dia, Accion.BLOQUEAR, "400")
        registrar(ahora, Accion.BLOQUEAR, "611")
        registrar(ahora, Accion.PERMITIR, null)
        registrar(ahora - 10 * dia, Accion.PERMITIR, null)
        // Simuladas: 611 dos veces más (solo cuentan si se piden).
        registrar(ahora, Accion.BLOQUEAR, "611", OrigenLlamada.SIMULADA)
        registrar(ahora, Accion.BLOQUEAR, "611", OrigenLlamada.SIMULADA)

        // Por defecto se excluyen las simulaciones.
        // Hoy: 2 bloqueadas (400 y 611) y 1 permitida. 7 días: + el 400 de hace 3 días.
        assertEquals(
            Estadisticas(
                total = 5, bloqueadas = 3, permitidas = 2,
                bloqueadasHoy = 2, permitidasHoy = 1,
                bloqueadas7Dias = 3, permitidas7Dias = 1
            ),
            repositorio.estadisticas(ahora = ahora, zona = zona)
        )
        assertEquals(
            listOf(UsoRegla("400", 2), UsoRegla("611", 1)),
            repositorio.rankingReglas()
        )

        // Con simulaciones las cifras cambian y el 611 pasa delante.
        assertEquals(
            Estadisticas(
                total = 7, bloqueadas = 5, permitidas = 2,
                bloqueadasHoy = 4, permitidasHoy = 1,
                bloqueadas7Dias = 5, permitidas7Dias = 1
            ),
            repositorio.estadisticas(incluirSimulaciones = true, ahora = ahora, zona = zona)
        )
        assertEquals(
            listOf(UsoRegla("611", 3), UsoRegla("400", 2)),
            repositorio.rankingReglas(incluirSimulaciones = true)
        )
    }

    @Test
    fun hoyEmpiezaALas00LocalesYSieteDiasSonDiasDeCalendario() = runBlocking<Unit> {
        // ahora = 09/11/2026 12:00 (Madrid).
        registrar(local(2026, 11, 9, 0, 0), Accion.BLOQUEAR, "400")      // 00:00 de hoy -> hoy
        registrar(local(2026, 11, 9, 0, 0) - 1, Accion.BLOQUEAR, "400")  // 23:59:59.999 de ayer:
                                                                         // dentro de las "últimas 24 h", pero NO es hoy
        registrar(local(2026, 11, 3, 0, 0), Accion.PERMITIR, null)       // 00:00 de hace 6 días -> 7 días
        registrar(local(2026, 11, 3, 0, 0) - 1, Accion.PERMITIR, null)   // 02/11 23:59: fuera de 7 días aunque
                                                                         // sea posterior a "ahora - 7 × 24 h"
        assertTrue(local(2026, 11, 3, 0, 0) - 1 > ahora - 7 * dia)

        assertEquals(
            Estadisticas(
                total = 4, bloqueadas = 2, permitidas = 2,
                bloqueadasHoy = 1, permitidasHoy = 0,
                bloqueadas7Dias = 2, permitidas7Dias = 1
            ),
            repositorio.estadisticas(ahora = ahora, zona = zona)
        )
    }

    @Test
    fun rankingConEmpateOrdenaPorPatron() = runBlocking<Unit> {
        // Se insertan en orden "desfavorable" (611 antes que 400) a propósito:
        // el desempate debe ser alfabético, no por orden de inserción.
        registrar(ahora, Accion.BLOQUEAR, "611")
        registrar(ahora, Accion.BLOQUEAR, "611")
        registrar(ahora, Accion.BLOQUEAR, "500")
        registrar(ahora, Accion.BLOQUEAR, "400")
        registrar(ahora, Accion.BLOQUEAR, "400")

        assertEquals(
            listOf(UsoRegla("400", 2), UsoRegla("611", 2), UsoRegla("500", 1)),
            repositorio.rankingReglas()
        )
        assertEquals(
            listOf(UsoRegla("400", 2), UsoRegla("611", 2)),
            repositorio.rankingReglas(limite = 2)
        )
    }

    @Test
    fun listadoRecienteOrdenadoYSinSimulacionesPorDefecto() = runBlocking<Unit> {
        val idViejo = registrar(ahora - 2 * dia, Accion.BLOQUEAR, "400")
        val idNuevo = registrar(ahora - 1 * dia, Accion.PERMITIR, null)
        val idSimulado = registrar(ahora, Accion.BLOQUEAR, "611", OrigenLlamada.SIMULADA)

        assertEquals(listOf(idNuevo, idViejo), repositorio.listarRecientes().map { it.id })
        assertEquals(
            listOf(idSimulado, idNuevo, idViejo),
            repositorio.listarRecientes(incluirSimulaciones = true).map { it.id }
        )
        assertEquals(listOf(idNuevo), repositorio.listarRecientes(limite = 1).map { it.id })

        val nuevo = repositorio.listarRecientes().first()
        assertEquals(Accion.PERMITIR, nuevo.decision)
        assertEquals(OrigenLlamada.REAL, nuevo.origen)
        assertEquals(null, nuevo.reglaPatron)
    }

    // ---------- c) simulaciones guardadas desde el Simulador (paso 9) ----------

    @Test
    fun filaSimuladaSoloCuentaConElInterruptorEncendido() = runBlocking<Unit> {
        // Igual que el Simulador: se inserta con insertar() (no insertarSincrono)
        // y origen SIMULADA. La fecha es "ahora" del reloj real, así que las
        // estadísticas se piden con el reloj y la zona por defecto.
        val numeroCompleto = "+34 400 555 123"
        repositorio.insertar(
            numero = numeroCompleto,
            decision = Accion.BLOQUEAR,
            reglaId = 1L,
            reglaPatron = "400",
            reglaTipo = TipoRegla.PREFIJO,
            reglaPrioridad = 0,
            motivo = "Bloqueado por regla «400»",
            origen = OrigenLlamada.SIMULADA
        )

        // Interruptor APAGADO (valor por defecto): no cuenta en nada.
        assertEquals(Estadisticas.VACIAS, repositorio.estadisticas(incluirSimulaciones = false))
        assertTrue(repositorio.rankingReglas(incluirSimulaciones = false).isEmpty())
        assertTrue(repositorio.listarRecientes(incluirSimulaciones = false).isEmpty())

        // Interruptor ENCENDIDO: cuenta como bloqueada hoy, en 7 días y en total.
        assertEquals(
            Estadisticas(
                total = 1, bloqueadas = 1, permitidas = 0,
                bloqueadasHoy = 1, permitidasHoy = 0,
                bloqueadas7Dias = 1, permitidas7Dias = 0
            ),
            repositorio.estadisticas(incluirSimulaciones = true)
        )
        assertEquals(listOf(UsoRegla("400", 1)), repositorio.rankingReglas(incluirSimulaciones = true))

        val fila = repositorio.listarRecientes(incluirSimulaciones = true).single()
        assertEquals(OrigenLlamada.SIMULADA, fila.origen)
        // También enmascarada: insertar() pasa por Enmascarar.numero.
        assertEquals("400555***", fila.numeroEnmascarado)
        assertFalse(fila.numeroEnmascarado.contains(ReglaBloqueo.normalizar(numeroCompleto)))
    }
}
