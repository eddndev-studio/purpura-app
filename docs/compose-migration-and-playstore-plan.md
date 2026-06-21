# Plan: migracion a Jetpack Compose + modernizacion + publicacion en Play Store

Fecha de inicio: 2026-06-20. Rama: `feat/compose-migration`. Estado: EN PROGRESO.

## Encuadre (que es y que NO es esto)

- **NO es un rediseno.** El sistema de diseno (`specs/02-design-system.md`: marca
  morada, forma pill, roles M3, modo oscuro, contraste AA verificado) es bueno y se
  conserva **verbatim**. Se porta a Compose (Color/Type/Shape/Theme) y se moderniza
  la **ejecucion** (componentes Compose) + se anade **movimiento** (motion).
- **El objetivo** es portar las 11 pantallas de XML/ViewBinding a Compose, restilizar
  el mapa para que no se vea "Google Maps crudo", anadir animaciones modernas y dejar
  el build listo para Play Store (AAB firmado, SDK al dia, metadata/assets de tienda).

## "Done" alcanzable de forma autonoma (codigo)

1. [x] Toolchain: compileSdk/targetSdk 36, Compose BOM 2026.06.00 + plugin compose de Kotlin 2.3. (build verde)
2. [x] Design system portado a Compose (Theme, Color, Type, Shape, Extended) desde la spec. (build verde)
3. [x] Libreria de componentes Compose compartidos (card, badges, estados, interop, mapa lite) con motion.
4. [x] Las 11 pantallas migradas a Compose (interop Fragment->ComposeView; build verde en cada paso):
       Inicio, Acerca de, Auth, Respaldo, Restaurar, Calendario, Mapa de calor, Consultar,
       Detalle, Anadir/Editar, y el selector de ubicacion (mapa restilizado, sigue como Fragment).
5. [x] Mapa restilizado (estilo morado dia/noche, marcador de marca) en selector y mapa lite del Detalle.
6. [x] Motion: animateItem en listas/rejillas, animateColorAsState en selecciones, Crossfade/AnimatedVisibility,
       pull-to-refresh M3. (Transiciones a nivel navegacion: pendientes; requieren colapsar a Navigation-Compose.)
7. [x] Build emite **AAB firmado** (`bundleRelease`) + release.yml actualizado.
8. [x] Metadata y assets de la ficha de Play redactados en `docs/playstore/`.
9. [x] Checklist de los pasos SOLO-humanos de Play Console (handoff) en `docs/playstore/console-checklist.md`.

## Pendiente (no bloqueante del "done" de codigo)

- Verificacion RUNTIME/VISUAL en device/emulador. Se escribio `app/src/androidTest/.../ScreenSmokeTest.kt`
  (monta las 10 pantallas con estado de ejemplo y verifica que componen sin crashear; cubre el riesgo
  de scroll anidado en los grids). COMPILA y construye el test APK, pero NO se pudo EJECUTAR aqui: el
  emulador headless no llego a boot_completed en este entorno (3 intentos). Correr en una maquina con
  emulador/device:  `./gradlew :app:connectedDebugAndroidTest`. Luego QA visual + capturas para la ficha.
- El scaffold de MainActivity (drawer/toolbar/bottom nav) sigue en XML hospedando el NavHost; las
  pantallas son Compose. Colapsar a Navigation-Compose single-activity habilitaria transiciones de
  navegacion y shared elements; es una mejora posterior, no requisito de publicacion.

## Pasos SOLO-humanos (handoff, no los puedo hacer yo)

- Crear/gestionar la app en Play Console, subir el AAB, completar Data Safety,
  clasificacion de contenido, hospedar la politica de privacidad, QA en device real,
  capturas finales. Estos NO son "fallo": son entrega manual.
- Confirmar si la app YA esta publicada (el usuario dijo "me parece"): cambia
  app-nueva vs update, continuidad de versionCode y reclamo de paquete.

## Hechos verificados (2026-06-20)

- Baseline `:app:assembleDebug` = VERDE antes de tocar nada.
- Plataformas SDK instaladas: android-34/35/36; build-tools 34/35.
- Play exige targetSdk >= 35 desde 31-ago-2025; "dentro de 1 ano" del ultimo release
  desde nov-2025. Android 16 (API 36) ya salio -> apuntamos a 36.
- Pipeline actual (`release.yml`) genera **APK** (`assembleRelease`), NO AAB. Gap real.
- Sin Compose en el codigo aun. App: domain (51 kt) + app (77 kt), Hilt/Room/Retrofit,
  Navigation Component, ~11 pantallas XML+ViewBinding.

## Riesgo / verificacion visual

La calidad visual NO se puede ver desde el codigo. Se mantiene el build verde tras cada
pantalla; la calidad visual queda marcada como **pendiente de revision en device** hasta
poder correr emulador/capturas.

## Bitacora de progreso

- 2026-06-20: rama creada, baseline verde, plan escrito. Empezando foundation.
- 2026-06-20: foundation (SDK 36 + Compose + tema) verde. Inicio/Acerca migradas (patron probado).
- 2026-06-20: 6 pantallas migradas via dynamic workflow (Auth/Backup/Restore/Calendar/Heatmap/Query),
  build verde al primer intento. AAB en release.yml + metadata de Play. Mapa restilizado.
- 2026-06-20: Detalle y Anadir/Editar migradas. 11/11 pantallas en Compose. build + tests verdes.
  Codigo del "done" completo; queda verificacion visual en device (handoff).
