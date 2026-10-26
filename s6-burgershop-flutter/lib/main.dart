import 'package:flutter/material.dart';

//main() el punto de arranque de CUALQUIER app Flutter.
//Siempre existe, un runApp() = onCreate() en kotlin
void main() {
  runApp(const BurgerShopApp());
}

//APP RAÍZ

class BurgerShopApp extends StatelessWidget {

  //const + super.key: es nuestro constructor - Flutter pide siempre ese comando
  const BurgerShopApp({super.key});

  //build(): es el métod0 que tod0 widget debe tener. Aquí es donde dibujas tod0 lo que se ve en la pantalla
  @override
  Widget build(BuildContext context) {
    //MaterialApp: el "contenedor raíz" de toda la app
    return MaterialApp(
      debugShowCheckedModeBanner: false, //quita la cinta roja
      title: 'BurgerShop',
      theme: ThemeData(
        //COLOR DE LA APP: esta es la única línea para cambiar el aspecto
        colorSchemeSeed: Colors.red,
        useMaterial3: true,
      ),
      home: const PantallaCatalogo(), //la primera pantalla que se ve al abrir la app
    );
  }
}

//MODELO DE DATOS
//En Dart no existe "data class" como en kotlin: una clase normal con un constructor que recibe los campos para hacer el mismo trabajo

class Producto {
  //final: el valor no cambia una vez creado el objeto (val en kotlin)
  final String nombre;
  final String precio; //Al modificar a carro de compras modificamos la variable
  final String imagenAsset;

  //Constructor: "required" obliga a pasar siempre estos tres datos al crear Producto
  Producto({
    required this.nombre,
    required this.precio,
    required this.imagenAsset
  });
}

//DATOS DE PRUEBA
//Los mismos productos en kotlin, apuntando a los mismos Archivos
//final List<Producto> = una lista que no se puede reasignar, pero el contenido si podríamos modificarlo

final List<Producto> catalogoHamburguesas = [
  Producto (
    nombre: 'Clásica con queso',
    precio: '6,50 €',
    imagenAsset: 'assets/images/burger_clasica.jpg'
  ),
  Producto (
    nombre: 'BBQ Bacon',
    precio: '7,90 €',
    imagenAsset: 'assets/images/burger_bbq.jpg'
  ),
  Producto (
    nombre: 'Doble carne',
    precio: '8,50 €',
    imagenAsset: 'assets/images/burger_doble.jpg'
  ),
  Producto (
    nombre: 'Vegetariana',
    precio: '7,20 €',
    imagenAsset: 'assets/images/burger_vegetariana.jpg'
  ),
  Producto (
    nombre: 'Picante jalapeño',
    precio: '7,80 €',
    imagenAsset: 'assets/images/burger_picante.jpg'
  ),
  Producto (
      nombre: 'Pollo crispy',
      precio: '6,90 €',
      imagenAsset: 'assets/images/burger_pollo.jpg'
  )
];

//PANTALLA 1: CATÁLOGO RESPONSIVE (RETO 2)

class PantallaCatalogo extends StatelessWidget {
  const PantallaCatalogo({super.key});
  @override
  Widget build(BuildContext context) {
    //Scaffold: La estructura estándar de una pantalla Android/iOS

    return Scaffold(
      //Barra superior con el título
      appBar: AppBar(title: const Text('🍔 BurgerShop 🍔')),
      //LayoutBuilder: nos da el ANCHO disponible de la pantalla
      body: LayoutBuilder(
        builder: (context, constraints) {
          //Mismo cálculo que GridCells.Adaptive(minSize = 250.dp)
          //"cuántas tarjetas de al menos 250 caben en el ancho disponible
          const double anchoMinimo = 250;
          const double espacio = 16;//hueco entre las tarjetas
          final double anchoUtil = constraints.maxWidth - (espacio * 2);
          int columnas = ((anchoUtil + espacio) / (anchoMinimo + espacio)).floor();
          if (columnas < 1) columnas = 1;//como mínimo 1 columna

          //GridView.Builder: Equivalente de LazyVerticalGrid en Compose

          return GridView.builder(
            padding: const EdgeInsets.all(espacio),//margen alrededor de la rejilla
            gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: columnas,//móvil vertical 1; horizontal 3
              mainAxisSpacing: espacio,//hueco vertical entre targetas
              crossAxisSpacing: espacio, //hueco horizontal entre columnas

              //ALTO fijo de cada tarjeta

              mainAxisExtent: 340,
            ),
            itemCount: catalogoHamburguesas.length, //cuantos elementos tiene la lista
            itemBuilder: (context, index) {
              final producto = catalogoHamburguesas[index];
              return TargetaProducto(producto: producto);
            },
          );
        },
      ),
    );
  }
}

//TARGETA DE PRODUCTO

class TargetaProducto extends StatelessWidget {
  final Producto producto;//Este es el dato para dibujar
  const TargetaProducto({super.key, required this.producto});

  @override

  Widget build(BuildContext context) {
    //Card: una caja con sombras y bordes redondeados por defecto
    //Es igual a nuestro Card de Compose
    return Card(
      margin: EdgeInsets.zero,//el hueco entre las targetas
      clipBehavior: Clip.antiAlias,//recorta la imagen a las esquinas redondeadas

      //Column: apila elementos de arriba a abajo
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,//alinea el texto a la izquierda
        children: [
          //Expanded: La imagen ocupa tod0 el alto
          Expanded(
            child: Image.asset(
              producto.imagenAsset,
              width: double.infinity,//ocupa tod0 el ancho disponible
              fit: BoxFit.cover//= ContentScale.Crop en Kotlin: recorta el contenido sin deformar
            ),
          ),
          //Segunda Column, con margen interior, para el texto y el botón
          Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              mainAxisSize: MainAxisSize.min,//ocupa solo lo que necesita su contenido
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  producto.nombre,
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)
                ),
                const SizedBox(height: 4),//Spacer(height) en kotlin - hueco pequeño
                Text(
                    producto.precio,
                    style: TextStyle(fontSize: 16, color: Theme.of(context).colorScheme.primary)//color del tema actual
                ),
                const SizedBox(height: 8),//Spacer(height) en kotlin - hueco pequeño
                SizedBox(
                  width: double.infinity,//el boton ocupa tod0 el ancho
                  child: ElevatedButton(
                    onPressed: () {
                      //de momento no hace nada pero en consola dirá producto añadido al carrito
                      print('¡Añadido: ${producto.nombre} al carrito!');
                    },
                    child: const Text("Añadir al carrito")
                  )
                )
              ]
            )
          )
        ]
      )
    );
  }
}