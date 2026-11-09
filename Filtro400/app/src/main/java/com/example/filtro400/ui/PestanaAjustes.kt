package com.example.filtro400.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.filtro400.csv.ExportadorCsv
import com.example.filtro400.csv.ImportadorCsv
import com.example.filtro400.csv.TextoImportacion
import com.example.filtro400.data.RepositorioReglas
import com.example.filtro400.ui.tema.ColoresNeu
import com.example.filtro400.ui.tema.FilaInterruptor
import com.example.filtro400.ui.tema.NeuBoton
import com.example.filtro400.ui.tema.NeuCargando
import com.example.filtro400.ui.tema.NeuDesplegable
import com.example.filtro400.ui.tema.NeuDialogo
import com.example.filtro400.ui.tema.NeuTarjeta
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

/** Columnas del CSV, en orden. Se enseñan una por línea en "Formato del archivo". */
private val COLUMNAS_CSV = listOf("patron", "tipo", "accion", "prioridad", "activa", "etiqueta")

/**
 * Pestaña Ajustes, en tres secciones (cada una en una NeuTarjeta con título):
 *  - Privacidad: "Guardar registro de llamadas" (SharedPreferences, lo gestiona
 *    [PantallaInicio]) y "Borrar registro" (la confirmación la muestra
 *    [PantallaInicio]);
 *  - Copia de seguridad: exportar / importar reglas en CSV con el Storage Access
 *    Framework (SAF): el usuario elige el archivo y la app no necesita permisos;
 *  - Ayuda: dos preguntas frecuentes desplegables.
 *
 * Toda la E/S (BD y archivos) va en Dispatchers.IO.
 */
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
    // Resultado de exportar: una frase bajo los botones.
    var mensajeExportar by remember { mutableStateOf<String?>(null) }
    var mensajeExportarEsError by remember { mutableStateOf(false) }
    // Resultado de importar: se enseña en un diálogo.
    var resultadoImportacion by remember { mutableStateOf<ImportadorCsv.ResultadoImportacion?>(null) }

    // EXPORTAR: el sistema pide dónde guardar y nos devuelve una Uri (o null si se cancela).
    val lanzadorExportar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        alcance.launch {
            trabajando = true
            val resultado = withContext(Dispatchers.IO) {
                runCatching {
                    val reglas = repositorioReglas.listarTodas()
                    val texto = ExportadorCsv.exportar(reglas)
                    val salida = contexto.contentResolver.openOutputStream(uri, "wt")
                        ?: error("no se pudo abrir el archivo de destino")
                    salida.use { it.write(texto.toByteArray(Charsets.UTF_8)) }
                    reglas.size
                }
            }
            trabajando = false
            resultado.onSuccess { n ->
                mensajeExportar = if (n == 1) "Exportada 1 regla." else "Exportadas $n reglas."
                mensajeExportarEsError = false
            }.onFailure { fallo ->
                if (fallo is CancellationException) throw fallo
                mensajeExportar = "No se pudo exportar. Prueba a elegir otra carpeta."
                mensajeExportarEsError = true
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
            mensajeExportar = null
            val resultado = withContext(Dispatchers.IO) {
                runCatching {
                    val entrada = contexto.contentResolver.openInputStream(uri)
                        ?: error("no se pudo abrir el archivo")
                    entrada.use { ImportadorCsv(repositorioReglas).importarDesdeStream(it) }
                }
            }
            trabajando = false
            resultado.onSuccess { r ->
                if (!r.fallo) alReglasImportadas()
                resultadoImportacion = r
            }.onFailure { fallo ->
                if (fallo is CancellationException) throw fallo
                resultadoImportacion = ImportadorCsv.ResultadoImportacion(
                    fallo = true,
                    mensaje = "No se pudo abrir el archivo seleccionado."
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = MARGEN_LISTA,
        verticalArrangement = Arrangement.spacedBy(SEPARACION_LISTA)
    ) {
        item { TituloVista(titulo = "Ajustes") }

        // --- Privacidad ---
        item {
            NeuTarjeta {
                TituloSeccion("Privacidad")
                FilaInterruptor(
                    titulo = "Guardar registro de llamadas",
                    activo = guardarRegistro,
                    alCambiar = alCambiarGuardarRegistro,
                    textoActivo = "Sí",
                    textoInactivo = "No"
                )
                Texto("Solo se guarda el número enmascarado y se borra a los 30 días.")
                NeuBoton(
                    texto = "Borrar registro",
                    alPulsar = alBorrarRegistro,
                    primario = false,
                    colorTexto = ColoresNeu.bloqueo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }

        // --- Copia de seguridad (CSV) ---
        item {
            NeuTarjeta {
                TituloSeccion("Copia de seguridad")
                Texto("Guarda tus reglas en un archivo o recupéralas de otro.")
                // Cada botón con weight(1f): ninguno empuja al otro fuera de la fila.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NeuBoton(
                        texto = "Exportar reglas",
                        alPulsar = { lanzadorExportar.launch("filtro400_reglas.csv") },
                        primario = false,
                        habilitado = !trabajando,
                        modifier = Modifier.weight(1f)
                    )
                    NeuBoton(
                        texto = "Importar reglas",
                        alPulsar = { lanzadorImportar.launch(TIPOS_CSV) },
                        primario = false,
                        habilitado = !trabajando,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (trabajando) NeuCargando(modifier = Modifier.padding(top = 4.dp))
                mensajeExportar?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (mensajeExportarEsError) ColoresNeu.bloqueo else ColoresNeu.permitir
                    )
                }
                NeuDesplegable(titulo = "Formato del archivo") {
                    Texto("Texto CSV en UTF-8, con estas columnas en este orden, separadas por punto y coma (;):")
                    // Una columna por línea y en monoespaciada: nunca se parte una palabra.
                    COLUMNAS_CSV.forEach { columna ->
                        Text(
                            text = columna,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            color = ColoresNeu.texto,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Texto("Límites: 1 MB y 5000 filas.")
                    Texto("Importar añade o actualiza reglas. Nunca borra ninguna.")
                }
            }
        }

        // --- Ayuda ---
        item {
            NeuTarjeta {
                TituloSeccion("Ayuda")
                NeuDesplegable(titulo = "¿Por qué no se bloquean mis contactos?") {
                    Texto(
                        "Android solo entrega al filtro las llamadas de números que no están " +
                            "en tu agenda. Las de tus contactos entran siempre, sin pasar por la app."
                    )
                }
                NeuDesplegable(titulo = "¿Cómo activo o quito el filtro?") {
                    Texto(
                        "Se gestiona desde los ajustes del sistema, en la app de filtro de " +
                            "llamadas. Desde Inicio puedes activarlo con «Activar filtro». " +
                            "Al volver a la app se comprueba de nuevo si está activo."
                    )
                }
            }
        }
    }

    resultadoImportacion?.let { r ->
        DialogoResultadoImportacion(resultado = r, alCerrar = { resultadoImportacion = null })
    }
}

/**
 * Resultado de la importación con el estilo neumórfico: una línea por cifra
 * (añadidas / actualizadas / omitidas / erróneas) y, si hay errores, cada uno
 * con su número de línea. La lista ya llega recortada a 50 con "y N más"
 * ([com.example.filtro400.csv.RecorteErrores]).
 */
@Composable
private fun DialogoResultadoImportacion(
    resultado: ImportadorCsv.ResultadoImportacion,
    alCerrar: () -> Unit
) {
    NeuDialogo(
        titulo = if (resultado.fallo) "Importación cancelada" else "Importación terminada",
        alCerrar = alCerrar,
        botones = {
            NeuBoton(texto = "Cerrar", alPulsar = alCerrar, modifier = Modifier.weight(1f))
        }
    ) {
        if (resultado.fallo) {
            // Frase completa ya probada (TextoImportacion): causa + "no se ha cambiado nada".
            Text(
                text = TextoImportacion.resumen(resultado),
                style = MaterialTheme.typography.bodyLarge,
                color = ColoresNeu.bloqueo
            )
        } else {
            lineasResumenImportacion(resultado).forEach { linea ->
                Text(text = linea, style = MaterialTheme.typography.bodyLarge, color = ColoresNeu.texto)
            }
        }
        if (resultado.errores.isNotEmpty()) {
            Text(
                text = "Líneas con errores (no se han importado):",
                style = MaterialTheme.typography.titleMedium,
                color = ColoresNeu.texto
            )
            resultado.errores.forEach { e ->
                Text(
                    text = TextoImportacion.linea(e),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (e.numeroLinea > 0) ColoresNeu.bloqueo else ColoresNeu.textoSecundario
                )
            }
        }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = ColoresNeu.texto
    )
}

/** Párrafo de explicación de las secciones. */
@Composable
private fun Texto(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.bodyLarge, color = ColoresNeu.textoSecundario)
}
