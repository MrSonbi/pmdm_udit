package com.example.filtro400

// "object" = una única instancia, sin necesidad de crearla con ReglaBloqueo().
// Aquí vive la normalización que comparten la UI, el motor, el servicio y el CSV.
object ReglaBloqueo {

    // Un mismo número puede llegar de varias formas:
    // "400123456", "+34400123456", "0034 400 123 456", "+34 400 123 456"...
    // Lo dejamos siempre en el mismo formato antes de comparar.
    fun normalizar(numero: String): String {
        // Nos quedamos solo con dígitos y el signo "+" (fuera espacios y guiones).
        val limpio = numero.filter { it.isDigit() || it == '+' }
        return when {
            // Quitamos primero +34 / 0034 para que un número español quede igual
            // se escriba como se escriba. El orden importa: "0034" empieza por "00".
            limpio.startsWith("+34") -> limpio.removePrefix("+34")
            limpio.startsWith("0034") -> limpio.removePrefix("0034")
            // "00" inicial es el prefijo internacional antiguo: 0044 = +44.
            limpio.startsWith("00") -> "+" + limpio.removePrefix("00")
            else -> limpio
        }
    }
}