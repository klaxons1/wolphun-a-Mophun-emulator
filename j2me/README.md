# J2ME port of decompiled games

`release/HoneyCave2-j2me.jar` (+ `.jad`) is **Honey Cave 2** as a MIDlet for ordinary Java phones
(MIDP 2.0, CLDC 1.1, JSR-135 for sound). The jar is about 100 KB and needs roughly 0.5 MB of heap.
It does not contain an emulator or interpreter. The game is the Java class produced by the PIP2 →
Java decompiler ([docs/DECOMPILER.md](../docs/DECOMPILER.md)), running on a small mophun SDK written
for MIDP.

## Controls

| phone key | mophun button |
|---|---|
| joystick / 2 4 6 8 | up, left, right, down |
| 1 3 7 9 | diagonals (1/3 = jump left/right) |
| 5, joystick centre | FIRE |
| left softkey, `*` | SELECT (menu / pause) |
| right softkey, `#`, `0` | FIRE2 |

The game is paused, with the clock frozen, while the phone shows something else. Saves are kept in RMS.

## Screen

Honey Cave 2 is a "multiscreen" mophun game: it lays itself out for the size that `vGetCaps` reports.
The MIDlet reports **176x208** when the screen is at least that big, **128x160** on smaller screens and
**128x128** on screens shorter than 160 pixels. If the picture can be at least 25% bigger, it is
stretched to fill the screen while keeping its proportions (for example 240x320 shows 239x283).
You can change this with optional JAD attributes:

```
Wolphun-Screen: 128x160     force a game resolution
Wolphun-Scale: off          never stretch
Wolphun-Sound: off          start muted
```

## Building

```
desktop/make-j2me-jar.sh  game.mpn HoneyCave2 decompiled/HoneyCave2.java "Honey Cave 2"
desktop\make-j2me-jar.bat game.mpn HoneyCave2 decompiled\HoneyCave2.java "Honey Cave 2"
```

You need JDK 8. The J2ME class libraries and ProGuard come from
[LLM-J2ME-SUITE](https://github.com/klaxons1/LLM-J2ME-SUITE). The script clones it into `~/.cache`,
or you can set `J2ME_SUITE` to point at your own copy. The script works in these steps:

1. `wolphun.decomp.j2me.J2meAssets` (a desktop program) runs `wolphun.vm.Pip2` once to lay out the
   address space. It writes the following files:
   * `g.img`: code, relocated data and bss, the layout constants and the container's resources.
     The phone never parses or decrypts the `.mpn`.
   * `f.fnt`: the desktop system font, rendered at several heights through the same `Screen.drawText`
     path the desktop runtime uses, stored as 1-bit glyphs. Text therefore lays out identically.
   * `gen/wolphun/game/HoneyCave2.java`: the decompiled class made Java 1.3-clean. Annotations are
     removed, and every `sys("vName", ...)` becomes `sys(Sdk.vName, ...)` with the prototype's
     byte/short narrowing built in, so there is no string switch at run time.
2. `javac -source 1.3 -target 1.3` against `cldc11.jar`, `midp21.jar` and `jsr135.jar`.
3. ProGuard `-microedition` adds the CLDC `StackMap` attributes. This is the preverify step that real
   phone KVMs require. Names stay readable (`-dontobfuscate`).
4. The jar and JAD are written with the correct `MIDlet-Jar-Size`.

## Sources

* `src/wolphun/decomp/rt/Pip2Runtime.java`: the CLDC twin of the desktop base class. It provides the
  `byte[]` address space, `u8/u16/i32/put*`, `memcpy` with the same overlap semantics, the unsigned
  helpers and `sys`.
* `src/wolphun/j2me/Sdk.java`: the mophun functions that 2D games use, ported from
  `desktop/src/wolphun/vm/Sdk.java` with the same rules. That covers sprites and sprite slots,
  collisions, the tile map snapshot and animation, the game bitmap font and the system font, the LZ
  decompressor, resource and file streams (saved to RMS), MIDI through MMAPI and vibration. Pixel loops
  index the memory array directly, and nothing is allocated per frame.
* `src/wolphun/j2me/WCanvas.java`: the full-screen `GameCanvas` and the single game thread. It handles
  `drawRGB`/`flushGraphics`, the optional nearest-neighbour fit, keys, pause and resume. While the game
  busy-waits on `vGetTickCount` between frames, the thread sleeps instead of spinning.
* `src/wolphun/j2me/WMidlet.java`: the MIDlet.

## Verification

The jar's own classes were run on a desktop JVM through small stand-ins for the few MIDP classes they
use. These runs used a virtual clock and a scripted key sequence, the same `VTICK=20` and `--keys`
setup as `RunDecompiled`. Every frame's CRC was compared with the desktop decompiled runtime, which is
itself frame-identical to the interpreter:

| resolution | frames | differing |
|---|---|---|
| 176x208 | 8000 | 0 |
| 128x160 | 8000 | 0 |
| 128x128 | 6000 | 0 |

In real time the game keeps its own 75 ms frame pace (200 frames in 14.9 s). The deepest emulated stack
seen was 224 bytes, and the build reserves 16 KB. The game never calls `vNewPtr`, and the build
reserves a 16 KB heap anyway.

This check did not use a real J2ME emulator or a phone; freej2me-plus is the suggested next step.
