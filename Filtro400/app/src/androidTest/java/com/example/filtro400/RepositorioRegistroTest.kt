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

    // Instante fijo para que "hoy" y "hace N días" no dependan de la hora del test.
    private val ahora = System.currentTimeMillis()
    private val dia = 24L * 60L * 60L * 1000L

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
        assertEquals(Estadisticas(0, 0, 0, 0, 0), repositorio.estadisticas(ahora = ahora))
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
        assertEquals(
            Estadisticas(total = 5, bloqueadas = 3, permitidas = 2, hoy = 3, ultimos7Dias = 4),
            repositorio.estadisticas(ahora = ahora)
        )
        assertEquals(
            listOf(UsoRegla("400", 2), UsoRegla("611", 1)),
            repositorio.rankingReglas()
        )

        // Con simulaciones las cifras cambian y el 611 pasa delante.
        assertEquals(
            Estadisticas(total = 7, bloqueadas = 5, permitidas = 2, hoy = 5, ultimos7Dias = 6),
            repositorio.estadisticas(incluirSimulaciones = true, ahora = ahora)
        )
        assertEquals(
            listOf(UsoRegla("611", 3), UsoRegla("400", 2)),
            repositorio.rankingReglas(incluirSimulaciones = true)
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
}
