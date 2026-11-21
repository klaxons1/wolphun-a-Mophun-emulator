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
| `src/hc2/Res.java` | loads `d.bin` and the PNGs; sprite table, `TiledLayer` maps with animated tiles, collision attributes |
| `tools/Hc2Assets.java` | build-time converter: original `.mpn` → `s.png b0-3.png t0-2.png f.png m0-7.mid d.bin` |

How it is built:

- **Graphics.** Sprites are atlas regions drawn with `drawRegion`. Maps are `TiledLayer`s. Tile
  set *n+k+1* is the transparent variant of tile *k*, and the 2- and 4-frame animated tiles use
  `createAnimatedTile`.
- **Screen.** The game picks the original layout for the screen: 176x208, 128x160 or 128x128. It
  draws into an off-screen image and scales up (nearest neighbour) when that makes the picture at
  least 25% bigger.
- **Allocation.** The game loop does not allocate.
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
| `HC2-Screen: 176x208` | force a layout |
| `HC2-Scale: off` | no up-scaling |
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
