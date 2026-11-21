#!/bin/sh
# Builds a standalone, playable jar from a Mophun ROM: decompiles the game to Java, compiles it
# and packs it with the emulator's SDK/runtime. Run the result with just a Java 8+ runtime:
#     java -jar HoneyCave2.jar          (or double-click it)
#
#   ./make-playable-jar.sh game.mpn ClassName ["Window title"] [out.jar] [--no-rom]
#
# The ROM is bundled in the jar (the decompiled code still reads its data section and its
# graphics/sounds from it). With --no-rom it is not: pass it on the command line instead,
# java -jar Game.jar game.mpn
set -e
cd "$(dirname "$0")"
[ -n "$2" ] || { echo "usage: $0 game.mpn ClassName [\"Title\"] [out.jar] [--no-rom]"; exit 1; }
ROM="$1"; CLS="$2"; TITLE="${3:-$2}"; OUT="${4:-$CLS.jar}"; BUNDLE=1
[ "$5" = "--no-rom" ] && BUNDLE=0
case "$ROM" in /*) ;; *) ROM="$OLDPWD/$ROM" ;; esac
case "$OUT" in /*) ;; *) OUT="$OLDPWD/$OUT" ;; esac

# Java 8 bytecode, whatever the JDK version
if javac --release 8 -version >/dev/null 2>&1; then J8="--release 8"; else J8="-source 8 -target 8"; fi

echo "[1/4] compiling the emulator runtime"
rm -rf build/playjar && mkdir -p build/playjar/classes build/playjar/src
find src -name '*.java' > build/playjar/sources.txt
javac $J8 -nowarn -encoding UTF-8 -d build/playjar/classes @build/playjar/sources.txt

echo "[2/4] decompiling $(basename "$ROM")"
java -cp build/playjar/classes wolphun.decomp.Main "$ROM" build/playjar/src --class="$CLS"

echo "[3/4] compiling the decompiled game"
javac $J8 -nowarn -encoding UTF-8 -cp build/playjar/classes -d build/playjar/classes build/playjar/src/"$CLS".java

echo "[4/4] packing"
S=build/playjar/classes
cp assets/wolphun.png $S/ 2>/dev/null || true
cp build/playjar/src/"$CLS".java $S/"$CLS".java.txt       # the decompiled source, for the curious
{
  echo "class=$CLS"
  echo "title=$TITLE"
  echo "rom=/game.mpn"
  echo "width=128"
  echo "height=160"
  echo "scale=4"
} > $S/play.properties
[ $BUNDLE = 1 ] && cp "$ROM" $S/game.mpn
printf 'Main-Class: wolphun.decomp.rt.Play\n' > build/playjar/manifest.txt
rm -f "$OUT"
jar cfm "$OUT" build/playjar/manifest.txt -C $S .
rm -rf build/playjar
echo "OK -> $OUT   (run: java -jar \"$(basename "$OUT")\")"
