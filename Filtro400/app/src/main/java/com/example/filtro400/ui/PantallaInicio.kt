package com.example.filtro400.ui

import android.app.role.RoleManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.data.Regla
import com.example.filtro400.data.RepositorioReglas
import com.example.filtro400.motor.Decision
import com.example.filtro400.motor.MotorFiltrado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla principal de la Fase 2:
 * - estado del rol de filtrado,
 * - probador que usa el mismo motor que el servicio,
 * - lista de prefijos con CRUD y switch de activación.
 */
@Composable
fun PantallaInicio() {
    val contexto = LocalContext.current
    val repositorio = remember { RepositorioReglas.obtener(contexto.applicationContext) }
    val alcance = rememberCoroutineScope()
    val roleManager = remember { contexto.getSystemService(RoleManager::class.java) }

    var filtroActivo by remember {
        mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING))
    }

    // Si el usuario cambia el rol desde Ajustes del sistema y vuelve a la app,
    // ON_RESUME vuelve a preguntar a Android para que la pantalla no mienta.
    val cicloVida = LocalLifecycleOwner.current
    DisposableEffect(cicloVida) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                filtroActivo = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            }
        }
        cicloVida.lifecycle.addObserver(observador)
        onDispose { cicloVida.lifecycle.removeObserver(observador) }
    }

    val lanzadorRol = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        filtroActivo = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    var reglas by remember { mutableStateOf<List<Regla>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var errorCarga by remember { mutableStateOf<String?>(null) }
    var mensajeAccion by remember { mutableStateOf<String?>(null) }
    var recarga by remember { mutableIntStateOf(0) }
    var numeroPrueba by remember { mutableStateOf("") }
    var resultadoPrueba by remember { mutableStateOf<Decision?>(null) }

    // La BD se consulta fuera del hilo principal. `recarga` se incrementa después
    // de cada cambio para volver a leer la lista ya actualizada.
    LaunchedEffect(recarga) {
        cargando = true
        val resultado = withContext(Dispatchers.IO) {
            runCatching { repositorio.listarTodas() }
        }
        cargando = false
        resultado.onSuccess { lista ->
            reglas = lista
            errorCarga = null
            // Si cambian las reglas, el resultado anterior del probador ya no vale.
            resultadoPrueba = null
        }.onFailure { fallo ->
            errorCarga = "No se pudieron cargar las reglas: ${fallo.message ?: "error desconocido"}"
        }
    }

    var dialogoAbierto by remember { mutableStateOf(false) }
    var reglaEnEdicion by remember { mutableStateOf<Regla?>(null) }
    var reglaAEliminar by remember { mutableStateOf<Regla?>(null) }

    fun ejecutarAccion(accion: suspend () -> Unit) {
        alcance.launch {
            val fallo = runCatching { accion() }.exceptionOrNull()
            if (fallo == null) {
                mensajeAccion = null
                recarga++
            } else {
                mensajeAccion = "No se pudo completar el cambio: " +
                    (fallo.message ?: "error desconocido")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TarjetaEstado(
                filtroActivo = filtroActivo,
                alActivar = {
                    val intent = roleManager.createRequestRoleIntent(
                        RoleManager.ROLE_CALL_SCREENING
                    )
                    lanzadorRol.launch(intent)
                }
            )
        }

        item {
            TarjetaProbador(
                numero = numeroPrueba,
                alCambiarNumero = {
                    numeroPrueba = it
                    resultadoPrueba = null
                },
                resultado = resultadoPrueba,
                alProbar = {
                    alcance.launch {
                        // Mismo repositorio y mismo motor que usa el servicio.
                        val consulta = withContext(Dispatchers.IO) {
                            runCatching { repositorio.reglasActivas() }
                        }
                        consulta.onSuccess { reglasActivas ->
                            resultadoPrueba = MotorFiltrado.evaluar(numeroPrueba, reglasActivas)
                        }.onFailure { fallo ->
                            mensajeAccion = "No se pudo probar el número: " +
                                (fallo.message ?: "error desconocido")
                        }
                    }
                }
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Prefijos bloqueados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Se bloquea cualquier llamada que empiece por uno de estos patrones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = {
                    reglaEnEdicion = null
                    dialogoAbierto = true
                }) {
                    Text("Añadir prefijo")
                }
            }
        }

        if (errorCarga != null || mensajeAccion != null) {
            item {
                Card {
                    Text(
                        text = errorCarga ?: mensajeAccion.orEmpty(),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (cargando) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }

        if (!cargando && reglas.isEmpty()) {
            item {
                Card {
                    Text(
                        text = "No hay prefijos guardados. Ahora mismo se permitirían todas las llamadas.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        items(reglas, key = { it.id }) { regla ->
            TarjetaRegla(
                regla = regla,
                alCambiarActiva = { activa ->
                    ejecutarAccion {
                        withContext(Dispatchers.IO) {
                            repositorio.cambiarActiva(regla.id, activa)
                        }
                    }
                },
                alEditar = {
                    reglaEnEdicion = regla
                    dialogoAbierto = true
                },
                alEliminar = { reglaAEliminar = regla }
            )
        }

        item {
            Text(
                text = "Limitación de Android: solo llegan al filtro las llamadas de números " +
                    "que NO están en la agenda. Los contactos no pasan por esta app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (dialogoAbierto) {
        val reglaActual = reglaEnEdicion
        DialogoRegla(
            regla = reglaActual,
            patronesExistentes = reglas
                .filter { it.id != reglaActual?.id }
                .map { it.patron },
            alCerrar = {
                dialogoAbierto = false
                reglaEnEdicion = null
            },
            alGuardar = { patron, etiqueta ->
                withContext(Dispatchers.IO) {
                    if (reglaActual == null) {
                        repositorio.insertar(patron, etiqueta)
                    } else {
                        repositorio.actualizar(reglaActual.id, patron, etiqueta)
                    }
                }
                recarga++
            }
        )
    }

    reglaAEliminar?.let { regla ->
        AlertDialog(
            onDismissRequest = { reglaAEliminar = null },
            title = { Text("Eliminar prefijo") },
            text = {
                Text(
                    "¿Seguro que quieres eliminar la regla «${regla.patron}»? " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    reglaAEliminar = null
                    ejecutarAccion {
                        withContext(Dispatchers.IO) {
                            repositorio.eliminar(regla.id)
                        }
                    }
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { reglaAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TarjetaEstado(filtroActivo: Boolean, alActivar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Filtro 400",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (filtroActivo) "✅ Filtro ACTIVO" else "⚠️ Filtro inactivo",
                style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = alActivar, enabled = !filtroActivo) {
                Text("Activar filtro de llamadas")
            }
            Text(
                text = if (filtroActivo) {
                    "Puedes cambiar el rol en Ajustes del sistema. Al volver a la app se comprueba de nuevo."
                } else {
                    "Android muestra su propio diálogo: la app no puede darse el rol a sí misma."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaProbador(
    numero: String,
    alCambiarNumero: (String) -> Unit,
    resultado: Decision?,
    alProbar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Probador de números",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Usa las mismas reglas guardadas que el servicio real.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = numero,
                onValueChange = alCambiarNumero,
                label = { Text("Ej. +34 400 123 456") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = alProbar) {
                Text("Probar")
            }

            if (resultado != null) {
                Text(
                    text = if (resultado.bloquea) "🚫 Se BLOQUEARÍA" else "✅ Pasaría",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (resultado.bloquea) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(resultado.motivo, style = MaterialTheme.typography.bodyMedium)
                resultado.regla?.let { regla ->
                    val etiqueta = regla.etiqueta
                        ?.takeIf { it.isNotBlank() }
                        ?.let { " — $it" }
                        .orEmpty()
                    Text(
                        text = "Regla aplicada: ${regla.patron}$etiqueta",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                val normalizado = ReglaBloqueo.normalizar(numero)
                if (normalizado.isNotEmpty()) {
                    Text(
                        text = "Número normalizado: $normalizado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaRegla(
    regla: Regla,
    alCambiarActiva: (Boolean) -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = regla.patron,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    regla.etiqueta?.takeIf { it.isNotBlank() }?.let { etiqueta ->
                        Text(
                            text = etiqueta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = regla.activa, onCheckedChange = alCambiarActiva)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (regla.activa) "Activa" else "Inactiva",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (regla.activa) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = alEditar) {
                    Text("Editar")
                }
                TextButton(onClick = alEliminar) {
                    Text("Eliminar")
                }
            }
        }
    }
}
