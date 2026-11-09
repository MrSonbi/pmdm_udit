package com.example.filtro400.ui

import android.app.role.RoleManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.filtro400.data.RepositorioReglas
import com.example.filtro400.datos.Preferencias
import com.example.filtro400.modelo.Regla
import com.example.filtro400.motor.Decision
import com.example.filtro400.motor.MotorFiltrado
import com.example.filtro400.registro.OrigenLlamada
import com.example.filtro400.registro.RepositorioRegistro
import com.example.filtro400.util.Enmascarar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla con navegación por pestañas: Inicio, Reglas, Registro, Estadísticas y Ajustes.
 * Registro, Estadísticas y Ajustes viven en sus propios archivos (Pestana*.kt).
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

    // Contador que las pestañas Registro y Estadísticas observan: al cambiar,
    // vuelven a leer la BD (al volver a la app, tras borrar el registro...).
    var refrescoRegistro by remember { mutableIntStateOf(0) }

    // "Incluir simulaciones" es UN solo estado compartido por Registro y
    // Estadísticas: así las dos pestañas cuentan siempre lo mismo. Apagado
    // por defecto: solo cuentan las llamadas reales.
    var incluirSimulaciones by rememberSaveable { mutableStateOf(false) }

    val cicloVida = LocalLifecycleOwner.current
    DisposableEffect(cicloVida) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                // El usuario puede haber quitado el rol en Ajustes del sistema.
                filtroActivo = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                guardarRegistro = Preferencias.guardarRegistroActivado(contexto)
                // Mientras la app estaba en segundo plano pudo entrar alguna llamada.
                refrescoRegistro++
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
    // --- Simulador ---
    var numeroSimulado by remember { mutableStateOf("") }
    var resultadoSimulado by remember { mutableStateOf<Decision?>(null) }
    // Interruptor "guardar en el registro": APAGADO por defecto para no
    // llenar el registro con pruebas sin querer.
    var guardarSimulacion by rememberSaveable { mutableStateOf(false) }
    var avisoSimulacion by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recarga) {
        cargando = true
        val resultado = withContext(Dispatchers.IO) {
            runCatching { repositorio.listarTodas() }
        }
        cargando = false
        resultado.onSuccess { lista ->
            reglas = lista
            errorCarga = null
            // Las reglas han cambiado: el último resultado ya no es fiable.
            resultadoSimulado = null
            avisoSimulacion = null
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
            // Pestañas solo texto (sin dependencia de iconos extra).
            NavigationBar {
                PESTANAS.forEachIndexed { indice, titulo ->
                    NavigationBarItem(
                        selected = pestana == indice,
                        onClick = { pestana = indice },
                        icon = {},
                        label = {
                            Text(
                                text = titulo,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
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
                        TarjetaSimulador(
                            numero = numeroSimulado,
                            alCambiarNumero = {
                                numeroSimulado = it
                                resultadoSimulado = null
                                avisoSimulacion = null
                            },
                            guardarEnRegistro = guardarSimulacion,
                            alCambiarGuardarEnRegistro = { guardarSimulacion = it },
                            resultado = resultadoSimulado,
                            aviso = avisoSimulacion,
                            alSimular = {
                                val numero = numeroSimulado
                                val guardar = guardarSimulacion
                                alcance.launch {
                                    // Mismas reglas que el servicio: la copia en caché de
                                    // las reglas activas (no se hace SQL si ya está cargada;
                                    // solo la primera vez, y por eso va en Dispatchers.IO).
                                    val consulta = withContext(Dispatchers.IO) {
                                        runCatching { repositorio.reglasActivas() }
                                    }
                                    val reglasActivas = consulta.getOrElse { fallo ->
                                        avisoSimulacion = "No se pudieron leer las reglas activas: " +
                                            (fallo.message ?: "error desconocido")
                                        return@launch
                                    }
                                    // El MISMO motor puro que usa FiltroLlamadasService.
                                    val decision = MotorFiltrado.evaluar(numero, reglasActivas)
                                    resultadoSimulado = decision
                                    avisoSimulacion = null

                                    if (guardar) {
                                        // insertar() ya enmascara el número antes de guardarlo.
                                        val guardado = runCatching {
                                            repositorioRegistro.insertar(
                                                numero = numero,
                                                decision = decision.accion,
                                                reglaId = decision.regla?.id,
                                                reglaPatron = decision.regla?.patron,
                                                reglaTipo = decision.regla?.tipo,
                                                reglaPrioridad = decision.regla?.prioridad,
                                                motivo = decision.motivo,
                                                origen = OrigenLlamada.SIMULADA
                                            )
                                        }
                                        avisoSimulacion = if (guardado.isSuccess) {
                                            refrescoRegistro++
                                            "Guardada en el registro como simulación. No cuenta en " +
                                                "Registro ni en Estadísticas salvo con «Incluir simulaciones»."
                                        } else {
                                            "No se pudo guardar la simulación en el registro."
                                        }
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
                    if (!cargando && errorCarga == null && reglas.isEmpty()) {
                        item {
                            TarjetaAviso(
                                "Todavía no hay reglas. Mientras no añadas ninguna, se permiten " +
                                    "todas las llamadas. Pulsa «Añadir regla» o importa un CSV desde Ajustes."
                            )
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

            2 -> PestanaRegistro(
                repositorioRegistro = repositorioRegistro,
                refresco = refrescoRegistro,
                guardarRegistro = guardarRegistro,
                incluirSimulaciones = incluirSimulaciones,
                alCambiarIncluirSimulaciones = { incluirSimulaciones = it },
                alBorrarRegistro = { mostrarConfirmacionBorrarRegistro = true },
                contentPadding = paddingValores
            )

            3 -> PestanaEstadisticas(
                repositorioRegistro = repositorioRegistro,
                refresco = refrescoRegistro,
                incluirSimulaciones = incluirSimulaciones,
                alCambiarIncluirSimulaciones = { incluirSimulaciones = it },
                contentPadding = paddingValores
            )

            else -> PestanaAjustes(
                repositorioReglas = repositorio,
                guardarRegistro = guardarRegistro,
                alCambiarGuardarRegistro = { activar ->
                    guardarRegistro = activar
                    Preferencias.setGuardarRegistro(contexto, activar)
                },
                alBorrarRegistro = { mostrarConfirmacionBorrarRegistro = true },
                // Tras importar, la pestaña Reglas debe releer la BD.
                alReglasImportadas = { recarga++ },
                contentPadding = paddingValores
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
        DialogoBorrarRegistro(
            alConfirmar = {
                mostrarConfirmacionBorrarRegistro = false
                alcance.launch {
                    withContext(Dispatchers.IO) {
                        runCatching { repositorioRegistro.borrarTodos() }
                    }
                    // Registro y Estadísticas se vuelven a leer (quedarán vacías).
                    refrescoRegistro++
                }
            },
            alCancelar = { mostrarConfirmacionBorrarRegistro = false }
        )
    }
}

/** Títulos de la barra inferior, en orden. El índice es la pestaña. */
private val PESTANAS = listOf("Inicio", "Reglas", "Registro", "Estadísticas", "Ajustes")

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

/**
 * Simulador: evalúa un número con el mismo [MotorFiltrado.evaluar] y las mismas
 * reglas activas en caché que el servicio, y explica la decisión.
 *
 * Nunca muestra el número tecleado ya normalizado: solo cómo quedaría
 * enmascarado en el registro (igual que una llamada real).
 */
@Composable
private fun TarjetaSimulador(
    numero: String,
    alCambiarNumero: (String) -> Unit,
    guardarEnRegistro: Boolean,
    alCambiarGuardarEnRegistro: (Boolean) -> Unit,
    resultado: Decision?,
    aviso: String?,
    alSimular: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Simulador de llamadas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Usa el mismo motor y las mismas reglas activas que el filtro real.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = numero,
                onValueChange = alCambiarNumero,
                label = { Text("Número entrante") },
                placeholder = { Text("Ej. +34 400 555 123") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Guardar en el registro")
                    Text(
                        text = "Se guarda como «simulada», con el número enmascarado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = guardarEnRegistro, onCheckedChange = alCambiarGuardarEnRegistro)
            }
            Button(onClick = alSimular) {
                Text("Simular")
            }

            if (resultado != null) {
                Text(
                    text = if (resultado.bloquea) "🚫 Se BLOQUEARÍA" else "✅ Se PERMITIRÍA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (resultado.bloquea) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                val regla = resultado.regla
                if (regla != null) {
                    val etiqueta = regla.etiqueta
                        ?.takeIf { it.isNotBlank() }
                        ?.let { " — $it" }
                        .orEmpty()
                    DatoSimulacion("Regla aplicada", "${regla.patron}$etiqueta")
                    DatoSimulacion("Tipo", textoTipo(regla.tipo))
                    DatoSimulacion("Prioridad", regla.prioridad.toString())
                } else {
                    DatoSimulacion("Regla aplicada", "ninguna")
                }
                DatoSimulacion("Motivo", resultado.motivo)
                DatoSimulacion("En el registro aparecería como", Enmascarar.numero(numero))
            }
            aviso?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Una línea "Etiqueta: valor" del resultado del simulador. */
@Composable
private fun DatoSimulacion(etiqueta: String, valor: String) {
    Text(
        text = "$etiqueta: $valor",
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun TarjetaRegla(
    regla: Regla,
    alCambiarActiva: (Boolean) -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit
) {
    // Tres filas para que el texto largo nunca compita por el ancho con los botones:
    //  1) patrón + etiqueta + estado (con weight) y el Switch a la derecha;
    //  2) descripción (tipo • acción • prioridad) a todo el ancho, máx. 2 líneas;
    //  3) botones Editar / Eliminar en su propia fila, alineados a la derecha.
    // Sin alturas fijas: la tarjeta mide lo que mide su contenido.
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
        ) {
            // Fila 1
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = regla.patron,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    regla.etiqueta?.takeIf { it.isNotBlank() }?.let { etiqueta ->
                        Text(
                            text = etiqueta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (regla.activa) "Activa" else "Inactiva",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (regla.activa) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                // El Switch de Material 3 ya reserva su zona táctil mínima de 48 dp.
                Switch(checked = regla.activa, onCheckedChange = alCambiarActiva)
            }
            // Fila 2: a todo el ancho, nunca empuja a los botones.
            Text(
                text = "${textoTipo(regla.tipo)} • ${textoAccion(regla.accion)} • prioridad ${regla.prioridad}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, end = 8.dp)
            )
            // Fila 3: solo botones. Un TextButton mide como mínimo 48 dp de alto
            // (zona táctil) y su texto va en una línea, sin partirse.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = alEditar,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text("Editar", maxLines = 1, softWrap = false)
                }
                TextButton(
                    onClick = alEliminar,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text("Eliminar", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
