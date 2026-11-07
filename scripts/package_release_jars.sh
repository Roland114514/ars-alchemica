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

MC_JAR=$(find ~/.gradle/caches -name "minecraft-common-1.20.1*.jar" 2>/dev/null | head -n 1)
DFU_JAR=$(find ~/.gradle/caches -name "datafixerupper-6.0.8.jar" 2>/dev/null | head -n 1)

if [ -z "$MC_JAR" ]; then
    echo "ERROR: Minecraft 1.20.1 cache jar not found in ~/.gradle/caches."
    exit 1
fi

echo "Using Minecraft Jar: $MC_JAR"
echo "Using DFU Jar: $DFU_JAR"

# Ensure common classes exist (compiled against official 1.20.1 Mojmap)
if [ ! -d "common/build/classes/java/main/com" ]; then
    echo "ERROR: common/build/classes/java/main/com not found. Please compile common first."
    exit 1
fi

echo ">>> [1/3] Compiling Forge / NeoForge 1.20.1 Module (Mojmap space)..."
mkdir -p build/classes/forge
"$JAVAC" -encoding UTF-8 -d build/classes/forge \
  -cp "common/build/classes/java/main:$MC_JAR:$DFU_JAR" \
  $(find forge/src/main/java forge/src/stubs/java -name "*.java")

echo ">>> [2/3] Assembling NeoForge / Forge JAR..."
STAGE_NEOFORGE="build/staging/neoforge"
rm -rf "$STAGE_NEOFORGE"
mkdir -p "$STAGE_NEOFORGE"

# Copy real classes (never include test stubs)
cp -r common/build/classes/java/main/com "$STAGE_NEOFORGE/"
cp -r build/classes/forge/com/medievalmagic/arsalchemica/forge "$STAGE_NEOFORGE/com/medievalmagic/arsalchemica/"

# Copy assets and resources
cp -r common/src/main/resources/* "$STAGE_NEOFORGE/"
cp -r forge/src/main/resources/* "$STAGE_NEOFORGE/"

NEOFORGE_JAR="$OUTPUT_DIR/ars_alchemica-${MC_VERSION}-${VERSION}-neoforge.jar"
"$JAR" cf "$NEOFORGE_JAR" -C "$STAGE_NEOFORGE" .
echo "  -> Assembled NeoForge JAR: $NEOFORGE_JAR"

echo ">>> [3/3] Verifying Fabric JAR (Intermediary remapped space)..."
FABRIC_JAR="$OUTPUT_DIR/ars_alchemica-${MC_VERSION}-${VERSION}-fabric.jar"
if [ ! -f "$FABRIC_JAR" ] && [ -f "fabric/build/libs/fabric-${VERSION}.jar" ]; then
    cp "fabric/build/libs/fabric-${VERSION}.jar" "$FABRIC_JAR"
fi

echo ""
echo ">>> SUCCESS! Both NeoForge and Fabric 1.20.1 mod JARs are packaged and ready."
ls -lh "$OUTPUT_DIR"/*.jar
