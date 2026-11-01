# AriseUp 1.0.0

Primera versión del Sistema.

## Novedades

- **Misión diaria** "Preparación para volverse fuerte": flexiones, abdominales, sentadillas y correr. Se calibra con un test inicial y escala con tu nivel hasta 100/100/100/10 km.
- **Niveles, rangos (E → S) y stats**: Fuerza, Agilidad, Vitalidad, Sentido e Inteligencia, más puntos libres al subir de nivel y una reevaluación cada 4 semanas.
- **Zona de Penalización**: si fallas una misión obligatoria, hay un entreno de castigo. Si lo ignoras, pierdes XP, stats y, desde el tercer día, niveles.
- **Rutinas personalizadas**, **misiones urgentes** y **misiones ocultas** con títulos.
- **Nutrición**:
  - escáner de códigos de barras;
  - Open Food Facts;
  - unos 390 alimentos genéricos en español;
  - recetas;
  - reglas de dieta;
  - objetivos calculados según tu perfil.
- **Silueta del Jugador**, que cambia con tu peso y tu fuerza.
- **Estado de descanso** para vacaciones, enfermedad o lesión.
- **Health Connect**: importa tus carreras.
- **Copia de seguridad** en JSON.

## Instalación

1. Descarga `app-release.apk` en tu Android (8.0 o superior).
2. Permite instalar apps de origen desconocido para tu navegador o gestor de archivos.
3. Abre el APK e instala.

## Permisos

| Permiso | Para qué |
| --- | --- |
| Notificaciones | Avisos del Sistema: misiones, plazos y penalizaciones |
| Cámara | Escanear códigos de barras |
| Health Connect (lectura de ejercicio y distancia) | Importar carreras (opcional) |
| Internet | Solo para buscar en Open Food Facts |

## Cómo se firma una versión

1. La clave está fuera del repositorio, en `%USERPROFILE%\.ariseup\ariseup-release.jks`, y sus contraseñas en `keystore.properties` (ignorado por git).
2. Genera el APK firmado:

   ```bash
   ./gradlew assembleRelease
   ```

3. Publícalo:

   ```bash
   gh release create vX.Y.Z app/build/outputs/apk/release/app-release.apk --title "AriseUp X.Y.Z" --notes-file docs/release.md
   ```

**Guarda una copia de la clave y de `keystore.properties`.** Sin ellas no podrás publicar actualizaciones que se instalen encima de esta versión.
