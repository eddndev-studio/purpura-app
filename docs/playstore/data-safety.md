# Formulario de Seguridad de los Datos (Data Safety) - Purpura

Respuestas sugeridas para Play Console > Politica > Seguridad de los datos. Revisar contra el
comportamiento real antes de enviar (Google audita y puede sancionar respuestas inexactas).

## Resumen

- ¿La app recopila o comparte datos de usuario? **Si, recopila. No comparte con terceros.**
- ¿Los datos se cifran en transito? **Si (HTTPS).**
- ¿El usuario puede pedir que se eliminen sus datos? **Si.** Dos vias, ambas eliminan la cuenta y
  TODOS sus datos del servidor de forma permanente:
  1. **Dentro de la app** (IMPLEMENTADO): Cuenta > "Eliminar cuenta" > confirmacion > `DELETE
     /api/v1/account`. El backend borra el usuario y, por `ON DELETE CASCADE`, sus eventos y
     credencial; la app limpia la sesion y el cache local.
  2. **Sin la app** (URL web): indicar en el formulario la URL publica de la politica de privacidad
     como pagina de solicitud de eliminacion (seccion "Conservacion y eliminacion de la cuenta").
  NOTA para PRODUCCION: Play exige AMBAS. La via in-app ya existe; falta unicamente HOSPEDAR la
  politica en una URL publica y declararla en la seccion "Eliminacion de datos" de la Consola.

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
