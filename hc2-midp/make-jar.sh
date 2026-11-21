#!/bin/sh
# Builds the native MIDP 2.0 / CLDC 1.1 (+ JSR-135 MMAPI) version of Honey Cave 2.
#
#   ./make-jar.sh HoneyCave2.mpn [out.jar]
#
# The game code (src/hc2) is plain Java; the pictures, maps, texts and sounds are converted from
# the original mophun container at build time by tools/Hc2Assets.java (the .mpn is not shipped in
# the repository). Needs a JDK 8 and the J2ME class libraries + ProGuard of
# https://github.com/klaxons1/LLM-J2ME-SUITE (cloned automatically, or point J2ME_SUITE at a copy).
# ProGuard adds the CLDC stack maps (preverification): without them real phone KVMs refuse the jar.
# Writes out.jar and out.jad (default: ../release/HoneyCave2-midp.jar).
set -e
cd "$(dirname "$0")"
if [ $# -lt 1 ]; then
  echo "usage: $0 HoneyCave2.mpn [out.jar]" >&2; exit 2
fi
ROM=$(cd "$(dirname "$1")" && pwd)/$(basename "$1")
OUT=${2:-../release/HoneyCave2-midp.jar}
case "$OUT" in /*) ;; *) OUT=$(pwd)/$OUT ;; esac
JAD=${OUT%.jar}.jad
TITLE="Honey Cave 2"

SUITE=${J2ME_SUITE:-$HOME/.cache/LLM-J2ME-SUITE}
if [ ! -f "$SUITE/uei/cldc11.jar" ]; then
  echo "fetching LLM-J2ME-SUITE into $SUITE"
  git clone --depth 1 https://github.com/klaxons1/LLM-J2ME-SUITE "$SUITE"
fi
BOOT="$SUITE/uei/cldc11.jar:$SUITE/uei/midp21.jar:$SUITE/uei/jsr135.jar"

B=$(pwd)/build
rm -rf "$B"; mkdir -p "$B/tools" "$B/classes" "$B/jar"

echo "== asset converter (uses the desktop emulator's container / decryption classes)"
find ../desktop/src -name '*.java' > "$B/desktop-sources.txt"
mkdir -p ../desktop/out
javac -nowarn -encoding UTF-8 -d ../desktop/out @"$B/desktop-sources.txt"
javac -nowarn -encoding UTF-8 -cp ../desktop/out -d "$B/tools" tools/Hc2Assets.java
java -Djava.awt.headless=true -cp ../desktop/out:"$B/tools" Hc2Assets "$ROM" "$B/classes"

echo "== javac -source 1.3 -target 1.3 (CLDC 1.1 / MIDP 2.0 / JSR-135)"
find src -name '*.java' > "$B/sources.txt"
javac -nowarn -encoding UTF-8 -source 1.3 -target 1.3 -bootclasspath "$BOOT" -extdirs "" \
      -d "$B/classes" @"$B/sources.txt" 2>&1 | grep -v '^warning: \[options\]' | grep -v '^[0-9]* warnings$' || true
[ -f "$B/classes/hc2/HC2.class" ] && [ -f "$B/classes/hc2/Game.class" ] && [ -f "$B/classes/hc2/Res.class" ] || { echo "compilation failed" >&2; exit 1; }

VER=1.0.0
cat > "$B/MANIFEST.MF" <<EOF
Manifest-Version: 1.0
MIDlet-Name: $TITLE
MIDlet-Vendor: wolphun
MIDlet-Version: $VER
MIDlet-Description: $TITLE (native MIDP 2.0 port)
MIDlet-Icon: /icon.png
MIDlet-1: $TITLE, /icon.png, hc2.HC2
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.0
EOF

echo "== jar + ProGuard (shrink, preverify)"
cp ../j2me/res/icon.png "$B/classes/icon.png"
(cd "$B/classes" && jar cfm ../raw.jar ../MANIFEST.MF .)
cat > "$B/proguard.pro" <<EOF
-injars $B/raw.jar
-outjars $B/pre.jar
-libraryjars $SUITE/uei/cldc11.jar
-libraryjars $SUITE/uei/midp21.jar
-libraryjars $SUITE/uei/jsr135.jar
-microedition
-target 1.3
-dontoptimize
-dontobfuscate
-dontwarn
-dontnote
-keep public class * extends javax.microedition.midlet.MIDlet
EOF
java -jar "$SUITE/libs/proguard.jar" @"$B/proguard.pro"

# ProGuard keeps the manifest and the resources; re-pack with maximum compression
rm -rf "$B/jar"; mkdir -p "$B/jar"
(cd "$B/jar" && jar xf ../pre.jar && rm -rf META-INF && jar cfm "$OUT" ../MANIFEST.MF .)

SIZE=$(wc -c < "$OUT" | tr -d ' ')
cat > "$JAD" <<EOF
MIDlet-Name: $TITLE
MIDlet-Vendor: wolphun
MIDlet-Version: $VER
MIDlet-Description: $TITLE (native MIDP 2.0 port)
MIDlet-Icon: /icon.png
MIDlet-1: $TITLE, /icon.png, hc2.HC2
MIDlet-Jar-URL: $(basename "$OUT")
MIDlet-Jar-Size: $SIZE
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.0
EOF
echo "built $OUT ($SIZE bytes) + $(basename "$JAD")"
