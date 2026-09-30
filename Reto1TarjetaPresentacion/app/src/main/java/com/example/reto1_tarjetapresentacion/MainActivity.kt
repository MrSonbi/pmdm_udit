package com.example.reto1_tarjetapresentacion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.reto1_tarjetapresentacion.ui.theme.Reto1TarjetaPresentacionTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }

            Reto1TarjetaPresentacionTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TargetaPresentacion(
                        isDarkMode = isDarkMode,
                        onToggleTheme = { isDarkMode = !isDarkMode }
                    )
                }
            }
        }
    }
}

@Composable
fun TargetaPresentacion(
    isDarkMode: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- FOTO DE PERFIL COMO BOTÓN DE MODO OSCURO ---
        Image(
            painter = painterResource(id = R.drawable.foto_perfil),
            contentDescription = "Cambiar modo oscuro",
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .clickable { onToggleTheme() }, // Al hacer clic se alterna el tema
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nombre
        Text(
            text = "Nolan Martínez Gómez",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Rol
        Text(
            text = "Estudiante de DAM",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Botón GitHub
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MrSonbi"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6E40C4),
                contentColor = Color.White
            )
        ) {
            Text(text = "Mi perfil de GitHub")
        }

        Spacer(modifier = Modifier.height(16.dp))

        //Botón LinkedIn
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0077B5),
                contentColor = Color.White
            )
        ) {
            Text(text = "Mi perfil de LinkedIn")
        }

        Spacer(modifier = Modifier.height(16.dp))

        //Botón CV
        Button(
            onClick = {
                try {
                    // 1. Copiamos el recurso raw a un archivo temporal local
                    val inputStream = context.resources.openRawResource(R.raw.cv)
                    val file = File(context.cacheDir, "cv.pdf")
                    file.outputStream().use { inputStream.copyTo(it) }

                    // 2. Generamos la Uri segura con FileProvider
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )

                    // 3. Abrimos el PDF
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1DB954),
                contentColor = Color.White
            )
        ) {
            Text(text = "Ver CV (PDF)")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TargetaPreview() {
    Reto1TarjetaPresentacionTheme {
        TargetaPresentacion()
    }
}