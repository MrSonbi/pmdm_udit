package com.example.filtro400.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.example.filtro400.registro.RepositorioRegistro
import com.example.filtro400.ui.tema.FilaInterruptor
import com.example.filtro400.ui.tema.NeuTarjeta
import com.example.filtro400.ui.tema.SelectorSegmentado

/** Vistas de la pestaña Actividad, en el orden del selector. */
private val VISTAS_ACTIVIDAD = listOf("Registro", "Estadísticas")

/**
 * Pestaña Actividad: junta Registro y Estadísticas.
 *
 * Arriba, el selector "Registro | Estadísticas" y UN solo interruptor
 * "Mostrar pruebas (simulaciones)". Los dos estados ([vista] y
 * [mostrarPruebas]) viven en [PantallaInicio] con rememberSaveable: así se
 * conservan al cambiar de pestaña o girar el móvil, y las dos vistas cuentan
 * siempre lo mismo. Apagado por defecto: solo cuentan las llamadas reales.
 *
 * La cabecera se pasa a cada vista como primeros elementos de SU LazyColumn:
 * toda la pantalla se desplaza junta (nada queda fijo ocupando sitio).
 */
@Composable
fun PestanaActividad(
    repositorioRegistro: RepositorioRegistro,
    refresco: Int,
    guardarRegistro: Boolean,
    vista: Int,
    alCambiarVista: (Int) -> Unit,
    mostrarPruebas: Boolean,
    alCambiarMostrarPruebas: (Boolean) -> Unit,
    reglasQueBloquean: Set<String>,
    contentPadding: PaddingValues
) {
    val cabecera: LazyListScope.() -> Unit = {
        item {
            SelectorSegmentado(
                opciones = VISTAS_ACTIVIDAD,
                seleccionada = vista,
                alSeleccionar = alCambiarVista
            )
        }
        item {
            NeuTarjeta {
                FilaInterruptor(
                    titulo = "Mostrar pruebas (simulaciones)",
                    explicacion = "Por defecto solo cuentan las llamadas reales.",
                    activo = mostrarPruebas,
                    alCambiar = alCambiarMostrarPruebas,
                    textoActivo = "Sí",
                    textoInactivo = "No"
                )
            }
        }
    }

    if (vista == 0) {
        PestanaRegistro(
            repositorioRegistro = repositorioRegistro,
            refresco = refresco,
            guardarRegistro = guardarRegistro,
            incluirSimulaciones = mostrarPruebas,
            cabecera = cabecera,
            contentPadding = contentPadding
        )
    } else {
        PestanaEstadisticas(
            repositorioRegistro = repositorioRegistro,
            refresco = refresco,
            incluirSimulaciones = mostrarPruebas,
            reglasQueBloquean = reglasQueBloquean,
            cabecera = cabecera,
            contentPadding = contentPadding
        )
    }
}
