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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.filtro400.data.RepositorioReglas
import com.example.filtro400.datos.Preferencias
import com.example.filtro400.modelo.Regla
import com.example.filtro400.motor.Decision
import com.example.filtro400.motor.MotorFiltrado
import com.example.filtro400.registro.RepositorioRegistro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla con navegación por pestañas: Inicio, Reglas, Registro/Ajustes.
 */
@Composable
fun PantallaInicio() {
    val contexto = LocalContext.current
    val repositorio = remember { RepositorioReglas.obtener(contexto.applicationContext) }
    val repositorioRegistro = remember { RepositorioRegistro.obtener(contexto.applicationContext) }
    val alcance = rememberCoroutineScope()
    val roleManager = remember { contexto.getSystemService(RoleManager::class.java) }

    var pestana by rememberSaveable { mutableIntStateOf(0) }

    var filtroActivo by remember {
        mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING))
    }

    // Borrado automático de registros de más de 30 días al abrir la app, en segundo plano.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching { repositorioRegistro.borrarMasAntiguosDe(30) }
        }
    }

    var guardarRegistro by rememberSaveable {
        mutableStateOf(Preferencias.guardarRegistroActivado(contexto))
    }

    val cicloVida = LocalLifecycleOwner.current
    DisposableEffect(cicloVida) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                filtroActivo = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                guardarRegistro = Preferencias.guardarRegistroActivado(contexto)
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

    LaunchedEffect(recarga) {
        cargando = true
        val resultado = withContext(Dispatchers.IO) {
            runCatching { repositorio.listarTodas() }
        }
        cargando = false
        resultado.onSuccess { lista ->
            reglas = lista
            errorCarga = null
            resultadoPrueba = null
        }.onFailure { fallo ->
            errorCarga = "No se pudieron cargar las reglas: ${fallo.message ?: "error desconocido"}"
        }
    }

    var dialogoAbierto by remember { mutableStateOf(false) }
    var reglaEnEdicion by remember { mutableStateOf<Regla?>(null) }
    var reglaAEliminar by remember { mutableStateOf<Regla?>(null) }
    var mostrarConfirmacionBorrarRegistro by remember { mutableStateOf(false) }

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

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pestana == 0,
                    onClick = { pestana = 0 },
                    icon = {},
                    label = { Text("Inicio") }
                )
                NavigationBarItem(
                    selected = pestana == 1,
                    onClick = { pestana = 1 },
                    icon = {},
                    label = { Text("Reglas") }
                )
                NavigationBarItem(
                    selected = pestana == 2,
                    onClick = { pestana = 2 },
                    icon = {},
                    label = { Text("Registro") }
                )
            }
        }
    ) { paddingValores ->
        when (pestana) {
            0 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValores),
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
                    item {
                        Text(
                            text = "Limitación de Android: solo llegan al filtro las llamadas de números " +
                                "que NO están en la agenda. Los contactos no pasan por esta app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValores),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Reglas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Se evalúan por prioridad, tipo y acción. La lista blanca puede ganar al bloqueo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = {
                                reglaEnEdicion = null
                                dialogoAbierto = true
                            }) {
                                Text("Añadir regla")
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
                                    text = "No hay reglas guardadas. Ahora mismo se permitirían todas las llamadas.",
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
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValores),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Registro y ajustes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Card {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Guardar registro")
                                            Text(
                                                text = "Activo por defecto. Solo se guarda el número enmascarado.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = guardarRegistro,
                                            onCheckedChange = { activar ->
                                                guardarRegistro = activar
                                                Preferencias.setGuardarRegistro(contexto, activar)
                                            }
                                        )
                                    }
                                    TextButton(onClick = { mostrarConfirmacionBorrarRegistro = true }) {
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
                    }
                }
            }
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
            alGuardar = { patron, etiqueta, tipo, accion, prioridad ->
                withContext(Dispatchers.IO) {
                    if (reglaActual == null) {
                        repositorio.insertar(patron, etiqueta, tipo, accion, prioridad)
                    } else {
                        repositorio.actualizar(reglaActual.id, patron, etiqueta, tipo, accion, prioridad)
                    }
                }
                recarga++
            }
        )
    }

    reglaAEliminar?.let { regla ->
        AlertDialog(
            onDismissRequest = { reglaAEliminar = null },
            title = { Text("Eliminar regla") },
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

    if (mostrarConfirmacionBorrarRegistro) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrarRegistro = false },
            title = { Text("Borrar registro") },
            text = { Text("¿Seguro que quieres borrar todo el registro de actividad? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacionBorrarRegistro = false
                    alcance.launch {
                        withContext(Dispatchers.IO) {
                            runCatching { repositorioRegistro.borrarTodos() }
                        }
                    }
                }) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionBorrarRegistro = false }) {
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
                label = { Text("Ej. +34 400 555 123") },
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
                Column {
                    Text(
                        text = if (regla.activa) "Activa" else "Inactiva",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (regla.activa) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "${regla.tipo.name} • ${regla.accion.name} • prio ${regla.prioridad}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
