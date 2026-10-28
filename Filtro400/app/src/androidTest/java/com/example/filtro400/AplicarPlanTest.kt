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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AplicarPlanTest {

    private lateinit var contexto: Context
    private val nombreBd = "test_aplicarplan.db"
    private lateinit var baseDatos: BaseDatos

    @Before
    fun preparar() {
        contexto = InstrumentationRegistry.getInstrumentation().targetContext
        contexto.deleteDatabase(nombreBd)
        baseDatos = BaseDatos(contexto, nombreBd)
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
    fun aplicarPlanExitoYRollback() {
        // ÉXITO: contadores correctos y fallo=false
        val repositorio = RepositorioReglas::class.java.getDeclaredConstructor(Context::class.java).apply { isAccessible = true }.newInstance(contexto.applicationContext)
        val field = RepositorioReglas::class.java.getDeclaredField("baseDatos")
        field.isAccessible = true
        field.set(repositorio, baseDatos)
        val planExito = com.example.filtro400.csv.PlanImportacion(
            listOf(
                com.example.filtro400.csv.OperacionCsv.Insertar(1, "611", null, com.example.filtro400.modelo.TipoRegla.PREFIJO, com.example.filtro400.modelo.Accion.BLOQUEAR, 0, true)
            )
        )
        val resExito = repositorio.aplicarPlan(planExito)
        assertEquals(false, resExito.fallo)
        assertEquals(1, resExito.anyadidas)
        assertEquals(0, resExito.actualizadas)
        assertEquals(0, resExito.omitidas)

        // ROLLBACK: update con 0 filas afectadas debe provocar rollback
        val planRollback = com.example.filtro400.csv.PlanImportacion(
            listOf(
                com.example.filtro400.csv.OperacionCsv.Actualizar(2, 99999, "622", null, com.example.filtro400.modelo.TipoRegla.PREFIJO, com.example.filtro400.modelo.Accion.BLOQUEAR, 0, true)
            )
        )
        val resRollback = repositorio.aplicarPlan(planRollback)
        assertEquals(true, resRollback.fallo)
        assertEquals("no se ha cambiado nada", resRollback.mensaje)
    }
}
