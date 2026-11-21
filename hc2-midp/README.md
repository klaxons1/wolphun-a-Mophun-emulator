# Honey Cave 2: native MIDP 2.0 port

A clean rewrite of Honey Cave 2 for **MIDP 2.0 / CLDC 1.1** (and JSR-135 for music). It has no
mophun SDK, no emulated memory and no `sys()` calls. The game logic was ported by hand from the
decompiled code in `../decompiled/HoneyCave2.java`.

Prebuilt: `release/HoneyCave2-midp.jar` + `.jad`.

## Layout

| path | what |
|---|---|
| `src/hc2/HC2.java` | the MIDlet |
| `src/hc2/Game.java` | the whole game: a `GameCanvas` with one game thread (75 ms tick), state machine, objects in parallel `int[]` arrays, menus, highscores, name entry, help, save and resume |
| `src/hc2/Res.java` | loads `d.bin`, the PNGs and one `mapN.bin` at a time; sprite table, `TiledLayer` maps with animated tiles |
| `tools/Hc2Assets.java` | build-time converter: original `.mpn` → `s.png b0-3.png t0-2.png f.png m0-7.mid d.bin map0-6.bin` |

How it is built:

- **Graphics.** Sprites are atlas regions drawn with `drawRegion`. Maps are `TiledLayer`s, and
  the 2- and 4-frame animated tiles use `createAnimatedTile`. The tile sheets hold only the tile
  variants the maps actually use (opaque, or with colour 0 transparent), with identical ones
  merged. That is 118 tiles instead of 226, so the decoded tile images are about half the size.
- **Screen.** Like the original, which reads the display size with `vGetCaps`, the layout
  follows the display: full width and full height, with the HUD at the bottom. The original
  capped the height at 216; the port doesn't, and when a level is shorter than the screen the
  camera keeps its bottom on the HUD so the sky continues above. Nothing is scaled and there are
  no borders. The game draws straight into the GameCanvas back buffer, so it keeps no
  framebuffer of its own in the heap and does no per-frame copy.
- **Heap.** Only the map being played is in memory: its `TiledLayer`, the collision attributes
  and the objects. The converter writes each map to its own `mapN.bin`, already numbered for the
  `TiledLayer`, and the cell bytes are dropped once the layer is filled. Before, all seven maps
  stayed in memory, about 36 KB. Only one boss atlas is loaded at a time.
- **Allocation.** The game loop does not allocate. The time of day for the sun and moon is read
  from `Calendar` once per level, then advanced from the tick counter.
- **Timing.** The tick follows the original (wait until the tick counter reaches `next`, then
  `next = now + 75`), but with a single `Thread.sleep` for the rest of the frame instead of
  waking every 5 ms.
- **Sound.** MIDI players are created on first use and stay prefetched. If a phone refuses
  another player, the others are closed and the new one is tried once more. A sound that still
  fails is not tried again.
- **Storage.** RMS stores `hc2-highscore` and `hc2-resume`. The game is saved when you quit
  during play and when SELECT is pressed during play. `pauseApp` (a call, for example) freezes
  the game until the phone returns to it.

## Keys

| key | action |
|---|---|
| 2 4 6 8 / joystick | move (1 3 7 9 = diagonals) |
| 5 / OK | fire (sting) and select |
| 0, #, right softkey | back |
| \*, left softkey | menu (saves the game for "Resume game") |

## Optional JAD properties

| property | effect |
|---|---|
| `HC2-Sound: off` | no music |
| `HC2-Vibra: off` | no vibration |

## Build

```
./make-jar.sh HoneyCave2.mpn            (or make-jar.bat on Windows)
```

You need:

- a JDK 8;
- [LLM-J2ME-SUITE](https://github.com/klaxons1/LLM-J2ME-SUITE) for the CLDC/MIDP/MMAPI class
  libraries and ProGuard. It is cloned automatically, or you can set `J2ME_SUITE`.

The build compiles with `-source 1.3 -target 1.3` and runs ProGuard to add the CLDC stack maps.
The ROM is only read at build time and is not part of the repository.

Note: the mophun emulators in this repository pad `vutoa` numbers on the right, so 2 lives show
as "20". This port pads with leading zeros ("02", "001234"). That is what the game clearly
intends; the highscore table would otherwise print level 3 as "30".
