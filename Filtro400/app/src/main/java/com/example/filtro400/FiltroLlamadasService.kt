package com.example.filtro400

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.filtro400.data.RepositorioReglas
import com.example.filtro400.motor.MotorFiltrado
import com.example.filtro400.util.Enmascarar

// CallScreeningService: Android nos avisa de cada llamada entrante
// ANTES de que suene, y nosotros decidimos qué hacer con ella.
// Solo funciona con números que NO están en la agenda del usuario.
class FiltroLlamadasService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        var bloquear = false
        var numero = ""

        // Todo va dentro de try/catch y ante cualquier fallo PERMITIMOS la
        // llamada (fail-open). Es preferible dejar pasar una llamada que
        // romper el servicio de telefonía.
        try {
            // Solo filtramos llamadas entrantes (no las que haces tú).
            if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
                // handle = "tel:+34400123456"; schemeSpecificPart = "+34400123456".
                // Si el número está oculto viene vacío y el motor lo permite.
                numero = callDetails.handle?.schemeSpecificPart.orEmpty()

                // No consultamos SQLite aquí: el repositorio devuelve su copia
                // en memoria de las reglas activas (se carga la primera vez).
                val reglasActivas = RepositorioReglas
                    .obtener(applicationContext)
                    .reglasActivas()

                bloquear = MotorFiltrado.evaluar(numero, reglasActivas).bloquea
            }
        } catch (t: Throwable) {
            bloquear = false
        }

        // Siempre hay que responder, también cuando dejamos pasar la llamada.
        // Se construye y se envía una única respuesta, pase lo que pase.
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

        // Loguear/registrar va DESPUÉS de responder. Nunca el número completo:
        // un teléfono es un dato personal, aunque sea en Logcat.
        Log.d(
            "Filtro400",
            "Llamada ${if (bloquear) "BLOQUEADA" else "PERMITIDA"}: ${Enmascarar.numero(numero)}"
        )
    }
}
