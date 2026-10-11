@echo off
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
javac -encoding UTF-8 --release 17 -d build\classes ..\app\src\main\java\com\kingfischer\engine\bitboard\*.java ..\app\src\main\java\com\kingfischer\engine\chess\*.java ..\app\src\main\java\com\kingfischer\engine\movegen\MoveGenerator.java ..\app\src\main\java\com\kingfischer\engine\util\Zobrist.java ..\app\src\main\java\com\kingfischer\engine\gui\MoveNotation.java ..\app\src\main\java\com\kingfischer\engine\gui\GameModel.java src\com\jengacode\rooms\RulesCheck.java
if errorlevel 1 exit /b 1
jar --create --file rules.jar -C build\classes .
