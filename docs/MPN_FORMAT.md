# The `.mpn` file (Mophun VMGP executable)

A Mophun game is a single `.mpn` file: a **VMGP** executable for the **PIP2** virtual machine
(a small 32-bit RISC designed by Synergenix for phones such as the Sony Ericsson T300/T310/T610).
The file contains the bytecode, the initialized data, the game's resources (graphics, maps,
sounds, fonts) and the relocation/import table. The phone's runtime loads it, relocates it,
links the SDK calls by name and jumps to the first instruction.

Everything below is what `wolphun.vm.Vmgp`, `wolphun.vm.Pip2` and `mophun.MophunCrypt` implement,
illustrated with *Honey Cave 2* (`HoneyCave2_[v1.0]_[multiscreen].mpn`, 64 641 bytes).

## 1. Layout

```
+0      header (40 bytes)
+40     code       codeSize bytes      PIP2 bytecode (encrypted in Sony Ericsson releases)
        data       dataSize bytes      initialized globals
        resources  resSize bytes       offset table + resource blobs
        pool       poolCount * 8       relocations, imports, exported symbols, constants
        strings    stringSize bytes    NUL-terminated names used by the pool
```
`bss` (zeroed globals), heap and stack have sizes in the header but no bytes in the file.

### Header (little-endian)

| off | size | field | Honey Cave 2 |
|----:|-----:|-------|--------------|
| 0   | 4 | magic `VMGP` | |
| 4   | 4 | heap size / 4 | 256 (1 KB requested; the emulator gives at least 4 MB) |
| 8   | 2 | flags | 0x40 |
| 10  | 2 | stack size / 4 | 0 (runtime default) |
| 12  | 4 | code size | 32 408 |
| 16  | 4 | data size | 3 672 |
| 20  | 4 | bss size | 44 688 |
| 24  | 4 | resource section size | 23 472 |
| 28  | 4 | non-zero = LZ-packed code+data (S60/PPC builds; not supported) | 0 |
| 32  | 4 | pool item count | 556 |
| 36  | 4 | string table size | 599 |

### Memory map after loading
The loader places the sections one after another from **0x10000**, each 16-byte aligned:

```
0x10000  code     0x10000 .. 0x17e98
0x17ea0  data     0x17ea0 .. 0x18cf8
0x18d00  bss      0x18d00 .. 0x23b90
0x23b90  heap     (vNewPtr allocates here)
         stack    grows down from the top; sp starts at the very end
```
The entry point is the first instruction of the code section.

### Resources
The resource section starts with a table of `u32` offsets (relative to the section start),
terminated by 0; resource *i* runs from its offset to the next one. Games read them through
`vStreamOpen` / `vStreamRead` / `vDecompress`, and play sounds with `vPlayResource`. Honey Cave 2
has 18 resources. Some later games also embed a ZIP archive used as a small file system.

### The pool (8 bytes per item)
```
u32 info:  bits 0-3 type, bits 4-7 section ("item target": 1 code, 2 data, 4 bss), bits 8-31 meta
u32 target
```
| type | name | meaning |
|-----:|------|---------|
| 1 | Local | address `section_base + target` (code labels, globals) |
| 2 | Import | SDK function, name = `strings[meta]`; linked by name at load time |
| 3 | Global | exported symbol (`~C` = list of C++ static constructors, `~D` destructors) |
| 4 | SectionRelative | relocation: the word at `section(itemTarget)+target` gets `base(meta)` added |
| 5/6 | Swap16/32 | byte-order fix-ups (no-op on a little-endian host) |
| 7 | Const32 | 32-bit constant |
| 8 | SymAdd | `pool[meta-1] + target` (address of a field inside a global, etc.) |

Honey Cave 2: 233 Local, 50 Import, 1 Global, 155 SectionRelative, 12 Swap, 9 Const32,
95 SymAdd. The 50 imports are SDK functions such as `vClearScreen`, `vDrawObject`,
`vSpriteSet`, `vMapInit`, `vUpdateMap`, `vGetButtonData`, `vPlayResource`, `vTextOutU`, `vFlipScreen`.

## 2. Encryption (Sony Ericsson releases)
Games sold through Sony Ericsson have an **encrypted code section** (data, resources and pool are
plain). Each 32-bit word of code is decrypted with a 32-bit XTEA variant. The 128-bit key is
stored in the game's `META` resource, RSA-encrypted with exponent 3 against one of the Sony Ericsson
public moduli: `key = (META[0:0x80]^3 mod N)[0x14:0x24]`. `mophun.MophunCrypt` holds the moduli and
decrypts in memory. `GameLibrary.looksEncrypted` detects encryption from the byte diversity of the
code: XTEA output looks like noise, while bytecode has a very skewed distribution. For Honey Cave 2,
32 282 of the 32 408 code bytes change after decryption; everything else is identical.

## 3. PIP2 bytecode

**Registers** (33 x 32 bit; an instruction names a register by its byte offset, `index*4`):

| index | name | role |
|------:|------|------|
| 0 | zero | always 0 |
| 1 | sp | stack pointer |
| 2 | ra | return address |
| 3 | fp | frame pointer / callee-saved |
| 4-11 | s0-s7 | callee-saved |
| 12-15 | p0-p3 | arguments 1-4 (more on the stack at `[sp]`) |
| 16-29 | g0-g13 | temporaries |
| 30-31 | r0, r1 | return value |
| 32 | pc | program counter |

**Encoding**: one little-endian word `op | d<<8 | s<<16 | t<<24`. Opcodes 0x4A..0x66 are followed by an
extra word: if its top bit is set it is a raw constant `(w & 0x7FFFFFFF) | ((w<<1) & 0x80000000)`,
otherwise it is a **1-based pool index**. All addresses (globals, call targets, function pointers)
therefore go through the pool, which is what makes the file relocatable.

| opcodes | instructions |
|---------|--------------|
| 0x02-0x11 | ADD AND MUL DIV DIVU OR XOR SUB SLL SRA SRL NOT NEG EXSB EXSH MOV (register forms) |
| 0x12-0x1B | ADDB SUBB ANDB ORB MOVB / ADDH SUBH ANDH ORH MOVH: write only the low byte/half of `d` |
| 0x1C-0x2B | shifts and small-immediate forms (ADDQ, MULQ, ADDBi, ANDHi ...) with the immediate in `t` |
| 0x2C-0x3F | compare-with-immediate branches (`BEQI d, s` ... `BLTUIB`): target `pc + sx8(t)*4` |
| 0x40 | LDQ: `d` = signed 16-bit constant held in the `s` and `t` fields |
| 0x41 / 0x42 | JPr / CALLr: indirect jump and call (switch tables, function pointers, `JPr ra` = return) |
| 0x43-0x45 | STORE / RESTORE / RET: push/pop a register range (function prologue/epilogue) |
| 0x46 / 0x47 | KILLTASK / SLEEP: cooperative tasks (`vCreateTask`) |
| 0x48 / 0x49 | SYSCPY / SYSSET: memcpy / memset |
| 0x4A-0x51 | ADDi ANDi MULi DIVi DIVUi ORi XORi SUBi with a 32-bit immediate |
| 0x52-0x59 | STB STH STW / LDB LDH LDW LDBU LDHU `[s + imm]` |
| 0x5A | LDI `d = imm` |
| 0x5B / 0x5C | JPl / CALLl: direct jump/call; a CALLl through an Import item is an SDK call |
| 0x5D-0x66 | register-compare branches BEQ BNE BGE BGEU BGT BGTU BLE BLEU BLT BLTU: target `pc + imm` |

**Calling convention**: arguments in p0..p3, extra arguments at `[sp+0]`, `[sp+4]`, ...; result in r0.
A typical function is `STORE ra..fp` / `ADDQ sp,-N` / body / `ADDQ sp,+N` / `RET ra..fp`. SDK calls use
the same convention: the runtime reads p0..p3 and writes r0.

**Program shape** (Honey Cave 2, decompiled): `start` calls `main`, then `vTerminateVMGP`. `main`
initializes the game and loops forever. Each iteration reads the keys and calls the current
*state function* through a pointer at `0x19128` (title, menu, game, highscores...), then calls
`vFlipScreen` and waits until 75 ms have passed since the previous frame.

## 4. Tools in this repository
* `java -cp out wolphun.vm.Disasm game.mpn 0x10000 40`: disassembler (`--xref name` lists SDK call sites).
* `java -cp out wolphun.decomp.Main game.mpn outdir`: PIP2 -> Java decompiler, see [DECOMPILER.md](DECOMPILER.md).
