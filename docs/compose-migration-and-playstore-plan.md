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
3. [ ] Libreria de componentes Compose compartidos (boton pill, card, chips de
       estatus/tipo, scaffold, search bar) con motion.
4. [ ] Las 11 pantallas migradas a Compose (interop Fragment->ComposeView por pantalla,
       compila y es shippable en cada paso) o Navigation-Compose si conviene.
5. [ ] Mapa restilizado (estilo nocturno/morado, marcadores custom, UX de seleccion).
6. [ ] Motion: transiciones de navegacion, animaciones de lista, ripples, shared element
       donde aporte.
7. [ ] Build emite **AAB firmado** (`bundleRelease`) + release.yml actualizado.
8. [ ] Metadata y assets de la ficha de Play (textos, descripcion, lista de capturas
       requeridas) redactados en `docs/playstore/`.
9. [ ] Checklist de los pasos SOLO-humanos de Play Console (handoff).

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
