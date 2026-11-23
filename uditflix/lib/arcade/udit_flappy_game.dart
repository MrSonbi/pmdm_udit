import 'dart:math';

import 'package:flame/collisions.dart';
import 'package:flame/components.dart';
import 'package:flame/events.dart';
import 'package:flame/game.dart';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

//VIDEOJUEGO DE UDIT ARCADE
//Juego 2D sencillo al estilo Flappy Bird hecho con Flame.
//- Cada pulsación hace que el personaje salte.
//- La gravedad lo hace caer continuamente.
//- Los obstáculos se desplazan de derecha a izquierda.
//- Chocar con un obstáculo o con los bordes termina la partida.
class UditFlappyGame extends FlameGame
    with TapCallbacks, HasCollisionDetection {
  //Parámetros de la física y la dificultad
  static const double gravedad = 900;
  static const double impulso = -320;
  static const double velocidadObstaculos = 160;
  static const double alturaHueco = 180;
  static const double segundosEntreObstaculos = 1.6;

  //Nombres de los overlays (widgets de Flutter que se pintan encima del juego)
  static const String overlayInicio = 'inicio';
  static const String overlayGameOver = 'gameOver';

  //Mejor puntuación de la sesión. Es "static" para que se conserve
  //aunque salgamos de UDIT Arcade y volvamos a entrar.
  static int record = 0;

  final Random _azar = Random();
  late final Personaje personaje;
  late final TextComponent marcador;
  late final TextComponent mensaje;

  int puntuacion = 0;
  bool jugando = false;
  bool terminado = false;
  double _tiempoDesdeUltimoObstaculo = 0;

  @override
  Color backgroundColor() => AppColores.fondo;

  @override
  Future<void> onLoad() async {
    personaje = Personaje();
    //Puntuación grande con sombra para que se lea sobre cualquier fondo
    marcador = TextComponent(
      anchor: Anchor.topCenter,
      priority: 10,
      textRenderer: TextPaint(
        style: const TextStyle(
          color: Colors.white,
          fontSize: 56,
          fontWeight: FontWeight.w900,
          shadows: [
            Shadow(color: Colors.black, blurRadius: 10, offset: Offset(0, 4)),
            Shadow(color: AppColores.acento, blurRadius: 18),
          ],
        ),
      ),
    );
    mensaje = TextComponent(
      anchor: Anchor.center,
      priority: 10,
      textRenderer: TextPaint(
        style: const TextStyle(
          color: Colors.white70,
          fontSize: 22,
          fontWeight: FontWeight.w600,
          shadows: [Shadow(color: Colors.black, blurRadius: 6)],
        ),
      ),
    );
    addAll([FondoNocturno(), personaje, marcador, mensaje]);
    reiniciar();
  }

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    //Recolocamos los textos si cambia el tamaño de la pantalla
    if (isLoaded) _colocarTextos();
  }

  void _colocarTextos() {
    marcador.position = Vector2(size.x / 2, 24);
    mensaje.position = Vector2(size.x / 2, size.y * 0.7);
  }

  //Cada pulsación sobre el juego
  @override
  void onTapDown(TapDownEvent event) => pulsar();

  //Empieza la partida (si no había empezado) y hace saltar al personaje.
  //También la usa la pantalla de entrada al tocarla.
  void pulsar() {
    if (terminado) return;
    if (!jugando) {
      jugando = true;
      mensaje.text = '';
    }
    personaje.saltar();
  }

  @override
  void update(double dt) {
    super.update(dt);
    if (!jugando) return;
    //Generamos un nuevo par de obstáculos cada cierto tiempo
    _tiempoDesdeUltimoObstaculo += dt;
    if (_tiempoDesdeUltimoObstaculo >= segundosEntreObstaculos) {
      _tiempoDesdeUltimoObstaculo = 0;
      const margen = 80.0;
      final centroHueco = margen +
          alturaHueco / 2 +
          _azar.nextDouble() * (size.y - 2 * margen - alturaHueco);
      add(ParDeObstaculos(centroHueco: centroHueco));
    }
  }

  void sumarPunto() {
    puntuacion++;
    marcador.text = '$puntuacion';
  }

  //Fin de la partida: se congela el juego y se muestra el panel de Game Over
  void gameOver() {
    if (terminado) return;
    terminado = true;
    jugando = false;
    if (puntuacion > record) record = puntuacion;
    pauseEngine();
    overlays.add(overlayGameOver);
  }

  //Deja el juego en su estado inicial
  void reiniciar() {
    removeAll(children.whereType<ParDeObstaculos>().toList());
    puntuacion = 0;
    marcador.text = '0';
    mensaje.text = 'Toca para empezar';
    _tiempoDesdeUltimoObstaculo = segundosEntreObstaculos;
    jugando = false;
    terminado = false;
    personaje.colocarEnInicio();
    _colocarTextos();
    overlays.remove(overlayGameOver);
    resumeEngine();
  }
}

//FONDO NOCTURNO (solo decorativo)
//Degradado morado de noche y estrellas que se desplazan despacio
//a distintas velocidades (efecto parallax sencillo).
class FondoNocturno extends Component with HasGameReference<UditFlappyGame> {
  FondoNocturno() : super(priority: -10);

  final Paint _pinturaCielo = Paint();
  final Paint _pinturaEstrella = Paint();
  Size _tamanoCielo = Size.zero;
  //Cada estrella: x e y relativas (0..1), radio y velocidad
  final List<(double, double, double, double)> _estrellas = [];
  double _tiempo = 0;

  @override
  Future<void> onLoad() async {
    final azar = Random(7); //Semilla fija: siempre el mismo cielo
    for (var i = 0; i < 70; i++) {
      final radio = 0.5 + azar.nextDouble() * 1.5;
      //Las estrellas grandes (más "cercanas") se mueven más rápido
      _estrellas.add((azar.nextDouble(), azar.nextDouble(), radio, radio * 8));
    }
  }

  @override
  void update(double dt) => _tiempo += dt;

  @override
  void render(Canvas canvas) {
    final ancho = game.size.x;
    final alto = game.size.y;
    //Solo recreamos el degradado si cambia el tamaño
    if (_tamanoCielo != Size(ancho, alto)) {
      _tamanoCielo = Size(ancho, alto);
      _pinturaCielo.shader = const LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [Color(0xFF07040D), Color(0xFF221040), Color(0xFF4A1F7A)],
      ).createShader(Offset.zero & _tamanoCielo);
    }
    canvas.drawRect(Offset.zero & _tamanoCielo, _pinturaCielo);

    for (final (x, y, radio, velocidad) in _estrellas) {
      final posX = (x * ancho - _tiempo * velocidad) % ancho;
      _pinturaEstrella.color = Colors.white.withValues(alpha: 0.25 + radio / 3);
      canvas.drawCircle(Offset(posX, y * alto), radio, _pinturaEstrella);
    }
  }
}

//PERSONAJE: un círculo afectado por la gravedad que salta al pulsar
class Personaje extends CircleComponent
    with HasGameReference<UditFlappyGame>, CollisionCallbacks {
  double _velocidadVertical = 0;

  //Pinturas del dibujo (ala, ojo, pupila y pico)
  final Paint _pinturaAla = Paint()..color = const Color(0xFFE0A030);
  final Paint _pinturaOjo = Paint()..color = Colors.white;
  final Paint _pinturaPupila = Paint()..color = AppColores.fondo;
  final Paint _pinturaPico = Paint()..color = AppColores.comic;

  Personaje() : super(radius: 18, anchor: Anchor.center, priority: 5);

  @override
  Future<void> onLoad() async {
    //Cuerpo con degradado radial (brillo arriba a la izquierda)
    paint = Paint()
      ..shader = const RadialGradient(
        center: Alignment(-0.4, -0.5),
        colors: [Color(0xFFFFE29A), Color(0xFFFFC857), Color(0xFFE09B2D)],
      ).createShader(Rect.fromCircle(center: Offset(radius, radius), radius: radius));
    add(CircleHitbox());
  }

  void colocarEnInicio() {
    position = Vector2(game.size.x * 0.25, game.size.y / 2);
    _velocidadVertical = 0;
    angle = 0;
  }

  void saltar() => _velocidadVertical = UditFlappyGame.impulso;

  @override
  void update(double dt) {
    super.update(dt);
    if (!game.jugando) return;
    _velocidadVertical += UditFlappyGame.gravedad * dt;
    y += _velocidadVertical * dt;
    //Solo visual: el personaje se inclina hacia donde se mueve
    angle = (_velocidadVertical / 900).clamp(-0.5, 1.0);
    //Salirse por arriba o por abajo también cuenta como choque
    if (y - radius < 0 || y + radius > game.size.y) game.gameOver();
  }

  //Dibujo del personaje: cuerpo, ala, ojo y pico
  @override
  void render(Canvas canvas) {
    final r = radius;
    final centro = Offset(r, r);
    super.render(canvas); //Cuerpo (el círculo)
    //Ala
    canvas.drawOval(
      Rect.fromCenter(
        center: centro + Offset(-r * 0.3, r * 0.2),
        width: r * 1.1,
        height: r * 0.65,
      ),
      _pinturaAla,
    );
    //Ojo y pupila
    canvas.drawCircle(centro + Offset(r * 0.35, -r * 0.3), r * 0.34, _pinturaOjo);
    canvas.drawCircle(centro + Offset(r * 0.47, -r * 0.3), r * 0.15, _pinturaPupila);
    //Pico (triángulo)
    final pico = Path()
      ..moveTo(r * 1.75, r * 0.85)
      ..lineTo(r * 2.35, r * 1.1)
      ..lineTo(r * 1.75, r * 1.35)
      ..close();
    canvas.drawPath(pico, _pinturaPico);
  }

  //Detección de colisiones: si toca un obstáculo, se acaba la partida
  @override
  void onCollisionStart(
    Set<Vector2> intersectionPoints,
    PositionComponent other,
  ) {
    super.onCollisionStart(intersectionPoints, other);
    if (other is Obstaculo) game.gameOver();
  }
}

//OBSTÁCULO: un rectángulo con hitbox para detectar choques
class Obstaculo extends RectangleComponent {
  Obstaculo({required super.position, required super.size});

  late final RRect _forma;
  late final Paint _pinturaRelleno;
  final Paint _pinturaBorde = Paint()
    ..style = PaintingStyle.stroke
    ..strokeWidth = 2.5
    ..color = AppColores.juego;

  @override
  Future<void> onLoad() async {
    final rect = Rect.fromLTWH(0, 0, size.x, size.y);
    _forma = RRect.fromRectAndRadius(rect, const Radius.circular(10));
    //Degradado horizontal que da sensación de volumen
    _pinturaRelleno = Paint()
      ..shader = const LinearGradient(
        colors: [AppColores.morado, AppColores.acento, AppColores.morado],
      ).createShader(rect);
    add(RectangleHitbox());
  }

  //Dibujo: rectángulo redondeado con degradado y borde cian
  @override
  void render(Canvas canvas) {
    canvas.drawRRect(_forma, _pinturaRelleno);
    canvas.drawRRect(_forma.deflate(1.25), _pinturaBorde);
  }
}

//PAR DE OBSTÁCULOS: uno arriba y otro abajo con un hueco entre ellos.
//Se desplaza hacia la izquierda y suma un punto al superarlo.
class ParDeObstaculos extends PositionComponent
    with HasGameReference<UditFlappyGame> {
  static const double ancho = 70;
  final double centroHueco;
  bool _puntuado = false;

  ParDeObstaculos({required this.centroHueco});

  @override
  Future<void> onLoad() async {
    position = Vector2(game.size.x, 0);
    final arribaAlto = centroHueco - UditFlappyGame.alturaHueco / 2;
    final abajoY = centroHueco + UditFlappyGame.alturaHueco / 2;
    addAll([
      Obstaculo(position: Vector2.zero(), size: Vector2(ancho, arribaAlto)),
      Obstaculo(
        position: Vector2(0, abajoY),
        size: Vector2(ancho, game.size.y - abajoY),
      ),
    ]);
  }

  @override
  void update(double dt) {
    super.update(dt);
    x -= UditFlappyGame.velocidadObstaculos * dt;
    //Punto cuando el obstáculo queda completamente detrás del personaje
    if (!_puntuado && x + ancho < game.personaje.x) {
      _puntuado = true;
      game.sumarPunto();
    }
    //Eliminamos el obstáculo cuando sale de la pantalla
    if (x + ancho < 0) removeFromParent();
  }
}
