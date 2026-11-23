//MODELO CONTENIDO

//Eta clase presenta un contenido de nuestra plataforma UDITflix
//Por ejemplo: una película, un cómic o un videojuego

//En programación orientada a objetos, una clase define que características tendrán los objetos que creamos

class Contenido {
    final int id;
    final String titulo;
    final String descripcion;
    //Tipo de contenido: vídeo, cómic o juego
    final String tipo;
    final String duracion;
    //URL del vídeo de demostración (solo para contenidos reproducibles)
    final String? videoUrl;
    //Imagen vertical (póster 2:3) para las filas del catálogo
    final String? imagenUrl;
    //Imagen horizontal grande para el destacado y la cabecera de la ficha
    final String? imagenHeroUrl;

    //Constructor: permite crear nuevos Contenidos

    const Contenido ({
        required this.id,
        required this.titulo,
        required this.descripcion,
        required this.tipo,
        required this.duracion,
        this.videoUrl,
        this.imagenUrl,
        this.imagenHeroUrl,
    });
}
