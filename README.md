## Esta es la solución para el trabajo de la Unidad 2 (Creación de repositorio) de la IU Digital de Antioquia.

Es un programa hecho en Java puro (versión 17 o superior, sin usar librerías externas) que procesa la información de un día entero del sistema de transporte de TecnoValle. Maneja más de 920.000 registros y 200.000 usuarios usando programación funcional: funciones puras, datos que no se cambian (inmutabilidad), lambdas, funciones que reciben otras funciones, composición y streams para procesar datos de forma normal o al mismo tiempo.

## Cómo ejecutarlo

Se necesita JDK 17 o superior.

```bash
./ejecutar.sh          # macOS / Linux
ejecutar.bat           # Windows
./ejecutar.sh 500000   # opcional: cambiar el número de usuarios simulados
```

O a mano:

```bash
javac -encoding UTF-8 --release 17 -d out $(find src -name "*.java")
java -cp out pruebas.PruebasAnalizador
java -cp out App
```
