# Plan de pruebas en dispositivo — Púrpura

**App:** purpura-app · **Build objetivo:** v0.3.1 (versionCode 3001), APK firmado
**Dispositivo de referencia:** Vivo V30 Lite — Android 15 (API 35) · Funtouch/OriginOS
**Backend:** https://api.purpura.eddn.dev (producción, en vivo)
**Fecha de ejecución:** ____________  · **Ejecuta:** ____________

> Este plan verifica **render y flujos en pantalla**, que el CI (verde) NO cubre.
> Cada caso doble-funciona como **captura para el manual de usuario**: nombra la imagen con el ID del caso (ej. `TC-ADD-01.png`).

---

## 0. Pre-requisitos y entorno

Antes de empezar, confirmar TODO esto:

- [x] **Backend vivo:** `curl -s https://api.purpura.eddn.dev/health` → `{"status":"ok",...}` (¡ojo! `/health` va en la raíz, **no** bajo `/api/v1`). *(Riesgo #1 de la demo: si el backend está caído, la app es inservible.)*
- [x] **Build firmado v0.3.1 instalado:** `adb shell dumpsys package com.eddndev.purpura | grep versionName` → `0.3.1`.
- [x] **Cuenta de prueba** lista (Google y/o email+contraseña).
- [x] **Permiso de notificaciones:** se pedirá al guardar el primer evento con recordatorio → conceder.
- [x] **Batería (Vivo, crítico para recordatorios a largo plazo):** Ajustes → Batería → *Consumo en segundo plano alto* → permitir Púrpura; y Ajustes/i-Manager → *Inicio automático* → activar Púrpura.
- [x] **Conectividad** wifi o datos estable.

**Cómo capturar:** botones físicos, o `adb exec-out screencap -p > TC-XX.png`.

**Leyenda de estado:** `[ ]` pendiente · `OK` pasa · `FALLA` (anotar qué) · `BLOQ` bloqueado por feature sin implementar (ver §15).

---

## 1. Camino crítico (SMOKE — si algo aquí falla, parar y arreglar)

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| SMK-01 | Arranque + login | Abrir app sin sesión | Cae en pantalla de Autenticación | `[x]` |
| SMK-02 | Login Google | Continuar con Google | Entra a Inicio con datos del servidor | `OK` (verificado) |
| SMK-03 | Crear evento | AddEvent → llenar → Guardar | Vuelve a Inicio y el evento aparece | `[x]` |
| SMK-04 | Ver detalle | Tap en un evento de Inicio | Abre Detalle con sus datos | `[x]` |
| SMK-05 | Recordatorio | Evento at_time ~2 min → esperar | Llega la notificación | `OK` (verificado) |

---

## 2. Autenticación — REQ-AUTH-001..004

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-AUTH-01 | Login email/contraseña | Ingresar credenciales válidas | Entra a Inicio | `[ ]` |
| TC-AUTH-02 | Login Google | Continuar con Google | Entra a Inicio | `OK` |
| TC-AUTH-03 | Persistencia de sesión | Cerrar app (sin logout) y reabrir | Arranca directo en Inicio, sin pedir login | `OK` |
| TC-AUTH-04 | Arranque sin sesión | Primer arranque o tras logout | Pantalla de Autenticación | `OK` |
| TC-AUTH-05 | Credenciales inválidas | Email/contraseña mal | Mensaje de error claro, sin crash | `[ ]` |
| TC-AUTH-06 | Cerrar sesión (Acerca de) | About → Cerrar sesión | Borra token+cache, regresa a Auth | `BLOQ` (§15) |

## 3. Navegación — REQ-NAV-001 (drawer 7) / REQ-NAV-002 (bottom 3)

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-NAV-01 | Drawer = 7 ítems | Abrir menú hamburguesa | **Exactamente 7** ítems con texto+icono | `BLOQ` (hoy hay 8: sobra Heatmap, §15) |
| TC-NAV-02 | Bottom nav = 3 | Ver barra inferior | Inicio · Consultar · Salir | `OK` |
| TC-NAV-03 | Cada destino navega | Tocar cada entrada del drawer | Abre la pantalla correcta | `OK` |
| TC-NAV-04 | Back/Up | Navegar y volver | Sin pantallas huérfanas ni doble-navegación | `OK` |
| TC-NAV-05 | Chrome en Auth | Estar en Auth | Toolbar/drawer/bottom-nav ocultos | `OK` |

## 4. Inicio

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-HOME-01 | Lista próximos | Abrir Inicio | Eventos de [hoy, hoy+4] ordenados | `OK` |
| TC-HOME-02 | Swipe-refresh | Deslizar hacia abajo | Recarga desde el servidor | `OK` |
| TC-HOME-03 | Tap → Detalle | Tocar un evento | Abre Detalle | `OK` |
| TC-HOME-04 | Estado vacío | Sin eventos próximos | Mensaje de lista vacía (no pantalla en blanco) | `[ ]` |
| TC-HOME-05 | Refresh con error | Modo avión + swipe | Conserva cache + snackbar de aviso | `[ ]` |

## 5. Crear evento (AddEvent) — REQ-ADD-001..009

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-ADD-01 | Alta completa | Llenar todo + Guardar | Vuelve a Inicio, evento visible | `OK` |
| TC-ADD-02 | Chips de tipo | Probar cita/junta/entrega/examen/otros | Se refleja en el detalle | `OK` |
| TC-ADD-03 | Chips de estatus | pendiente/aplazado/realizado | Se guarda el elegido | `OK` |
| TC-ADD-04 | Fecha y hora | Date + Time picker | Hora en zona local (GMT-6) correcta | `OK` |
| TC-ADD-05 | Selector de ubicación | Abrir mapa → tap → confirmar | Guarda lat/lng + etiqueta (geocodificación) | `OK` |
| TC-ADD-06 | Selector de contacto | Tocar campo de contacto | Abre la agenda del sistema | `BLOQ` (§15) |
| TC-ADD-07 | Validación | Dejar descripción/contacto/fecha vacíos | Error por campo, no guarda | `OK` |
| TC-ADD-08 | Recordatorio | Chips None/AtTime/10min/1día | Se guarda el elegido | `OK` |

## 6. Detalle + edición — REQ-QUERY-009..012

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-DET-01 | Ver evento | Abrir Detalle | Todos los campos + mapa lite si hay ubicación | `OK` |
| TC-DET-02 | Cambiar estatus | Chips de estatus | Actualiza y persiste | `OK` |
| TC-DET-03 | Eliminar | Botón eliminar → confirmar | Vuelve y desaparece de las listas | `OK` |
| TC-DET-04 | Editar → refresca | Editar → cambiar hora → Guardar | Detalle muestra los datos nuevos | `OK` |
| TC-DET-05 | Editar preserva | Editar solo la descripción | Contacto/coordenadas/tipo intactos | `OK` |
| TC-DET-06 | Error de carga | Abrir con backend caído | Aviso + botón Reintentar | `[ ]` (pend: requiere modo avión, ver §14) |

## 7. Consultar — REQ-QUERY-001..008

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-QRY-01 | Modo Todos | Chip "Todos" | Lista todos los eventos | `OK` |
| TC-QRY-02 | Modo Día | Chip "Día" + fecha | Filtra a ese día | `OK` |
| TC-QRY-03 | Modo Rango | Chip "Rango" + desde/hasta | Filtra al rango | `OK` |
| TC-QRY-04 | Modo Mes | Chip "Mes" + elegir mes | Filtra al mes (picker propio) | `OK` |
| TC-QRY-05 | Modo Año | Chip "Año" + elegir año | Filtra al año | `OK` |
| TC-QRY-06 | Filtro tipo | Chip de tipo | Filtra por tipo | `OK` |
| TC-QRY-07 | Filtro estatus | Chip de estatus | Filtra por estatus | `OK` |
| TC-QRY-08 | Paginación | Scroll hasta el final | Carga más (acumulativo) | `OK` |
| TC-QRY-09 | Tap → Detalle | Tocar un resultado | Abre Detalle | `OK` |
| TC-QRY-10 | Rotación | Girar el teléfono con filtros activos | Conserva filtros y no abre picker huérfano | `OK` |

## 8. Recordatorios — REQ-NOTIF-001

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-NOTIF-01 | A la hora | Evento at_time ~2 min | Notificación al instante del evento | `OK` |
| TC-NOTIF-02 | 10 min antes | Evento +12 min, recordatorio 10min | Notificación ~2 min después de crear | `OK` |
| TC-NOTIF-03 | 1 día antes | Evento +1 día y unos min | Programa para ~ahora (verificar con `dumpsys alarm`) | `OK` |
| TC-NOTIF-04 | Sin recordatorio | Recordatorio "None" | NO programa alarma | `OK` |
| TC-NOTIF-05 | Tap notificación | Tocar la notificación | Abre el **evento** (Detalle) | `BLOQ` (hoy abre Inicio, §15) |
| TC-NOTIF-06 | Realizado cancela | Marcar evento como realizado | Su recordatorio se cancela | `OK` |
| TC-NOTIF-07 | Editar reprograma | Cambiar la hora del evento | Alarma se reprograma a la nueva hora | `OK` |

## 9. Calendario

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-CAL-01 | Render rejilla | Abrir Calendario | 7 columnas, semana inicia en **domingo** (es-MX) | `OK` |
| TC-CAL-02 | Puntos por tipo | Mes con eventos | Hasta 3 puntos de color por día | `OK` |
| TC-CAL-03 | Día → lista | Tocar un día con eventos | Lista inferior + tap → Detalle | `OK` |
| TC-CAL-04 | Cambiar mes | Avanzar/retroceder mes | Recarga los eventos del mes | `OK` |

## 10. Mapa de calor

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-HEAT-01 | Render densidad | Abrir Mapa de calor | Mosaicos teñidos (escala 5 pasos de morado) | `OK` |
| TC-HEAT-02 | Leyenda + total | Ver encabezado | Leyenda y total del mes | `OK` |
| TC-HEAT-03 | Tap conteo | Tocar un mosaico | Muestra el conteo del día | `OK` |
| TC-HEAT-04 | Cambiar mes | Navegar de mes | Recarga | `OK` |
| TC-HEAT-05 | Acceso (REQ-NAV-001) | ¿Desde dónde se llega? | Debe ser desde Inicio/Calendario, **no** del drawer | `BLOQ` (hoy está en drawer, §15) |

## 11. Respaldo (SAF)

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-BAK-01 | Respaldar | Backup → elegir destino (Drive/local) | Crea `purpura-respaldo-AAAA-MM-DD.json` | `OK` |
| TC-BAK-02 | Respaldo vacío | Backup sin eventos | Avisa y NO crea archivo de 0 bytes | `[ ]` (pend: caso vacío) |
| TC-BAK-03 | Archivo válido | Abrir el JSON generado | JSON bien formado (compatible con export del servidor) | `OK` (restore lo releyó) |

## 12. Restaurar (SAF)

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-RST-01 | Restaurar | Restore → elegir un .json válido | Resumen imported/updated/skipped/failed | `OK` |
| TC-RST-02 | Archivo inválido | Elegir un .json ajeno/corrupto | Aviso distinto al de red, sin crash | `[ ]` (pend: caso negativo) |
| TC-RST-03 | Ida y vuelta | Respaldar → borrar evento → restaurar | El evento reaparece | `OK` |

## 13. Acerca de / Mapa

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-ABT-01 | Info de la app | Abrir Acerca de | Muestra datos de la app | `OK` |
| TC-MAP-01 | Mapa en picker | Abrir selector de ubicación | El mapa renderiza (no gris) | `OK` (render verificado) |
| TC-MAP-02 | Mapa en Detalle | Evento con ubicación | Mapa lite centrado en el punto | `OK` |

## 14. Resiliencia / offline — REQ-NFR

| ID | Caso | Pasos | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-OFF-01 | Inicio offline | Modo avión → abrir Inicio | Muestra cache + aviso | `[ ]` |
| TC-OFF-02 | Detalle offline | Modo avión → abrir un evento | **Caveat conocido:** getById es network-first; puede fallar aunque esté en cache | `[ ]` |
| TC-OFF-03 | Recuperación | Quitar modo avión → reintentar | Vuelve a cargar sin reiniciar | `[ ]` |

---

## 15. Casos BLOQ — dependen de FRENTE 1 (features de compliance sin implementar)

Estos casos **fallarán hasta** que se implementen; no son "la app rota", son trabajo pendiente:

| Caso bloqueado | Requisito | Acción de FRENTE 1 |
|---|---|---|
| TC-AUTH-06 (logout en Acerca de) | REQ-AUTH-004 | Cablear `LogoutUseCase` a un botón en `AboutFragment` |
| TC-NAV-01 (drawer = 7) | REQ-NAV-001 | Quitar `nav_heatmap` del drawer (queda en 7) |
| TC-HEAT-05 (acceso al Heatmap) | REQ-NAV-001 | Exponer Heatmap desde Inicio/Calendario |
| TC-ADD-06 (selector de contacto) | REQ-ADD-002 | Intent de contactos + permiso `READ_CONTACTS` |
| TC-NOTIF-05 (tap abre el evento) | calidad NOTIF | Reenviar `eventId` al Intent y hacer deep-link al Detalle |

> Recomendación de secuencia: **FRENTE 1 (estos fixes) → re-tag firmado vX.Y.Z → ejecutar este plan sobre ese build.** Así las capturas y la verificación quedan sobre el artefacto final.

---

## 16. Resumen de resultados

| Suite | Total | OK | FALLA | BLOQ | Pendiente |
|---|---|---|---|---|---|
| 1 Smoke | 5 | 5 | 0 | 0 | 0 |
| 2 Auth | 6 | 3 | 0 | 1 | 2 |
| 3 Navegación | 5 | 4 | 0 | 1 | 0 |
| 4 Inicio | 5 | 3 | 0 | 0 | 2 |
| 5 AddEvent | 8 | 7 | 0 | 1 | 0 |
| 6 Detalle | 6 | 5 | 0 | 0 | 1 |
| 7 Consultar | 10 | 10 | 0 | 0 | 0 |
| 8 Recordatorios | 7 | 6 | 0 | 1 | 0 |
| 9 Calendario | 4 | 4 | 0 | 0 | 0 |
| 10 Heatmap | 5 | 4 | 0 | 1 | 0 |
| 11 Respaldo | 3 | 2 | 0 | 0 | 1 |
| 12 Restaurar | 3 | 2 | 0 | 0 | 1 |
| 13 Acerca/Mapa | 3 | 3 | 0 | 0 | 0 |
| 14 Offline | 3 | 0 | 0 | 0 | 3 |
| **TOTAL** | **73** | **58** | **0** | **5** | **10** |

**Bugs encontrados (anotar aquí):**

| # | Caso | Qué pasó | Severidad | Fix propuesto |
|---|---|---|---|---|
| | | | | |
