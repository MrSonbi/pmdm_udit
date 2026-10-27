package com.example.filtro400.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.filtro400.modelo.Regla
import com.example.filtro400.motor.ValidacionReglas
import kotlinx.coroutines.launch

/**
 * Diálogo de alta/edición de una regla.
 *
 * La validación es una función pura del motor. Si el patrón es muy corto
 * (2 dígitos o menos), se muestra una segunda confirmación antes de guardar.
 */
@Composable
fun DialogoRegla(
    regla: Regla?,
    patronesExistentes: List<String>,
    alCerrar: () -> Unit,
    alGuardar: suspend (patron: String, etiqueta: String?) -> Unit
) {
    val claveEstado = regla?.id ?: 0L
    var textoPatron by remember(claveEstado) { mutableStateOf(regla?.patron.orEmpty()) }
    var textoEtiqueta by remember(claveEstado) { mutableStateOf(regla?.etiqueta.orEmpty()) }
    var error by remember(claveEstado) { mutableStateOf<String?>(null) }
    var guardando by remember(claveEstado) { mutableStateOf(false) }
    var mostrarConfirmacion by remember(claveEstado) { mutableStateOf(false) }
    var patronPendiente by remember(claveEstado) { mutableStateOf<String?>(null) }
    val alcance = rememberCoroutineScope()

    fun guardar(patron: String) {
        if (guardando) return
        guardando = true
        error = null
        alcance.launch {
            val fallo = runCatching {
                alGuardar(patron, textoEtiqueta.trim().ifEmpty { null })
            }.exceptionOrNull()
            guardando = false
            if (fallo == null) {
                alCerrar()
            } else {
                error = "No se pudo guardar. Puede que el patrón ya exista. " +
                    (fallo.message ?: "")
            }
        }
    }

    fun intentarGuardar() {
        val validacion = ValidacionReglas.validar(textoPatron, patronesExistentes)
        when {
            validacion.error != null -> error = validacion.error
            validacion.requiereConfirmacion -> {
                patronPendiente = validacion.patron
                mostrarConfirmacion = true
            }
            else -> validacion.patron?.let { guardar(it) }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!guardando) alCerrar() },
        title = { Text(if (regla == null) "Añadir prefijo" else "Editar prefijo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = textoPatron,
                    onValueChange = {
                        textoPatron = it
                        error = null
                    },
                    label = { Text("Prefijo o número") },
                    placeholder = { Text("Ej. 400 o +44 400 123 456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    enabled = !guardando,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = textoEtiqueta,
                    onValueChange = { textoEtiqueta = it },
                    label = { Text("Etiqueta (opcional)") },
                    singleLine = true,
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Se guarda normalizado: sin espacios, guiones ni +34. " +
                        "No puede quedar vacío.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { intentarGuardar() }, enabled = !guardando) {
                Text(if (guardando) "Guardando..." else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = alCerrar, enabled = !guardando) {
                Text("Cancelar")
            }
        }
    )

    if (mostrarConfirmacion) {
        val patron = patronPendiente.orEmpty()
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("Prefijo muy corto") },
            text = {
                Text(
                    "El patrón «$patron» tiene 2 dígitos o menos y podría bloquear " +
                        "muchísimas llamadas. ¿Quieres guardarlo igualmente?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    guardar(patron)
                }) {
                    Text("Guardar igualmente")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
