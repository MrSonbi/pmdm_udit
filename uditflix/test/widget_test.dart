import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:uditflix/data/contenidos.dart';
import 'package:uditflix/main.dart';
import 'package:uditflix/screens/arcade_screen.dart';
import 'package:uditflix/screens/ficha_screen.dart';
import 'package:uditflix/theme/app_theme.dart';
import 'package:uditflix/widgets/poster_card.dart';

//Texto dentro de un póster (para no confundirlo con el del destacado)
Finder poster(String titulo) => find.descendant(
      of: find.byType(PosterCard),
      matching: find.text(titulo),
    );

//Cambia el tamaño de la "pantalla" del test (móvil o web ancha)
void usarPantalla(WidgetTester tester, Size tamano) {
  tester.view.physicalSize = tamano;
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
}

//Desplaza el catálogo hacia abajo hasta que aparezca el texto buscado.
//scrollUntilVisible lo deja pegado al borde superior, debajo de la barra
//transparente, así que después lo centramos en la pantalla.
Future<void> bajarHasta(WidgetTester tester, Finder finder) async {
  await tester.scrollUntilVisible(
    finder,
    300,
    scrollable: find.byType(Scrollable).first,
  );
  await Scrollable.ensureVisible(tester.element(finder), alignment: 0.5);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('El catálogo muestra los contenidos y el acceso a UDIT Arcade',
      (tester) async {
    await tester.pumpWidget(const MyApp());
    await tester.pumpAndSettle();

    expect(find.text('UDITflix'), findsOneWidget);
    //El primer contenido aparece en el destacado y en su fila
    expect(find.text('Introducción a Flutter'), findsWidgets);
    expect(find.byTooltip('UDIT Arcade'), findsOneWidget);
    //Destacado con sus dos botones y filas por categoría
    expect(find.text('Reproducir'), findsOneWidget);
    expect(find.text('Más info'), findsOneWidget);
    expect(find.text('Vídeos'), findsOneWidget);

    await bajarHasta(tester, find.text('Juega a UDIT Flappy'));
    expect(find.text('Cómics'), findsOneWidget);
    expect(find.text('Juegos'), findsOneWidget);
  });

  testWidgets('La ficha de un vídeo ofrece el botón Reproducir',
      (tester) async {
    await tester.pumpWidget(const MyApp());
    await tester.pumpAndSettle();

    //Pulsamos el póster de la fila de vídeos
    await bajarHasta(tester, poster('Introducción a Flutter'));
    await tester.tap(poster('Introducción a Flutter'));
    await tester.pumpAndSettle();

    //Comprobamos que estamos de verdad en la ficha
    expect(find.byType(FichaScreen), findsOneWidget);

    expect(find.text('Aprende a crear aplicaciones multiplataforma'),
        findsOneWidget);
    expect(find.text('Reproducir'), findsOneWidget);
    expect(find.text('Jugar en UDIT Arcade'), findsNothing);
    //La sección "Más como esto" está más abajo en la ficha
    await bajarHasta(tester, find.text('Más como esto'));
    expect(find.text('Más como esto'), findsOneWidget);
  });

  testWidgets('"Más info" del destacado abre la ficha', (tester) async {
    await tester.pumpWidget(const MyApp());
    await tester.pumpAndSettle();

    await tester.tap(find.text('Más info'));
    await tester.pumpAndSettle();

    expect(find.byType(FichaScreen), findsOneWidget);
    expect(find.text('Aprende a crear aplicaciones multiplataforma'),
        findsOneWidget);
    expect(find.byTooltip('Volver'), findsOneWidget);
  });

  testWidgets('La ficha de un juego lleva a UDIT Arcade', (tester) async {
    await tester.pumpWidget(const MyApp());
    await tester.pumpAndSettle();

    await bajarHasta(tester, poster('Programación de videojuegos'));
    await tester.tap(poster('Programación de videojuegos'));
    await tester.pumpAndSettle();

    expect(find.byType(FichaScreen), findsOneWidget);

    expect(find.text('Jugar en UDIT Arcade'), findsOneWidget);
    expect(find.text('Reproducir'), findsNothing);
  });

  testWidgets('La ficha de un cómic muestra "Próximamente" deshabilitado',
      (tester) async {
    final comic = contenidos.firstWhere((c) => c.tipo == 'Cómic');
    await tester.pumpWidget(MaterialApp(
      theme: AppTheme.oscuro,
      home: FichaScreen(contenido: comic),
    ));
    await tester.pumpAndSettle();

    final boton = find.ancestor(
      of: find.text('Próximamente'),
      matching: find.byWidgetPredicate((w) => w is ButtonStyleButton),
    );
    expect(boton, findsOneWidget);
    expect(tester.widget<ButtonStyleButton>(boton).onPressed, isNull);
  });

  //Si algo se desborda, Flutter lanza un error y el test falla
  for (final (nombre, tamano) in [
    ('móvil 360 px', const Size(360, 740)),
    ('web ancha', const Size(1440, 900)),
  ]) {
    testWidgets('Sin desbordamientos en $nombre', (tester) async {
      usarPantalla(tester, tamano);
      await tester.pumpWidget(const MyApp());
      await tester.pumpAndSettle();
      await bajarHasta(tester, find.text('Juega a UDIT Flappy'));

      await tester.pumpWidget(MaterialApp(
        theme: AppTheme.oscuro,
        home: FichaScreen(contenido: contenidos.first),
      ));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('UDIT Arcade: el personaje cae, aparece Game Over y se reinicia',
      (tester) async {
    await tester.pumpWidget(MaterialApp(
      theme: AppTheme.oscuro,
      home: const ArcadeScreen(),
    ));
    //Dejamos que Flame cargue el juego
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    //Pantalla de entrada con instrucciones y récord
    expect(find.text('Toca para volar'), findsOneWidget);
    expect(find.textContaining('RÉCORD'), findsOneWidget);

    //Primera pulsación: empieza la partida
    await tester.tap(find.text('Toca para volar'));
    //Sin más pulsaciones el personaje cae hasta salir de la pantalla
    for (var i = 0; i < 60; i++) {
      await tester.pump(const Duration(milliseconds: 50));
    }

    expect(find.text('Game Over'), findsOneWidget);
    expect(find.text('Reiniciar'), findsOneWidget);
    expect(find.text('Salir'), findsOneWidget);
    expect(find.textContaining('RÉCORD'), findsOneWidget);

    await tester.tap(find.text('Reiniciar'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 50));

    expect(find.text('Game Over'), findsNothing);
  });
}
