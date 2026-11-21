#!/bin/sh
# Differential test of the decompiler: runs the ROM in the PIP2 interpreter and the decompiled
# Java class with the same scripted input and deterministic clock, then compares the log of
# every SDK call and a checksum of every frame.
#   ./verify-decompiled.sh game.mpn ClassName [frames] [keys]
# (decompile first with ./decompile.sh game.mpn ClassName)
set -e
cd "$(dirname "$0")"
ROM="$1"; CLS="$2"; FRAMES="${3:-3000}"
case "$ROM" in /*) ;; *) ROM="$OLDPWD/$ROM" ;; esac
# default input: FIRE every 80 key polls (menus), then walk right/left/jump in long stretches
KEYS="${4:-every:80:16:3,3000:8:1500,5500:4:1500,8000:9:1500,10500:2:1500,13000:8:1500,15500:4:1500,18000:9:1500}"
T=$(mktemp -d)
export VTICK=20
CP="out:decompiled/classes"
java -cp "$CP" wolphun.decomp.rt.RunDecompiled --interp "$ROM" --headless --frames="$FRAMES" --keys="$KEYS" \
     --hash --trace="$T/interp.txt" --saves="$T/s1" > "$T/interp.frames"
java -cp "$CP" wolphun.decomp.rt.RunDecompiled "$CLS" "$ROM" --headless --frames="$FRAMES" --keys="$KEYS" \
     --hash --trace="$T/decomp.txt" --saves="$T/s2" > "$T/decomp.frames"
tail -n 1 "$T/interp.frames"; tail -n 1 "$T/decomp.frames"
ok=1
if cmp -s "$T/interp.txt" "$T/decomp.txt"; then echo "SDK call log: identical ($(wc -l < "$T/interp.txt") lines)"
else echo "SDK call log: DIFFERENT"; diff "$T/interp.txt" "$T/decomp.txt" | head -20; ok=0; fi
grep '^frame' "$T/interp.frames" > "$T/f1"; grep '^frame' "$T/decomp.frames" > "$T/f2"
if cmp -s "$T/f1" "$T/f2"; then echo "frames: identical ($(wc -l < "$T/f1") frame checksums)"
else echo "frames: DIFFERENT"; ok=0; fi
echo "logs in $T"
[ $ok = 1 ]
