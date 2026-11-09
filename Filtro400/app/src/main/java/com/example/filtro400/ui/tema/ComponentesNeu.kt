package com.example.filtro400.ui.tema

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Piezas del estilo neumórfico, SIN dependencias externas.
 *
 * La idea del neumorfismo: las piezas tienen el MISMO color que el fondo y solo se
 * distinguen por la luz. Una luz imaginaria llega desde arriba a la izquierda:
 *  - pieza ELEVADA: sombra clara arriba-izquierda y oscura abajo-derecha;
 *  - pieza HUNDIDA: al revés y POR DENTRO (sombra oscura en el borde interior
 *    de arriba-izquierda), como un hueco en la superficie.
 *
 * Las sombras se dibujan con Paint.setShadowLayer de Android. Con aceleración por
 * hardware funciona en cualquier forma desde la API 28, y la app pide minSdk 29.
 *
 * Regla de accesibilidad: la sombra es solo decoración. Ningún estado se comunica
 * solo con sombra o color; siempre hay texto ("Activa", "Pausada"...).
 */

/** Radio de esquina por defecto de tarjetas y botones. */
val RADIO_NEU: Dp = 20.dp

/** Zona táctil mínima recomendada por Android. */
private val TACTIL_MIN: Dp = 48.dp

/**
 * Pieza ELEVADA: dibuja detrás del contenido un rectángulo redondeado relleno
 * con [relleno] y dos sombras (clara arriba-izquierda, oscura abajo-derecha).
 *
 * Las sombras salen fuera de los límites del componente: deja margen alrededor
 * (los contentPadding de las listas ya lo dan).
 */
fun Modifier.neuElevado(
    radio: Dp = RADIO_NEU,
    elevacion: Dp = 6.dp,
    relleno: Color = ColoresNeu.fondo
): Modifier = drawWithCache {
    val r = radio.toPx()
    val d = elevacion.toPx()
    // Pincel de Android (no de Compose): es el que tiene setShadowLayer.
    val pincel = android.graphics.Paint().apply {
        isAntiAlias = true
        color = relleno.toArgb()
    }
    val clara = ColoresNeu.sombraClara.toArgb()
    val oscura = ColoresNeu.sombraOscura.copy(alpha = 0.7f).toArgb()
    onDrawBehind {
        drawIntoCanvas { lienzo ->
            val nativo = lienzo.nativeCanvas
            if (d > 0f) {
                // 1) Luz: sombra blanca desplazada hacia arriba-izquierda.
                pincel.setShadowLayer(d * 2f, -d, -d, clara)
                nativo.drawRoundRect(0f, 0f, size.width, size.height, r, r, pincel)
                // 2) Sombra: gris azulado desplazado hacia abajo-derecha.
                pincel.setShadowLayer(d * 2f, d, d, oscura)
                nativo.drawRoundRect(0f, 0f, size.width, size.height, r, r, pincel)
            } else {
                pincel.clearShadowLayer()
                nativo.drawRoundRect(0f, 0f, size.width, size.height, r, r, pincel)
            }
        }
    }
}

/**
 * Pieza HUNDIDA (campos de texto, opción activa del selector, pestaña activa).
 *
 * setShadowLayer solo dibuja sombras EXTERIORES, así que la sombra interior se
 * simula: se recorta (clip) a la forma de la pieza y se dibuja un "marco" que la
 * rodea por fuera. El marco queda fuera del recorte y no se ve, pero su sombra
 * desplazada entra en la pieza y oscurece el borde interior.
 *
 * Además lleva un degradado suave y un borde fino: aunque un móvil no pintase
 * las sombras, la pieza se seguiría viendo hundida.
 */
fun Modifier.neuHundido(
    radio: Dp = RADIO_NEU,
    profundidad: Dp = 4.dp,
    colorBorde: Color = ColoresNeu.sombraOscura.copy(alpha = 0.6f),
    grosorBorde: Dp = 1.dp
): Modifier = drawWithCache {
    val r = radio.toPx()
    val d = profundidad.toPx()
    val ancho = size.width
    val alto = size.height
    val forma = android.graphics.Path().apply {
        addRoundRect(0f, 0f, ancho, alto, r, r, android.graphics.Path.Direction.CW)
    }
    // Marco = rectángulo grande MENOS la pieza (regla par-impar deja el hueco).
    val marco = android.graphics.Path().apply {
        fillType = android.graphics.Path.FillType.EVEN_ODD
        addRect(-ancho, -alto, ancho * 2f, alto * 2f, android.graphics.Path.Direction.CW)
        addRoundRect(0f, 0f, ancho, alto, r, r, android.graphics.Path.Direction.CW)
    }
    val pincel = android.graphics.Paint().apply {
        isAntiAlias = true
        color = ColoresNeu.fondo.toArgb()
    }
    val clara = ColoresNeu.sombraClara.toArgb()
    val oscura = ColoresNeu.sombraOscura.toArgb()
    // Degradado: algo más oscuro arriba (donde "no llega la luz").
    val degradado = Brush.verticalGradient(listOf(ColoresNeu.fondoHundido, ColoresNeu.fondo))
    val grosor = grosorBorde.toPx()
    onDrawBehind {
        drawRoundRect(brush = degradado, cornerRadius = CornerRadius(r, r))
        drawIntoCanvas { lienzo ->
            val nativo = lienzo.nativeCanvas
            nativo.save()
            nativo.clipPath(forma)
            // Sombra oscura dentro, en el borde de arriba-izquierda.
            pincel.setShadowLayer(d * 2f, d, d, oscura)
            nativo.drawPath(marco, pincel)
            // Brillo dentro, en el borde de abajo-derecha.
            pincel.setShadowLayer(d * 2f, -d, -d, clara)
            nativo.drawPath(marco, pincel)
            nativo.restore()
        }
        if (grosor > 0f) {
            drawRoundRect(
                color = colorBorde,
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = grosor)
            )
        }
    }
}

/**
 * Tarjeta elevada. Si [alPulsar] no es null, toda la tarjeta es pulsable y
 * mientras se mantiene pulsada se ve HUNDIDA (estado pulsado visible).
 * Mide lo que mide su contenido: nunca tiene una altura fija.
 */
@Composable
fun NeuTarjeta(
    modifier: Modifier = Modifier,
    alPulsar: (() -> Unit)? = null,
    descripcionAccion: String? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val fuente = remember { MutableInteractionSource() }
    val pulsada by fuente.collectIsPressedAsState()
    val forma = RoundedCornerShape(RADIO_NEU)
    val superficie = if (alPulsar != null && pulsada) {
        Modifier.neuHundido()
    } else {
        Modifier.neuElevado()
    }
    val pulsable = if (alPulsar != null) {
        Modifier.clickable(
            interactionSource = fuente,
            indication = LocalIndication.current,
            onClickLabel = descripcionAccion,
            role = Role.Button,
            onClick = alPulsar
        )
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(superficie)
            .clip(forma)
            .then(pulsable)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = contenido
    )
}

/**
 * Botón neumórfico.
 *  - [primario] = true: relleno de acento y texto blanco (la acción principal).
 *  - [primario] = false: elevado del color del fondo, texto en acento o en
 *    [colorTexto] (por ejemplo, el rojo de bloqueo para "Eliminar regla").
 * Al pulsarlo se hunde. Alto mínimo 48 dp; el texto puede ocupar dos líneas
 * antes que salirse.
 */
@Composable
fun NeuBoton(
    texto: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    primario: Boolean = true,
    habilitado: Boolean = true,
    colorTexto: Color? = null
) {
    val fuente = remember { MutableInteractionSource() }
    val pulsado by fuente.collectIsPressedAsState()
    val forma = RoundedCornerShape(RADIO_NEU)
    val superficie = when {
        primario -> Modifier.neuElevado(
            elevacion = if (pulsado || !habilitado) 1.dp else 6.dp,
            // Pulsado: el acento se oscurece un poco, además de "bajar".
            relleno = if (pulsado) {
                Color(PaletaNeu.oscurecer(PaletaNeu.ACENTO, 0.85))
            } else {
                ColoresNeu.acento
            }
        )
        pulsado -> Modifier.neuHundido()
        else -> Modifier.neuElevado(elevacion = if (habilitado) 6.dp else 1.dp)
    }
    val color = when {
        primario -> ColoresNeu.blanco
        else -> colorTexto ?: ColoresNeu.acento
    }
    Box(
        modifier = modifier
            .then(superficie)
            .clip(forma)
            .clickable(
                interactionSource = fuente,
                indication = LocalIndication.current,
                enabled = habilitado,
                role = Role.Button,
                onClick = alPulsar
            )
            .defaultMinSize(minWidth = TACTIL_MIN, minHeight = TACTIL_MIN)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = if (habilitado) color else color.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Selector segmentado: varias opciones en una barra elevada; la activa se ve
 * HUNDIDA, en negrita y con el texto en acento. Cada opción tiene weight(1f): si
 * un texto no cabe, salta de línea dentro de su hueco y nunca empuja a los demás.
 * Todas las opciones miden lo mismo de alto (IntrinsicSize.Min), sin altura fija.
 */
@Composable
fun SelectorSegmentado(
    opciones: List<String>,
    seleccionada: Int,
    alSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .neuElevado(elevacion = 4.dp)
            .padding(6.dp)
            .height(IntrinsicSize.Min)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        opciones.forEachIndexed { indice, texto ->
            val activa = indice == seleccionada
            val forma = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    // Acotado: la Row padre mide height(IntrinsicSize.Min), es decir,
                    // lo que mide la opción más alta. Nunca crece más que eso.
                    .fillMaxHeight()
                    .then(if (activa) Modifier.neuHundido(radio = 16.dp, profundidad = 3.dp) else Modifier)
                    .clip(forma)
                    .selectable(
                        selected = activa,
                        enabled = habilitado,
                        role = Role.RadioButton,
                        onClick = { alSeleccionar(indice) }
                    )
                    .defaultMinSize(minHeight = TACTIL_MIN)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = texto,
                    color = if (activa) ColoresNeu.acento else ColoresNeu.texto,
                    fontWeight = if (activa) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Colores del Switch de Material con la paleta neumórfica. */
@Composable
fun coloresInterruptorNeu() = SwitchDefaults.colors(
    checkedThumbColor = ColoresNeu.blanco,
    checkedTrackColor = ColoresNeu.acento,
    checkedBorderColor = ColoresNeu.acento,
    uncheckedThumbColor = ColoresNeu.textoSecundario,
    uncheckedTrackColor = ColoresNeu.fondoHundido,
    uncheckedBorderColor = ColoresNeu.textoSecundario
)

/**
 * Interruptor con su estado escrito debajo ("Activa" / "Pausada").
 * Toda la columna es la zona táctil (>= 48 dp) y se anuncia como un Switch;
 * el Switch interno no recibe clics propios (onCheckedChange = null).
 */
@Composable
fun NeuInterruptor(
    activo: Boolean,
    alCambiar: (Boolean) -> Unit,
    textoActivo: String,
    textoInactivo: String,
    modifier: Modifier = Modifier,
    descripcion: String? = null
) {
    val estado = if (activo) textoActivo else textoInactivo
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .toggleable(value = activo, role = Role.Switch, onValueChange = alCambiar)
            .semantics {
                stateDescription = estado
                if (descripcion != null) contentDescription = descripcion
            }
            .defaultMinSize(minWidth = TACTIL_MIN, minHeight = TACTIL_MIN)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Switch(checked = activo, onCheckedChange = null, colors = coloresInterruptorNeu())
        Text(
            text = estado,
            style = MaterialTheme.typography.labelMedium,
            color = if (activo) ColoresNeu.acento else ColoresNeu.textoSecundario,
            fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Campo de texto HUNDIDO con su etiqueta encima y, si hay [error], el mensaje
 * debajo en texto y en el color de bloqueo (además de un borde de ese color).
 * El error nunca se indica solo con color: siempre hay una frase.
 */
@Composable
fun CampoHundido(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    ejemplo: String? = null,
    error: String? = null,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    habilitado: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = ColoresNeu.texto
        )
        BasicTextField(
            value = valor,
            onValueChange = alCambiar,
            singleLine = true,
            enabled = habilitado,
            keyboardOptions = teclado,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ColoresNeu.texto),
            cursorBrush = SolidColor(ColoresNeu.acento),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = etiqueta
                    // this.error(...) es la propiedad de accesibilidad, no kotlin.error().
                    if (error != null) this.error(error)
                },
            decorationBox = { campo ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neuHundido(radio = 16.dp)
                        .then(
                            if (error != null) {
                                Modifier.border(2.dp, ColoresNeu.bloqueo, RoundedCornerShape(16.dp))
                            } else {
                                Modifier
                            }
                        )
                        .defaultMinSize(minHeight = 52.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (valor.isEmpty() && ejemplo != null) {
                        Text(
                            text = ejemplo,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ColoresNeu.textoSecundario
                        )
                    }
                    campo()
                }
            }
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = ColoresNeu.bloqueo
            )
        }
    }
}

/** Alto de la barra inferior (sin contar la barra de navegación del sistema). */
private val ALTO_BARRA: Dp = 72.dp

/** Alto de la celda de cada pestaña: aquí dentro se dibuja el indicador hundido. */
private val ALTO_CELDA_PESTANA: Dp = 48.dp

/**
 * Barra inferior propia, solo texto.
 *
 * ALTURA ACOTADA: la barra mide [ALTO_BARRA] más el hueco de la barra de
 * navegación del sistema (windowInsetsPadding). Ni la barra ni sus hijos usan
 * fillMaxHeight/fillMaxSize: el slot bottomBar del Scaffold ofrece TODA la
 * pantalla como alto máximo, y un hijo con fillMaxHeight la ocuparía entera
 * (fallo de la versión anterior, que iba dentro de un NavigationBar cuyo alto
 * es solo un MÍNIMO de 80 dp, no un máximo).
 *
 * La pestaña seleccionada lleva el indicador HUNDIDO dentro de una celda de alto
 * fijo [ALTO_CELDA_PESTANA] (48 dp, la zona táctil mínima), con el texto en acento
 * y en negrita. Cada pestaña tiene weight(1f); el texto va en una sola línea a
 * 13 sp para que quepa incluso con letra al 130 %.
 *
 * El efecto neumórfico de la barra en sí es solo su fondo: una sombra suave
 * hacia arriba y un filo claro, como si la barra estuviera un poco elevada.
 */
@Composable
fun BarraNeu(
    pestanas: List<String>,
    seleccionada: Int,
    alSeleccionar: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val sombra = 8.dp.toPx()
                // Sombra oscura difuminada justo ENCIMA de la barra.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, ColoresNeu.sombraOscura.copy(alpha = 0.45f)),
                        startY = -sombra,
                        endY = 0f
                    ),
                    topLeft = Offset(0f, -sombra),
                    size = Size(size.width, sombra)
                )
                drawRect(color = ColoresNeu.fondo)
                // Filo de luz en el borde superior.
                drawLine(
                    color = ColoresNeu.sombraClara,
                    start = Offset(0f, 1.dp.toPx()),
                    end = Offset(size.width, 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(ALTO_BARRA)
            .padding(horizontal = 2.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pestanas.forEachIndexed { indice, titulo ->
            val activa = indice == seleccionada
            val forma = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 1.dp)
                    .height(ALTO_CELDA_PESTANA)
                    .then(if (activa) Modifier.neuHundido(radio = 14.dp, profundidad = 3.dp) else Modifier)
                    .clip(forma)
                    .selectable(
                        selected = activa,
                        role = Role.Tab,
                        onClick = { alSeleccionar(indice) }
                    )
                    .padding(horizontal = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    ),
                    color = if (activa) ColoresNeu.acento else ColoresNeu.textoSecundario,
                    fontWeight = if (activa) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    softWrap = false,
                    // Último recurso en pantallas muy estrechas; con los nombres
                    // actuales no debería verse nunca.
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
