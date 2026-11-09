package com.example.filtro400

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.data.RepositorioReglas
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.registro.OrigenLlamada
import com.example.filtro400.registro.RepositorioRegistro
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
 * Test de REGRESIÓN del singleton oculto que tenía BaseDatos.
 *
 * Antes, la segunda `BaseDatos(contexto, "otro.db")` del proceso ignoraba el
 * nombre y abría el fichero de la primera. Aquí se comprueba que dos
 * BaseDatos con nombres distintos son dos ficheros distintos y que lo que se
 * escribe en una NO aparece en la otra.
 */
@RunWith(AndroidJUnit4::class)
class AislamientoBaseDatosTest {

    private lateinit var contexto: Context
    private val nombreA = "test_aislamiento_a.db"
    private val nombreB = "test_aislamiento_b.db"
    private lateinit var bdA: BaseDatos
    private lateinit var bdB: BaseDatos

    @Before
    fun preparar() {
        contexto = InstrumentationRegistry.getInstrumentation().targetContext
        contexto.deleteDatabase(nombreA)
        contexto.deleteDatabase(nombreB)
        bdA = BaseDatos(contexto, nombreA)
        bdB = BaseDatos(contexto, nombreB)
    }

    @After
    fun limpiar() {
        runCatching { bdA.close() }
        runCatching { bdB.close() }
        contexto.deleteDatabase(nombreA)
        contexto.deleteDatabase(nombreB)
    }

    @Test
    fun dosNombresSonDosFicherosDistintos() {
        assertEquals(nombreA, bdA.databaseName)
        assertEquals(nombreB, bdB.databaseName)

        val rutaA = bdA.writableDatabase.path
        val rutaB = bdB.writableDatabase.path
        assertNotEquals(rutaA, rutaB)
        assertEquals(contexto.getDatabasePath(nombreA).absolutePath, rutaA)
        assertEquals(contexto.getDatabasePath(nombreB).absolutePath, rutaB)

        // Ninguna de las dos es la BD real de la app.
        assertFalse(rutaA.endsWith(BaseDatos.NOMBRE_BD))
        assertFalse(rutaB.endsWith(BaseDatos.NOMBRE_BD))
    }

    @Test
    fun unaReglaEscritaEnANoApareceEnB() {
        val reglasA = RepositorioReglas(bdA)
        val reglasB = RepositorioReglas(bdB)

        reglasA.insertar("699", "solo en A", TipoRegla.PREFIJO, Accion.BLOQUEAR, null)

        assertTrue(reglasA.listarTodas().any { it.patron == "699" })
        assertFalse(reglasB.listarTodas().any { it.patron == "699" })
        assertFalse(reglasB.reglasActivas().any { it.patron == "699" })
    }

    @Test
    fun unRegistroEscritoEnANoApareceEnB() = runBlocking<Unit> {
        val registroA = RepositorioRegistro(bdA)
        val registroB = RepositorioRegistro(bdB)

        registroA.insertarSincrono(
            numero = "611000000",
            decision = Accion.BLOQUEAR,
            reglaId = null,
            reglaPatron = "611",
            reglaTipo = TipoRegla.PREFIJO,
            reglaPrioridad = 0,
            motivo = "test",
            origen = OrigenLlamada.REAL
        )

        assertEquals(1, registroA.contar())
        assertEquals(0, registroB.contar())
    }
}
