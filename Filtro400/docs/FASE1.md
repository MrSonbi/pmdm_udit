# Fase 1 — Bloquear llamadas que empiezan por 400

## Qué hace

App Android (Kotlin y Jetpack Compose) que rechaza las llamadas entrantes cuyo número empieza por 400, tanto si llegan como `400123456` como `+34 400 123 456` o `0034 400 123 456`. No sustituye a la app de Teléfono. En esta fase el prefijo está escrito en el código: no hay base de datos ni pantalla de configuración.

Archivos principales:

- `ReglaBloqueo.kt`: decide si un número se bloquea (no depende de Android).
- `FiltroLlamadasService.kt`: servicio que recibe cada llamada y responde a Android.
- `MainActivity.kt`: pantalla mínima con el estado del filtro, el botón para pedir el rol y un probador de números.

## Decisiones técnicas

**`CallScreeningService`.** Es un servicio de Android al que el sistema avisa de cada llamada entrante antes de que suene (método `onScreenCall`) y al que se le puede decir qué hacer con ella. Lo elegí porque permite filtrar sin ser la app de teléfono predeterminada, que es lo que pide el enunciado.

**Registro en el Manifest.** El servicio se declara con `android:permission="android.permission.BIND_SCREENING_SERVICE"`, que asegura que solo el sistema puede vincularse a él, y un `<intent-filter>` con la acción `android.telecom.CallScreeningService`.

**`ROLE_CALL_SCREENING`.** Android solo usa el servicio si el usuario elige la app como app de filtrado de llamadas. La app no puede asignarse el rol ella misma: pide el diálogo del sistema con `RoleManager.createRequestRoleIntent(...)` y, al volver, comprueba con `isRoleHeld(...)` si se concedió. Por eso existe una actividad mínima. El rol y `callDirection` exigen Android 10 (API 29).

**Responder a Android.** Con `respondToCall(callDetails, respuesta)` y un `CallResponse`. Al bloquear: `setDisallowCall(true)` (no suena), `setRejectCall(true)` (se rechaza) y `setSkipNotification(true)` (sin aviso de llamada perdida). Siempre hay que responder, también cuando se deja pasar la llamada.

**Comprobar el prefijo.** `startsWith("400")` sobre el número ya normalizado. `normalizar()` se queda solo con dígitos y `+` y quita `+34` y `0034`, así que las tres formas de escribir el mismo número dan `400123456`. Un número extranjero como `+44 400…` queda como `+44400…` y no se bloquea. Un número oculto llega vacío y se deja pasar.

## Dificultades encontradas

- **El rol no se puede dar desde dentro de la app.** Hay que lanzar el diálogo del sistema y volver a comprobar el estado cuando el usuario responde.
- **Servicio sin declarar.** Al revisar el proyecto para empezar la Fase 2 vi que `FiltroLlamadasService` no figuraba en `AndroidManifest.xml`. Sin esa declaración Android no puede vincularse al servicio, así que en esta fase el filtro solo se había comprobado con el probador, no con llamadas. Se corrigió al empezar la Fase 2.
- **`minSdk` 24 con APIs de Android 10.** `@RequiresApi` solo silencia al compilador: en Android 9 o anterior la app fallaría al ejecutarse. Se arregló subiendo `minSdk` a 29.
- **Número completo en el log.** El `Log.d` de depuración escribía el número entero, que es un dato personal. En la Fase 2 se sustituyó por una versión enmascarada.

## Pruebas

Casos que cubre el probador (y, desde la Fase 2, los tests de `ReglaBloqueoTest`):

| Entrada | Resultado esperado |
|---|---|
| `+34 400 123 456` | se bloquea |
| `0034 400 123 456` | se bloquea |
| `400123456` | se bloquea |
| `+44 400 123 456` | pasa (es de otro país) |
| `612345678` | pasa |
| (vacío, número oculto) | pasa |

La comprobación con llamadas entrantes simuladas en el emulador se hizo a partir de la Fase 2, cuando el servicio ya estaba declarado (ver `FASE2.md`).

## Limitaciones

- Android solo entrega al servicio las llamadas de números que no están en la agenda, y no se debe suponer que llegan todas.
- Los números ocultos no se bloquean.
- El prefijo está fijo en el código (se resuelve en la Fase 2).
- Requiere Android 10 o superior.
