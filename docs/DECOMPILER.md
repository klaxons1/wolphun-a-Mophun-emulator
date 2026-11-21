# PIP2 -> Java decompiler (`wolphun.decomp`)

Turns the bytecode of a Mophun game (`.mpn`, see [MPN_FORMAT.md](MPN_FORMAT.md)) into a single
readable Java class. The class **compiles and runs**: memory, the SDK and function pointers go
through a small runtime (`wolphun.decomp.rt.Pip2Runtime`) that is wired to the emulator's own
SDK implementation.

```sh
cd desktop
./decompile.sh  path/to/HoneyCave2.mpn HoneyCave2      # -> decompiled/HoneyCave2.java (+ classes/)
java -cp out:decompiled/classes wolphun.decomp.rt.RunDecompiled HoneyCave2 path/to/HoneyCave2.mpn
./verify-decompiled.sh path/to/HoneyCave2.mpn HoneyCave2 6000     # differential test
```
On Windows, use `decompile.bat` and `run-decompiled.bat`. Encrypted Sony Ericsson ROMs are decrypted
in memory first. Options of `wolphun.decomp.Main`:
* `--class=Name` / `--pkg=a.b`: name of the generated class.
* `--addr`: annotate every basic block with its bytecode address.
* `--only=0xADDR`: decompile only one function.

## What the output looks like
From Honey Cave 2 (`main`, at 0x10074):
```java
void main() {
    int fp = 0, p0 = 0, r0 = 0;
    sp -= 12;
    if ((fun_12dd4() & 0xFF) != 0) {
        vTerminateVMGP();
        throw terminated();
    }
    fp = vGetTickCount();
    L_10090: while (true) {
        ...
        icall(i32(g_19128), p0);              // current game state, through a function pointer
        vFlipScreen(0);
        while (true) {
            if (leu(fp, vGetTickCount())) {   // 75 ms frame throttle
                fp = vGetTickCount() + 75;
                continue L_10090;
            }
        }
    }
}
```
* Registers become Java locals named after the ABI (`p0..p3` arguments, `r0` result, `s0..s7`/`fp`,
  `g0..g13`). `sp` is a field: stack frames stay in the game's memory, because games pass
  addresses of stack buffers to the SDK.
* Memory accesses: `u8/s8/u16/s16/i32(addr)` read, `put8/put16/put32(addr, v)` write. Global data
  appears as `g_XXXXX` constants. String arguments of SDK calls are shown in a comment.
* `ltu/leu/gtu/geu/divu` are unsigned operations. `setB/setH` replace the low byte/half of a register
  (PIP2 `.b`/`.h` instructions) and only appear where the upper bits really matter.
* `icall(fn, ...)` dispatches function pointers to the address-taken methods.

For Honey Cave 2 (32 KB of bytecode) the decompiler finds 116 functions and writes about 4 700
lines in a quarter of a second.

## How it works
| stage | class | |
|-------|-------|--|
| load | `Image` | parse VMGP, decrypt, lay out sections exactly like the interpreter, relocate the pool |
| discover | `Program` | decode, find functions (entry, CALLl targets, code addresses used as values, relocated code pointers), recognise switch tables (`SLLi; LDW [r+TABLE]; JPr` plus the bound check), build CFGs; a jump into another function is a tail call |
| lift | `Lifter` | every opcode -> a tiny IR (`Ir`: Assign/Store/Eval statements, Bin/Un/Load/SetPart/Call expressions, GOTO/IF/SWITCH/RETURN/HALT terminators); STORE/RET register saves are dropped |
| analyse | `Analysis` | interprocedural fixpoint for parameters (registers live at entry) and return values (r0 used by some caller); **width-aware liveness**: an SDK `byte`/`short` parameter, or a callee that only reads the low byte, does not keep the upper bits alive, which removes almost all partial-register noise; then dead code removal, constant folding, signed/unsigned compare cleanup and single-use expression inlining |
| structure | `Structurer` | dominator-tree translation into labelled blocks/loops ("Beyond Relooper", N. Ramsey 2022), then a prettifier: while/do-while conditions, if/else shaping, guard clauses, dead labels; irreducible CFGs fall back to a `switch (state)` dispatcher |
| print | `JavaWriter` | Java source with precedence-aware parentheses, compound assignments, SDK stubs |

Indirect calls (`CALLr`) can only reach address-taken functions, so they pass as many argument
registers as the widest address-taken function reads.

## Verification
`RunDecompiled` runs either the decompiled class or (`--interp`) the original bytecode, with:
* the same screen and SDK;
* a deterministic clock (`VTICK`);
* scripted keys clocked by `vGetButtonData` calls, because menus often poll the keypad without
  flipping the screen.

Both modes write the same log, every SDK call with its arguments and result, plus a CRC of every
frame. `verify-decompiled.sh` runs both and compares them. For Honey Cave 2, the two logs are
**identical through 8 000 frames**: menus, level start, walking and jumping, sprite collisions,
sound effects, vibration and save-file writes (over 600 000 SDK calls, every frame bit-identical).
41 of the game's 50 SDK imports are exercised.

## Limitations
* Cooperative tasks (`vCreateTask`, `SLEEP`, `KILLTASK` from a task) are not supported by the runtime
  (Honey Cave 2 does not use them).
* The output is literal: there are no struct types, so field accesses are `u16(g0 + 10)`, and no
  names except `start`/`main` and exported symbols. Some redundant masks remain (`(x & 0xFF) & 0xFF`
  across assignments).
* Function pointers outside the recognised patterns (computed code addresses) end in
  `badPointer()`. Code that is reached only that way is not discovered.
* Only tested in depth on Honey Cave 2. Other games may hit unrecognised switch patterns
  (an indirect `JPr` that is not a switch becomes an indirect tail call) or SDK functions with
  unknown prototypes (assumed to take 4 arguments and return a value).
