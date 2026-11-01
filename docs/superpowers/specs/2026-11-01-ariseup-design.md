# AriseUp: especificación de diseño

Fecha: 2026-11-01 · Estado: aprobada (2026-11-01)

## 1. Qué es

AriseUp es una app **Android** que simula el **Sistema** de *Solo Leveling*: tú eres el Jugador y el Sistema te asigna misiones de **ejercicio** y **dieta**. Cumplirlas te da XP, niveles, rangos y stats. Fallarlas te manda a la **Zona de Penalización**, donde puedes perder XP, stats e incluso niveles.

- Solo ejercicio y dieta. Los hábitos generales ya los cubre FarmHabit.
- Uso personal, **local-first**: los datos solo viven en el móvil, sin servidor ni cuentas.
- Interfaz y documentación en español; identificadores de código en inglés.
- Repositorio **público** en `github.com/AdellEdgar/AriseUp`, con licencia MIT.
- Proyecto de fan, sin afiliación con *Solo Leveling* ni sus titulares. No usa imágenes, sonidos, personajes ni nombres de la obra (ni "Solo Leveling" ni "Sung Jin-Woo" en la interfaz). Sí usa la estética genérica de "ventanas del Sistema".
- No es consejo médico: el primer arranque lo indica y recomienda consultar a un profesional antes de empezar.

**Criterio de éxito:** la usas a diario durante semanas porque registrar es rápido, los avisos llegan a tiempo y la amenaza de la penalización se siente real.

## 2. Núcleo del juego

### 2.1 Nivel, XP y rango

- XP para pasar del nivel `n` al `n+1`: `xpToNext(n) = round(100 · n^1.5)`.
- **XP de un día perfecto** a nivel `n`: `dayPool(n) = round(100 · n^0.9)`. Se reparte entre las misiones del día:

  | Misión | Parte del día perfecto |
  |---|---|
  | Diaria | 45 % |
  | Rutinas del día | 20 % (dividido entre las rutinas de ese día) |
  | Nutrición | 25 % |
  | Reglas de dieta | 10 % (dividido entre las reglas) |

  Si un día no hay rutinas o reglas, esa parte simplemente no se gana.
- **Ritmo resultante con días perfectos:** rango D en unas 3 semanas, C en unos 3,5 meses, B en unos 9 meses, A en unos 18 meses y S en unos 2,7 años. Todas las constantes de equilibrio viven en un único objeto `Balance` para poder ajustarlas.
- **Rangos:**

  | Rango | Niveles |
  |---|---|
  | E | 1–9 |
  | D | 10–24 |
  | C | 25–44 |
  | B | 45–69 |
  | A | 70–99 |
  | S | 100+ |

  Subir o bajar de rango lanza una ventana especial.
- El nivel mínimo es 1. La XP dentro del nivel nunca baja de 0.

### 2.2 Stats

Hay cinco: **FUE** (Fuerza), **AGI** (Agilidad), **VIT** (Vitalidad), **SEN** (Sentido) e **INT** (Inteligencia). Internamente se guardan con decimales; en pantalla se muestran como enteros. El valor mínimo es 1.

**Valor inicial:** 10 en todas. El test inicial no da bonus: solo fija la referencia para la primera reevaluación.

**Ganancia por actividad:**

| Acción | Ganancia |
|---|---|
| Misión diaria completada | FUE +0,3, AGI +0,3, VIT +0,1 |
| Rutina completada | +0,3 en su stat (cada rutina tiene una categoría: fuerza → FUE, cardio → AGI, movilidad → SEN) |
| Misión de nutrición cumplida | VIT +0,3, INT +0,1 |
| Regla de dieta cumplida | INT +0,05 |
| Día con al menos 3 comidas registradas | INT +0,1 |
| Cada 7 días de racha | VIT +0,5 |
| Subida de nivel | 3 puntos libres para repartir |

**Reevaluación cada 4 semanas.** Por cada stat, el bonus o la pérdida es `round(Δ% / 5)`, limitado a ±5. `Δ%` es el cambio respecto a la evaluación anterior:
- **FUE:** media del cambio en máximo de flexiones y máximo de sentadillas.
- **AGI:** distancia del test de Cooper (12 minutos corriendo).
- **SEN:** test de flexibilidad sentado (*sit-and-reach*, en cm).
- **VIT** (regla propia, no `Δ%/5`), según el objetivo:
  - **perder grasa:** +1 por cada 1 % de peso perdido y −1 por cada 1 % ganado;
  - **ganar músculo:** +1 por cada 0,5 % ganado (hasta un 2 % al mes; por encima no suma más) y −1 por cada 0,5 % perdido;
  - **mantener:** +2 si el peso está dentro de ±1,5 %, 0 entre 1,5 % y 3 %, y −2 si se aleja más del 3 %;
  - **% de grasa** (si se registra): además, +1 por cada punto bajado y −1 por cada punto subido;
  - total limitado a ±5.
- **INT** (regla propia), según el porcentaje `p` de días con registro completo en las 4 semanas:
  - si `p > 80`: `+round((p − 80) / 4)`;
  - si `p < 50`: `−round((50 − p) / 10)`;
  - entre 50 y 80: 0.

La reevaluación aparece como misión durante 3 días. Si no la haces, no hay penalización, pero tampoco bonus, y se vuelve a proponer.

### 2.3 Misión diaria: "Preparación para volverse fuerte"

- Cuatro ejercicios: **flexiones**, **abdominales**, **sentadillas** y **correr (km)**.
- **Objetivo inicial** `T0`: el 50 % del máximo de cada ejercicio en el test (mínimo 5 repeticiones). Para correr, el 50 % de la distancia del test de Cooper, redondeado a 0,5 km (mínimo 1 km).
- **Escalado con el nivel:** `T(n) = T0 + (meta − T0) · min(1, (n − 1) / 44)`, donde `meta = max(100, T0)` repeticiones o `max(10, T0)` km. Así se alcanza la meta en el nivel 45 (rango B). A partir de ahí suma +1 repetición y +0,1 km cada 2 niveles.
- **Ajustes de `T0`:** una reevaluación lo sube si mejoró tu máximo. Nunca baja por reevaluación; sí baja de forma natural si bajas de nivel.
- Se puede hacer **por partes** a lo largo del día, sumando repeticiones o km.
- **Solo cuenta si se completa al 100 %.**
- **Reinicio diario** a las 00:00 por defecto (configurable).

### 2.4 Rutinas personalizadas

- **Datos de una rutina:** nombre, días de la semana, categoría (fuerza, cardio o movilidad), si es obligatoria u opcional, y una lista de ejercicios.
- **Ejercicios:** la app trae una biblioteca de unos 40 habituales y puedes añadir los tuyos. Cada ejercicio tiene un tipo:
  - repeticiones,
  - repeticiones + peso,
  - tiempo,
  - distancia.
- Cada ejercicio de la rutina lleva un número de series y un objetivo por serie.
- **Se completa** cuando todas las series registradas alcanzan su objetivo.
- **Si se falla:** una rutina obligatoria activa la penalización; una opcional solo pierde su recompensa.
- **Temporizador de descanso** entre series, configurable.

### 2.5 Misiones del Sistema

**Urgentes**
- **Cuándo aparecen:** en cada reinicio hay un 20 % de probabilidad de que el día tenga una, con un máximo de 2 por semana. Nunca en Estado de descanso ni en la Zona de Penalización.
- Aparecen a una hora aleatoria entre las 10:00 y las 19:00 y tienen un plazo de 3 a 6 horas.
- **Contenido:** un ejercicio de la misión diaria, con una cantidad entre el 30 % y el 60 % de su objetivo diario. Es adicional a la diaria.
- **Recompensa:** el 50 % de `dayPool(n)`, más FUE o AGI +0,3.
- **Si se falla:** −10 % de `xpToNext(n)`. **No** activa la penalización.

**Ocultas (logros)**
- Solo se revelan al conseguirlas y algunas dan un **título**. Lista inicial:
  - primer día completado;
  - 7, 30, 100 y 365 días de racha;
  - superar una penalización;
  - superar una penalización en su primer día;
  - primera subida de rango y cada rango alcanzado;
  - 1.000 flexiones acumuladas (y 10.000);
  - 100 km corridos acumulados;
  - 30 días con nutrición cumplida;
  - primera receta creada;
  - mejora en todas las stats en una misma reevaluación;
  - completar una urgente en menos de una hora.
- Los títulos se eligen en la ficha de estado.

### 2.6 Zona de Penalización

- **Se activa** si al cerrar un día queda **sin cumplir** alguna misión **obligatoria**: la diaria, una rutina obligatoria, la nutrición (si está activa) o una regla obligatoria. Varias misiones fallidas el mismo día generan **una sola** penalización.
- **Misión de penalización:** el 150 % de los objetivos de la misión diaria vigente. Es física aunque lo fallado fuera la dieta. Se puede hacer por partes.
- **Mientras está activa:**
  - la interfaz se vuelve **roja** y muestra una cuenta atrás hasta el siguiente cierre;
  - la racha queda a 0;
  - siguen apareciendo las misiones normales de cada día, que se pueden cumplir y dan recompensa. Si se fallan, no se crea una segunda penalización.
- **Degradación**, aplicada en cada cierre de día mientras la penalización siga sin cumplirse:
  - **cierres 1 y 2:** −20 % de `xpToNext(n)` (la XP no baja de 0) y −1 en cada stat;
  - **cierre 3 y siguientes:** lo anterior, y además **−1 nivel por cierre**. Al bajar, la XP queda a 0 dentro del nuevo nivel. Los puntos libres ya gastados no se devuelven y los no gastados se conservan.
- **Al cumplirla** termina la degradación, pero lo perdido no se recupera. Puede desbloquear misiones ocultas.

### 2.7 Test inicial y reevaluación

- **Contenido del test:**
  - máximo de flexiones;
  - máximo de abdominales;
  - máximo de sentadillas;
  - test de Cooper (distancia en 12 minutos);
  - *sit-and-reach* en cm;
  - peso, y opcionalmente cintura y % de grasa.
- Se puede rellenar por partes en varios días. La misión diaria empieza cuando están los cuatro datos de ejercicio.
- La reevaluación usa el mismo formulario.

### 2.8 Racha

- Cuenta los días consecutivos con **todas** las misiones obligatorias cumplidas.
- Los días en Estado de descanso la congelan: ni la suman ni la rompen.
- Una penalización la pone a 0.

### 2.9 Estado de descanso

- **Motivos:** vacaciones, enfermedad o lesión. Se indican fecha de inicio y fecha de fin (o sin fin).
- **Efectos:** no hay misiones obligatorias, penalización, degradación ni urgentes, y la racha se congela. Entrenar y registrar comida sigue dando XP y stats como misiones opcionales.
- **Restricciones:**
  - El descanso empieza **hoy como pronto**, nunca con efecto retroactivo. Como el día actual aún no se ha cerrado, activarlo hoy cubre hoy.
  - No se puede activar estando en la Zona de Penalización.
  - Por **vacaciones**, máximo **30 días por año natural**.
  - Por **enfermedad o lesión** no hay límite, pero cada 7 días el Sistema pide confirmarlo. Si no se confirma en 24 h, el descanso termina en el siguiente cierre.
- **Al volver:**
  - durante 7 días, la misión diaria baja un 30 %;
  - si el descanso duró más de 21 días, se propone una reevaluación.

## 3. Dieta

### 3.1 Perfil y objetivos

- **Datos del perfil:** sexo, fecha de nacimiento, altura, peso, nivel de actividad (5 niveles con su factor) y objetivo (perder grasa, mantener o ganar músculo).
- **Gasto diario estimado:** la fórmula de Mifflin-St Jeor da el gasto en reposo, que se multiplica por el factor de actividad.
- **Calorías:** el gasto diario × 0,8 para perder grasa, × 1,0 para mantener o × 1,1 para ganar músculo.
- **Proteína:** 2,0 g/kg para perder o ganar; 1,6 g/kg para mantener.
- **Grasa:** el 27 % de las calorías.
- **Hidratos:** el resto.
- Todos los valores son editables a mano; si se editan, dejan de recalcularse solos. Se recalculan con cada peso nuevo, salvo los editados.

### 3.2 Registro de comidas

- **Comidas del día:** desayuno, comida, merienda, cena y snacks.
- **Datos de cada alimento (por 100 g o ml):** kcal, proteína, hidratos, azúcares, grasa, saturadas, fibra y sal.
- **Datos opcionales:** tamaño de ración, peso por unidad, marca y código de barras.
- **Origen de los alimentos:**
  - **GENERIC:** la tabla genérica.
  - **OFF:** Open Food Facts, guardado en local tras consultarlo.
  - **CUSTOM:** creado a mano.
- **Búsqueda:**
  - primero en local (recientes, propios, recetas y tabla genérica), que funciona sin conexión;
  - después, si pulsas "Buscar en internet", en Open Food Facts. Se lanza solo al pulsar y no mientras escribes, para respetar su límite de peticiones.
- **Escáner de códigos:** CameraX + ML Kit. Busca primero en local y luego en Open Food Facts. Si no lo encuentra, abre "Crear alimento" con el código ya rellenado.
- **Cantidad:** en g o ml, en raciones o en unidades. Las macros se calculan al momento.
- **Recetas:** ingredientes con cantidades y número de raciones; las macros por ración se calculan solas.
- **Otras funciones:** copiar una comida de otro día y lista de favoritos.

### 3.3 Misiones de dieta

- **Nutrición** (obligatoria por defecto; se puede desactivar o hacer opcional). Se cumple si:
  - las kcal están dentro de **±10 %** del objetivo,
  - la proteína llega al **≥90 %**,
  - y hay **≥3 comidas** con algo registrado.
- **Reglas de dieta:** las define el usuario y pueden ser de dos tipos:
  - **casilla:** "sin ultraprocesados";
  - **contador con objetivo:** "agua: 8 vasos".

  Cada regla es obligatoria u opcional.
- **Margen de registro:** la dieta del día D se puede editar hasta 12 h después del reinicio (las 12:00 por defecto). Por eso las misiones de dieta del día D se evalúan en ese momento y no en el cierre general. Si falla alguna obligatoria, se activa la penalización (o se suma a la ya activa sin duplicarla).

### 3.4 Peso y medidas

- Registro de peso y, opcionalmente, cintura y % de grasa, con gráfica.
- Alimenta el recálculo de objetivos, la reevaluación de VIT y la silueta del Jugador (§4.5).
- **Recordatorio de pesaje:**
  - se configura en Ajustes: cada día, cada 2 días o desactivado (por defecto, cada día a las 08:00);
  - no se envía si ya hay un peso registrado ese día;
  - los días que toca aparece la misión **opcional** *"[Registro de condición física]"*, que da el 3 % de `dayPool(n)` y VIT +0,05;
  - no penaliza.

## 4. Interfaz

### 4.1 Estética

- **Colores:** fondo casi negro (`#05070D`) con tonos azul marino. Las **ventanas del Sistema** son paneles semitransparentes con borde cian (`#3FD8FF`) que resplandece, esquinas marcadas y un encabezado entre corchetes.
- **Tipografía:** Rajdhani para títulos e Inter para el texto. Se cargan con las fuentes descargables de Google (sin incluir archivos); si fallan, se usa la fuente del sistema.
- **Animaciones:**
  - apertura de ventana con barrido de escaneo;
  - texto que aparece letra a letra en los mensajes del Sistema;
  - subida de nivel o de rango con destello.
- **Sonido y vibración:** sonidos **originales** generados por código (no hay archivos de terceros), que se pueden desactivar, y vibración en los avisos importantes.
- **Penalización:** con la Zona de Penalización activa, todo el tema pasa a rojo (`#FF3B3B`).

### 4.2 Primer arranque

1. Aviso de no ser consejo médico.
2. *"[Has cumplido los requisitos para convertirte en Jugador. ¿Aceptas?]"* El "No" vuelve a mostrar la pregunta.
3. Nombre de jugador, perfil físico y objetivo.
4. Test inicial.
5. Primera ficha de estado.

### 4.3 Pantallas

Barra inferior con cuatro pestañas:

1. **Estado:**
   - la silueta del Jugador (§4.5), en el centro;
   - nombre, título, nivel, rango y barra de XP;
   - las 5 stats y el reparto de puntos libres;
   - racha;
   - aviso de penalización o de descanso.
2. **Misiones:**
   - las de hoy (diaria, rutinas, nutrición, reglas, urgentes y reevaluación), y la de penalización si existe;
   - registro de repeticiones, series, peso, tiempo y distancia, con temporizador de descanso;
   - el gestor de rutinas se abre desde aquí.
3. **Nutrición:**
   - el día con sus comidas y las barras de kcal y macros;
   - botones de escanear y buscar;
   - recetas, alimentos propios y reglas de dieta.
4. **Progreso:**
   - gráficas de peso, stats y marcas de los tests;
   - calendario e historial (cumplido, fallado, descanso o penalización);
   - registro de eventos del Sistema;
   - títulos y misiones ocultas.

**Ajustes**, desde el icono de la ficha:
- perfil y objetivos;
- hora de reinicio;
- Estado de descanso;
- notificaciones, sonido y vibración;
- Health Connect;
- exportar e importar copia de seguridad;
- acerca de.

### 4.4 Notificaciones

| Aviso | Cuándo |
|---|---|
| Misión diaria disponible | En el reinicio, o a la hora elegida |
| Plazo cerca | 2 h antes del cierre con obligatorias pendientes |
| Misión urgente | Cuando aparece |
| Penalización | Cuando se activa |
| Degradación | Cuando se aplica, y si has bajado de nivel |
| Subida de nivel o de rango | Al ocurrir |
| Reevaluación disponible | Al tocar |
| Confirmar descanso por enfermedad | Cada 7 días |
| Pesaje | A la hora elegida, cada día o cada 2 días, si aún no hay peso de hoy |

Usan WorkManager con horarios aproximados (unos minutos de margen). No se usan alarmas exactas.

### 4.5 La silueta del Jugador

En el centro de la pestaña **Estado** hay una figura humana de **sombra**: relleno oscuro con un contorno cian que resplandece. Representa tu cuerpo real y cambia con tus datos.

**Parámetros de forma**

Los calcula la función pura `BodyShapeModel` en `:domain`; todos los valores van de 0 a 1:

- **Grasa:**
  - si hay % de grasa registrado, se usa ese dato;
  - si no, se estima con la fórmula de Deurenberg: `%grasa = 1,2·IMC + 0,23·edad − 10,8·(1 si hombre) − 5,4`;
  - se convierte a la escala 0–1 entre 8 % y 40 % (hombre) o entre 15 % y 48 % (mujer), recortando a los límites.
- **Músculo:** `clamp((FUE − 5) / 95, 0, 1)`.
- **Altura:** de 150 a 200 cm, se traduce en una escala vertical de 0,92 a 1,08.
- **Sexo:** elige las proporciones base (hombros, cadera y cintura).

**Dibujo**
- Se hace en un `Canvas` de Compose (componente `ShadowAvatar`): vista frontal, sin rostro.
- **Contorno:** curvas Bézier con unos 24 puntos de control. Cada punto se interpola entre cuatro formas base (delgado, con grasa, musculado y musculado con grasa) según la grasa y el músculo.
- **Acabado:** relleno con degradado oscuro, contorno con brillo (desenfoque) y, alrededor, un aura de partículas cuya intensidad crece con el rango.
- **Animación:**
  - un peso nuevo o un cambio de FUE hace que la figura se transforme suavemente (unos 1,2 s);
  - en reposo hay una leve "respiración".

**Estados visuales**

| Estado | Aspecto |
|---|---|
| Normal | Cian |
| Zona de Penalización | Rojo, postura ligeramente encorvada y parpadeo del contorno |
| Estado de descanso | Tono atenuado |
| Subida de rango | Destello y aura intensa durante la animación |

**Detalle:** al tocar la figura se abre una ventana con:
- peso actual, IMC y % de grasa (estimado o registrado);
- variación desde el primer registro;
- una comparación con la silueta del primer día superpuesta como "fantasma".

## 5. Arquitectura

### 5.1 Módulos

- **`:domain`**: Kotlin/JVM puro, sin dependencias de Android. Contiene los modelos y **todas las reglas** como funciones puras, con `Balance` reuniendo las constantes:
  - `Leveling`: curva, rangos y reparto del día perfecto;
  - `DailyQuestScaling`;
  - `Stats` y `Reevaluation`;
  - `Penalty` y su degradación;
  - `RestMode`;
  - `Nutrition`: Mifflin-St Jeor, objetivos y evaluación de la misión;
  - `UrgentQuestGenerator`, que recibe la fuente de aleatoriedad;
  - `Achievements`;
  - `BodyShapeModel`: parámetros de la silueta;
  - `DayCloser`: dado el estado, el día y la hora actual, devuelve el nuevo estado y la lista de eventos.
- **`:app`**: Android con Jetpack Compose y Material 3 con el tema propio. Sus paquetes:
  - `data`: Room (entidades, DAOs y migraciones), repositorios y DataStore;
  - `network`: cliente de Open Food Facts;
  - `health`: Health Connect;
  - `scan`: CameraX + ML Kit;
  - `system`: trabajos de WorkManager, notificaciones y orquestación del cierre;
  - `ui`: tema, componente `SystemWindow` y pantallas con sus ViewModels;
  - `di`: `AppContainer` manual.

### 5.2 Cierre de día (pieza crítica)

- El `DayCloseCoordinator` busca el último día cerrado y **cierra en orden cada día pendiente** hasta ayer, aplicando `DayCloser` de forma pura. Persiste estado y eventos en una única transacción por día.
- **Es idempotente:** cada día tiene un registro con su estado (abierto, cerrado sin dieta o cerrado del todo), así que ejecutarlo dos veces no aplica nada dos veces.
- **Se ejecuta en tres momentos:**
  - un trabajo único programado al reinicio diario;
  - un trabajo periódico de respaldo cada 6 h;
  - al abrir la app.
- La evaluación de dieta del día D se hace igual, al cumplirse su margen.
- **Antes de cerrar un día** se sincroniza Health Connect: las carreras que empezaron en ese día cuentan para él.
- **Tiempo:** el reloj (`Clock`) se inyecta en todo. Los días se calculan con la hora de reinicio y la zona horaria del dispositivo.

### 5.3 Datos

- **Room:** perfil, estado del jugador, stats, días, misiones con su progreso, rutinas, ejercicios, alimentos, recetas, registros de comida, reglas, peso y medidas, tests, descansos, logros y eventos.
- **DataStore:** preferencias.
- **Tabla genérica:** unos 500 alimentos básicos (frutas, verduras, carnes, pescados, cereales, legumbres, lácteos, huevos, frutos secos, aceites...).
  - **Fuente:** FoodData Central del USDA (SR Legacy, dominio público), con nombres traducidos al español.
  - **Generación:** un script de `tools/` crea la base precargada (`assets/generic_foods.db`). El script y la lista de alimentos elegidos se versionan.
- **Copia de seguridad:** JSON con versión de esquema, con exportar e importar mediante el selector de archivos del sistema. Importar sustituye todos los datos tras confirmarlo.

### 5.4 Integraciones

- **Open Food Facts:**
  - consulta por código: `GET https://world.openfoodfacts.org/api/v2/product/{code}`;
  - búsqueda por texto con su API de búsqueda, priorizando productos de España;
  - cliente Ktor + kotlinx.serialization, con `User-Agent: AriseUp/<versión> (https://github.com/AdellEdgar/AriseUp)`.
- **Health Connect:**
  - lee `ExerciseSessionRecord` (carrera) y `DistanceRecord` del intervalo de cada sesión;
  - permisos de lectura de ejercicio y distancia, que se piden desde Ajustes;
  - las carreras importadas se registran como progreso del ejercicio "correr" y no se duplican (se guarda el identificador de cada registro).
- **ML Kit:** escáner de códigos con el modelo incluido en la app, que funciona sin conexión.

### 5.5 Plataforma y build

- **Versiones de Android:** `minSdk` 26; `compileSdk` y `targetSdk` 37.
- **Build:** Gradle con Kotlin DSL y *version catalog*.
- **Paquete:** `io.github.adelledgar.ariseup`.
- **CI:** GitHub Actions compila y pasa los tests de `:domain` y `:app` en cada push.
- **Versiones publicadas:**
  - APK de *release* firmado con una clave local (`keystore.properties` y el `.jks` están en `.gitignore`), subido a GitHub Releases con `gh`;
  - el usuario debe guardar una copia de la clave.

### 5.6 Errores y casos límite

- **Sin red:** la búsqueda en Open Food Facts muestra "[Sin conexión con el servidor]" y todo lo local sigue funcionando.
- **Producto con datos incompletos:** se guarda igual; los campos que faltan cuentan como 0 y el alimento queda marcado como incompleto para editarlo.
- **Health Connect no instalado o sin permisos:** la importación queda desactivada con un aviso; el registro a mano sigue disponible.
- **Cambios de hora o de zona horaria:** los días se basan en la fecha local del momento del cierre. Si la fecha retrocede, no se reabren días cerrados.
- **Notificaciones denegadas (Android 13+):** se pide permiso en el primer arranque. Si se deniega, aparece un aviso fijo en Ajustes.

## 6. Pruebas

- **`:domain`:** JUnit 5 + Kotest (propiedades). Se prueban la curva y los rangos, el escalado, la degradación con la simulación de N días, el descanso con sus límites, la nutrición, el generador de urgentes (con semilla fija) y la idempotencia de `DayCloser`.
- **`:app`:**
  - Robolectric: DAOs, migraciones, `DayCloseCoordinator` con reloj falso (incluido "la app no se abre en 5 días") y exportar/importar en ida y vuelta;
  - cliente de Open Food Facts con un servidor simulado;
  - tests de Compose de los flujos principales: primer arranque, completar la misión diaria, registrar una comida y entrar y salir de la penalización.
- `./gradlew check` debe pasar antes de cada commit relevante.

## 7. Fuera de alcance (primera versión)

- GPS propio para correr.
- Reconocimiento de comida por foto.
- Sincronización en la nube o varios dispositivos.
- Widgets, Wear OS e iOS.
- Tienda e inventario del Sistema.

## 8. Requiere al usuario

- **Permiso para descargar** el conjunto de datos SR Legacy de FoodData Central (USDA, unos 25 MB, dominio público), que el script de la tabla genérica usa.
- **Crear el repositorio público** `AdellEdgar/AriseUp` en GitHub y hacer el primer push (ya autorizado: "repositorio público").
- **Instalar el APK** en el móvil y conceder permisos (notificaciones, cámara y Health Connect).
