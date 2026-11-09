package com.example.filtro400.ui.tema

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Colores de [PaletaNeu] ya convertidos a [Color] de Compose.
 * Las pantallas usan estos nombres en vez de escribir hexadecimales.
 */
object ColoresNeu {
    val fondo = Color(PaletaNeu.FONDO)
    val sombraClara = Color(PaletaNeu.SOMBRA_CLARA)
    val sombraOscura = Color(PaletaNeu.SOMBRA_OSCURA)
    val texto = Color(PaletaNeu.TEXTO)
    val textoSecundario = Color(PaletaNeu.TEXTO_SECUNDARIO)
    val acento = Color(PaletaNeu.ACENTO)
    val bloqueo = Color(PaletaNeu.BLOQUEO)
    val permitir = Color(PaletaNeu.PERMITIR)
    val blanco = Color(PaletaNeu.BLANCO)
    val superficie = Color(PaletaNeu.SUPERFICIE)
    val simulada = Color(PaletaNeu.SIMULADA)
    val seleccion = Color(PaletaNeu.SELECCION)
    val fondoHundido = Color(PaletaNeu.FONDO_HUNDIDO)
}

/**
 * Esquema de Material 3 construido SOLO con la paleta neumórfica. Así los
 * componentes Material que se siguen usando (Switch, diálogos, tarjetas de las
 * pestañas no rediseñadas) heredan colores con contraste comprobado.
 * Sin color dinámico de Android 12: la app se ve igual en todos los móviles.
 */
private val esquemaClaro = lightColorScheme(
    primary = ColoresNeu.acento,
    onPrimary = ColoresNeu.blanco,
    primaryContainer = ColoresNeu.seleccion,
    onPrimaryContainer = ColoresNeu.texto,
    secondary = ColoresNeu.acento,
    onSecondary = ColoresNeu.blanco,
    secondaryContainer = ColoresNeu.seleccion,
    onSecondaryContainer = ColoresNeu.texto,
    tertiary = ColoresNeu.permitir,
    onTertiary = ColoresNeu.blanco,
    tertiaryContainer = ColoresNeu.simulada,
    onTertiaryContainer = ColoresNeu.texto,
    error = ColoresNeu.bloqueo,
    onError = ColoresNeu.blanco,
    background = ColoresNeu.fondo,
    onBackground = ColoresNeu.texto,
    surface = ColoresNeu.fondo,
    onSurface = ColoresNeu.texto,
    surfaceVariant = ColoresNeu.fondo,
    onSurfaceVariant = ColoresNeu.textoSecundario,
    surfaceTint = Color.Transparent,
    // Diálogos y barra inferior: el mismo fondo, sin tinte.
    surfaceContainerLowest = ColoresNeu.fondo,
    surfaceContainerLow = ColoresNeu.fondo,
    surfaceContainer = ColoresNeu.fondo,
    surfaceContainerHigh = ColoresNeu.fondo,
    // Tarjetas Material (Card): ya no se usan en ui/ (todo es NeuTarjeta), se
    // deja por si algún componente Material lo necesita.
    surfaceContainerHighest = ColoresNeu.superficie,
    // Bordes y pista del Switch apagado: el gris secundario (contraste >= 3:1).
    outline = ColoresNeu.textoSecundario,
    outlineVariant = ColoresNeu.sombraOscura
)

private fun estilo(tamano: TextUnit, peso: FontWeight = FontWeight.Normal): TextStyle =
    TextStyle(
        fontSize = tamano,
        // Interlineado generoso: el texto respira y se lee mejor.
        lineHeight = (tamano.value * 1.35f).sp,
        fontWeight = peso
    )

/**
 * Tamaños de texto de la app:
 *  - título 28 sp (titleLarge / headline*),
 *  - subtítulo 18 sp (titleMedium),
 *  - cuerpo 16 sp (body*Large/Medium, titleSmall, labelLarge),
 *  - etiquetas 14 sp como mínimo (bodySmall, labelMedium, labelSmall).
 * Ningún estilo baja de 14 sp. Única excepción, fuera de la tipografía: los
 * textos de una sola línea (TextoUnaLinea: botones, pestañas, selector) pueden
 * reducirse hasta 12 sp SOLO si no caben, en vez de partirse o salir con "…".
 */
private val tipografiaNeu = Typography(
    displayLarge = estilo(40.sp, FontWeight.Bold),
    displayMedium = estilo(36.sp, FontWeight.Bold),
    displaySmall = estilo(32.sp, FontWeight.Bold),
    headlineLarge = estilo(28.sp, FontWeight.Bold),
    headlineMedium = estilo(28.sp, FontWeight.Bold),
    headlineSmall = estilo(28.sp, FontWeight.SemiBold),
    titleLarge = estilo(28.sp, FontWeight.SemiBold),
    titleMedium = estilo(18.sp, FontWeight.SemiBold),
    titleSmall = estilo(16.sp, FontWeight.SemiBold),
    bodyLarge = estilo(16.sp),
    bodyMedium = estilo(16.sp),
    bodySmall = estilo(14.sp),
    labelLarge = estilo(16.sp, FontWeight.Medium),
    labelMedium = estilo(14.sp, FontWeight.Medium),
    labelSmall = estilo(14.sp, FontWeight.Medium)
)

/**
 * Tema de la app. Pinta el fondo neumórfico en TODA la pantalla, de modo que
 * cualquier pestaña (rediseñada o no) queda sobre el mismo gris suave.
 * Por ahora solo hay modo claro (el modo oscuro queda fuera de esta etapa).
 */
@Composable
fun TemaNeu(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = esquemaClaro,
        typography = tipografiaNeu
    ) {
        // Surface pinta el fondo y fija el color de texto por defecto (LocalContentColor).
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ColoresNeu.fondo,
            contentColor = ColoresNeu.texto
        ) {
            contenido()
        }
    }
}
