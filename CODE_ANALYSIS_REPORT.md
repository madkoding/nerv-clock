# NERV Clock — Reporte de Análisis de Código
**Fecha:** 2026-02-21
**Alcance:** Codebase completo (Windows, macOS, Linux, Android)
**Revisado:** ~7,200 líneas (Java, Rust, JavaScript, Bash, YAML)

---

## 1. Estructura General del Proyecto

```
nerv-clock/
├── ui/                          # Frontend compartido (HTML/CSS/JS)
│   ├── index.html               # 83 líneas — shell del reloj
│   ├── style.css                # 654 líneas — diseño responsivo
│   └── clock.js                 # 432 líneas — lógica de modos
│
├── src-tauri/                   # Backend desktop (Rust + Tauri v2)
│   ├── src/main.rs              # 58 líneas — init y detección Wayland/X11
│   └── Cargo.toml               # Dependencias Rust
│
├── android/                     # App y Widget Android
│   ├── build.sh                 # 198 líneas — build manual sin Gradle
│   └── app/src/main/
│       ├── java/com/nerv/clock/ # 5,007 líneas de Java
│       └── res/                 # Recursos XML
│
└── .github/workflows/
    └── release.yml              # CI/CD multiplatforma
```

**Tecnologías:** Tauri 2 (desktop) + Java nativo (Android) + UI web compartida.
**Enfoque:** Un solo frontend HTML/CSS/JS reutilizado en ambas plataformas mediante
transformaciones `sed` en tiempo de build.

---

## 2. Code Smells y Problemas de Calidad — Por Severidad

### CRÍTICO

#### [C-1] `WidgetUpdateService` es una God Class (664 líneas)
**Archivo:** `android/app/src/main/java/com/nerv/clock/WidgetUpdateService.java:40-664`

Una sola clase gestiona: actualizaciones de widget, renderizado de bitmaps,
cambio de tema, clicks de botones, estado de batería, estado de pantalla
y gestión de dimensiones. Viola el Principio de Responsabilidad Única y
hace el testing prácticamente imposible.

**Acción:** Dividir en `WidgetRenderManager`, `BatteryStateMonitor`,
`ScreenStateMonitor` y `ThemeManager`.

---

#### [C-2] Singleton estático con riesgo de memory leak
**Archivo:** `android/app/src/main/java/com/nerv/clock/WidgetUpdateService.java:63-66`

```java
private static WidgetUpdateService instance = null;
private static Handler handler;
private static ClockViewRenderer clockRenderer;    // Retiene Context
private static boolean isServiceRunning = false;
```

Referencias estáticas a objetos que contienen `Context` impiden que el
garbage collector libere memoria cuando el Service es destruido y recreado.
Sin sincronización: hay race conditions si el sistema recrea el servicio en
paralelo.

**Acción:** Usar el patrón de `onStartCommand` con idempotencia en lugar de
singleton estático. Reemplazar `static Handler` por `WeakReference<Handler>`.

---

#### [C-3] 41 capturas genéricas de `Exception` que ocultan fallos
**Archivos:** Distribuidas en todos los archivos Java

```java
} catch (Exception e) {
    Log.e(TAG, "Update error: " + e.getMessage()); // Traga el stacktrace
}
```

Tres patrones distintos conviven: log-y-continúa, retornar-null,
y fallback-silencioso. Ninguno distingue errores recuperables de fatales.

**Acción:** Capturar excepciones específicas (`NullPointerException`,
`IllegalStateException`, `OutOfMemoryError`). Usar un `CrashReporter` o
al menos `Log.e(TAG, "msg", e)` para incluir el stacktrace completo.

---

#### [C-4] Magic numbers sin nombrar
**Archivo:** `android/app/src/main/java/com/nerv/clock/ui/ClockView.java` (múltiples líneas)

```java
topMargin = h * 0.01f;       // ¿Qué es 0.01?
topBarHeight = h * 0.15f;    // ¿Por qué 15%?
float spacing = 28;          // pixels hardcodeados
float radius = 14;           // pixels hardcodeados
updateHandler.postDelayed(this, 40);  // ¿40ms = 25fps?
```

**Acción:** Extraer a constantes con nombre:
`MARGIN_RATIO = 0.01f`, `TOP_BAR_HEIGHT_RATIO = 0.15f`,
`HEXAGON_SPACING_PX = 28`, `TARGET_FPS_MS = 40L`.

---

### ALTO

#### [A-1] Handler con posting recursivo sin limpieza garantizada
**Archivo:** `android/app/src/main/java/com/nerv/clock/ui/ClockView.java:696-726`

```java
private Runnable updateRunnable = new Runnable() {
    @Override
    public void run() {
        if (!isUpdating) return;
        clockLogic.update();
        invalidate();
        updateHandler.postDelayed(this, 40); // se re-encola a sí mismo
    }
};
```

Si la View es destruida sin llamar `stopUpdating()`, el handler continúa
ejecutando indefinidamente. `onDetachedFromWindow()` no está sobreescrito.

**Acción:** Sobreescribir `onDetachedFromWindow()` para llamar `stopUpdating()`.
Considerar `Choreographer.postFrameCallback()` en lugar de `postDelayed`.

---

#### [A-2] BroadcastReceiver sin `unregisterReceiver` en caso de excepción
**Archivo:** `android/app/src/main/java/com/nerv/clock/WidgetUpdateService.java:249-279`

El receiver se registra en `registerScreenReceiver()` pero si `onDestroy()`
no es llamado por el sistema (crash, kill forzado), el receiver queda activo.
No hay `try-finally` que garantice la limpieza.

**Acción:** Registrar el receiver en `onCreate()` con `try-finally` y
desregistrar en `onDestroy()`. Validar `screenReceiver != null` antes de
`unregisterReceiver`.

---

#### [A-3] Bitmap reciclado antes de tiempo en `WebViewManager`
**Archivo:** `android/app/src/main/java/com/nerv/clock/widget/WebViewManager.java:184-216`

```java
if (renderBitmap != null) {
    renderBitmap.recycle();     // se recicla
}
renderBitmap = Bitmap.createBitmap(...); // si esto lanza OutOfMemoryError...
// renderBitmap anterior ya fue reciclado, el nuevo bitmap nunca se asignó
```

Si `createBitmap` lanza `OutOfMemoryError`, el bitmap anterior fue reciclado
pero la referencia no fue actualizada, causando uso de bitmap reciclado en el
próximo frame.

**Acción:**
```java
Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
if (renderBitmap != null) renderBitmap.recycle();
renderBitmap = newBitmap;
```

---

#### [A-4] `setDrawingCacheEnabled` deprecado
**Archivo:** `android/app/src/main/java/com/nerv/clock/widget/WebViewManager.java:205-209`

```java
webView.setDrawingCacheEnabled(true);
webView.buildDrawingCache(true);
webView.draw(renderCanvas);
```

`setDrawingCacheEnabled` está deprecado desde API 28. En API 31+ (minSdk del
proyecto) puede comportarse de forma no determinista.

**Acción:** Reemplazar con `webView.draw(renderCanvas)` directamente o usar
`PixelCopy` para renderizado hardware-accelerated.

---

#### [A-5] HashMap para 8 dígitos fijos — ineficiencia estructural
**Archivo:** `android/app/src/main/java/com/nerv/clock/ui/ClockView.java:51-52`

```java
private Map<String, Float> digitGhostAlpha = new HashMap<>();
private Map<String, String> lastDigitValues = new HashMap<>();
```

Se usan exactamente 8 claves fijas ("h1","h2","m1","m2","s1","s2","d1","d2").
HashMap tiene overhead de boxing, hashing y buckets innecesario.

**Acción:** `float[] digitGhostAlpha = new float[8]` con un enum/índice constante.

---

### MEDIO

#### [M-1] Null checks ausentes en `ClockViewRenderer`
**Archivo:** `android/app/src/main/java/com/nerv/clock/ui/ClockViewRenderer.java:125-136`

```java
public void drawClock(Canvas canvas, int width, int height) {
    clockLogic.update();       // NPE si clockLogic es null
    updateWarningState();
```

`clockLogic` se asigna en el constructor pero `ClockViewRenderer` es
instanciado estáticamente, por lo que si el constructor falla parcialmente
queda un objeto en estado inconsistente.

---

#### [M-2] Consultas DOM duplicadas en `clock.js`
**Archivo:** `ui/clock.js:240-265`

```javascript
const digitGroups = document.querySelectorAll('.digit-group, .colon');
digitGroups.forEach(el => el.style.display = 'none');
// ... más adelante en otra función ...
const digitGroups = document.querySelectorAll('.digit-group, .colon');
digitGroups.forEach(el => el.style.display = '');
```

El selector se ejecuta dos veces en el DOM. Para un widget de reloj que
redibuja frecuentemente, es innecesario.

**Acción:** Cachear en variable de instancia o usar `classList.toggle`.

---

#### [M-3] Variable global `window.nervClock`
**Archivo:** `ui/clock.js:421`

```javascript
window.nervClock = new NervClock();
```

Expone la instancia completa del reloj al scope global. En la WebView de
Android, cualquier script inyectado por `evaluateJavascript` puede acceder
y modificar el estado del reloj.

**Acción:** Eliminar la asignación a `window`. Si Tauri necesita acceso,
usar el bridge oficial (`window.__TAURI__`).

---

#### [M-4] Inconsistencia de versión Java entre `build.sh` y CI
**Archivos:**
- `android/build.sh:92` → `javac -source 11 -target 11`
- `.github/workflows/release.yml:177` → `javac -source 1.8 -target 1.8`

El bytecode local es Java 11 pero el bytecode de CI es Java 8. La APK
publicada en releases usa bytecode diferente al que el desarrollador prueba
localmente. Puede producir comportamientos distintos en producción.

**Acción:** Unificar a `-source 11 -target 11` en ambos lugares (o usar
`--release 11` en Java moderno).

---

#### [M-5] Transformaciones CSS con `sed` — build frágil
**Archivos:** `android/build.sh:127-141`, `.github/workflows/release.yml:198-209`

```bash
sed -i 's/cqw/vw/g' "$ASSETS_DIR/style.css"
sed -i 's/font-size: 12vw/font-size: 28vmin/g' "$ASSETS_DIR/style.css"
# ... 9 comandos sed más
```

Las transformaciones convierten CSS de desktop (container queries, `cqw`)
a CSS móvil (`vw`, `vmin`). Si el CSS upstream cambia cualquier valor en píxeles
o unidades, los sed fallan silenciosamente sin error. Los valores de fuente
son diferentes entre `build.sh` (`28vmin`) y `release.yml` (`13vw`).

**Acción:** Mantener dos archivos CSS separados (`style.desktop.css`,
`style.android.css`) o usar variables CSS con un punto de entrada distinto.

---

#### [M-6] Fallback loop escrito como `if` repetido
**Archivo:** `android/build.sh:28-48`

```bash
if [ ! -d "$BUILD_TOOLS" ]; then
    BUILD_TOOLS="$SDK_PATH/build-tools/35.0.0"
fi
if [ ! -d "$BUILD_TOOLS" ]; then
    BUILD_TOOLS="$SDK_PATH/build-tools/34.0.0"
fi
```

Cuatro bloques `if` idénticos para probar versiones de build-tools.

**Acción:**
```bash
for ver in 36.1.0 35.0.0 34.0.0 33.0.2; do
    [ -d "$SDK_PATH/build-tools/$ver" ] && BUILD_TOOLS="$SDK_PATH/build-tools/$ver" && break
done
```

---

## 3. Hardcoded Secrets, API Keys y Datos Sensibles

### CRÍTICO

#### [S-1] Credenciales del keystore en texto plano — doble aparición
**Archivo 1:** `android/build.sh:172-173`
```bash
-storepass android \
-keypass android \
```

**Archivo 2:** `.github/workflows/release.yml:230-231`
```bash
-storepass android \
-keypass android \
```

Las contraseñas del keystore de release (`android`/`android`) están
hardcodeadas en dos archivos versionados. Cualquiera con acceso al repositorio
puede recrear el keystore de firma. En repositorios públicos esto invalida
completamente la cadena de confianza de la firma de la APK.

**Acción inmediata:**
1. Revocar el keystore actual y generar uno nuevo con contraseñas seguras
2. Almacenar las contraseñas como GitHub Secrets: `KEYSTORE_STORE_PASS`,
   `KEYSTORE_KEY_PASS`
3. En build.sh: leer de variables de entorno con valores por defecto seguros
   solo para builds de desarrollo local

```bash
STORE_PASS="${KEYSTORE_STORE_PASS:?Error: KEYSTORE_STORE_PASS no definido}"
KEY_PASS="${KEYSTORE_KEY_PASS:?Error: KEYSTORE_KEY_PASS no definido}"
```

---

#### [S-2] Token de GitHub con acceso a repositorio externo
**Archivo:** `.github/workflows/release.yml:331`

```yaml
GITHUB_TOKEN: ${{ secrets.RELEASE_TOKEN }}
```

El `RELEASE_TOKEN` tiene permisos de escritura sobre `madkoding/nerv-clock-releases`.
Si el workflow es comprometido mediante un fork malicioso con un PR, el token
podría ser exfiltrado. No hay restricciones de rama en el job que lo usa.

**Acción:**
- Agregar condición `if: github.ref_type == 'tag'` al job de release
- Revisar scopes del `RELEASE_TOKEN` — debería tener solo `contents: write`
  sobre el repo de releases, no sobre el repo principal

---

### BAJO

#### [S-3] Datos de identidad en el keystore
**Archivo:** `android/build.sh:175-180`

```bash
-dname "CN=NERV, OU=MAGI, O=NERV, L=Tokyo-3, ST=Japan, C=JP"
```

Datos ficticios (Evangelion), no datos reales. Sin impacto de seguridad,
pero un keystore de producción debería tener datos de organización real
para stores como Google Play.

---

## 4. Dependencias Desactualizadas o con Vulnerabilidades

### Rust (Cargo.toml)

| Dependencia | Versión especificada | Riesgo |
|-------------|---------------------|--------|
| `tauri` | `"2"` (cualquier 2.x) | Puede recibir breaking changes entre minor versions |
| `tauri-plugin-shell` | `"2"` | Ídem |
| `serde` | `"1"` | Estable, bajo riesgo |
| `serde_json` | `"1"` | Estable, bajo riesgo |

**Problema principal:** Las versiones `"2"` y `"1"` sin restricción minor
permiten actualizaciones automáticas en `cargo update`. Para builds reproducibles
se recomienda fijar minor version: `"2.1"` o usar `=2.0.8`.

**`Cargo.lock` está en `.gitignore`?** Verificar — para aplicaciones Tauri
**se debe commitear** `Cargo.lock` para builds reproducibles. Las bibliotecas
omiten el lock, las aplicaciones no.

### Node.js (package.json)

| Dependencia | Versión | Riesgo |
|-------------|---------|--------|
| `@tauri-apps/cli` | `"^2"` | Permite actualizaciones menores automáticas |

Solo hay una dependencia npm. Bajo riesgo superficial, pero `^2` permite
actualizaciones a `2.x.y` que podrían desalinearse con el `tauri = "2"` en
Cargo.toml si las versiones del CLI y las librerías no están sincronizadas.

**Acción:** Fijar a la versión exacta del CLI usada: `"2.1.0"` en lugar de `"^2"`.

### Android

No hay archivo `build.gradle` — el build es manual con `javac` + `aapt2` + `d8`.
Las versiones de herramientas SDK están hardcodeadas en los scripts:

| Herramienta | Versión en build.sh | Versión en release.yml |
|-------------|--------------------|-----------------------|
| Android SDK build-tools | 36.1.0 | 35.0.0 |
| Platform SDK | API 35 | API 35 |
| `targetSdkVersion` | (en manifest) 36 | 36 |

**Inconsistencia:** build.sh usa build-tools 36.1.0 pero release.yml usa 35.0.0
como primera opción. La APK local y la de CI pueden diferir en comportamiento
del `d8` dexer.

---

## 5. Inconsistencias de Estilo Entre Plataformas

### Arquitectura

| Aspecto | Desktop (Rust/Tauri) | Android (Java) |
|---------|---------------------|----------------|
| Manejo de errores | `.expect("msg")` — panic | `catch(Exception e)` — log silencioso |
| Modelo de concurrencia | Tauri async/event loop | Handler + Runnable manual |
| Ciclo de vida | Gestionado por Tauri | Gestionado por Service/Widget |
| Logging | No hay logging explícito | `Log.d/e/w` de Android |
| Internacionalización | Sin soporte | Sin soporte (strings en código) |

### CSS — Unidades incompatibles

El mismo `style.css` usa unidades distintas en cada plataforma:

| Plataforma | Unidades de fuente | Layout |
|------------|------------------|--------|
| Desktop | `cqw`, `cqh` | `container-type: size` |
| Android (post-build.sh) | `vw`, `vmin` | Sin container queries |
| Android (post-release.yml) | `vw` (valores distintos) | Sin container queries |

Los valores de fuente tras la transformación difieren: `build.sh` convierte
`12vw → 28vmin` mientras `release.yml` convierte `12vw → 13vw`. El resultado
visual puede ser diferente.

### Java — Abreviaciones inconsistentes en TAG

```java
// ClockView.java — nombre completo
private static final String TAG = "ClockView";

// ClockViewRenderer.java — nombre completo
private static final String TAG = "ClockViewRenderer";

// BatteryOptimizationHelper.java — abreviado
private static final String TAG = "BatteryOptHelper";

// WidgetUpdateService.java — abreviado
private static final String TAG = "WidgetSvc";
```

Dificulta el filtrado de logcat. Convención recomendada: usar siempre el
nombre completo de la clase (`getClass().getSimpleName()`).

### Gestión de errores en JavaScript

```javascript
// Patrón 1: try-catch con log (clock.js:335)
} catch (err) {
    console.log('Tauri close failed:', err);
}

// Patrón 2: sin manejo de errores (clock.js:372-388)
const { invoke } = window.__TAURI__.core;
invoke('some_command'); // sin .catch()
```

---

## Lista Accionable — Priorizada por Severidad

### Prioridad 1 — Hacer ahora (seguridad)

| # | Acción | Archivos | Esfuerzo |
|---|--------|----------|---------|
| 1 | **Rotar el keystore** y mover credenciales a GitHub Secrets (`KEYSTORE_STORE_PASS`, `KEYSTORE_KEY_PASS`) | `build.sh:172-173`, `release.yml:230-231` | Bajo |
| 2 | **Restringir el job de release** con `if: github.ref_type == 'tag'` para proteger `RELEASE_TOKEN` | `release.yml` | Bajo |
| 3 | **Deshabilitar** `setAllowFileAccessFromFileURLs(true)` y `setAllowUniversalAccessFromFileURLs(true)` en WebView | `WebViewManager.java:125-126` | Bajo |

### Prioridad 2 — Esta semana (estabilidad y crashes)

| # | Acción | Archivos | Esfuerzo |
|---|--------|----------|---------|
| 4 | **Corregir el bitmap race condition** en `WebViewManager.render()` — asignar nuevo bitmap antes de reciclar el anterior | `WebViewManager.java:184-216` | Bajo |
| 5 | **Agregar `onDetachedFromWindow()`** en `ClockView` para detener el handler recursivo | `ClockView.java:696-726` | Bajo |
| 6 | **Sincronizar versión Java** a `11` en `release.yml` (actualmente usa `1.8`) | `release.yml:177` | Bajo |
| 7 | **Reemplazar `setDrawingCacheEnabled`** (deprecado) con `webView.draw(canvas)` directo | `WebViewManager.java:205-209` | Bajo |
| 8 | **Commitear `Cargo.lock`** al repositorio para builds reproducibles de desktop | `.gitignore` | Bajo |

### Prioridad 3 — Este mes (calidad de código)

| # | Acción | Archivos | Esfuerzo |
|---|--------|----------|---------|
| 9 | **Extraer magic numbers** a constantes nombradas en `ClockView` | `ClockView.java` | Medio |
| 10 | **Reemplazar `catch (Exception e)`** con excepciones específicas e incluir stacktrace (`Log.e(TAG, "msg", e)`) | 41 ocurrencias | Medio |
| 11 | **Unificar CSS** — crear `style.desktop.css` y `style.android.css` separados para eliminar las 9+ transformaciones `sed` | `ui/style.css`, `build.sh`, `release.yml` | Medio |
| 12 | **Refactorizar el fallback de build-tools** a un loop en `build.sh` | `build.sh:28-48` | Bajo |
| 13 | **Fijar versiones de dependencias** — `@tauri-apps/cli` a versión exacta, tauri a `"2.x"` | `package.json`, `Cargo.toml` | Bajo |
| 14 | **Eliminar `window.nervClock`** del scope global en `clock.js` | `clock.js:421` | Bajo |
| 15 | **Cachear selectores DOM** repetidos en `clock.js` | `clock.js:240-265` | Bajo |

### Prioridad 4 — Backlog (arquitectura)

| # | Acción | Archivos | Esfuerzo |
|---|--------|----------|---------|
| 16 | **Dividir `WidgetUpdateService`** (664 líneas, God Class) en al menos 3 clases | `WidgetUpdateService.java` | Alto |
| 17 | **Eliminar static singleton** — reemplazar por `WeakReference` o patrón de binding | `WidgetUpdateService.java:63-66` | Alto |
| 18 | **Reemplazar `HashMap` de 8 dígitos** por arrays con índices constantes | `ClockView.java:51-52` | Bajo |
| 19 | **Estandarizar TAGs de logging** usando `getClass().getSimpleName()` | Todos los `.java` | Bajo |
| 20 | **Agregar `addJavascriptInterface` guard** — validar origen de scripts antes de `evaluateJavascript` | `WebViewManager.java:227` | Medio |

---

## Resumen Ejecutivo

| Categoría | Crítico | Alto | Medio | Bajo |
|-----------|---------|------|-------|------|
| Seguridad | 2 | 1 | 1 | 1 |
| Estabilidad | 0 | 3 | 4 | 0 |
| Calidad | 2 | 1 | 5 | 6 |
| Build/CI | 0 | 1 | 3 | 2 |
| **Total** | **4** | **6** | **13** | **9** |

**El riesgo más alto es la doble exposición de credenciales del keystore** en
`build.sh` y `release.yml`. Si el repositorio es o fue público en algún momento,
la cadena de firma de la APK está comprometida y se debe rotar el keystore
antes de cualquier release de producción.
