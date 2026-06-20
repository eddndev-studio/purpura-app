# Assets graficos requeridos por Play Store

Lista de los recursos visuales que Play exige para la ficha. Son archivos de imagen que hay que
producir (no codigo). El icono adaptativo de la app YA existe en el repo (mipmap ic_launcher, fondo
#5D007F); de ahi se puede derivar el icono de 512.

| Asset | Especificacion | Estado |
|---|---|---|
| Icono de la app (Play) | 512 x 512 px, PNG 32-bit, < 1 MB | Derivar del icono adaptativo existente |
| Grafico destacado | 1024 x 500 px, PNG/JPG | Por crear (fondo morado + logo + tagline) |
| Capturas telefono | 2-8 imagenes, 16:9 o 9:16, lado 320-3840 px | Por capturar en device/emulador |
| Captura tablet (opcional) | 7" y 10" si se ofrece soporte tablet | Opcional |

## Capturas sugeridas (cuando haya emulador/device)

1. Inicio con la lista de eventos (muestra la card morada).
2. Crear evento (formulario con chips de tipo/estatus pill).
3. Calendario mensual con dias marcados.
4. Mapa de calor del mes.
5. Detalle de evento con el mapa.
6. Modo oscuro (cualquiera de las anteriores) para lucir la noche "high-end".

Sugerencia: enmarcar las capturas con un fondo morado y un titulo corto por imagen para una ficha
mas profesional (se puede hacer con la skill design-taste-frontend o una plantilla simple).

> NOTA: estas capturas requieren correr la app (emulador o device). Quedan como tarea de la fase de
> verificacion visual; no se pueden generar desde el codigo.
