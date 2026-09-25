# Mobile-application
Proyecto integrador de 5to semestre

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
