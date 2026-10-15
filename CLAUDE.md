# Karsy — reglas para Claude y sus agentes

Marketplace de autos seminuevos y usados en México. Sin pagos ni chat interno; el contacto es externo (llamada, WhatsApp o correo).

- `app/` — Android: Kotlin + Jetpack Compose. Paquete `com.pame.karsy`.
  `feature/<pantalla>/` (Screen + ViewModel) → `data/repository/` → `core/supabase/`.
- `web/` — React + Vite + TypeScript.
- `supabase/` — migraciones, plantillas y edge functions. Un solo proyecto de Supabase para app y web.

## Reglas de trabajo

1. **Haz solo lo que pide la tarea.** Nada de refactorizar, renombrar, reformatear ni "mejorar" código ajeno a la tarea. Si ves algo que convendría cambiar, anótalo en el reporte final.
2. **No agregues dependencias** (Gradle o npm) sin que la tarea lo pida.
3. **No agregues capas nuevas** (módulo domain, UseCases, interfaces para una sola clase). Respeta la arquitectura actual.
4. **Base de datos:** no la hagas más grande de lo necesario. Las migraciones existentes no se editan: todo cambio va en una migración nueva. Toda tabla nueva lleva RLS. Las migraciones las aplica el humano, nunca un agente.
5. **Seguridad:** en la app y la web solo va la llave anon/publishable. La `service_role` nunca entra al repo. Los permisos reales se validan en RLS, no solo ocultando botones.
6. **Ramas:** trabaja en una rama `agente/<tarea-corta>` creada desde `pame-dev`. No hagas push ni merge; eso lo hace el humano.
7. **Windows:** en la terminal de Windows usa `.\gradlew.bat`; en bash usa `./gradlew`.

## Flujo para cada tarea

1. **Plan corto:** antes de editar, escribe en 3 a 6 puntos qué archivos vas a tocar y por qué.
2. **Implementa** solo eso.
3. **Tester:** delega al subagente `tester` para escribir pruebas de la lógica nueva y correr las verificaciones.
4. **Revisores:** si cambiaste `app/`, delega a `revisor-kotlin`. Si cambiaste `supabase/` o consultas a la base, delega a `revisor-supabase`.
5. **Supervisor:** al final delega SIEMPRE al subagente `supervisor`, pasándole el texto de la tarea original.
6. Si algún revisor o el supervisor dice BLOQUEAR / CORREGIR, corrige y repite los pasos 3 a 5. Máximo 2 rondas; si sigue sin aprobarse, detente y explica qué falta.
7. **Reporte final** para el humano:
   - Qué se hizo (archivos y una línea por cada uno).
   - Resultado del tester (tabla).
   - Veredicto de cada revisor y del supervisor.
   - Pendientes o dudas que requieren decisión humana.
