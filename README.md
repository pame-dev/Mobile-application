# Mobile-application
Proyecto integrador de 5to semestre

## Conectar la app con Supabase

La app lee la URL y la clave pública del proyecto desde `local.properties`
(ese archivo no se sube a git). Agrega estas dos líneas:

```properties
SUPABASE_URL=https://rkuikrlejybrxrlmzsms.supabase.co
SUPABASE_ANON_KEY=<anon / publishable key>
```

La clave está en el Dashboard de Supabase → Project Settings → API.

## Base de datos

Las migraciones están en `supabase/migrations`. Para aplicarlas al proyecto vinculado:

```bash
npx supabase db push
```

Cuentas de prueba (contraseña `6767`):

| Cuenta | Correo | Rol |
|---|---|---|
| Pame | pame@karsy.com | Administrador |
| Nancy | nancy@karsy.com | Lote |
| Jahir | jahir@karsy.com | Particular |
