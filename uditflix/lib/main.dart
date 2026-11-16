import 'package:flutter/material.dart';
import 'screens/catalogo_screen.dart';

//Punto de entrada de aplicación

void main() {
  runApp(const MyApp());
}

//Widge principal de UDITflix

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      //Nombre de nuestra aplicación
      title: 'UDITflix',
      //Ocultamos la etiqueta DEBUG
      debugShowCheckedModeBanner: false,
      //Configuración visual general
      theme: ThemeData(
        useMaterial3: true,
        //Color principal de nuestra aplicación
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF5B2D90)
          ),
          //Color de fondo de pantallas
          scaffoldBackgroundColor: const Color(0xFFF7F5FA),
          //Aspecto de la barra superior
          appBarTheme: const AppBarTheme(
            backgroundColor: Color(0xFF5B2D90),
            foregroundColor: Colors.white,
            centerTitle: false,
          )
        ),
        home: const CatalogoScreen(),
      );
  }
}