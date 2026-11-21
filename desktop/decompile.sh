#!/bin/sh
# PIP2 -> Java decompiler (Linux/macOS). Usage:  ./decompile.sh game.mpn [ClassName]
# Decompiles to decompiled/<ClassName>.java and compiles it into decompiled/classes.
# Run the result with:  java -cp out:decompiled/classes wolphun.decomp.rt.RunDecompiled <ClassName> game.mpn
set -e
cd "$(dirname "$0")"
[ -n "$1" ] || { echo "usage: $0 game.mpn [ClassName]"; exit 1; }
if [ ! -f out/wolphun/decomp/Main.class ]; then
  mkdir -p out
  find src -name '*.java' > sources.txt
  javac -nowarn -encoding UTF-8 -d out @sources.txt
  rm -f sources.txt
fi
ROM="$1"; shift
case "$ROM" in /*) ;; *) ROM="$OLDPWD/$ROM" ;; esac
if [ -n "$1" ]; then java -cp out wolphun.decomp.Main "$ROM" decompiled --class="$1"
else java -cp out wolphun.decomp.Main "$ROM" decompiled; fi
mkdir -p decompiled/classes
javac -nowarn -encoding UTF-8 -cp out -d decompiled/classes decompiled/*.java
echo "OK -> desktop/decompiled/"
