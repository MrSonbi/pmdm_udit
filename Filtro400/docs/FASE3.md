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

## Tests nuevos
- `MotorFiltradoFase3Test`: ganaPrioridadSobreLongitud, numeroExactoGanaAPrefijo, ganaPatronMasLargoConMismaPrioridadYSinNumeroExacto, permiteGanaEnEmpateAbsoluto, listaBlancaConMayorPrioridadPermiteAunqueHayaBloqueo, reglaNumeroExactaNoCoincideSiEsDiferente, reglaInactivaConCamposNuevosNoAplica, listaBlancaInactivaNoPermite, prefijoDePrioridadAltaGanaANumeroDePrioridadBaja, numeroFicticioConRegla400PorDefectoSeBloquea, numeroFicticioConPermitirExactoPasa
- `ValidacionReglasFase3Test`: bloquearPorDefectoTienePrioridadCero, permitirPorDefectoTienePrioridadCien, permiteFijarPrioridadManual, rechazaPrioridadFueraDeRango, validaReglaNumeroExacto, prioridadPorDefectoHelperCoincide
- `MapeoReglaTest`: mapeaFilaValida, omiteFilaConPrioridadFueraDeRango, omiteFilaConTipoOAccionDesconocidos

## Limitaciones
- Solo llegan al filtro las llamadas de números que NO están en la agenda (limitación de Android).
- `CallResponse` sin cambios. `fail-open`. Nunca se loguea el número completo.
- No se puede volver a una versión anterior de la app con la BD v2 (SQLiteOpenHelper no permite bajar de versión).
- Sin dependencias nuevas, sin subir AGP/Kotlin/Compose/Gradle, `minSdk = 29`.
- Sin Room/KSP/kapt.
