package com.example.filtro400.ui

import android.database.sqlite.SQLiteConstraintException
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.filtro400.ReglaBloqueo
import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.Regla
import com.example.filtro400.modelo.TipoRegla
import com.example.filtro400.motor.ValidacionReglas
import com.example.filtro400.ui.tema.CampoHundido
import com.example.filtro400.ui.tema.ColoresNeu
import com.example.filtro400.ui.tema.FilaDesplegable
import com.example.filtro400.ui.tema.NeuBoton
import com.example.filtro400.ui.tema.NeuConfirmacion
import com.example.filtro400.ui.tema.NeuDialogo
import com.example.filtro400.ui.tema.SelectorSegmentado
import kotlinx.coroutines.launch

/** Opciones del selector "¿Qué quieres hacer?", en el orden en que se muestran. */
private val ACCIONES = listOf(Accion.BLOQUEAR, Accion.PERMITIR)

/** Opciones del selector "¿A qué llamadas?", en el orden en que se muestran. */
private val TIPOS = listOf(TipoRegla.PREFIJO, TipoRegla.NUMERO)

/**
 * Diálogo de alta/edición de una regla, en UNA sola pantalla y con preguntas
 * en lenguaje llano:
 *  1) ¿Qué quieres hacer? -> Bloquear / Permitir
 *  2) ¿A qué llamadas?    -> Empiezan por / Un número
 *  3) el prefijo o número (campo hundido, con el error de validación debajo)
 *  4) etiqueta opcional
 *  5) "Opciones avanzadas" (plegado): la prioridad.
 *
 * La validación es la función pura de siempre ([ValidacionReglas.validar]). Si el
 * prefijo es muy corto (2 dígitos o menos) se pide una segunda confirmación.
 *
 * En edición aparece "Eliminar regla": [alEliminar] cierra este diálogo y
 * [PantallaInicio] pide la confirmación. Así TODA regla se puede eliminar.
 */
@Composable
fun DialogoRegla(
    regla: Regla?,
    patronesExistentes: List<String>,
    alCerrar: () -> Unit,
    alGuardar: suspend (patron: String, etiqueta: String?, tipo: TipoRegla, accion: Accion, prioridad: Int) -> Unit,
    alEliminar: (() -> Unit)? = null
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
    // Error del patrón o del guardado (bajo el patrón) y error de la prioridad
    // (bajo la prioridad): así el usuario sabe qué corregir.
    var error by remember(claveEstado) { mutableStateOf<String?>(null) }
    var errorPrioridad by remember(claveEstado) { mutableStateOf<String?>(null) }
    var guardando by remember(claveEstado) { mutableStateOf(false) }
    var mostrarConfirmacion by remember(claveEstado) { mutableStateOf(false) }
    var patronPendiente by remember(claveEstado) { mutableStateOf<String?>(null) }
    // Plegado por defecto: la mayoría de la gente no necesita tocar la prioridad.
    var verAvanzadas by remember(claveEstado) { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    fun actualizarPrioridadPorDefecto() {
        // Solo en altas: en edición se respeta la prioridad que ya tenía la regla.
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
        // Prioridad vacía -> la de por defecto; texto que no es un número -> error.
        val textoPrioridad = prioridadTexto.trim()
        val prioridad = textoPrioridad.toIntOrNull()
        if (textoPrioridad.isNotEmpty() && prioridad == null) {
            errorPrioridad = "La prioridad debe ser un número entero entre 0 y 100."
            verAvanzadas = true // el error nunca queda escondido en la parte plegada
            return
        }
        if (prioridad != null && prioridad !in 0..100) {
            errorPrioridad = "La prioridad debe estar entre 0 y 100."
            verAvanzadas = true
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

    // NeuDialogo: el contenido va en una Column con verticalScroll y los botones
    // Cancelar / Guardar quedan FIJOS abajo, fuera del área desplazable y con
    // imePadding: siempre se alcanzan, también con el teclado abierto.
    // Tocar fuera NO cierra (no se pierde lo escrito); Atrás o "Cancelar" sí.
    NeuDialogo(
        titulo = if (regla == null) "Añadir regla" else "Editar regla",
        alCerrar = { if (!guardando) alCerrar() },
        botones = {
            // Guardar es la acción principal; Cancelar, la secundaria. Cada botón
            // tiene weight(1f): ninguno puede empujar al otro fuera de la fila.
            NeuBoton(
                texto = "Cancelar",
                alPulsar = alCerrar,
                primario = false,
                habilitado = !guardando,
                modifier = Modifier.weight(1f)
            )
            NeuBoton(
                texto = if (guardando) "Guardando…" else "Guardar",
                alPulsar = { intentarGuardar() },
                habilitado = !guardando,
                modifier = Modifier.weight(1f)
            )
        }
    ) {
        Pregunta("¿Qué quieres hacer?")
        SelectorSegmentado(
            opciones = listOf("Bloquear", "Permitir"),
            seleccionada = ACCIONES.indexOf(accionSeleccionada),
            alSeleccionar = {
                accionSeleccionada = ACCIONES[it]
                actualizarPrioridadPorDefecto()
            },
            habilitado = !guardando
        )

        Pregunta("¿A qué llamadas?")
        SelectorSegmentado(
            opciones = listOf("Empiezan por", "Un número"),
            seleccionada = TIPOS.indexOf(tipoSeleccionado),
            alSeleccionar = { tipoSeleccionado = TIPOS[it] },
            habilitado = !guardando
        )

        CampoHundido(
            valor = textoPatron,
            alCambiar = {
                textoPatron = it
                error = null
            },
            etiqueta = if (tipoSeleccionado == TipoRegla.NUMERO) "Número" else "Prefijo",
            ejemplo = if (tipoSeleccionado == TipoRegla.NUMERO) "Ej. 612 345 678" else "Ej. 400",
            error = error,
            teclado = KeyboardOptions(keyboardType = KeyboardType.Phone),
            habilitado = !guardando
        )

        // Vista previa en lenguaje llano de lo que hará la regla.
        val vistaPrevia = ReglaBloqueo.normalizar(textoPatron)
        if (vistaPrevia.isNotEmpty() && error == null) {
            Text(
                text = fraseRegla(tipoSeleccionado, accionSeleccionada, vistaPrevia) + ".",
                style = MaterialTheme.typography.bodyLarge,
                color = ColoresNeu.textoSecundario
            )
        }

        CampoHundido(
            valor = textoEtiqueta,
            alCambiar = { textoEtiqueta = it },
            etiqueta = "Nombre (opcional)",
            ejemplo = "Ej. Publicidad",
            habilitado = !guardando
        )

        FilaDesplegable(
            titulo = "Opciones avanzadas",
            abierta = verAvanzadas,
            alCambiar = { verAvanzadas = !verAvanzadas }
        )
        if (verAvanzadas) {
            CampoHundido(
                valor = prioridadTexto,
                alCambiar = {
                    prioridadTexto = it
                    errorPrioridad = null
                },
                etiqueta = "Prioridad (de 0 a 100)",
                error = errorPrioridad,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
                habilitado = !guardando
            )
            Text(
                text = "Si varias reglas coinciden, gana la de prioridad más alta. " +
                    "Por defecto: 0 al bloquear y 100 al permitir siempre. " +
                    "Si la dejas vacía, se usa la de por defecto. " +
                    "El número se guarda sin espacios, guiones ni +34.",
                style = MaterialTheme.typography.bodySmall,
                color = ColoresNeu.textoSecundario
            )
        }

        // "Eliminar regla" (solo en edición) va dentro del área desplazable,
        // al final: no es una acción de este formulario, sino otra distinta.
        if (regla != null && alEliminar != null) {
            NeuBoton(
                texto = "Eliminar regla",
                alPulsar = alEliminar,
                primario = false,
                colorTexto = ColoresNeu.bloqueo,
                habilitado = !guardando,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (mostrarConfirmacion) {
        val patron = patronPendiente.orEmpty()
        val prioridadConfirmacion = prioridadTexto.trim().toIntOrNull()
            ?: ValidacionReglas.prioridadPorDefecto(accionSeleccionada)
        NeuConfirmacion(
            titulo = "¿Guardar un prefijo tan corto?",
            texto = "«$patron» tiene 2 dígitos o menos: afectaría a muchísimas llamadas.",
            textoConfirmar = "Guardar",
            alConfirmar = {
                mostrarConfirmacion = false
                guardar(patron, prioridadConfirmacion)
            },
            alCancelar = { mostrarConfirmacion = false }
        )
    }
}

/** Pregunta de una sección del diálogo (subtítulo de 18 sp). */
@Composable
private fun Pregunta(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.titleMedium)
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
