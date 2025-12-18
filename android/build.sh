#!/bin/bash
# Build script for NERV Clock Android Widget
# Uses manual compilation (no Gradle needed)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# SDK paths - Updated to support Android 11-16 (API 30-36)
# Compile with latest SDK for newest features, minSdkVersion ensures backward compatibility
SDK_PATH="$HOME/Android/Sdk"
BUILD_TOOLS="$SDK_PATH/build-tools/35.0.0"
PLATFORM="$SDK_PATH/platforms/android-35"

# Fallback to older versions if 35 not available
if [ ! -d "$BUILD_TOOLS" ]; then
    BUILD_TOOLS="$SDK_PATH/build-tools/34.0.0"
fi
if [ ! -d "$BUILD_TOOLS" ]; then
    BUILD_TOOLS="$SDK_PATH/build-tools/33.0.2"
fi
if [ ! -d "$BUILD_TOOLS" ]; then
    BUILD_TOOLS="$SDK_PATH/build-tools/30.0.3"
fi
if [ ! -d "$PLATFORM" ]; then
    PLATFORM="$SDK_PATH/platforms/android-34"
fi
if [ ! -d "$PLATFORM" ]; then
    PLATFORM="$SDK_PATH/platforms/android-33"
fi
if [ ! -d "$PLATFORM" ]; then
    PLATFORM="$SDK_PATH/platforms/android-30"
fi

echo "📍 Using BUILD_TOOLS: $BUILD_TOOLS"
echo "📍 Using PLATFORM: $PLATFORM"

# Build directories
BUILD_DIR="build"
GEN_DIR="$BUILD_DIR/gen"
OBJ_DIR="$BUILD_DIR/obj"
BIN_DIR="$BUILD_DIR/bin"
CLASSES_DIR="$BUILD_DIR/classes"

# Clean and create directories
rm -rf "$BUILD_DIR"
mkdir -p "$GEN_DIR" "$OBJ_DIR" "$BIN_DIR" "$CLASSES_DIR"

echo "📦 Compilando recursos..."
"$BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o "$OBJ_DIR/resources.zip"

echo "📦 Enlazando recursos..."
"$BUILD_TOOLS/aapt2" link \
    "$OBJ_DIR/resources.zip" \
    -I "$PLATFORM/android.jar" \
    --manifest app/src/main/AndroidManifest.xml \
    --java "$GEN_DIR" \
    -o "$OBJ_DIR/app.apk.unaligned" \
    --auto-add-overlay

echo "☕ Compilando código Java..."
find app/src/main/java -name "*.java" > "$BUILD_DIR/sources.txt"
echo "$GEN_DIR/com/nerv/clock/R.java" >> "$BUILD_DIR/sources.txt"

javac -source 1.8 -target 1.8 \
    -bootclasspath "$PLATFORM/android.jar" \
    -classpath "$PLATFORM/android.jar" \
    -g:none \
    -d "$CLASSES_DIR" \
    @"$BUILD_DIR/sources.txt" 2>/dev/null || true

echo "📱 Creando DEX..."
# Use d8 for SDK 34+, fallback to dx for older versions
# --min-api 30 ensures DEX is compatible with Android 11+ while using latest bytecode
if [ -f "$BUILD_TOOLS/d8" ]; then
    "$BUILD_TOOLS/d8" --min-api 30 --lib "$PLATFORM/android.jar" --output "$OBJ_DIR" $(find "$CLASSES_DIR" -name "*.class")
else
    "$BUILD_TOOLS/dx" --dex --output="$OBJ_DIR/classes.dex" "$CLASSES_DIR"
fi

echo "📦 Creando APK..."
cp "$OBJ_DIR/app.apk.unaligned" "$OBJ_DIR/app.apk.tmp"
cd "$OBJ_DIR"
zip -j app.apk.tmp classes.dex
cd "$SCRIPT_DIR"

# Add assets (HTML, CSS, JS, fonts for WebView)
# Copy from shared ui/ folder to avoid code duplication
echo "📁 Copiando UI compartida a assets..."
ASSETS_DIR="app/src/main/assets"
rm -rf "$ASSETS_DIR"
mkdir -p "$ASSETS_DIR/fonts"
cp ../ui/index.html "$ASSETS_DIR/widget.html"
# Use device-width viewport so content fills the entire widget area
sed -i 's/width=device-width/width=device-width/g' "$ASSETS_DIR/widget.html"
cp ../ui/style.css "$ASSETS_DIR/"
# Fix CSS units for Android WebView (cqw not supported, use vw instead)
# vw/vh units will work based on the WebView dimensions
sed -i 's/cqw/vw/g' "$ASSETS_DIR/style.css"
sed -i 's/cqh/vh/g' "$ASSETS_DIR/style.css"
# Remove container queries completely (not supported in Android WebView)
sed -i 's/container-type: size;//g' "$ASSETS_DIR/style.css"
sed -i 's/container-name: clock;//g' "$ASSETS_DIR/style.css"
# Remove @container blocks entirely (they cause parsing errors in old WebViews)
# This removes @container ... { ... } blocks
sed -i '/@container/,/^}/d' "$ASSETS_DIR/style.css"
# Change clock digits from vw to vmin so they scale with the smaller dimension
# vmin = minimum of vw and vh, so it adapts to both width AND height
# segment-digit main font: vw -> vmin
sed -i 's/font-size: 12vw/font-size: 28vmin/g' "$ASSETS_DIR/style.css"
sed -i 's/font-size: 13vw/font-size: 28vmin/g' "$ASSETS_DIR/style.css"
# segment-digit.small and colon: vw -> vmin
sed -i 's/font-size: 10vw/font-size: 22vmin/g' "$ASSETS_DIR/style.css"
sed -i 's/font-size: 8vw/font-size: 18vmin/g' "$ASSETS_DIR/style.css"
# Also update the @supports fallback values
sed -i 's/font-size: 9vw/font-size: 28vmin/g' "$ASSETS_DIR/style.css"
sed -i 's/font-size: 6vw/font-size: 18vmin/g' "$ASSETS_DIR/style.css"
cp ../ui/clock.js "$ASSETS_DIR/"
cp ../ui/fonts/dseg7.ttf "$ASSETS_DIR/fonts/" 2>/dev/null || true
cp ../ui/fonts/dseg7.woff "$ASSETS_DIR/fonts/" 2>/dev/null || true
cp ../ui/fonts/NimbusSans-Regular.otf "$ASSETS_DIR/fonts/" 2>/dev/null || true
cp ../ui/fonts/NimbusSans-Bold.otf "$ASSETS_DIR/fonts/" 2>/dev/null || true

if [ -d "$ASSETS_DIR" ]; then
    echo "📁 Añadiendo assets al APK..."
    cd app/src/main
    zip -r "$SCRIPT_DIR/$OBJ_DIR/app.apk.tmp" assets/
    cd "$SCRIPT_DIR"
fi

echo "✍️ Alineando y firmando APK..."
"$BUILD_TOOLS/zipalign" -f 4 "$OBJ_DIR/app.apk.tmp" "$OBJ_DIR/app.apk.aligned"

# Create keystore if needed
KEYSTORE="$SCRIPT_DIR/release.keystore"
if [ ! -f "$KEYSTORE" ]; then
    echo "🔑 Generando keystore..."
    keytool -genkey -v \
        -keystore "$KEYSTORE" \
        -alias nerv \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass android \
        -keypass android \
        -dname "CN=NERV, OU=MAGI, O=NERV, L=Tokyo-3, ST=Japan, C=JP"
fi

"$BUILD_TOOLS/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$BIN_DIR/NervClock.apk" \
    "$OBJ_DIR/app.apk.aligned"

echo ""
echo "✅ APK generado: $BIN_DIR/NervClock.apk"
echo ""
echo "Para instalar:"
echo "  adb install -r $BIN_DIR/NervClock.apk"
