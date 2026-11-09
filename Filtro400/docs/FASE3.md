# FASE 3 — Filtro 400

## Decisiones
- Migración v1 → v2 con `ALTER TABLE` (sin reconstruir tabla). En instalación nueva se crea esquema v2 completo en `onCreate`.
- `onUpgrade` no siembra el 400; las reglas v1 migran a `PREFIJO`, `BLOQUEAR`, `0`.
- Paquete `modelo/` con `Regla`, `Accion`, `TipoRegla`. `motor/` y `data/` dependen de `modelo/`, nunca entre sí.
- Motor con desempate: prioridad (mayor primero) → `NUMERO` exacto antes que `PREFIJO` → patrón más largo → `PERMITIR` en empate absoluto.
- Registro cerrado en v2 con columnas exactas e índices en `fecha` y `regla_id`. Solo se guarda el número enmascarado. Origen `REAL`/`SIMULADA`.
- Ajuste "guardar registro" activo por defecto en `SharedPreferences`. Borrado automático >30 días al abrir la app en segundo plano; borrado manual con confirmación.
- CSV: añadir/actualizar sin borrar, una sola transacción, validación con `ValidacionReglas`, refresco de caché una vez, resumen.
- Prioridad por defecto: `BLOQUEAR` = 0, `PERMITIR` = 100 (aplicado en UI y CSV cuando falte).

## Pasos ejecutados
- Paso 1: migración v1→v2 + test instrumentado. Tests instrumentados: 5, 0 fallos/errores.
- Pasos 2-4: modelo, motor v3, validación/CRUD, interfaz con tipo/acción/prioridad, robustez del mapeo, limpieza de sobrecargas antiguas.
- Pasos 5-6: navegación con pestañas solo texto, registro/ajustes, servicio responde primero y registra después en try/catch.

## Dificultades
- **Los tests instrumentados se pisaban por un singleton oculto en `BaseDatos`.**
  - Síntoma: los tests que comparten proceso no estaban aislados. Una BD "de test" podía acabar
    siendo el fichero de otro test, y `esquemaMigradoEsIgualAlDeUnaInstalacionNueva` podía comparar
    un fichero consigo mismo y salir verde sin probar nada.
  - Causa: el constructor de `BaseDatos` guardaba la primera instancia en un campo estático
    `instancia` y, si ya existía, IGNORABA el `nombreBd` recibido y abría el fichero de esa primera
    instancia. Además, `AplicarPlanTest` usaba reflexión para colar su BD dentro de `RepositorioReglas`.
  - Solución:
    1. `BaseDatos` vuelve a ser un `SQLiteOpenHelper` normal, sin estado estático:
       `SQLiteOpenHelper(contexto.applicationContext, nombreBd, null, VERSION_BD)`. El nombre es
       obligatorio para que nadie abra `filtro400.db` sin querer.
    2. El singleton vive en UN solo sitio: `data/AlmacenDatos.obtener(contexto)` crea
       `BaseDatos(applicationContext, NOMBRE_BD)` una única vez. `RepositorioReglas.obtener` y
       `RepositorioRegistro.obtener` (y a través de ellos el servicio, la UI y el CSV) piden la BD ahí.
       Comprobado con `Select-String -Pattern 'BaseDatos\('` sobre `app\src\main`: la única
       construcción real está en `AlmacenDatos.kt` (el resto de coincidencias son comentarios y la
       propia declaración de la clase).
    3. `RepositorioReglas(baseDatos)` y `RepositorioRegistro(baseDatos)` reciben la BD por constructor.
       Los tests crean `BaseDatos(contexto, "test_xxx.db")` y la inyectan. Ya no hay reflexión.
    4. Los tests instrumentados prueban el código de producción (`insertarSincrono` con fecha opcional,
       `borrarMasAntiguosDe`, `contar`, `estadisticas`, `rankingReglas`, `listarRecientes`,
       `aplicarPlan`) en vez de SQL escrito a mano.
    5. Test de regresión `AislamientoBaseDatosTest`: dos `BaseDatos` con nombres distintos abren
       rutas distintas y lo que se escribe en una no aparece en la otra. `MigracionV1V2Test` comprueba
       que la BD migrada y la nueva son ficheros distintos (ruta y datos) ANTES de comparar esquemas.
- **Las estadísticas no existían.** El paso 7 se daba por escrito, pero no había ninguna consulta de
  estadísticas ni de listado en el código. Se han añadido como métodos de `RepositorioRegistro`
  (`estadisticas`, `rankingReglas`, `listarRecientes`) con `incluirSimulaciones = false` por defecto.
  El ranking es determinista: `ORDER BY c DESC, regla_patron ASC`.
- **La UI no estaba conectada (auditoría pasos 5-7).** La barra inferior tenía 3 pestañas; "Registro"
  solo tenía ajustes (sin lista) y no existían Estadísticas ni CSV en la UI. Solución: 5 pestañas
  (Inicio, Reglas, Registro, Estadísticas, Ajustes). `ui/PestanaRegistro.kt` usa `listarRecientes`
  (100 filas reales, filtro Todas/Bloqueadas/Permitidas, borrar con confirmación);
  `ui/PestanaEstadisticas.kt` usa `estadisticas` y `rankingReglas` con el interruptor
  "incluir simulaciones"; `ui/PestanaAjustes.kt` tiene "guardar registro", borrar registro y
  exportar/importar CSV con SAF (`CreateDocument` / `OpenDocument`). Un contador `refrescoRegistro`
  se incrementa en `ON_RESUME` y tras borrar, y las pestañas releen la BD. Estados vacíos explícitos.
- **`Enmascarar` tenía un caso especial para "1234".** Se sustituye por una regla general
  (1 visible si longitud ≤ 3, 2 si ≤ 6, si no `min(6, longitud - 3)`; vacío -> "(oculto)") y un
  test de propiedades de 1 a 15 dígitos.

## Tests nuevos
- `MotorFiltradoFase3Test`: ganaPrioridadSobreLongitud, numeroExactoGanaAPrefijo, ganaPatronMasLargoConMismaPrioridadYSinNumeroExacto, permiteGanaEnEmpateAbsoluto, listaBlancaConMayorPrioridadPermiteAunqueHayaBloqueo, reglaNumeroExactaNoCoincideSiEsDiferente, reglaInactivaConCamposNuevosNoAplica, listaBlancaInactivaNoPermite, prefijoDePrioridadAltaGanaANumeroDePrioridadBaja, numeroFicticioConRegla400PorDefectoSeBloquea, numeroFicticioConPermitirExactoPasa
- `ValidacionReglasFase3Test`: bloquearPorDefectoTienePrioridadCero, permitirPorDefectoTienePrioridadCien, permiteFijarPrioridadManual, rechazaPrioridadFueraDeRango, validaReglaNumeroExacto, prioridadPorDefectoHelperCoincide
- `MapeoReglaTest`: mapeaFilaValida, omiteFilaConPrioridadFueraDeRango, omiteFilaConTipoOAccionDesconocidos

- `EnmascararTest`: enmascaradoExactoSegunEspecificacion, numeroVacioDevuelveOculto, tramosDeLongitud, propiedadesParaNumerosDe1A15Digitos

## Prueba de compilación (auditoría pasos 5-7)
- `.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest` -> `BUILD SUCCESSFUL`.
- Tests JVM (XML): 12 suites, 80 tests, 0 fallos, 0 errores, 0 omitidos.
- Tests instrumentados: compilan; los ejecuta el usuario (sin emulador en esta sesión).

## Limitaciones
- Solo llegan al filtro las llamadas de números que NO están en la agenda (limitación de Android).
- `CallResponse` sin cambios. `fail-open`. Nunca se loguea el número completo.
- No se puede volver a una versión anterior de la app con la BD v2 (SQLiteOpenHelper no permite bajar de versión).
- Sin dependencias nuevas, sin subir AGP/Kotlin/Compose/Gradle, `minSdk = 29`.
- Sin Room/KSP/kapt.
