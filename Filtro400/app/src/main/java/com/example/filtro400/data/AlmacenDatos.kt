package com.example.filtro400.data

import android.content.Context

/**
 * ÚNICO sitio donde vive la base de datos real de la app (`filtro400.db`).
 *
 * Todo el código de producción (repositorios, servicio, pantallas) pide la BD
 * aquí. Así hay una sola conexión compartida por el proceso y nunca se abren
 * dos SQLiteOpenHelper distintos sobre el mismo fichero.
 *
 * Por qué no lo hace la propia [BaseDatos]: si el singleton estuviera escondido
 * dentro del constructor, cualquier `BaseDatos(contexto, "otro.db")` acabaría
 * devolviendo la primera BD abierta del proceso (es justo el fallo que hacía
 * que los tests instrumentados se pisaran). Separando el "singleton" del
 * "helper", el helper es predecible y los tests crean el suyo con otro nombre.
 */
object AlmacenDatos {

    // @Volatile: todos los hilos (UI, IO, servicio) ven la misma instancia.
    @Volatile
    private var baseDatos: BaseDatos? = null

    /** Devuelve la BD real, creándola la primera vez con el contexto de la aplicación. */
    fun obtener(contexto: Context): BaseDatos {
        return baseDatos ?: synchronized(this) {
            baseDatos ?: BaseDatos(contexto.applicationContext, BaseDatos.NOMBRE_BD)
                .also { baseDatos = it }
        }
    }
}
