#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
rm -rf out
javac -encoding UTF-8 --release 17 -d out $(find src -name "*.java")
echo ">>> Pruebas"
java -Dstdout.encoding=UTF-8 -cp out pruebas.PruebasAnalizador
echo ">>> Aplicación"
java -Dstdout.encoding=UTF-8 -cp out App "$@"
