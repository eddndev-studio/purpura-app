# Formulario de Seguridad de los Datos (Data Safety) - Purpura

Respuestas sugeridas para Play Console > Politica > Seguridad de los datos. Revisar contra el
comportamiento real antes de enviar (Google audita y puede sancionar respuestas inexactas).

## Resumen

- ¿La app recopila o comparte datos de usuario? **Si, recopila. No comparte con terceros.**
- ¿Los datos se cifran en transito? **Si (HTTPS).**
- ¿El usuario puede pedir que se eliminen sus datos? **Si.** En el formulario, indicar la URL publica
  de la politica de privacidad como pagina de solicitud de eliminacion de cuenta (seccion
  "Conservacion y eliminacion de la cuenta"). NOTA (pendiente de codigo): para PRODUCCION, Play exige
  ademas una accion de "Eliminar cuenta" DENTRO de la app; ver el endpoint DELETE /account y la
  pantalla pendientes en el roadmap. Para pruebas internas basta la URL de solicitud.

## Tipos de datos recopilados

| Tipo de dato | Recopilado | Compartido | Proposito | Obligatorio |
|---|---|---|---|---|
| Direccion de correo | Si | No | Funcionalidad de la app (cuenta) | Si |
| ID de usuario | Si | No | Funcionalidad de la app (cuenta) | Si |
| Nombre (registro/contacto) | Si | No | Funcionalidad de la app | Opcional |
| Contactos (nombre/telefono del contacto elegido) | Si | No | Funcionalidad de la app (adjuntar contacto a evento) | Opcional |
| Ubicacion aproximada/precisa elegida en el mapa | Si | No | Funcionalidad de la app (ubicacion del evento) | Opcional |
| Otros datos del usuario (contenido de eventos: descripcion, fechas, tipo, estatus) | Si | No | Funcionalidad de la app | Si |

Notas:
- La "ubicacion" aqui es el punto que el usuario ELIGE para un evento, NO la ubicacion en tiempo real
  del dispositivo. La app no pide el permiso de ubicacion del sistema.
- El respaldo en Google Drive usa alcance `drive.file` (solo archivos creados por la app).

## Practicas de seguridad

- Cifrado en transito: Si.
- Mecanismo de eliminacion de datos: Si, el usuario puede solicitar la eliminacion (correo de
  soporte) y al cerrar sesion se borran los datos locales.

## Pendiente de confirmar antes de enviar

- Si se considera que el contenido de eventos incluye datos sensibles, ajustar categorias.
- Confirmar la URL de la politica de privacidad (obligatoria en este formulario).
