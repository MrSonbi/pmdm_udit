# Fase 3 — Sistema completo de filtrado

## Qué incluye

La app tiene cuatro pantallas (Inicio, Reglas, Actividad y Ajustes) y estas funciones:

1. **Reglas:** números concretos y prefijos, cada uno con una acción (bloquear o permitir), una prioridad y un estado (activa o pausada). Se pueden crear, editar, borrar y pausar.
2. **Lista blanca:** reglas que permiten siempre, para que una llamada concreta entre aunque haya un bloqueo.
3. **Prioridades:** cada regla tiene una prioridad de 0 a 100 que decide los conflictos.
4. **Registro de actividad:** cada llamada que filtra la app queda anotada, con el número enmascarado.
5. **Importar y exportar** las reglas en CSV.
6. **Estadísticas:** llamadas bloqueadas y permitidas hoy y en los últimos 7 días, y las reglas que más bloquean.
7. **Simulador:** prueba un número sin recibir una llamada, con el mismo motor que usa el servicio real.

Se conserva todo lo anterior: el `400` sigue bloqueando por defecto.

## Decisiones técnicas

### 1. Cómo separé interfaz, base de datos y motor

- `modelo/`: `Regla`, `Accion` y `TipoRegla`. Kotlin puro.
- `motor/`: `MotorFiltrado` (la decisión) y `ValidacionReglas`. Kotlin puro, sin Android ni base de datos, por eso se prueban con JUnit en el ordenador.
- `data/`: `BaseDatos` (`SQLiteOpenHelper`), `AlmacenDatos` (el único sitio que crea la base de datos) y `RepositorioReglas` (CRUD y copia en memoria).
- `registro/`: `RepositorioRegistro`.
- `csv/`: lectura, exportación y planificador de la importación.
- `ui/`: pantallas de Compose y el tema visual.
- `util/`: `Enmascarar`.
- `FiltroLlamadasService` y `MainActivity` siguen en el paquete raíz porque el Manifest las referencia por nombre.

### 2. Estructura de tablas

**`reglas`**: `id`, `patron` (único, normalizado), `etiqueta`, `activa`, `creada_en`, `tipo` (`PREFIJO` o `NUMERO`), `accion` (`BLOQUEAR` o `PERMITIR`) y `prioridad`.

**`registro`**: `id`, `fecha`, `numero_enmascarado`, `decision`, `regla_id`, `regla_patron`, `regla_tipo`, `regla_prioridad`, `motivo` y `origen` (`REAL` o `SIMULADA`, con `CHECK`). Guarda una copia del patrón de la regla para que el historial siga siendo legible aunque la regla se borre. Tiene índices en `fecha` y en `regla_id`.

**Migración de la versión 1 a la 2.** Usa `ALTER TABLE ... ADD COLUMN` con valores por defecto (`PREFIJO`, `BLOQUEAR`, 0), sin `DROP` ni reconstruir la tabla, así que las reglas de la Fase 2 siguen bloqueando igual. No se vuelve a sembrar el `400`: si el usuario lo había borrado, sigue borrado (la semilla es solo de `onCreate`). `onCreate` crea ya el esquema completo de la versión 2 porque `SQLiteOpenHelper` no llama a `onUpgrade` en una instalación nueva.

**`patron` sigue siendo único.** Un mismo patrón no puede estar a la vez en bloqueo y en lista blanca; para cambiarlo se edita la regla. Cambiar la restricción `UNIQUE` obligaría a reconstruir la tabla.

### 3. Conflicto entre lista blanca y bloqueo

`MotorFiltrado.evaluar` es una función pura. Descarta las reglas pausadas y las que no coinciden, y entre las que quedan decide así:

| Orden | Criterio |
|---|---|
| 1 | Gana la mayor prioridad |
| 2 | En empate, el número exacto antes que el prefijo |
| 3 | En empate, el patrón más largo |
| 4 | En empate total, `PERMITIR` |

Si ninguna regla coincide, o el número está oculto, la llamada pasa. Por defecto las reglas que permiten nacen con prioridad 100 y las que bloquean con 0, así que la lista blanca gana salvo que el usuario cambie la prioridad. Ejemplo (números ficticios): con "bloquear prefijo 400" y "permitir el número 400555123", la llamada de ese número entra y el resto de los 400 se bloquean.

### 4. Cómo evito consultas lentas durante la llamada

- `onScreenCall` no consulta SQLite ni usa la red: lee la copia en memoria de las reglas activas. La base de datos solo se lee la primera vez que el proceso necesita las reglas, y la copia se recarga una sola vez tras cada cambio.
- La respuesta (`respondToCall`) se envía primero. El registro se escribe después, con una inserción síncrona de una fila dentro de un `try/catch`. Es síncrona a propósito: tras responder, el sistema puede desvincular el servicio y una corrutina podría cancelarse y perder el registro.
- Si cualquier cosa falla, la llamada se deja pasar (fail-open). Un error al registrar nunca cambia la decisión.
- El ajuste «guardar registro» se lee de `SharedPreferences`, no de SQLite.

### 5. Cómo compruebo que un cambio de filtros no da errores

- La validación y el motor son funciones puras con tests JVM, incluidos los casos límite (prefijo vacío, prioridad fuera de rango, empates).
- La importación de CSV primero **planifica** (función pura, con tests) y después **aplica** el plan en una única transacción: si algo falla, se deshace todo y no cambia nada.
- La migración tiene tests instrumentados con una base de datos temporal y una prueba manual en el emulador: con la app de la Fase 2 añadí una regla propia (`+34 600`) y borré el `400`; tras instalar encima la versión nueva, la regla seguía y el `400` no reapareció.
- Las filas de la base de datos con valores inválidos (tipo o prioridad desconocidos) se omiten al cargar la caché, para que una fila mala no estropee las demás.

### 6. Qué se guarda de las llamadas

Se guarda: fecha, número **enmascarado**, decisión, regla aplicada (patrón, tipo y prioridad), motivo y si fue real o simulada. No se guardan nombres, datos de la agenda ni duración.

- El enmascarado se aplica **dentro** del repositorio del registro, no depende de quien lo llame. Deja visible solo un prefijo corto (como mucho 6 caracteres) y sustituye el resto por asteriscos; nunca devuelve el número original (un test lo comprueba con números de 1 a 15 dígitos).
- El usuario puede desactivar el registro y borrarlo con confirmación. Además se borra solo lo que tiene más de 30 días al abrir la app.
- Los logs nunca llevan el número; ante un error solo se escribe el tipo de excepción.

## Otras decisiones

**CSV.** Columnas `patron;tipo;accion;prioridad;activa;etiqueta`, con cabecera y `;` (se abre bien en Excel en español). Al importar:

- Se detecta `;` o `,` y se tolera la cabecera.
- Límites: 1 MB (contando los bytes realmente leídos), 5000 filas y 1000 caracteres por línea.
- Cada fila se valida con la misma validación que la interfaz. Los errores de una fila (tipo desconocido, prioridad fuera de rango, prefijo de 2 dígitos o menos, patrón repetido en el fichero) no detienen el resto y se listan con su número de línea (máximo 50 y «y N más»).
- Un patrón que ya existe se actualiza; si la etiqueta o la prioridad vienen vacías, se conservan las existentes.
- El resumen muestra añadidas, actualizadas, omitidas y erróneas.

**Estadísticas.** «Hoy» cuenta desde las 00:00 de la hora local (función pura con la zona horaria como parámetro, con tests). Las bloqueadas y las permitidas se muestran por separado. El ranking de reglas se ordena por número de llamadas y, en empate, por patrón, para que el resultado sea siempre el mismo. Las simulaciones quedan fuera salvo que se active «Mostrar pruebas».

**Simulador.** Está en «Probar un número» (Inicio). Pasa el número por el mismo `MotorFiltrado` y las mismas reglas en memoria que el servicio, y explica el resultado con una frase. Opcionalmente guarda la prueba en el registro marcada como simulada.

**Diseño.** Rediseño neumórfico (soft UI) hecho a mano con Compose y sin librerías: sombra clara y oscura con `drawBehind`, tarjetas y botones elevados, campos hundidos. Pensado para que sea simple: cuatro pestañas, frases llanas («Bloquea llamadas que empiezan por 400»), una acción principal por pantalla, y los estados siempre en texto (Activa, Bloquea, Permite) y no solo en color o sombra.

## Dificultades encontradas

- **Tests que se pisaban.** Los tests instrumentados pasaban uno a uno pero fallaban juntos. La causa era un singleton oculto en `BaseDatos`: si ya existía una instancia, ignoraba el nombre de base de datos pedido y abría el fichero de la primera. Los tests de migración veían una base de datos recién creada. Lo resolví dejando `BaseDatos` como un helper normal, con el singleton solo en `AlmacenDatos`, inyectando la base de datos a los repositorios y añadiendo un test de regresión (`AislamientoBaseDatosTest`). Pasan dos ejecuciones seguidas.
- **Importador de CSV.** La primera versión informaba de fallo cuando la importación había ido bien, leía las columnas en otro orden que el formato acordado, abortaba todo por un error en una fila y recortaba la lista de errores dos veces. Lo rehice separando planificar y aplicar, con tests de cada caso.
- **Interfaz.** Con una descripción larga, los botones de la tarjeta de reglas se desbordaban y «Eliminar» desaparecía. En el primer rediseño, además, la barra inferior ocupaba toda la pantalla. Se corrigió acotando alturas y moviendo el borrado a la pantalla de edición.
- **Git.** Para compilar el APK de la Fase 2 creé una copia de trabajo (`git worktree`) dentro del propio repositorio y GitHub la mostró como una carpeta enlazada. La quité con `git rm --cached` y compilo esa copia fuera del repositorio.
- **Proceso.** Trabajé con un asistente de código (opencode). Sus informes no siempre coincidían con el código: decía tener cinco pestañas y había tres, y un caso especial en `Enmascarar` hacía pasar un test. Por eso comprobé cada paso yo mismo: compilación desde cero, recuento de tests desde los XML, tests instrumentados dos veces seguidas y lectura del código.

## Pruebas

**Automáticas.**

- Tests JVM (`testDebugUnitTest`): 105 sin fallos antes del rediseño visual, en 15 suites: motor, validación, normalización, enmascarado, fechas, mapeo de reglas, codec CSV, planificador de importación, recorte de errores y textos de la importación. Total tras el rediseño: 
com.example.filtro400.EnmascararTest 5 0 0
com.example.filtro400.ExampleUnitTest 1 0 0
com.example.filtro400.FechasTest 7 0 0
com.example.filtro400.MapeoReglaTest 3 0 0
com.example.filtro400.MotorFiltradoFase3Test 11 0 0
com.example.filtro400.MotorFiltradoTest 7 0 0
com.example.filtro400.ReglaBloqueoTest 7 0 0
com.example.filtro400.ValidacionReglasFase3Test 6 0 0
com.example.filtro400.ValidacionReglasTest 12 0 0
com.example.filtro400.csv.CsvCodecTest 10 0 0
com.example.filtro400.csv.ExportadorImportadorRoundTripTest 2 0 0
com.example.filtro400.csv.PlanificadorImportacionTest 21 0 0
com.example.filtro400.csv.RecorteErroresTest 4 0 0
com.example.filtro400.csv.ResumenPlanTest 4 0 0
com.example.filtro400.csv.TextoImportacionTest 5 0 0
com.example.filtro400.ui.PaletaNeuTest 7 0 0
com.example.filtro400.ui.TextosEtapa2Test 8 0 0
com.example.filtro400.ui.TextosTest 7 0 0
TOTAL tests/fallos/errores: 127 0 0.
- Tests instrumentados (emulador, base de datos temporal): 21 sin fallos, ejecutados dos veces seguidas. Migración (4), registro (10), `aplicarPlan` con éxito y rollback (3), aislamiento de bases de datos (3) y el de ejemplo (1).

**Manuales** (emulador de Android Studio, números ficticios): llamada entrante simulada con la app cerrada, cambio de regla en caliente, lista blanca que gana al bloqueo, ajuste de guardar registro y borrado del registro, estadísticas, exportar e importar CSV (ida y vuelta, y con filas erróneas), simulador con y sin guardado, estado del rol y migración de la Fase 2 a la 3. Los pasos y resultados están en `docs/PRUEBAS_MANUALES.md` y las capturas y salidas de Gradle en `docs/evidencias/`.

## Limitaciones

- Android solo entrega al servicio las llamadas de números que no están en la agenda, y no se debe suponer que llegan todas.
- Los números ocultos se permiten.
- No se puede volver a una versión anterior de la app con la base de datos ya migrada (`SQLiteOpenHelper` no permite bajar de versión).
- Una etiqueta vacía en el CSV conserva la existente, así que el CSV no puede borrar etiquetas.
- Un patrón no puede estar a la vez en bloqueo y en lista blanca.
- No se han implementado las ampliaciones opcionales: franjas horarias, bases de datos antispam, actualización de listas, puntuación de riesgo, detección de números ocultos ni exportación de estadísticas.

## Cómo compilar e instalar

Con el JDK 17: `.\gradlew.bat assembleDebug`. El APK queda en `app/build/outputs/apk/debug/app-debug.apk` (la copia de entrega es `entregas/fase3-app-debug.apk`). Al abrir la app, pulsar «Activar filtro» y conceder el rol.
