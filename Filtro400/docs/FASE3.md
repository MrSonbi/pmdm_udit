# FASE 3 — Filtro 400

> Documento de cierre (paso 10, 09/11/2026). Cada afirmación se ha comprobado leyendo el código
> indicado o con una salida real guardada en `docs/evidencias/`.

## a) Decisiones técnicas

### Organización por capas (`app/src/main/java/com/example/filtro400/`)
| Paquete | Contenido | Depende de |
|---|---|---|
| `modelo/` | `Regla`, `Accion`, `TipoRegla` | solo `ReglaBloqueo` |
| `motor/` | `MotorFiltrado`, `Decision`, `ValidacionReglas` (Kotlin puro, sin Android) | `modelo/`, `ReglaBloqueo` (nunca `data/`) |
| `data/` | `BaseDatos`, `AlmacenDatos`, `RepositorioReglas`, `MapeoRegla` | `modelo/`, `motor.ValidacionReglas`; `aplicarPlan` usa los tipos del plan de `csv/` |
| `datos/` | `Preferencias` (`SharedPreferences`) | — |
| `registro/` | `RepositorioRegistro`, `RegistroLlamada`, `OrigenLlamada` | `data/`, `modelo/`, `util/` |
| `csv/` | `CsvCodec`, `ExportadorCsv`, `PlanificadorImportacion` (puro), `ImportadorCsv`, `RecorteErrores`, `TextoImportacion` | `modelo/`, `motor/`, `data/` (solo `ImportadorCsv`) |
| `ui/` | Pantallas Compose (5 pestañas); `MainActivity` solo monta `PantallaInicio` | las demás |
| `util/` | `Enmascarar`, `Fechas` (puros) | `ReglaBloqueo` |

`ReglaBloqueo.normalizar()` es la única normalización (motor, `Enmascarar`, UI y CSV).

### Tablas y migración v1 → v2 (`data/BaseDatos.kt`, `VERSION_BD = 2`)
- `reglas`: `id, patron (UNIQUE), etiqueta, activa, creada_en, tipo, accion, prioridad`.
- `registro`: `id, fecha, numero_enmascarado, decision, regla_id, regla_patron, regla_tipo,
  regla_prioridad, motivo, origen` con `CHECK (origen IN ('REAL','SIMULADA'))` e índices en
  `fecha` y `regla_id`. Guarda copia del patrón/tipo/prioridad para que el historial siga legible
  si la regla se borra.
- `onUpgrade` (v1 → v2): tres `ALTER TABLE ... ADD COLUMN` con valores por defecto `PREFIJO`,
  `BLOQUEAR`, `0` (las reglas antiguas se comportan igual que en la Fase 2) y `CREATE TABLE
  registro` + índices. **Sin `DROP`** y **sin re-sembrar el 400**: la semilla está solo en
  `onCreate`, que crea directamente el esquema v2 completo en instalaciones nuevas.

### Conflicto lista blanca / bloqueo (`motor/MotorFiltrado.evaluar`)
1. Número normalizado vacío (oculto) → `PERMITIR` sin mirar reglas.
2. Candidatas: reglas activas con patrón no vacío; `NUMERO` coincide si es igual, `PREFIJO` si el
   número empieza por el patrón. Sin candidatas → `PERMITIR`.
3. Orden: `prioridad` descendente → `NUMERO` antes que `PREFIJO` → patrón más largo → en empate
   absoluto, `PERMITIR` antes que `BLOQUEAR`. Gana la primera.
- Prioridad por defecto (`ValidacionReglas.prioridadPorDefecto`): `BLOQUEAR` = 0, `PERMITIR` = 100,
  así una regla de lista blanca creada sin prioridad gana al bloqueo por defecto.

### Rendimiento durante la llamada (`FiltroLlamadasService.onScreenCall`)
- Las reglas salen de `RepositorioReglas.reglasActivas()`: caché `@Volatile` en memoria; solo
  consulta SQLite si aún no está cargada (primera llamada del proceso). Cada alta, edición, cambio
  de activa, borrado o importación la refresca. No hay red.
- Se calcula la decisión y se responde **una sola vez** con `respondToCall`; **después**, en
  `try/catch` separados, se loguea (número enmascarado) y, si el ajuste está activo, se inserta en
  el registro de forma síncrona (`insertarSincrono`).
- Fail-open: cualquier excepción antes de responder deja `bloquear = false`.

### Privacidad
- Se guarda: fecha, número **enmascarado**, decisión, datos de la regla aplicada, motivo y origen.
  `insertarSincrono` aplica `Enmascarar.numero` antes de escribir; el número completo no llega a la
  BD ni a Logcat (`Log.w` del fallo al responder solo incluye el nombre de la clase de excepción).
- `Enmascarar`: vacío → "(oculto)"; si no, prefijo visible + `***` (1 carácter → 0 visibles;
  2-3 → 1; 4-6 → 2; resto → `min(6, longitud − 3)`); siempre queda oculto al menos uno.
- Retención: al abrir la app (`LaunchedEffect` en `PantallaInicio`) se llama a
  `borrarMasAntiguosDe(30)`; también hay borrado manual con confirmación.
- Ajuste "Guardar registro" en `SharedPreferences` (`datos/Preferencias`, activo por defecto): si
  está apagado, el servicio no inserta nada. Las simulaciones solo se guardan si se enciende el
  interruptor del Simulador (apagado por defecto) y se marcan con origen `SIMULADA`.

### Cómo se valida una actualización de filtros
- **Tests JVM** del motor, validación, CSV, fechas y enmascarado (sin Android).
- **Tests instrumentados** con BD temporal: cada test crea `BaseDatos(contexto, "test_xxx.db")` y la
  inyecta en los repositorios; ninguno abre `filtro400.db`. `MigracionV1V2Test` crea una BD v1 y
  comprueba datos, valores por defecto, que no reaparece el 400 y que el esquema migrado es igual al
  de una instalación nueva.
- **Importación CSV**: `PlanificadorImportacion` calcula el plan (puro) y
  `RepositorioReglas.aplicarPlan` lo ejecuta en **una transacción** (`beginTransaction` /
  `setTransactionSuccessful` / `endTransaction`); si falla una operación hay rollback y no cambia
  ninguna regla (`AplicarPlanTest.fallaAMitadYHaceRollbackCompleto`). Importar añade o actualiza,
  nunca borra. Límites: 1 MB, 5000 filas y 1000 caracteres por línea (`CsvCodec`).

## b) Dificultades encontradas
- **Singleton oculto en `BaseDatos`.** Una versión anterior guardaba la primera instancia en un
  campo estático e ignoraba el nombre de las siguientes, así que los tests instrumentados podían
  compartir fichero y pisarse. Solución: `BaseDatos` es un `SQLiteOpenHelper` normal con nombre
  obligatorio; producción la obtiene solo de `AlmacenDatos.obtener` (`filtro400.db`) y los
  repositorios la reciben por constructor. `AislamientoBaseDatosTest` lo comprueba.
- **Contadores e informe de errores del importador CSV incorrectos.** Los errores se recortaban a
  50 en `aplicarPlan` y otra vez en `ImportadorCsv` (y la UI a 10): la línea "y N más" entraba en el
  segundo recorte y N salía mal. Ahora hay un único recorte (`RecorteErrores`) y
  `ResultadoImportacion.erroneas` es el total real calculado antes de recortar. Además, un patrón
  solo cuenta como "visto" si su fila pasa la validación, para que una fila inválida no convierta en
  duplicada a una válida posterior.
- **"Hoy" y el cambio de hora.** Con "ahora − 24 h" o con UTC, "hoy" incluía llamadas de ayer y los
  días de 23 h/25 h fallaban. `util/Fechas` usa `LocalDate.minusDays` y `atStartOfDay(zona)`.

## c) Pruebas
- Comando (09/11/2026): `.\gradlew.bat clean testDebugUnitTest assembleDebug assembleDebugAndroidTest`
  → **BUILD SUCCESSFUL**, código de salida 0. Salida completa en `docs/evidencias/gradle-fase3.txt`.
- **JVM: 15 suites, 105 tests, 0 fallos, 0 errores, 0 omitidos** (recuento por suite leído de los
  XML en `docs/evidencias/tests-jvm-fase3.txt`).
- **Instrumentados** (21 tests: `MigracionV1V2Test` 4, `AislamientoBaseDatosTest` 3,
  `AplicarPlanTest` 3, `RepositorioRegistroTest` 10, `ExampleInstrumentedTest` 1): compilados con
  `assembleDebugAndroidTest`; los ejecuta el alumno con `connectedDebugAndroidTest`.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Pruebas manuales en el emulador: checklist en `docs/PRUEBAS_MANUALES.md`.

## d) Limitaciones conocidas
- Android solo entrega al servicio las llamadas de números que **no** están en la agenda. Tampoco se
  asume que lleguen todas las demás: el sistema decide cuándo invocar el servicio; la app solo
  actúa sobre las que recibe.
- No se puede volver a una versión anterior con la BD v2: `BaseDatos` no sobrescribe
  `onDowngrade` y `SQLiteOpenHelper` no permite bajar de versión.
- Una etiqueta vacía en el CSV conserva la existente (`etiquetaCsvLimpia ?: etiqueta existente`):
  el CSV no puede borrar etiquetas ni reglas.
- Un fichero de exactamente 1 MB también se rechaza (`LimitadorBytesInputStream`).
- El interruptor del Simulador no se recuerda entre ejecuciones (vuelve a apagado).
- No se han implementado ampliaciones opcionales.
