# purpura-app

App Android de Purpura, el organizador de eventos. Escrita en Kotlin con XML Views,
Clean Architecture y desarrollada con TDD. Consume el backend
[purpura-backend](https://api.purpura.eddn.dev).

## Stack

- Kotlin + Android SDK (Views, ViewBinding/DataBinding)
- Arquitectura: Clean (presentación MVVM + dominio + datos)
- DI: Hilt
- Navegación: Navigation Component (single-activity)
- Asíncrono: Coroutines + Flow
- Red: Retrofit + OkHttp
- Persistencia local: Room (caché)
- Auth: Google Sign-In + JWT del backend
- Mapas: Google Maps SDK
- Recordatorios: AlarmManager (alarmas exactas) + notificaciones locales

## Requisitos

- Android Studio (Ladybug o superior) o Gradle CLI
- JDK 21 para correr Gradle (AGP 8.6)
- Android SDK con `compileSdk = 34`
- Dispositivo/emulador con `minSdk = 26` (Android 8.0) o superior

## Estructura

Dos módulos Gradle: `:app` (UI + datos) y `:domain` (núcleo puro, sin dependencias de Android).

```
app/
  src/main/java/com/eddndev/purpura/
    ui/            pantallas (Fragments + ViewModels) por feature
    data/          implementaciones de repos: Retrofit, Room, mappers, recordatorios
    di/            módulos de Hilt
domain/
  src/main/kotlin/com/eddndev/purpura/domain/
    model/         entidades del negocio (Event, Contact, Location, ...)
    repository/    puertos (interfaces) que la capa de datos implementa
    usecase/       casos de uso que orquestan el dominio
    query/         filtros de consulta
docs/
  PLAN-PRUEBAS-DEVICE.md   plan de pruebas manuales en dispositivo
```

## Configuración

La app necesita una clave de Google Maps y el client ID de Google Sign-In. La clave de
Maps se inyecta en el build (no se versiona):

- Local: define `MAPS_API_KEY` en `~/.gradle/gradle.properties` o `local.properties`.
- CI: se inyecta como secreto en el workflow de release.

El endpoint del backend por defecto es `https://api.purpura.eddn.dev` (producción).

## Compilar y correr

```bash
# Tests unitarios (app + domain)
./gradlew testDebugUnitTest

# APK debug
./gradlew :app:assembleDebug

# Instalar en un dispositivo conectado
./gradlew :app:installDebug
```

> Si Gradle falla por versión de Java, exporta `JAVA_HOME` a un JDK 21:
> `export JAVA_HOME=$(ls -d /usr/lib/jvm/java-21-openjdk* | head -1)`.

## Versionado y releases

`versionName`/`versionCode` se inyectan desde el entorno (`APP_VERSION_NAME`,
`APP_VERSION_CODE`) en el build firmado, de modo que el tag `vX.Y.Z`, el nombre del
artefacto y la versión dentro del APK nunca se desincronizan. El pipeline de release
(GitHub Actions) firma el APK por cada tag `vX.Y.Z`.

## Funcionalidades

- Autenticación con Google y email/contraseña, con persistencia de sesión.
- Inicio: eventos de hoy y próximos días, con pull-to-refresh.
- Añadir / editar evento: tipo, contacto (selector del sistema), lugar (etiqueta o
  punto en el mapa), fecha/hora, estatus y recordatorio.
- Consultar: filtros por texto, mes y año.
- Calendario mensual con eventos por día.
- Mapa de calor mensual de densidad de eventos (acceso desde la toolbar de Inicio/Calendario).
- Recordatorios con alarmas exactas y notificación local que abre el evento al tocarla.
- Respaldo y restauración de eventos en Drive/Dropbox (SAF).
- Acerca de, con cierre de sesión.

## Pruebas

- Unitarias: `./gradlew testDebugUnitTest` (lógica de dominio y ViewModels).
- En dispositivo: ver [`docs/PLAN-PRUEBAS-DEVICE.md`](docs/PLAN-PRUEBAS-DEVICE.md), que cubre
  render y flujos en pantalla que el CI no verifica, y sirve de guion para las capturas del
  manual de usuario.
