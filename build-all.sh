#!/bin/bash
# Build script for NERV Clock - All platforms
# Creates release folder with timestamped builds

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Timestamp for this build
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RELEASE_DIR="$SCRIPT_DIR/release/$TIMESTAMP"

echo "🚀 NERV Clock Build - All Platforms"
echo "📅 Timestamp: $TIMESTAMP"
echo "📁 Release dir: $RELEASE_DIR"
echo ""

# Create release directory
mkdir -p "$RELEASE_DIR"

# Build Android
echo "═══════════════════════════════════════════"
echo "📱 Building Android..."
echo "═══════════════════════════════════════════"
cd android
./build.sh
cp build/bin/NervClock.apk "$RELEASE_DIR/NervClock_${TIMESTAMP}.apk"
echo "✅ Android: NervClock_${TIMESTAMP}.apk"
cd "$SCRIPT_DIR"

# Build Desktop (Windows, Linux, macOS)
echo ""
echo "═══════════════════════════════════════════"
echo "🖥️  Building Desktop (Tauri)..."
echo "═══════════════════════════════════════════"
npm run build

# Find and copy built files
TAURI_RELEASE="src-tauri/target/release"

# Linux AppImage
if [ -f "$TAURI_RELEASE/bundle/appimage/"*.AppImage ]; then
    for f in "$TAURI_RELEASE/bundle/appimage/"*.AppImage; do
        BASENAME=$(basename "$f" .AppImage)
        cp "$f" "$RELEASE_DIR/${BASENAME}_${TIMESTAMP}.AppImage"
        echo "✅ Linux AppImage: ${BASENAME}_${TIMESTAMP}.AppImage"
    done
fi

# Linux deb
if [ -f "$TAURI_RELEASE/bundle/deb/"*.deb ]; then
    for f in "$TAURI_RELEASE/bundle/deb/"*.deb; do
        BASENAME=$(basename "$f" .deb)
        cp "$f" "$RELEASE_DIR/${BASENAME}_${TIMESTAMP}.deb"
        echo "✅ Linux DEB: ${BASENAME}_${TIMESTAMP}.deb"
    done
fi

# Linux rpm
if [ -f "$TAURI_RELEASE/bundle/rpm/"*.rpm ]; then
    for f in "$TAURI_RELEASE/bundle/rpm/"*.rpm; do
        BASENAME=$(basename "$f" .rpm)
        cp "$f" "$RELEASE_DIR/${BASENAME}_${TIMESTAMP}.rpm"
        echo "✅ Linux RPM: ${BASENAME}_${TIMESTAMP}.rpm"
    done
fi

# Windows exe/msi (if cross-compiled or on Windows)
if ls "$TAURI_RELEASE/bundle/msi/"*.msi 2>/dev/null; then
    for f in "$TAURI_RELEASE/bundle/msi/"*.msi; do
        BASENAME=$(basename "$f" .msi)
        cp "$f" "$RELEASE_DIR/${BASENAME}_${TIMESTAMP}.msi"
        echo "✅ Windows MSI: ${BASENAME}_${TIMESTAMP}.msi"
    done
fi

if ls "$TAURI_RELEASE/bundle/nsis/"*.exe 2>/dev/null; then
    for f in "$TAURI_RELEASE/bundle/nsis/"*.exe; do
        BASENAME=$(basename "$f" .exe)
        cp "$f" "$RELEASE_DIR/${BASENAME}_${TIMESTAMP}.exe"
        echo "✅ Windows EXE: ${BASENAME}_${TIMESTAMP}.exe"
    done
fi

# macOS app/dmg (if on macOS)
if ls "$TAURI_RELEASE/bundle/macos/"*.app 2>/dev/null; then
    for f in "$TAURI_RELEASE/bundle/macos/"*.app; do
        BASENAME=$(basename "$f" .app)
        # Compress .app as zip
        cd "$TAURI_RELEASE/bundle/macos"
        zip -r "$RELEASE_DIR/${BASENAME}_${TIMESTAMP}_macos.zip" "$(basename "$f")"
        cd "$SCRIPT_DIR"
        echo "✅ macOS App: ${BASENAME}_${TIMESTAMP}_macos.zip"
    done
fi

if ls "$TAURI_RELEASE/bundle/dmg/"*.dmg 2>/dev/null; then
    for f in "$TAURI_RELEASE/bundle/dmg/"*.dmg; do
        BASENAME=$(basename "$f" .dmg)
        cp "$f" "$RELEASE_DIR/${BASENAME}_${TIMESTAMP}.dmg"
        echo "✅ macOS DMG: ${BASENAME}_${TIMESTAMP}.dmg"
    done
fi

# Copy standalone binary if exists
if [ -f "$TAURI_RELEASE/nerv-clock" ]; then
    cp "$TAURI_RELEASE/nerv-clock" "$RELEASE_DIR/nerv-clock_${TIMESTAMP}_linux"
    echo "✅ Linux Binary: nerv-clock_${TIMESTAMP}_linux"
fi

if [ -f "$TAURI_RELEASE/nerv-clock.exe" ]; then
    cp "$TAURI_RELEASE/nerv-clock.exe" "$RELEASE_DIR/nerv-clock_${TIMESTAMP}.exe"
    echo "✅ Windows Binary: nerv-clock_${TIMESTAMP}.exe"
fi

# Create build info file
cat > "$RELEASE_DIR/BUILD_INFO.txt" << EOF
NERV Clock Build Information
============================
Build Timestamp: $TIMESTAMP
Build Date: $(date)
Build Machine: $(hostname)
Git Commit: $(git rev-parse HEAD 2>/dev/null || echo "N/A")
Git Branch: $(git branch --show-current 2>/dev/null || echo "N/A")

Files in this release:
$(ls -la "$RELEASE_DIR" | tail -n +2)
EOF

echo ""
echo "═══════════════════════════════════════════"
echo "✅ Build complete!"
echo "═══════════════════════════════════════════"
echo "📁 Release folder: $RELEASE_DIR"
echo ""
ls -la "$RELEASE_DIR"
