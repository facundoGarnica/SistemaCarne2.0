# Instalar y actualizar el sistema

## 1. Generar el sistema (en tu PC)

Doble clic en **`generar-instalador.bat`** (en la carpeta del proyecto). Tarda 1–2 minutos y deja:

| Archivo | Para qué |
|---|---|
| `dist\SistemaCarne\` | Carpeta para copiar a la otra PC (trae su propio Java) |
| `dist\SistemaCarne.jar` | Archivo para publicar una actualización en GitHub |

> Si dice que no puede borrar `dist\SistemaCarne`, cerrá el sistema y el Explorador si tiene esa carpeta abierta.

---

## 2. Primera instalación en la otra PC

Esa PC ya tiene el sistema viejo y su base `carnedb`. **El sistema nuevo usa esa misma base**, así que los datos se mantienen. Al abrir, Hibernate agrega solo las tablas y columnas que falten; no borra nada.

1. **Cerrá el sistema viejo.**
2. **Hacé una copia de la base, por las dudas:** copiá `backup-base.bat` (está dentro de `dist\SistemaCarne`) a esa PC y ejecutalo. Deja un `.sql` en la carpeta `backups`.
3. **Copiá la carpeta `dist\SistemaCarne` completa** a `C:\SistemaCarne` (con pendrive, por ejemplo).
4. **Revisá `C:\SistemaCarne\config.properties`.** Viene con `root` / `1234` y la base `carnedb` en esa misma PC. Si cambia la contraseña de MySQL, se cambia ahí, sin recompilar.
5. **Creá el acceso directo:** clic derecho en `SistemaCarne.exe` → *Enviar a* → *Escritorio (crear acceso directo)*.
6. Abrilo. Debería entrar directo a Nueva Venta.

Si algo sale mal con la base, la copia del paso 2 se restaura así (en una consola, desde la carpeta `bin` de MySQL):
```
mysql -u root -p carnedb < "C:\SistemaCarne\backups\carnedb_FECHA.sql"
```

---

## 3. Publicar una actualización (en tu PC)

1. En **`pom.xml`** subí la versión, por ejemplo de `1.0.0` a `1.1.0`:
   ```xml
   <version>1.1.0</version>
   ```
   Si no la subís, la otra PC no se entera de que hay algo nuevo.
2. Ejecutá **`generar-instalador.bat`**.
3. Andá a GitHub → tu repositorio **sistemaCarne2.0** → **Releases** → **Draft a new release**:
   - **Tag:** `v1.1.0` (la misma versión del pom, con una `v` adelante)
   - **Título:** lo que quieras, por ejemplo `Versión 1.1.0`
   - **Descripción:** qué cambió. Se muestra en la pestaña Actualizaciones de la otra PC.
   - **Adjuntá** el archivo `dist\SistemaCarne.jar`. Arrastralo a la zona de archivos.
   - **Publish release**.

> No adjuntes la carpeta `dist\SistemaCarne` (180 MB). Solo hace falta el `.jar`: el Java que trae el .exe sigue sirviendo.

---

## 4. Actualizar la otra PC

En el sistema, el botón **Actualizaciones** (arriba a la derecha, tecla **F9**) se pinta de **verde** cuando hay una versión nueva.

1. Salí de Nueva Venta y asegurate de que no haya ventas en curso.
2. Entrá a **Actualizaciones** → **Descargar e instalar**.
3. El sistema:
   - hace una copia de la base en `C:\SistemaCarne\backups\`,
   - descarga la versión nueva,
   - se cierra y **se vuelve a abrir solo** con la versión nueva (tarda unos segundos).

**Si la versión nueva trae un problema:** Actualizaciones → **Volver a la versión anterior**. Vuelve al `.jar` que había antes de la última actualización, sin tocar los datos.

Desde esa misma pantalla también se puede hacer una copia de la base cuando quieras (**Hacer copia ahora**).

---

## Cuándo hace falta reinstalar la carpeta completa

Casi nunca. Solo si cambia algo del Java que viene adentro del .exe: por ejemplo, si el sistema empieza a usar una parte de Java que antes no usaba, o si se cambia la versión de Java del proyecto. En ese caso se repite el paso 2. El `config.properties` y la carpeta `backups` se conservan si no los pisás.

## Carpetas dentro de `C:\SistemaCarne`

| Carpeta / archivo | Qué es |
|---|---|
| `SistemaCarne.exe` | Abre el sistema |
| `config.properties` | Datos de MySQL de esa PC |
| `app\SistemaCarne.jar` | El sistema (lo reemplaza el actualizador) |
| `runtime\` | Java incluido |
| `backups\` | Copias de la base (conviene borrar las viejas de vez en cuando) |
| `update\` | Descargas y la versión anterior guardada |
