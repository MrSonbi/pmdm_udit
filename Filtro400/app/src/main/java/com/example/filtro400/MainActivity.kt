package com.example.filtro400

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.filtro400.ui.PantallaInicio
import com.example.filtro400.ui.theme.Filtro400Theme

/**
 * MainActivity solo monta la pantalla. La lógica y los composables viven en `ui/`
 * para que la actividad no se convierta en un archivo gigante.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Filtro400Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaInicio()
                }
            }
        }
    }
}
