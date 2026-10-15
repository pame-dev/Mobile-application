// Textos legales: los mismos de la app (strings.xml: terms_* y privacy_notice_*).
// Si cambian en la app, hay que copiarlos aquí también (en español y en inglés).

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

// Versiones en inglés (values-en/strings-en.xml de la app).

export const TERMS_EN: LegalDoc = {
  "update": "Last updated: October 2026",
  "intro": "Welcome to Karsy. By creating an account or using the app you agree to these terms and conditions. Please read them carefully.",
  "sections": [
    [
      "1. About Karsy",
      "Karsy is a platform that connects vehicle buyers and sellers in Mexico. Karsy does not own the listed vehicles and does not take part in the sale; it only makes it easier for both parties to get in touch."
    ],
    [
      "2. User accounts",
      "You can sign up as an Individual or as a Dealer. You agree to provide real, up-to-date information and to keep your password private. You are responsible for all activity on your account."
    ],
    [
      "3. Listings",
      "Listings must describe the vehicle truthfully: price, mileage, condition and real photos of the vehicle. Every listing is reviewed by the admin team before buyers can see it and may be rejected if it does not follow these rules."
    ],
    [
      "4. Featured listings",
      "You can request that your vehicle appear in the featured section. An administrator will review the request and may approve or reject it, stating the reason. An approved feature lasts 1 month from approval; when it ends, the listing stays active as a regular listing and you can request it again."
    ],
    [
      "5. Contact and deals between users",
      "Agreements, payments and deliveries happen directly between buyer and seller. We recommend checking the vehicle\\'s paperwork, verifying the seller\\'s identity and not paying in advance before seeing the vehicle."
    ],
    [
      "6. Prohibited conduct",
      "You may not post false information, photos that do not match the vehicle or offensive content, impersonate someone else, send spam or try to scam other users."
    ],
    [
      "7. Reports and moderation",
      "Any user can report a listing or an account. The Karsy team will review every report and may disable listings or suspend accounts that do not follow these terms."
    ],
    [
      "8. Privacy",
      "We use your name, email, phone and WhatsApp to run your account and so interested buyers can contact you. Your phone and WhatsApp are only shown on your listings. We do not sell your information to third parties."
    ],
    [
      "9. Changes to these terms",
      "Karsy may update these terms at any time. We will let you know in the app when there are important changes. If you keep using Karsy after a change, you accept the updated terms."
    ]
  ]
}

export const POLICY_EN: LegalDoc = {
  "update": "Last updated: October 2026",
  "intro": "At Karsy we take care of your personal data. This notice explains what information we keep, what we use it for, who it is shared with and how you can exercise your rights.",
  "sections": [
    [
      "1. Data we collect",
      "When you sign up: name, email, password (encrypted), contact phone or WhatsApp and your location (state and municipality). If you are a Lot, also the business name, the person in charge, the address and opening hours. Optionally: profile photo and a description. When you publish: your vehicle details, photos and video."
    ],
    [
      "2. Activity in the app",
      "We keep the vehicles you view, the ones you add to favorites, when you request a seller\\'s contact details and the reports you send. If you allow notifications, we keep an identifier of your phone to send them to you."
    ],
    [
      "3. What we use your data for",
      "To create and protect your account, publish and show your vehicles, connect buyers with sellers, show your history and favorites, notify you about your listings, review reports and keep the platform safe. We do not sell your data or use it for third-party advertising."
    ],
    [
      "4. What other users see",
      "Your public profile and listings show your name, photo, description and location. Your phone or WhatsApp is only shown when a signed-in buyer requests your contact details. Your email and password are never shown."
    ],
    [
      "5. Services we use",
      "Your data is stored in Supabase (database, sign-in and files). Notifications are sent through Firebase Cloud Messaging, by Google. These services only process the information they need to work."
    ],
    [
      "6. Your rights",
      "You can access and correct your data from your profile at any time. You can also request the cancellation of your account or object to the use of your data by contacting the Karsy team. If this notice changes, we will update it here with its new date."
    ]
  ]
}

// ── Aceptación durante el registro ──

export type DocLegal = 'terminos' | 'privacidad'

/**
 * Versión de cada documento: cámbiala cuando cambie su texto (aquí y en la app). Una aceptación
 * de otra versión ya no cuenta y el usuario tiene que volver a leerlo y aceptarlo.
 */
export const VERSION_LEGAL: Record<DocLegal, string> = { terminos: '2026-10', privacidad: '2026-10' }

/**
 * Documentos aceptados en el registro en curso (al final de cada documento, uno por uno).
 * Solo en memoria, igual que el borrador del formulario: se pierden al recargar la página.
 */
let aceptados: Partial<Record<DocLegal, string>> = {}

export const aceptarDoc = (d: DocLegal) => {
  aceptados[d] = VERSION_LEGAL[d]
}
export const docAceptado = (d: DocLegal) => aceptados[d] === VERSION_LEGAL[d]
export const limpiarAceptaciones = () => {
  aceptados = {}
}
