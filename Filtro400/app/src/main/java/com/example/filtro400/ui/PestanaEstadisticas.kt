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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.filtro400.registro.RepositorioRegistro

/** Tamaño del ranking de reglas. */
private const val TOP_REGLAS = 10

/**
 * Pestaña Estadísticas. Usa los métodos ya probados de [RepositorioRegistro]:
 *  - [RepositorioRegistro.estadisticas]: bloqueadas y permitidas (hoy, 7 días
 *    y total) en una sola consulta; "hoy" empieza a las 00:00 de la zona local;
 *  - [RepositorioRegistro.rankingReglas]: reglas que más han decidido.
 *
 * El interruptor "incluir simulaciones" (compartido con Registro) solo cambia
 * el parámetro `incluirSimulaciones`: por defecto las filas SIMULADAS que
 * guarda el Simulador no cuentan.
 * Las consultas van en Dispatchers.IO dentro del repositorio.
 */
@Composable
fun PestanaEstadisticas(
    repositorioRegistro: RepositorioRegistro,
    refresco: Int,
    incluirSimulaciones: Boolean,
    alCambiarIncluirSimulaciones: (Boolean) -> Unit,
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
                limite = TOP_REGLAS
            )
            totales to top
        }
        cargando = false
        resultado.onSuccess { (totales, top) ->
            estadisticas = totales
            ranking = top
            error = null
        }.onFailure {
            error = "No se pudieron calcular las estadísticas: ${it.message ?: "error desconocido"}"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Estadísticas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item { TarjetaIncluirSimulaciones(incluirSimulaciones, alCambiarIncluirSimulaciones) }

        val datos = estadisticas
        when {
            cargando && datos == null -> item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            error != null -> item { TarjetaAviso(error.orEmpty(), esError = true) }

            datos == null || datos.total == 0 -> item {
                TarjetaAviso(
                    if (incluirSimulaciones) {
                        "Todavía no hay llamadas registradas (ni reales ni simuladas), así que no " +
                            "hay estadísticas."
                    } else {
                        "Todavía no hay llamadas reales registradas, así que no hay estadísticas. " +
                            "Se rellenan con las llamadas que filtra el servicio."
                    }
                )
            }

            else -> {
                // Bloqueadas y permitidas SIEMPRE por separado, con etiqueta explícita.
                item {
                    TarjetaCifras(titulo = "Hoy (desde las 00:00)") {
                        Cifra("Bloqueadas hoy", datos.bloqueadasHoy, destacada = true)
                        Cifra("Permitidas hoy", datos.permitidasHoy)
                    }
                }
                item {
                    TarjetaCifras(titulo = "Últimos 7 días (hoy y los 6 anteriores)") {
                        Cifra("Bloqueadas en los últimos 7 días", datos.bloqueadas7Dias, destacada = true)
                        Cifra("Permitidas en los últimos 7 días", datos.permitidas7Dias)
                    }
                }
                item {
                    TarjetaCifras(titulo = "Todo el registro") {
                        Cifra("Bloqueadas en total", datos.bloqueadas, destacada = true)
                        Cifra("Permitidas en total", datos.permitidas)
                    }
                }
                item {
                    Text(
                        text = "Reglas que más han decidido",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (ranking.isEmpty()) {
                    item { TarjetaAviso("Ninguna llamada registrada ha coincidido con una regla.") }
                } else {
                    itemsIndexed(ranking, key = { _, uso -> uso.patron }) { indice, uso ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${indice + 1}. ${uso.patron}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (uso.veces == 1) "1 vez" else "${uso.veces} veces",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tarjeta con un título y varias cifras debajo. */
@Composable
private fun TarjetaCifras(titulo: String, contenido: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            contenido()
        }
    }
}

@Composable
private fun Cifra(texto: String, valor: Int, destacada: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = texto, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = valor.toString(),
            style = if (destacada) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (destacada) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
