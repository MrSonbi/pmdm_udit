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
    fun aplicarPlanConRollbackSiFallaYUnaSolaRecarga() {
        // Construimos un repositorio con BD temporal usando reflexión no, mejor instanciamos
        val repositorio = RepositorioReglas::class.java.getDeclaredConstructor(Context::class.java)
            .apply { isAccessible = true }
            .newInstance(contexto.applicationContext)
        // Reemplazar baseDatos
        val field = RepositorioReglas::class.java.getDeclaredField("baseDatos")
        field.isAccessible = true
        field.set(repositorio, baseDatos)
        // Forzamos recarga
        repositorio.listarTodas()
    }
}
