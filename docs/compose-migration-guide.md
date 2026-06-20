# Guia de migracion de pantalla XML -> Compose (Purpura)

Patron PROBADO (build verde) en `ui/home` y `ui/about`. Toda pantalla nueva DEBE seguirlo al pie.

## Regla de oro

- NO cambiar el ViewModel ni su `UiState`. La pantalla Compose es una funcion de
  `(state, callbacks) -> UI`. Toda la logica de datos se queda en el ViewModel.
- El Fragment CONSERVA: launchers de `ActivityResult`, permisos en runtime, `setFragmentResult` /
  `setFragmentResultListener`, navegacion (`findNavController()`), menus. Pasa esas acciones como
  CALLBACKS (lambdas) al composable. El composable NUNCA llama a `findNavController` ni pide permisos.
- NO editar archivos COMPARTIDOS: `res/values/strings.xml`, `res/values*/colors.xml`, el tema
  (`ui/theme/*`) ni los componentes (`ui/compose/*`). Si necesitas un string nuevo, NO lo agregues:
  reusa uno existente o REPORTA en tu salida la clave + texto sugerido para que el orquestador lo cree.
- Maximo ~400 LOC por archivo. Comentarios y UI en espanol con acentos; identificadores en ASCII.

## El puente Fragment -> Compose

```kotlin
@AndroidEntryPoint
class FooFragment : Fragment() {
    private val viewModel: FooViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        purpuraComposeView {                       // de ui/compose/ComposeInterop.kt: ya aplica PurpuraTheme
            val state by viewModel.uiState.collectAsStateWithLifecycle()  // androidx.lifecycle.compose
            FooScreen(
                state = state,
                onBar = { findNavController().navigate(R.id.barFragment) },
                onErrorShown = viewModel::errorShown,
            )
        }

    // onViewCreated: registrar menus / fragment-result listeners como antes (si aplica).
}
```

`import androidx.compose.runtime.getValue` es obligatorio para el `by`.

## Componentes disponibles (ui/compose/)

- `EventCard(event, onClick, modifier)` - tarjeta de evento completa (lista de Inicio/Consultar/Calendario).
- `EventTypeBadge(type)`, `EventStatusBadge(status)`, `PurpuraBadge(text, colors)` - chips pill de solo lectura.
- `colorsFor(type)` / `colorsFor(status)` -> `ColorPair(strong, container)` para colorear a mano.
- `EmptyState(icon, title, body)`, `LoadingState()`, `ErrorState(icon, message, retryLabel, onRetry)`.
- `purpuraComposeView { ... }` - el puente de arriba.

## Tokens de tema (NO hardcodear color)

- Roles M3: `MaterialTheme.colorScheme.primary / onSurface / surfaceContainer / error / ...`
- Extendidos: `PurpuraTheme.colors.statusPendiente.strong`, `.heatmap[level]`, `.onBrandSubtle`, etc.
  (importar `com.eddndev.purpura.ui.theme.PurpuraTheme`).
- Forma: TODO lo interactivo (Button, OutlinedButton, FilterChip, TextField, FAB) usa `shape = Pill`
  (`com.eddndev.purpura.ui.theme.Pill`). Superficies (Card, Surface) usan `MaterialTheme.shapes.medium/large`.
- Tipografia: `MaterialTheme.typography.titleMedium / bodyLarge / labelSmall / ...`

## Etiquetas y formato (reusar, NO duplicar)

- `stringResource(EventDisplay.typeLabel(type))`, `EventDisplay.statusLabel(status)`, `reminderLabel(...)`.
- Fechas/horas: `EventDisplay.formatFullDate(...)`, `formatTime(...)`, `formatMonth(yearMonth)`, etc.
- Otros textos: `stringResource(R.string.xxx)` con las claves YA existentes del XML de la pantalla.

## Movimiento (motion) esperado

- Listas `LazyColumn`/`LazyVerticalGrid`: `Modifier.animateItem()` en cada item.
- Aparicion de contenido tras cargar: `AnimatedVisibility` o `Crossfade` entre loading/contenido/error.
- Cambios de seleccion (chips, dia de calendario): `animateColorAsState` / `animateDpAsState`.
- Pull-to-refresh: `androidx.compose.material3.pulltorefresh.PullToRefreshBox`.

## Equivalencias rapidas

| XML / View | Compose |
|---|---|
| RecyclerView + Adapter | `LazyColumn { items(...) { EventCard(...) } }` |
| GridLayoutManager(7) | `LazyVerticalGrid(GridCells.Fixed(7))` |
| SwipeRefreshLayout | `PullToRefreshBox(isRefreshing, onRefresh)` |
| ChipGroup (single select) | fila de `FilterChip(selected, onClick, shape = Pill)` |
| TextInputLayout + EditText | `OutlinedTextField(shape = Pill)` |
| MaterialButton | `Button(shape = Pill)` / `OutlinedButton(shape = Pill)` |
| MaterialAlertDialog | `AlertDialog(...)` |
| MaterialDatePicker | `DatePickerDialog` Compose o mantener el de Material en el Fragment via callback |
| Snackbar | `Scaffold(snackbarHost=...)` + `SnackbarHostState.showSnackbar` en `LaunchedEffect(key)` |
| SupportMapFragment / MapView | mantener via `AndroidView` o dejar el Fragment; restilizar el mapa aparte |

## Entregable por pantalla

1. `ui/<feature>/<Feature>Screen.kt` con `@Composable fun <Feature>Screen(state, callbacks...)`.
   Incluir un `@Preview` con un estado de ejemplo si es barato.
2. Reescribir `ui/<feature>/<Feature>Fragment.kt` para montar la pantalla via `purpuraComposeView`.
3. Borrar el uso de ViewBinding del Fragment (el layout XML puede quedarse sin usar; no lo borres).
4. Reportar: strings nuevos necesarios (clave + texto) y cualquier desviacion.
