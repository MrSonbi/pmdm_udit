import 'package:flutter/material.dart';
import 'screens/catalogo_screen.dart';
import 'theme/app_theme.dart';

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
      //Tema oscuro de UDITflix (colores, textos, botones...).
      //Está definido en lib/theme/app_theme.dart
      theme: AppTheme.oscuro,
      themeMode: ThemeMode.dark,
      home: const CatalogoScreen(),
    );
  }
}
