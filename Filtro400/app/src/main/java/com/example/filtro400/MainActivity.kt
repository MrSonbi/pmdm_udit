package com.example.filtro400

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.filtro400.ui.PantallaInicio
import com.example.filtro400.ui.tema.TemaNeu

/**
 * MainActivity solo monta la pantalla. La lógica y los composables viven en `ui/`
 * para que la actividad no se convierta en un archivo gigante.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // TemaNeu pinta el fondo neumórfico en toda la app (ui/tema/).
            TemaNeu {
                PantallaInicio()
            }
        }
    }
}
