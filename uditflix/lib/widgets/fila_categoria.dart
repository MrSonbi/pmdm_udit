import 'package:flutter/material.dart';
import '../models/contenido.dart';
import '../theme/app_theme.dart';
import 'poster_card.dart';

//FILA HORIZONTAL DE UNA CATEGORÍA
//Título de la categoría y una lista horizontal de pósters.
//Aparece con un fundido suave (y un pequeño desplazamiento hacia arriba).
class FilaCategoria extends StatelessWidget {
  final String titulo;
  final List<Contenido> contenidos;
  //Posición de la fila: cuanto más abajo, más tarda en aparecer
  final int orden;

  const FilaCategoria({
    super.key,
    required this.titulo,
    required this.contenidos,
    this.orden = 0,
  });

  @override
  Widget build(BuildContext context) {
    //Si no hay contenidos no dibujamos la fila
    if (contenidos.isEmpty) return const SizedBox.shrink();

    final anchoPantalla = MediaQuery.sizeOf(context).width;
    final margen = Medidas.margen(anchoPantalla);
    final anchoPoster = Medidas.anchoPoster(anchoPantalla);
    //Espacio extra arriba y abajo para que no se recorte la sombra
    const espacioSombra = 12.0;

    //Animación de aparición: opacidad de 0 a 1
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: Duration(milliseconds: 450 + 150 * orden),
      curve: Curves.easeOut,
      builder: (context, valor, child) => Opacity(
        opacity: valor,
        child: Transform.translate(
          offset: Offset(0, 24 * (1 - valor)),
          child: child,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: EdgeInsets.fromLTRB(margen, 20, margen, 0),
            child: Text(titulo, style: Theme.of(context).textTheme.titleLarge),
          ),
          SizedBox(
            height: Medidas.altoPoster(anchoPantalla) + espacioSombra * 2,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              padding: EdgeInsets.symmetric(
                horizontal: margen,
                vertical: espacioSombra,
              ),
              itemCount: contenidos.length,
              itemBuilder: (context, index) => Padding(
                padding: const EdgeInsets.only(right: 12),
                child: PosterCard(
                  contenido: contenidos[index],
                  ancho: anchoPoster,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
