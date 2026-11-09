# Informe de sesión — Fase 3, cierre del paso 8 (09/11/2026)

No se ha usado el emulador ni `connectedDebugAndroidTest`. No se ha ejecutado git. Sin dependencias nuevas.
No se ha empezado el paso 9.

## Resultado
- `.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest` → **BUILD SUCCESSFUL**, código de salida 0.
- Tests JVM: **14 suites, 100 tests, 0 fallos, 0 errores, 0 omitidos** (antes: 12 suites, 80 tests).
- APK: `app/build/outputs/apk/debug/app-debug.apk` (31 090 776 bytes, 09/11/2026 14:37:08).
- APK de tests instrumentados: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
  (2 414 147 bytes, 09/11/2026 14:37:08). **Compilado, no ejecutado** (lo ejecutas tú).

## Qué se ha hecho

### 1. PlanificadorImportacion
- **a)** Actualización con prioridad vacía: misma acción → conserva la prioridad existente; acción
  distinta → `prioridadPorDefecto(nueva acción)`. La comparación "sin cambios" usa el mismo valor.
  Tests: `actualizacionCambiaAccionYPrioridadVaciaUsaPorDefectoDeLaNuevaAccion` (BLOQUEAR 7 → PERMITIR: 100),
  `actualizacionPermitirABloquearPrioridadVaciaDaCero`, `actualizacionMismaAccionPrioridadVaciaConservaLaExistenteAunqueCambieOtroCampo`
  (PERMITIR 42 se queda en 42), `sinCambiosMismaAccionPrioridadVaciaSeOmiteAunqueNoSeaLaDeDefecto`,
  `cambioDeAccionConPrioridadVaciaNoSeConsideraSinCambios`.
- **b)** `vistoEnFichero.add` se hace tras TODAS las validaciones. Tests:
  `filaInvalidaNoConvierteEnDuplicadaAUnaPosteriorValida` (prioridad 150 → error; "+34 611" válida
  → Insertar; "611" otra vez → duplicado) y `filaConCaracteresInvalidosNoConvierteEnDuplicadaAUnaPosteriorValida`
  ("611x" → error; "611" → Insertar).
- **c)** Recorte único: nuevo `csv/RecorteErrores.recortar` (puro, máximo 50, añade al final
  `ErrorLinea(0, "y N más")` con N = total − 50). `aplicarPlan` devuelve todos los errores;
  `ImportadorCsv.importarDesdeStream` junta parseo + plan, ordena y recorta una vez.
  **Además** había un tercer recorte en la UI (`PestanaAjustes`, `ERRORES_VISIBLES = 10`) que contaba
  la línea "y N más" como un error más; se ha quitado y la UI muestra la lista tal cual llega.
  Tests `RecorteErroresTest`: 0, 50, 51 ("y 1 más") y 120 ("y 70 más").
- **d)** Etiqueta de actualización = `etiquetaCsvLimpia ?: reglaExistente.etiqueta`. Quitado el
  import sin usar (`modelo.Accion`) y el `if (validacion.esValida)` redundante (y la prioridad de
  alta duplicada: ahora usa `validacion.prioridad`). Eliminados `ImportadorCsv.importar()` y
  `procesarLineas()`: antes de borrarlos, `Select-String -Pattern '\.importar\(|procesarLineas'`
  solo encontraba sus propias definiciones y la llamada de una a la otra. Tras la sesión no hay
  ninguna coincidencia. Test añadido: `actualizacionEtiquetaAusenteConservaYEtiquetaNuevaSustituye`.

### 2. Estadísticas
- `util/Fechas` (puro, zona inyectable): `inicioDelDia(instante, zona)` e
  `inicioDelDiaHaceDias(instante, dias, zona)` con `LocalDate.minusDays` + `atStartOfDay(zona)`.
- `RepositorioRegistro.Estadisticas` ahora tiene `bloqueadasHoy`, `permitidasHoy`,
  `bloqueadas7Dias`, `permitidas7Dias` (además de `total`, `bloqueadas`, `permitidas`); se calculan
  en una sola consulta. `estadisticas(incluirSimulaciones, ahora, zona = ZoneId.systemDefault())`.
  Se ha quitado el `inicioDelDia` privado con `Calendar`.
- **Decisión que debes validar:** "últimos 7 días" pasa de `ahora − 7 × 24 h` a "hoy y los 6 días de
  calendario anteriores, desde las 00:00 locales". Lo he cambiado para que sea coherente con "hoy" y
  no dependa de la hora del día; si prefieres la ventana móvil de 168 h, es cambiar un argumento.
- UI: tarjetas "Hoy (desde las 00:00)" → "Bloqueadas hoy" / "Permitidas hoy";
  "Últimos 7 días (hoy y los 6 anteriores)" → "Bloqueadas en los últimos 7 días" / "Permitidas en
  los últimos 7 días"; "Todo el registro" → "Bloqueadas en total" / "Permitidas en total".
- `FechasTest` (JVM, 7 tests): día normal; 00:00 exacta y 1 ms antes; Madrid ≠ UTC y ≠ "−24 h";
  29/03/2026 (día de 23 h); 25/10/2026 (día de 25 h); 7 días que cruzan el cambio de hora;
  0 días = inicio del día.
- `RepositorioRegistroTest` (instrumentado): `ahora` fijo = 09/11/2026 12:00 Europe/Madrid y
  `zona` explícita; expectativas actualizadas a los campos separados; test nuevo
  `hoyEmpiezaALas00LocalesYSieteDiasSonDiasDeCalendario` (fila a las 00:00 cuenta como hoy, la de
  23:59:59.999 de ayer no; la de las 00:00 de hace 6 días cuenta en 7 días, 1 ms antes no).
  **Solo compilado, sin ejecutar.**

### 3. Enmascarar
- 1 dígito → 0 visibles ("***"); 2-3 → 1; 4-6 → 2; resto → `min(6, longitud − 3)`.
- `EnmascararTest`: nuevo `unDigitoNoSeMuestra`; `tramosDeLongitud` ahora prueba 2, 3 y 4 dígitos;
  la prueba de propiedades (1..15) comprueba además que siempre queda oculto al menos un carácter.

### 4. docs/FASE3.md
- Añadida la limitación: "Una etiqueta vacía en el CSV conserva la existente, así que el CSV no
  puede borrar etiquetas".
- Revisado entero contra el código. Corregido o eliminado:
  - "`motor/` y `data/` … nunca entre sí": **falso**, `data/RepositorioReglas` y `data/MapeoRegla`
    importan `motor.ValidacionReglas`. Corregido.
  - "Paso 1 … tests instrumentados: 5, 0 fallos" y "12 suites, 80 tests": resultados de ejecuciones
    antiguas, no comprobables leyendo código. Eliminados; sustituidos por el recuento de hoy.
    (Además `MigracionV1V2Test` tiene 4 tests, no 5.)
  - Párrafos históricos ("el paso 7 se daba por escrito", "la UI no estaba conectada", "caso
    especial para 1234"): no comprobables en el código actual. Sustituidos por la descripción del
    estado actual.
  - "`CallResponse` sin cambios": ambiguo; sustituido por lo que hace el servicio.
  - Añadido lo comprobado: no hay ninguna inserción `SIMULADA` en `app/src/main` (el interruptor
    "incluir simulaciones" no cambia nada en la app real), y que `LimitadorBytesInputStream` rechaza
    también un fichero de exactamente 1 MB (lanza al pedir una lectura con 1 048 576 bytes ya
    leídos). **No he cambiado ese comportamiento**: está fuera de lo pedido.

## Recuento por suite JVM (XML de `app/build/test-results/testDebugUnitTest`)

| Suite | Tests | Fallos | Errores | Omitidos |
|---|---:|---:|---:|---:|
| com.example.filtro400.csv.CsvCodecTest | 10 | 0 | 0 | 0 |
| com.example.filtro400.csv.ExportadorImportadorRoundTripTest | 2 | 0 | 0 | 0 |
| com.example.filtro400.csv.PlanificadorImportacionTest | 21 | 0 | 0 | 0 |
| com.example.filtro400.csv.RecorteErroresTest | 4 | 0 | 0 | 0 |
| com.example.filtro400.csv.ResumenPlanTest | 4 | 0 | 0 | 0 |
| com.example.filtro400.EnmascararTest | 5 | 0 | 0 | 0 |
| com.example.filtro400.ExampleUnitTest | 1 | 0 | 0 | 0 |
| com.example.filtro400.FechasTest | 7 | 0 | 0 | 0 |
| com.example.filtro400.MapeoReglaTest | 3 | 0 | 0 | 0 |
| com.example.filtro400.MotorFiltradoFase3Test | 11 | 0 | 0 | 0 |
| com.example.filtro400.MotorFiltradoTest | 7 | 0 | 0 | 0 |
| com.example.filtro400.ReglaBloqueoTest | 7 | 0 | 0 | 0 |
| com.example.filtro400.ValidacionReglasFase3Test | 6 | 0 | 0 | 0 |
| com.example.filtro400.ValidacionReglasTest | 12 | 0 | 0 | 0 |
| **Total (14 suites)** | **100** | **0** | **0** | **0** |

Tests instrumentados (número de `@Test` en el código, compilados y sin ejecutar):
`AislamientoBaseDatosTest` 3, `AplicarPlanTest` 3, `ExampleInstrumentedTest` 1,
`MigracionV1V2Test` 4, `RepositorioRegistroTest` 9.

## Archivos tocados
Nuevos:
- `app/src/main/java/com/example/filtro400/csv/RecorteErrores.kt`
- `app/src/main/java/com/example/filtro400/util/Fechas.kt`
- `app/src/test/java/com/example/filtro400/csv/RecorteErroresTest.kt`
- `app/src/test/java/com/example/filtro400/FechasTest.kt`
- `docs/INFORME_SESION.md`

Modificados:
- `app/src/main/java/com/example/filtro400/csv/PlanificadorImportacion.kt`
- `app/src/main/java/com/example/filtro400/csv/ImportadorCsv.kt`
- `app/src/main/java/com/example/filtro400/data/RepositorioReglas.kt` (solo el final de `aplicarPlan`)
- `app/src/main/java/com/example/filtro400/registro/RepositorioRegistro.kt`
- `app/src/main/java/com/example/filtro400/ui/PestanaEstadisticas.kt`
- `app/src/main/java/com/example/filtro400/ui/PestanaAjustes.kt` (quitado el recorte a 10)
- `app/src/main/java/com/example/filtro400/util/Enmascarar.kt`
- `app/src/test/java/com/example/filtro400/csv/PlanificadorImportacionTest.kt`
- `app/src/test/java/com/example/filtro400/EnmascararTest.kt`
- `app/src/androidTest/java/com/example/filtro400/RepositorioRegistroTest.kt`
- `docs/FASE3.md`

## Salida real de Gradle (sin recortar)
Comando (PowerShell, con `JAVA_HOME` de AGENTS.md):
`.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest` → código de salida 0.

```
Reusing configuration cache.
> Task :app:preBuild UP-TO-DATE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:generateDebugAndroidTestAssets UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:mapDebugSourceSetPaths UP-TO-DATE
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
> Task :app:desugarDebugFileDependencies UP-TO-DATE
> Task :app:generateDebugGlobalSynthetics UP-TO-DATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:preDebugAndroidTestBuild SKIPPED
> Task :app:mergeDebugJniLibFolders UP-TO-DATE
> Task :app:validateSigningDebug UP-TO-DATE
> Task :app:generateDebugResources UP-TO-DATE
> Task :app:extractDeepLinksDebug UP-TO-DATE
> Task :app:checkDebugDuplicateClasses UP-TO-DATE
> Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
> Task :app:javaPreCompileDebugUnitTest UP-TO-DATE
> Task :app:writeDebugSigningConfigVersions UP-TO-DATE
> Task :app:writeDebugAppMetadata UP-TO-DATE
> Task :app:generateDebugAndroidTestResources UP-TO-DATE
> Task :app:javaPreCompileDebugAndroidTest UP-TO-DATE
> Task :app:packageDebugResources UP-TO-DATE
> Task :app:generateDebugAndroidTestGlobalSynthetics UP-TO-DATE
> Task :app:mergeLibDexDebug UP-TO-DATE
> Task :app:processDebugNavigationResources UP-TO-DATE
> Task :app:mergeDebugAndroidTestAssets UP-TO-DATE
> Task :app:mapDebugAndroidTestSourceSetPaths UP-TO-DATE
> Task :app:desugarDebugAndroidTestFileDependencies UP-TO-DATE
> Task :app:mergeDebugAssets UP-TO-DATE
> Task :app:packageDebugAndroidTestResources UP-TO-DATE
> Task :app:processDebugAndroidTestNavigationResources UP-TO-DATE
> Task :app:compileDebugNavigationResources UP-TO-DATE
> Task :app:validateSigningDebugAndroidTest UP-TO-DATE
> Task :app:compressDebugAndroidTestAssets UP-TO-DATE
> Task :app:parseDebugLocalResources UP-TO-DATE
> Task :app:compileDebugAndroidTestNavigationResources UP-TO-DATE
> Task :app:writeDebugAndroidTestSigningConfigVersions UP-TO-DATE
> Task :app:checkDebugAarMetadata UP-TO-DATE
> Task :app:mergeDebugAndroidTestJniLibFolders UP-TO-DATE
> Task :app:compressDebugAssets UP-TO-DATE
> Task :app:parseDebugAndroidTestLocalResources UP-TO-DATE
> Task :app:checkDebugAndroidTestAarMetadata UP-TO-DATE
> Task :app:mergeDebugResources UP-TO-DATE
> Task :app:checkDebugAndroidTestDuplicateClasses UP-TO-DATE
> Task :app:mergeDebugAndroidTestResources UP-TO-DATE
> Task :app:mergeDebugNativeLibs UP-TO-DATE
> Task :app:generateDebugRFile UP-TO-DATE
> Task :app:generateDebugAndroidTestRFile UP-TO-DATE
> Task :app:mergeLibDexDebugAndroidTest UP-TO-DATE
> Task :app:stripDebugDebugSymbols UP-TO-DATE
> Task :app:mergeExtDexDebug UP-TO-DATE
> Task :app:mergeDebugAndroidTestNativeLibs NO-SOURCE
> Task :app:stripDebugAndroidTestDebugSymbols NO-SOURCE
> Task :app:mergeExtDexDebugAndroidTest UP-TO-DATE
> Task :app:mergeDebugAndroidTestManifest UP-TO-DATE
> Task :app:processDebugAndroidTestManifest UP-TO-DATE
> Task :app:processDebugAndroidTestResources UP-TO-DATE
> Task :app:processDebugMainManifest
> Task :app:processDebugManifest
> Task :app:processDebugManifestForPackage
> Task :app:processDebugResources
> Task :app:compileDebugKotlin
> Task :app:compileDebugJavaWithJavac NO-SOURCE
> Task :app:processDebugJavaRes UP-TO-DATE
> Task :app:mergeDebugJavaResource UP-TO-DATE
> Task :app:bundleDebugClassesToCompileJar
> Task :app:bundleDebugClassesToRuntimeJar
> Task :app:dexBuilderDebug
> Task :app:mergeProjectDexDebug
> Task :app:packageDebug
> Task :app:assembleDebug
> Task :app:createDebugApkListingFileRedirect
> Task :app:compileDebugUnitTestKotlin
> Task :app:processDebugUnitTestJavaRes UP-TO-DATE
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:compileDebugAndroidTestKotlin
> Task :app:compileDebugAndroidTestJavaWithJavac NO-SOURCE
> Task :app:processDebugAndroidTestJavaRes UP-TO-DATE
> Task :app:mergeDebugAndroidTestJavaResource UP-TO-DATE
> Task :app:dexBuilderDebugAndroidTest
> Task :app:mergeProjectDexDebugAndroidTest
> Task :app:packageDebugAndroidTest
> Task :app:assembleDebugAndroidTest
> Task :app:createDebugAndroidTestApkListingFileRedirect UP-TO-DATE
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 3s
72 actionable tasks: 17 executed, 55 up-to-date
Configuration cache entry reused.
```

## Pendiente para ti
- Ejecutar los tests instrumentados (`connectedDebugAndroidTest`), en especial `RepositorioRegistroTest`.
- Confirmar el criterio de "últimos 7 días" (días de calendario frente a 168 h móviles).
- Actualizar en AGENTS.md el estado de los pasos 7-8 si das el cierre por bueno (no lo he tocado).
