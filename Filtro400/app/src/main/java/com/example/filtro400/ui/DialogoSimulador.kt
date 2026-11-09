package com.example.filtro400.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.example.filtro400.data.RepositorioReglas
import com.example.filtro400.motor.Decision
import com.example.filtro400.motor.MotorFiltrado
import com.example.filtro400.registro.OrigenLlamada
import com.example.filtro400.registro.RepositorioRegistro
import com.example.filtro400.ui.tema.CampoHundido
import com.example.filtro400.ui.tema.ColoresNeu
import com.example.filtro400.ui.tema.FilaInterruptor
import com.example.filtro400.ui.tema.NeuBoton
import com.example.filtro400.ui.tema.NeuDialogo
import com.example.filtro400.ui.tema.NeuTarjeta
import com.example.filtro400.util.Enmascarar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Simulador ("Probar un número") en un diálogo con el mismo estilo que el de
 * reglas: campo hundido, interruptor "Guardar en el registro", y los botones
 * Cerrar / Simular fijos abajo.
 *
 * Usa el MISMO motor puro ([MotorFiltrado.evaluar]) y la misma caché de reglas
 * activas que el servicio. El resultado se explica con una frase llana
 * ([fraseSimulacion], probada con JUnit) en el color de bloqueo o de permitir.
 *
 * Nunca se muestra el número normalizado: solo cómo quedaría ENMASCARADO.
 * [alGuardada] avisa a [PantallaInicio] para que Actividad relea el registro.
 */
@Composable
fun DialogoSimulador(
    repositorio: RepositorioReglas,
    repositorioRegistro: RepositorioRegistro,
    alCerrar: () -> Unit,
    alGuardada: () -> Unit
) {
    val alcance = rememberCoroutineScope()
    var numero by remember { mutableStateOf("") }
    // Número con el que se calculó el resultado (la frase depende de él).
    var numeroSimulado by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf<Decision?>(null) }
    // APAGADO por defecto: no se llena el registro con pruebas sin querer.
    var guardarEnRegistro by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var simulando by remember { mutableStateOf(false) }

    fun simular() {
        if (simulando) return
        val tecleado = numero
        val guardar = guardarEnRegistro
        simulando = true
        alcance.launch {
            // Caché de reglas activas: solo la primera vez hace SQL (por eso, IO).
            val consulta = withContext(Dispatchers.IO) {
                runCatching { repositorio.reglasActivas() }
            }
            val reglasActivas = consulta.getOrElse {
                aviso = "No se pudieron leer las reglas activas. Inténtalo de nuevo."
                resultado = null
                simulando = false
                return@launch
            }
            val decision = MotorFiltrado.evaluar(tecleado, reglasActivas)
            resultado = decision
            numeroSimulado = tecleado
            aviso = null

            if (guardar) {
                // insertar() enmascara el número antes de guardarlo (y va en IO).
                val guardado = runCatching {
                    repositorioRegistro.insertar(
                        numero = tecleado,
                        decision = decision.accion,
                        reglaId = decision.regla?.id,
                        reglaPatron = decision.regla?.patron,
                        reglaTipo = decision.regla?.tipo,
                        reglaPrioridad = decision.regla?.prioridad,
                        motivo = decision.motivo,
                        origen = OrigenLlamada.SIMULADA
                    )
                }
                aviso = if (guardado.isSuccess) {
                    alGuardada()
                    "Guardada como prueba. Se ve en Actividad con «Mostrar pruebas (simulaciones)»."
                } else {
                    "No se pudo guardar la prueba en el registro."
                }
            }
            simulando = false
        }
    }

    NeuDialogo(
        titulo = "Probar un número",
        alCerrar = alCerrar,
        botones = {
            NeuBoton(
                texto = "Cerrar",
                alPulsar = alCerrar,
                primario = false,
                modifier = Modifier.weight(1f)
            )
            NeuBoton(
                texto = "Simular",
                alPulsar = { simular() },
                habilitado = !simulando,
                modifier = Modifier.weight(1f)
            )
        }
    ) {
        Text(
            text = "Mira qué haría el filtro con un número, usando tus reglas activas. " +
                "No se hace ninguna llamada.",
            style = MaterialTheme.typography.bodyLarge,
            color = ColoresNeu.textoSecundario
        )
        CampoHundido(
            valor = numero,
            alCambiar = {
                numero = it
                // El resultado anterior ya no corresponde al número escrito.
                resultado = null
                aviso = null
            },
            etiqueta = "Número de teléfono",
            ejemplo = "Ej. 400 555 123",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        FilaInterruptor(
            titulo = "Guardar en el registro",
            explicacion = "Se guarda como prueba, con el número enmascarado.",
            activo = guardarEnRegistro,
            alCambiar = { guardarEnRegistro = it },
            textoActivo = "Sí",
            textoInactivo = "No"
        )

        resultado?.let { decision ->
            NeuTarjeta {
                // El color refuerza; lo que manda es la frase.
                Text(
                    text = if (decision.bloquea) "Se bloquearía" else "Entraría",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (decision.bloquea) ColoresNeu.bloqueo else ColoresNeu.permitir
                )
                Text(
                    text = fraseSimulacion(numeroSimulado, decision),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (decision.bloquea) ColoresNeu.bloqueo else ColoresNeu.permitir
                )
                Text(
                    text = "En el registro aparecería como ${Enmascarar.numero(numeroSimulado)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColoresNeu.textoSecundario
                )
            }
        }
        aviso?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = ColoresNeu.texto)
        }
    }
}
