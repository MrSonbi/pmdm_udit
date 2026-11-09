package com.example.filtro400.ui

import android.database.sqlite.SQLiteConstraintException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.motor.ValidacionReglas
import kotlinx.coroutines.launch

/**
 * Diálogo de alta/edición de una regla.
 *
 * La validación es una función pura del motor. Si el patrón es muy corto
 * (2 dígitos o menos) y es [TipoRegla.PREFIJO], se muestra una segunda
 * confirmación antes de guardar.
 */
@Composable
fun DialogoRegla(
    regla: Regla?,
    patronesExistentes: List<String>,
    alCerrar: () -> Unit,
    alGuardar: suspend (patron: String, etiqueta: String?, tipo: TipoRegla, accion: Accion, prioridad: Int) -> Unit
) {
    val claveEstado = regla?.id ?: 0L
    var textoPatron by remember(claveEstado) { mutableStateOf(regla?.patron.orEmpty()) }
    var textoEtiqueta by remember(claveEstado) { mutableStateOf(regla?.etiqueta.orEmpty()) }
    var tipoSeleccionado by remember(claveEstado) { mutableStateOf(regla?.tipo ?: TipoRegla.PREFIJO) }
    var accionSeleccionada by remember(claveEstado) {
        mutableStateOf(regla?.accion ?: Accion.BLOQUEAR)
    }
    var prioridadTexto by remember(claveEstado) {
        mutableStateOf((regla?.prioridad ?: ValidacionReglas.prioridadPorDefecto(accionSeleccionada)).toString())
    }
    // Error del patrón o del guardado (se muestra bajo el patrón) y error de la
    // prioridad (se muestra bajo la prioridad): así el usuario sabe qué corregir.
    var error by remember(claveEstado) { mutableStateOf<String?>(null) }
    var errorPrioridad by remember(claveEstado) { mutableStateOf<String?>(null) }
    var guardando by remember(claveEstado) { mutableStateOf(false) }
    var mostrarConfirmacion by remember(claveEstado) { mutableStateOf(false) }
    var patronPendiente by remember(claveEstado) { mutableStateOf<String?>(null) }
    val alcance = rememberCoroutineScope()

    fun actualizarPrioridadPorDefecto() {
        if (regla == null) {
            prioridadTexto = ValidacionReglas.prioridadPorDefecto(accionSeleccionada).toString()
        }
    }

    fun guardar(patron: String, prioridad: Int) {
        if (guardando) return
        guardando = true
        error = null
        alcance.launch {
            val fallo = runCatching {
                alGuardar(
                    patron,
                    textoEtiqueta.trim().ifEmpty { null },
                    tipoSeleccionado,
                    accionSeleccionada,
                    prioridad
                )
            }.exceptionOrNull()
            guardando = false
            if (fallo == null) {
                alCerrar()
            } else {
                error = mensajeFalloGuardado(fallo)
            }
        }
    }

    fun intentarGuardar() {
        // Prioridad vacía -> la de por defecto; texto que no es un número -> error
        // (antes se cambiaba en silencio por la prioridad por defecto).
        val textoPrioridad = prioridadTexto.trim()
        val prioridad = textoPrioridad.toIntOrNull()
        if (textoPrioridad.isNotEmpty() && prioridad == null) {
            errorPrioridad = "La prioridad debe ser un número entero entre 0 y 100."
            return
        }
        if (prioridad != null && prioridad !in 0..100) {
            errorPrioridad = "La prioridad debe estar entre 0 y 100."
            return
        }
        errorPrioridad = null
        val validacion = ValidacionReglas.validar(
            textoPatron,
            patronesExistentes,
            tipo = tipoSeleccionado,
            accion = accionSeleccionada,
            prioridad = prioridad
        )
        when {
            validacion.error != null -> error = validacion.error
            validacion.requiereConfirmacion && tipoSeleccionado == TipoRegla.PREFIJO -> {
                patronPendiente = validacion.patron
                mostrarConfirmacion = true
            }
            else -> {
                val patronFinal = validacion.patron
                val prioridadFinal = validacion.prioridad
                if (patronFinal != null) {
                    guardar(patronFinal, prioridadFinal)
                } else {
                    error = "No se ha podido validar la regla."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!guardando) alCerrar() },
        title = { Text(if (regla == null) "Añadir regla" else "Editar regla") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tipo", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tipoSeleccionado == TipoRegla.PREFIJO,
                        onClick = { if (!guardando) tipoSeleccionado = TipoRegla.PREFIJO },
                        label = { Text("Prefijo") },
                        enabled = !guardando
                    )
                    FilterChip(
                        selected = tipoSeleccionado == TipoRegla.NUMERO,
                        onClick = { if (!guardando) tipoSeleccionado = TipoRegla.NUMERO },
                        label = { Text("Número exacto") },
                        enabled = !guardando
                    )
                }
                Text("Acción", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = accionSeleccionada == Accion.BLOQUEAR,
                        onClick = {
                            if (!guardando) {
                                accionSeleccionada = Accion.BLOQUEAR
                                actualizarPrioridadPorDefecto()
                            }
                        },
                        label = { Text("Bloquear") },
                        enabled = !guardando
                    )
                    FilterChip(
                        selected = accionSeleccionada == Accion.PERMITIR,
                        onClick = {
                            if (!guardando) {
                                accionSeleccionada = Accion.PERMITIR
                                actualizarPrioridadPorDefecto()
                            }
                        },
                        label = { Text("Permitir (lista blanca)") },
                        enabled = !guardando
                    )
                }
                OutlinedTextField(
                    value = textoPatron,
                    onValueChange = {
                        textoPatron = it
                        error = null
                    },
                    label = { Text(if (tipoSeleccionado == TipoRegla.NUMERO) "Número" else "Prefijo o número") },
                    placeholder = { Text("Ej. 400 o +34 612345678") },
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
                    value = prioridadTexto,
                    onValueChange = { prioridadTexto = it; errorPrioridad = null },
                    label = { Text("Prioridad (0-100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !guardando,
                    isError = errorPrioridad != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorPrioridad != null) {
                    Text(
                        text = errorPrioridad.orEmpty(),
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
                        "Prioridad por defecto: 0 al bloquear y 100 al permitir. " +
                        "Con prioridad vacía se usa la de por defecto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { intentarGuardar() }, enabled = !guardando) {
                Text(if (guardando) "Guardando…" else "Guardar")
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
        val prioridadConfirmacion = prioridadTexto.trim().toIntOrNull()
            ?: ValidacionReglas.prioridadPorDefecto(accionSeleccionada)
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
                    guardar(patron, prioridadConfirmacion)
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

/**
 * Mensaje claro para el usuario cuando falla el guardado en la BD.
 * La columna `patron` es UNIQUE: si dos reglas acaban con el mismo patrón
 * normalizado, SQLite lanza [SQLiteConstraintException].
 */
private fun mensajeFalloGuardado(fallo: Throwable): String = when (fallo) {
    is SQLiteConstraintException -> "Ya existe una regla con ese patrón."
    else -> "No se pudo guardar la regla. Inténtalo de nuevo."
}
