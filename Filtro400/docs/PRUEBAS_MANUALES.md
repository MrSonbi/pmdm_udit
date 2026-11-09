# Pruebas manuales — Fase 3 (Filtro 400)

Entorno: emulador Android (API ≥ 29) con `app/build/outputs/apk/debug/app-debug.apk` instalado y
Filtro 400 como app de filtrado de llamadas (rol `ROLE_CALL_SCREENING`), salvo que el caso diga otra
cosa. Usar solo números **ficticios** que no estén en la agenda del emulador (Android solo entrega al
servicio llamadas de números fuera de la agenda). Llamada simulada: *Extended controls → Phone →
Call device*, o `adb emu gsm call <número>`.

Las columnas **Resultado real** y **Captura** las rellena el alumno.

| # | Caso | Pasos | Resultado esperado | Resultado real | Captura |
|---|---|---|---|---|---|
| 1 | Llamada simulada con la app cerrada | 1. Con la regla 400 activa, cerrar la app (quitarla de recientes). 2. Llamar desde el emulador al número ficticio `400123456`. 3. Llamar a `600111222`. 4. Abrir la app → Registro. | `400123456` no suena (rechazada, sin notificación). `600111222` suena con normalidad. El Registro muestra las dos filas con el número enmascarado (`400123***`, `600111***`), BLOQUEADA y PERMITIDA. | | |
| 2 | Cambio de regla en caliente | 1. En Reglas, añadir prefijo `612` / Bloquear. 2. Sin reiniciar nada, llamar desde `612000000`. 3. Desactivar la regla `612` y volver a llamar. | La primera llamada se bloquea; tras desactivar la regla, la segunda suena. No hace falta reiniciar la app ni el emulador. | | |
| 3 | Lista blanca gana al bloqueo | 1. Con el prefijo `400` / Bloquear (prioridad 0), añadir número exacto `400999888` / Permitir (prioridad por defecto 100). 2. Llamar desde `400999888`. 3. Llamar desde `400999887`. | `400999888` suena (permitida por la regla «400999888»); `400999887` se bloquea por «400». El Registro muestra la regla aplicada en cada caso. | | |
| 4 | Ajuste "guardar registro" | 1. Ajustes → apagar "Guardar registro". 2. Llamar desde `400123456`. 3. Abrir Registro. 4. Volver a encenderlo y repetir la llamada. | Con el ajuste apagado la llamada se bloquea igual, pero no aparece ninguna fila nueva. Con el ajuste encendido sí aparece. | | |
| 5 | Borrar registro | 1. Con varias filas en Registro, pulsar borrar. 2. Cancelar en el diálogo. 3. Repetir y confirmar. | Al cancelar no cambia nada. Al confirmar, el Registro queda vacío y Estadísticas muestra 0. | | |
| 6 | Estadísticas | 1. Hacer 2 llamadas bloqueadas y 1 permitida. 2. Abrir Estadísticas. | "Hoy", "Últimos 7 días" y "Todo el registro" muestran 2 bloqueadas y 1 permitida por separado; el ranking muestra las reglas que han decidido. | | |
| 7 | Exportar e importar CSV (ida y vuelta) | 1. Ajustes → Exportar CSV y guardarlo con el selector del sistema. 2. Borrar una regla en la app. 3. Ajustes → Importar el mismo fichero. | El CSV tiene la cabecera `patron;tipo;accion;prioridad;activa;etiqueta`. Al importar, la regla borrada vuelve a aparecer ("1 añadida") y el resto cuentan como omitidas (sin cambios). | | |
| 8 | Importar CSV con filas erróneas | 1. Preparar un CSV con filas válidas y otras con tipo desconocido, prioridad no numérica, prioridad 150 y menos de 3 columnas. 2. Importarlo. | Las filas válidas se aplican; el resumen indica cuántas son erróneas y la lista muestra "Línea N: …" con el motivo de cada una. Ninguna regla existente se borra. | | |
| 9 | Simulador sin guardado | 1. Inicio → Simulador, interruptor "Guardar en el registro" apagado. 2. Simular `400123456`. 3. Abrir Registro. | Muestra "Se BLOQUEARÍA", la regla «400», tipo, prioridad, motivo y el número enmascarado. No se añade ninguna fila al Registro. | | |
| 10 | Simulador con guardado | 1. Encender "Guardar en el registro" y simular `400123456`. 2. Registro y Estadísticas con "incluir simulaciones" apagado y luego encendido. | Con el interruptor apagado la simulación no aparece ni cuenta. Encendido, aparece marcada "SIMULADA (no es una llamada real)" y cuenta en Estadísticas. | | |
| 11 | Migración v1 → v2 con el APK de la Fase 2 | 1. Desinstalar la app. 2. Instalar el APK de la Fase 2, añadir reglas (p. ej. `612`) y borrar la regla `400`. 3. Instalar encima el APK de la Fase 3 (`adb install -r app-debug.apk`). 4. Abrir Reglas. | Se conservan las reglas de la Fase 2 como Prefijo • Bloquear • prioridad 0; la regla `400` **no** reaparece; Registro funciona (tabla nueva vacía). | | |
| 12 | Estado del rol | 1. Instalación limpia: abrir la app. 2. Pulsar activar y conceder el rol. 3. Quitar el rol desde los ajustes del sistema y volver a la app. | La tarjeta de estado indica que el filtro no está activo; tras conceder el rol indica que está activo; al quitarlo y volver, indica de nuevo que no está activo. | | |
