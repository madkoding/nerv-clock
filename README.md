# NERV Clock - Unified Evangelion Style Clock

A clock widget with **transparent background** that works on:
- 🖥️ **Desktop** (Windows, macOS, Linux) - via Tauri
- 📱 **Android** - Native widget with transparent background

## Structure

```
nerv-clock/
├── ui/                    # Shared HTML/CSS/JS design
│   ├── index.html
│   ├── style.css
│   └── clock.js
├── src-tauri/             # Tauri Desktop App
│   └── ...
└── android/               # Android Widget
    └── ...
```

## Shared Design

Both platforms use the same NERV/Evangelion visual style:
- 🟠 NERV Orange (#FF6A00)
- 🔴 NERV Red (#C80000)
- 🟢 Status Green (#00FF88)
- ⬛ Dark transparent background

## Building

### Desktop (Tauri)

```bash
cd nerv-clock
npm install
npm run dev     # Development
npm run build   # Production
```

### Android Widget

```bash
cd nerv-clock/android
chmod +x build.sh
./build.sh
adb install -r build/bin/NervClock.apk
```

## Features

- ✅ **Transparent background** on both platforms
- ✅ Analog clock with NERV styling
- ✅ Digital time display
- ✅ MAGI SYSTEM status
- ✅ Sync rate indicator
- ✅ Corner accent decorations
- ✅ Draggable window (desktop)
- ✅ Always on top (desktop)
- ✅ Resizable widget (Android)
