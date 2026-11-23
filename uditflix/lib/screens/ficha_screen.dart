import 'package:flutter/material.dart';
import '../data/contenidos.dart';
import '../models/contenido.dart';
import '../theme/app_theme.dart';
import '../widgets/fila_categoria.dart';
import '../widgets/imagen_contenido.dart';
import '../widgets/poster_card.dart';
import 'arcade_screen.dart';
import 'reproductor_screen.dart';

//FICHA DE DETALLE
//Cabecera con imagen grande que se colapsa al hacer scroll, información
//del contenido, botón principal según el tipo y "Más como esto".
class FichaScreen extends StatelessWidget {
  final Contenido contenido;

  const FichaScreen({super.key, required this.contenido});

  @override
  Widget build(BuildContext context) {
    final textos = Theme.of(context).textTheme;
    final anchoPantalla = MediaQuery.sizeOf(context).width;
    final margen = Medidas.margen(anchoPantalla);
    final colorTipo = AppColores.deTipo(contenido.tipo);

    //Contenidos del mismo tipo (sin incluir el actual)
    final similares = contenidos
        .where((c) => c.tipo == contenido.tipo && c.id != contenido.id)
        .toList();

    return Scaffold(
      body: CustomScrollView(
        slivers: [
          //CABECERA: imagen grande que se colapsa al hacer scroll
          SliverAppBar(
            pinned: true,
            expandedHeight: anchoPantalla < 600 ? 320 : 440,
            //Botón de volver con fondo semitransparente
            leading: Padding(
              padding: const EdgeInsets.all(8),
              child: IconButton(
                tooltip: 'Volver',
                style: IconButton.styleFrom(
                  backgroundColor: Colors.black.withValues(alpha: 0.45),
                ),
                icon: const Icon(Icons.arrow_back_rounded),
                onPressed: () => Navigator.maybePop(context),
              ),
            ),
            flexibleSpace: FlexibleSpaceBar(
              background: Stack(
                fit: StackFit.expand,
                children: [
                  //La imagen vuela desde el póster gracias a Hero
                  Hero(
                    tag: etiquetaHero(contenido),
                    flightShuttleBuilder: vueloDelPoster,
                    child: ImagenContenido(
                      url: contenido.imagenHeroUrl ?? contenido.imagenUrl,
                      tipo: contenido.tipo,
                      tamanoIcono: 96,
                    ),
                  ),
                  //Degradado inferior hacia el color de fondo
                  const DecoratedBox(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        stops: [0.45, 1],
                        colors: [Colors.transparent, AppColores.fondo],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),

          //INFORMACIÓN DEL CONTENIDO
          SliverToBoxAdapter(
            child: Padding(
              padding: EdgeInsets.fromLTRB(margen, 8, margen, 0),
              //En pantallas anchas limitamos el ancho para que se lea bien
              child: Align(
                alignment: Alignment.topLeft,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 760),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(contenido.titulo, style: textos.headlineMedium),
                      const SizedBox(height: 12),
                      //Chips: tipo, duración y etiqueta
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          Chip(
                            avatar: Icon(
                              iconoDeTipo(contenido.tipo),
                              color: colorTipo,
                            ),
                            backgroundColor: colorTipo.withValues(alpha: 0.18),
                            label: Text(contenido.tipo.toUpperCase()),
                          ),
                          Chip(
                            avatar: const Icon(Icons.schedule_rounded),
                            label: Text(contenido.duracion.toUpperCase()),
                          ),
                          const Chip(
                            avatar: Icon(Icons.auto_awesome_rounded),
                            label: Text('UDIT ORIGINAL'),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      Text(contenido.descripcion, style: textos.bodyLarge),
                      const SizedBox(height: 24),
                      //Botón principal ancho, según el tipo de contenido
                      SizedBox(
                        width: double.infinity,
                        child: _BotonPrincipal(contenido: contenido),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),

          //MÁS COMO ESTO: fila con contenidos del mismo tipo
          SliverToBoxAdapter(
            child: FilaCategoria(
              titulo: 'Más como esto',
              contenidos: similares,
            ),
          ),
          const SliverToBoxAdapter(child: SizedBox(height: 32)),
        ],
      ),
    );
  }
}

//Botón principal de la ficha:
//- Vídeo: "Reproducir"  - Juego: "Jugar en UDIT Arcade"  - Otros: "Próximamente"
class _BotonPrincipal extends StatelessWidget {
  final Contenido contenido;

  const _BotonPrincipal({required this.contenido});

  @override
  Widget build(BuildContext context) {
    if (contenido.videoUrl != null) {
      return ElevatedButton.icon(
        icon: const Icon(Icons.play_arrow_rounded),
        label: const Text('Reproducir'),
        onPressed: () => Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => ReproductorScreen(contenido: contenido),
          ),
        ),
      );
    }
    if (contenido.tipo == 'Juego') {
      return ElevatedButton.icon(
        icon: const Icon(Icons.sports_esports_rounded),
        label: const Text('Jugar en UDIT Arcade'),
        onPressed: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => const ArcadeScreen()),
        ),
      );
    }
    //onPressed: null deja el botón deshabilitado
    return ElevatedButton.icon(
      icon: const Icon(Icons.lock_clock_rounded),
      label: const Text('Próximamente'),
      onPressed: null,
    );
  }
}
