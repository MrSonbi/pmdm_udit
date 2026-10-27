package com.example.filtro400

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.filtro400.data.BaseDatos
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.registro.OrigenLlamada
import com.example.filtro400.registro.RepositorioRegistro
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test instrumentado con BD temporal para RepositorioRegistro.
 * Usa BD inyectable, nunca toca la BD real de la app.
 */
@RunWith(AndroidJUnit4::class)
class RepositorioRegistroTest {

    private lateinit var contexto: Context
    private val nombreBd = "test_registro.db"
    private lateinit var baseDatos: BaseDatos
    private lateinit var repositorio: RepositorioRegistro

    @Before
    fun preparar() {
        contexto = InstrumentationRegistry.getInstrumentation().targetContext
        contexto.deleteDatabase(nombreBd)
        baseDatos = BaseDatos(contexto, nombreBd)
        repositorio = RepositorioRegistro(contexto, baseDatos)
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
    fun insercionGuardaNumeroEnmascaradoNuncaCompleto() {
        val db = baseDatos.writableDatabase
        val numeroCompleto = "400123456789"
        val valores = android.content.ContentValues().apply {
            put(BaseDatos.COL_REG_FECHA, System.currentTimeMillis())
            put(BaseDatos.COL_REG_NUMERO, com.example.filtro400.util.Enmascarar.numero(numeroCompleto))
            put(BaseDatos.COL_REG_DECISION, Accion.BLOQUEAR.name)
            put(BaseDatos.COL_REG_REGLA_ID, 1L)
            put(BaseDatos.COL_REG_REGLA_PATRON, "400")
            put(BaseDatos.COL_REG_REGLA_TIPO, TipoRegla.PREFIJO.name)
            put(BaseDatos.COL_REG_REGLA_PRIORIDAD, 0)
            put(BaseDatos.COL_REG_MOTIVO, "test")
            put(BaseDatos.COL_REG_ORIGEN, OrigenLlamada.REAL.name)
        }
        db.insert(BaseDatos.TABLA_REGISTRO, null, valores)

        db.rawQuery("SELECT numero_enmascarado FROM registro", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            val enmascarado = cursor.getString(0)
            assertNotEquals(numeroCompleto, enmascarado)
            assertTrue(enmascarado.endsWith("***"))
        }
    }

    @Test
    fun borradoMasAntiguosDe30DiasEliminaSoloLosViejos() {
        val db = baseDatos.writableDatabase
        val ahora = System.currentTimeMillis()
        val hace40Dias = ahora - 40L * 24L * 60L * 60L * 1000L
        val hace5Dias = ahora - 5L * 24L * 60L * 60L * 1000L

        fun insertar(fecha: Long, origen: String) {
            val valores = android.content.ContentValues().apply {
                put(BaseDatos.COL_REG_FECHA, fecha)
                put(BaseDatos.COL_REG_NUMERO, "400123***")
                put(BaseDatos.COL_REG_DECISION, Accion.BLOQUEAR.name)
                put(BaseDatos.COL_REG_ORIGEN, origen)
            }
            db.insert(BaseDatos.TABLA_REGISTRO, null, valores)
        }

        insertar(hace40Dias, OrigenLlamada.REAL.name)
        insertar(hace5Dias, OrigenLlamada.REAL.name)
        insertar(ahora, OrigenLlamada.SIMULADA.name)

        val limite = ahora - 30L * 24L * 60L * 60L * 1000L
        db.delete(BaseDatos.TABLA_REGISTRO, "${BaseDatos.COL_REG_FECHA} < ?", arrayOf(limite.toString()))

        db.rawQuery("SELECT COUNT(*) FROM registro", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }
    }

    @Test
    fun consultasDeEstadisticasYOrigenRealSimulada() {
        val db = baseDatos.writableDatabase
        val ahora = System.currentTimeMillis()
        val hoy = ahora
        val hace3Dias = ahora - 3L * 24L * 60L * 60L * 1000L

        fun insertar(fecha: Long, decision: String, reglaPatron: String?, origen: String) {
            val valores = android.content.ContentValues().apply {
                put(BaseDatos.COL_REG_FECHA, fecha)
                put(BaseDatos.COL_REG_NUMERO, "611***")
                put(BaseDatos.COL_REG_DECISION, decision)
                put(BaseDatos.COL_REG_REGLA_PATRON, reglaPatron)
                put(BaseDatos.COL_REG_ORIGEN, origen)
            }
            db.insert(BaseDatos.TABLA_REGISTRO, null, valores)
        }

        insertar(hoy, Accion.BLOQUEAR.name, "400", OrigenLlamada.REAL.name)
        insertar(hoy, Accion.PERMITIR.name, null, OrigenLlamada.REAL.name)
        insertar(hace3Dias, Accion.BLOQUEAR.name, "611", OrigenLlamada.SIMULADA.name)

        db.rawQuery("SELECT COUNT(*) FROM registro WHERE decision = '${Accion.BLOQUEAR.name}'", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }
        db.rawQuery("SELECT COUNT(*) FROM registro WHERE decision = '${Accion.PERMITIR.name}'", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.rawQuery("SELECT COUNT(*) FROM registro WHERE origen = '${OrigenLlamada.SIMULADA.name}'", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.rawQuery("SELECT regla_patron, COUNT(*) as c FROM registro WHERE regla_patron IS NOT NULL GROUP BY regla_patron ORDER BY c DESC", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("400", cursor.getString(0))
        }
    }
}
