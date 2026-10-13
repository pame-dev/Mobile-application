# Mobile-application
Proyecto integrador de 5to semestre

Karsy tiene dos clientes que usan **el mismo proyecto de Supabase** (mismas tablas,
mismo inicio de sesión y mismas fotos), así que lo que se hace en uno se ve en el otro.

```
app/        App de Android (Kotlin + Jetpack Compose)
web/        Versión web (React + Vite + TypeScript)
supabase/   Migraciones y plantillas de correo, compartidas por las dos
```

## App de Android

1. En `local.properties` (no se sube al repo) pon los datos de Supabase:
   ```
   SUPABASE_URL=https://<proyecto>.supabase.co
   SUPABASE_ANON_KEY=<anon / publishable key>
   ```
2. Con un emulador o celular conectado:
   ```powershell
   .\gradlew.bat installDebug
   ```

La app abre en la pantalla de **Bienvenida**.

## Web

Requiere Node 20 o más nuevo.

1. Copia `web/.env.example` como `web/.env` y llena los **mismos** valores que la app:
   ```
   VITE_SUPABASE_URL=https://<proyecto>.supabase.co
   VITE_SUPABASE_ANON_KEY=<anon / publishable key>
   ```
   Solo va la llave pública (anon). `.env` está en `.gitignore`; nunca subas la
   `service_role`.
2. Instala y corre:
   ```powershell
   cd web
   npm install
   npm run dev      # http://localhost:5173
   ```
3. Para generar la versión final: `npm run build` (queda en `web/dist/`).

A diferencia de la app, la web abre directo en el **Inicio como visitante**;
*Iniciar sesión* y *Regístrate* están en la barra de arriba.

## Base de datos

Las migraciones están en `supabase/migrations`. Para aplicarlas al proyecto vinculado:

```bash
npx supabase db push
```

Cuentas de prueba (la contraseña se comparte por privado con el equipo):

| Cuenta | Correo | Rol |
|---|---|---|
| Pame | pame@karsy.com | Administrador |
| Nancy | nancy@karsy.com | Lote |
| Jahir | jahir@karsy.com | Particular |

Estas cuentas ya tienen el correo verificado, así que entran sin código.

## Códigos por correo

La app pide un código de 6 dígitos en dos casos:

- **Verificar el correo**: al registrarse, o al iniciar sesión con un correo sin verificar.
- **¿Olvidaste tu contraseña?**: correo → código → contraseña nueva y su confirmación.

Supabase Auth genera el código, lo manda por correo y lo valida. En el proyecto
de Supabase (Dashboard) hay que configurar:

1. **Authentication → Sign In / Providers → Email**: activar **Confirm email** y
   dejar **Email OTP Length** en 6.
2. **Authentication → Emails**: cambiar dos plantillas para que manden el código
   (`{{ .Token }}`) en lugar del enlace:
   - **Confirm signup**: asunto `Tu código de verificación de Karsy`, contenido de
     `supabase/templates/confirmacion.html`.
   - **Reset Password**: asunto `Tu código para restablecer tu contraseña de Karsy`,
     contenido de `supabase/templates/recuperacion.html`.
3. **Authentication → Emails → SMTP Settings**: el correo integrado de Supabase
   solo envía a los miembros del equipo del proyecto y muy pocos correos por hora.
   Para usuarios reales hay que configurar un SMTP propio (Resend, Gmail, etc.).

`supabase/config.toml` tiene la misma configuración para el entorno local.




Para instalarlo
.\gradlew.bat installDebug


bajar db:
npx supabase link --project-ref rkuikrlejybrxrlmzsms

subir db:
npx supabase db push


para correrla en cel:

~/Android/Sdk/platform-tools/adb devices


./gradlew installDebug

hacer respaldos de la db manuales (sin Docker, con pg_dump 17):

# una sola vez: instalar el cliente de Postgres 17 (repo de Ubuntu 24.04 "noble", base de Mint 22)
sudo apt install -y curl ca-certificates
sudo install -d /usr/share/postgresql-common/pgdg
sudo curl -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc --fail https://www.postgresql.org/media/keys/ACCC4CF8.asc
echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt noble-pgdg main" | sudo tee /etc/apt/sources.list.d/pgdg.list
sudo apt update && sudo apt install -y postgresql-client-17

# cada respaldo (la contraseña es la de la BD: Project Settings → Database; no la guardes en el repo)
export PGPASSWORD='CONTRASEÑA_DE_LA_BD'
PGURL="postgresql://postgres.rkuikrlejybrxrlmzsms@aws-0-us-west-2.pooler.supabase.com:5432/postgres"
/usr/lib/postgresql/17/bin/pg_dump "$PGURL" --schema=public -f respaldos/esquema_$(date +%F).sql
/usr/lib/postgresql/17/bin/pg_dump "$PGURL" --data-only --schema=public --schema=auth -f respaldos/datos_$(date +%F).sql
unset PGPASSWORD

estructura completa de la bd (tablas, funciones, triggers...): estructura_base_de_datos.txt
