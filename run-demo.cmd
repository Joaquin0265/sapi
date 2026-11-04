@echo off
REM ============================================================================
REM  SAPI - Ejecuta la aplicacion en el perfil "demo" (H2 en memoria, sin Oracle)
REM  Uso: doble clic, o desde la terminal:  .\run-demo.cmd
REM  Luego abrir: http://localhost:8090
REM ============================================================================
setlocal
cd /d "%~dp0"

if "%JAVA_HOME%"=="" (
    echo [ERROR] JAVA_HOME no esta definido.
    echo Configura la variable de entorno JAVA_HOME apuntando a un JDK 21 o superior.
    pause
    exit /b 1
)

call mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo" "-Dmaven.test.skip=true"
