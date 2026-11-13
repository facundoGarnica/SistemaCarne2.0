@echo off
rem ==============================================================
rem  Copia de seguridad de la base "carnedb" (sin abrir el sistema).
rem  Usar ANTES de instalar el sistema nuevo por primera vez.
rem  Deja el archivo en la carpeta "backups" al lado de este .bat.
rem ==============================================================
setlocal
chcp 65001 >nul

set "USUARIO=root"
set "CLAVE=1234"
set "BASE=carnedb"

rem Buscar mysqldump.exe: primero en el PATH, después en Program Files\MySQL
set "MYSQLDUMP="
for /f "delims=" %%P in ('where mysqldump 2^>nul') do if not defined MYSQLDUMP set "MYSQLDUMP=%%P"
if not defined MYSQLDUMP (
    for /d %%D in ("%ProgramFiles%\MySQL\*") do if exist "%%D\bin\mysqldump.exe" set "MYSQLDUMP=%%D\bin\mysqldump.exe"
)
if not defined MYSQLDUMP (
    echo No se encontro mysqldump.exe. Revise que MySQL este instalado.
    pause
    exit /b 1
)

for /f %%F in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HH-mm-ss"') do set "FECHA=%%F"
if not exist "%~dp0backups" mkdir "%~dp0backups"
set "ARCHIVO=%~dp0backups\%BASE%_%FECHA%.sql"

echo Haciendo copia de la base %BASE%...
set "MYSQL_PWD=%CLAVE%"
"%MYSQLDUMP%" -u %USUARIO% --single-transaction --routines --default-character-set=utf8mb4 --result-file="%ARCHIVO%" %BASE%
if errorlevel 1 (
    echo.
    echo ERROR: no se pudo hacer la copia.
    pause
    exit /b 1
)

echo.
echo Copia lista: %ARCHIVO%
pause
