package com.example.filtro400.ui

import com.example.filtro400.modelo.Accion
import com.example.filtro400.modelo.TipoRegla

/*
 * Textos legibles para los enums del modelo. En pantalla no se enseñan los
 * nombres internos (PREFIJO, BLOQUEAR...), sino palabras en español normal.
 */

internal fun textoTipo(tipo: TipoRegla): String = when (tipo) {
    TipoRegla.PREFIJO -> "Prefijo"
    TipoRegla.NUMERO -> "Número exacto"
}

internal fun textoAccion(accion: Accion): String = when (accion) {
    Accion.BLOQUEAR -> "Bloquear"
    Accion.PERMITIR -> "Permitir (lista blanca)"
}
