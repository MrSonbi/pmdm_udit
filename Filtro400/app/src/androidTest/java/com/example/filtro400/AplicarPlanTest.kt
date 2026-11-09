package com.example.filtro400

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.filtro400.csv.OperacionCsv
import com.example.filtro400.csv.PlanImportacion
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.data.RepositorioReglas
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de [RepositorioReglas.aplicarPlan] sobre SQLite real.
 *
 * El repositorio recibe una BaseDatos propia (`test_aplicarplan.db`) por
 * constructor: sin reflexión y sin tocar nunca `filtro400.db`.
 * Una instalación nueva trae sembrada la regla 400.
 */
@RunWith(AndroidJUnit4::class)
class AplicarPlanTest {

    private lateinit var contexto: Context
    private val nombreBd = "test_aplicarplan.db"
    private lateinit var baseDatos: BaseDatos
    private lateinit var repositorio: RepositorioReglas

    @Before
    fun preparar() {
        contexto = InstrumentationRegistry.getInstrumentation().targetContext
        contexto.deleteDatabase(nombreBd)
        baseDatos = BaseDatos(contexto, nombreBd)
        repositorio = RepositorioReglas(baseDatos)
    }

    @After
    fun limpiar() {
        try {
            baseDatos.close()
        } catch (_: Exception) {
        }
        contexto.deleteDatabase(nombreBd)
    }

    @Test
    fun usaSuPropiaBaseDeDatos() {
        assertEquals(nombreBd, baseDatos.databaseName)
        assertTrue(baseDatos.readableDatabase.path.endsWith(nombreBd))
    }

    @Test
    fun planCorrectoEscribeEnLaBdYRefrescaLaCache() {
        val regla400 = repositorio.listarTodas().single { it.patron == "400" }
        // Se carga la caché ANTES para comprobar que aplicarPlan la refresca.
        assertTrue(repositorio.reglasActivas().none { it.patron == "611" })

        val plan = PlanImportacion(
            listOf(
                OperacionCsv.Insertar(2, "611", "Nueva", TipoRegla.PREFIJO, Accion.BLOQUEAR, 0, true),
                OperacionCsv.Actualizar(3, regla400.id, "400", "Editada", TipoRegla.PREFIJO, Accion.BLOQUEAR, 5, true),
                OperacionCsv.Omitir(4, "duplicada")
            )
        )

        val resultado = repositorio.aplicarPlan(plan)

        assertFalse(resultado.fallo)
        assertEquals(1, resultado.anyadidas)
        assertEquals(1, resultado.actualizadas)
        assertEquals(1, resultado.omitidas)
        assertTrue(resultado.errores.isEmpty())

        // Las filas EXISTEN en la BD (listarTodas lee SQLite, no la caché).
        val enBd = repositorio.listarTodas()
        val nueva = enBd.singleOrNull { it.patron == "611" }
        assertNotNull(nueva)
        assertEquals("Nueva", nueva!!.etiqueta)
        val editada = enBd.single { it.id == regla400.id }
        assertEquals("Editada", editada.etiqueta)
        assertEquals(5, editada.prioridad)

        // Y la caché del servicio las ve.
        val activas = repositorio.reglasActivas()
        assertTrue(activas.any { it.patron == "611" })
        assertEquals(5, activas.single { it.id == regla400.id }.prioridad)
    }

    @Test
    fun fallaAMitadYHaceRollbackCompleto() {
        val antesEnBd = repositorio.listarTodas()
        val antesEnCache = repositorio.reglasActivas()

        // Un Insertar válido seguido de un Actualizar a un id inexistente:
        // el update afecta a 0 filas, se lanza excepción y debe deshacerse TODO.
        val plan = PlanImportacion(
            listOf(
                OperacionCsv.Insertar(2, "622", null, TipoRegla.PREFIJO, Accion.BLOQUEAR, 0, true),
                OperacionCsv.Actualizar(3, 99_999L, "633", null, TipoRegla.PREFIJO, Accion.BLOQUEAR, 0, true)
            )
        )

        val resultado = repositorio.aplicarPlan(plan)

        assertTrue(resultado.fallo)
        assertEquals("no se ha cambiado nada", resultado.mensaje)
        assertEquals(0, resultado.anyadidas)
        assertEquals(0, resultado.actualizadas)

        // La fila insertada NO existe: la transacción se deshizo.
        val despuesEnBd = repositorio.listarTodas()
        assertNull(despuesEnBd.firstOrNull { it.patron == "622" })
        assertEquals(antesEnBd, despuesEnBd)

        // La caché no cambia.
        assertEquals(antesEnCache, repositorio.reglasActivas())
    }
}
