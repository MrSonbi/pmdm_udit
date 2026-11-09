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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.filtro400.modelo.Accion
import com.example.filtro400.registro.OrigenLlamada
import com.example.filtro400.registro.RegistroLlamada
import com.example.filtro400.registro.RepositorioRegistro
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Cuántas filas recientes enseña la pestaña Registro. */
private const val LIMITE_RECIENTES = 100

/** Filtro de la lista: todas, solo bloqueadas o solo permitidas. */
private enum class FiltroDecision(val texto: String) {
    TODAS("Todas"),
    BLOQUEADAS("Bloqueadas"),
    PERMITIDAS("Permitidas");

    fun acepta(fila: RegistroLlamada): Boolean = when (this) {
        TODAS -> true
        BLOQUEADAS -> fila.decision == Accion.BLOQUEAR
        PERMITIDAS -> fila.decision == Accion.PERMITIR
    }
}

/** "09/11/2026 13:52" en la zona horaria del móvil. */
private val formatoFecha: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-ES"))
    .withZone(ZoneId.systemDefault())

/**
 * Pestaña Registro: últimas llamadas, la más reciente primero, con filtro por
 * decisión y botón para borrar todo (con confirmación).
 *
 * Las filas SIMULADAS (guardadas desde el Simulador) solo aparecen con el
 * interruptor [incluirSimulaciones], y se marcan con otro color y la etiqueta
 * "SIMULADA" para no confundirlas con llamadas reales.
 *
 * [refresco] es un contador: cada vez que cambia se vuelve a leer la BD
 * (al volver a la app, tras borrar el registro...).
 * Las lecturas van en Dispatchers.IO dentro de [RepositorioRegistro].
 */
@Composable
fun PestanaRegistro(
    repositorioRegistro: RepositorioRegistro,
    refresco: Int,
    guardarRegistro: Boolean,
    incluirSimulaciones: Boolean,
    alCambiarIncluirSimulaciones: (Boolean) -> Unit,
    alBorrarRegistro: () -> Unit,
    contentPadding: PaddingValues
) {
    var filas by remember { mutableStateOf<List<RegistroLlamada>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filtro by rememberSaveable { mutableStateOf(FiltroDecision.TODAS) }

    // Se vuelve a leer al llegar un refresco y al cambiar el interruptor.
    LaunchedEffect(refresco, incluirSimulaciones) {
        cargando = true
        val resultado = runCatching {
            repositorioRegistro.listarRecientes(
                incluirSimulaciones = incluirSimulaciones,
                limite = LIMITE_RECIENTES
            )
        }
        cargando = false
        resultado.onSuccess {
            filas = it
            error = null
        }.onFailure {
            error = "No se pudo leer el registro: ${it.message ?: "error desconocido"}"
        }
    }

    val visibles = filas.filter { filtro.acepta(it) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Registro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (incluirSimulaciones) {
                        "Últimas $LIMITE_RECIENTES llamadas (reales y simuladas), la más reciente primero. " +
                            "Solo se guarda el número enmascarado."
                    } else {
                        "Últimas $LIMITE_RECIENTES llamadas reales, la más reciente primero. " +
                            "Solo se guarda el número enmascarado."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FiltroDecision.entries.forEach { opcion ->
                        FilterChip(
                            selected = filtro == opcion,
                            onClick = { filtro = opcion },
                            label = { Text(opcion.texto) }
                        )
                    }
                }
                TextButton(onClick = alBorrarRegistro, enabled = filas.isNotEmpty()) {
                    Text("Borrar registro")
                }
            }
        }
        item { TarjetaIncluirSimulaciones(incluirSimulaciones, alCambiarIncluirSimulaciones) }

        when {
            cargando -> item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            error != null -> item { TarjetaAviso(error.orEmpty(), esError = true) }

            filas.isEmpty() -> item {
                TarjetaAviso(
                    when {
                        !guardarRegistro ->
                            "No hay llamadas registradas y el registro está desactivado en Ajustes."
                        incluirSimulaciones ->
                            "Todavía no hay llamadas registradas, ni reales ni simuladas."
                        else ->
                            "Todavía no hay llamadas reales registradas. Aparecerán aquí cuando el " +
                                "filtro reciba llamadas de números que no están en la agenda. " +
                                "Las simulaciones guardadas se ven con «Incluir simulaciones»."
                    }
                )
            }

            visibles.isEmpty() -> item {
                TarjetaAviso("No hay llamadas ${filtro.texto.lowercase()} entre las últimas $LIMITE_RECIENTES.")
            }

            else -> items(visibles, key = { it.id }) { fila -> TarjetaFilaRegistro(fila) }
        }
    }
}

@Composable
private fun TarjetaFilaRegistro(fila: RegistroLlamada) {
    val bloqueada = fila.decision == Accion.BLOQUEAR
    val simulada = fila.origen == OrigenLlamada.SIMULADA
    // Las simuladas llevan otro color de fondo y la etiqueta "SIMULADA".
    val colores = if (simulada) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = colores) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fila.numeroEnmascarado,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (bloqueada) "Bloqueada" else "Permitida",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (bloqueada) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            if (simulada) {
                Text(
                    text = "SIMULADA (no es una llamada real)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Text(
                text = formatoFecha.format(Instant.ofEpochMilli(fila.fecha)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val regla = fila.reglaPatron?.let { "Regla: $it" } ?: "Sin regla aplicada"
            Text(text = regla, style = MaterialTheme.typography.bodySmall)
            fila.motivo?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Tarjeta con el interruptor "Incluir simulaciones". El estado vive en
 * [PantallaInicio] y lo comparten Registro y Estadísticas.
 */
@Composable
internal fun TarjetaIncluirSimulaciones(incluir: Boolean, alCambiar: (Boolean) -> Unit) {
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
            Switch(checked = incluir, onCheckedChange = alCambiar)
        }
    }
}

/** Tarjeta de texto para estados vacíos, avisos y errores. */
@Composable
internal fun TarjetaAviso(texto: String, esError: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = texto,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Confirmación común para "Borrar registro" (se usa desde Registro y desde Ajustes).
 */
@Composable
internal fun DialogoBorrarRegistro(alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Borrar registro") },
        text = {
            Text("¿Seguro que quieres borrar todo el registro de actividad? Esta acción no se puede deshacer.")
        },
        confirmButton = { TextButton(onClick = alConfirmar) { Text("Borrar") } },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )
}
