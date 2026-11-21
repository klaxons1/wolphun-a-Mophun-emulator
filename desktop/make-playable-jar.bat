@echo off
REM Builds a standalone, playable jar from a Mophun ROM (decompiled to Java + the emulator SDK).
REM   make-playable-jar.bat game.mpn ClassName ["Window title"] [out.jar]
REM Then play with any Java 8+ runtime:  java -jar ClassName.jar   (or double-click it)
if "%~2"=="" (echo usage: make-playable-jar.bat game.mpn ClassName ["Title"] [out.jar] & exit /b 1)
setlocal
set ROM=%~f1
set CLS=%~2
set TITLE=%~3
if "%TITLE%"=="" set TITLE=%CLS%
set OUT=%~f4
if "%~4"=="" set OUT=%CD%\%CLS%.jar
pushd "%~dp0"
if exist build\playjar rmdir /s /q build\playjar
mkdir build\playjar\classes build\playjar\src
dir /s /b src\*.java > build\playjar\sources.txt
javac --release 8 -nowarn -encoding UTF-8 -d build\playjar\classes @build\playjar\sources.txt 2>nul || javac -source 8 -target 8 -nowarn -encoding UTF-8 -d build\playjar\classes @build\playjar\sources.txt
if errorlevel 1 goto fail
java -cp build\playjar\classes wolphun.decomp.Main "%ROM%" build\playjar\src --class=%CLS%
if errorlevel 1 goto fail
javac --release 8 -nowarn -encoding UTF-8 -cp build\playjar\classes -d build\playjar\classes build\playjar\src\%CLS%.java 2>nul || javac -source 8 -target 8 -nowarn -encoding UTF-8 -cp build\playjar\classes -d build\playjar\classes build\playjar\src\%CLS%.java
if errorlevel 1 goto fail
copy /y assets\wolphun.png build\playjar\classes\ >nul
copy /y build\playjar\src\%CLS%.java build\playjar\classes\%CLS%.java.txt >nul
copy /y "%ROM%" build\playjar\classes\game.mpn >nul
(echo class=%CLS%& echo title=%TITLE%& echo rom=/game.mpn& echo width=128& echo height=160& echo scale=4) > build\playjar\classes\play.properties
echo Main-Class: wolphun.decomp.rt.Play> build\playjar\manifest.txt
if exist "%OUT%" del "%OUT%"
jar cfm "%OUT%" build\playjar\manifest.txt -C build\playjar\classes .
if errorlevel 1 goto fail
rmdir /s /q build\playjar
popd
echo OK -^> %OUT%
exit /b 0
:fail
popd
echo BUILD FAILED
exit /b 1
