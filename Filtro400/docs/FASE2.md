# Fase 2 — Filtros configurables con SQLite

## 1. Decisiones técnicas y por qué

### Base de datos
- **`SQLiteOpenHelper` nativo**, sin Room/KSP/kapt: menos piezas en el build y es lo que pide el enunciado.
- Tabla `reglas`: `id` PK autoincrement, `patron` TEXT NOT NULL UNIQUE, `etiqueta` TEXT,
  `activa` INTEGER 0/1 y `creada_en` INTEGER (epoch ms).
- `DATABASE_VERSION = 1`. `onUpgrade` está preparado para que la Fase 3 añada columnas con
  `ALTER TABLE` y no borre datos.
- La regla `400` ("Llamadas comerciales España") se siembra **solo en `onCreate`**. Si el
  usuario la edita, desactiva o borra, no se vuelve a crear.

### Servicio: cómo accede a los filtros
- `RepositorioReglas` es un singleton con `applicationContext`, compartido por la UI y el
  servicio (mismo proceso).
- Mantiene una copia inmutable en memoria (`@Volatile`) de las **reglas activas**.
  - Se carga de SQLite la primera vez que se pide (el proceso puede arrancar solo porque
    entra una llamada).
  - Se refresca tras cada alta, edición, baja o cambio de `activa`.
- `onScreenCall` no consulta la tabla: lee esa copia y llama a `MotorFiltrado.evaluar()`.
  Así la llamada no espera una consulta SQL en cada número.
- **Si se modifica un filtro con la app en marcha**, la UI actualiza la BD y refresca la
  caché del singleton; la siguiente llamada ya usa la regla nueva. Si el proceso hubiera
  muerto, la primera llamada vuelve a cargar desde SQLite.

### Seguridad y respuesta a la llamada
- `onScreenCall` va dentro de `try/catch` y es **fail-open**: cualquier error permite la
  llamada. Se envía **una sola** `CallResponse`; el log enmascarado va después de responder.
- Se eliminó el log con el número completo. `util/Enmascarar.kt` deja, como máximo, seis
  caracteres visibles (`400123***`).
- El Manifest declara `FiltroLlamadasService` con `BIND_SCREENING_SERVICE`, `exported=true`
  y el intent-filter `android.telecom.CallScreeningService` (en la Fase 1 faltaba).
- `minSdk` sube de 24 a 29 porque `callDirection` y el rol exigen API 29. Se quitaron los
  `@RequiresApi` que sobraban.

### Motor y validación
- `MotorFiltrado.evaluar(numero, reglas)` es Kotlin puro: normaliza, ignora reglas inactivas,
  permite números vacíos/ocultos y elige la regla **más larga** si coinciden varias. Esa
  decisión ya prepara el desempate por especificidad de la Fase 3. Devuelve
  `Decision(accion, regla, motivo)`.
- `ValidacionReglas.validar()` es pura y se ejecuta antes de guardar:
  - rechaza letras/símbolos en la entrada cruda (`400abc`),
  - normaliza y rechaza vacío (`+34` se queda vacío),
  - rechaza `+` mal colocado y más de 15 dígitos,
  - detecta duplicados comparando patrones normalizados,
  - si quedan 2 dígitos o menos, pide confirmación a la UI.
- `normalizar()` convierte `00` inicial en `+` (0044 = +44) y sigue quitando `+34`/`0034`.

### UI
- `MainActivity` solo monta `PantallaInicio`; la lógica está en `ui/`.
- La pantalla tiene estado del rol, botón para pedirlo, probador, lista `LazyColumn`, alta,
  edición, borrado con confirmación, switch de activa y mensaje de lista vacía.
- El estado del rol se vuelve a comprobar en `ON_RESUME`, por si se cambia en Ajustes.
- El acceso a SQLite desde la UI se hace en `Dispatchers.IO`.
- No se añadieron dependencias. El BOM de Compose del proyecto no incluye
  `material-icons-core` de forma transitiva, así que se usaron botones de texto
  ("Editar", "Eliminar") en vez de añadir un artefacto nuevo.

## 2. Dificultades reales de esta sesión

1. **El servicio no estaba en el Manifest de la Fase 1.** El rol no podía funcionar bien.
   Se añadió el `<service>` con su permiso e intent-filter.
2. **`minSdk = 24` con APIs de 29.** El `@RequiresApi` no evita el fallo en ejecución.
   Se subió a 29 y se quitaron las anotaciones.
3. **Iconos no disponibles.** `Icons.Default.*` no compilaba porque
   `material-icons-core` no llega como dependencia transitiva. Para respetar "sin
   dependencias nuevas" se cambiaron los botones a texto.
4. **Compilaciones intermedias.** Al quitar `debeBloquearse()` de `ReglaBloqueo`, la UI y el
   servicio antiguos dejaban de compilar. Se añadió un puente temporal y se eliminó cuando
   ambos pasaron a usar el motor y el repositorio.
5. **Un test tenía un espacio en el nombre de la función** y no compilaba; se corrigió antes
   de ejecutar la suite.

## 3. Pruebas

### Automáticas ejecutadas
Comando (PowerShell, fijando solo el JDK 17 de `JAVA_HOME`):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug --console=plain
```

Salida real (resumen):

```text
> Task :app:compileDebugUnitTestKotlin
> Task :app:testDebugUnitTest
> Task :app:assembleDebug UP-TO-DATE

BUILD SUCCESSFUL in 1s
42 actionable tasks: 2 executed, 40 up-to-date
```

Resultados reales por clase (de `app/build/test-results/testDebugUnitTest`):

| Clase | Tests | Fallos | Errores |
|---|---:|---:|---:|
| `EnmascararTest` | 3 | 0 | 0 |
| `ExampleUnitTest` | 1 | 0 | 0 |
| `MotorFiltradoTest` | 7 | 0 | 0 |
| `ReglaBloqueoTest` | 7 | 0 | 0 |
| `ValidacionReglasTest` | 12 | 0 | 0 |
| **Total** | **30** | **0** | **0** |

APK generado en `app/build/outputs/apk/debug/app-debug.apk`.

### Checklist manual (para hacer tú)

Emulador de Android Studio: `Extended controls > Phone > llamada entrante` con un número
ficticio. En móvil real, usa solo números autorizados.

- [ ] Instalar el APK y abrir la app: aparece el filtro inactivo y la regla `400` activa.
- [ ] Pulsar "Activar filtro de llamadas", aceptar el diálogo del sistema y ver "Filtro ACTIVO".
- [ ] Probador con `+34 400 123 456`: "Se BLOQUEARÍA", regla `400`, normalizado `400123456`.
- [ ] Probador con `400555123`: "Se BLOQUEARÍA".
- [ ] Probador con `612345678`: "Pasaría".
- [ ] Probador con `+44 400 123 456`: "Pasaría" (no es un 400 español).
- [ ] Probador vacío: "Pasaría" (número oculto).
- [ ] Añadir `900` con etiqueta "Pruebas"; probar `900123456` y ver que bloquea.
- [ ] Editar `900` a `800`; comprobar que el probador cambia.
- [ ] Desactivar `800` con el switch; el probador debe permitir; volver a activarlo.
- [ ] Intentar añadir `400` de nuevo: error de duplicado.
- [ ] Intentar añadir `+34`: error porque queda vacío.
- [ ] Intentar añadir `400abc`: error de carácter no válido.
- [ ] Intentar añadir 16 dígitos: error de longitud.
- [ ] Añadir `44`: aparece la confirmación de prefijo corto; probar Cancelar y Guardar.
- [ ] Eliminar una regla con confirmación; comprobar que desaparece de la lista.
- [ ] Borrar `400`, cerrar y abrir la app: no se vuelve a sembrar.
- [ ] Quitar el rol en Ajustes del sistema, volver a la app: debe mostrarse inactivo.
- [ ] Llamada real/simulada desde `400123456`: no debe sonar; revisar Logcat (`Filtro400`)
  y comprobar que el número aparece enmascarado.
- [ ] Llamada desde un número permitido: debe sonar.
- [ ] Añadir un número como contacto y llamar: la app no lo recibe (limitación de Android).

Capturas (pegar aquí):

1. [Captura: pantalla principal con la lista]
2. [Captura: probador bloqueando un 400]
3. [Captura: diálogo de alta con error de validación]
4. [Captura: llamada entrante bloqueada / Logcat enmascarado]

## 4. Limitaciones conocidas

- Requiere Android 10 (API 29) o superior por el rol de filtrado y `callDirection`.
- Android solo entrega al `CallScreeningService` llamadas de números **que no están en la
  agenda**. Los contactos no pasan por este filtro y no se puede asumir que llegan todas.
- La app necesita el rol `ROLE_CALL_SCREENING`; si otro servicio lo tiene o el fabricante lo
  restringe, el filtro no actúa.
- Los números ocultos llegan vacíos: se permiten por seguridad (no se bloquea a ciegas).
- La coincidencia es por **prefijo**, sin expresiones regulares ni comodines.
- Un prefijo de 1–2 dígitos puede bloquear muchísimas llamadas; solo se avisa, no se impide.
- La regla `400` es editable/borrable como cualquier otra y no se vuelve a sembrar.
- La caché asume un único proceso (el actual). Si en el futuro se usaran procesos separados,
  habría que invalidarla entre procesos.
- Si falla la lectura de SQLite durante una llamada, se permite la llamada (fail-open).
- La primera llamada tras arrancar el proceso puede leer la BD una vez para llenar la caché;
  las siguientes ya no.
- No se implementa nada de la Fase 3: registro, listas blancas, CSV, estadísticas ni
  navegación inferior.
