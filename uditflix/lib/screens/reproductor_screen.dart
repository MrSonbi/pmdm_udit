import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:video_player/video_player.dart';
import '../models/contenido.dart';

//REPRODUCTOR DE VÍDEO
//Reproduce el vídeo de un contenido con el paquete video_player.
//Los controles (título, reproducir/pausar, progreso y pantalla completa)
//van superpuestos al vídeo y se ocultan solos a los 3 segundos.
class ReproductorScreen extends StatefulWidget {
  final Contenido contenido;

  const ReproductorScreen({super.key, required this.contenido});

  @override
  State<ReproductorScreen> createState() => _ReproductorScreenState();
}

class _ReproductorScreenState extends State<ReproductorScreen> {
  late final VideoPlayerController _controlador;
  //Mensaje de error si el vídeo no se puede cargar
  String? _error;
  bool _pantallaCompleta = false;
  //Visibilidad de los controles y temporizador que los oculta
  bool _controlesVisibles = true;
  Timer? _temporizador;

  //Estilo común de los botones redondeados sobre el vídeo
  static final ButtonStyle _estiloBoton = IconButton.styleFrom(
    backgroundColor: Colors.white.withValues(alpha: 0.15),
    foregroundColor: Colors.white,
  );

  @override
  void initState() {
    super.initState();
    _controlador = VideoPlayerController.networkUrl(
      Uri.parse(widget.contenido.videoUrl!),
    );
    //Redibujamos cada vez que cambia el estado del vídeo (posición, pausa...)
    _controlador.addListener(_actualizar);
    _controlador.initialize().then((_) {
      if (!mounted) return;
      _controlador.play();
      _programarOcultado();
    }).catchError((Object e) {
      if (mounted) setState(() => _error = 'No se pudo cargar el vídeo.');
    });
  }

  void _actualizar() {
    if (mounted) setState(() {});
  }

  //Oculta los controles a los 3 s (solo si el vídeo se está reproduciendo)
  void _programarOcultado() {
    _temporizador?.cancel();
    _temporizador = Timer(const Duration(seconds: 3), () {
      if (mounted && _controlador.value.isPlaying) {
        setState(() => _controlesVisibles = false);
      }
    });
  }

  //Tocar el vídeo muestra u oculta los controles
  void _alternarControles() {
    setState(() => _controlesVisibles = !_controlesVisibles);
    if (_controlesVisibles) _programarOcultado();
  }

  void _reproducirPausar() {
    _controlador.value.isPlaying ? _controlador.pause() : _controlador.play();
    _programarOcultado();
  }

  //Activa o desactiva la pantalla completa (horizontal y sin barras del sistema)
  void _cambiarPantallaCompleta() {
    setState(() => _pantallaCompleta = !_pantallaCompleta);
    if (_pantallaCompleta) {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
      SystemChrome.setPreferredOrientations([
        DeviceOrientation.landscapeLeft,
        DeviceOrientation.landscapeRight,
      ]);
    } else {
      _restaurarSistema();
    }
    _programarOcultado();
  }

  void _restaurarSistema() {
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    SystemChrome.setPreferredOrientations([]);
  }

  @override
  void dispose() {
    _temporizador?.cancel();
    _controlador.removeListener(_actualizar);
    _controlador.dispose();
    if (_pantallaCompleta) _restaurarSistema();
    super.dispose();
  }

  //Convierte una duración a formato mm:ss
  String _formato(Duration d) {
    final m = d.inMinutes.remainder(60).toString().padLeft(2, '0');
    final s = d.inSeconds.remainder(60).toString().padLeft(2, '0');
    return '$m:$s';
  }

  @override
  Widget build(BuildContext context) {
    final listo = _error == null && _controlador.value.isInitialized;

    //En pantalla completa, "atrás" sale de pantalla completa en vez de cerrar
    return PopScope(
      canPop: !_pantallaCompleta,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) _cambiarPantallaCompleta();
      },
      child: Scaffold(
        backgroundColor: Colors.black,
        body: SafeArea(
          child: Stack(
            fit: StackFit.expand,
            children: [
              //Vídeo centrado (o el indicador de carga / el error)
              Center(child: _video()),
              if (listo) ...[
                //Capa transparente que recoge los toques sobre el vídeo
                GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: _alternarControles,
                ),
                //Controles superpuestos con aparición/desaparición suave
                AnimatedOpacity(
                  opacity: _controlesVisibles ? 1 : 0,
                  duration: const Duration(milliseconds: 250),
                  child: IgnorePointer(
                    ignoring: !_controlesVisibles,
                    child: _controles(),
                  ),
                ),
              ] else
                //Mientras carga (o si falla) dejamos siempre el botón de volver
                Align(alignment: Alignment.topCenter, child: _barraSuperior()),
            ],
          ),
        ),
      ),
    );
  }

  Widget _video() {
    if (_error != null) {
      return Text(_error!, style: const TextStyle(color: Colors.white));
    }
    final valor = _controlador.value;
    if (!valor.isInitialized) return const CircularProgressIndicator();
    return AspectRatio(
      aspectRatio: valor.aspectRatio,
      child: VideoPlayer(_controlador),
    );
  }

  //Barra superior: volver y título del contenido
  Widget _barraSuperior() {
    return Padding(
      padding: const EdgeInsets.all(8),
      child: Row(
        children: [
          IconButton(
            style: _estiloBoton,
            tooltip: 'Volver',
            icon: const Icon(Icons.arrow_back_rounded),
            onPressed: () => Navigator.maybePop(context),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              widget.contenido.titulo,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ),
        ],
      ),
    );
  }

  Widget _controles() {
    final valor = _controlador.value;
    final colores = Theme.of(context).colorScheme;

    return DecoratedBox(
      //Degradado oscuro arriba y abajo para que los controles se lean
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          stops: [0, 0.25, 0.65, 1],
          colors: [
            Color(0xB3000000),
            Colors.transparent,
            Colors.transparent,
            Color(0xCC000000),
          ],
        ),
      ),
      child: Column(
        children: [
          _barraSuperior(),
          //Botón grande central de reproducir/pausar
          Expanded(
            child: Center(
              child: IconButton(
                style: _estiloBoton,
                iconSize: 56,
                tooltip: valor.isPlaying ? 'Pausar' : 'Reproducir',
                icon: Icon(
                  valor.isPlaying
                      ? Icons.pause_rounded
                      : Icons.play_arrow_rounded,
                ),
                onPressed: _reproducirPausar,
              ),
            ),
          ),
          //Parte inferior: progreso, tiempo y pantalla completa
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 8, 8),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                //Barra de progreso (se puede arrastrar para avanzar)
                ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: VideoProgressIndicator(
                    _controlador,
                    allowScrubbing: true,
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    colors: VideoProgressColors(
                      playedColor: colores.primary,
                      bufferedColor: Colors.white.withValues(alpha: 0.35),
                      backgroundColor: Colors.white.withValues(alpha: 0.12),
                    ),
                  ),
                ),
                Row(
                  children: [
                    Text(
                      '${_formato(valor.position)} / ${_formato(valor.duration)}',
                      style: const TextStyle(color: Colors.white),
                    ),
                    const Spacer(),
                    IconButton(
                      style: _estiloBoton,
                      tooltip: _pantallaCompleta
                          ? 'Salir de pantalla completa'
                          : 'Pantalla completa',
                      icon: Icon(
                        _pantallaCompleta
                            ? Icons.fullscreen_exit_rounded
                            : Icons.fullscreen_rounded,
                      ),
                      onPressed: _cambiarPantallaCompleta,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
