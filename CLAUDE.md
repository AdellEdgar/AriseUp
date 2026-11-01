# AriseUp: guía del proyecto

App Android que simula el **Sistema** de un cazador (inspirada en *Solo Leveling*, proyecto de fan sin afiliación) para **ejercicio y dieta**: misiones, XP, niveles, rangos, stats, Zona de Penalización, registro de comidas y una silueta que refleja el cuerpo del usuario. Local-first: los datos solo viven en el móvil.

- Spec: `docs/superpowers/specs/2026-11-01-ariseup-design.md` (autoridad de diseño).
- Plan: `docs/superpowers/plans/2026-11-01-ariseup.md`.

## Reglas de trabajo

- Claude trabaja de seguido y **hace los commits** (mensaje en español, prefijo convencional, línea `Co-Authored-By`).
- Repositorio **público**: nunca datos personales, claves ni `keystore.properties`.
- Interfaz y documentación en **español**; identificadores en inglés; comentarios en español solo cuando aportan.
- Si algo necesita al usuario (permisos, móvil, cuentas), se para todo y se le espera.
- Antes de cada commit: `./gradlew check` pasa.
- Nada de recursos, nombres ni sonidos de la obra original. Sonidos generados por código.

## Stack

| Pieza | Elección |
| --- | --- |
| Lenguaje | Kotlin (JVM 17) |
| UI | Jetpack Compose + Material 3, Navigation Compose |
| Datos | Room (KSP), DataStore |
| Segundo plano | WorkManager |
| Red | Ktor client + kotlinx.serialization (solo Open Food Facts) |
| Escáner | CameraX + ML Kit Barcode (bundled) |
| Salud | Health Connect (lectura de carreras) |
| Gráficas | Vico |
| Tests | JUnit 5 + Kotest property (`:domain`), JUnit 4 + Robolectric (`:app`) |

## Estructura

- `domain/`: Kotlin puro. **Todas las reglas del juego** como funciones puras; constantes en `Balance.kt`. Sin Android.
- `app/`: Android. `data/` (Room, repos, DataStore, backup), `network/`, `health/`, `scan/`, `system/` (cierre de día, workers, notificaciones), `ui/`, `di/AppContainer.kt`.
- `tools/generic_foods/`: script que genera `app/src/main/assets/generic_foods.json` desde USDA SR Legacy.

## Comandos

- `./gradlew check`: tests y lint.
- `./gradlew assembleDebug`: APK de debug.
- `./gradlew :domain:test`: tests del dominio.
- `./gradlew assembleRelease`: APK firmado (necesita `keystore.properties`; ver `docs/release.md`).
- `py -I tools/generic_foods/test_build.py`: tests del script de alimentos.

## Decisiones clave

- El **cierre de día** (`DayCloseCoordinator`) es idempotente y procesa en orden todos los días pendientes; el reloj (`Clock`) se inyecta en todo.
- Las misiones de ejercicio se recompensan al completarse; las de dieta, al evaluarse (12 h después del reinicio).
