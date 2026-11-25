# EcoRetos — Guía de despliegue en la nube

```
App Android  ──►  API Spring Boot (Railway)  ──►  PostgreSQL (Supabase)
```

Tiempo estimado: 40–60 minutos. Hazlo en este orden.

---

## Paso 1 — Base de datos en Supabase (10 min)

1. Entra a **https://supabase.com** → *Start your project* → inicia sesión con GitHub.
2. **New project**:
   - Name: `ecoretos`
   - Database Password: crea una y **guárdala** (la vas a necesitar en el Paso 3).
   - Region: **East US (North Virginia)** (la más cercana a Railway).
   - Clic en *Create new project* y espera 1–2 minutos.
3. Menú izquierdo → **SQL Editor** → *New query*.
4. Abre el archivo `database/supabase_setup.sql` de este repo, copia **todo**, pégalo y presiona **Run**.
   - Al final debe mostrar: usuarios 5, retos 6, insignias 3.
   - Si sale un aviso sobre "destructive operation" o RLS, acepta: es normal.
5. Arriba en el proyecto, botón **Connect** → pestaña de cadenas de conexión → elige **Session pooler** (NO "Direct connection").
   Verás algo así:
   ```
   postgresql://postgres.abcdefghijk:[YOUR-PASSWORD]@aws-0-us-east-1.pooler.supabase.com:5432/postgres
   ```
   Anota estas tres partes:
   - **Host**: `aws-0-us-east-1.pooler.supabase.com` (el tuyo puede variar)
   - **Usuario**: `postgres.abcdefghijk` (incluye el punto y el código del proyecto)
   - **Contraseña**: la del punto 2

> ⚠️ Usa el **Session pooler**. La "Direct connection" (`db.xxxx.supabase.co`) es solo IPv6 y Railway no puede conectarse a ella.

---

## Paso 2 — Subir el API a GitHub (5 min)

Este repo ya trae los cambios para la nube: driver PostgreSQL, `Dockerfile`, configuración por variables de entorno y el arreglo de las URLs de imágenes.

Si ya tienes el repo `EcoRetos-api` en GitHub, reemplaza los archivos por los de esta carpeta y haz:

```bash
git add .
git commit -m "Migración a PostgreSQL y despliegue en Railway"
git push
```

Si no existe, créalo en GitHub (puede ser privado) y sube esta carpeta.
Verifica que el `Dockerfile` quede **en la raíz** del repo (junto a `pom.xml`).

---

## Paso 3 — Desplegar el API en Railway (15 min)

1. Entra a **https://railway.com** → *Login* → con GitHub. (La prueba gratis no pide tarjeta.)
2. **New Project** → **Deploy from GitHub repo** → elige el repo del API.
   - Si no aparece, clic en *Configure GitHub App* y dale acceso al repo.
3. Railway detecta el `Dockerfile` y empieza a construir. **El primer intento puede fallar** porque aún no tiene las variables; es normal.
4. Clic en el servicio → pestaña **Variables** → *New Variable* (o *Raw Editor*) y agrega:

   ```
   DB_URL=jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require
   DB_USER=postgres.abcdefghijk
   DB_PASSWORD=tu_contraseña_de_supabase
   PORT=8080
   ```
   (Reemplaza host y usuario por los tuyos del Paso 1. Ojo: `DB_URL` empieza con `jdbc:` y **no** lleva usuario ni contraseña.)

   Al guardar, Railway vuelve a desplegar solo.
5. Pestaña **Settings** → sección **Networking** → **Generate Domain**.
   - Si pregunta el puerto, pon **8080**.
   - Te dará una URL tipo `https://ecoretos-api-production.up.railway.app`.
6. Pestaña **Deployments** → espera a que diga **Active / Success** (2–4 min). En *View logs* debe aparecer `Started EcoretosApiApplication`.
7. **Prueba** en el navegador:
   - `https://TU-DOMINIO.up.railway.app/ping` → debe responder **EcoRetos API funcionando**
   - `https://TU-DOMINIO.up.railway.app/retos` → lista de retos en JSON
   - `https://TU-DOMINIO.up.railway.app/swagger-ui/index.html` → documentación del API

### (Opcional) Que las fotos no se borren
Sin esto, las fotos de evidencias se pierden cada vez que el API se redespliega.
En Railway: clic derecho en el lienzo del proyecto → **Volume** → conéctalo al servicio del API con *Mount path* **`/app/uploads`**.

---

## Paso 4 — Apuntar la app Android al API (10 min)

1. En Android Studio abre `app/src/main/java/com/example/ecoretosapp/data/api/RetrofitClient.kt`.
2. Cambia la línea:
   ```kotlin
   private const val BASE_URL = "https://TU-DOMINIO.up.railway.app/"
   ```
   por tu dominio real del Paso 3 (con `https://` y la `/` al final).
3. *Build* → *Build App Bundle(s) / APK(s)* → **Build APK(s)** → instálalo en el celular.
4. Prueba con datos móviles (sin WiFi de la casa) para confirmar que ya no depende de tu red.

---

## Usuarios de demo

| Rol | CIF | Contraseña |
|---|---|---|
| Administrador | `ADMIN001` | `admin123` |
| Estudiante | `EST001` | `est123` |
| Estudiante | `EST002` | `est123` |
| Estudiante | `EST003` | `est123` |
| Estudiante | `EST004` | `est123` |

---

## Antes de presentar (5 min antes)

1. Abre `https://TU-DOMINIO.up.railway.app/ping` en el navegador para confirmar que responde.
2. Haz un login de prueba en la app.

---

## Si algo falla

Railway → servicio → **Deployments** → **View logs**. Busca el error:

| Error en los logs | Causa | Solución |
|---|---|---|
| `password authentication failed` | Usuario o contraseña mal | `DB_USER` debe ser `postgres.xxxxx` (con el código), revisa la contraseña |
| `Connection refused` / `timeout` / `Network is unreachable` | Se usó la conexión directa | Usa el host del **Session pooler** (`...pooler.supabase.com`) |
| `Tenant or user not found` | Usuario sin el código del proyecto | `DB_USER=postgres.xxxxx`, no solo `postgres` |
| `Max client connections reached` | Demasiadas conexiones | Agrega la variable `DB_POOL_SIZE=3` |
| `OutOfMemoryError` / el servicio se reinicia | Poca memoria | Settings → Resources: sube la memoria a 1 GB |
| La app dice error de conexión | `BASE_URL` incorrecta | Debe ser `https://...up.railway.app/` con `/` al final |
| `/retos` devuelve `[]` | No se corrió el script | Repite el Paso 1.4 |

---

## Variables de entorno disponibles

| Variable | Obligatoria | Valor por defecto | Para qué |
|---|---|---|---|
| `DB_URL` | Sí | `jdbc:postgresql://localhost:5432/ecoretos` | Conexión JDBC a Supabase |
| `DB_USER` | Sí | `postgres` | Usuario de la BD |
| `DB_PASSWORD` | Sí | `postgres` | Contraseña de la BD |
| `DB_POOL_SIZE` | No | `5` | Conexiones máximas |
| `DDL_AUTO` | No | `update` | Hibernate crea/ajusta tablas |
| `SHOW_SQL` | No | `false` | Mostrar SQL en logs |
| `PUBLIC_URL` | No | (se deduce) | URL base para las imágenes |
| `PORT` | Recomendada | `8080` | Fíjala en `8080` para que coincida con el dominio de Railway |

## Desarrollo local

Con PostgreSQL instalado en tu PC, crea la base `ecoretos`, corre `database/supabase_setup.sql` y ejecuta el API normalmente: los valores por defecto apuntan a `localhost`.

## Qué se cambió respecto a la versión con SQL Server

- `pom.xml`: driver `mssql-jdbc` → `postgresql`.
- `application.properties`: credenciales por variables de entorno (ya no van en el código), puerto `PORT`, pool pequeño para Supabase.
- `ArchivoService`: las URLs de las fotos ya no usan la IP `192.168.1.12`; se generan con el dominio público.
- `WebConfig`: crea la carpeta de uploads al arrancar (antes las imágenes daban 404 en un servidor nuevo).
- Nuevos: `Dockerfile`, `.dockerignore`, `database/supabase_setup.sql`, esta guía.
- App Android: `BASE_URL` a HTTPS y tiempos de espera más amplios.
