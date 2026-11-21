@echo off
rem Builds the native MIDP 2.0 / CLDC 1.1 (+ JSR-135) version of Honey Cave 2.
rem
rem   make-jar.bat HoneyCave2.mpn [out.jar]
rem
rem Needs a JDK 8 on PATH and a copy of https://github.com/klaxons1/LLM-J2ME-SUITE
rem (set J2ME_SUITE=C:\path\LLM-J2ME-SUITE, or it is cloned with git into %USERPROFILE%\.cache).
rem The pictures, maps, texts and sounds are converted from the original .mpn at build time.
rem ProGuard adds the CLDC stack maps (preverification): real phone KVMs need them.
setlocal EnableDelayedExpansion
cd /d "%~dp0"
if "%~1"=="" (
  echo usage: %~nx0 HoneyCave2.mpn [out.jar]
  exit /b 2
)
set "ROM=%~f1"
set "OUT=%~f2"
if "%~2"=="" set "OUT=%~dp0..\release\HoneyCave2-midp.jar"
for %%F in ("%OUT%") do set "OUT=%%~fF" & set "JAD=%%~dpnF.jad" & set "JARNAME=%%~nxF"
set "TITLE=Honey Cave 2"

if "%J2ME_SUITE%"=="" set "J2ME_SUITE=%USERPROFILE%\.cache\LLM-J2ME-SUITE"
if not exist "%J2ME_SUITE%\uei\cldc11.jar" (
  echo fetching LLM-J2ME-SUITE into %J2ME_SUITE%
  git clone --depth 1 https://github.com/klaxons1/LLM-J2ME-SUITE "%J2ME_SUITE%" || exit /b 1
)
set "U=%J2ME_SUITE%\uei"
set "BOOT=%U%\cldc11.jar;%U%\midp21.jar;%U%\jsr135.jar"

set "B=%~dp0build"
if exist "%B%" rmdir /s /q "%B%"
mkdir "%B%\tools"
mkdir "%B%\classes"

echo == asset converter
dir /s /b ..\desktop\src\*.java > "%B%\desktop-sources.txt"
if not exist ..\desktop\out mkdir ..\desktop\out
javac -nowarn -encoding UTF-8 -d ..\desktop\out @"%B%\desktop-sources.txt" || exit /b 1
javac -nowarn -encoding UTF-8 -cp ..\desktop\out -d "%B%\tools" tools\Hc2Assets.java || exit /b 1
java -Djava.awt.headless=true -cp "..\desktop\out;%B%\tools" Hc2Assets "%ROM%" "%B%\classes" || exit /b 1

echo == javac -source 1.3 -target 1.3
dir /s /b src\*.java > "%B%\sources.txt"
javac -nowarn -encoding UTF-8 -source 1.3 -target 1.3 -bootclasspath "%BOOT%" -extdirs "" -d "%B%\classes" @"%B%\sources.txt"
if not exist "%B%\classes\hc2\Game.class" ( echo compilation failed & exit /b 1 )

set VER=1.0.0
(
echo Manifest-Version: 1.0
echo MIDlet-Name: %TITLE%
echo MIDlet-Vendor: wolphun
echo MIDlet-Version: %VER%
echo MIDlet-Description: %TITLE% ^(native MIDP 2.0 port^)
echo MIDlet-Icon: /icon.png
echo MIDlet-1: %TITLE%, /icon.png, hc2.HC2
echo MicroEdition-Configuration: CLDC-1.1
echo MicroEdition-Profile: MIDP-2.0
) > "%B%\MANIFEST.MF"

echo == jar + ProGuard (preverify)
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
echo MIDlet-Description: %TITLE% ^(native MIDP 2.0 port^)
echo MIDlet-Icon: /icon.png
echo MIDlet-1: %TITLE%, /icon.png, hc2.HC2
echo MIDlet-Jar-URL: %JARNAME%
echo MIDlet-Jar-Size: !SIZE!
echo MicroEdition-Configuration: CLDC-1.1
echo MicroEdition-Profile: MIDP-2.0
) > "%JAD%"
echo built %OUT% (!SIZE! bytes)
endlocal
