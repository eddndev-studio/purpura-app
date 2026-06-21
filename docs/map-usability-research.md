# Research: usabilidad del selector de ubicacion (mapa)

Fecha: 2026-06-09. Objetivo: mejorar lo dificil que es encontrar un lugar en el
mapa actual. Pensado para planear la 0.6.1.

## 1. De donde partimos (codigo actual)

`ui/location/LocationPickerFragment.kt` (+ `fragment_location_picker.xml`):

- Solo **tap-para-marcar**: el usuario debe paneear desde el centro por defecto
  (CDMX, zoom 10) y tocar el punto. No hay busqueda de ningun tipo.
- Geocodificacion **inversa** best-effort con el `Geocoder` de Android, y SOLO al
  confirmar (devuelve la etiqueta por Fragment Result). El usuario no ve la
  direccion del punto antes de confirmar.
- **Sin "mi ubicacion"** (a proposito no se pide `ACCESS_FINE_LOCATION`).
- Dependencia: solo `play-services-maps` 18.2.0. **No hay Places SDK**.
- API key restringida a **solo servicio Maps** + paquete + 2 SHA-1. APIs
  habilitadas en `purpura-eddndev`: solo Maps Android (no Places, no Geocoding).
  Billing activo.

El hueco principal es claro: **no se puede buscar por nombre/direccion**.

## 2. Opciones de busqueda (la pieza que pediste)

### A1 (RECOMENDADO) - Places Autocomplete (New), Places SDK for Android

Barra de busqueda que sugiere lugares mientras escribes (POI por nombre +
direcciones). Al elegir una prediccion, se obtienen lat/lng + nombre via Place
Details y se centra el mapa con el marcador.

- SDK: `com.google.android.libraries.places:places` (Autocomplete **New**, v3.5.0+;
  NO usar el autocomplete *legacy*, esta deprecado).
- **Session tokens** (UUID v4): agrupan las pulsaciones + la seleccion en una
  "sesion" para facturacion. Se generan en cliente y la sesion cierra con el
  Place Details que usa el mismo token.
- Setup GCP necesario: habilitar **Places API (New)** en `purpura-eddndev` y
  **ampliar la restriccion de la Maps key** para permitir tambien Places (hoy es
  Maps-only -> sin esto da PERMISSION_DENIED), o emitir una key aparte.
- **Costo a nuestra escala: $0.** Desde marzo 2025 cada SKU tiene tope gratis
  mensual propio; Autocomplete (per request), Place Details con campos Essentials
  (LAT_LNG, FORMATTED_ADDRESS) y Dynamic Maps son **Essentials = 10,000 eventos
  gratis/mes**. La sesion de Autocomplete y Place Details "IDs Only" son
  ilimitados sin cargo. Un proyecto de clase no se acerca al tope. (Precios fuera
  de tope, como referencia: Autocomplete ~$2.83/1k, Geocoding $5/1k, Dynamic Maps
  $7/1k.)
- Esfuerzo: medio. Mejor UX con diferencia.
- Caveat repo publico: la key queda expuesta pero sigue restringida por
  SHA-1+paquete; ampliarla a Places solo habilita Places desde nuestra app
  firmada.

### A2 - Busqueda con el Geocoder de Android (gratis, cero GCP)

Un campo de texto -> `Geocoder.getFromLocationName(query, n, bounds)` en hilo IO
-> centra el mapa en el mejor resultado y coloca el marcador. Espeja el patron de
geocodificacion inversa que ya tenemos.

- Pros: **cero facturacion, cero setup** (no toca billing ni la key), dependencia
  nula, llega rapido.
- Contras: **sin autocompletado** (hay que escribir y enviar), calidad variable
  segun el backend del dispositivo, puede devolver vacio, y es mas de
  direcciones que de nombres de negocio (POI hit-or-miss). En Android 13+ usar la
  sobrecarga asincrona con listener (la bloqueante esta deprecada; hoy ya la
  @Suppress-eamos en la inversa).
- Esfuerzo: bajo.

**Recomendacion:** A1 es el arreglo real de usabilidad y a nuestro volumen es
gratis. A2 sirve si se quiere 0.6.1 sin tocar GCP/keys; incluso se puede enviar
A2 ya y A1 despues. No mezclar ambas barras: elegir una.

## 3. Orientacion: "mi ubicacion" (alto valor, bajo esfuerzo)

### B1 - Boton/punto de mi ubicacion

`map.isMyLocationEnabled = true` + `FusedLocationProviderClient`
(`play-services-location`) para centrar en la posicion actual.

- Requiere `ACCESS_FINE_LOCATION` (o COARSE) en runtime -> **cambia la postura de
  permisos** que hoy evitamos a proposito. Para un selector de mapa es UX
  esperada; COARSE es menos intrusivo si basta aproximar.
- Costo: gratis (GPS del device, sin API).
- Tambien mejora el centro por defecto: en vez de CDMX fijo, abrir cerca del
  usuario.

## 4. Pulido de interaccion (bajo esfuerzo, gratis)

- **C1 - Reticula central (crosshair):** pin fijo en el centro; el usuario panea
  el mapa debajo y confirma con el `camera.target`. Mas preciso que el tap; comun
  en apps de delivery/ride.
- **C2 - Direccion en vivo:** reverse-geocode en `OnCameraIdleListener` (o al
  marcar) y mostrar la direccion en una tarjeta inferior ANTES de confirmar, para
  que el usuario sepa que esta eligiendo. (Hoy solo se geocodifica al confirmar.)
- **C3 - Controles del mapa:** `uiSettings` con zoom buttons, brujula, boton de mi
  ubicacion.
- **C4 - Tipo de mapa:** toggle normal/satelite/hibrido (`map.mapType`); el
  satelite ayuda a ubicar lugares visualmente.
- **C5 - Sesgo regional:** limitar autocomplete/geocoder a Mexico y al viewport
  visible para resultados mas relevantes.

## 5. Extras (si sobra tiempo)

- **D1 - Recientes/favoritos:** guardar ubicaciones usadas (Room/prefs) para
  re-elegir rapido. Offline, gratis.
- **D2 - "Buscar en esta zona"** al paneear.

## 6. Encaje arquitectonico (clean, <400 LOC, TDD)

- El `LocationPickerFragment` hoy no tiene ViewModel. Para A1/A2 conviene un
  `LocationPickerViewModel` que sostenga el query, la lista de predicciones y el
  punto elegido (sobreviven rotacion) y deje el Fragment fino (<400 LOC).
- Mantener las llamadas a Places/Geocoder en un adaptador delgado (data o ui),
  idealmente tras un puerto si se quiere testear sin red (espeja como
  `CloudBackupRepository` aisla Drive). El manejo de session token vive ahi.
- A2 es casi copy del `reverseGeocode` existente -> trivial de testear.

## 7. Costos a nuestra escala (resumen)

Todo lo propuesto cae en **Essentials (10k gratis/mes)** o es gratis del device
(Geocoder de Android, mi ubicacion). A volumen de proyecto de clase: **$0/mes**.

## 8. Alcance sugerido para 0.6.1

Minimo de alto impacto:

1. Busqueda: **A1 (Places Autocomplete New)** -- el arreglo real. (Alternativa de
   cero-riesgo: A2 con el Geocoder.)
2. **C2** direccion en vivo + **C1** reticula central (mas preciso y claro).
3. **B1** boton de mi ubicacion (si aceptamos el permiso de ubicacion).

Dejar C4/D1/D2 para despues. Verificar TODO en device (el render del mapa y la
busqueda solo se prueban en telefono firmado con el SHA-1 registrado) antes de
taggear 0.6.1.

## Fuentes

- Places SDK for Android - Autocomplete (New): https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete
- Session tokens y pricing: https://developers.google.com/maps/documentation/places/android-sdk/session-pricing
- Cambios de precios marzo 2025: https://developers.google.com/maps/billing-and-pricing/march-2025
- Lista de precios Core Services: https://developers.google.com/maps/billing-and-pricing/pricing
- Geocoder (Android, getFromLocationName): https://developer.android.com/reference/android/location/Geocoder
