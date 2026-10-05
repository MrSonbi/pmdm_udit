package com.example.burgershop

import android.media.Image
import android.os.Bundle
import android.os.PersistableBundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.burgershop.ui.theme.BurgershopTheme
import java.nio.file.WatchEvent

//ACTIVITY PRINCIPAL
// La puerta de entrada de app
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            //MaterialTheme: aplica los colores y tipografías por defecto a tod0 lo que hay dentro
            MaterialTheme {
                Surface(
                    //Surface: el lienzo de fondo que ocupa la pantalla
                    modifier = Modifier.fillMaxSize()
                ) {
                    CatalogoHamburguesas(catalogoHamburguesas)
                }
            }
        }
    }
}

//MODELO DE DATOS
//El "value" que define que información tiene cada producto
data class Producto(
    val nombre: String,
    val precio: String,
    val imanResId: Int //el identificador de la imagen en res/drawable
)

//DATOS DE PRUEBA (hardcodeados)
//De momento viven aquí mismo, en el código. No vienen de ningún servidor ni base de datos

val catalogoHamburguesas = listOf(
    Producto(
        "Clásico con Queso",
        "6,50 €",
        R.drawable.burger_clasica
    ), Producto(
        "BBQ Bacon",
        "7,90 €",
        R.drawable.burger_bbq
    ), Producto(
        "Doble Carne",
        "8,50 €",
        R.drawable.burger_doble
    ), Producto(
        "Vegetariana",
        "7,20 €",
        R.drawable.burger_vegetariana
    ), Producto(
        "Picante Jalapeño",
        "7,80 €",
        R.drawable.burger_picante
    ), Producto(
        "Pollo Crispy",
        "6,90 €",
        R.drawable.burger_pollo
    )
)

//CATÁlogo
//LazyColumn: pinta una lista que se puede recorrer en scroll
//Vertical solo dibuja de memoria lo que se ve en pantalla (por eso se llama "lazy", perezoso): es eficiente aunque la lista tenga cientos de elementos
@Composable
fun CatalogoHamburguesas(productos: List<Producto>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        //margen alrededor de toda la lista
        contentPadding = PaddingValues(16.dp),
        //espacio entra una tarjeta y la siguiente
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(productos) { producto ->
            TargetaProducto(producto)
        }
    }
}

//TARGETA DE PRODUCTO
//Una "caja" (Card) una imagen arriba y datos + botón
@Composable
fun TargetaProducto(producto: Producto) {

    //Card: una superficie elevada, con sombra y bordes redondeados por defecto - Ideal para agrupar visualmente la info de un producto

    Card(
        modifier = Modifier.fillMaxSize()
    ) {
        //Column: apila sus elementos de arriba = abajo (flexbox)
        Column {
            Image(
                painter = painterResource(
                    id = producto.imanResId
                ),
                //para accesibilidad (lectores de pantalla)
                contentDescription = producto.nombre,
                modifier = Modifier
                    .fillMaxSize()//Ocupa tod0 el ancho de la tarjeta
                    .height(180.dp),//alto - fijo es justo -- "se rompe al rotar la pantalla"
                contentScale = ContentScale.Crop//Recorta la imagen sin deformarla
            )

            //Segunda Column, con margen interior, para el texto y el botón
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = producto.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = producto.precio,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("Añadir al carrito")
                }
            }
        }
    }
}