package com.example.reto2e_commerceadaptativoresponsiveui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaPrincipal()
                }
            }
        }
    }
}

// MODELO DE DATOS
data class Producto(
    val nombre: String,
    val precio: String,
    val imagenResId: Int
)

// DATOS DE PRUEBA
val catalogoCartas = listOf(
    Producto("Charizard ex (Shiny)", "120,00 €", R.drawable.charizard),
    Producto("Pikachu (Promo)", "450,00 €", R.drawable.pikachu),
    Producto("Mewtwo EX (Shiny)", "35,50 €", R.drawable.mewtwo),
    Producto("Mega Rayquaza ex", "85,00 €", R.drawable.rayquaza),
    Producto("Umbreon ex", "110,00 €", R.drawable.umbreon),
    Producto("Lugia ex", "50,00 €", R.drawable.lugia)
)

// PANTALLA PRINCIPAL CON SCAFFOLD Y TOPAPPBAR (SIN ICONOS)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal() {
    var cargando by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Simulación de carga asíncrona
    LaunchedEffect(Unit) {
        delay(1200)
        cargando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚡ PokeCard Shop", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cargando) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando catálogo...", fontSize = 16.sp)
                }
            } else {
                CatalogoCartas(
                    productos = catalogoCartas,
                    onAñadirAlCarrito = { producto ->
                        scope.launch {
                            snackbarHostState.showSnackbar("${producto.nombre} añadido al carrito")
                        }
                    }
                )
            }
        }
    }
}

// CATÁLOGO CON LAZYVERTICALGRID RESPONSIVO
@Composable
fun CatalogoCartas(
    productos: List<Producto>,
    onAñadirAlCarrito: (Producto) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(productos) { producto ->
            TarjetaProducto(
                producto = producto,
                onAñadirAlCarrito = { onAñadirAlCarrito(producto) }
            )
        }
    }
}

// TARJETA DE PRODUCTO OPTIMIZADA
@Composable
fun TarjetaProducto(
    producto: Producto,
    onAñadirAlCarrito: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = producto.imagenResId),
                    contentDescription = producto.nombre,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = producto.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = producto.precio,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onAñadirAlCarrito,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Añadir al carrito")
                }
            }
        }
    }
}