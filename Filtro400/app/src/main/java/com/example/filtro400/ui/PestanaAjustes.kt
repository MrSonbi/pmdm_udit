package com.example.filtro400.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.filtro400.csv.ExportadorCsv
import com.example.filtro400.csv.ImportadorCsv
import com.example.filtro400.csv.TextoImportacion
import com.example.filtro400.data.RepositorioReglas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Tipos MIME que se ofrecen al abrir un CSV (los gestores de archivos no son uniformes). */
private val TIPOS_CSV = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "application/csv",
    "text/plain"
)

/**
 * Pestaña Ajustes:
 *  - interruptor "guardar registro" (SharedPreferences, lo gestiona [PantallaInicio]);
 *  - borrar registro (la confirmación la muestra [PantallaInicio]);
 *  - exportar / importar reglas en CSV con el Storage Access Framework (SAF):
 *    el usuario elige el fichero y la app no necesita permisos de almacenamiento.
 *
 * Toda la E/S (BD y ficheros) va en Dispatchers.IO.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PestanaAjustes(
    repositorioReglas: RepositorioReglas,
    guardarRegistro: Boolean,
    alCambiarGuardarRegistro: (Boolean) -> Unit,
    alBorrarRegistro: () -> Unit,
    alReglasImportadas: () -> Unit,
    contentPadding: PaddingValues
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var trabajando by remember { mutableStateOf(false) }
    var mensajeCsv by remember { mutableStateOf<String?>(null) }
    var mensajeCsvEsError by remember { mutableStateOf(false) }
    var erroresImportacion by remember { mutableStateOf<List<ImportadorCsv.ErrorLinea>>(emptyList()) }

    fun mostrar(mensaje: String, esError: Boolean) {
        mensajeCsv = mensaje
        mensajeCsvEsError = esError
    }

    // EXPORTAR: el sistema pide dónde guardar y nos devuelve una Uri (o null si se cancela).
    val lanzadorExportar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        alcance.launch {
            trabajando = true
            erroresImportacion = emptyList()
            val resultado = withContext(Dispatchers.IO) {
                runCatching {
                    val reglas = repositorioReglas.listarTodas()
                    val texto = ExportadorCsv.exportar(reglas)
                    val salida = contexto.contentResolver.openOutputStream(uri, "wt")
                        ?: error("no se pudo abrir el fichero de destino")
                    salida.use { it.write(texto.toByteArray(Charsets.UTF_8)) }
                    reglas.size
                }
            }
            trabajando = false
            resultado.onSuccess { n ->
                mostrar(if (n == 1) "Exportada 1 regla." else "Exportadas $n reglas.", esError = false)
            }.onFailure { fallo ->
                if (fallo is CancellationException) throw fallo
                mostrar("No se pudo exportar: ${fallo.message ?: "error desconocido"}", esError = true)
            }
        }
    }

    // IMPORTAR: añade o actualiza reglas en UNA transacción; nunca borra.
    val lanzadorImportar = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        alcance.launch {
            trabajando = true
            erroresImportacion = emptyList()
            val resultado = withContext(Dispatchers.IO) {
                runCatching {
                    val entrada = contexto.contentResolver.openInputStream(uri)
                        ?: error("no se pudo abrir el fichero")
                    entrada.use { ImportadorCsv(repositorioReglas).importarDesdeStream(it) }
                }
            }
            trabajando = false
            resultado.onSuccess { r ->
                // Resumen: añadidas / actualizadas / omitidas / erróneas (texto puro y probado).
                mostrar(TextoImportacion.resumen(r), esError = r.fallo)
                if (!r.fallo) alReglasImportadas()
                erroresImportacion = r.errores
            }.onFailure { fallo ->
                if (fallo is CancellationException) throw fallo
                mostrar(
                    "No se pudo abrir el fichero seleccionado. No se ha cambiado ninguna regla.",
                    esError = true
                )
            }
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
                text = "Ajustes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // --- Registro de actividad ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Guardar registro")
                            Text(
                                text = "Activo por defecto. Solo se guarda el número enmascarado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = guardarRegistro, onCheckedChange = alCambiarGuardarRegistro)
                    }
                    TextButton(onClick = alBorrarRegistro) {
                        Text("Borrar registro")
                    }
                    Text(
                        text = "Se borran automáticamente los registros de más de 30 días al abrir la app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- CSV ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Reglas en CSV", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Columnas: patron;tipo;accion;prioridad;activa;etiqueta. Importar añade o " +
                            "actualiza reglas sin borrar ninguna. Límite: 1 MB y 5000 filas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // FlowRow: si no caben los dos botones, el segundo baja de línea.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { lanzadorExportar.launch("filtro400_reglas.csv") },
                            enabled = !trabajando
                        ) { Text("Exportar", maxLines = 1, softWrap = false) }
                        OutlinedButton(
                            onClick = { lanzadorImportar.launch(TIPOS_CSV) },
                            enabled = !trabajando
                        ) { Text("Importar", maxLines = 1, softWrap = false) }
                    }
                    if (trabajando) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    mensajeCsv?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (mensajeCsvEsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (erroresImportacion.isNotEmpty()) {
                        Text("Líneas con errores (no se han importado):", style = MaterialTheme.typography.labelLarge)
                        // La lista ya llega recortada (RecorteErrores: máx. 50 + "y N más").
                        erroresImportacion.forEach { e ->
                            Text(
                                text = TextoImportacion.linea(e),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
