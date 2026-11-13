@echo off
rem ==============================================================
rem  Genera el sistema listo para instalar / publicar:
rem
rem    dist\SistemaCarne\       carpeta para copiar a la otra PC
rem       SistemaCarne.exe       (trae su propio Java: no depende del de la PC)
rem       config.properties      datos de MySQL de esa PC
rem       backup-base.bat        copia de la base sin abrir el sistema
rem    dist\SistemaCarne.jar    archivo para subir a GitHub Releases
rem
rem  Antes de publicar una actualizacion: subir <version> en pom.xml.
rem ==============================================================
setlocal
chcp 65001 >nul
cd /d "%~dp0"

rem ---------- Maven: del PATH o el que bajo el wrapper ----------
set "MVN="
for /f "delims=" %%P in ('where mvn.cmd 2^>nul') do if not defined MVN set "MVN=%%P"
if not defined MVN (
    for /d %%D in ("%USERPROFILE%\.m2\wrapper\dists\apache-maven-*") do (
        for /d %%E in ("%%D\*") do if exist "%%E\bin\mvn.cmd" set "MVN=%%E\bin\mvn.cmd"
    )
)
if not defined MVN (
    echo No se encontro Maven. Instale Maven o abra el proyecto una vez en NetBeans.
    goto error
)

rem ---------- jpackage: del JDK (JAVA_HOME o PATH) ----------
set "JPACKAGE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
if not defined JPACKAGE for /f "delims=" %%P in ('where jpackage 2^>nul') do if not defined JPACKAGE set "JPACKAGE=%%P"
if not defined JPACKAGE (
    echo No se encontro jpackage. Hace falta un JDK 17 o mas nuevo.
    goto error
)
for %%J in ("%JPACKAGE%") do set "JDKBIN=%%~dpJ"

echo [1/4] Compilando con Maven...
call "%MVN%" -q clean package -DskipTests
if errorlevel 1 goto error

rem Version tomada del pom.xml (Maven la deja en version.properties)
for /f "tokens=2 delims==" %%V in ('findstr /b "version=" target\classes\version.properties') do set "VERSION=%%V"
echo        Version %VERSION%

echo [2/4] Preparando archivos...
if exist target\jpackage-input rmdir /s /q target\jpackage-input
mkdir target\jpackage-input
copy /y target\SistemaCarne.jar target\jpackage-input\ >nul
if exist dist\SistemaCarne rmdir /s /q dist\SistemaCarne
if exist dist\SistemaCarne (
    echo No se pudo borrar dist\SistemaCarne: cierre el sistema y las ventanas
    echo del Explorador que tengan esa carpeta abierta, y vuelva a intentar.
    goto error
)
if not exist dist mkdir dist

rem Modulos de Java que lleva el .exe (jdk.crypto.ec hace falta para HTTPS
rem con Mercado Pago y GitHub; en JDK nuevos ya viene dentro de java.base)
set "MODULOS=java.se,jdk.unsupported,jdk.charsets,jdk.localedata,jdk.zipfs,jdk.accessibility,jdk.management,jdk.net"
"%JDKBIN%java.exe" --list-modules | findstr /b "jdk.crypto.ec@" >nul && set "MODULOS=%MODULOS%,jdk.crypto.ec"

echo [3/4] Generando SistemaCarne.exe (tarda un poco)...
"%JPACKAGE%" --type app-image ^
    --name SistemaCarne ^
    --app-version %VERSION% ^
    --vendor "Carniceria" ^
    --input target\jpackage-input ^
    --main-jar SistemaCarne.jar ^
    --main-class carniceria.sistemacarne.Launcher ^
    --icon instalacion\SistemaCarne.ico ^
    --add-modules %MODULOS% ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --dest dist
if errorlevel 1 goto error

echo [4/4] Copiando configuracion y archivo para GitHub...
copy /y instalacion\config.properties dist\SistemaCarne\ >nul
copy /y instalacion\backup-base.bat dist\SistemaCarne\ >nul
copy /y target\SistemaCarne.jar dist\SistemaCarne.jar >nul
rem El .exe (con su icono) viaja dentro del .jar de GitHub: al actualizar,
rem el sistema lo instala solo (ver Util\ActualizadorEjecutable.java)
"%JDKBIN%jar.exe" uf dist\SistemaCarne.jar -C dist\SistemaCarne SistemaCarne.exe
if errorlevel 1 goto error

echo.
echo ==============================================================
echo  Listo - version %VERSION%
echo   Para instalar en otra PC:  copiar la carpeta dist\SistemaCarne
echo   Para publicar actualizacion: subir dist\SistemaCarne.jar a un
echo   Release de GitHub con el tag v%VERSION%  (ver INSTALACION.md)
echo ==============================================================
if not "%1"=="/nopause" pause
exit /b 0

:error
echo.
echo *** Hubo un error, no se genero el instalador ***
if not "%1"=="/nopause" pause
exit /b 1
