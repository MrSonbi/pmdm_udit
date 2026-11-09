# Fase 2 — Filtros configurables con SQLite

## Qué cambia respecto a la Fase 1

Los prefijos ya no están escritos en el código: se guardan en una base de datos SQLite y se gestionan desde la app (añadir, editar, eliminar y activar o desactivar cada uno). Se cargan al abrir la app y el servicio decide con ellos. El `400` de la Fase 1 sigue bloqueando porque se crea como primera regla cuando se crea la base de datos.

## Decisiones técnicas

**Variables o base de datos.** Una variable vive en memoria y se pierde al cerrar la app o cuando Android mata el proceso. La base de datos se guarda en el almacenamiento privado de la app y sobrevive a cierres y reinicios, que es lo que necesita un filtro configurable.

**SQLite en Android.** Uso `SQLiteOpenHelper` (clase `BaseDatos`) y no Room: el enunciado habla de SQLite y así evito los procesadores de anotaciones (KSP o kapt), que son otra fuente de fallos de compilación. `onCreate` crea la tabla y siembra el `400`; `onUpgrade` queda preparado porque la Fase 3 cambiará el esquema. La tabla `reglas` tiene `id`, `patron` (único y ya normalizado), `etiqueta` (opcional), `activa` y `creada_en`.

**CRUD.** Las cuatro operaciones están en la interfaz: crear (alta de un prefijo), leer (la lista se carga al abrir la app), actualizar (editar y activar o desactivar) y borrar (con confirmación). Antes de guardar, una función pura valida el patrón: lo normaliza y rechaza el vacío, las letras, los duplicados y los de más de 15 dígitos. Si tiene 2 dígitos o menos pide confirmación, porque bloquearía muchísimas llamadas.

**Lista en pantalla.** Una `LazyColumn` con una tarjeta por regla (prefijo, etiqueta, interruptor de activa, editar y eliminar) y un diálogo para el alta y la edición.

**Cómo accede el servicio a los filtros.** El servicio no consulta SQLite durante la llamada. `RepositorioReglas` guarda en memoria una copia inmutable (`@Volatile`) de las reglas activas. Se carga de la base de datos la primera vez que se pide (el proceso puede arrancar solo porque entra una llamada) y se vuelve a cargar después de cada alta, edición o baja. La decisión la toma `MotorFiltrado.evaluar(numero, reglas)`, una función pura que no depende de Android, y el probador usa exactamente la misma.

**Qué pasa si cambio un filtro con la app en marcha.** Cada cambio actualiza la base de datos y recarga la copia en memoria, así que la siguiente llamada ya usa la regla nueva. Se comprobó en el emulador: con el `400` desactivado la llamada suena, y al volver a activarlo se bloquea.

**Otras decisiones.**

- `onScreenCall` responde exactamente una vez y, si algo falla, deja pasar la llamada (es mejor que bloquear por error). El log solo muestra el número enmascarado.
- `minSdk` pasa a 29 y el servicio queda declarado en el Manifest.
- `normalizar()` trata también el `00` inicial como `+`, para que `0044…` y `+44…` sean el mismo número.
- Al volver a la app se comprueba de nuevo el rol, por si el usuario lo cambió en los ajustes del sistema.
- No usé iconos para no añadir dependencias; los botones son de texto.

## Dificultades encontradas

- **Servicio sin declarar en el Manifest.** Lo detecté al revisar el proyecto al empezar la fase (ver `FASE1.md`). Sin esa declaración el sistema no puede usar el servicio.
- **`minSdk` 24 frente a APIs de Android 10.** Subí `minSdk` a 29 en lugar de dejar `@RequiresApi`, que solo evita el aviso del compilador.
- **Un prefijo `+34` bloquearía todas las llamadas.** `normalizar("+34")` devuelve una cadena vacía y `startsWith("")` es siempre verdadero. La validación lo rechaza.
- **`normalizar()` elimina las letras.** Sin más, `400abc` se habría guardado como `400`. La validación rechaza la entrada original si contiene letras.
- **Versión de Java.** Había un Java 25 en el `PATH` y Gradle necesita el JDK 17, así que en cada terminal fijo `JAVA_HOME` y `Path` al JDK 17.

## Pruebas

**Automáticas.** `testDebugUnitTest`: 30 tests sin fallos al cerrar la fase: normalización (7), motor (7), validación (12), enmascarado (3) y el de ejemplo (1).

**Manuales** (emulador de Android Studio y números ficticios):

- Conceder el rol y comprobar el estado.
- Probador con `+34 400 123 456`.
- Añadir, editar, desactivar y borrar reglas.
- Casos de validación: duplicado, `+34`, letras, 16 dígitos y prefijo corto.
- Llamada entrante simulada (Extended controls → Phone) con la app cerrada del todo.
- Cambio de un filtro con la app en marcha.

Las capturas están en `docs/evidencias/`.

## Limitaciones

- Solo llegan al servicio las llamadas de números que no están en la agenda, y no se debe suponer que llegan todas.
- Todas las reglas bloquean: todavía no hay lista blanca ni prioridades (Fase 3).
- La app y el servicio comparten proceso, que es lo que asume la copia en memoria de las reglas.
