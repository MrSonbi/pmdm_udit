import 'package:flutter/material.dart';
import '../models/contenido.dart';
import '../screens/ficha_screen.dart';
import '../screens/reproductor_screen.dart';
import '../theme/app_theme.dart';
import 'imagen_contenido.dart';

//BANNER DESTACADO (HERO)
//Imagen grande del contenido destacado con un degradado hacia negro,
//título, descripción y los botones "Reproducir" y "Más info".
class HeroDestacado extends StatelessWidget {
  final Contenido contenido;

  const HeroDestacado({super.key, required this.contenido});

  @override
  Widget build(BuildContext context) {
    final tamano = MediaQuery.sizeOf(context);
    final margen = Medidas.margen(tamano.width);
    final textos = Theme.of(context).textTheme;
    //En móvil, alto fijo; en pantallas anchas, proporcional a la ventana
    final alto = tamano.width < 600
        ? 480.0
        : (tamano.height * 0.75).clamp(420.0, 640.0);

    return SizedBox(
      height: alto,
      child: Stack(
        fit: StackFit.expand,
        children: [
          ImagenContenido(
            url: contenido.imagenHeroUrl,
            tipo: contenido.tipo,
            tamanoIcono: 96,
          ),
          //Degradado: oscuro arriba (para la barra) y negro abajo (para el texto)
          const DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                stops: [0, 0.25, 0.55, 1],
                colors: [
                  Color(0x99000000),
                  Colors.transparent,
                  Color(0x660E0B14),
                  AppColores.fondo,
                ],
              ),
            ),
          ),
          //Textos y botones en la parte inferior
          Positioned(
            left: margen,
            right: margen,
            bottom: 24,
            child: Align(
              alignment: Alignment.bottomLeft,
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 560),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'UDIT ORIGINAL · ${contenido.tipo.toUpperCase()}',
                      style: textos.labelSmall?.copyWith(
                        color: AppColores.deTipo(contenido.tipo),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      contenido.titulo,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: textos.displaySmall,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      contenido.descripcion,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: textos.bodyLarge,
                    ),
                    const SizedBox(height: 16),
                    //Wrap: si no caben en una línea, los botones bajan
                    Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: [
                        if (contenido.videoUrl != null)
                          ElevatedButton.icon(
                            icon: const Icon(Icons.play_arrow_rounded),
                            label: const Text('Reproducir'),
                            onPressed: () => Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) =>
                                    ReproductorScreen(contenido: contenido),
                              ),
                            ),
                          ),
                        FilledButton.icon(
                          icon: const Icon(Icons.info_outline_rounded),
                          label: const Text('Más info'),
                          onPressed: () => Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) =>
                                  FichaScreen(contenido: contenido),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
