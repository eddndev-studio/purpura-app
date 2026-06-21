# Purpura - Especificacion de rediseno UI (contrato)

Estado actual (2026-06-20): la migracion XML->Compose dejo pantallas funcionales pero con
"look de demo": navegacion partida (drawer de 7 + bottom nav de 3 con "Salir"), banda morada
en la status bar (edge-to-edge mal resuelto en Android 15), layouts portados literalmente, sin
jerarquia ni motion, y mapas con el look crudo de Google Maps.

Este documento es EL CONTRATO. Todo agente de rediseno lo obedece al pie de la letra. La meta es
un producto de punta, publicable, coherente pantalla a pantalla.

## 0. Reglas no negociables (convenciones del repo)

- ASCII puro. SIN emojis en codigo, comentarios ni strings de UI.
- < 400 LOC por archivo. Si una pantalla crece, extraer composables privados al mismo archivo o
  a un archivo `XScreenParts.kt` hermano.
- Mantener el hosting por Fragment + Navigation Component (NO migrar a Compose Navigation).
- Cada pantalla es `XScreen(state, callbacks)` pura; el Fragment resuelve navegacion/launchers.
- Reusar el design system. NO inventar tokens ni componentes nuevos: usar los de `ui/theme` y
  `ui/compose`. Si falta un componente, lo agrega la fase de fundacion, no un agente de pantalla.
- Marca mono-morado, sin dynamic color. Bordes pill en controles interactivos.
- Comentarios en espanol, concisos, explicando el porque.

## 1. Arquitectura de informacion (navegacion)

Eliminar el DrawerLayout y la `MaterialToolbar` XML. La Activity solo hospeda
`NavHostFragment` + `BottomNavigationView`. El app bar pasa a cada pantalla (TopAppBar M3).

Bottom nav = 4 destinos (M3 NavigationBar, indicador pill):

1. Inicio       -> homeFragment       (ic_home)
2. Calendario   -> calendarFragment   (ic_calendar_month)
3. Consultar    -> queryFragment      (ic_search)
4. Cuenta       -> accountFragment    (ic_person / nuevo)

- "Salir" (cerrar app) se ELIMINA de la navegacion principal.
- Respaldo, Restaurar, Acerca de y Cerrar sesion viven dentro de "Cuenta" (hub).
- Mapa de calor deja de ser destino suelto: es un MODO DE VISTA dentro de Calendario
  (toggle segmentado "Mes / Calor" en el TopAppBar de Calendario; al elegir Calor se navega
  a heatmapFragment, que muestra el mismo toggle para volver). Se elimina `menu_heatmap`.

Top-level (sin flecha atras): Inicio, Calendario, Consultar, Cuenta, Mapa de calor.
Con flecha atras (navigationIcon -> navigateUp): Detalle, Anadir/Editar, Respaldo, Restaurar,
Acerca de, Selector de ubicacion.

## 2. Edge-to-edge y system bars

- `MainActivity.onCreate`: `enableEdgeToEdge()`. Quitar `android:statusBarColor` del tema
  (ignorado en A15). Sin DrawerLayout no se pinta el scrim morado.
- El root de `activity_main.xml` NO usa `fitsSystemWindows`; los insets fluyen a Compose.
- Cada pantalla maneja insets via Scaffold + TopAppBar (`windowInsets` por defecto incluye
  status bar). El `BottomNavigationView` consume el inset inferior (gesture bar).
- Iconos de status bar: claros en dark, oscuros en light (ya lo hace `PurpuraTheme`).

## 3. Escala de espaciado y grid

Usar multiplos de 4. Margen horizontal de pantalla = 16dp. Separacion entre secciones = 24dp.
Separacion entre items de lista = 12dp. Padding interno de cards = 16dp. Radio de superficies:
cards 16dp, contenedores grandes 20-24dp, controles pill (50%).

Tokens Compose en `ui/theme/Dimens.kt` (nuevo) o dp literales segun spec. Preferir el objeto
`Spacing` si se crea en fundacion.

## 4. Tipografia (jerarquia)

- TopAppBar grande: `headlineMedium` (titulos de seccion de nivel superior con LargeTopAppBar
  donde aporte; small TopAppBar en pantallas con back).
- Titulo de card / item: `titleMedium`.
- Cuerpo: `bodyMedium` / `bodyLarge`.
- Etiquetas y metadatos: `labelLarge` / `labelMedium` en `onSurfaceVariant`.
- Numeros destacados (hora del evento, conteos): `headlineSmall` / `titleLarge` en primary.

## 5. Componentes compartidos (los provee la fundacion; los agentes solo los usan)

Existentes a mantener/mejorar: `EventCard`, `EmptyState`, `ErrorState`, `LoadingState`,
`PurpuraBadge`/`EventTypeBadge`/`EventStatusBadge`, `LiteLocationMap`, `colorsFor`.

Nuevos / mejorados en fundacion:

- `PurpuraTopBar(title, subtitle?, onBack?, actions)` y variante grande -> wrapper sobre M3
  `TopAppBar` / `LargeTopAppBar` con colores de marca y manejo de inset.
- `SectionHeader(text, trailing?)` -> etiqueta de seccion consistente (labelLarge, primary).
- `SegmentedToggle(options, selected, onSelect)` -> control segmentado pill (Mes/Calor, etc.).
- `MapCard(lat, lng, label?, onOpenExternal?)` -> presentacion pulida del mapa: card 16dp,
  altura fija, marcador de marca, sombra suave, etiqueta de direccion sobre degradado inferior,
  affordance "abrir en Maps". Resuelve el "look crudo" de Maps.
- `InfoRow(icon, label, value)` -> fila etiqueta/valor para Detalle y Cuenta.
- `EventCard` (mejora): tesela de fecha mas legible, jerarquia tipo/estado, chevron sutil,
  ripple y `animateItem`.

## 6. Motion / animaciones

- Transiciones de pantalla: fade-through suave (NavHost o por pantalla con AnimatedContent donde
  aplique). No exagerar.
- Listas: `animateItem()` en items; aparicion escalonada sutil opcional.
- Estados (loading/empty/error): `Crossfade` o `AnimatedContent`.
- Selecciones (chips, dias, toggle): `animateColorAsState` / `animateFloatAsState`.
- FAB: que reaccione al scroll (extendido -> compacto) en Inicio.
- Duraciones cortas (150-300ms), curvas estandar M3. Respetar accesibilidad.

## 7. Mapas (requerimiento explicito del usuario)

El look crudo de Google Maps "se siente extrano". Mejoras:
- Usar el estilo JSON de marca ya existente (`MapStyling`, dia/noche) en TODOS los mapas.
- Encapsular en `MapCard`: esquinas 16dp, sin UI nativa de Maps (sin botones/zoom/brujula en la
  vista lite), marcador morado de marca, degradado inferior con la direccion, y un boton
  "Abrir en Maps" para el detalle. En el selector de ubicacion, mapa a pantalla con controles
  minimos y pin central fijo + boton confirmar pill.

## 8. Briefs por pantalla (rediseno)

Inicio (EXEMPLAR, lo hace la fundacion):
- LargeTopAppBar "Inicio" con saludo/fecha de hoy como subtitulo.
- Encabezado de seccion "Proximos 5 dias". Lista de `EventCard` con espaciado 12dp.
- Empty state centrado y atractivo. FAB extendido que se compacta al hacer scroll.

Calendario:
- TopAppBar con `SegmentedToggle` Mes/Calor. Grid de mes con celdas comodas (>=48dp),
  hoy y seleccionado claramente distinguibles, puntos por tipo. Lista del dia debajo con
  SectionHeader y EventCards. Navegacion de mes con flechas grandes y mes/anio centrado.

Mapa de calor:
- Mismo TopAppBar con toggle Mes/Calor (Calor activo). Grid de intensidad con celdas con
  respiro, leyenda clara (Menos/Mas) legible, total del mes. Tap de dia -> snackbar/conteo.

Consultar:
- Filtros agrupados en secciones con SectionHeader (Periodo / Tipo / Estado), chips reflow.
  Boton de rango de fecha integrado. Resultados con paginacion y EventCards. Estados claros.
- Considerar mover filtros a un bottom sheet "Filtros" con resumen de chips activos arriba.

Detalle:
- TopAppBar con back y acciones (editar). Cabecera con fecha/hora destacada y badges tipo/estado.
- Secciones con InfoRow (contacto, ubicacion, recordatorio). `MapCard` pulido para la ubicacion.
- Estado como `SegmentedToggle` o segmented buttons. Acciones: editar (primario) + eliminar
  (texto/peligro) con confirmacion.

Anadir/Editar:
- TopAppBar con back + titulo (Anadir / Editar). Formulario seccionado con SectionHeader.
  Campos pill, validacion inline. Tipo/recordatorio en FlowRow de chips. Fecha/hora con pickers
  M3 nativos de Compose (no delegar a dialogos XML). Boton guardar pill, con estado de carga.

Cuenta (NUEVA):
- LargeTopAppBar "Cuenta". Cabecera con avatar + nombre/email de la sesion.
- Seccion "Datos": filas Respaldo y Restaurar (-> sus fragments).
- Seccion "Aplicacion": Acerca de (-> aboutFragment), version (BuildConfig).
- Boton "Cerrar sesion" (peligro) con confirmacion -> logout use case -> gate a Auth.

Respaldo / Restaurar:
- TopAppBar con back. Layout centrado mejorado: icono, titulo, descripcion, acciones jerarquizadas
  (Drive primario, archivo secundario). Feedback de resultado claro.

Acerca de:
- TopAppBar con back. Identidad de la app, version, creditos. (El logout se mueve a Cuenta;
  About puede conservar un acceso o solo informar.)

Auth:
- Sin chrome. Branding centrado, campos pill, jerarquia clara, boton Google con identidad
  correcta, divisor "o" real, manejo de teclado/foco, validacion de email.

## 9. Restricciones para el fan-out

- Un agente por pantalla edita SOLO su `ui/<screen>/XScreen.kt` (y su `XScreenParts.kt` si lo
  crea). NO toca `ui/theme/*`, `ui/compose/*`, otros screens, Activity, nav graph ni recursos
  compartidos. Si cree que necesita un componente compartido nuevo, lo reporta (no lo crea).
- Mantener firmas `XScreen(state, callbacks)` salvo que se justifique; si cambian, actualizar el
  Fragment correspondiente (y solo ese).
- El build verde y la verificacion en device son responsabilidad de la fase de integracion (no
  de cada agente). Cada agente entrega codigo que compila a su leal saber, basado en el exemplar.

## 10. Definicion de "hecho"

- Compila (`:app:assembleDebug`), tests verdes.
- Coherencia visual con el exemplar y este spec.
- Verificado en device (V2314, Android 15): sin banda en status bar, navegacion 4-tabs limpia,
  mapas pulidos, sin crashes, redireccion a login al expirar sesion.
- Sin merge/push a main sin confirmacion explicita del usuario.
