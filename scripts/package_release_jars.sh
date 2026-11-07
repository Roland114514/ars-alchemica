#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

echo "=========================================================="
echo "=== ARS ALCHEMICA: MULTI-LOADER JAR PACKAGING PIPELINE ==="
echo "=========================================================="

JDK_BIN="/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin"
JAVAC="$JDK_BIN/javac"
JAR="$JDK_BIN/jar"

VERSION="0.6.0"
MC_VERSION="1.20.1"
OUTPUT_DIR="build/libs"
mkdir -p "$OUTPUT_DIR"

echo ">>> [1/4] Compiling Common Module..."
mkdir -p build/classes/common_main
"$JAVAC" -encoding UTF-8 -d build/classes/common_main \
  $(find common/src/main/java common/src/test/java/net/minecraft common/src/test/java/org -name "*.java")

echo ">>> [2/4] Compiling Forge / NeoForge 1.20.1 Module..."
mkdir -p build/classes/forge
"$JAVAC" -encoding UTF-8 -d build/classes/forge \
  $(find common/src/main/java forge/src/main/java forge/src/stubs/java common/src/test/java/net/minecraft common/src/test/java/org -name "*.java")

echo ">>> [3/4] Compiling Fabric 1.20.1 Module..."
mkdir -p build/classes/fabric
"$JAVAC" -encoding UTF-8 -d build/classes/fabric \
  $(find common/src/main/java fabric/src/main/java fabric/src/stubs/java common/src/test/java/net/minecraft common/src/test/java/org -name "*.java")

echo ">>> [4/4] Assembling Release JARs..."

# --- Package NeoForge / Forge JAR ---
STAGE_NEOFORGE="build/staging/neoforge"
rm -rf "$STAGE_NEOFORGE"
mkdir -p "$STAGE_NEOFORGE"

# Copy classes
cp -r build/classes/common_main/com "$STAGE_NEOFORGE/"
cp -r build/classes/forge/com/medievalmagic/arsalchemica/forge "$STAGE_NEOFORGE/com/medievalmagic/arsalchemica/"

# Copy assets and resources
cp -r common/src/main/resources/* "$STAGE_NEOFORGE/"
cp -r forge/src/main/resources/* "$STAGE_NEOFORGE/"

NEOFORGE_JAR="$OUTPUT_DIR/ars_alchemica-${MC_VERSION}-${VERSION}-neoforge.jar"
"$JAR" cf "$NEOFORGE_JAR" -C "$STAGE_NEOFORGE" .
echo "  -> Assembled: $NEOFORGE_JAR"

# --- Package Fabric JAR ---
STAGE_FABRIC="build/staging/fabric"
rm -rf "$STAGE_FABRIC"
mkdir -p "$STAGE_FABRIC"

# Copy classes
cp -r build/classes/common_main/com "$STAGE_FABRIC/"
cp -r build/classes/fabric/com/medievalmagic/arsalchemica/fabric "$STAGE_FABRIC/com/medievalmagic/arsalchemica/"

# Copy assets and resources
cp -r common/src/main/resources/* "$STAGE_FABRIC/"
cp -r fabric/src/main/resources/* "$STAGE_FABRIC/"

FABRIC_JAR="$OUTPUT_DIR/ars_alchemica-${MC_VERSION}-${VERSION}-fabric.jar"
"$JAR" cf "$FABRIC_JAR" -C "$STAGE_FABRIC" .
echo "  -> Assembled: $FABRIC_JAR"

echo ""
echo ">>> SUCCESS! Both NeoForge and Fabric 1.20.1 mod JARs are packaged and ready."
ls -lh "$OUTPUT_DIR"/*.jar
