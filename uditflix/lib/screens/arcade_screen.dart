import 'package:flame/game.dart';
import 'package:flutter/material.dart';
import '../arcade/udit_flappy_game.dart';
import '../theme/app_theme.dart';

//UDIT ARCADE
//Sección de videojuegos de UDITflix. Muestra el juego dentro de un
//GameWidget y, encima, dos paneles hechos con Flutter (overlays):
//la pantalla de entrada y el panel de Game Over.
class ArcadeScreen extends StatefulWidget {
  const ArcadeScreen({super.key});

  @override
  State<ArcadeScreen> createState() => _ArcadeScreenState();
}

class _ArcadeScreenState extends State<ArcadeScreen> {
  //Creamos el juego una sola vez para que no se reinicie al redibujar
  final UditFlappyGame _juego = UditFlappyGame();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('UDIT Arcade'),
        backgroundColor: AppColores.fondo,
      ),
      body: GameWidget<UditFlappyGame>(
        game: _juego,
        //Al entrar se muestra la pantalla de inicio
        initialActiveOverlays: const [UditFlappyGame.overlayInicio],
        overlayBuilderMap: {
          UditFlappyGame.overlayInicio: (context, juego) =>
              _PantallaInicio(juego: juego),
          UditFlappyGame.overlayGameOver: (context, juego) =>
              _PanelGameOver(juego: juego),
        },
      ),
    );
  }
}

//Tarjeta centrada y con ancho máximo, común a los dos paneles
class _TarjetaPanel extends StatelessWidget {
  final List<Widget> children;

  const _TarjetaPanel({required this.children});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 360),
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: children,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

//Texto pequeño "RÉCORD: N" con el estilo de etiqueta del tema
class _Record extends StatelessWidget {
  const _Record();

  @override
  Widget build(BuildContext context) {
    return Text(
      'Récord: ${UditFlappyGame.record}'.toUpperCase(),
      style: Theme.of(context).textTheme.labelSmall?.copyWith(
            color: AppColores.juego,
          ),
    );
  }
}

//PANTALLA DE ENTRADA
//Título, instrucciones y récord. Tocar en cualquier sitio empieza a jugar.
class _PantallaInicio extends StatelessWidget {
  final UditFlappyGame juego;

  const _PantallaInicio({required this.juego});

  void _empezar() {
    juego.overlays.remove(UditFlappyGame.overlayInicio);
    juego.pulsar();
  }

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: _empezar,
      child: ColoredBox(
        color: AppColores.fondo.withValues(alpha: 0.55),
        child: _TarjetaPanel(
          children: [
            const Icon(
              Icons.sports_esports_rounded,
              size: 56,
              color: AppColores.juego,
            ),
            const SizedBox(height: 12),
            Text(
              'UDIT Flappy',
              textAlign: TextAlign.center,
              style: textos.headlineMedium,
            ),
            const SizedBox(height: 8),
            Text(
              'Esquiva los obstáculos y llega lo más lejos que puedas.',
              textAlign: TextAlign.center,
              style: textos.bodyMedium,
            ),
            const SizedBox(height: 20),
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.touch_app_rounded, color: AppColores.acento),
                const SizedBox(width: 8),
                Flexible(
                  child: Text('Toca para volar', style: textos.titleMedium),
                ),
              ],
            ),
            const SizedBox(height: 16),
            const _Record(),
          ],
        ),
      ),
    );
  }
}

//PANEL DE GAME OVER: puntuación, récord y botones Reiniciar y Salir
class _PanelGameOver extends StatelessWidget {
  final UditFlappyGame juego;

  const _PanelGameOver({required this.juego});

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    return ColoredBox(
      color: AppColores.fondo.withValues(alpha: 0.55),
      child: _TarjetaPanel(
        children: [
          Text('Game Over', style: textos.headlineMedium),
          const SizedBox(height: 16),
          Text('PUNTUACIÓN', style: textos.labelSmall),
          Text(
            '${juego.puntuacion}',
            style: textos.displaySmall?.copyWith(color: AppColores.acento),
          ),
          const SizedBox(height: 8),
          const _Record(),
          const SizedBox(height: 24),
          //Wrap: en pantallas estrechas los botones bajan de línea
          Wrap(
            alignment: WrapAlignment.center,
            spacing: 12,
            runSpacing: 12,
            children: [
              ElevatedButton.icon(
                icon: const Icon(Icons.replay_rounded),
                label: const Text('Reiniciar'),
                onPressed: juego.reiniciar,
              ),
              FilledButton.icon(
                icon: const Icon(Icons.logout_rounded),
                label: const Text('Salir'),
                onPressed: () => Navigator.maybePop(context),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
