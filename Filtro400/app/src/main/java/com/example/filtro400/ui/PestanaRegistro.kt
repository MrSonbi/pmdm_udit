package com.example.filtro400.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.filtro400.modelo.Accion
import com.example.filtro400.registro.OrigenLlamada
import com.example.filtro400.registro.RegistroLlamada
import com.example.filtro400.registro.RepositorioRegistro
import com.example.filtro400.ui.tema.ColoresNeu
import com.example.filtro400.ui.tema.NeuCargando
import com.example.filtro400.ui.tema.NeuConfirmacion
import com.example.filtro400.ui.tema.NeuTarjeta
import com.example.filtro400.ui.tema.SelectorSegmentado
import com.example.filtro400.ui.tema.neuHundido
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Cuántas filas recientes enseña la vista Registro. */
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

/** "09/11/2026, 13:52" en la zona horaria del móvil. */
private val formatoFecha: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy, HH:mm", Locale.forLanguageTag("es-ES"))
    .withZone(ZoneId.systemDefault())

/** Margen de las listas: deja sitio a las sombras neumórficas. */
internal val MARGEN_LISTA = PaddingValues(horizontal = 20.dp, vertical = 20.dp)

/** Separación entre tarjetas de una lista. */
internal val SEPARACION_LISTA = 18.dp

/**
 * Vista Registro (dentro de Actividad): últimas llamadas, la más reciente
 * primero, cada una en una NeuTarjeta, con el selector Todas | Bloqueadas |
 * Permitidas. "Borrar registro" ya no está aquí: solo en Ajustes > Privacidad.
 *
 * Las pruebas (filas SIMULADAS) solo aparecen con "Mostrar pruebas" y llevan
 * la marca "Prueba". [cabecera] son los elementos de Actividad (selector de
 * vista e interruptor), que van al principio de la misma lista desplazable.
 *
 * [refresco] es un contador: cada vez que cambia se vuelve a leer la BD.
 * Las lecturas van en Dispatchers.IO dentro de [RepositorioRegistro].
 */
@Composable
fun PestanaRegistro(
    repositorioRegistro: RepositorioRegistro,
    refresco: Int,
    guardarRegistro: Boolean,
    incluirSimulaciones: Boolean,
    cabecera: LazyListScope.() -> Unit,
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
            error = "No se pudo leer el registro. Vuelve a intentarlo más tarde."
        }
    }

    val visibles = filas.filter { filtro.acepta(it) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = MARGEN_LISTA,
        verticalArrangement = Arrangement.spacedBy(SEPARACION_LISTA)
    ) {
        cabecera()
        item {
            TituloVista(
                titulo = "Registro",
                subtitulo = "Últimas $LIMITE_RECIENTES llamadas, la más reciente primero. " +
                    "Solo se guarda el número enmascarado."
            )
        }
        item {
            SelectorSegmentado(
                opciones = FiltroDecision.entries.map { it.texto },
                seleccionada = filtro.ordinal,
                alSeleccionar = { filtro = FiltroDecision.entries[it] }
            )
        }
        if (!guardarRegistro) {
            item {
                TarjetaAviso("El registro está desactivado. Puedes activarlo en Ajustes > Privacidad.")
            }
        }

        when {
            cargando -> item { NeuCargando() }

            error != null -> item { TarjetaAviso(error.orEmpty(), esError = true) }

            filas.isEmpty() -> item {
                TarjetaAviso("Aún no hay llamadas. Aquí aparecerán las que filtre la app.")
            }

            visibles.isEmpty() -> item {
                TarjetaAviso(
                    "No hay llamadas ${filtro.texto.lowercase()} entre las últimas $LIMITE_RECIENTES."
                )
            }

            else -> items(visibles, key = { it.id }) { fila -> TarjetaLlamada(fila) }
        }
    }
}

/**
 * Una llamada: el número enmascarado grande, el estado ESCRITO ("Bloqueada" /
 * "Permitida") en su color, la fecha y hora, la regla aplicada y, si es una
 * simulación, la marca "Prueba". Todo en una Column: no hay filas que se
 * puedan desbordar.
 */
@Composable
private fun TarjetaLlamada(fila: RegistroLlamada) {
    val bloqueada = fila.decision == Accion.BLOQUEAR
    NeuTarjeta {
        Text(
            text = fila.numeroEnmascarado,
            style = MaterialTheme.typography.titleLarge,
            color = ColoresNeu.texto
        )
        Text(
            text = if (bloqueada) "Bloqueada" else "Permitida",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (bloqueada) ColoresNeu.bloqueo else ColoresNeu.permitir
        )
        Text(
            text = formatoFecha.format(Instant.ofEpochMilli(fila.fecha)),
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresNeu.textoSecundario
        )
        Text(
            text = textoReglaAplicada(fila.reglaPatron),
            style = MaterialTheme.typography.bodyMedium,
            color = ColoresNeu.texto
        )
        if (fila.origen == OrigenLlamada.SIMULADA) {
            // Marca hundida con texto: se distingue sin depender del color.
            Text(
                text = "Prueba",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ColoresNeu.acento,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .neuHundido(radio = 10.dp, profundidad = 2.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

/** Título de una vista y una frase corta debajo. */
@Composable
internal fun TituloVista(titulo: String, subtitulo: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = titulo, style = MaterialTheme.typography.titleLarge, color = ColoresNeu.texto)
        if (subtitulo != null) {
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyLarge,
                color = ColoresNeu.textoSecundario
            )
        }
    }
}

/** NeuTarjeta de texto para estados vacíos, avisos y errores. */
@Composable
internal fun TarjetaAviso(texto: String, esError: Boolean = false) {
    NeuTarjeta {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = if (esError) ColoresNeu.bloqueo else ColoresNeu.texto
        )
    }
}

/** Confirmación de "Borrar registro" (se abre desde Ajustes > Privacidad). */
@Composable
internal fun DialogoBorrarRegistro(alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    NeuConfirmacion(
        titulo = "Borrar registro",
        texto = "Se borrarán todas las llamadas del registro, también las pruebas. " +
            "No se puede deshacer. Tus reglas no cambian.",
        textoConfirmar = "Borrar",
        alConfirmar = alConfirmar,
        alCancelar = alCancelar,
        peligrosa = true
    )
}
