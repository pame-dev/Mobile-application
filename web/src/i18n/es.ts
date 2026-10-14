// Textos en español (idioma por defecto). en.ts debe tener las mismas claves.
// Los textos comunes van aquí; los de cada área, en areas/<área>.ts.

import * as auth from './areas/auth'
import * as publicar from './areas/publicar'
import * as vehiculo from './areas/vehiculo'
import * as perfil from './areas/perfil'
import * as admin from './areas/admin'

export const CORE_ES = {
  // Comunes
  'common.cancel': 'Cancelar',
  'common.save': 'Guardar',
  'common.saving': 'Guardando…',
  'common.retry': 'Reintentar',
  'common.back': 'Volver',
  'common.close': 'Cerrar',
  'common.loading': 'Cargando…',
  'common.goHome': 'Ir al inicio',

  // Barra superior
  'nav.home': 'Inicio',
  'nav.lots': 'Lotes',
  'nav.favorites': 'Favoritos',
  'nav.searchPlaceholder': 'Buscar marca, modelo o año...',
  'nav.searchLabel': 'Buscar vehículos',
  'nav.filters': 'Filtros',
  'nav.login': 'Iniciar sesión',
  'nav.register': 'Regístrate',
  'nav.publish': 'Publicar',
  'nav.notifications': 'Notificaciones',
  'nav.notificationsUnread': 'Notificaciones ({n} sin leer)',
  'nav.myAccount': 'Mi cuenta',
  'nav.admin': 'Administrador',
  'nav.lot': 'Lote',
  'nav.individual': 'Particular',
  'nav.myProfile': 'Mi perfil',
  'nav.myDashboard': 'Mi panel',
  'nav.history': 'Historial',
  'nav.settings': 'Configuración',
  'nav.adminPanel': 'Panel de administración',
  'nav.logout': 'Cerrar sesión',
  'nav.favoritesGuest': 'Regístrate para guardar tus favoritos',
  'nav.registerMe': 'Registrarme',

  // Cerrar sesión
  'logout.title': '¿Cerrar sesión?',
  'logout.sub': '¿Estás seguro de que deseas salir de tu cuenta de Karsy? Tendrás que iniciar sesión de nuevo para gestionar tus vehículos.',
  'logout.confirm': 'Sí, salir',

  // Filtros
  'filters.title': 'FILTROS',
  'filters.modalTitle': 'Filtros',
  'filters.guest': 'Crea una cuenta para acceder a los filtros y funciones exclusivas.',
  'filters.registerFree': 'Regístrate gratis →',
  'filters.clear': 'Limpiar',
  'filters.apply': 'Aplicar',
  'filters.type': 'Tipo',
  'filters.all': 'Todos',
  'filters.brand': 'Marca',
  'filters.allBrands': 'Todas',
  'filters.model': 'Modelo',
  'filters.year': 'Año',
  'filters.anyYear': 'Cualquiera',
  'filters.price': 'Rango de precio',
  'filters.min': 'Mín. $0',
  'filters.max': 'Máx. $2M',
  'filters.minLabel': 'Precio mínimo',
  'filters.maxLabel': 'Precio máximo',
  'sort.label': 'Ordenar',
  'sort.recent': 'Más recientes',
  'sort.low': 'Menor precio',
  'sort.high': 'Mayor precio',

  // Inicio
  'home.eyebrow': 'Marketplace automotriz #1 en México',
  'home.title1': 'Encuentra tu próximo',
  'home.title2': 'vehículo ideal',
  'home.lead': 'Miles de vehículos verificados en toda la república mexicana. Compra, vende y negocia con confianza.',
  'home.statVehicles': 'Vehículos',
  'home.statBrands': 'Marcas',
  'home.statModels': 'Modelos',
  'home.visitorTitle': 'Estás en modo visitante',
  'home.visitorText': 'Regístrate para guardar favoritos, contactar vendedores y publicar tu vehículo.',
  'home.visitorCta': 'Crear cuenta gratis →',
  'home.featured': 'Vehículos destacados',
  'home.featuredSub': 'Selección especial de nuestra plataforma',
  'home.prev': 'Anteriores',
  'home.next': 'Siguientes',
  'home.featuredBadge': '★ Destacado',
  'home.results': 'Resultados',
  'home.all': 'Todos los vehículos',
  'home.countOne': '{n} vehículo disponible',
  'home.countMany': '{n} vehículos disponibles',
  'home.clearFilters': 'Limpiar filtros',
  'home.loadError': 'No pudimos cargar los vehículos. Revisa tu conexión e intenta de nuevo.',
  'home.empty': 'Todavía no hay vehículos publicados.',
  'home.noMatch': 'Ningún vehículo coincide con tu búsqueda.',

  // Pie de página
  'footer.tagline': 'Marketplace automotriz para comprar y vender autos nuevos y seminuevos en todo México.',
  'footer.explore': 'Explorar',
  'footer.vehicles': 'Vehículos',
  'footer.lots': 'Lotes y agencias',
  'footer.publish': 'Publicar vehículo',
  'footer.account': 'Tu cuenta',
  'footer.legal': 'Legal',
  'footer.language': 'Idioma',

  // Idioma
  'lang.title': 'Idioma',
  'lang.sub': 'Elige el idioma en el que quieres ver Karsy.',
  'lang.es': 'Español',
  'lang.en': 'English',

  // Configuración
  'settings.title': 'Configuración',
  'settings.notificationsDesc': 'Alertas y avisos de tu cuenta',
  'settings.privacy': 'Privacidad y seguridad',
  'settings.privacyDesc': 'Contraseña, sesiones y políticas de privacidad',
  'settings.language': 'Idioma',
  'settings.theme': 'Tema',
  'settings.themeDesc': 'Claro, oscuro o el del sistema',
  'settings.themeLight': 'Claro',
  'settings.themeDark': 'Oscuro',
  'settings.themeSystem': 'Sistema',
  'settings.terms': 'Términos y condiciones',
  'settings.termsDesc': 'Políticas de privacidad y uso',
  'settings.backLabel': 'Regresar',

  // Privacidad y seguridad
  'privacy.security': 'Seguridad',
  'privacy.changePassword': 'Cambiar contraseña',
  'privacy.changePasswordDesc': 'Necesitas tu contraseña actual',
  'privacy.signOutAll': 'Cerrar sesión en todos los dispositivos',
  'privacy.signOutAllDesc': 'Útil si perdiste tu teléfono o crees que alguien entró a tu cuenta',
  'privacy.signOutAllTitle': '¿Cerrar sesión en todos lados?',
  'privacy.signOutAllMsg': 'Se cerrará tu sesión en este navegador y en todos los dispositivos donde hayas entrado. Tendrás que volver a iniciar sesión.',
  'privacy.signOutAllConfirm': 'Cerrar todas',
  'privacy.privacy': 'Privacidad',
  'privacy.policy': 'Políticas de privacidad',
  'privacy.policyDesc': 'Qué datos guardamos y para qué',

  // Cambiar contraseña
  'password.title': 'Cambiar contraseña',
  'password.sub': 'Por seguridad, primero escribe tu contraseña actual.',
  'password.current': 'Contraseña actual',
  'password.new': 'Contraseña nueva',
  'password.confirm': 'Confirmar contraseña nueva',
  'password.errCurrent': 'Escribe tu contraseña actual.',
  'password.errMismatch': 'Las contraseñas no coinciden.',
  'password.errSame': 'La nueva contraseña debe ser diferente a la anterior.',
  'password.updated': 'Contraseña actualizada.',

  // Notificaciones
  'notif.title': 'Notificaciones',
  'notif.loadError': 'No pudimos cargar tus notificaciones. Revisa tu conexión e intenta de nuevo.',
  'notif.empty': 'Aún no tienes notificaciones.',
  'notif.reason': 'Motivo: {motivo}',
  'notif.yourVehicle': 'tu vehículo',
  'notif.generic': 'Tienes un aviso sobre "{v}".',
  'notif.favorito': 'A alguien le gustó tu publicación "{v}" y la agregó a sus favoritos.',
  'notif.propuesta_enviada': 'Tu publicación "{v}" se envió al administrador y la está revisando.',
  'notif.publicacion_aprobada': '¡Tu publicación "{v}" fue aprobada! Ya está visible para todos.',
  'notif.destacado_aprobado': 'Tu publicación "{v}" ahora es destacada durante 1 mes.',
  'notif.edicion_aprobada': 'Se aprobaron los cambios a tu publicación "{v}". Ya se ven publicados.',
  'notif.publicacion_rehabilitada': 'Tu publicación "{v}" fue rehabilitada y vuelve a estar visible.',
  'notif.publicacion_rechazada': 'Tu publicación "{v}" fue rechazada. Corrígela y vuelve a enviarla.',
  'notif.edicion_rechazada': 'No se aprobaron los cambios a "{v}". Sigue visible la versión anterior.',
  'notif.publicacion_deshabilitada': 'Administración deshabilitó tu publicación "{v}". Ya no es visible para otros usuarios.',
  'notif.destacado_rechazado': 'Tu solicitud para destacar "{v}" fue rechazada.',
  'notif.destacado_por_vencer': 'Tu destacado de "{v}" termina en menos de 3 días. Renuévalo para seguir apareciendo en destacados.',
  'notif.cuenta_suspendida': 'Tu cuenta de Karsy fue suspendida por administración. Mientras esté suspendida no podrás entrar a la app.',
  'notif.cuenta_reactivada': 'Tu cuenta de Karsy fue reactivada. Ya puedes volver a entrar.',
  'notif.reporte_atendido': 'Gracias por tu reporte sobre "{v}". Lo revisamos y tomamos medidas.',
  'notif.reporte_descartado': 'Revisamos tu reporte sobre "{v}" y no encontramos un incumplimiento.',

  // Textos legales
  'legal.terms': 'Términos y condiciones',
  'legal.policy': 'Políticas de privacidad',

  // 404
  'notFound.title': 'No encontramos esta página',
  'notFound.message': 'Puede que el enlace esté mal escrito o que la página ya no exista.',
  'placeholder.message': 'Esta pantalla llega en la siguiente fase de la versión web.',
} as const

export const ES = {
  ...CORE_ES,
  ...auth.es,
  ...publicar.es,
  ...vehiculo.es,
  ...perfil.es,
  ...admin.es,
}

export type Clave = keyof typeof ES
