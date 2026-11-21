@echo off
REM Runs a decompiled game class with the emulator's screen and SDK.
REM Usage:  run-decompiled.bat ClassName game.mpn [--headless --frames=N --hash --trace=file --keys=...]
REM         run-decompiled.bat --interp  game.mpn [...]     (same, but in the PIP2 interpreter)
java -cp out;decompiled\classes wolphun.decomp.rt.RunDecompiled %*
