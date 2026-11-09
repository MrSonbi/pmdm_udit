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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
 *  - [RepositorioRegistro.estadisticas]: totales en una sola consulta;
 *  - [RepositorioRegistro.rankingReglas]: reglas que más han decidido.
 *
 * El interruptor "incluir simulaciones" solo cambia el parámetro
 * `incluirSimulaciones`; ahora mismo el probador no guarda simulaciones.
 * Las consultas van en Dispatchers.IO dentro del repositorio.
 */
@Composable
fun PestanaEstadisticas(
    repositorioRegistro: RepositorioRegistro,
    refresco: Int,
    contentPadding: PaddingValues
) {
    var incluirSimulaciones by rememberSaveable { mutableStateOf(false) }
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
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Incluir simulaciones")
                        Text(
                            text = "Por defecto solo cuentan las llamadas reales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = incluirSimulaciones, onCheckedChange = { incluirSimulaciones = it })
                }
            }
        }

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
                    "Todavía no hay llamadas registradas, así que no hay estadísticas. " +
                        "Se rellenan con las llamadas que filtra el servicio."
                )
            }

            else -> {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Cifra("Total bloqueadas", datos.bloqueadas, destacada = true)
                            Cifra("Total permitidas", datos.permitidas)
                            Cifra("Llamadas registradas hoy", datos.hoy)
                            Cifra("Llamadas en los últimos 7 días", datos.ultimos7Dias)
                            Cifra("Total en el registro", datos.total)
                        }
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
