//Importamos flutter para utilizar sus componentes visuales
import 'package:flutter/material.dart';
//Importamos la lista de contenidos simulados
import '../data/contenidos.dart';
import '../theme/app_theme.dart';
//Widgets reutilizables del catálogo
import '../widgets/banner_arcade.dart';
import '../widgets/fila_categoria.dart';
import '../widgets/hero_destacado.dart';
//Pantalla a la que navegamos desde el icono de la barra
import 'arcade_screen.dart';

//PANTALLA DE CATÁLOGO (INICIO)
//Estilo plataforma de streaming: destacado arriba, filas por categoría
//y acceso a UDIT Arcade. Es Stateful porque escucha el scroll para
//oscurecer la barra superior.
class CatalogoScreen extends StatefulWidget {
  //Constructor de la pantalla
  const CatalogoScreen({super.key});

  @override
  State<CatalogoScreen> createState() => _CatalogoScreenState();
}

class _CatalogoScreenState extends State<CatalogoScreen> {
  //Controlador que nos dice cuánto se ha desplazado la pantalla
  final ScrollController _scroll = ScrollController();

  //Categorías que se muestran como filas (título visible y tipo del modelo)
  static const Map<String, String> _categorias = {
    'Vídeos': 'Vídeo',
    'Cómics': 'Cómic',
    'Juegos': 'Juego',
  };

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  //Método que construye la interfaz visual
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      //El contenido pasa por debajo de la barra (la barra empieza transparente)
      extendBodyBehindAppBar: true,
      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(kToolbarHeight),
        //Solo se redibuja la barra cuando cambia el scroll
        child: ListenableBuilder(
          listenable: _scroll,
          builder: (context, _) {
            final desplazamiento = _scroll.hasClients ? _scroll.offset : 0.0;
            //0 = transparente, 1 = fondo opaco (a los 200 px de scroll)
            final opacidad = (desplazamiento / 200).clamp(0.0, 1.0);
            return AppBar(
              backgroundColor: AppColores.fondo.withValues(alpha: opacidad),
              title: const _Logo(),
              //Acceso directo a la sección de videojuegos
              actions: [
                IconButton(
                  tooltip: 'UDIT Arcade',
                  icon: const Icon(Icons.sports_esports_rounded),
                  onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const ArcadeScreen()),
                  ),
                ),
                const SizedBox(width: 8),
              ],
            );
          },
        ),
      ),
      //CustomScrollView permite combinar distintos bloques (slivers)
      //en un único desplazamiento vertical
      body: CustomScrollView(
        controller: _scroll,
        slivers: [
          //a) Contenido destacado: el primero de la lista
          SliverToBoxAdapter(
            child: HeroDestacado(contenido: contenidos.first),
          ),
          //b) Una fila horizontal por cada categoría
          for (final (indice, categoria) in _categorias.entries.indexed)
            SliverToBoxAdapter(
              child: FilaCategoria(
                titulo: categoria.key,
                orden: indice,
                contenidos: contenidos
                    .where((c) => c.tipo == categoria.value)
                    .toList(),
              ),
            ),
          //c) Banner de acceso a UDIT Arcade
          const SliverToBoxAdapter(child: BannerArcade()),
          const SliverToBoxAdapter(child: SizedBox(height: 40)),
        ],
      ),
    );
  }
}

//LOGO "UDITflix": "UDIT" con degradado morado y "flix" en blanco
class _Logo extends StatelessWidget {
  const _Logo();

  @override
  Widget build(BuildContext context) {
    final estilo = Theme.of(context).textTheme.headlineSmall?.copyWith(
          fontWeight: FontWeight.w900,
          letterSpacing: -0.5,
        );
    return Semantics(
      label: 'UDITflix',
      child: ExcludeSemantics(
        child: Stack(
          children: [
            //Texto completo (es el que encuentran los tests y lectores)
            Text('UDITflix', style: estilo),
            //Encima, "UDIT" pintado con un degradado
            ShaderMask(
              blendMode: BlendMode.srcIn,
              shaderCallback: (rect) => const LinearGradient(
                colors: [AppColores.acento, AppColores.juego],
              ).createShader(rect),
              child: Text('UDIT', style: estilo),
            ),
          ],
        ),
      ),
    );
  }
}
