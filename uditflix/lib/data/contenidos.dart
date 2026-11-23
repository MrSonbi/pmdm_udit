//DATOS SIMULADOS DE UDITFLIX

//En esta primera versión no tenemos una API ni una base de datos.
//Por eso utilizamos una lista de objetos creada manualmente

//Más adelante sustituiremos estos datos por información que recibiremos desdde el servidor mediante una API REST

import '../models/contenido.dart';

//Vídeos de demostración públicos (comprobados: responden 200 video/mp4)
const String _videoMariposa =
    'https://flutter.github.io/assets-for-api-docs/assets/videos/butterfly.mp4';
const String _videoAbeja =
    'https://flutter.github.io/assets-for-api-docs/assets/videos/bee.mp4';

//Imágenes de ejemplo de picsum.photos. La "semilla" hace que cada
//contenido tenga siempre la misma imagen.
const String _poster = 'https://picsum.photos/seed/uditflix';
const String _hero = 'https://picsum.photos/seed/uditflixhero';

//Lista de contenidos que aparecerán en nuestra aplicación
//Cada elemento de lista es un objeto de tipo contenido

const List<Contenido> contenidos = [
  Contenido(
  id: 1,
  titulo: 'Introducción a Flutter',
  descripcion: 'Aprende a crear aplicaciones multiplataforma',
  tipo: 'Vídeo',
  duracion: '25 minutos',
  videoUrl: _videoMariposa,
  imagenUrl: '${_poster}1/400/600',
  imagenHeroUrl: '${_hero}1/1200/700',
  ),
  Contenido(
  id: 2,
  titulo: 'Desarrollo con Java',
  descripcion: 'Introducción orientada a objetos',
  tipo: 'Vídeo',
  duracion: '25 minutos',
  videoUrl: _videoAbeja,
  imagenUrl: '${_poster}2/400/600',
  imagenHeroUrl: '${_hero}2/1200/700',
  ),
  Contenido(
  id: 3,
  titulo: 'El mundo de los cómics',
  descripcion: 'Una colección de historias y aventuras ilustradas',
  tipo: 'Cómic',
  duracion: '19 páginas',
  imagenUrl: '${_poster}3/400/600',
  imagenHeroUrl: '${_hero}3/1200/700',
  ),
  Contenido(
  id: 4,
  titulo: 'Programación de videojuegos',
  descripcion: 'Descubre los fundamentos de la lógicas de vieojuegos',
  tipo: 'Juego',
  duracion: '20 minutos',
  imagenUrl: '${_poster}4/400/600',
  imagenHeroUrl: '${_hero}4/1200/700',
  ),
  //Contenidos añadidos para que cada fila del catálogo tenga varios elementos
  Contenido(
  id: 5,
  titulo: 'Diseño de interfaces',
  descripcion: 'Principios de diseño UX/UI para aplicaciones móviles',
  tipo: 'Vídeo',
  duracion: '18 minutos',
  videoUrl: _videoAbeja,
  imagenUrl: '${_poster}5/400/600',
  imagenHeroUrl: '${_hero}5/1200/700',
  ),
  Contenido(
  id: 6,
  titulo: 'Bases de datos en la nube',
  descripcion: 'Guarda y sincroniza los datos de tus apps en la nube',
  tipo: 'Vídeo',
  duracion: '30 minutos',
  videoUrl: _videoMariposa,
  imagenUrl: '${_poster}6/400/600',
  imagenHeroUrl: '${_hero}6/1200/700',
  ),
  Contenido(
  id: 7,
  titulo: 'Héroes del campus',
  descripcion: 'Un grupo de estudiantes de UDIT salva el campus a base de código',
  tipo: 'Cómic',
  duracion: '24 páginas',
  imagenUrl: '${_poster}7/400/600',
  imagenHeroUrl: '${_hero}7/1200/700',
  ),
  Contenido(
  id: 8,
  titulo: 'Código y viñetas',
  descripcion: 'Historias cortas sobre la vida de una programadora',
  tipo: 'Cómic',
  duracion: '32 páginas',
  imagenUrl: '${_poster}8/400/600',
  imagenHeroUrl: '${_hero}8/1200/700',
  ),
  Contenido(
  id: 9,
  titulo: 'Pixel Runner',
  descripcion: 'Diseña y prueba tu propio juego de plataformas 2D',
  tipo: 'Juego',
  duracion: '15 minutos',
  imagenUrl: '${_poster}9/400/600',
  imagenHeroUrl: '${_hero}9/1200/700',
  ),
  Contenido(
  id: 10,
  titulo: 'Física para juegos',
  descripcion: 'Gravedad, velocidad y colisiones explicadas paso a paso',
  tipo: 'Juego',
  duracion: '22 minutos',
  imagenUrl: '${_poster}10/400/600',
  imagenHeroUrl: '${_hero}10/1200/700',
  ),
];
