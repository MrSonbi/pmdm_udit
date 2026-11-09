package com.example.filtro400.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.ui.tema.ColoresNeu
import com.example.filtro400.ui.tema.NeuBoton
import com.example.filtro400.ui.tema.NeuInterruptor
import com.example.filtro400.ui.tema.NeuTarjeta
import com.example.filtro400.ui.tema.SelectorSegmentado

/**
 * Pestaña Reglas (estilo neumórfico).
 *
 * Una sola acción principal: el botón "Añadir regla", fijo abajo y a todo el
 * ancho. Arriba, el título, una frase corta y el selector Todas / Bloqueos /
 * Lista blanca. Cada regla es una tarjeta: pulsarla abre la edición (y desde
 * la edición se puede eliminar), así no hay botones sueltos en la tarjeta.
 *
 * Esta pestaña no toca la BD: recibe la lista y avisa con callbacks a
 * [PantallaInicio], que hace el trabajo en Dispatchers.IO.
 */
@Composable
fun PestanaReglas(
    reglas: List<Regla>,
    cargando: Boolean,
    mensajeError: String?,
    alCambiarActiva: (Regla, Boolean) -> Unit,
    alEditar: (Regla) -> Unit,
    alAnadir: () -> Unit,
    contentPadding: PaddingValues
) {
    var filtro by rememberSaveable { mutableStateOf(FiltroReglas.TODAS) }
    val visibles = reglas.filter { filtro.acepta(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        LazyColumn(
            // weight(1f): la lista ocupa el hueco que deja el botón fijo de abajo.
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            // Margen generoso: las sombras neumórficas salen de las tarjetas.
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Reglas",
                        style = MaterialTheme.typography.titleLarge,
                        color = ColoresNeu.texto
                    )
                    Text(
                        text = "Decide qué llamadas se bloquean y cuáles entran siempre.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColoresNeu.textoSecundario
                    )
                }
            }
            item {
                SelectorSegmentado(
                    opciones = FiltroReglas.entries.map { it.texto },
                    seleccionada = filtro.ordinal,
                    alSeleccionar = { filtro = FiltroReglas.entries[it] }
                )
            }
            if (mensajeError != null) {
                item {
                    NeuTarjeta {
                        Text(
                            text = mensajeError,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ColoresNeu.bloqueo
                        )
                    }
                }
            }
            when {
                cargando -> item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = ColoresNeu.acento
                        )
                    }
                }

                visibles.isEmpty() && mensajeError == null -> item {
                    // Lista vacía: una frase amable. El botón "Añadir regla" es el
                    // mismo de siempre, fijo abajo.
                    Text(
                        text = textoReglasVacias(filtro, hayReglas = reglas.isNotEmpty()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColoresNeu.texto,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }

                else -> items(visibles, key = { it.id }) { regla ->
                    TarjetaRegla(
                        regla = regla,
                        alCambiarActiva = { activa -> alCambiarActiva(regla, activa) },
                        alEditar = { alEditar(regla) }
                    )
                }
            }
        }

        // Acción principal de la pantalla, siempre visible.
        NeuBoton(
            texto = "Añadir regla",
            alPulsar = alAnadir,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
        )
    }
}

/**
 * Tarjeta de una regla.
 *
 * Una sola Row: a la izquierda una Column con weight(1f) (patrón, frase y
 * etiqueta: pueden partirse en varias líneas, nunca se salen); a la derecha
 * solo el interruptor con su estado escrito ("Activa" / "Pausada"), que es
 * corto. Sin botones sueltos y sin alturas fijas: el desbordamiento de la
 * tarjeta antigua no puede repetirse.
 */
@Composable
private fun TarjetaRegla(
    regla: Regla,
    alCambiarActiva: (Boolean) -> Unit,
    alEditar: () -> Unit
) {
    val frase = fraseRegla(regla)
    NeuTarjeta(alPulsar = alEditar, descripcionAccion = "Editar la regla") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = regla.patron,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (regla.activa) ColoresNeu.texto else ColoresNeu.textoSecundario
                )
                Text(
                    text = frase,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (regla.accion == Accion.BLOQUEAR) ColoresNeu.bloqueo else ColoresNeu.permitir
                )
                regla.etiqueta?.takeIf { it.isNotBlank() }?.let { etiqueta ->
                    Text(
                        text = etiqueta,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColoresNeu.textoSecundario
                    )
                }
            }
            NeuInterruptor(
                activo = regla.activa,
                alCambiar = alCambiarActiva,
                textoActivo = textoEstadoRegla(true),
                textoInactivo = textoEstadoRegla(false),
                descripcion = frase
            )
        }
    }
}
