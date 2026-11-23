import 'package:flutter/material.dart';

//PALETA DE UDITFLIX
//Todos los colores de la app salen de aquí. Así, si cambiamos un color,
//cambia en todas las pantallas a la vez.
class AppColores {
  //Fondo casi negro y superficies (tarjetas, chips...)
  static const Color fondo = Color(0xFF0E0B14);
  static const Color superficie = Color(0xFF1A1424);
  static const Color superficieAlta = Color(0xFF2A2138);

  //Morado corporativo de UDIT y un acento más vivo
  static const Color morado = Color(0xFF5B2D90);
  static const Color acento = Color(0xFF8E4DFF);

  //Textos
  static const Color texto = Colors.white;
  static const Color textoSecundario = Color(0xFFA99FBA);

  //Un color por tipo de contenido
  static const Color video = Color(0xFF8E4DFF);
  static const Color comic = Color(0xFFFF8A3D);
  static const Color juego = Color(0xFF22D3EE);

  //Devuelve el color asociado a un tipo de contenido
  static Color deTipo(String tipo) {
    switch (tipo) {
      case 'Cómic':
        return comic;
      case 'Juego':
        return juego;
      default:
        return video;
    }
  }
}

//Icono asociado a cada tipo de contenido
IconData iconoDeTipo(String tipo) {
  switch (tipo) {
    case 'Cómic':
      return Icons.menu_book_rounded;
    case 'Juego':
      return Icons.sports_esports_rounded;
    default:
      return Icons.play_circle_fill_rounded;
  }
}

//MEDIDAS RESPONSIVE
//Calculan tamaños según el ancho de la pantalla (móvil, tablet o web).
class Medidas {
  //Margen lateral: más estrecho en móvil, más amplio en pantallas grandes
  static double margen(double anchoPantalla) => anchoPantalla >= 900 ? 48 : 16;

  //Ancho de los pósters: más grandes cuanto más ancha es la pantalla
  static double anchoPoster(double anchoPantalla) {
    if (anchoPantalla >= 1200) return 190;
    if (anchoPantalla >= 600) return 150;
    return 120;
  }

  //Los pósters tienen proporción 2:3 (ancho:alto)
  static double altoPoster(double anchoPantalla) =>
      anchoPoster(anchoPantalla) * 1.5;
}

//TEMA OSCURO DE UDITFLIX
class AppTheme {
  static ThemeData get oscuro {
    //Partimos de la tipografía oscura por defecto y la ajustamos
    final base = ThemeData(brightness: Brightness.dark).textTheme;
    final textos = base.copyWith(
      //Títulos grandes y en negrita
      displaySmall: base.displaySmall?.copyWith(
        fontWeight: FontWeight.w800,
        color: AppColores.texto,
        height: 1.1,
      ),
      headlineMedium: base.headlineMedium?.copyWith(
        fontWeight: FontWeight.w800,
        color: AppColores.texto,
      ),
      headlineSmall: base.headlineSmall?.copyWith(
        fontWeight: FontWeight.w800,
        color: AppColores.texto,
      ),
      titleLarge: base.titleLarge?.copyWith(
        fontWeight: FontWeight.w700,
        color: AppColores.texto,
      ),
      titleMedium: base.titleMedium?.copyWith(
        fontWeight: FontWeight.w600,
        color: AppColores.texto,
      ),
      //Subtítulos y descripciones en gris
      bodyLarge: base.bodyLarge?.copyWith(
        color: AppColores.textoSecundario,
        height: 1.5,
      ),
      bodyMedium: base.bodyMedium?.copyWith(
        color: AppColores.textoSecundario,
      ),
      //Etiquetas pequeñas con letras separadas (se escriben en mayúsculas)
      labelSmall: base.labelSmall?.copyWith(
        fontWeight: FontWeight.w700,
        letterSpacing: 1.6,
        color: AppColores.textoSecundario,
      ),
      labelLarge: base.labelLarge?.copyWith(fontWeight: FontWeight.w700),
    );

    //Forma redondeada común a botones
    final formaBoton = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(10),
    );
    const paddingBoton = EdgeInsets.symmetric(horizontal: 22, vertical: 14);

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColores.morado,
        brightness: Brightness.dark,
      ).copyWith(
        primary: AppColores.acento,
        onPrimary: Colors.white,
        secondary: AppColores.morado,
        surface: AppColores.superficie,
        onSurface: AppColores.texto,
      ),
      scaffoldBackgroundColor: AppColores.fondo,
      textTheme: textos,

      //Barra superior: sin sombras ni tintes de Material 3
      appBarTheme: AppBarTheme(
        backgroundColor: AppColores.fondo,
        foregroundColor: AppColores.texto,
        elevation: 0,
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
        centerTitle: false,
        titleTextStyle: textos.titleLarge,
      ),

      //Tarjetas oscuras con bordes redondeados y sombra
      cardTheme: CardThemeData(
        color: AppColores.superficie,
        surfaceTintColor: Colors.transparent,
        elevation: 8,
        shadowColor: Colors.black,
        margin: EdgeInsets.zero,
        clipBehavior: Clip.antiAlias,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),

      //Chips en forma de píldora con texto pequeño y separado
      chipTheme: ChipThemeData(
        backgroundColor: AppColores.superficieAlta,
        side: BorderSide.none,
        shape: const StadiumBorder(),
        labelStyle: textos.labelSmall?.copyWith(color: AppColores.texto),
        iconTheme: const IconThemeData(
          color: AppColores.textoSecundario,
          size: 16,
        ),
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      ),

      //Botón principal: acento morado vivo
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColores.acento,
          foregroundColor: Colors.white,
          iconColor: Colors.white,
          disabledBackgroundColor: AppColores.superficieAlta,
          disabledForegroundColor: AppColores.textoSecundario,
          disabledIconColor: AppColores.textoSecundario,
          elevation: 4,
          shadowColor: AppColores.acento.withValues(alpha: 0.5),
          padding: paddingBoton,
          shape: formaBoton,
          textStyle: textos.labelLarge,
        ),
      ),

      //Botón secundario ("Más info"): blanco translúcido
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: Colors.white.withValues(alpha: 0.18),
          foregroundColor: Colors.white,
          iconColor: Colors.white,
          padding: paddingBoton,
          shape: formaBoton,
          textStyle: textos.labelLarge,
        ),
      ),

      iconTheme: const IconThemeData(color: AppColores.texto, size: 24),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: AppColores.acento,
      ),
    );
  }
}
