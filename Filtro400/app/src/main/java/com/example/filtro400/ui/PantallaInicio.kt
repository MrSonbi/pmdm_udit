package com.example.filtro400.ui

import android.app.role.RoleManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.filtro400.data.RepositorioReglas
import com.example.filtro400.datos.Preferencias
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.registro.RepositorioRegistro
import com.example.filtro400.ui.tema.BarraNeu
import com.example.filtro400.ui.tema.ColoresNeu
import com.example.filtro400.ui.tema.NeuBoton
import com.example.filtro400.ui.tema.NeuConfirmacion
import com.example.filtro400.ui.tema.NeuTarjeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Títulos de la barra inferior, en orden. El índice es la pestaña.
 * Con 4 pestañas cada una tiene 1/4 del ancho: todas caben en una línea.
 */
private val PESTANAS = listOf("Inicio", "Reglas", "Actividad", "Ajustes")

private const val PESTANA_INICIO = 0
private const val PESTANA_REGLAS = 1
private const val PESTANA_ACTIVIDAD = 2

/**
 * Pantalla principal con 4 pestañas: Inicio, Reglas, Actividad (Registro y
 * Estadísticas) y Ajustes. Cada pestaña vive en su propio archivo; aquí está
 * el estado compartido (reglas, rol, ajustes) y los diálogos comunes.
 */
@Composable
fun PantallaInicio() {
    val contexto = LocalContext.current
    val repositorio = remember { RepositorioReglas.obtener(contexto.applicationContext) }
    val repositorioRegistro = remember { RepositorioRegistro.obtener(contexto.applicationContext) }
    val alcance = rememberCoroutineScope()
    val roleManager = remember { contexto.getSystemService(RoleManager::class.java) }

    var pestana by rememberSaveable { mutableIntStateOf(PESTANA_INICIO) }

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

    // Contador que Inicio y Actividad observan: al cambiar, vuelven a leer la BD
    // (al volver a la app, tras borrar el registro, tras guardar una prueba...).
    var refrescoRegistro by remember { mutableIntStateOf(0) }

    // Estado de Actividad, elevado aquí para que sobreviva al cambio de pestaña:
    //  - vista: 0 = Registro, 1 = Estadísticas;
    //  - mostrarPruebas: UN solo interruptor para las dos vistas, así cuentan
    //    siempre lo mismo. Apagado por defecto: solo llamadas reales.
    var vistaActividad by rememberSaveable { mutableIntStateOf(0) }
    var mostrarPruebas by rememberSaveable { mutableStateOf(false) }

    val cicloVida = LocalLifecycleOwner.current
    DisposableEffect(cicloVida) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                // El usuario puede haber quitado el rol en los ajustes del sistema.
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

    LaunchedEffect(recarga) {
        cargando = true
        val resultado = withContext(Dispatchers.IO) {
            runCatching { repositorio.listarTodas() }
        }
        cargando = false
        resultado.onSuccess { lista ->
            reglas = lista
            errorCarga = null
        }.onFailure {
            errorCarga = "No se pudieron cargar las reglas. Vuelve a intentarlo más tarde."
        }
    }

    // Resumen del día para Inicio: solo llamadas REALES (sin simulaciones).
    var resumenHoy by remember { mutableStateOf<RepositorioRegistro.Estadisticas?>(null) }
    LaunchedEffect(refrescoRegistro) {
        // estadisticas() ya hace la consulta en Dispatchers.IO.
        resumenHoy = runCatching {
            repositorioRegistro.estadisticas(incluirSimulaciones = false)
        }.getOrNull()
    }

    var dialogoAbierto by remember { mutableStateOf(false) }
    var reglaEnEdicion by remember { mutableStateOf<Regla?>(null) }
    var reglaAEliminar by remember { mutableStateOf<Regla?>(null) }
    var mostrarConfirmacionBorrarRegistro by remember { mutableStateOf(false) }
    var simuladorAbierto by rememberSaveable { mutableStateOf(false) }

    fun ejecutarAccion(accion: suspend () -> Unit) {
        alcance.launch {
            val fallo = runCatching { accion() }.exceptionOrNull()
            if (fallo == null) {
                mensajeAccion = null
                recarga++
            } else {
                mensajeAccion = "No se pudo completar el cambio. Inténtalo de nuevo."
            }
        }
    }

    Scaffold(
        containerColor = ColoresNeu.fondo,
        bottomBar = {
            // Pestañas solo texto (sin dependencia de iconos extra); la activa, hundida.
            BarraNeu(
                pestanas = PESTANAS,
                seleccionada = pestana,
                alSeleccionar = { pestana = it }
            )
        }
    ) { paddingValores ->
        when (pestana) {
            PESTANA_INICIO -> PestanaInicioResumen(
                filtroActivo = filtroActivo,
                alActivar = {
                    lanzadorRol.launch(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    )
                },
                resumenHoy = resumenHoy,
                alProbarNumero = { simuladorAbierto = true },
                contentPadding = paddingValores
            )

            PESTANA_REGLAS -> PestanaReglas(
                reglas = reglas,
                cargando = cargando,
                mensajeError = errorCarga ?: mensajeAccion,
                alCambiarActiva = { regla, activa ->
                    ejecutarAccion {
                        withContext(Dispatchers.IO) {
                            repositorio.cambiarActiva(regla.id, activa)
                        }
                    }
                },
                alEditar = { regla ->
                    reglaEnEdicion = regla
                    dialogoAbierto = true
                },
                alAnadir = {
                    reglaEnEdicion = null
                    dialogoAbierto = true
                },
                contentPadding = paddingValores
            )

            PESTANA_ACTIVIDAD -> PestanaActividad(
                repositorioRegistro = repositorioRegistro,
                refresco = refrescoRegistro,
                guardarRegistro = guardarRegistro,
                vista = vistaActividad,
                alCambiarVista = { vistaActividad = it },
                mostrarPruebas = mostrarPruebas,
                alCambiarMostrarPruebas = { mostrarPruebas = it },
                reglasQueBloquean = reglas
                    .filter { it.accion == Accion.BLOQUEAR }
                    .map { it.patron }
                    .toSet(),
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

    if (simuladorAbierto) {
        DialogoSimulador(
            repositorio = repositorio,
            repositorioRegistro = repositorioRegistro,
            alCerrar = { simuladorAbierto = false },
            alGuardada = { refrescoRegistro++ }
        )
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
            },
            // Solo en edición: cierra el diálogo y pide la confirmación de abajo.
            alEliminar = reglaActual?.let { r ->
                {
                    dialogoAbierto = false
                    reglaEnEdicion = null
                    reglaAEliminar = r
                }
            }
        )
    }

    reglaAEliminar?.let { regla ->
        NeuConfirmacion(
            titulo = "Eliminar regla",
            texto = "Se eliminará la regla ${regla.patron} (${fraseRegla(regla).lowercase()}). " +
                "No se puede deshacer.",
            textoConfirmar = "Eliminar",
            peligrosa = true,
            alConfirmar = {
                reglaAEliminar = null
                ejecutarAccion {
                    withContext(Dispatchers.IO) {
                        repositorio.eliminar(regla.id)
                    }
                }
            },
            alCancelar = { reglaAEliminar = null }
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
                    // Inicio y Actividad se vuelven a leer (quedarán vacías).
                    refrescoRegistro++
                }
            },
            alCancelar = { mostrarConfirmacionBorrarRegistro = false }
        )
    }
}

/**
 * Pestaña Inicio, simplificada: estado del filtro, resumen del día y el botón
 * "Probar un número". Sin emojis: los estados van siempre en texto.
 */
@Composable
private fun PestanaInicioResumen(
    filtroActivo: Boolean,
    alActivar: () -> Unit,
    resumenHoy: RepositorioRegistro.Estadisticas?,
    alProbarNumero: () -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = MARGEN_LISTA,
        verticalArrangement = Arrangement.spacedBy(SEPARACION_LISTA)
    ) {
        item { TituloVista(titulo = "Filtro 400") }
        item { TarjetaEstado(filtroActivo = filtroActivo, alActivar = alActivar) }
        item { TarjetaResumenHoy(resumenHoy) }
        item {
            NeuBoton(
                texto = "Probar un número",
                alPulsar = alProbarNumero,
                primario = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Estado del filtro, grande. Con el rol: "Filtro activo" en color de permitir
 * y una frase corta, SIN botón (nada de botones desactivados). Sin el rol:
 * "Filtro desactivado" en color de bloqueo y el botón primario "Activar filtro".
 */
@Composable
private fun TarjetaEstado(filtroActivo: Boolean, alActivar: () -> Unit) {
    NeuTarjeta {
        Text(
            text = if (filtroActivo) "Filtro activo" else "Filtro desactivado",
            style = MaterialTheme.typography.headlineMedium,
            color = if (filtroActivo) ColoresNeu.permitir else ColoresNeu.bloqueo
        )
        Text(
            text = if (filtroActivo) {
                "Las llamadas que coincidan con tus reglas se bloquean solas."
            } else {
                "Ahora mismo entran todas las llamadas. Actívalo para que se apliquen tus reglas."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = ColoresNeu.texto
        )
        if (!filtroActivo) {
            NeuBoton(
                texto = "Activar filtro",
                alPulsar = alActivar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

/**
 * Resumen del día (solo llamadas reales): "Bloqueadas hoy: N" grande y
 * "Permitidas hoy: N" debajo; si no hay ninguna, "Aún no hay llamadas hoy".
 */
@Composable
private fun TarjetaResumenHoy(resumen: RepositorioRegistro.Estadisticas?) {
    NeuTarjeta {
        Text(text = "Hoy", style = MaterialTheme.typography.titleMedium, color = ColoresNeu.texto)
        val bloqueadas = resumen?.bloqueadasHoy ?: 0
        val permitidas = resumen?.permitidasHoy ?: 0
        if (bloqueadas + permitidas == 0) {
            Text(
                text = "Aún no hay llamadas hoy",
                style = MaterialTheme.typography.bodyLarge,
                color = ColoresNeu.textoSecundario
            )
        } else {
            Text(
                text = "Bloqueadas hoy: $bloqueadas",
                style = MaterialTheme.typography.headlineSmall,
                color = ColoresNeu.bloqueo
            )
            Text(
                text = "Permitidas hoy: $permitidas",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = ColoresNeu.permitir
            )
        }
    }
}
