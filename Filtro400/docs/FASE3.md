# FASE 3 — Filtro 400

> Revisado el 09/11/2026 contra el código: cada afirmación de este documento se ha
> comprobado leyendo las fuentes indicadas. Se han quitado los resultados de ejecuciones
> antiguas que no se pueden comprobar leyendo el código.

## Decisiones
- **Migración v1 → v2** con `ALTER TABLE` (sin reconstruir la tabla): `BaseDatos.onUpgrade`
  añade `tipo`, `accion` y `prioridad` con valores por defecto `PREFIJO`, `BLOQUEAR`, `0`, y crea
  la tabla `registro` con sus dos índices. En instalación nueva, `onCreate` crea el esquema v2
  completo y siembra la regla 400.
- `onUpgrade` **no** siembra el 400: si el usuario lo borró en v1, sigue borrado
  (`MigracionV1V2Test.migracionNoVuelveASembrarEl400SiSeBorro`).
- **Paquetes**: `modelo/` (`Regla`, `Accion`, `TipoRegla`) no depende de nadie. `motor/` solo
  depende de `modelo/` (y de `ReglaBloqueo`), nunca de `data/`. `data/` sí usa
  `motor.ValidacionReglas` (prioridad por defecto y mapeo de filas en `MapeoRegla`).
- **Motor** (`MotorFiltrado.evaluar`, Kotlin puro), orden de desempate: prioridad (mayor primero)
  → `NUMERO` exacto antes que `PREFIJO` → patrón más largo → `PERMITIR` en empate absoluto.
  Número vacío u oculto → se permite.
- **Registro**: tabla `registro` con columnas `id, fecha, numero_enmascarado, decision,
  regla_id, regla_patron, regla_tipo, regla_prioridad, motivo, origen` e índices en `fecha` y
  `regla_id`. `origen` tiene `CHECK IN ('REAL','SIMULADA')`. `insertarSincrono` enmascara el
  número con `Enmascarar.numero` antes de guardarlo.
- **Servicio**: calcula la decisión, responde una sola vez (`respondToCall`) y DESPUÉS, en
  `try/catch` separados, loguea (número enmascarado) e inserta en el registro de forma síncrona
  si el ajuste está activo. Cualquier excepción antes de responder → se permite (fail-open).
- **Ajuste "guardar registro"** en `SharedPreferences` (`datos/Preferencias`), activo por defecto.
- **Borrado del registro**: automático de filas con más de 30 días al abrir la app
  (`LaunchedEffect` en `PantallaInicio`, en `Dispatchers.IO`); manual con diálogo de confirmación.
- **CSV**: importar añade o actualiza, nunca borra. El plan lo calcula `PlanificadorImportacion`
  (puro) y `RepositorioReglas.aplicarPlan` lo ejecuta en UNA transacción; si algo falla se hace
  rollback y no cambia nada. La caché de reglas activas se refresca una vez al terminar.
  Límites: 1 MB y 5000 filas (`CsvCodec`), además de 1000 caracteres por línea.
- **Prioridad por defecto**: `BLOQUEAR` = 0, `PERMITIR` = 100 (`ValidacionReglas.prioridadPorDefecto`),
  usada en el diálogo de reglas, en `RepositorioReglas.insertar/actualizar` y en el CSV.
- **CSV, actualización de una regla existente con prioridad vacía**: si la acción de la fila es
  la misma que la de la regla, se conserva la prioridad existente; si la acción cambia, se usa la
  prioridad por defecto de la NUEVA acción. La comparación "sin cambios" usa el mismo criterio.
- **CSV, etiqueta**: al actualizar, `etiqueta = etiquetaCsvLimpia ?: etiqueta existente`.
- **CSV, duplicados en el fichero**: un patrón solo se marca como "visto" cuando la fila ha
  pasado la validación; una fila inválida no hace duplicada a una posterior válida.
- **CSV, errores**: `aplicarPlan` devuelve todos los errores; `ImportadorCsv.importarDesdeStream`
  los junta con los de parseo, los ordena por línea y los recorta UNA vez con
  `RecorteErrores.recortar` (50 + una línea "y N más", con N = total − 50). La UI los muestra tal
  cual, sin volver a recortar.
- **Estadísticas**: bloqueadas y permitidas siempre por separado, para hoy, últimos 7 días y
  todo el registro. "Hoy" = desde las 00:00 de la zona local; "últimos 7 días" = hoy y los 6 días
  de calendario anteriores (desde las 00:00 locales de hace 6 días). Los límites se calculan en
  `util/Fechas` (puro, zona inyectable) y `RepositorioRegistro.estadisticas(ahora, zona)` los
  usa en una sola consulta SQL. Por defecto excluyen las filas `SIMULADA`.
- **Simulador (paso 9)**: sustituye al probador. Usa `RepositorioReglas.reglasActivas()` (la
  misma caché `@Volatile` que el servicio; solo hace SQL si aún no está cargada) y el mismo
  `MotorFiltrado.evaluar`. Muestra decisión, regla aplicada (patrón y etiqueta), tipo, prioridad,
  motivo y cómo aparecería el número en el registro (enmascarado); nunca el número normalizado.
- **Simulaciones en el registro**: interruptor "Guardar en el registro" en el Simulador, apagado
  por defecto (`rememberSaveable`, no se guarda en `SharedPreferences`). Si está activo se llama a
  `RepositorioRegistro.insertar(..., origen = SIMULADA)`, que enmascara dentro. Es independiente del
  ajuste "Guardar registro" (que solo afecta a las llamadas reales).
- **"Incluir simulaciones"**: un único estado en `PantallaInicio`, compartido por Registro y
  Estadísticas (apagado por defecto), para que las dos pestañas cuenten lo mismo.
- **Mensajes de importación**: `csv/TextoImportacion` (puro, con tests JVM) compone el resumen
  "N añadidas, N actualizadas, N omitidas (sin cambios), N erróneas" y cada error "Línea N: …".
  `ResultadoImportacion.erroneas` es el total real de filas con error, calculado antes del recorte.
- **Enmascarado** (`util/Enmascarar`): normaliza con `ReglaBloqueo.normalizar`; vacío →
  "(oculto)"; si no, prefijo visible + "***": 1 dígito → 0 visibles ("***"); 2-3 → 1; 4-6 → 2;
  resto → `min(6, longitud − 3)`. Siempre queda oculto al menos un carácter.

## Interfaz
- Barra inferior con 5 pestañas solo texto (`icon = {}`): Inicio, Reglas, Registro,
  Estadísticas, Ajustes.
- `PestanaRegistro`: últimas 100 filas (`listarRecientes`), reales o también simuladas según el
  interruptor "incluir simulaciones"; las simuladas llevan fondo `tertiaryContainer` y la
  etiqueta "SIMULADA (no es una llamada real)". Filtro Todas/Bloqueadas/Permitidas, borrar con
  confirmación.
- `PestanaEstadisticas`: tarjetas "Hoy (desde las 00:00)", "Últimos 7 días (hoy y los 6
  anteriores)" y "Todo el registro", cada una con "Bloqueadas…" y "Permitidas…"; ranking de las
  10 reglas que más han decidido (`ORDER BY c DESC, regla_patron ASC`); interruptor
  "incluir simulaciones".
- `PestanaAjustes`: "guardar registro", borrar registro y exportar/importar CSV con SAF
  (`CreateDocument` / `OpenDocument`).
- Un contador `refrescoRegistro` se incrementa en `ON_RESUME` y tras borrar el registro; Registro
  y Estadísticas vuelven a leer la BD al cambiar.

## Dificultades
- **Los tests instrumentados podían compartir fichero de BD.**
  - Causa: una versión anterior de `BaseDatos` guardaba la primera instancia en un campo estático
    e ignoraba el `nombreBd` de las siguientes.
  - Solución (estado actual del código):
    1. `BaseDatos` es un `SQLiteOpenHelper` normal, sin estado estático y con el nombre
       obligatorio: `SQLiteOpenHelper(contexto.applicationContext, nombreBd, null, VERSION_BD)`.
    2. El único sitio de producción que construye la BD es `data/AlmacenDatos.obtener`, con
       `BaseDatos.NOMBRE_BD`. Comprobado con `Select-String -Pattern 'BaseDatos\('` sobre
       `app\src\main`: la única construcción real está en `AlmacenDatos.kt` (el resto son
       comentarios y la declaración de la clase).
    3. `RepositorioReglas(baseDatos)` y `RepositorioRegistro(baseDatos)` reciben la BD por
       constructor; los tests crean `BaseDatos(contexto, "test_xxx.db")` y la inyectan.
    4. `AislamientoBaseDatosTest` comprueba que dos `BaseDatos` con nombres distintos abren rutas
       distintas y no comparten datos. `MigracionV1V2Test` comprueba que la BD migrada y la nueva
       son ficheros distintos (ruta y datos) antes de comparar esquemas.
- **Recorte de errores duplicado.** Se recortaba a 50 en `aplicarPlan` y otra vez en
  `ImportadorCsv` (y la UI recortaba a 10): la línea "y N más" entraba en el segundo recorte, se
  ordenaba la primera (línea 0) y N salía mal. Ahora hay un único recorte puro (`RecorteErrores`).
- **"Hoy" y el cambio de hora.** Con "ahora − 24 h" o con UTC, "hoy" incluía llamadas de ayer.
  Restar días en bloques de 24 h falla en los días de 23 h o 25 h; por eso `Fechas` resta días de
  calendario con `LocalDate.minusDays` y usa `atStartOfDay(zona)`.

## Tests
- JVM (`app/src/test`): 15 suites, 105 tests (paso 9: `TextoImportacionTest`, 5) (ver `docs/INFORME_SESION.md` para el recuento por
  suite de la última ejecución).
  - Nuevos o ampliados en esta sesión: `FechasTest` (7, incluye los cambios de hora de
    Europe/Madrid del 29/03/2026 y 25/10/2026), `RecorteErroresTest` (0, 50, 51 y 120 errores),
    `PlanificadorImportacionTest` (prioridad vacía con/sin cambio de acción, "sin cambios",
    fila inválida que no hace duplicada a la válida, etiqueta), `EnmascararTest` (1 dígito → "***").
- Instrumentados (`app/src/androidTest`), todos con BD `test_*.db`, nunca `filtro400.db`:
  `MigracionV1V2Test` (4), `AislamientoBaseDatosTest` (3), `AplicarPlanTest` (3),
  `RepositorioRegistroTest` (10, con instante y zona fijos: 09/11/2026 12:00 Europe/Madrid, salvo
  `filaSimuladaSoloCuentaConElInterruptorEncendido`, que usa `insertar` y el reloj real),
  `ExampleInstrumentedTest` (1). En esta sesión solo se han compilado
  (`assembleDebugAndroidTest`); los ejecuta el usuario.

## Limitaciones
- Solo llegan al filtro las llamadas de números que NO están en la agenda (limitación de Android).
- Fail-open: ante cualquier error se permite la llamada. Nunca se loguea el número completo.
- No se puede volver a una versión anterior de la app con la BD v2: `BaseDatos` no sobrescribe
  `onDowngrade` y `SQLiteOpenHelper` no permite bajar de versión.
- **Una etiqueta vacía en el CSV conserva la existente, así que el CSV no puede borrar etiquetas.**
  Para quitar una etiqueta hay que editar la regla desde la app.
- El CSV no puede borrar reglas (solo añade o actualiza).
- El interruptor "Guardar en el registro" del Simulador no se recuerda entre ejecuciones de la app
  (vuelve a apagado), a propósito.
- Una simulación guardada usa la hora real del móvil: cuenta en "hoy" con "incluir simulaciones".
- `LimitadorBytesInputStream` lanza el error de tamaño en cuanto se han leído 1 048 576 bytes y se
  pide otra lectura, así que un fichero de exactamente 1 MB también se rechaza.
- Sin dependencias nuevas, sin subir AGP/Kotlin/Compose/Gradle, `minSdk = 29`. Sin Room/KSP/kapt.
