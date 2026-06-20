# Checklist de publicacion en Play Console (pasos SOLO-humanos)

Estos pasos NO se pueden automatizar desde el repo: requieren la cuenta de Play Console del
desarrollador, subir artefactos y completar formularios. El codigo (AAB firmado por tag, SDK al dia,
metadata redactada) ya esta listo del lado del repo.

## 0. Antes de empezar

- [ ] Confirmar si la app YA existe en Play Console (el usuario "cree" que si).
      - Si EXISTE -> es una ACTUALIZACION: revisar el versionCode actual y taggear una version mayor.
      - Si NO existe -> crear la app (idioma por defecto es-MX, gratuita).
- [ ] Tener la cuenta de desarrollador (pago unico de 25 USD ya cubierto si la cuenta existe).

## 1. Artefacto

- [ ] Generar el AAB firmado: taggear `vX.Y.Z` (el workflow Release crea `purpura-vX.Y.Z.aab`) o
      local: `./gradlew :app:bundleRelease` con las credenciales de firma (keystore.properties).
- [ ] Activar **Play App Signing** (recomendado): subir el AAB; Google gestiona la llave de firma de
      la app y el keystore actual queda como **upload key**.
- [ ] Verificar: targetSdk 36 (cumple el floor de Play >= 35). minSdk 26.

## 2. Ficha de Store (ver listing.md)

- [ ] Nombre, descripcion breve y completa (copiar de listing.md).
- [ ] Icono de alta resolucion 512x512 (PNG, ver assets-needed.md).
- [ ] Grafico destacado 1024x500.
- [ ] Capturas de pantalla: minimo 2 de telefono (recomendado 4-8). Ver assets-needed.md.
- [ ] Categoria: Productividad. Datos de contacto: eddndev@gmail.com.

## 3. Politica y cumplimiento

- [ ] **Hospedar la politica de privacidad** (privacy-policy.md) en una URL publica y pegarla en la
      ficha y en Data Safety.
- [ ] Completar **Seguridad de los datos** (ver data-safety.md).
- [ ] **Clasificacion de contenido** (cuestionario IARC).
- [ ] **App de Gobierno / publico objetivo y contenido** (audiencia, no dirigida a ninos).
- [ ] Declaracion de **permisos sensibles**: READ_CONTACTS y USE_EXACT_ALARM pueden pedir
      justificacion. Justificacion: contactos = adjuntar contacto a un evento; alarma exacta = la
      funcion central son recordatorios puntuales.
- [ ] Anuncios: la app NO contiene anuncios -> declararlo.

## 4. Pruebas y lanzamiento

- [ ] QA en device real del flujo completo (login, crear/editar evento, recordatorio, mapa, respaldo
      Drive round-trip) sobre el AAB de release.
- [ ] Crear una version en **pruebas internas**, subir el AAB, validar.
- [ ] Promover a **produccion** (o lanzamiento por fases).

## 5. Post-lanzamiento

- [ ] Revisar Pre-launch report (Google ejecuta la app en dispositivos reales y reporta crashes/a11y).
- [ ] Monitorear Android vitals (ANRs, crashes).
