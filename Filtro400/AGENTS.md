# AGENTS.md — Filtro 400

## Contexto
App Android "Filtro 400" (`com.example.filtro400`) que bloquea llamadas entrantes
mediante `CallScreeningService` + `ROLE_CALL_SCREENING`. No sustituye la app de teléfono.
Las llamadas comerciales españolas pueden empezar por 400; el usuario añade sus propios
prefijos/números. Se avanza por fases; cada fase conserva la anterior.

## Estado y norma de fases
- Fase 1: terminada (regla 400 fija, servicio, probador).
- Fase 2: terminada (SQLite, CRUD, motor puro, validación, tests).
- Fase 3: PENDIENTE. **No empezarla hasta que el usuario escriba "continúa con la Fase 3".**
- No implementar ampliaciones opcionales de la Fase 3 sin pedirlo.

## Reglas innegociables
- Mantener nombres/paquete de `MainActivity` y `FiltroLlamadasService`.
- SQLite nativo con `SQLiteOpenHelper`; ajustes en `SharedPreferences`. Sin Room/KSP/kapt.
- `ReglaBloqueo.normalizar()` es la única normalización; se usa en UI, motor, servicio y CSV.
- `MotorFiltrado.evaluar()` es puro (sin Android). El servicio solo lee la caché `@Volatile`.
- `onScreenCall`: fail-open; responder una sola vez antes de loguear/registrar.
- Nunca loguear el número completo; usar `util.Enmascarar`.
- Código/comentarios en español y estilo didáctico. Iconos Compose por defecto.
- Sin dependencias nuevas ni subir AGP/Kotlin/Compose/Gradle. `minSdk = 29`.
- Android solo entrega al servicio llamadas de números que NO están en la agenda: es una
  limitación conocida, no un fallo.

## Comandos (Windows/PowerShell)
```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```
APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Arquitectura
- `data/`: `Regla`, `BaseDatos` (tabla `reglas`, semilla 400), `RepositorioReglas`
  (singleton con `applicationContext`, caché de reglas activas refrescada en cada CRUD).
- `motor/`: `MotorFiltrado`, `Decision`, `Accion`, `ValidacionReglas` (Kotlin puro).
- `ui/`: pantallas Compose; `MainActivity` solo monta `PantallaInicio`.
- `util/`: `Enmascarar` (futuro CSV en Fase 3).
- Servicio y UI comparten repositorio; la UI accede a la BD en `Dispatchers.IO`.

## Fase 3 (diseño previsto, NO implementar aún)
- Migración v1→v2 con `ALTER TABLE` (tipo, accion, prioridad) y tabla `registro`.
- Motor con prioridad, lista blanca, empate por especificidad y registro de actividad.
- CSV con SAF y estadísticas. Mantener el motor preparado para nuevos tipos de regla.

## Entregables
- Código compilando, APK debug y `docs/FASE<N>.md` con decisiones, dificultades, pruebas
  reales (comando + salida) y limitaciones.

## Estado de la Fase 3
- **Pasos hechos y commiteados:** Pasos 1-6 (migración v1→v2, `modelo/`, motor v3, validación/CRUD, navegación con pestañas, registro/ajustes).
- **En curso / escritos sin verificar:** Pasos 7-8 (estadísticas, pestañas Estadísticas y Ajustes, CSV con SAF) en el último commit "WIP fase3 (7-8)". El último comando falló; **sin verificar aún**.
- **Pendientes:** Verificar y estabilizar pasos 7-8. **No empezar paso 10 hasta que se indique.**
- **Condiciones vigentes:**
  - Windows PowerShell (no usar `tail`, `head` ni `grep`; usar `Select-Object -Last ...` o `Select-String`).
  - **No usar emulador ni `connectedDebugAndroidTest` hasta el paso 10.**
  - **No ejecutar git.**
  - Sin dependencias nuevas, sin subir AGP/Kotlin/Compose/Gradle. `minSdk = 29`.
  - SQLite nativo con `SQLiteOpenHelper`; ajustes en `SharedPreferences`. Sin Room/KSP/kapt.
  - **Límites del CSV (1 MB, 5000 filas)**.
  - **Enmascarado obligatorio del número** (`util.Enmascarar`); nunca loguear el número completo.
  - **Registro síncrono tras responder** (el servicio responde primero y registra después en `try/catch`).
  - **Los tests instrumentados nunca usan filtro400.db**.
  - `onScreenCall`: fail-open; responder una sola vez antes de loguear/registrar.
  - `ReglaBloqueo.normalizar()` es la única normalización; `MotorFiltrado.evaluar()` es puro (sin Android).
