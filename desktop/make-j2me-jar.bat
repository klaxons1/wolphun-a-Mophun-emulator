@echo off
rem Builds a J2ME (MIDP 2.0 / CLDC 1.1 + MMAPI) MIDlet of a decompiled mophun game.
rem
rem   make-j2me-jar.bat game.mpn ClassName decompiled\ClassName.java ["Title"] [out.jar]
rem
rem Needs a JDK 8 on PATH and a copy of https://github.com/klaxons1/LLM-J2ME-SUITE
rem (set J2ME_SUITE=C:\path\LLM-J2ME-SUITE, or it is cloned with git into %USERPROFILE%\.cache).
rem ProGuard adds the CLDC stack maps (preverification): real phone KVMs need them.
setlocal EnableDelayedExpansion
cd /d "%~dp0"
if "%~3"=="" (
  echo usage: %~nx0 game.mpn ClassName decompiled.java ["Title"] [out.jar]
  exit /b 2
)
set "ROM=%~f1"
set "CLS=%~2"
set "SRC=%~f3"
set "TITLE=%~4"
if "%TITLE%"=="" set "TITLE=%CLS%"
set "OUT=%~f5"
if "%~5"=="" set "OUT=%~dp0..\release\%CLS%-j2me.jar"
for %%F in ("%OUT%") do set "OUT=%%~fF" & set "JAD=%%~dpnF.jad" & set "JARNAME=%%~nxF"

if "%J2ME_SUITE%"=="" set "J2ME_SUITE=%USERPROFILE%\.cache\LLM-J2ME-SUITE"
if not exist "%J2ME_SUITE%\uei\cldc11.jar" (
  echo fetching LLM-J2ME-SUITE into %J2ME_SUITE%
  git clone --depth 1 https://github.com/klaxons1/LLM-J2ME-SUITE "%J2ME_SUITE%" || exit /b 1
)
set "U=%J2ME_SUITE%\uei"
set "BOOT=%U%\cldc11.jar;%U%\midp21.jar;%U%\jsr135.jar"

set "B=%~dp0build\j2me"
if exist "%B%" rmdir /s /q "%B%"
mkdir "%B%\classes"

echo == desktop tools
dir /s /b src\*.java > "%B%\desktop-sources.txt"
javac -nowarn -encoding UTF-8 -d out @"%B%\desktop-sources.txt" || exit /b 1

echo == assets (memory image, resources, font) + CLDC source
java -Djava.awt.headless=true -cp out wolphun.decomp.j2me.J2meAssets "%ROM%" "%CLS%" "%SRC%" ..\j2me\src\wolphun\j2me\Sdk.java "%B%" "%TITLE%" || exit /b 1

echo == javac -source 1.3 -target 1.3
dir /s /b ..\j2me\src\*.java > "%B%\sources.txt"
dir /s /b "%B%\gen\*.java" >> "%B%\sources.txt"
javac -nowarn -encoding UTF-8 -source 1.3 -target 1.3 -bootclasspath "%BOOT%" -extdirs "" -d "%B%\classes" @"%B%\sources.txt"
if not exist "%B%\classes\wolphun\j2me\WMidlet.class" ( echo compilation failed & exit /b 1 )

set VER=1.0.0
(
echo Manifest-Version: 1.0
echo MIDlet-Name: %TITLE%
echo MIDlet-Vendor: wolphun
echo MIDlet-Version: %VER%
echo MIDlet-Description: %TITLE% ^(mophun game decompiled to Java by wolphun^)
echo MIDlet-Icon: /icon.png
echo MIDlet-1: %TITLE%, /icon.png, wolphun.j2me.WMidlet
echo MicroEdition-Configuration: CLDC-1.1
echo MicroEdition-Profile: MIDP-2.0
) > "%B%\MANIFEST.MF"

echo == jar + ProGuard (preverify)
copy /y "%B%\res\g.img" "%B%\classes\" >nul
copy /y "%B%\res\f.fnt" "%B%\classes\" >nul
copy /y ..\j2me\res\icon.png "%B%\classes\icon.png" >nul
pushd "%B%\classes" && jar cfm ..\raw.jar ..\MANIFEST.MF . && popd
(
echo -injars "%B%\raw.jar"
echo -outjars "%B%\pre.jar"
echo -libraryjars "%U%\cldc11.jar"
echo -libraryjars "%U%\midp21.jar"
echo -libraryjars "%U%\jsr135.jar"
echo -microedition
echo -target 1.3
echo -dontoptimize
echo -dontobfuscate
echo -dontwarn
echo -dontnote
echo -keep public class * extends javax.microedition.midlet.MIDlet
) > "%B%\proguard.pro"
java -jar "%J2ME_SUITE%\libs\proguard.jar" @"%B%\proguard.pro" || exit /b 1

mkdir "%B%\jar"
pushd "%B%\jar" && jar xf ..\pre.jar && rmdir /s /q META-INF && jar cfm "%OUT%" ..\MANIFEST.MF . && popd

for %%F in ("%OUT%") do set SIZE=%%~zF
(
echo MIDlet-Name: %TITLE%
echo MIDlet-Vendor: wolphun
echo MIDlet-Version: %VER%
echo MIDlet-Description: %TITLE% ^(mophun game decompiled to Java by wolphun^)
echo MIDlet-Icon: /icon.png
echo MIDlet-1: %TITLE%, /icon.png, wolphun.j2me.WMidlet
echo MIDlet-Jar-URL: %JARNAME%
echo MIDlet-Jar-Size: !SIZE!
echo MicroEdition-Configuration: CLDC-1.1
echo MicroEdition-Profile: MIDP-2.0
) > "%JAD%"
echo built %OUT% (!SIZE! bytes)
endlocal
