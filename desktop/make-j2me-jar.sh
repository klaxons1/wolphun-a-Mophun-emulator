#!/bin/sh
# Builds a J2ME (MIDP 2.0 / CLDC 1.1 + MMAPI) MIDlet of a decompiled mophun game.
#
#   ./make-j2me-jar.sh game.mpn ClassName decompiled/ClassName.java ["Title"] [out.jar]
#
# Needs a JDK 8 (javac -source 1.3 -target 1.3) and the J2ME class libraries + ProGuard of
# https://github.com/klaxons1/LLM-J2ME-SUITE (cloned automatically, or point J2ME_SUITE at a copy).
# ProGuard adds the CLDC stack maps (preverification) -- without them real phone KVMs refuse the jar.
# Writes out.jar and out.jad (default: ../release/ClassName-j2me.jar).
set -e
cd "$(dirname "$0")"
if [ $# -lt 3 ]; then
  echo "usage: $0 game.mpn ClassName decompiled.java [\"Title\"] [out.jar]" >&2; exit 2
fi
ROM=$(cd "$(dirname "$1")" && pwd)/$(basename "$1")
CLS=$2
SRC=$(cd "$(dirname "$3")" && pwd)/$(basename "$3")
TITLE=${4:-$CLS}
OUT=${5:-../release/$CLS-j2me.jar}
case "$OUT" in /*) ;; *) OUT=$(pwd)/$OUT ;; esac
JAD=${OUT%.jar}.jad

SUITE=${J2ME_SUITE:-$HOME/.cache/LLM-J2ME-SUITE}
if [ ! -f "$SUITE/uei/cldc11.jar" ]; then
  echo "fetching LLM-J2ME-SUITE into $SUITE"
  git clone --depth 1 https://github.com/klaxons1/LLM-J2ME-SUITE "$SUITE"
fi
BOOT="$SUITE/uei/cldc11.jar:$SUITE/uei/midp21.jar:$SUITE/uei/jsr135.jar"

B=$(pwd)/build/j2me
rm -rf "$B"; mkdir -p "$B/classes" "$B/jar"

echo "== desktop tools"
find src -name '*.java' > "$B/desktop-sources.txt"
javac -nowarn -encoding UTF-8 -d out @"$B/desktop-sources.txt"

echo "== assets (memory image, resources, font) + CLDC source"
java -Djava.awt.headless=true -cp out wolphun.decomp.j2me.J2meAssets "$ROM" "$CLS" "$SRC" \
     ../j2me/src/wolphun/j2me/Sdk.java "$B" "$TITLE"

echo "== javac -source 1.3 -target 1.3 (CLDC 1.1 / MIDP 2.0 / JSR-135)"
( find ../j2me/src -name '*.java'; find "$B/gen" -name '*.java' ) > "$B/sources.txt"
javac -nowarn -encoding UTF-8 -source 1.3 -target 1.3 -bootclasspath "$BOOT" -extdirs "" \
      -d "$B/classes" @"$B/sources.txt" 2>&1 | grep -v '^warning: \[options\]' | grep -v '^[0-9]* warnings$' || true
[ -f "$B/classes/wolphun/j2me/WMidlet.class" ] || { echo "compilation failed" >&2; exit 1; }

VER=1.0.0
cat > "$B/MANIFEST.MF" <<EOF
Manifest-Version: 1.0
MIDlet-Name: $TITLE
MIDlet-Vendor: wolphun
MIDlet-Version: $VER
MIDlet-Description: $TITLE (mophun game decompiled to Java by wolphun)
MIDlet-Icon: /icon.png
MIDlet-1: $TITLE, /icon.png, wolphun.j2me.WMidlet
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.0
EOF

echo "== jar + ProGuard (shrink, keep names readable, preverify)"
cp "$B/res/g.img" "$B/res/f.fnt" "$B/classes/"
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
MIDlet-Description: $TITLE (mophun game decompiled to Java by wolphun)
MIDlet-Icon: /icon.png
MIDlet-1: $TITLE, /icon.png, wolphun.j2me.WMidlet
MIDlet-Jar-URL: $(basename "$OUT")
MIDlet-Jar-Size: $SIZE
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.0
EOF
echo "built $OUT ($SIZE bytes) + $(basename "$JAD")"
