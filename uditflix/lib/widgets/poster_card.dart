import 'package:flutter/material.dart';
import '../models/contenido.dart';
import '../screens/ficha_screen.dart';
import '../theme/app_theme.dart';
import 'imagen_contenido.dart';

//Etiqueta de la animación Hero que une el póster con la cabecera de la ficha
String etiquetaHero(Contenido contenido) => 'poster-${contenido.id}';

//Durante el vuelo de la animación Hero mostramos siempre el PÓSTER
//(que ya está cargado), venga de donde venga, para que no parpadee.
Widget vueloDelPoster(
  BuildContext contextoVuelo,
  Animation<double> animacion,
  HeroFlightDirection direccion,
  BuildContext contextoOrigen,
  BuildContext contextoDestino,
) {
  final heroPoster = direccion == HeroFlightDirection.push
      ? contextoOrigen.widget as Hero
      : contextoDestino.widget as Hero;
  return heroPoster.child;
}

//TARJETA TIPO PÓSTER
//Imagen vertical 2:3 con bordes redondeados, sombra, etiqueta del tipo
//y título. Al pulsarla se encoge un poco y abre la ficha del contenido.
class PosterCard extends StatefulWidget {
  final Contenido contenido;
  final double ancho;

  const PosterCard({super.key, required this.contenido, required this.ancho});

  @override
  State<PosterCard> createState() => _PosterCardState();
}

class _PosterCardState extends State<PosterCard> {
  //true mientras el dedo (o el ratón) está pulsando la tarjeta
  bool _pulsada = false;

  void _abrirFicha() {
    final contenido = widget.contenido;
    //Empezamos a descargar la imagen grande de la ficha antes de llegar a ella
    final imagenHero = contenido.imagenHeroUrl;
    if (imagenHero != null) {
      precacheImage(
        NetworkImage(imagenHero),
        context,
        onError: (error, stackTrace) {}, //Sin internet no pasa nada
      );
    }
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => FichaScreen(contenido: contenido)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final contenido = widget.contenido;
    final textos = Theme.of(context).textTheme;
    final colorTipo = AppColores.deTipo(contenido.tipo);

    return GestureDetector(
      onTapDown: (_) => setState(() => _pulsada = true),
      onTapUp: (_) => setState(() => _pulsada = false),
      onTapCancel: () => setState(() => _pulsada = false),
      onTap: _abrirFicha,
      //Microinteracción: ligera escala al pulsar
      child: AnimatedScale(
        scale: _pulsada ? 0.95 : 1,
        duration: const Duration(milliseconds: 120),
        child: Container(
          width: widget.ancho,
          height: widget.ancho * 1.5,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.5),
                blurRadius: 12,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: Stack(
              fit: StackFit.expand,
              children: [
                //Imagen del póster (vuela hasta la ficha con Hero)
                Hero(
                  tag: etiquetaHero(contenido),
                  flightShuttleBuilder: vueloDelPoster,
                  child: ImagenContenido(
                    url: contenido.imagenUrl,
                    tipo: contenido.tipo,
                  ),
                ),
                //Degradado inferior para que el título se lea bien
                const DecoratedBox(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      stops: [0.5, 1],
                      colors: [Colors.transparent, Colors.black],
                    ),
                  ),
                ),
                //Etiqueta del tipo arriba a la izquierda
                Positioned(
                  top: 8,
                  left: 8,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 6,
                      vertical: 2,
                    ),
                    decoration: BoxDecoration(
                      color: colorTipo,
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      contenido.tipo.toUpperCase(),
                      style: textos.labelSmall?.copyWith(
                        color: AppColores.fondo,
                        fontSize: 9,
                        letterSpacing: 1.2,
                      ),
                    ),
                  ),
                ),
                //Título abajo
                Positioned(
                  left: 8,
                  right: 8,
                  bottom: 8,
                  child: Text(
                    contenido.titulo,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: textos.labelLarge?.copyWith(
                      color: AppColores.texto,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
