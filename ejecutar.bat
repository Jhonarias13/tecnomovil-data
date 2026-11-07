@echo off
chcp 65001 > nul
cd /d "%~dp0"
if exist out rmdir /s /q out
dir /s /b src\*.java > fuentes.txt
javac -encoding UTF-8 --release 17 -d out @fuentes.txt
del fuentes.txt
echo ^>^>^> Pruebas
java -Dstdout.encoding=UTF-8 -cp out pruebas.PruebasAnalizador
echo ^>^>^> Aplicacion
java -Dstdout.encoding=UTF-8 -cp out App %*
