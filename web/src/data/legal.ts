// Textos legales: los mismos de la app (strings.xml: terms_* y privacy_notice_*).
// Si cambian en la app, hay que copiarlos aquí también.

export interface LegalDoc {
  update: string
  intro: string
  sections: [string, string][]
}

export const TERMINOS: LegalDoc = {
  "update": "Última actualización: octubre de 2026",
  "intro": "Bienvenido a Karsy. Al crear una cuenta o usar la aplicación aceptas estos términos y condiciones. Te pedimos leerlos con atención.",
  "sections": [
    [
      "1. Sobre Karsy",
      "Karsy es una plataforma que conecta a compradores y vendedores de vehículos en México. Karsy no es propietario de los vehículos publicados ni participa en la compraventa; solo facilita el contacto entre las partes."
    ],
    [
      "2. Cuentas de usuario",
      "Puedes registrarte como Particular o como Lote. Te comprometes a dar información real y actualizada, y a mantener tu contraseña en secreto. Eres responsable de toda la actividad que ocurra en tu cuenta."
    ],
    [
      "3. Publicaciones",
      "Las publicaciones deben describir el vehículo de forma veraz: precio, kilometraje, estado y fotos reales del vehículo. Toda publicación pasa por una revisión del equipo de administración antes de mostrarse a los compradores y puede ser rechazada si no cumple con estas reglas."
    ],
    [
      "4. Publicaciones destacadas",
      "Puedes solicitar que tu vehículo aparezca en la sección de destacados. La solicitud será revisada por un administrador, quien puede aprobarla o rechazarla indicando el motivo. Un destacado aprobado dura 1 mes a partir de la aprobación; al terminar, la publicación sigue activa como una publicación normal y puedes volver a solicitarlo."
    ],
    [
      "5. Contacto y tratos entre usuarios",
      "Los acuerdos, pagos y entregas se realizan directamente entre comprador y vendedor. Te recomendamos revisar la documentación del vehículo, verificar la identidad del vendedor y no realizar pagos por adelantado sin haber visto el vehículo."
    ],
    [
      "6. Conductas no permitidas",
      "No está permitido publicar información falsa, fotos que no correspondan al vehículo, contenido ofensivo, suplantar a otra persona, enviar spam ni intentar estafar a otros usuarios."
    ],
    [
      "7. Reportes y moderación",
      "Cualquier usuario puede reportar una publicación o una cuenta. El equipo de Karsy revisará cada reporte y podrá deshabilitar publicaciones o suspender cuentas que no cumplan con estos términos."
    ],
    [
      "8. Privacidad",
      "Usamos tu nombre, correo, teléfono y WhatsApp para operar tu cuenta y para que los compradores interesados puedan contactarte. Tu teléfono y WhatsApp solo se muestran en tus publicaciones. No vendemos tu información a terceros."
    ],
    [
      "9. Cambios a estos términos",
      "Karsy puede actualizar estos términos en cualquier momento. Te avisaremos dentro de la aplicación cuando haya cambios importantes. Si continúas usando Karsy después de un cambio, aceptas los términos actualizados."
    ]
  ]
}

export const POLITICAS: LegalDoc = {
  "update": "Última actualización: octubre de 2026",
  "intro": "En Karsy cuidamos tus datos personales. Este aviso explica qué información guardamos, para qué la usamos, con quién se comparte y cómo puedes ejercer tus derechos.",
  "sections": [
    [
      "1. Datos que recabamos",
      "Al registrarte: nombre, correo electrónico, contraseña (cifrada), teléfono o WhatsApp de contacto y tu ubicación (estado y municipio). Si eres Lote, también el nombre comercial, la persona responsable, la dirección y los horarios de atención. De forma opcional: foto de perfil y una descripción. Al publicar: los datos, fotos y video de tu vehículo."
    ],
    [
      "2. Actividad dentro de la app",
      "Guardamos los vehículos que ves, los que agregas a favoritos, cuándo pides los datos de contacto de un vendedor y los reportes que envías. Si aceptas las notificaciones, guardamos un identificador de tu teléfono para enviártelas."
    ],
    [
      "3. Para qué usamos tus datos",
      "Para crear y proteger tu cuenta, publicar y mostrar tus vehículos, conectar a compradores con vendedores, mostrarte tu historial y tus favoritos, avisarte sobre tus publicaciones, revisar reportes y mantener la seguridad de la plataforma. No vendemos tus datos ni los usamos para publicidad de terceros."
    ],
    [
      "4. Qué ven otros usuarios",
      "En tu perfil público y en tus publicaciones se muestran tu nombre, foto, descripción y ubicación. Tu teléfono o WhatsApp solo se muestra cuando un comprador con sesión iniciada pide tus datos de contacto. Tu correo y tu contraseña nunca se muestran."
    ],
    [
      "5. Servicios que usamos",
      "Tus datos se almacenan en Supabase (base de datos, inicio de sesión y archivos). Las notificaciones se envían por medio de Firebase Cloud Messaging, de Google. Estos servicios solo procesan la información necesaria para funcionar."
    ],
    [
      "6. Tus derechos",
      "Puedes acceder a tus datos y corregirlos desde tu perfil en cualquier momento. También puedes solicitar la cancelación de tu cuenta o oponerte al uso de tus datos contactando al equipo de Karsy. Si este aviso cambia, lo actualizaremos aquí con su nueva fecha."
    ]
  ]
}
