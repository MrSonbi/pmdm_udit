import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

//IMAGEN DE UN CONTENIDO
//Carga una imagen de Internet de forma segura:
//- Mientras carga se ve un degradado del color del tipo.
//- Si falla (por ejemplo, sin conexión) se ve el degradado con el icono del tipo.
//Así la app nunca se rompe aunque no haya internet.
class ImagenContenido extends StatelessWidget {
  final String? url;
  final String tipo;
  //Tamaño del icono que se muestra si la imagen falla
  final double tamanoIcono;

  const ImagenContenido({
    super.key,
    required this.url,
    required this.tipo,
    this.tamanoIcono = 40,
  });

  @override
  Widget build(BuildContext context) {
    final imagenUrl = url;
    if (imagenUrl == null) {
      return FondoDeTipo(tipo: tipo, tamanoIcono: tamanoIcono);
    }
    return Stack(
      fit: StackFit.expand,
      children: [
        //Fondo que se ve mientras la imagen aún no ha llegado
        FondoDeTipo(tipo: tipo, tamanoIcono: 0),
        Image.network(
          imagenUrl,
          fit: BoxFit.cover,
          //frameBuilder: la imagen aparece con un fundido suave al cargar.
          //(Antes del primer frame no dibuja nada y se ve el fondo de detrás)
          frameBuilder: (context, child, frame, cargadaAlInstante) {
            if (cargadaAlInstante) return child;
            return AnimatedOpacity(
              opacity: frame == null ? 0 : 1,
              duration: const Duration(milliseconds: 400),
              curve: Curves.easeOut,
              child: child,
            );
          },
          //loadingBuilder: mientras se descargan los datos mostramos el degradado
          loadingBuilder: (context, child, progreso) {
            if (progreso == null) return child;
            return FondoDeTipo(tipo: tipo, tamanoIcono: 0);
          },
          //errorBuilder: si la imagen falla, degradado con el icono del tipo
          errorBuilder: (context, error, stackTrace) =>
              FondoDeTipo(tipo: tipo, tamanoIcono: tamanoIcono),
        ),
      ],
    );
  }
}

//Degradado del color del tipo de contenido, con su icono opcional
class FondoDeTipo extends StatelessWidget {
  final String tipo;
  //Si es 0 no se muestra el icono
  final double tamanoIcono;

  const FondoDeTipo({super.key, required this.tipo, this.tamanoIcono = 40});

  @override
  Widget build(BuildContext context) {
    final color = AppColores.deTipo(tipo);
    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            color.withValues(alpha: 0.55),
            AppColores.superficie,
          ],
        ),
      ),
      child: tamanoIcono > 0
          ? Center(
              child: Icon(
                iconoDeTipo(tipo),
                size: tamanoIcono,
                color: color.withValues(alpha: 0.8),
              ),
            )
          : const SizedBox.expand(),
    );
  }
}
