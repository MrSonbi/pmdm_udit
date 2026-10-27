package com.example.filtro400

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.filtro400.data.RepositorioReglas
import com.example.filtro400.datos.Preferencias
import com.example.filtro400.motor.Decision
import com.example.filtro400.motor.MotorFiltrado
import com.example.filtro400.registro.OrigenLlamada
import com.example.filtro400.registro.RepositorioRegistro
import com.example.filtro400.util.Enmascarar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// CallScreeningService: Android nos avisa de cada llamada entrante
// ANTES de que suene, y nosotros decidimos qué hacer con ella.
// Solo funciona con números que NO están en la agenda del usuario.
class FiltroLlamadasService : CallScreeningService() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        var bloquear = false
        var numero = ""
        var decision: Decision? = null
        var esEntrante = false

        // Todo va dentro de try/catch y ante cualquier fallo PERMITIMOS la
        // llamada (fail-open). Es preferible dejar pasar una llamada que
        // romper el servicio de telefonía.
        try {
            // Solo filtramos llamadas entrantes (no las que haces tú).
            if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
                esEntrante = true
                // handle = "tel:+34400123456"; schemeSpecificPart = "+34400123456".
                // Si el número está oculto viene vacío y el motor lo permite.
                numero = callDetails.handle?.schemeSpecificPart.orEmpty()

                // No consultamos SQLite aquí: el repositorio devuelve su copia
                // en memoria de las reglas activas (se carga la primera vez).
                val reglasActivas = RepositorioReglas
                    .obtener(applicationContext)
                    .reglasActivas()

                decision = MotorFiltrado.evaluar(numero, reglasActivas)
                bloquear = decision.bloquea
            }
        } catch (t: Throwable) {
            bloquear = false
            decision = null
        }

        // RESPONDER PRIMERO. Siempre hay que responder, también cuando dejamos
        // pasar la llamada. Se construye y se envía una única respuesta.
        try {
            val respuesta = CallResponse.Builder()
                .setDisallowCall(bloquear)      // impide que suene
                .setRejectCall(bloquear)        // la rechaza
                .setSkipNotification(bloquear)  // sin notificación de llamada perdida
                .build()

            respondToCall(callDetails, respuesta)
        } catch (t: Throwable) {
            Log.w("Filtro400", "No se pudo responder a la llamada", t)
        }

        // Loguear/registrar va DESPUÉS de responder, dentro de try/catch.
        // Nunca el número completo: un teléfono es un dato personal.
        // El ajuste "guardar registro" se lee de SharedPreferences.
        if (esEntrante) {
            try {
                Log.d(
                    "Filtro400",
                    "Llamada ${if (bloquear) "BLOQUEADA" else "PERMITIDA"}: ${Enmascarar.numero(numero)}"
                )
            } catch (t: Throwable) {
                // El log nunca debe afectar al resultado.
            }

            try {
                if (Preferencias.guardarRegistroActivado(applicationContext)) {
                    val dec = decision
                    alcance.launch {
                        runCatching {
                            RepositorioRegistro.obtener(applicationContext).insertar(
                                numero = numero,
                                decision = if (bloquear) com.example.filtro400.modelo.Accion.BLOQUEAR else com.example.filtro400.modelo.Accion.PERMITIR,
                                reglaId = dec?.regla?.id,
                                reglaPatron = dec?.regla?.patron,
                                reglaTipo = dec?.regla?.tipo,
                                reglaPrioridad = dec?.regla?.prioridad,
                                motivo = dec?.motivo,
                                origen = OrigenLlamada.REAL
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                // Un fallo de registro no puede afectar a la decisión.
            }
        }
    }
}

