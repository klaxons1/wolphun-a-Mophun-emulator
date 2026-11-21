@echo off
REM PIP2 -> Java decompiler. Usage:  decompile.bat game.mpn [ClassName]
REM   1. decompiles the ROM to decompiled\<ClassName>.java   (encrypted ROMs are decrypted first)
REM   2. compiles it against out\ into decompiled\classes
REM Then run the decompiled game with:  run-decompiled.bat <ClassName> game.mpn
REM Needs the emulator built first (build.bat).
if "%~1"=="" (echo usage: decompile.bat game.mpn [ClassName] & exit /b 1)
set CLS=
if not "%~2"=="" set CLS=--class=%~2
java -cp out wolphun.decomp.Main "%~1" decompiled %CLS%
if errorlevel 1 exit /b 1
if not exist decompiled\classes mkdir decompiled\classes
javac -nowarn -encoding UTF-8 -cp out -d decompiled\classes decompiled\*.java
if errorlevel 1 (echo the decompiled source does not compile & exit /b 1)
echo OK -^> decompiled\
