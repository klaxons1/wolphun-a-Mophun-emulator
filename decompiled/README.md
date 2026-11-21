# Decompiled games

`HoneyCave2.java` is the output of the PIP2 -> Java decompiler (`wolphun.decomp`, see
[docs/DECOMPILER.md](../docs/DECOMPILER.md)) for `HoneyCave2_[v1.0]_[multiscreen].mpn`,
generated with:

```sh
java -cp desktop/out wolphun.decomp.Main "HoneyCave2_[v1.0]_[multiscreen].mpn" decompiled --class=HoneyCave2
```

It compiles against the emulator (`desktop/out`) and runs with
`wolphun.decomp.rt.RunDecompiled HoneyCave2 <rom.mpn>`. The ROM is still needed at run time,
for its data section and resources. `desktop/verify-decompiled.sh` compares it with the PIP2
interpreter: the SDK-call log and every frame checksum are identical (6 000 and 8 000 frame runs).
