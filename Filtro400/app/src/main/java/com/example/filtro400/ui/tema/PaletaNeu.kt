package com.example.filtro400.ui.tema

import kotlin.math.pow

/**
 * Paleta del estilo neumórfico ("soft UI"), modo claro.
 *
 * Kotlin PURO: los colores son Long en formato ARGB (0xAARRGGBB) y no se importa
 * nada de Android ni de Compose. Así el contraste se comprueba con JUnit en el
 * ordenador (ver PaletaNeuTest). El tema de Compose convierte estos Long en Color.
 *
 * Todos los pares texto/fondo cumplen WCAG AA (>= 4.5:1). Con los valores pedidos
 * no ha hecho falta oscurecer ninguno; si se cambia un color y deja de llegar,
 * [oscurecerHasta] da el valor más cercano que sí llega.
 */
object PaletaNeu {

    /** Fondo de toda la app: un gris azulado suave que no cansa la vista. */
    const val FONDO: Long = 0xFFE6EBF2

    /** Sombra clara (arriba-izquierda): simula la luz que llega a la pieza. */
    const val SOMBRA_CLARA: Long = 0xFFFFFFFF

    /** Sombra oscura (abajo-derecha): la pieza "flota" sobre el fondo. */
    const val SOMBRA_OSCURA: Long = 0xFFA9B4C8

    /** Texto principal. */
    const val TEXTO: Long = 0xFF1F2A3C

    /** Texto secundario (etiquetas, explicaciones). */
    const val TEXTO_SECUNDARIO: Long = 0xFF4A5568

    /** Acento: botón principal, opción seleccionada, pestaña activa. */
    const val ACENTO: Long = 0xFF3B5BA9

    /** Bloqueo y errores. */
    const val BLOQUEO: Long = 0xFFB3402F

    /** Permitir (lista blanca). */
    const val PERMITIR: Long = 0xFF26704F

    /** Texto sobre el acento (botón principal). */
    const val BLANCO: Long = 0xFFFFFFFF

    /**
     * Superficie de las tarjetas Material que aún no se han rediseñado (Inicio,
     * Registro, Estadísticas y Ajustes): un poco más clara que el fondo para que se
     * distingan sin sombras. Al ser más clara, el contraste del texto sube.
     */
    const val SUPERFICIE: Long = 0xFFEEF2F7

    /** Fondo de las filas SIMULADAS del Registro (tono crema, distinto del resto). */
    const val SIMULADA: Long = 0xFFF5F0E1

    /** Fondo de un chip Material seleccionado (pestaña Registro). */
    const val SELECCION: Long = 0xFFD5DCE8

    /**
     * Tono más oscuro del degradado de las piezas hundidas (campos, opción activa
     * del selector, pestaña activa). El texto en acento se lee sobre él.
     */
    val FONDO_HUNDIDO: Long = oscurecer(FONDO, 0.95)

    /**
     * Luminancia relativa WCAG 2.x de un color ARGB (se ignora el canal alfa).
     * Resultado entre 0 (negro) y 1 (blanco).
     */
    fun luminancia(color: Long): Double {
        fun canal(desplazamiento: Int): Double {
            val c = ((color shr desplazamiento) and 0xFF).toDouble() / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * canal(16) + 0.7152 * canal(8) + 0.0722 * canal(0)
    }

    /**
     * Contraste WCAG entre dos colores: (L1 + 0.05) / (L2 + 0.05), con L1 la
     * luminancia mayor. Va de 1:1 (iguales) a 21:1 (negro sobre blanco).
     * Es simétrica: contraste(a, b) == contraste(b, a).
     */
    fun contraste(a: Long, b: Long): Double {
        val la = luminancia(a)
        val lb = luminancia(b)
        val clara = maxOf(la, lb)
        val oscura = minOf(la, lb)
        return (clara + 0.05) / (oscura + 0.05)
    }

    /**
     * Oscurece [color] multiplicando cada canal RGB por [factor] (0..1).
     * El alfa se conserva.
     */
    fun oscurecer(color: Long, factor: Double): Long {
        fun canal(desplazamiento: Int): Long {
            val c = (color shr desplazamiento) and 0xFF
            return (c * factor).toLong().coerceIn(0L, 255L)
        }
        val alfa = (color shr 24) and 0xFF
        return (alfa shl 24) or (canal(16) shl 16) or (canal(8) shl 8) or canal(0)
    }

    /**
     * Devuelve [color] tal cual si ya llega a [minimo] sobre [fondo]; si no, lo
     * oscurece poco a poco (2 % cada paso) hasta que llegue. Sirve para corregir
     * un color de texto sin cambiar su tono.
     */
    fun oscurecerHasta(color: Long, fondo: Long, minimo: Double = 4.5): Long {
        var actual = color
        var pasos = 0
        while (contraste(actual, fondo) < minimo && pasos < 200) {
            actual = oscurecer(actual, 0.98)
            pasos++
        }
        return actual
    }
}
