-- ############################################################################
-- Quien guardó un auto en favoritos lo sigue viendo si su dueño lo pausa
-- ############################################################################
-- Antes, si el dueño pausaba su publicación, quien la tenía en favoritos dejaba de poder
-- leerla: al abrir el aviso "ya no está disponible" salía "no encontrado" y el auto
-- desaparecía de sus favoritos sin explicación. Ahora la puede seguir leyendo y la app y la
-- web le muestran "Este auto ya no está disponible" (foto en gris, "Ver autos parecidos").
-- Solo las PAUSADAS por el dueño: las que deshabilitó el equipo no, porque tienen un motivo
-- (motivo_deshabilitacion) y a veces fotos retiradas que solo deben ver el dueño y el equipo.
-- El resto de la gente sigue sin ver las pausadas.
-- Las fotos (fotos_publicacion_leer) y v_anuncios (security_invoker) siguen este mismo
-- permiso, así que no hace falta cambiarlas.


-- Cuerpo de 20261102120000_moderadores.sql más la condición de favoritos.
DROP POLICY publicaciones_leer ON public.publicaciones;
CREATE POLICY publicaciones_leer ON public.publicaciones
    FOR SELECT TO anon, authenticated
    USING (
        (fecha_primera_publicacion IS NOT NULL
            AND estado_administrativo = 'habilitada'
            AND estado_publicacion IN ('activa', 'vendida'))
        OR id_propietario = auth.uid()
        OR public.es_personal(auth.uid())
        -- Pausada por su dueño (no deshabilitada por el equipo) y esta cuenta la tiene en
        -- favoritos. favoritos_leer_propios solo compara la cuenta, sin volver a consultar
        -- publicaciones, así que no hay recursión.
        OR (fecha_primera_publicacion IS NOT NULL
            AND estado_administrativo = 'habilitada'
            AND estado_publicacion = 'deshabilitada'
            AND auth.uid() IS NOT NULL
            AND EXISTS (SELECT 1 FROM public.favoritos f
                        WHERE f.id_publicacion = publicaciones.id_publicacion
                          AND f.id_cuenta = auth.uid()))
    );
