package com.example.filtro400.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.filtro400.registro.RepositorioRegistro
import com.example.filtro400.ui.tema.ColoresNeu
import com.example.filtro400.ui.tema.NeuCargando
import com.example.filtro400.ui.tema.NeuTarjeta
import com.example.filtro400.ui.tema.neuHundido

/** Cuántas reglas se piden al repositorio antes de quedarnos con las de bloqueo. */
private const val CONSULTA_RANKING = 50

/** Cuántas reglas enseña el ranking "Reglas que más bloquean". */
private const val TOP_REGLAS = 5

/**
 * Vista Estadísticas (dentro de Actividad). Usa los métodos ya probados de
 * [RepositorioRegistro] (en Dispatchers.IO):
 *  - [RepositorioRegistro.estadisticas]: bloqueadas y permitidas de hoy y de
 *    los últimos 7 días; "hoy" empieza a las 00:00 de la zona local;
 *  - [RepositorioRegistro.rankingReglas]: reglas que más han decidido. Aquí
 *    solo se enseñan las que hoy son de BLOQUEO ([reglasQueBloquean]), porque
 *    la tarjeta es "Reglas que más bloquean".
 *
 * El interruptor "Mostrar pruebas" (en Actividad) llega como
 * [incluirSimulaciones]. [cabecera] va al principio de la lista desplazable.
 */
@Composable
fun PestanaEstadisticas(
    repositorioRegistro: RepositorioRegistro,
    refresco: Int,
    incluirSimulaciones: Boolean,
    reglasQueBloquean: Set<String>,
    cabecera: LazyListScope.() -> Unit,
    contentPadding: PaddingValues
) {
    var estadisticas by remember { mutableStateOf<RepositorioRegistro.Estadisticas?>(null) }
    var ranking by remember { mutableStateOf<List<RepositorioRegistro.UsoRegla>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Se recalcula al cambiar el interruptor y cada vez que llega un refresco.
    LaunchedEffect(refresco, incluirSimulaciones) {
        cargando = true
        val resultado = runCatching {
            val totales = repositorioRegistro.estadisticas(incluirSimulaciones = incluirSimulaciones)
            val top = repositorioRegistro.rankingReglas(
                incluirSimulaciones = incluirSimulaciones,
                limite = CONSULTA_RANKING
            )
            totales to top
        }
        cargando = false
        resultado.onSuccess { (totales, top) ->
            estadisticas = totales
            ranking = top
            error = null
        }.onFailure {
            error = "No se pudieron calcular las estadísticas. Vuelve a intentarlo más tarde."
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = MARGEN_LISTA,
        verticalArrangement = Arrangement.spacedBy(SEPARACION_LISTA)
    ) {
        cabecera()
        item { TituloVista(titulo = "Estadísticas") }

        val datos = estadisticas
        when {
            cargando && datos == null -> item { NeuCargando() }

            error != null -> item { TarjetaAviso(error.orEmpty(), esError = true) }

            datos == null || datos.total == 0 -> item {
                TarjetaAviso(
                    "Todavía no hay datos. Cuando la app filtre alguna llamada, " +
                        "aquí verás un resumen sencillo."
                )
            }

            else -> {
                // Bloqueadas y permitidas SIEMPRE por separado y con texto.
                item {
                    NeuTarjeta {
                        TituloTarjeta("Hoy")
                        Cifra("Bloqueadas hoy", datos.bloqueadasHoy, bloqueo = true)
                        Cifra("Permitidas hoy", datos.permitidasHoy, bloqueo = false)
                    }
                }
                item {
                    NeuTarjeta {
                        TituloTarjeta("Últimos 7 días")
                        Cifra("Bloqueadas", datos.bloqueadas7Dias, bloqueo = true)
                        Cifra("Permitidas", datos.permitidas7Dias, bloqueo = false)
                    }
                }
                item {
                    val top = ranking.filter { it.patron in reglasQueBloquean }.take(TOP_REGLAS)
                    NeuTarjeta {
                        TituloTarjeta("Reglas que más bloquean")
                        if (top.isEmpty()) {
                            Text(
                                text = "Ninguna regla ha bloqueado llamadas todavía.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ColoresNeu.texto
                            )
                        } else {
                            val maximo = top.maxOf { it.veces }.coerceAtLeast(1)
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                top.forEach { uso -> BarraRanking(uso, maximo) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TituloTarjeta(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.titleMedium, color = ColoresNeu.texto)
}

/**
 * Una cifra: la etiqueta encima ("Bloqueadas hoy") y el valor debajo, grande y
 * con el plural correcto ("1 llamada", "3 llamadas"). En columna, sin filas.
 */
@Composable
private fun Cifra(etiqueta: String, valor: Int, bloqueo: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(text = etiqueta, style = MaterialTheme.typography.bodyMedium, color = ColoresNeu.textoSecundario)
        Text(
            text = textoLlamadas(valor),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (bloqueo) ColoresNeu.bloqueo else ColoresNeu.permitir
        )
    }
}

/**
 * Una barra horizontal sencilla hecha con Box: el carril hundido a todo el
 * ancho y, dentro, la barra rellena en proporción al máximo. La barra es
 * decorativa; el dato va en texto ("Regla 400" y "12 llamadas").
 * Sin altura fija: solo un alto MÍNIMO para que se vea.
 */
@Composable
private fun BarraRanking(uso: RepositorioRegistro.UsoRegla, maximo: Int) {
    val fraccion = (uso.veces.toFloat() / maximo).coerceIn(0.04f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = textoReglaAplicada(uso.patron),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = ColoresNeu.texto
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neuHundido(radio = 8.dp, profundidad = 2.dp)
                .padding(3.dp)
                // Lectores de pantalla: el dato ya está en texto, la barra se omite.
                .clearAndSetSemantics { }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraccion)
                    .defaultMinSize(minHeight = 10.dp)
                    .background(ColoresNeu.bloqueo, RoundedCornerShape(6.dp))
            )
        }
        Text(
            text = textoLlamadas(uso.veces),
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresNeu.textoSecundario
        )
    }
}
