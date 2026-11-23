import 'package:flutter/material.dart';
import '../screens/arcade_screen.dart';
import '../theme/app_theme.dart';

//TARJETA-BANNER DE UDIT ARCADE
//Acceso llamativo al videojuego desde el catálogo.
class BannerArcade extends StatelessWidget {
  const BannerArcade({super.key});

  @override
  Widget build(BuildContext context) {
    final margen = Medidas.margen(MediaQuery.sizeOf(context).width);
    final textos = Theme.of(context).textTheme;

    return Padding(
      padding: EdgeInsets.fromLTRB(margen, 24, margen, 0),
      child: Card(
        child: InkWell(
          onTap: () => Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const ArcadeScreen()),
          ),
          child: Ink(
            height: 140,
            //Degradado del morado UDIT al cian de los juegos
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [AppColores.morado, AppColores.acento, AppColores.juego],
              ),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Row(
              children: [
                //Icono del mando dentro de un círculo translúcido
                Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: Colors.white.withValues(alpha: 0.18),
                  ),
                  child: const Icon(Icons.sports_esports_rounded, size: 40),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'UDIT ARCADE',
                        style: textos.labelSmall?.copyWith(
                          color: AppColores.texto,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Juega a UDIT Flappy',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: textos.titleLarge,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Toca para volar y bate tu récord',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: textos.bodyMedium?.copyWith(
                          color: AppColores.texto.withValues(alpha: 0.85),
                        ),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right_rounded, size: 32),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
