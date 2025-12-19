# NERV Clock - Java Implementation Branch

Esta rama (`java`) contiene una reimplementación completa de la interfaz de usuario en **Java puro**, sin WebView.

## Características

### Implementación en Java
- **ClockView**: Vista personalizada que extiende `android.view.View` con dibujado via Canvas
- **ClockLogic**: Lógica del reloj con soporte para múltiples modos
- **ColorScheme**: Esquema de colores exacto del HTML original
- **FontManager**: Carga y manejo de fuentes DSEG7 y Nimbus Sans

### Modos de Reloj
1. **NORMAL** - Hora actual del sistema (verde)
2. **RACING** - Cronómetro (rojo)
3. **SLOW** - Pomodoro 25 min / 5 min (amarillo)
4. **STOP** - Pausa/Reanuda (gris)

### Efectos Visuales
- ✨ **Glow Effects** - Efecto luminoso alrededor de los dígitos
- 📺 **Scan Lines** - Líneas de escaneo CRT
- 🔶 **Hexagon Pattern** - Patrón hexagonal de fondo
- 🟡 **Warning States** - Cambio de color para advertencias
- 💫 **Blink Animations** - Animaciones de parpadeo
- 👻 **Ghost Digits** - Dígitos fantasma al cambiar

### UI Components
- **TopBar**: Texto japonés, indicador de modo, warning box
- **ClockDisplay**: Dígitos 7-segmento con colones parpadeantes
- **ControlBar**: Botones STOP, SLOW, NORMAL, RACING
- **Status Light**: Luz de estado pulsante

## Estructura de Archivos

```
android/app/src/main/java/com/nerv/clock/
├── NervClockActivity.java          # Activity principal
├── ui/
│   ├── ClockView.java              # Vista principal con dibujado
│   ├── ClockLogic.java             # Lógica de reloj
│   ├── ColorScheme.java            # Esquema de colores
│   ├── FontManager.java            # Gestor de fuentes
│   └── DrawingUtils.java           # Utilidades de dibujado

android/app/src/main/assets/fonts/
├── dseg7.ttf                       # Fuente 7-segmento
├── dseg7.woff                      # Fuente 7-segmento (web)
├── NimbusSans-Regular.otf          # Fuente regular
└── NimbusSans-Bold.otf             # Fuente bold
```

## Colores NERV

| Color | Hex | Uso |
|-------|-----|-----|
| Naranja | `#FF6A00` | Dígitos, texto principal, glow |
| Naranja Brillante | `#FF8C00` | Variante clara |
| Rojo | `#CC0000` | Modo RACING, crítico |
| Verde | `#00CC00` | Modo NORMAL, status light |
| Amarillo | `#FFCC00` | Modo SLOW, warning |
| Oscuro | `#0A0800` | Fondo principal |
| Oscuro Café | `#1A1400` | Fondo gradiente |

## Compilación

```bash
cd android
./build.sh
```

Genera: `build/bin/NervClock.apk`

## Instalación

```bash
adb install -r build/bin/NervClock.apk
```

## Ejecución

### Como Widget (original)
El widget sigue funcionando normalmente en el home screen.

### Como Activity (nueva)
```bash
adb shell am start -n com.nerv.clock/.NervClockActivity
```

## Comparación HTML vs Java

| Aspecto | HTML | Java |
|---------|------|------|
| Renderizado | CSS/HTML | Canvas |
| WebView | Requerido | ❌ No |
| Performance | Variable | Mejor |
| Escalabilidad | Responsiva | DPI-aware |
| Compatibilidad | Android 5+ | Android 11+ |
| Tamaño APK | ~200KB | ~150KB |

## Características Completadas

- ✅ Dibujado de dígitos 7-segmento exactos
- ✅ Colones con animación de parpadeo
- ✅ Cuatro modos de reloj funcionales
- ✅ Botones interactivos con estados
- ✅ Warning box con patrón de rayas
- ✅ Top bar con información del sistema
- ✅ Control bar con botones de modo
- ✅ Status light pulsante
- ✅ Patrón hexagonal de fondo
- ✅ Líneas de escaneo
- ✅ Efectos glow y sombras
- ✅ Animaciones de crítico
- ✅ Mensaje de "DEPLETED" en pomodoro

## Notas de Desarrollo

### Canvas vs WebView
- Canvas proporciona mejor control pixel-perfect
- Sin overhead de WebView
- Mejor rendimiento en dispositivos antiguos
- Dibujo más fluido de animaciones

### Consideraciones Futuras
- Soporte para tema oscuro/claro
- Exportar como overlay flotante
- Integración con sistema de notificaciones
- Widgets redimensionables

## Testing

Para probar todos los modos:

1. **NORMAL**: Se muestra hora actual en verde
2. **RACING**: Inicia cronómetro, botón STOP cambia a PLAY en pausa
3. **SLOW**: Pomodoro de 25 minutos, color amarillo a los 4 minutos
4. **Crítico**: Pulsación roja cuando queda < 1 minuto
5. **DEPLETED**: Mensaje "電力枯渇 DEPLETED" al terminar

---

**Rama**: `java`
**Basada en**: `master` (3750b3e)
**Última actualización**: 2025-12-19
