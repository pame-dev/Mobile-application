Create **exactly 3 separate, editable high-fidelity mobile screens** for the existing **KARSY** car marketplace application.

IMPORTANT:

* The Login screen already exists and should NOT be redesigned.
* Create ONLY the 3 new screens described below.
* Each screen must be an independent **402 × 874 px mobile frame**.
* Do NOT combine the screens into a collage.
* Do NOT put multiple screens inside one frame.
* Each screen must be fully editable in Figma.
* Maintain the same visual language as the existing KARSY Login screen.
* Use Auto Layout and reusable components.
* All visible text must be in **Spanish (Mexico)**.

## KARSY DESIGN STYLE

KARSY is a modern marketplace for buying and selling new and used cars in Mexico.

The registration experience should feel:

* Modern
* Clean
* Professional
* Trustworthy
* Simple
* Easy to understand

Use a minimalist automotive design.

### Colors

Primary blue:
#1565C0

Dark blue:
#0D47A1

Light blue:
#E3F2FD

Background:
#F7F9FC

White:
#FFFFFF

Text:
#172033

Secondary text:
#667085

Borders:
#DDE2E8

Success:
#2E7D32

### Typography

Use **Inter**.

* H1: 28px Bold
* H2: 24px SemiBold
* Body: 16px Regular
* Labels: 14px Medium
* Button: 16px SemiBold
* Caption: 12px Regular

Use rounded corners between 12px and 16px.

Use subtle shadows only where necessary.

---

# SCREEN 01 — REGISTRO / SELECCIÓN DE CLIENTE

Create a separate frame named:

**01 — Registro — Tipo de cliente**

This screen is opened when the user presses **"Registrarse"** on the existing Login screen.

Top:

* KARSY logo
* Back arrow

Main title:

**"Registro"**

Subtitle:

**"Selecciona qué tipo de cliente eres"**

Create two large selectable cards/buttons.

### OPTION 1

Icon representing an individual person.

Title:

**"Particular"**

Description:

**"Compra y vende vehículos como persona particular."**

Primary selection style.

### OPTION 2

Icon representing a dealership/business.

Title:

**"Lote"**

Description:

**"Publica y administra vehículos de tu lote o agencia."**

Secondary selection style.

Make both options visually clear and easy to select.

At the bottom add a small text:

**"Podrás completar tu perfil después."**

The two options must clearly communicate that selecting one will take the user to a different registration form.

---

# SCREEN 02 — REGISTRO PARTICULAR

Create a separate frame named:

**02 — Registro — Particular**

This screen opens when the user selects **"Particular"**.

Top:

* Back arrow
* KARSY logo

Title:

**"Crear perfil"**

Subtitle:

**"Registra tus datos para comenzar a usar KARSY."**

Create a clean scrollable registration form.

Fields:

### Información personal

**Nombre**
Placeholder:
"Ingresa tu nombre"

**Apellido**
Placeholder:
"Ingresa tu apellido"

**Correo electrónico**
Placeholder:
"[correo@ejemplo.com](mailto:correo@ejemplo.com)"

**Teléfono**
Placeholder:
"10 dígitos"

### Seguridad

**Contraseña**
Placeholder:
"Ingresa una contraseña"

Include an eye icon to show/hide password.

**Confirmar contraseña**
Placeholder:
"Repite tu contraseña"

Include an eye icon.

At the bottom create a large primary blue button:

**"Crear perfil"**

Below the button:

**"Al crear tu perfil aceptas los términos y condiciones de KARSY."**

Make the form visually clean with clear labels, large touch targets and enough spacing.

The screen should be scrollable because all fields may not fit vertically.

---

# SCREEN 03 — REGISTRO LOTE

Create a separate frame named:

**03 — Registro — Lote**

This screen opens when the user selects **"Lote"**.

Top:

* Back arrow
* KARSY logo

Title:

**"Crear perfil de lote"**

Subtitle:

**"Registra tu lote para comenzar a publicar vehículos."**

Create a clean scrollable form divided into sections.

## RESPONSABLE

Section title:

**"Datos del responsable"**

Fields:

**Nombre del responsable**
Placeholder:
"Ingresa el nombre"

**Apellido del responsable**
Placeholder:
"Ingresa el apellido"

**Correo electrónico**
Placeholder:
"[correo@ejemplo.com](mailto:correo@ejemplo.com)"

**Teléfono comercial**
Placeholder:
"10 dígitos"

---

## DATOS DEL LOTE

Section title:

**"Información del lote"**

Fields:

**Nombre del lote**
Placeholder:
"Ej. Auto Premium"

**Dirección del lote**
Placeholder:
"Ingresa la dirección"

**Ciudad**
Placeholder:
"Selecciona una ciudad"

**Estado**
Placeholder:
"Selecciona un estado"

**Descripción del lote**
Multiline text area.

Placeholder:

"Describe brevemente tu lote, los servicios que ofrece y el tipo de vehículos que maneja."

---

## FOTO DE PERFIL

Section title:

**"Foto de perfil"**

Create a large circular image upload area.

Inside:

Camera/gallery icon.

Text:

**"Agregar foto"**

Small helper text:

**"Logo del lote o imagen de perfil"**

Allow the user to upload or change the image.

---

## SEGURIDAD

Section title:

**"Seguridad"**

Fields:

**Contraseña**
Placeholder:
"Ingresa una contraseña"

Eye icon.

**Confirmar contraseña**
Placeholder:
"Repite tu contraseña"

Eye icon.

At the bottom create a large primary blue button:

**"Crear perfil"**

Below:

**"Al crear tu perfil aceptas los términos y condiciones de KARSY."**

---

# FORM DESIGN

All input fields should have:

* Clear label above the field
* Rounded rectangle
* White background
* Light gray border
* 48–52px minimum height
* Comfortable horizontal padding
* Clear focus state using KARSY blue
* Error state using red
* Password visibility icon
* Appropriate keyboard/input type

Use vertical spacing of approximately 16px between fields and 24px between sections.

---

# NAVIGATION FLOW

The intended flow is:

Existing Login screen

↓

Press **"Registrarse"**

↓

**01 — Registro — Tipo de cliente**

↓

If user selects **"Particular"**

↓

**02 — Registro — Particular**

OR

↓

If user selects **"Lote"**

↓

**03 — Registro — Lote**

Do NOT create the Login screen.

Do NOT create additional screens.

Only create these 3 independent editable frames.

---

# FINAL REQUIREMENTS

The three screens must visually belong to the same KARSY application.

Keep:

* Same blue
* Same typography
* Same buttons
* Same input styles
* Same spacing
* Same border radius
* Same icon style
* Same visual hierarchy

The result should look like a polished real-world registration flow for a professional Mexican automotive marketplace.

Make the forms practical for later implementation using **Kotlin + Jetpack Compose**.
