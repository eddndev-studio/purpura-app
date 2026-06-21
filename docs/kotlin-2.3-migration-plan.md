# Plan de migración a Kotlin 2.3 + Places (para retomar)

Fecha: 2026-06-09. Estado: **PAUSADO, listo para ejecutar.** Meta de la sesión:
"migrar a Kotlin 2.3+ el proyecto y aplicar la actualización para usar Places y
mejor usabilidad del mapa".

Las versiones de esta matriz fueron verificadas como EXISTENTES vía curl contra
repositorios autoritativos (Maven Central / Google Maven) en junio 2026 por un
workflow de investigación de 7 agentes. Confianza: **media** (las versiones son
correctas y mutuamente compatibles con alta certeza; el resultado del build verde
tras el salto K1->K2 y el riesgo de runtime de kotlin-reflect NO son garantizables
sin compilar y probar en device).

## 0. Estado actual del repo

- Rama: **`feat/app-map-place-search`** (creada desde main). Contiene, SIN COMMITEAR:
  - `gradle/libs.versions.toml`: ya agregada `places = "4.3.1"` (versión + library).
  - `app/build.gradle.kts`: ya agregado `implementation(libs.places)`.
  - `app/.../PurpuraApplication.kt`: inicializa Places
    (`Places.initializeWithNewPlacesApiEnabled`, leyendo la key del manifest, con
    guarda si está vacía).
  - `app/.../ui/location/LocationPickerFragment.kt`: reescrito con barra de
    búsqueda que lanza el widget Autocomplete (New)
    (`PlaceAutocomplete.createIntent` + `getPredictionFromIntent` + `fetchPlace`
    con session token), preservando tap-para-marcar y geocodificación inversa.
  - `app/.../res/layout/fragment_location_picker.xml`: barra de búsqueda pill.
  - `app/.../res/values/strings.xml`: `location_search_hint`, `location_search_error`.
- **El build está EN ROJO** a propósito: `kspDebugKotlin` falla porque
  `places:4.3.1` trae metadata Kotlin 2.1.0 y el proyecto compila con Kotlin
  1.9.24 ("metadata is 2.1.0, expected 1.9.0"). **La migración a Kotlin 2.3 lo
  resuelve** (el compilador 2.3 lee metadata 2.1.0 sin problema).
- `main` y el release `v0.6.0` (GDrive + icono) están intactos y verdes.

## 1. Matriz de versiones (libs.versions.toml [versions])

| clave | actual | -> nuevo | nota |
|---|---|---|---|
| agp | 8.6.0 | **8.13.2** | techo del tronco 8.x; AGP 8.13 exige Gradle >=8.13; D8/R8 de 8.13 procesan metadata Kotlin 2.3 |
| kotlin | 1.9.24 | **2.3.20** | NO 2.3.21: el POM de KSP 2.3.9 declara stdlib 2.3.20 -> cero skew con KSP |
| ksp | 1.9.24-1.0.20 | **2.3.9** | ESQUEMA PLANO nuevo (sin sufijo `-<kotlin>`; `2.3.x-2.0.x` = 404). KSP2 es el único motor en 2.3.x |
| hilt | 2.51.1 | **2.58** | techo: 2.59+ exige AGP 9. 2.58 trae kotlin-metadata-jvm 2.2.20 -> lee metadata 2.3 vía KSP sin workaround |
| room | 2.6.1 | **2.8.4** | targetea Kotlin 2.0+; KSP2; exportSchema=false -> bump simple sin código |
| coroutines | 1.8.1 | **1.11.0** | higiene (1.8.1 también compila) |
| moshi | 1.15.1 | **1.15.2** | higiene; NO basta solo (ver kotlin-reflect) |
| places | 4.3.1 | 4.3.1 | se mantiene; NO migrar a 5.x (elimina APIs, exige desugaring) |
| gradle (wrapper) | 8.9 | **8.13** | AGP 8.13.2 lo exige |

Los plugin ids NO cambian (solo su `version.ref`): `com.google.devtools.ksp`,
`com.google.dagger.hilt.android`.

## 2. GOTCHA CRÍTICO de runtime — pin de kotlin-reflect

`moshi-kotlin` (1.15.1 y 1.15.2) arrastra transitivamente **kotlin-reflect 1.8.21**
(confirmado en su POM). `KotlinJsonAdapterFactory` usa kotlin-reflect EN RUNTIME
para introspeccionar las clases de modelo de la app, que pasarán a tener metadata
Kotlin 2.3 -> **reflect 1.8.21 NO la puede leer -> crash al deserializar JSON
on-device** (login, listado de eventos). **Los 108 tests NO lo detectan.**

Mitigación OBLIGATORIA:
- `libs.versions.toml [libraries]`:
  `kotlin-reflect = { group = "org.jetbrains.kotlin", name = "kotlin-reflect", version.ref = "kotlin" }`
- `app/build.gradle.kts dependencies{}`: `implementation(libs.kotlin.reflect)`

## 3. Pasos de migración (ordenados)

1. `gradle/wrapper/gradle-wrapper.properties`: `gradle-8.9-bin.zip` ->
   `gradle-8.13-bin.zip`.
2. `gradle/libs.versions.toml [versions]`: aplicar la tabla de la sección 1.
3. `gradle/libs.versions.toml [libraries]`: añadir `kotlin-reflect` (sección 2).
4. `app/build.gradle.kts`: añadir `implementation(libs.kotlin.reflect)`. El resto
   no cambia: Hilt/Room siguen vía `ksp(...)` (NO kapt).
5. NO tocar `gradle.properties`: el flag `ksp.useKSP2` no existe y en 2.3.x KSP2 es
   el único motor (no añadirlo). No tocar `android.useAndroidX`/`nonTransitiveRClass`.
6. NO cambiar `build.gradle.kts` raíz ni `domain/build.gradle.kts`. El
   `kotlinOptions { jvmTarget = "17" }` de app y el `KotlinCompile` de domain
   siguen válidos en AGP 8.13 (DSL deprecado pero no roto; migrar a
   `compilerOptions` es opcional, fuera de alcance).
7. `./gradlew clean assembleDebug` (vigilar `kspDebugKotlin`). Si aparecen errores
   K2 (when no exhaustivo / smart-cast más estricto), corregir en código.
8. `./gradlew app:dependencies` -> confirmar que `kotlin-reflect` y `kotlin-stdlib*`
   resuelven a **2.3.20**, NO 1.8.21. No asumir alineación automática.
9. `./gradlew :domain:test :app:testDebugUnitTest` -> los 108 tests deben seguir
   verdes. Y `assembleRelease` + `lintVitalRelease`.
10. **VERIFICAR EN DEVICE** la deserialización JSON real (login, listado de eventos)
    para descartar el crash de kotlin-reflect; los tests no lo cubren. Probar el
    selector de ubicación con la búsqueda de Places. NO taggear hasta verificar.

JDK 21 para correr Gradle sigue siendo válido (AGP 8.13 pide JDK 17 mínimo).

## 4. Techos a NO rebasar

- **NO AGP 9.0.x:** desde Kotlin 2.3.0, aplicar `kotlin-android` junto a AGP 9
  lanza error de configuración (AGP 9 trae Kotlin integrado). 8.13.2 es el techo
  del tronco 8.x (no existe 8.14; AGP saltó a 9.0).
- **NO Hilt 2.59+:** exige AGP 9 / Gradle 9.1+.
- **NO Places 5.x:** elimina `AutocompleteFragment` y campos Place (ADDRESS,
  LAT_LNG, NAME...) y exige Java 8 desugaring -> es reescritura, fuera de alcance.
- **NO Kotlin 2.4 / 2.3.21:** fuera de la pareja verificada con KSP 2.3.9.

## 5. Riesgos a vigilar al compilar

- **K1->K2:** exhaustividad de `when` por flujo de datos (Stable en 2.3.0) y
  smart-casts/inferencia más estrictos pueden volverse ERROR. `allWarningsAsErrors`
  NO está activo (verificado en los 3 build.gradle.kts) -> las advertencias nuevas
  no rompen el build.
- **Room 2.8.4** se publicó en la era Kotlin 2.2 (no validado oficialmente contra
  2.3.x); se apoya en el contrato "Kotlin 2.0+" + forward-compat de KSP2.
- **Salto AGP 8.6->8.13.2 + Gradle 8.9->8.13:** posibles avisos de deprecación;
  revisar el log del primer build limpio.
- **kotlin-reflect (runtime):** ver sección 2.

## 6. Mejor usabilidad del mapa (parte del objetivo)

Ya implementado en la rama: **búsqueda de lugares** (Places Autocomplete New) con
barra pill -> es la mejora central de usabilidad. Requisito del usuario en GCP
(acción suya, no automatizable): habilitar **Places API (New)** en el proyecto
`purpura-eddndev` y **ampliar la restricción de la Maps key** para permitir Places
(hoy es Maps-only) o emitir una key aparte. Costo a escala de proyecto de clase:
$0 (Essentials, 10k gratis/mes). Detalle y extras (mi-ubicación, dirección en
vivo, retícula central, satélite, recientes) en `docs/map-usability-research.md`.

## 7. Cómo retomar

```
cd /home/eddndev/moviles/ProyectoMoviles/purpura-app
git branch                 # confirmar en feat/app-map-place-search
git status -s              # los cambios de Places + search siguen ahí
export ANDROID_HOME=$HOME/.local/share/android
export JAVA_HOME=$(ls -d /usr/lib/jvm/java-21* | head -1)
# aplicar secciones 1-6, luego:
./gradlew clean assembleDebug
```

Cuando todo verde + verificado en device: commits semánticos granulares (bump de
toolchain por un lado, feature de Places por otro), merge --no-ff a main, y
release 0.6.1.

## Fuentes (verificadas jun 2026)

- Kotlin/KSP: github.com/JetBrains/kotlin/releases, github.com/google/ksp/releases
  (KSP 2.3.0: "KSP version is no longer tied to the Kotlin compiler version").
- Hilt: github.com/google/dagger/releases (2.57 unshade metadata #4779; 2.58
  metadata-jvm 2.2.20; 2.59 exige AGP 9; issue #5001 confirma Kotlin 2.3).
- Room: developer.android.com/jetpack/androidx/releases/room (2.8.4).
- AGP: developer.android.com/build/kotlin-support y agp-8-13-0-release-notes.
- Places: developers.google.com/maps/documentation/places/android-sdk/release-notes.
- Moshi/reflect: repo1.maven.org/.../moshi-kotlin/1.15.2 (declara kotlin-reflect 1.8.21).
