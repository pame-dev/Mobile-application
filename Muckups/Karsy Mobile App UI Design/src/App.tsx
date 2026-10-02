import React, { useState, useEffect, createContext, useContext } from "react"

const karsyLogo = new URL("./karsy-logo.png", import.meta.url).href

const UserModeContext = createContext<"visitor" | "user" | "admin">("user")

// ─── Logo Karsy reutilizable ─────────────────────────────────────────
function KarsyLogo({ size = 34 }: { size?: number }) {
  return (
    <img
      src={karsyLogo}
      alt="Karsy"
      style={{
        width: size,
        height: size,
        borderRadius: Math.max(4, Math.round(size * 0.35)),
        objectFit: "cover",
        display: "block",
        flexShrink: 0,
      }}
    />
  )
}

const CAROUSEL_IMAGES = [
  {
    url: "https://images.unsplash.com/photo-1767749995450-7b63ab7cd4fd?w=600&h=900&fit=crop&auto=format",
    alt: "Black Cadillac SUV",
  },
  {
    url: "https://images.unsplash.com/photo-1580273916550-e323be2ae537?w=600&h=900&fit=crop&auto=format",
    alt: "Gray Mercedes coupe on road",
  },
  {
    url: "https://images.unsplash.com/photo-1571987502227-9231b837d92a?w=600&h=900&fit=crop&auto=format",
    alt: "Gray electric sedan",
  },
  {
    url: "https://images.unsplash.com/photo-1758411898152-5fde4b5eef56?w=600&h=900&fit=crop&auto=format",
    alt: "White luxury SUV by water",
  },
]

function GlobeIcon() {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="12" cy="12" r="10" />
      <line x1="2" y1="12" x2="22" y2="12" />
      <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
    </svg>
  )
}

function MailIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#8E9A8E"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
      <polyline points="22,6 12,13 2,6" />
    </svg>
  )
}

function LockIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#8E9A8E"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </svg>
  )
}

function EyeOffIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#8E9A8E"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  )
}

const PHONE_W = 375
const PHONE_H = 812

const phoneShell: React.CSSProperties = {
  width: PHONE_W,
  height: PHONE_H,
  borderRadius: 40,
  overflow: "hidden",
  boxShadow: "0 32px 80px rgba(13,43,69,0.35), 0 8px 24px rgba(0,0,0,0.2)",
  display: "flex",
  flexDirection: "column",
  flexShrink: 0,
  background: "#F4F7F9",
}

function BackArrow() {
  return (
    <svg
      width="22"
      height="22"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#0D2B45"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <polyline points="15 18 9 12 15 6" />
    </svg>
  )
}

function PersonIcon({ active }: { active: boolean }) {
  const c = active ? "#FFFFFF" : "#52A3AA"
  return (
    <svg
      width="36"
      height="36"
      viewBox="0 0 24 24"
      fill="none"
      stroke={c}
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  )
}

function DealerIcon({ active }: { active: boolean }) {
  const c = active ? "#FFFFFF" : "#52A3AA"
  return (
    <svg
      width="36"
      height="36"
      viewBox="0 0 24 24"
      fill="none"
      stroke={c}
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <rect x="2" y="7" width="20" height="14" rx="2" />
      <path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2" />
      <line x1="12" y1="12" x2="12" y2="17" />
      <line x1="9" y1="14.5" x2="15" y2="14.5" />
    </svg>
  )
}

function PhoneTopBar({
  onBack,
  logoColor = "#0D2B45",
}: {
  onBack?: () => void
  logoColor?: string
}) {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "52px 24px 0",
      }}
    >
      <button
        onClick={onBack}
        style={{
          background: "none",
          border: "none",
          cursor: "pointer",
          padding: 4,
          display: "flex",
          alignItems: "center",
        }}
      >
        <BackArrow />
      </button>
      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
        <KarsyLogo size={38} />
        <span
          style={{
            fontFamily: "'Outfit', sans-serif",
            fontWeight: 700,
            fontSize: 20,
            color: logoColor,
            letterSpacing: "0.04em",
          }}
        >
          Karsy
        </span>
      </div>
      <div style={{ width: 30 }} />
    </div>
  )
}

function FormInput({
  label,
  placeholder,
  type = "text",
  hasEye = false,
}: {
  label: string
  placeholder: string
  type?: string
  hasEye?: boolean
}) {
  const [show, setShow] = useState(false)
  return (
    <div style={{ marginBottom: 16 }}>
      <label
        style={{
          display: "block",
          fontFamily: "'DM Sans', sans-serif",
          fontSize: 13,
          fontWeight: 600,
          color: "#333333",
          marginBottom: 6,
        }}
      >
        {label}
      </label>
      <div style={{ position: "relative" }}>
        <input
          type={hasEye ? (show ? "text" : "password") : type}
          placeholder={placeholder}
          style={{
            width: "100%",
            padding: hasEye ? "14px 44px 14px 16px" : "14px 16px",
            borderRadius: 12,
            border: "1.5px solid #E2E8ED",
            background: "#FFFFFF",
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 15,
            color: "#333333",
            boxSizing: "border-box",
            transition: "border-color 0.2s",
          }}
        />
        {hasEye && (
          <button
            onClick={() => setShow(!show)}
            style={{
              position: "absolute",
              right: 14,
              top: "50%",
              transform: "translateY(-50%)",
              background: "none",
              border: "none",
              cursor: "pointer",
              padding: 0,
              display: "flex",
            }}
          >
            <EyeOffIcon />
          </button>
        )}
      </div>
    </div>
  )
}

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        gap: 10,
        margin: "20px 0 14px",
      }}
    >
      <span
        style={{
          fontFamily: "'Outfit', sans-serif",
          fontSize: 13,
          fontWeight: 700,
          color: "#0D2B45",
          letterSpacing: "0.06em",
          textTransform: "uppercase",
        }}
      >
        {children}
      </span>
      <div style={{ flex: 1, height: 1, background: "#E2E8ED" }} />
    </div>
  )
}

function PrimaryButton({
  children,
  style,
  onClick,
}: {
  children: React.ReactNode
  style?: React.CSSProperties
  onClick?: () => void
}) {
  return (
    <button
      onClick={onClick}
      style={{
        width: "100%",
        padding: "17px 0",
        borderRadius: 14,
        background: "#0D2B45",
        border: "none",
        color: "#FFFFFF",
        fontFamily: "'Outfit', sans-serif",
        fontSize: 16,
        fontWeight: 600,
        letterSpacing: "0.01em",
        cursor: "pointer",
        boxShadow: "0 4px 16px rgba(13,43,69,0.28)",
        transition: "background 0.2s, transform 0.1s",
        ...style,
      }}
      onMouseEnter={(e) => {
        ;(e.currentTarget as HTMLButtonElement).style.background = "#112f50"
        ;(e.currentTarget as HTMLButtonElement).style.transform =
          "translateY(-1px)"
      }}
      onMouseLeave={(e) => {
        ;(e.currentTarget as HTMLButtonElement).style.background = "#0D2B45"
        ;(e.currentTarget as HTMLButtonElement).style.transform =
          "translateY(0)"
      }}
    >
      {children}
    </button>
  )
}

function TermsNote({
  checked,
  onChange,
}: {
  checked: boolean
  onChange: (v: boolean) => void
}) {
  return (
    <label
      style={{
        display: "flex",
        alignItems: "flex-start",
        gap: 10,
        cursor: "pointer",
        padding: "10px 12px",
        borderRadius: 12,
        background: checked ? "#EBF6F7" : "#F4F7F9",
        border: `1.5px solid ${checked ? "#52A3AA" : "#E2E8ED"}`,
        transition: "all 0.2s",
        marginBottom: 16,
        userSelect: "none",
      }}
    >
      <div
        style={{
          width: 20,
          height: 20,
          borderRadius: 6,
          border: `2px solid ${checked ? "#52A3AA" : "#C4CCCC"}`,
          background: checked ? "#52A3AA" : "#FFFFFF",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          flexShrink: 0,
          marginTop: 1,
          transition: "all 0.2s",
        }}
      >
        {checked && (
          <svg
            width="12"
            height="12"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#FFFFFF"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <polyline points="20 6 9 17 4 12" />
          </svg>
        )}
      </div>
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        style={{ display: "none" }}
      />
      <span
        style={{
          fontFamily: "'DM Sans', sans-serif",
          fontSize: 12,
          color: "#333333",
          lineHeight: 1.5,
        }}
      >
        Acepto los{" "}
        <span
          style={{
            color: "#52A3AA",
            textDecoration: "underline",
            cursor: "pointer",
            fontWeight: 600,
          }}
          onClick={(e) => e.preventDefault()}
        >
          términos y condiciones
        </span>{" "}
        de KARSY.
      </span>
    </label>
  )
}

// ─── Toast flotante de "Regístrate" ─────────────────────────────────────────
function RegisterToast({
  message,
  onClose,
  onRegister,
}: {
  message: string
  onClose: () => void
  onRegister: () => void
}) {
  useEffect(() => {
    const t = setTimeout(onClose, 3500)
    return () => clearTimeout(t)
  }, [onClose])

  return (
    <div
      style={{
        position: "fixed",
        bottom: 24,
        left: "50%",
        transform: "translateX(-50%)",
        zIndex: 9999,
        width: "min(420px, calc(100vw - 32px))",
        background: "#0D2B45",
        borderRadius: 16,
        boxShadow:
          "0 12px 40px rgba(13,43,69,0.35), 0 4px 12px rgba(0,0,0,0.2)",
        padding: "16px 18px",
        display: "flex",
        alignItems: "center",
        gap: 14,
        fontFamily: "'DM Sans', sans-serif",
        animation: "toastSlideUp 0.28s cubic-bezier(0.16, 1, 0.3, 1)",
      }}
      role="alert"
    >
      <div
        style={{
          width: 40,
          height: 40,
          borderRadius: "50%",
          background: "#52A3AA",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          flexShrink: 0,
        }}
      >
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#FFFFFF"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div
          style={{
            color: "#FFFFFF",
            fontSize: 14,
            fontWeight: 600,
            lineHeight: 1.35,
            marginBottom: 2,
          }}
        >
          {message}
        </div>
        <div
          style={{
            color: "rgba(255,255,255,0.6)",
            fontSize: 12,
            lineHeight: 1.4,
          }}
        >
          Crea una cuenta gratis para continuar.
        </div>
      </div>
      <button
        onClick={(e) => {
          e.stopPropagation()
          onRegister()
        }}
        style={{
          padding: "9px 16px",
          borderRadius: 10,
          background: "#52A3AA",
          border: "none",
          color: "#FFFFFF",
          fontFamily: "'Outfit', sans-serif",
          fontSize: 13,
          fontWeight: 700,
          cursor: "pointer",
          flexShrink: 0,
          whiteSpace: "nowrap",
        }}
      >
        Registrarme
      </button>
      <button
        onClick={onClose}
        style={{
          background: "none",
          border: "none",
          cursor: "pointer",
          padding: 4,
          display: "flex",
          alignItems: "center",
          color: "rgba(255,255,255,0.6)",
          flexShrink: 0,
        }}
        aria-label="Cerrar"
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.2"
          strokeLinecap="round"
        >
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>

      <style>{`
        @keyframes toastSlideUp {
          from { transform: translate(-50%, 20px); opacity: 0; }
          to { transform: translate(-50%, 0); opacity: 1; }
        }
      `}</style>
    </div>
  )
}

function RegisterTypeScreen() {
  const [selected, setSelected] = useState<"Particular" | "Lote" | null>(null)

  const card = (id: "Particular" | "Lote", title: string, desc: string) => {
    const active = selected === id
    return (
      <button
        onClick={() => setSelected(id)}
        style={{
          display: "flex",
          alignItems: "flex-start",
          gap: 16,
          width: "100%",
          padding: "20px 20px",
          borderRadius: 16,
          border: active ? "2px solid #52A3AA" : "1.5px solid #E2E8ED",
          background: active ? "#EBF6F7" : "#FFFFFF",
          cursor: "pointer",
          textAlign: "left",
          marginBottom: 14,
          boxShadow: active
            ? "0 4px 16px rgba(82,163,170,0.18)"
            : "0 2px 8px rgba(13,43,69,0.06)",
          transition: "all 0.2s",
        }}
      >
        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: 14,
            flexShrink: 0,
            background: active ? "#52A3AA" : "#EBF6F7",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            transition: "background 0.2s",
          }}
        >
          {id === "Particular" ? (
            <PersonIcon active={active} />
          ) : (
            <DealerIcon active={active} />
          )}
        </div>
        <div>
          <div
            style={{
              fontFamily: "'Outfit', sans-serif",
              fontSize: 17,
              fontWeight: 700,
              color: "#0D2B45",
              marginBottom: 4,
            }}
          >
            {title}
          </div>
          <div
            style={{
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 14,
              color: "#8E9A8E",
              lineHeight: 1.4,
            }}
          >
            {desc}
          </div>
        </div>
      </button>
    )
  }

  return (
    <div style={{ ...phoneShell, position: "relative" }}>
      <PhoneTopBar />
      <div style={{ padding: "20px 24px 0" }}>
        <h1
          style={{
            fontFamily: "'Outfit', sans-serif",
            fontSize: 28,
            fontWeight: 700,
            color: "#0D2B45",
            margin: "0 0 6px",
            letterSpacing: "-0.02em",
          }}
        >
          Registro
        </h1>
        <p
          style={{
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 15,
            color: "#8E9A8E",
            margin: "0 0 28px",
          }}
        >
          Selecciona qué tipo de cliente eres
        </p>
        {card(
          "Particular",
          "Particular",
          "Compra y vende vehículos como persona particular.",
        )}
        {card(
          "Lote",
          "Lote",
          "Publica y administra vehículos de tu lote o agencia.",
        )}
      </div>
      <div style={{ flex: 1 }} />
      <div style={{ padding: "0 24px 44px" }}>
        <PrimaryButton>Continuar</PrimaryButton>
        <p
          style={{
            textAlign: "center",
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 12,
            color: "#8E9A8E",
            margin: "14px 0 0",
          }}
        >
          Podrás completar tu perfil después.
        </p>
      </div>
    </div>
  )
}

function RegisterParticularScreen() {
  const [acceptedTerms, setAcceptedTerms] = useState(false)
  return (
    <div style={{ ...phoneShell }}>
      <PhoneTopBar />
      <div style={{ padding: "20px 24px 0" }}>
        <h1
          style={{
            fontFamily: "'Outfit', sans-serif",
            fontSize: 28,
            fontWeight: 700,
            color: "#0D2B45",
            margin: "0 0 6px",
            letterSpacing: "-0.02em",
          }}
        >
          Crear perfil
        </h1>
        <p
          style={{
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 15,
            color: "#8E9A8E",
            margin: 0,
          }}
        >
          Registra tus datos para comenzar a ver y vender vehículos
        </p>
      </div>
      <div style={{ flex: 1, overflowY: "auto", padding: "4px 24px 0" }}>
        <SectionLabel>Información personal</SectionLabel>
        <FormInput label="Nombre" placeholder="Ingresa tu nombre" />
        <FormInput label="Apellido" placeholder="Ingresa tu apellido" />
        <FormInput
          label="Correo electrónico"
          placeholder="correo@ejemplo.com"
          type="email"
        />
        <FormInput label="Teléfono" placeholder="10 dígitos" type="tel" />
        <SectionLabel>Seguridad</SectionLabel>
        <FormInput
          label="Contraseña"
          placeholder="Ingresa una contraseña"
          type="password"
          hasEye
        />
        <FormInput
          label="Confirmar contraseña"
          placeholder="Repite tu contraseña"
          type="password"
          hasEye
        />
        <div style={{ marginTop: 24, marginBottom: 40 }}>
          <TermsNote checked={acceptedTerms} onChange={setAcceptedTerms} />
          <PrimaryButton
            style={{
              opacity: acceptedTerms ? 1 : 0.5,
              cursor: acceptedTerms ? "pointer" : "not-allowed",
            }}
          >
            Crear perfil
          </PrimaryButton>
        </div>
      </div>
    </div>
  )
}

function RegisterLoteScreen() {
  const [acceptedTerms, setAcceptedTerms] = useState(false)
  return (
    <div style={{ ...phoneShell }}>
      <PhoneTopBar />
      <div style={{ padding: "20px 24px 0" }}>
        <h1
          style={{
            fontFamily: "'Outfit', sans-serif",
            fontSize: 28,
            fontWeight: 700,
            color: "#0D2B45",
            margin: "0 0 6px",
            letterSpacing: "-0.02em",
          }}
        >
          Crear perfil de lote
        </h1>
        <p
          style={{
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 15,
            color: "#8E9A8E",
            margin: 0,
          }}
        >
          Registra tu lote para comenzar a publicar vehículos.
        </p>
      </div>
      <div style={{ flex: 1, overflowY: "auto", padding: "4px 24px 0" }}>
        <SectionLabel>Datos del responsable</SectionLabel>
        <FormInput
          label="Nombre del responsable"
          placeholder="Ingresa el nombre"
        />
        <FormInput
          label="Apellido del responsable"
          placeholder="Ingresa el apellido"
        />
        <FormInput
          label="Correo electrónico"
          placeholder="correo@ejemplo.com"
          type="email"
        />
        <FormInput
          label="Teléfono comercial"
          placeholder="10 dígitos"
          type="tel"
        />
        <SectionLabel>Información del lote</SectionLabel>
        <FormInput label="Nombre del lote" placeholder="Ej. Auto Premium" />
        <FormInput
          label="Dirección del lote"
          placeholder="Ingresa la dirección"
        />
        <div
          style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}
        >
          <FormInput label="Ciudad" placeholder="Selecciona una ciudad" />
          <FormInput label="Estado" placeholder="Selecciona un estado" />
        </div>
        <div style={{ marginBottom: 16 }}>
          <label
            style={{
              display: "block",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 13,
              fontWeight: 600,
              color: "#333333",
              marginBottom: 6,
            }}
          >
            Descripción del lote
          </label>
          <textarea
            placeholder="Describe brevemente tu lote, los servicios que ofrece y el tipo de vehículos que maneja."
            rows={4}
            style={{
              width: "100%",
              padding: "14px 16px",
              borderRadius: 12,
              border: "1.5px solid #E2E8ED",
              background: "#FFFFFF",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 15,
              color: "#333333",
              resize: "none",
              boxSizing: "border-box",
              lineHeight: 1.5,
            }}
          />
        </div>
        <SectionLabel>Foto de perfil</SectionLabel>
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            width: 100,
            height: 100,
            borderRadius: "50%",
            border: "2px dashed #52A3AA",
            background: "#EBF6F7",
            margin: "0 auto 20px",
            cursor: "pointer",
            gap: 6,
          }}
        >
          <svg
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#52A3AA"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
            <circle cx="12" cy="13" r="4" />
          </svg>
          <span
            style={{
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 11,
              color: "#52A3AA",
              fontWeight: 600,
              textAlign: "center",
              lineHeight: 1.3,
            }}
          >
            Agregar
            <br />
            foto
          </span>
        </div>
        <p
          style={{
            textAlign: "center",
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 12,
            color: "#8E9A8E",
            margin: "0 0 4px",
          }}
        >
          Logo del lote o imagen de perfil
        </p>
        <SectionLabel>Seguridad</SectionLabel>
        <FormInput
          label="Contraseña"
          placeholder="Ingresa una contraseña"
          type="password"
          hasEye
        />
        <FormInput
          label="Confirmar contraseña"
          placeholder="Repite tu contraseña"
          type="password"
          hasEye
        />
        <div style={{ marginTop: 24, marginBottom: 40 }}>
          <TermsNote checked={acceptedTerms} onChange={setAcceptedTerms} />
          <PrimaryButton
            style={{
              opacity: acceptedTerms ? 1 : 0.5,
              cursor: acceptedTerms ? "pointer" : "not-allowed",
            }}
          >
            Crear perfil
          </PrimaryButton>
        </div>
      </div>
    </div>
  )
}

function WelcomeScreen() {
  const [lang, setLang] = useState<"ES" | "EN">("ES")
  return (
    <div
      style={{
        width: 375,
        height: 812,
        position: "relative",
        overflow: "hidden",
        borderRadius: 40,
        background: "#0D2B45",
        boxShadow:
          "0 32px 80px rgba(13,43,69,0.45), 0 8px 24px rgba(0,0,0,0.3)",
        flexShrink: 0,
      }}
    >
      <div style={{ position: "absolute", inset: 0 }}>
        {CAROUSEL_IMAGES.map((img, i) => (
          <div
            key={i}
            className="carousel-slide"
            style={{ backgroundImage: `url(${img.url})` }}
            role="img"
            aria-label={img.alt}
          />
        ))}
      </div>
      <div
        style={{
          position: "absolute",
          inset: 0,
          background:
            "linear-gradient(to top, #0D2B45 0%, #0D2B45 28%, rgba(13,43,69,0.75) 50%, rgba(13,43,69,0.1) 72%, transparent 100%)",
        }}
      />
      <div
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          right: 0,
          padding: "52px 24px 0",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <KarsyLogo size={36} />
          <span
            style={{
              fontFamily: "'Outfit', sans-serif",
              fontWeight: 700,
              fontSize: 22,
              color: "#FFFFFF",
              letterSpacing: "0.04em",
            }}
          >
            Karsy
          </span>
        </div>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8,
            background: "rgba(255,255,255,0.12)",
            borderRadius: 20,
            padding: "6px 12px",
            backdropFilter: "blur(8px)",
          }}
        >
          <span style={{ display: "flex", alignItems: "center" }}>
            <svg
              width="20"
              height="20"
              viewBox="0 0 20 20"
              style={{ borderRadius: 3, overflow: "hidden" }}
            >
              <rect width="20" height="20" fill="#c60b1e" />
              <rect y="5" width="20" height="10" fill="#ffc400" />
            </svg>
          </span>
          <div style={{ display: "flex", alignItems: "center", gap: 0 }}>
            <button
              onClick={() => setLang("ES")}
              style={{
                background: "none",
                border: "none",
                cursor: "pointer",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 12,
                fontWeight: lang === "ES" ? 600 : 400,
                color: lang === "ES" ? "#FFFFFF" : "rgba(255,255,255,0.5)",
                padding: "0 6px",
                transition: "color 0.2s",
              }}
            >
              ES
            </button>
            <span style={{ color: "rgba(255,255,255,0.3)", fontSize: 11 }}>
              |
            </span>
            <button
              onClick={() => setLang("EN")}
              style={{
                background: "none",
                border: "none",
                cursor: "pointer",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 12,
                fontWeight: lang === "EN" ? 600 : 400,
                color: lang === "EN" ? "#FFFFFF" : "rgba(255,255,255,0.5)",
                padding: "0 6px",
                transition: "color 0.2s",
              }}
            >
              EN
            </button>
          </div>
        </div>
      </div>
      <div
        style={{
          position: "absolute",
          bottom: 0,
          left: 0,
          right: 0,
          padding: "0 24px 36px",
        }}
      >
        <div style={{ marginBottom: 28, textAlign: "center", height: 85 }}>
          <h1
            style={{
              fontFamily: "'Outfit', sans-serif",
              fontSize: 28,
              fontWeight: 700,
              color: "#FFFFFF",
              margin: 0,
              lineHeight: 1.2,
              letterSpacing: "-0.01em",
              height: "fit-content",
              position: "relative",
              bottom: 0,
            }}
          >
            Tu próximo auto,
            <br />a un toque de distancia.
          </h1>
        </div>
        <button
          style={{
            width: "100%",
            padding: "16px 0",
            borderRadius: 14,
            background: "rgb(86, 121, 159)",
            border: "1.5px solid rgb(255, 255, 255)",
            color: "#FFFFFF",
            fontFamily: "'Outfit', sans-serif",
            fontSize: 16,
            fontWeight: 600,
            letterSpacing: "0.01em",
            cursor: "pointer",
            marginBottom: 12,
            transition: "all 0.2s",
          }}
          onMouseEnter={(e) => {
            ;(e.currentTarget as HTMLButtonElement).style.background = "#112f50"
          }}
          onMouseLeave={(e) => {
            ;(e.currentTarget as HTMLButtonElement).style.background = "#0D2B45"
          }}
        >
          Iniciar Sesión
        </button>
        <button
          style={{
            width: "100%",
            padding: "16px 0",
            borderRadius: 14,
            background: "rgb(13, 43, 69)",
            border: "1px solid #FFFFFF",
            color: "#FFFFFF",
            fontFamily: "'Outfit', sans-serif",
            fontSize: 16,
            fontWeight: 600,
            letterSpacing: "0.01em",
            cursor: "pointer",
            marginBottom: 20,
            transition: "background 0.2s",
          }}
          onMouseEnter={(e) => {
            ;(e.currentTarget as HTMLButtonElement).style.background = "#1a1a1a"
          }}
          onMouseLeave={(e) => {
            ;(e.currentTarget as HTMLButtonElement).style.background = "#000000"
          }}
        >
          Registro
        </button>
        <div style={{ textAlign: "center", marginBottom: 16 }}>
          <button
            style={{
              background: "none",
              border: "none",
              color: "#8E9A8E",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 14,
              cursor: "pointer",
              textDecoration: "underline",
              textUnderlineOffset: 3,
            }}
          >
            Continuar como visitante
          </button>
        </div>
        <p
          style={{
            textAlign: "center",
            color: "#8E9A8E",
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 11,
            margin: 0,
            lineHeight: 1.5,
          }}
        >
          Al continuar, aceptas nuestros{" "}
          <span
            style={{
              color: "#52A3AA",
              textDecoration: "underline",
              cursor: "pointer",
            }}
          >
            Términos y Condiciones
          </span>
        </p>
      </div>
    </div>
  )
}

function LoginScreen() {
  const [showPassword, setShowPassword] = useState(false)
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [recoveryStep, setRecoveryStep] = useState<0 | 1 | 2 | 3>(0)
  const [recoveryEmail, setRecoveryEmail] = useState("")
  const [otp, setOtp] = useState(["", "", "", ""])
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [recoverySuccess, setRecoverySuccess] = useState(false)

  const maskedEmail = (() => {
    const [name, domain] = recoveryEmail.split("@")
    if (!domain) return "al******@gmail.com"
    const visible = name.slice(0, Math.min(2, name.length))
    return `${visible}${"*".repeat(Math.max(5, name.length - visible.length))}@${domain}`
  })()

  const resetRecovery = () => {
    setRecoveryStep(0)
    setRecoveryEmail("")
    setOtp(["", "", "", ""])
    setNewPassword("")
    setConfirmPassword("")
    setRecoverySuccess(false)
  }

  if (recoveryStep > 0) {
    const title =
      recoveryStep === 1
        ? "¿Olvidaste tu contraseña?"
        : recoveryStep === 2
          ? "Código de verificación"
          : "Nueva contraseña"

    const subtitle =
      recoveryStep === 1
        ? "Ingresa tu correo electrónico asociado a tu cuenta para enviarte un código de verificación."
        : recoveryStep === 2
          ? `Ingresa el código de 4 dígitos enviado a tu correo ${maskedEmail}.`
          : "Crea una contraseña segura y fácil de recordar para acceder a tu cuenta."

    const inputStyle: React.CSSProperties = {
      width: "100%",
      boxSizing: "border-box",
      padding: "15px 46px",
      borderRadius: 12,
      border: "1.5px solid #E2E8ED",
      background: "#FFFFFF",
      fontFamily: "'DM Sans', sans-serif",
      fontSize: 15,
      color: "#333333",
      outline: "none",
    }

    return (
      <div style={{ ...phoneShell, position: "relative" }}>
        <div
          style={{
            padding: "52px 24px 0",
            display: "grid",
            gridTemplateColumns: "40px 1fr 40px",
            alignItems: "center",
          }}
        >
          <button
            onClick={() => {
              if (recoveryStep === 1) resetRecovery()
              else setRecoveryStep((recoveryStep - 1) as 1 | 2)
            }}
            aria-label="Regresar"
            style={{ border: "none", background: "transparent", padding: 4, cursor: "pointer", display: "flex" }}
          >
            <BackArrow />
          </button>
          <div style={{ display: "flex", justifyContent: "center" }}>
            <KarsyLogo size={46} />
          </div>
          <div />
        </div>

        <div style={{ padding: "28px 28px 0", textAlign: "center" }}>
          <h1 style={{ fontFamily: "'Outfit', sans-serif", fontSize: 25, fontWeight: 700, color: "#0D2B45", margin: "0 0 9px", lineHeight: 1.2 }}>
            {title}
          </h1>
          <p style={{ fontFamily: "'DM Sans', sans-serif", fontSize: 13.5, color: "#8E9A8E", lineHeight: 1.55, margin: 0 }}>
            {subtitle}
          </p>
        </div>

        <div style={{ padding: "32px 28px 36px", flex: 1 }}>
          {recoveryStep === 1 && (
            <>
              <label style={{ display: "block", fontFamily: "'DM Sans', sans-serif", fontSize: 12, fontWeight: 600, color: "#333333", marginBottom: 8, letterSpacing: "0.04em" }}>
                CORREO ELECTRÓNICO
              </label>
              <div style={{ position: "relative", marginBottom: 24 }}>
                <div style={{ position: "absolute", left: 16, top: "50%", transform: "translateY(-50%)", display: "flex", pointerEvents: "none" }}>
                  <MailIcon />
                </div>
                <input
                  type="email"
                  value={recoveryEmail}
                  onChange={(e) => setRecoveryEmail(e.target.value)}
                  placeholder="ejemplo@correo.com"
                  style={inputStyle}
                />
              </div>
              <PrimaryButton
                onClick={() => {
                  if (recoveryEmail.trim() && recoveryEmail.includes("@")) setRecoveryStep(2)
                }}
                style={{ opacity: recoveryEmail.trim() && recoveryEmail.includes("@") ? 1 : 0.55 }}
              >
                Enviar código
              </PrimaryButton>
            </>
          )}

          {recoveryStep === 2 && (
            <>
              <div style={{ display: "flex", justifyContent: "center", gap: 12, margin: "4px 0 22px" }}>
                {otp.map((digit, index) => (
                  <input
                    key={index}
                    value={digit}
                    inputMode="numeric"
                    maxLength={1}
                    aria-label={`Dígito ${index + 1}`}
                    onChange={(e) => {
                      const value = e.target.value.replace(/\D/g, "").slice(-1)
                      const next = [...otp]
                      next[index] = value
                      setOtp(next)
                      if (value) {
                        const nextInput = e.currentTarget.parentElement?.children[index + 1] as HTMLInputElement | undefined
                        nextInput?.focus()
                      }
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Backspace" && !otp[index] && index > 0) {
                        const prev = e.currentTarget.parentElement?.children[index - 1] as HTMLInputElement | undefined
                        prev?.focus()
                      }
                    }}
                    style={{
                      width: 56,
                      height: 58,
                      boxSizing: "border-box",
                      borderRadius: 14,
                      border: digit ? "1.8px solid #52A3AA" : "1.5px solid #DDE5E9",
                      background: "#F4F7F9",
                      textAlign: "center",
                      fontFamily: "'Outfit', sans-serif",
                      fontSize: 24,
                      fontWeight: 700,
                      color: "#0D2B45",
                      outline: "none",
                    }}
                  />
                ))}
              </div>
              <div style={{ textAlign: "center", fontFamily: "'DM Sans', sans-serif", fontSize: 12.5, color: "#8E9A8E", marginBottom: 28 }}>
                ¿No recibiste el código?{" "}
                <button
                  onClick={() => setOtp(["", "", "", ""])}
                  style={{ border: "none", background: "transparent", padding: 0, color: "#52A3AA", font: "inherit", fontWeight: 700, cursor: "pointer" }}
                >
                  Reenviar código
                </button>
              </div>
              <PrimaryButton
                onClick={() => {
                  if (otp.every(Boolean)) setRecoveryStep(3)
                }}
                style={{ opacity: otp.every(Boolean) ? 1 : 0.55 }}
              >
                Verificar
              </PrimaryButton>
            </>
          )}

          {recoveryStep === 3 && (
            <>
              {[
                {
                  label: "NUEVA CONTRASEÑA",
                  value: newPassword,
                  setValue: setNewPassword,
                  show: showNewPassword,
                  toggle: () => setShowNewPassword(!showNewPassword),
                },
                {
                  label: "CONFIRMAR CONTRASEÑA",
                  value: confirmPassword,
                  setValue: setConfirmPassword,
                  show: showConfirmPassword,
                  toggle: () => setShowConfirmPassword(!showConfirmPassword),
                },
              ].map((field) => (
                <div key={field.label} style={{ marginBottom: 16 }}>
                  <label style={{ display: "block", fontFamily: "'DM Sans', sans-serif", fontSize: 12, fontWeight: 600, color: "#333333", marginBottom: 8, letterSpacing: "0.04em" }}>
                    {field.label}
                  </label>
                  <div style={{ position: "relative" }}>
                    <div style={{ position: "absolute", left: 16, top: "50%", transform: "translateY(-50%)", display: "flex", pointerEvents: "none" }}>
                      <LockIcon />
                    </div>
                    <input
                      type={field.show ? "text" : "password"}
                      value={field.value}
                      onChange={(e) => field.setValue(e.target.value)}
                      placeholder="••••••••••••"
                      style={{ ...inputStyle, paddingRight: 48 }}
                    />
                    <button
                      onClick={field.toggle}
                      aria-label={field.show ? "Ocultar contraseña" : "Mostrar contraseña"}
                      style={{ position: "absolute", right: 15, top: "50%", transform: "translateY(-50%)", border: "none", background: "transparent", padding: 0, display: "flex", cursor: "pointer" }}
                    >
                      <EyeOffIcon />
                    </button>
                  </div>
                </div>
              ))}

              <div style={{ background: "#F4F7F9", borderRadius: 12, padding: "12px 14px", margin: "4px 0 22px", fontFamily: "'DM Sans', sans-serif", fontSize: 12, lineHeight: 1.8 }}>
                <div style={{ color: newPassword.length >= 8 ? "#52A3AA" : "#8E9A8E" }}>
                  {newPassword.length >= 8 ? "✓" : "○"} Mínimo 8 caracteres.
                </div>
                <div style={{ color: /[A-Z]/.test(newPassword) && /\d/.test(newPassword) ? "#52A3AA" : "#8E9A8E" }}>
                  {/[A-Z]/.test(newPassword) && /\d/.test(newPassword) ? "✓" : "○"} Al menos una letra mayúscula y un número.
                </div>
              </div>

              <PrimaryButton
                onClick={() => {
                  const valid = newPassword.length >= 8 && /[A-Z]/.test(newPassword) && /\d/.test(newPassword) && newPassword === confirmPassword
                  if (valid) setRecoverySuccess(true)
                }}
                style={{
                  opacity:
                    newPassword.length >= 8 &&
                    /[A-Z]/.test(newPassword) &&
                    /\d/.test(newPassword) &&
                    newPassword === confirmPassword
                      ? 1
                      : 0.55,
                }}
              >
                Restablecer contraseña
              </PrimaryButton>

              {confirmPassword && newPassword !== confirmPassword && (
                <div style={{ color: "#E53935", fontFamily: "'DM Sans', sans-serif", fontSize: 11.5, textAlign: "center", marginTop: 10 }}>
                  Las contraseñas no coinciden.
                </div>
              )}
            </>
          )}
        </div>

        {recoverySuccess && (
          <div style={{ position: "absolute", inset: 0, zIndex: 20, background: "rgba(13, 43, 69, 0.5)", display: "flex", alignItems: "center", justifyContent: "center", padding: 28 }}>
            <div style={{ width: "100%", background: "#FFFFFF", borderRadius: 24, padding: "28px 24px", boxSizing: "border-box", boxShadow: "0 20px 50px rgba(13,43,69,0.25)", textAlign: "center" }}>
              <div style={{ width: 64, height: 64, borderRadius: "50%", background: "#52A3AA", color: "#FFFFFF", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 17px", fontSize: 30, fontWeight: 700 }}>
                ✓
              </div>
              <h2 style={{ fontFamily: "'Outfit', sans-serif", fontSize: 21, color: "#0D2B45", margin: "0 0 9px" }}>
                ¡Contraseña actualizada!
              </h2>
              <p style={{ fontFamily: "'DM Sans', sans-serif", fontSize: 13, color: "#333333", lineHeight: 1.55, margin: "0 0 22px" }}>
                Tu contraseña se ha cambiado con éxito. Ya puedes iniciar sesión con tu nueva credencial.
              </p>
              <PrimaryButton onClick={resetRecovery}>Iniciar Sesión</PrimaryButton>
            </div>
          </div>
        )}
      </div>
    )
  }

  return (
    <div
      style={{
        width: 375,
        height: 812,
        background: "#F4F7F9",
        borderRadius: 40,
        overflow: "hidden",
        boxShadow:
          "0 32px 80px rgba(13,43,69,0.35), 0 8px 24px rgba(0,0,0,0.2)",
        display: "flex",
        flexDirection: "column",
        flexShrink: 0,
        position: "relative",
      }}
    >
      <div style={{ height: 52 }} />
      <div style={{ padding: "32px 28px 0", textAlign: "center" }}>
        <div
          style={{
            margin: "0 auto 24px",
            display: "flex",
            justifyContent: "center",
          }}
        >
          <KarsyLogo size={52} />
        </div>
        <h1
          style={{
            fontFamily: "'Outfit', sans-serif",
            fontSize: 26,
            fontWeight: 700,
            color: "#333333",
            margin: "0 0 8px",
            letterSpacing: "-0.02em",
            lineHeight: 1.2,
            position: "relative",
          }}
        >
          Iniciar Sesión&nbsp;&nbsp;
        </h1>
      </div>
      <div style={{ padding: "36px 28px 0", flex: 1 }}>
        <div style={{ marginBottom: 14 }}>
          <label
            style={{
              display: "block",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 12,
              fontWeight: 600,
              color: "#333333",
              marginBottom: 8,
              letterSpacing: "0.04em",
              textTransform: "uppercase",
            }}
          >
            Correo electrónico
          </label>
          <div style={{ position: "relative" }}>
            <div
              style={{
                position: "absolute",
                left: 16,
                top: "50%",
                transform: "translateY(-50%)",
                display: "flex",
                alignItems: "center",
                pointerEvents: "none",
              }}
            >
              <MailIcon />
            </div>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="ejemplo@correo.com"
              style={{
                width: "100%",
                padding: "15px 16px 15px 48px",
                borderRadius: 12,
                border: "1.5px solid #E2E8ED",
                background: "#FFFFFF",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 15,
                color: "#333333",
                transition: "border-color 0.2s, box-shadow 0.2s",
              }}
            />
          </div>
        </div>
        <div style={{ marginBottom: 10 }}>
          <label
            style={{
              display: "block",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 12,
              fontWeight: 600,
              color: "#333333",
              marginBottom: 8,
              letterSpacing: "0.04em",
              textTransform: "uppercase",
            }}
          >
            Contraseña
          </label>
          <div style={{ position: "relative" }}>
            <div
              style={{
                position: "absolute",
                left: 16,
                top: "50%",
                transform: "translateY(-50%)",
                display: "flex",
                alignItems: "center",
                pointerEvents: "none",
              }}
            >
              <LockIcon />
            </div>
            <input
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••••••"
              style={{
                width: "100%",
                padding: "15px 48px 15px 48px",
                borderRadius: 12,
                border: "1.5px solid #E2E8ED",
                background: "#FFFFFF",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 15,
                color: "#333333",
                transition: "border-color 0.2s, box-shadow 0.2s",
              }}
            />
            <button
              onClick={() => setShowPassword(!showPassword)}
              style={{
                position: "absolute",
                right: 16,
                top: "50%",
                transform: "translateY(-50%)",
                background: "none",
                border: "none",
                cursor: "pointer",
                display: "flex",
                alignItems: "center",
                padding: 0,
              }}
            >
              <EyeOffIcon />
            </button>
          </div>
        </div>
        <div style={{ textAlign: "right", marginBottom: 32 }}>
          <button
            style={{
              background: "none",
              border: "none",
              cursor: "pointer",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 13,
              color: "#52A3AA",
              fontWeight: 500,
              textDecoration: "underline",
              textUnderlineOffset: 3,
            }}
            onClick={() => setRecoveryStep(1)}
          >
            ¿Olvidaste tu contraseña?
          </button>
        </div>
        <button
          style={{
            width: "100%",
            padding: "17px 0",
            borderRadius: 14,
            background: "#0D2B45",
            border: "none",
            color: "#FFFFFF",
            fontFamily: "'Outfit', sans-serif",
            fontSize: 16,
            fontWeight: 600,
            letterSpacing: "0.01em",
            cursor: "pointer",
            transition: "background 0.2s, transform 0.1s",
            boxShadow: "0 4px 16px rgba(13,43,69,0.3)",
          }}
          onMouseEnter={(e) => {
            ;(e.currentTarget as HTMLButtonElement).style.background = "#112f50"
            ;(e.currentTarget as HTMLButtonElement).style.transform =
              "translateY(-1px)"
          }}
          onMouseLeave={(e) => {
            ;(e.currentTarget as HTMLButtonElement).style.background = "#0D2B45"
            ;(e.currentTarget as HTMLButtonElement).style.transform =
              "translateY(0)"
          }}
        >
          Iniciar Sesión
        </button>
      </div>
      <div style={{ padding: "24px 28px 40px", textAlign: "center" }}>
        <span
          style={{
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 14,
            color: "#333333",
          }}
        >
          ¿Aún no tienes cuenta?{" "}
        </span>
        <button
          style={{
            background: "none",
            border: "none",
            cursor: "pointer",
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 14,
            fontWeight: 600,
            color: "#52A3AA",
            padding: 0,
            textDecoration: "underline",
            textUnderlineOffset: 3,
          }}
        >
          Registrarse
        </button>
      </div>
    </div>
  )
}

const FEATURED_CARS = [
  {
    id: 1,
    brand: "BMW",
    model: "Serie 3 320i",
    year: 2023,
    price: "$685,000",
    desc: "Sedán premium con tecnología deportiva, pantalla 12.3'' y asistente de manejo.",
    img: "https://images.unsplash.com/photo-1783740486439-b4487fad649f?w=700&h=440&fit=crop&auto=format",
    badge: "Destacado",
  },
  {
    id: 2,
    brand: "Porsche",
    model: "Cayenne S",
    year: 2022,
    price: "$1,450,000",
    desc: "SUV de alto rendimiento, 440 CV, interior en piel Nappa y techo panorámico.",
    img: "https://images.unsplash.com/photo-1763165524637-9067debdc80b?w=700&h=440&fit=crop&auto=format",
    badge: "Premium",
  },
  {
    id: 3,
    brand: "Mercedes-Benz",
    model: "GLA 200",
    year: 2023,
    price: "$798,000",
    desc: "Crossover compacto con MBUX, cámara 360° y acabados de lujo de serie.",
    img: "https://images.unsplash.com/photo-1788178243401-854e12eeb8b1?w=700&h=440&fit=crop&auto=format",
    badge: "Seminuevo",
  },
  {
    id: 4,
    brand: "Audi",
    model: "A4 2.0 TFSI",
    year: 2022,
    price: "$730,000",
    desc: "Sedán ejecutivo con Virtual Cockpit, tracción quattro y faros Matrix LED.",
    img: "https://images.unsplash.com/photo-1764013290175-2b76e9a00b2e?w=700&h=440&fit=crop&auto=format",
    badge: "Destacado",
  },
]

const ALL_CARS = [
  {
    id: 5,
    brand: "Toyota",
    model: "Corolla LE",
    year: 2022,
    price: "$325,000",
    desc: "Sedán confiable, eficiente en combustible con seguridad Toyota Safety Sense.",
    img: "https://images.unsplash.com/photo-1610809589386-9ea41901eb54?w=500&h=320&fit=crop&auto=format",
  },
  {
    id: 6,
    brand: "Honda",
    model: "Civic Sport",
    year: 2021,
    price: "$298,000",
    desc: "Compacto deportivo con pantalla táctil de 7'', carplay y frenos ABS.",
    img: "https://images.unsplash.com/photo-1629538745524-5b748fddac9f?w=500&h=320&fit=crop&auto=format",
  },
  {
    id: 7,
    brand: "Mazda",
    model: "3 Sedán i Sport",
    year: 2023,
    price: "$365,000",
    desc: "Diseño KODO premiado, motor Skyactiv-G y sistema de sonido Bose.",
    img: "https://images.unsplash.com/photo-1522770450359-3de04ff5c9e2?w=500&h=320&fit=crop&auto=format",
  },
  {
    id: 8,
    brand: "Nissan",
    model: "Versa Advance",
    year: 2022,
    price: "$245,000",
    desc: "El sedán más vendido de México, económico y con excelente maniobrabilidad.",
    img: "https://images.unsplash.com/photo-1602791036370-b00a495d8a58?w=500&h=320&fit=crop&auto=format",
  },
  {
    id: 9,
    brand: "Volkswagen",
    model: "Jetta Trendline",
    year: 2020,
    price: "$280,000",
    desc: "Sedán alemán de clase media con acabados de primera y gran espacio interior.",
    img: "https://images.unsplash.com/photo-1647588854348-f3b37a0f0ef7?w=500&h=320&fit=crop&auto=format",
  },
  {
    id: 10,
    brand: "Kia",
    model: "Forte EX",
    year: 2023,
    price: "$340,000",
    desc: "Diseño dinámico, garantía 7 años, climatizador automático y control crucero.",
    img: "https://images.unsplash.com/photo-1770936044591-979681c051cf?w=500&h=320&fit=crop&auto=format",
  },
]

interface CarDetail {
  id: number
  brand: string
  model: string
  year: number
  price: string
  desc: string
  img: string
  badge?: string
  transmision: string
  kilometraje: string
  cilindros: string
  caballos: string
  tipoCarro: string
  color: string
  cantDueños: string
  contactoNombre: string
  contactoTelefono: string
  contactoCorreo: string
  detalles: string
  descripcionLarga: string
}

function enrichCar(
  car: typeof ALL_CARS[0] | typeof FEATURED_CARS[0],
): CarDetail {
  return {
    ...car,
    transmision: "Automática",
    kilometraje: "45,000 km",
    cilindros: "4",
    caballos: "158 HP",
    tipoCarro: "Sedán",
    color: "Blanco perla",
    cantDueños: "1",
    contactoNombre: "Pamela Rodríguez",
    contactoTelefono: "+52 55 1234 5678",
    contactoCorreo: "pamela@correo.com",
    detalles:
      "Sin golpes ni rayones visibles. Pintura original. Mantenimiento al día en agencia. Llantas nuevas, factura original.",
    descripcionLarga: car.desc,
  }
}

function HeartIcon({ filled, size = 18 }: { filled?: boolean size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill={filled ? "#52A3AA" : "none"}
      stroke={filled ? "#52A3AA" : "#8E9A8E"}
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
    </svg>
  )
}

function WarningIcon({ size = 18 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="#8E9A8E"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M10.3 3.7 2.5 17.2A2 2 0 0 0 4.2 20h15.6a2 2 0 0 0 1.7-2.8L13.7 3.7a2 2 0 0 0-3.4 0z" />
      <line x1="12" y1="9" x2="12" y2="13" />
      <line x1="12" y1="17" x2="12.01" y2="17" />
    </svg>
  )
}

function DashboardIcon({ size = 18 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="#0D2B45"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <rect x="3" y="3" width="7" height="9" rx="1.5" />
      <rect x="14" y="3" width="7" height="5" rx="1.5" />
      <rect x="14" y="12" width="7" height="9" rx="1.5" />
      <rect x="3" y="16" width="7" height="5" rx="1.5" />
    </svg>
  )
}

function NavIcon({ path, active }: { path: string active: boolean }) {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke={active ? "#FFFFFF" : "rgba(255,255,255,0.65)"}
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d={path} />
    </svg>
  )
}

const NAV_ICON_PATHS: Record<string, string> = {
  Inicio: "M3 11.5 12 4l9 7.5M5 10v10h14V10",
  Usuarios:
    "M17 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75",
  Lotes:
    "M3 21h18M5 21V7l7-4 7 4v14M9 21v-6h6v6M9 11h.01M15 11h.01M9 15h.01M15 15h.01",
  Vehículos:
    "M5 17h14M5 17a2 2 0 1 0 4 0M5 17l1.5-5h11L19 17M15 17a2 2 0 1 0 4 0",
  Consultas:
    "M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5Z",
  Reportes: "M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1ZM4 22V15",
}

function StarBadge({ text }: { text: string }) {
  return (
    <span
      style={{
        display: "inline-block",
        padding: "4px 10px",
        borderRadius: 20,
        background: "#0D2B45",
        color: "#FFFFFF",
        fontFamily: "'DM Sans', sans-serif",
        fontSize: 11,
        fontWeight: 600,
        letterSpacing: "0.04em",
      }}
    >
      {text}
    </span>
  )
}

function FeaturedCard({
  car,
  onSelect,
  onRequireRegister,
}: {
  car: typeof FEATURED_CARS[0]
  onSelect: () => void
  onRequireRegister: (msg: string) => void
}) {
  const userMode = useContext(UserModeContext)
  const [fav, setFav] = useState(false)

  const handleFav = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (userMode === "visitor") {
      onRequireRegister("Regístrate para guardar tus favoritos")
      return
    }
    setFav(!fav)
  }

  return (
    <div
      onClick={onSelect}
      style={{
        minWidth: 320,
        width: 320,
        borderRadius: 20,
        background: "#FFFFFF",
        boxShadow: "0 4px 24px rgba(13,43,69,0.10)",
        overflow: "hidden",
        flexShrink: 0,
        transition: "transform 0.2s, box-shadow 0.2s",
        cursor: "pointer",
      }}
      onMouseEnter={(e) => {
        ;(e.currentTarget as HTMLDivElement).style.transform =
          "translateY(-4px)"
        ;(e.currentTarget as HTMLDivElement).style.boxShadow =
          "0 12px 32px rgba(13,43,69,0.16)"
      }}
      onMouseLeave={(e) => {
        ;(e.currentTarget as HTMLDivElement).style.transform = "translateY(0)"
        ;(e.currentTarget as HTMLDivElement).style.boxShadow =
          "0 4px 24px rgba(13,43,69,0.10)"
      }}
    >
      <div style={{ position: "relative", height: 200, background: "#dde6ec" }}>
        <img
          src={car.img}
          alt={`${car.brand} ${car.model}`}
          style={{
            width: "100%",
            height: "100%",
            objectFit: "cover",
            display: "block",
          }}
        />
        <div style={{ position: "absolute", top: 12, left: 12 }}>
          <StarBadge text={car.badge} />
        </div>
        {userMode !== "admin" && (
          <button
            onClick={handleFav}
            style={{
              position: "absolute",
              top: 10,
              right: 12,
            background: "#FFFFFF",
            border: "none",
            borderRadius: "50%",
            width: 34,
            height: 34,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            cursor: "pointer",
            boxShadow: "0 2px 8px rgba(0,0,0,0.12)",
            }}
          >
            <HeartIcon filled={fav} />
          </button>
        )}
      </div>
      <div style={{ padding: "16px 18px 20px" }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-start",
            marginBottom: 6,
          }}
        >
          <div>
            <div
              style={{
                fontFamily: "'Outfit', sans-serif",
                fontSize: 17,
                fontWeight: 700,
                color: "#0D2B45",
                lineHeight: 1.2,
              }}
            >
              {car.brand} {car.model}
            </div>
            <div
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 13,
                color: "#8E9A8E",
                marginTop: 2,
              }}
            >
              {car.year}
            </div>
          </div>
          <div
            style={{
              fontFamily: "'Outfit', sans-serif",
              fontSize: 17,
              fontWeight: 700,
              color: "#52A3AA",
              whiteSpace: "nowrap",
            }}
          >
            {car.price}{" "}
            <span style={{ fontSize: 11, fontWeight: 500, color: "#8E9A8E" }}>
              MXN
            </span>
          </div>
        </div>
        <p
          style={{
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 13,
            color: "#667085",
            margin: 0,
            lineHeight: 1.5,
            display: "-webkit-box",
            WebkitLineClamp: 2,
            WebkitBoxOrient: "vertical",
            overflow: "hidden",
          }}
        >
          {car.desc}
        </p>
      </div>
    </div>
  )
}

function VehicleCard({
  car,
  onSelect,
  onRequireRegister,
}: {
  car: typeof ALL_CARS[0]
  onSelect: () => void
  onRequireRegister: (msg: string) => void
}) {
  const userMode = useContext(UserModeContext)
  const [fav, setFav] = useState(false)

  const handleFav = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (userMode === "visitor") {
      onRequireRegister("Regístrate para guardar tus favoritos")
      return
    }
    setFav(!fav)
  }

  return (
    <div
      onClick={onSelect}
      style={{
        borderRadius: 16,
        background: "#FFFFFF",
        boxShadow: "0 2px 16px rgba(13,43,69,0.08)",
        overflow: "hidden",
        transition: "transform 0.2s, box-shadow 0.2s",
        cursor: "pointer",
      }}
      onMouseEnter={(e) => {
        ;(e.currentTarget as HTMLDivElement).style.transform =
          "translateY(-3px)"
        ;(e.currentTarget as HTMLDivElement).style.boxShadow =
          "0 8px 28px rgba(13,43,69,0.14)"
      }}
      onMouseLeave={(e) => {
        ;(e.currentTarget as HTMLDivElement).style.transform = "translateY(0)"
        ;(e.currentTarget as HTMLDivElement).style.boxShadow =
          "0 2px 16px rgba(13,43,69,0.08)"
      }}
    >
      <div style={{ position: "relative", height: 180, background: "#dde6ec" }}>
        <img
          src={car.img}
          alt={`${car.brand} ${car.model}`}
          style={{
            width: "100%",
            height: "100%",
            objectFit: "cover",
            display: "block",
          }}
        />
        {userMode !== "admin" && (
          <button
            onClick={handleFav}
            style={{
              position: "absolute",
              top: 10,
              right: 10,
            background: "#FFFFFF",
            border: "none",
            borderRadius: "50%",
            width: 32,
            height: 32,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            cursor: "pointer",
            boxShadow: "0 2px 8px rgba(0,0,0,0.12)",
            }}
          >
            <HeartIcon filled={fav} size={16} />
          </button>
        )}
      </div>
      <div style={{ padding: "14px 16px 18px" }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-start",
            marginBottom: 4,
          }}
        >
          <div>
            <div
              style={{
                fontFamily: "'Outfit', sans-serif",
                fontSize: 15,
                fontWeight: 700,
                color: "#0D2B45",
              }}
            >
              {car.brand} {car.model}
            </div>
            <div
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 12,
                color: "#8E9A8E",
                marginTop: 1,
              }}
            >
              {car.year}
            </div>
          </div>
          <div
            style={{
              fontFamily: "'Outfit', sans-serif",
              fontSize: 15,
              fontWeight: 700,
              color: "#52A3AA",
              textAlign: "right",
            }}
          >
            {car.price}
            <div
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 10,
                fontWeight: 400,
                color: "#8E9A8E",
              }}
            >
              MXN
            </div>
          </div>
        </div>
        <p
          style={{
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 12,
            color: "#667085",
            margin: "6px 0 0",
            lineHeight: 1.5,
            display: "-webkit-box",
            WebkitLineClamp: 2,
            WebkitBoxOrient: "vertical",
            overflow: "hidden",
          }}
        >
          {car.desc}
        </p>
      </div>
    </div>
  )
}

function WebSubHeader({ title, onBack }: { title: string onBack: () => void }) {
  return (
    <header
      style={{
        position: "sticky",
        top: 0,
        zIndex: 100,
        background: "#FFFFFF",
        borderBottom: "1px solid #E2E8ED",
        boxShadow: "0 2px 12px rgba(13,43,69,0.07)",
      }}
    >
      <div
        style={{
          maxWidth: 720,
          margin: "0 auto",
          padding: "0 32px",
          height: 60,
          display: "flex",
          alignItems: "center",
          gap: 12,
        }}
      >
        <button
          onClick={onBack}
          style={{
            display: "flex",
            alignItems: "center",
            gap: 6,
            background: "none",
            border: "none",
            cursor: "pointer",
            color: "#0D2B45",
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 14,
            fontWeight: 600,
            padding: "6px 10px 6px 2px",
            borderRadius: 8,
            transition: "background 0.15s",
          }}
          onMouseEnter={(e) => {
            ;(e.currentTarget as HTMLButtonElement).style.background = "#F4F7F9"
          }}
          onMouseLeave={(e) => {
            ;(e.currentTarget as HTMLButtonElement).style.background = "none"
          }}
        >
          <BackArrow />
          Volver
        </button>
        <div style={{ flex: 1 }} />
        <span
          style={{
            fontFamily: "'Outfit', sans-serif",
            fontWeight: 700,
            fontSize: 17,
            color: "#0D2B45",
          }}
        >
          {title}
        </span>
        <div style={{ flex: 1 }} />
        <div style={{ width: 64 }} />
      </div>
    </header>
  )
}

function WebHistorialView({ onBack }: { onBack: () => void }) {
  const items = [
    {
      title: "Honda Civic Sport 2021",
      price: "$298,000 MXN",
      sub: "Visto hace 2 horas",
      img: PROFILE_POST_IMGS[1],
    },
    {
      title: "Toyota Corolla LE 2022",
      price: "$325,000 MXN",
      sub: "Visto ayer",
      img: PROFILE_POST_IMGS[0],
    },
    {
      title: "Mazda 3 Sedán 2023",
      price: "$365,000 MXN",
      sub: "Visto hace 3 días",
      img: PROFILE_POST_IMGS[7],
    },
    {
      title: "BMW Serie 3 320i 2023",
      price: "$685,000 MXN",
      sub: "Visto hace 5 días",
      img: PROFILE_POST_IMGS[2],
    },
    {
      title: "Porsche Cayenne S 2022",
      price: "$1,450,000 MXN",
      sub: "Visto hace 1 semana",
      img: PROFILE_POST_IMGS[4],
    },
    {
      title: "Nissan Versa Advance 2022",
      price: "$245,000 MXN",
      sub: "Visto hace 1 semana",
      img: PROFILE_POST_IMGS[3],
    },
  ]
  return (
    <div style={{ minHeight: "100vh", background: "#F4F7F9" }}>
      <WebSubHeader title="🕘 Historial" onBack={onBack} />
      <div
        style={{ maxWidth: 720, margin: "0 auto", padding: "32px 32px 80px" }}
      >
        <div
          style={{
            background: "#FFFFFF",
            borderRadius: 20,
            border: "1px solid #E2E8ED",
            boxShadow: "0 2px 12px rgba(13,43,69,0.07)",
            overflow: "hidden",
          }}
        >
          {items.map((item, idx) => (
            <div
              key={item.title}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 16,
                padding: "16px 24px",
                borderBottom:
                  idx < items.length - 1 ? "1px solid #F4F7F9" : "none",
                cursor: "pointer",
                transition: "background 0.15s",
              }}
              onMouseEnter={(e) => {
                ;(e.currentTarget as HTMLDivElement).style.background =
                  "#F4F7F9"
              }}
              onMouseLeave={(e) => {
                ;(e.currentTarget as HTMLDivElement).style.background = "none"
              }}
            >
              <div
                style={{
                  width: 72,
                  height: 72,
                  borderRadius: 14,
                  overflow: "hidden",
                  flexShrink: 0,
                  background: "#dde6ec",
                }}
              >
                <img
                  src={item.img}
                  alt={item.title}
                  style={{ width: "100%", height: "100%", objectFit: "cover" }}
                />
              </div>
              <div style={{ flex: 1 }}>
                <div
                  style={{
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: 15,
                    fontWeight: 600,
                    color: "#0D2B45",
                    marginBottom: 2,
                  }}
                >
                  {item.title}
                </div>
                <div
                  style={{
                    fontFamily: "'Outfit', sans-serif",
                    fontSize: 15,
                    fontWeight: 700,
                    color: "#52A3AA",
                    marginBottom: 2,
                  }}
                >
                  {item.price}
                </div>
                <div
                  style={{
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: 12,
                    color: "#8E9A8E",
                  }}
                >
                  {item.sub}
                </div>
              </div>
              <svg
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#C4CCCC"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function WebConfigView({ onBack }: { onBack: () => void }) {
  return (
    <div style={{ minHeight: "100vh", background: "#F4F7F9" }}>
      <WebSubHeader title="⚙️ Configuración" onBack={onBack} />
      <div
        style={{ maxWidth: 720, margin: "0 auto", padding: "32px 32px 80px" }}
      >
        <div
          style={{
            background: "#FFFFFF",
            borderRadius: 20,
            border: "1px solid #E2E8ED",
            boxShadow: "0 2px 12px rgba(13,43,69,0.07)",
            overflow: "hidden",
            marginBottom: 20,
          }}
        >
          {[
            {
              icon: "🔔",
              label: "Notificaciones",
              desc: "Alertas y avisos de tu cuenta",
            },
            {
              icon: "🔒",
              label: "Privacidad y seguridad",
              desc: "Contraseña, accesos y sesiones",
            },
            { icon: "🌐", label: "Idioma", desc: "Español (México)" },
            {
              icon: "📄",
              label: "Términos y condiciones",
              desc: "Políticas de privacidad y uso",
            },
          ].map((item, idx, arr) => (
            <button
              key={item.label}
              style={{
                width: "100%",
                display: "flex",
                alignItems: "center",
                gap: 16,
                padding: "18px 24px",
                background: "none",
                border: "none",
                borderBottom:
                  idx < arr.length - 1 ? "1px solid #F4F7F9" : "none",
                cursor: "pointer",
                textAlign: "left",
                transition: "background 0.15s",
              }}
              onMouseEnter={(e) => {
                ;(e.currentTarget as HTMLButtonElement).style.background =
                  "#F4F7F9"
              }}
              onMouseLeave={(e) => {
                ;(e.currentTarget as HTMLButtonElement).style.background =
                  "none"
              }}
            >
              <span style={{ fontSize: 22, width: 36, textAlign: "center" }}>
                {item.icon}
              </span>
              <div style={{ flex: 1 }}>
                <div
                  style={{
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: 15,
                    fontWeight: 600,
                    color: "#333333",
                  }}
                >
                  {item.label}
                </div>
                <div
                  style={{
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: 13,
                    color: "#8E9A8E",
                    marginTop: 2,
                  }}
                >
                  {item.desc}
                </div>
              </div>
              <svg
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#C4CCCC"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </button>
          ))}
        </div>
        <div
          style={{
            background: "#FFFFFF",
            borderRadius: 16,
            border: "1px solid #FECACA",
            overflow: "hidden",
          }}
        >
          <button
            style={{
              width: "100%",
              display: "flex",
              alignItems: "center",
              gap: 14,
              padding: "16px 24px",
              background: "none",
              border: "none",
              cursor: "pointer",
              textAlign: "left",
            }}
          >
            <span style={{ fontSize: 20 }}>🚪</span>
            <span
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 15,
                fontWeight: 600,
                color: "#D93025",
              }}
            >
              Cerrar sesión
            </span>
          </button>
        </div>
      </div>
    </div>
  )
}

function WebProfileView({
  onBack,
  onPanel,
}: {
  onBack: () => void
  onPanel: () => void
}) {
  const [editOpen, setEditOpen] = useState(false)
  const [subView, setSubView] = useState<null | "history" | "config">(null)

  if (subView === "history")
    return <WebHistorialView onBack={() => setSubView(null)} />
  if (subView === "config")
    return <WebConfigView onBack={() => setSubView(null)} />

  return (
    <div style={{ minHeight: "100vh", background: "#F4F7F9" }}>
      <header
        style={{
          position: "sticky",
          top: 0,
          zIndex: 100,
          background: "#FFFFFF",
          borderBottom: "1px solid #E2E8ED",
          boxShadow: "0 2px 12px rgba(13,43,69,0.07)",
        }}
      >
        <div
          style={{
            maxWidth: 720,
            margin: "0 auto",
            padding: "0 32px",
            height: 60,
            display: "flex",
            alignItems: "center",
          }}
        >
          <button
            onClick={onBack}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 6,
              background: "none",
              border: "none",
              cursor: "pointer",
              color: "#0D2B45",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 14,
              fontWeight: 600,
              padding: "6px 10px 6px 2px",
              borderRadius: 8,
              transition: "background 0.15s",
            }}
            onMouseEnter={(e) => {
              ;(e.currentTarget as HTMLButtonElement).style.background =
                "#F4F7F9"
            }}
            onMouseLeave={(e) => {
              ;(e.currentTarget as HTMLButtonElement).style.background = "none"
            }}
          >
            <BackArrow />
            Inicio
          </button>
          <div style={{ flex: 1 }} />
          <span
            style={{
              fontFamily: "'Outfit', sans-serif",
              fontWeight: 700,
              fontSize: 17,
              color: "#0D2B45",
            }}
          >
            Mi perfil
          </span>
          <div style={{ flex: 1 }} />
          <button
            onClick={() => setSubView("config")}
            title="Configuración"
            style={{
              background: "none",
              border: "none",
              cursor: "pointer",
              padding: 4,
              display: "flex",
              alignItems: "center",
              color: "#0D2B45",
              width: 64,
              justifyContent: "flex-end",
            }}
          >
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="12" cy="12" r="3" />
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
          </button>
        </div>
      </header>

      <div
        style={{ maxWidth: 720, margin: "0 auto", padding: "32px 32px 80px" }}
      >
        <div
          style={{
            background: "#FFFFFF",
            borderRadius: 20,
            border: "1px solid #E2E8ED",
            overflow: "hidden",
            boxShadow: "0 2px 12px rgba(13,43,69,0.07)",
            marginBottom: 16,
          }}
        >
          <div
            style={{
              height: 100,
              background: "linear-gradient(135deg, #0D2B45 0%, #52A3AA 100%)",
            }}
          />
          <div style={{ padding: "0 28px 24px", marginTop: -44 }}>
            <div
              style={{
                display: "flex",
                alignItems: "flex-end",
                justifyContent: "space-between",
                marginBottom: 14,
              }}
            >
              <div style={{ position: "relative" }}>
                <div
                  style={{
                    width: 88,
                    height: 88,
                    borderRadius: "50%",
                    overflow: "hidden",
                    border: "4px solid #FFFFFF",
                    boxShadow: "0 2px 12px rgba(13,43,69,0.2)",
                  }}
                >
                  <img
                    src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=176&h=176&fit=crop&auto=format"
                    alt="Pamela Rodríguez"
                    style={{
                      width: "100%",
                      height: "100%",
                      objectFit: "cover",
                    }}
                  />
                </div>
                <div
                  style={{
                    position: "absolute",
                    bottom: 3,
                    right: 3,
                    width: 18,
                    height: 18,
                    borderRadius: "50%",
                    background: "#52A3AA",
                    border: "3px solid #FFFFFF",
                  }}
                />
              </div>
              <button
                onClick={() => setEditOpen(true)}
                style={{
                  padding: "9px 20px",
                  borderRadius: 10,
                  background: "#FFFFFF",
                  border: "1.5px solid #E2E8ED",
                  color: "#0D2B45",
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 14,
                  fontWeight: 600,
                  cursor: "pointer",
                  transition: "all 0.2s",
                  marginBottom: 4,
                }}
                onMouseEnter={(e) => {
                  ;(e.currentTarget as HTMLButtonElement).style.background =
                    "#F4F7F9"
                  ;(e.currentTarget as HTMLButtonElement).style.borderColor =
                    "#52A3AA"
                }}
                onMouseLeave={(e) => {
                  ;(e.currentTarget as HTMLButtonElement).style.background =
                    "#FFFFFF"
                  ;(e.currentTarget as HTMLButtonElement).style.borderColor =
                    "#E2E8ED"
                }}
              >
                Editar perfil
              </button>
            </div>
            <div
              style={{
                fontFamily: "'Outfit', sans-serif",
                fontSize: 20,
                fontWeight: 700,
                color: "#0D2B45",
                marginBottom: 2,
              }}
            >
              Pamela Rodríguez
            </div>
            <div
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 14,
                color: "#52A3AA",
                fontWeight: 600,
                marginBottom: 8,
              }}
            >
              @pamela
            </div>
            <div
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 14,
                color: "#667085",
                lineHeight: 1.55,
              }}
            >
              Vendedora de autos confiable 🚗 · Ciudad de México
            </div>
          </div>
        </div>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr 1fr",
            gap: 12,
            marginBottom: 16,
          }}
        >
          {[
            { num: "12", label: "Publicaciones" },
            { num: "5", label: "Vendidos" },
            { num: "7", label: "Activas" },
          ].map(({ num, label }) => (
            <div
              key={label}
              style={{
                background: "#FFFFFF",
                borderRadius: 14,
                border: "1px solid #E2E8ED",
                padding: "16px 0",
                textAlign: "center",
                boxShadow: "0 1px 6px rgba(13,43,69,0.05)",
              }}
            >
              <div
                style={{
                  fontFamily: "'Outfit', sans-serif",
                  fontSize: 22,
                  fontWeight: 700,
                  color: "#0D2B45",
                }}
              >
                {num}
              </div>
              <div
                style={{
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 12,
                  color: "#8E9A8E",
                  marginTop: 3,
                  fontWeight: 500,
                }}
              >
                {label}
              </div>
            </div>
          ))}
        </div>

        <div
          style={{
            background: "#FFFFFF",
            borderRadius: 20,
            border: "1px solid #E2E8ED",
            boxShadow: "0 2px 12px rgba(13,43,69,0.07)",
            overflow: "hidden",
          }}
        >
          <div
            style={{
              padding: "18px 22px 14px",
              borderBottom: "1px solid #F4F7F9",
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <span
              style={{
                fontFamily: "'Outfit', sans-serif",
                fontSize: 16,
                fontWeight: 700,
                color: "#0D2B45",
              }}
            >
              Mis publicaciones
            </span>
            <button
              onClick={onPanel}
              style={{
                padding: "7px 16px",
                borderRadius: 10,
                background: "#0D2B45",
                border: "none",
                color: "#FFFFFF",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 13,
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              + Mi panel
            </button>
          </div>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(3, 1fr)",
              gap: 3,
              padding: 3,
            }}
          >
            {PROFILE_POST_IMGS.map((src, i) => (
              <div
                key={i}
                style={{
                  aspectRatio: "1/1",
                  overflow: "hidden",
                  cursor: "pointer",
                  background: "#dde6ec",
                  borderRadius: 6,
                  transition: "opacity 0.2s",
                }}
                onMouseEnter={(e) => {
                  ;(e.currentTarget as HTMLDivElement).style.opacity = "0.78"
                }}
                onMouseLeave={(e) => {
                  ;(e.currentTarget as HTMLDivElement).style.opacity = "1"
                }}
              >
                <img
                  src={src}
                  alt={`Auto ${i + 1}`}
                  style={{
                    width: "100%",
                    height: "100%",
                    objectFit: "cover",
                    display: "block",
                  }}
                />
              </div>
            ))}
          </div>
        </div>
      </div>

      {editOpen && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 400,
            background: "rgba(13,43,69,0.45)",
            backdropFilter: "blur(4px)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: 24,
          }}
        >
          <div
            style={{
              background: "#FFFFFF",
              borderRadius: 20,
              width: "100%",
              maxWidth: 480,
              padding: 32,
              boxShadow: "0 24px 60px rgba(13,43,69,0.22)",
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                marginBottom: 24,
              }}
            >
              <span
                style={{
                  fontFamily: "'Outfit', sans-serif",
                  fontSize: 20,
                  fontWeight: 700,
                  color: "#0D2B45",
                }}
              >
                Editar perfil
              </span>
              <button
                onClick={() => setEditOpen(false)}
                style={{
                  background: "none",
                  border: "none",
                  cursor: "pointer",
                  padding: 4,
                }}
              >
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="#8E9A8E"
                  strokeWidth="2"
                  strokeLinecap="round"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <div
              style={{
                display: "flex",
                justifyContent: "center",
                marginBottom: 24,
              }}
            >
              <div style={{ position: "relative" }}>
                <div
                  style={{
                    width: 80,
                    height: 80,
                    borderRadius: "50%",
                    overflow: "hidden",
                    border: "3px solid #52A3AA",
                  }}
                >
                  <img
                    src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=160&h=160&fit=crop&auto=format"
                    alt="Avatar"
                    style={{
                      width: "100%",
                      height: "100%",
                      objectFit: "cover",
                    }}
                  />
                </div>
                <div
                  style={{
                    position: "absolute",
                    bottom: 0,
                    right: 0,
                    width: 26,
                    height: 26,
                    borderRadius: "50%",
                    background: "#52A3AA",
                    border: "2px solid #FFFFFF",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    cursor: "pointer",
                  }}
                >
                  <svg
                    width="13"
                    height="13"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="#FFFFFF"
                    strokeWidth="2.2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
                    <circle cx="12" cy="13" r="4" />
                  </svg>
                </div>
              </div>
            </div>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 14,
              }}
            >
              {[
                { label: "Nombre", val: "Pamela" },
                { label: "Apellido", val: "Rodríguez" },
                { label: "Teléfono", val: "+52 55 1234 5678" },
                { label: "Correo electrónico", val: "pamela@correo.com" },
              ].map(({ label, val }) => (
                <div key={label}>
                  <label
                    style={{
                      display: "block",
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 12,
                      fontWeight: 600,
                      color: "#333333",
                      marginBottom: 5,
                    }}
                  >
                    {label}
                  </label>
                  <input
                    defaultValue={val}
                    style={{
                      width: "100%",
                      padding: "11px 14px",
                      borderRadius: 10,
                      border: "1.5px solid #E2E8ED",
                      background: "#F4F7F9",
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 14,
                      color: "#333333",
                      boxSizing: "border-box",
                    }}
                  />
                </div>
              ))}
            </div>
            <div style={{ marginTop: 14 }}>
              <label
                style={{
                  display: "block",
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 12,
                  fontWeight: 600,
                  color: "#333333",
                  marginBottom: 5,
                }}
              >
                Descripción
              </label>
              <textarea
                defaultValue="Vendedora de autos confiable 🚗 | CDMX"
                rows={3}
                style={{
                  width: "100%",
                  padding: "11px 14px",
                  borderRadius: 10,
                  border: "1.5px solid #E2E8ED",
                  background: "#F4F7F9",
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 14,
                  color: "#333333",
                  resize: "none",
                  boxSizing: "border-box",
                }}
              />
            </div>
            <div style={{ display: "flex", gap: 10, marginTop: 20 }}>
              <button
                onClick={() => setEditOpen(false)}
                style={{
                  flex: 1,
                  padding: "12px 0",
                  borderRadius: 12,
                  border: "1.5px solid #E2E8ED",
                  background: "#FFFFFF",
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 14,
                  color: "#667085",
                  cursor: "pointer",
                }}
              >
                Cancelar
              </button>
              <button
                onClick={() => setEditOpen(false)}
                style={{
                  flex: 2,
                  padding: "12px 0",
                  borderRadius: 12,
                  border: "none",
                  background: "#0D2B45",
                  color: "#FFFFFF",
                  fontFamily: "'Outfit', sans-serif",
                  fontSize: 15,
                  fontWeight: 600,
                  cursor: "pointer",
                  boxShadow: "0 4px 14px rgba(13,43,69,0.25)",
                }}
              >
                Guardar cambios
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function WebFavoritesView({ onBack }: { onBack: () => void }) {
  const [favorites, setFavorites] = useState(FAVORITES.map((c) => c.id))
  const [search, setSearch] = useState("")

  const displayed = FAVORITES.filter(
    (c) =>
      c.brand.toLowerCase().includes(search.toLowerCase()) ||
      c.model.toLowerCase().includes(search.toLowerCase()),
  )

  return (
    <div style={{ minHeight: "100vh", background: "#F4F7F9" }}>
      <WebSubHeader title="❤️ Favoritos" onBack={onBack} />
      <div
        style={{ maxWidth: 720, margin: "0 auto", padding: "28px 32px 80px" }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 12,
            marginBottom: 24,
          }}
        >
          <div
            style={{
              flex: 1,
              display: "flex",
              alignItems: "center",
              background: "#FFFFFF",
              borderRadius: 12,
              border: "1.5px solid #E2E8ED",
              padding: "11px 16px",
              gap: 10,
            }}
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#8E9A8E"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Buscar en favoritos…"
              style={{
                flex: 1,
                border: "none",
                background: "none",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 15,
                color: "#333333",
                outline: "none",
              }}
            />
          </div>
          <button
            style={{
              padding: "11px 18px",
              borderRadius: 12,
              background: "#FFFFFF",
              border: "1.5px solid #E2E8ED",
              display: "flex",
              alignItems: "center",
              gap: 8,
              cursor: "pointer",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 14,
              color: "#52A3AA",
              fontWeight: 600,
            }}
          >
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#52A3AA"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <line x1="4" y1="6" x2="20" y2="6" />
              <line x1="8" y1="12" x2="16" y2="12" />
              <line x1="11" y1="18" x2="13" y2="18" />
            </svg>
            Filtrar
          </button>
        </div>

        <div
          style={{
            marginBottom: 16,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <span
            style={{
              fontFamily: "'Outfit', sans-serif",
              fontSize: 16,
              fontWeight: 700,
              color: "#0D2B45",
            }}
          >
            {displayed.length} vehículos guardados
          </span>
          <span
            style={{
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 13,
              color: "#52A3AA",
              cursor: "pointer",
              fontWeight: 600,
            }}
          >
            Ordenar ↕
          </span>
        </div>

        <div
          style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20 }}
        >
          {displayed.map((car) => {
            const isFav = favorites.includes(car.id)
            return (
              <div
                key={car.id}
                style={{
                  background: "#FFFFFF",
                  borderRadius: 18,
                  overflow: "hidden",
                  boxShadow: "0 2px 16px rgba(13,43,69,0.08)",
                  border: "1px solid #EEF1F4",
                  cursor: "pointer",
                  transition: "box-shadow 0.2s",
                }}
                onMouseEnter={(e) => {
                  ;(e.currentTarget as HTMLDivElement).style.boxShadow =
                    "0 6px 24px rgba(13,43,69,0.14)"
                }}
                onMouseLeave={(e) => {
                  ;(e.currentTarget as HTMLDivElement).style.boxShadow =
                    "0 2px 16px rgba(13,43,69,0.08)"
                }}
              >
                <div
                  style={{
                    position: "relative",
                    height: 160,
                    background: "#F4F7F9",
                  }}
                >
                  <img
                    src={car.img}
                    alt={car.model}
                    style={{
                      width: "100%",
                      height: "100%",
                      objectFit: "cover",
                    }}
                  />
                  <span
                    style={{
                      position: "absolute",
                      top: 12,
                      left: 12,
                      padding: "4px 10px",
                      borderRadius: 20,
                      background: "rgba(13,43,69,0.75)",
                      color: "#FFFFFF",
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 11,
                      fontWeight: 600,
                    }}
                  >
                    {car.condition}
                  </span>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      setFavorites((prev) =>
                        isFav
                          ? prev.filter((id) => id !== car.id)
                          : [...prev, car.id],
                      )
                    }}
                    style={{
                      position: "absolute",
                      top: 10,
                      right: 10,
                      width: 34,
                      height: 34,
                      borderRadius: "50%",
                      background: "rgba(255,255,255,0.92)",
                      border: "none",
                      cursor: "pointer",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      boxShadow: "0 1px 6px rgba(0,0,0,0.12)",
                    }}
                  >
                    <svg
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill={isFav ? "#0D2B45" : "none"}
                      stroke="#0D2B45"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                    </svg>
                  </button>
                </div>
                <div style={{ padding: "14px 16px 16px" }}>
                  <div
                    style={{
                      fontFamily: "'Outfit', sans-serif",
                      fontSize: 15,
                      fontWeight: 700,
                      color: "#333333",
                      marginBottom: 4,
                    }}
                  >
                    {car.brand} {car.model}
                  </div>
                  <div
                    style={{
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 13,
                      color: "#8E9A8E",
                      marginBottom: 10,
                    }}
                  >
                    {car.year} · ⭐ {car.rating}
                  </div>
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                    }}
                  >
                    <span
                      style={{
                        fontFamily: "'Outfit', sans-serif",
                        fontSize: 18,
                        fontWeight: 700,
                        color: "#0D2B45",
                      }}
                    >
                      {car.price}
                    </span>
                    <button
                      style={{
                        padding: "7px 14px",
                        borderRadius: 10,
                        background: "#0D2B45",
                        border: "none",
                        color: "#FFFFFF",
                        fontFamily: "'DM Sans', sans-serif",
                        fontSize: 12,
                        fontWeight: 600,
                        cursor: "pointer",
                      }}
                    >
                      Ver más
                    </button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>

        {displayed.length === 0 && (
          <div
            style={{ textAlign: "center", padding: "60px 0", color: "#8E9A8E" }}
          >
            <div style={{ fontSize: 40, marginBottom: 12 }}>🔍</div>
            <div
              style={{
                fontFamily: "'Outfit', sans-serif",
                fontSize: 16,
                fontWeight: 600,
                color: "#333333",
                marginBottom: 6,
              }}
            >
              Sin resultados
            </div>
            <div style={{ fontFamily: "'DM Sans', sans-serif", fontSize: 14 }}>
              Prueba con otro término de búsqueda.
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

function WebCarDetailView({
  car,
  onBack,
  onRequireRegister,
}: {
  car: CarDetail
  onBack: () => void
  onRequireRegister: (msg: string) => void
}) {
  const userMode = useContext(UserModeContext)
  const [fav, setFav] = useState(false)
  const [activeImg, setActiveImg] = useState(0)
  const [showReportMenu, setShowReportMenu] = useState(false)
  const [showContactSheet, setShowContactSheet] = useState(false)
  const [showSellerProfile, setShowSellerProfile] = useState(false)
  const gallery = [
    car.img,
    PROFILE_POST_IMGS[0],
    PROFILE_POST_IMGS[2],
    PROFILE_POST_IMGS[4],
  ]

  const fichaItems = [
    { label: "Modelo", value: car.model },
    { label: "Año", value: String(car.year) },
    { label: "Marca", value: car.brand },
    { label: "Transmisión", value: car.transmision },
    { label: "Kilometraje", value: car.kilometraje },
    { label: "Cilindros", value: car.cilindros },
    { label: "Caballos de fuerza", value: car.caballos },
    { label: "Tipo de carro", value: car.tipoCarro },
    { label: "Color", value: car.color },
    { label: "Cant. de dueños", value: car.cantDueños },
  ]

  const handleFav = () => {
    if (userMode === "visitor") {
      onRequireRegister("Regístrate para guardar tus favoritos")
      return
    }
    setFav(!fav)
  }

  const handleContact = () => {
    if (userMode === "visitor") {
      onRequireRegister("Regístrate para contactar al vendedor")
      return
    }
    setShowContactSheet(true)
  }

  const handleReport = () => {
    if (userMode === "visitor") {
      onRequireRegister("Regístrate para realizar un reporte")
      return
    }
    setShowReportMenu(true)
  }

  const submitReport = (type: "publicacion" | "usuario") => {
    setShowReportMenu(false)
    alert(
      type === "publicacion"
        ? "Reporte de publicación enviado. Nuestro equipo lo revisará."
        : "Reporte de usuario enviado. Nuestro equipo revisará la cuenta."
    )
  }

    const [webReportStep, setWebReportStep] = useState<"closed" | "type" | "reasons" | "success">("closed")
  const [webReportType, setWebReportType] = useState<"publicacion" | "cuenta" | null>(null)
  const [webReportReason, setWebReportReason] = useState("")
  const [webReportDetails, setWebReportDetails] = useState("")

  const openWebReport = () => {
    if (userMode === "visitor") {
      onRequireRegister("Regístrate para realizar un reporte")
      return
    }
    setWebReportType(null)
    setWebReportReason("")
    setWebReportDetails("")
    setWebReportStep("type")
  }

  const closeWebReport = () => {
    setWebReportStep("closed")
    setWebReportType(null)
    setWebReportReason("")
    setWebReportDetails("")
  }

  const chooseWebReportType = (type: "publicacion" | "cuenta") => {
    setWebReportType(type)
    setWebReportReason("")
    setWebReportDetails("")
    setWebReportStep("reasons")
  }

  const sendWebReport = () => {
    if (!webReportReason) return
    setWebReportStep("success")
  }

return (
    <div style={{ minHeight: "100vh", background: "#F4F7F9" }}>
      <WebSubHeader title="Detalle del vehículo" onBack={onBack} />
      <div
        style={{ maxWidth: 960, margin: "0 auto", padding: "32px 32px 80px" }}
      >
        <div style={{ marginBottom: 28 }}>
          <div
            style={{
              position: "relative",
              borderRadius: 20,
              overflow: "hidden",
              height: 480,
              background: "#dde6ec",
            }}
          >
            <img
              src={gallery[activeImg]}
              alt={`${car.brand} ${car.model}`}
              style={{ width: "100%", height: "100%", objectFit: "cover" }}
            />
            {car.badge && (
              <div style={{ position: "absolute", top: 20, left: 20 }}>
                <StarBadge text={car.badge} />
              </div>
            )}
            <span
              style={{
                position: "absolute",
                bottom: 16,
                right: 20,
                background: "rgba(0,0,0,0.55)",
                color: "#FFF",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 13,
                fontWeight: 600,
                padding: "5px 12px",
                borderRadius: 20,
              }}
            >
              {activeImg + 1}/{gallery.length}
            </span>

            <button
              onClick={() =>
                setActiveImg((activeImg - 1 + gallery.length) % gallery.length)
              }
              style={{
                position: "absolute",
                top: "50%",
                left: 16,
                transform: "translateY(-50%)",
                width: 44,
                height: 44,
                borderRadius: "50%",
                background: "rgba(255,255,255,0.92)",
                border: "none",
                cursor: "pointer",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                boxShadow: "0 2px 10px rgba(0,0,0,0.15)",
              }}
              aria-label="Anterior"
            >
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#0D2B45"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <polyline points="15 18 9 12 15 6" />
              </svg>
            </button>
            <button
              onClick={() => setActiveImg((activeImg + 1) % gallery.length)}
              style={{
                position: "absolute",
                top: "50%",
                right: 16,
                transform: "translateY(-50%)",
                width: 44,
                height: 44,
                borderRadius: "50%",
                background: "rgba(255,255,255,0.92)",
                border: "none",
                cursor: "pointer",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                boxShadow: "0 2px 10px rgba(0,0,0,0.15)",
              }}
              aria-label="Siguiente"
            >
              <svg
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#0D2B45"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </button>
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(4, 1fr)",
              gap: 12,
              marginTop: 14,
            }}
          >
            {gallery.map((src, i) => (
              <button
                key={i}
                onClick={() => setActiveImg(i)}
                style={{
                  height: 100,
                  borderRadius: 14,
                  overflow: "hidden",
                  border: `3px solid ${
                    i === activeImg ? "#0D2B45" : "transparent"
                  }`,
                  padding: 0,
                  cursor: "pointer",
                  background: "none",
                  transition: "border-color 0.2s, transform 0.15s",
                }}
                onMouseEnter={(e) => {
                  ;(e.currentTarget as HTMLButtonElement).style.transform =
                    "translateY(-2px)"
                }}
                onMouseLeave={(e) => {
                  ;(e.currentTarget as HTMLButtonElement).style.transform =
                    "translateY(0)"
                }}
              >
                <img
                  src={src}
                  alt=""
                  style={{ width: "100%", height: "100%", objectFit: "cover" }}
                />
              </button>
            ))}
          </div>
        </div>

        <div
          style={{
            display: "flex",
            alignItems: "flex-start",
            justifyContent: "space-between",
            marginBottom: 24,
            gap: 16,
            flexWrap: "wrap",
          }}
        >
          <div style={{ flex: 1, minWidth: 260 }}>
            <div
              style={{
                fontFamily: "'Outfit', sans-serif",
                fontSize: 30,
                fontWeight: 700,
                color: "#0D2B45",
                lineHeight: 1.2,
                marginBottom: 6,
              }}
            >
              {car.brand} {car.model}
            </div>
            <div
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 16,
                color: "#8E9A8E",
                fontWeight: 500,
              }}
            >
              {car.model} · {car.year}
            </div>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <div style={{ textAlign: "right" }}>
              <div
                style={{
                  fontFamily: "'Outfit', sans-serif",
                  fontSize: 34,
                  fontWeight: 700,
                  color: "#52A3AA",
                  lineHeight: 1,
                }}
              >
                {car.price}
              </div>
              <div
                style={{
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 13,
                  color: "#8E9A8E",
                  marginTop: 4,
                }}
              >
                MXN
              </div>
            </div>
            {userMode !== "admin" && (
              <div style={{ display: "flex", alignItems: "center", gap: 10, transform: "translateX(24px)" }}>
                <button
                                onClick={handleFav}
                                style={{
                                  background: "#FFFFFF",
                                  border: "1.5px solid #E2E8ED",
                                  borderRadius: 14,
                                width: 52,
                                height: 52,
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                cursor: "pointer",
                                flexShrink: 0,
                                }}
                              >
                                <HeartIcon filled={fav} size={22} />
                              </button>
                            <button
              onClick={openWebReport}
              aria-label="Reportar publicación o cuenta (web)"
              title="Reportar"
              style={{
                background: "#FFFFFF",
                border: "1.5px solid #E2E8ED",
                borderRadius: 14,
                width: 52,
                height: 52,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                cursor: "pointer",
                flexShrink: 0,
              }}
            >
              <WarningIcon size={22} />
            </button>
              </div>
            )}
          </div>
        </div>

        <SectionLabel>Ficha técnica</SectionLabel>
        <div
          style={{
            background: "#FFFFFF",
            borderRadius: 16,
            border: "1px solid #E2E8ED",
            padding: "4px 24px",
            marginBottom: 32,
            boxShadow: "0 1px 6px rgba(13,43,69,0.04)",
          }}
        >
          {fichaItems.map(({ label, value }, i) => (
            <div
              key={label}
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                padding: "14px 0",
                borderBottom:
                  i < fichaItems.length - 1 ? "1px solid #F4F7F9" : "none",
              }}
            >
              <span
                style={{
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 13,
                  color: "#8E9A8E",
                  fontWeight: 500,
                }}
              >
                {label}
              </span>
              <span
                style={{
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 14,
                  fontWeight: 700,
                  color: "#0D2B45",
                }}
              >
                {value}
              </span>
            </div>
          ))}
        </div>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr",
            gap: 20,
            marginBottom: 24,
          }}
        >
          <div
            style={{
              background: "#FFF",
              borderRadius: 16,
              border: "1px solid #E2E8ED",
              padding: 24,
            }}
          >
            <div
              style={{
                fontFamily: "'Outfit', sans-serif",
                fontSize: 14,
                fontWeight: 700,
                color: "#0D2B45",
                letterSpacing: "0.06em",
                textTransform: "uppercase",
                marginBottom: 12,
              }}
            >
              Descripción
            </div>
            <p
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 14,
                color: "#333",
                lineHeight: 1.7,
                margin: 0,
              }}
            >
              {car.descripcionLarga}
            </p>
          </div>
          <div
            style={{
              background: "#FFF",
              borderRadius: 16,
              border: "1px solid #E2E8ED",
              padding: 24,
            }}
          >
            <div
              style={{
                fontFamily: "'Outfit', sans-serif",
                fontSize: 14,
                fontWeight: 700,
                color: "#0D2B45",
                letterSpacing: "0.06em",
                textTransform: "uppercase",
                marginBottom: 12,
              }}
            >
              Detalles del auto
            </div>
            <p
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 14,
                color: "#333",
                lineHeight: 1.7,
                margin: 0,
              }}
            >
              {car.detalles}
            </p>
          </div>
        </div>

        <SectionLabel>Contacto del vendedor</SectionLabel>
        <div style={{ background: "#FFF", borderRadius: 16, border: "1px solid #E2E8ED", padding: 24, display: "flex", alignItems: "center", gap: 18, marginBottom: 24 }}>
          <button onClick={() => setShowSellerProfile(true)} aria-label={`Ver perfil de ${car.contactoNombre}`} style={{ width:64, height:64, borderRadius:"50%", overflow:"hidden", border:"2px solid #E2E8ED", padding:0, background:"#FFF", cursor:"pointer", flexShrink:0 }}>
            <img src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=128&h=128&fit=crop&auto=format" alt={car.contactoNombre} style={{ width:"100%", height:"100%", objectFit:"cover" }} />
          </button>
          <div style={{ flex:1 }}>
            <div style={{ fontFamily:"'Outfit', sans-serif", fontSize:18, fontWeight:700, color:"#0D2B45", marginBottom:5 }}>{car.contactoNombre}</div>
            <button onClick={() => setShowSellerProfile(true)} style={{ border:"none", background:"transparent", padding:0, color:"#52A3AA", fontFamily:"'DM Sans', sans-serif", fontSize:13, fontWeight:600, cursor:"pointer" }}>Ver perfil</button>
          </div>
        </div>

        <PrimaryButton onClick={handleContact}>
          Contactar vendedor
        </PrimaryButton>
      </div>

      {showContactSheet && (
        <div onClick={() => setShowContactSheet(false)} style={{ position:"fixed", inset:0, zIndex:10000, background:"rgba(13, 43, 69, 0.5)", display:"flex", alignItems:"flex-end", justifyContent:"center" }}>
          <div onClick={(e) => e.stopPropagation()} style={{ width:"min(520px, 100%)", background:"#FFFFFF", borderRadius:"26px 26px 0 0", padding:"10px 22px 30px", boxSizing:"border-box", boxShadow:"0 -12px 40px rgba(13,43,69,.2)" }}>
            <div style={{ width:44, height:4, borderRadius:999, background:"#D9E1E6", margin:"0 auto 17px" }} />
            <div style={{ position:"relative", marginBottom:18 }}>
              <div style={{ textAlign:"center", fontFamily:"'Outfit', sans-serif", fontSize:20, fontWeight:700, color:"#0D2B45" }}>Información de contacto</div>
              <button onClick={() => setShowContactSheet(false)} aria-label="Cerrar" style={{ position:"absolute", right:0, top:-6, width:34, height:34, border:"none", background:"transparent", color:"#8E9A8E", cursor:"pointer", fontSize:19 }}>✕</button>
            </div>
            <div style={{ background:"#FFF", border:"1px solid #E2E8ED", borderRadius:16, padding:17, display:"flex", alignItems:"center", gap:14, boxShadow:"0 4px 14px rgba(13,43,69,.08)", marginBottom:16 }}>
              <button onClick={() => { setShowContactSheet(false); setShowSellerProfile(true) }} style={{ width:58, height:58, borderRadius:"50%", overflow:"hidden", padding:0, border:"2px solid #E2E8ED", background:"#FFF", cursor:"pointer", flexShrink:0 }}>
                <img src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=128&h=128&fit=crop&auto=format" alt={car.contactoNombre} style={{ width:"100%", height:"100%", objectFit:"cover" }} />
              </button>
              <div><div style={{ fontFamily:"'Outfit', sans-serif", fontSize:16, fontWeight:700, color:"#0D2B45", marginBottom:4 }}>{car.contactoNombre}</div><div style={{ fontFamily:"'DM Sans', sans-serif", fontSize:13, fontWeight:700, color:"#52A3AA", marginBottom:3 }}>{car.contactoTelefono}</div><div style={{ fontFamily:"'DM Sans', sans-serif", fontSize:12.5, color:"#333" }}>{car.contactoCorreo}</div></div>
            </div>
            <div style={{ background:"#F4F7F9", borderRadius:16, padding:12, display:"grid", gap:10 }}>
              <a href={`tel:${car.contactoTelefono.replace(/\s/g, "")}`} style={{ padding:"14px 16px", borderRadius:999, background:"#0D2B45", color:"#FFF", textDecoration:"none", fontFamily:"'Outfit', sans-serif", fontSize:14, fontWeight:700, textAlign:"center" }}>☎ Llamar al vendedor</a>
              <a href={`mailto:${car.contactoCorreo}`} style={{ padding:"13px 16px", borderRadius:999, background:"#FFF", color:"#0D2B45", border:"1.5px solid #52A3AA", textDecoration:"none", fontFamily:"'Outfit', sans-serif", fontSize:14, fontWeight:700, textAlign:"center" }}>✉ Enviar correo</a>
            </div>
          </div>
        </div>
      )}

      {showSellerProfile && (
        <div style={{ position:"fixed", inset:0, zIndex:10001, background:"#F4F7F9", overflowY:"auto" }}>
          <div style={{ position:"sticky", top:0, zIndex:2, background:"#FFFFFF", borderBottom:"1px solid #E2E8ED" }}>
            <div style={{ maxWidth:720, margin:"0 auto", height:60, padding:"0 24px", display:"grid", gridTemplateColumns:"90px 1fr 90px", alignItems:"center", boxSizing:"border-box" }}>
              <button
                onClick={() => setShowSellerProfile(false)}
                style={{ border:"none", background:"transparent", cursor:"pointer", color:"#0D2B45", fontFamily:"'DM Sans', sans-serif", fontWeight:600, fontSize:14, display:"flex", alignItems:"center", gap:5, padding:0 }}
              >
                <BackArrow /> Inicio
              </button>
              <div style={{ textAlign:"center", fontFamily:"'Outfit', sans-serif", fontSize:17, fontWeight:700, color:"#0D2B45" }}>
                Perfil del vendedor
              </div>
              <div />
            </div>
          </div>

          <div style={{ maxWidth:720, margin:"0 auto", padding:"28px 24px 60px" }}>
            <div style={{ background:"#FFFFFF", borderRadius:20, border:"1px solid #E2E8ED", overflow:"hidden", boxShadow:"0 2px 12px rgba(13,43,69,0.07)", marginBottom:16 }}>
              <div style={{ height:100, background:"linear-gradient(135deg, #0D2B45 0%, #52A3AA 100%)" }} />
              <div style={{ padding:"0 28px 24px", marginTop:-44 }}>
                <div style={{ marginBottom:14 }}>
                  <div style={{ position:"relative", width:88 }}>
                    <div style={{ width:88, height:88, borderRadius:"50%", overflow:"hidden", border:"4px solid #FFFFFF", boxShadow:"0 2px 12px rgba(13,43,69,0.2)", boxSizing:"border-box" }}>
                      <img
                        src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=176&h=176&fit=crop&auto=format"
                        alt={car.contactoNombre}
                        style={{ width:"100%", height:"100%", objectFit:"cover" }}
                      />
                    </div>
                    <div style={{ position:"absolute", bottom:3, right:3, width:18, height:18, borderRadius:"50%", background:"#52A3AA", border:"3px solid #FFFFFF", boxSizing:"border-box" }} />
                  </div>
                </div>
                <div style={{ fontFamily:"'Outfit', sans-serif", fontSize:20, fontWeight:700, color:"#0D2B45", marginBottom:2 }}>
                  {car.contactoNombre}
                </div>
                <div style={{ fontFamily:"'DM Sans', sans-serif", fontSize:14, color:"#52A3AA", fontWeight:600, marginBottom:8 }}>
                  @pamela
                </div>
                <div style={{ fontFamily:"'DM Sans', sans-serif", fontSize:14, color:"#667085", lineHeight:1.55 }}>
                  Vendedora de autos confiable 🚗 · Ciudad de México
                </div>
              </div>
            </div>

            <div style={{ background:"#FFFFFF", borderRadius:20, border:"1px solid #E2E8ED", boxShadow:"0 2px 12px rgba(13,43,69,0.07)", overflow:"hidden" }}>
              <div style={{ padding:"18px 22px 14px", borderBottom:"1px solid #F4F7F9" }}>
                <span style={{ fontFamily:"'Outfit', sans-serif", fontSize:16, fontWeight:700, color:"#0D2B45" }}>
                  Publicaciones
                </span>
              </div>
              <div style={{ display:"grid", gridTemplateColumns:"repeat(3, 1fr)", gap:3, padding:3 }}>
                {[car.img, ...PROFILE_POST_IMGS.slice(0, 5)].map((src, i) => (
                  <img
                    key={`${src}-${i}`}
                    src={src}
                    alt={`Publicación ${i + 1} de ${car.contactoNombre}`}
                    style={{ width:"100%", aspectRatio:"1 / 1", objectFit:"cover", display:"block" }}
                  />
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {webReportStep !== "closed" && (
        <div
          onClick={closeWebReport}
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 9999,
            background: "rgba(13, 43, 69, 0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: 24,
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              width: "min(520px, 100%)",
              maxHeight: "82vh",
              overflowY: "auto",
              background: "#FFFFFF",
              borderRadius: 24,
              padding: "24px",
              boxSizing: "border-box",
              boxShadow: "0 20px 60px rgba(13,43,69,0.25)",
              fontFamily: "'DM Sans', sans-serif",
            }}
          >
            {webReportStep === "type" && (
              <>
                <div style={{ position: "relative", padding: "2px 40px 20px" }}>
                  <div style={{ textAlign: "center", fontFamily: "'Outfit', sans-serif", fontSize: 22, fontWeight: 700, color: "#0D2B45" }}>
                    ¿Qué deseas reportar?
                  </div>
                  <button onClick={closeWebReport} aria-label="Cerrar" style={{ position: "absolute", right: 0, top: -5, width: 34, height: 34, borderRadius: "50%", border: "none", background: "#F4F7F9", color: "#8E9A8E", cursor: "pointer", fontSize: 18 }}>✕</button>
                </div>
                {[
                  { type: "publicacion" as const, icon: "🚘⚠️", title: "Reportar publicación", desc: "Información falsa, fotos inapropiadas o posible fraude en este vehículo." },
                  { type: "cuenta" as const, icon: "👤⚠️", title: "Reportar cuenta", desc: "Comportamiento sospechoso, suplantación o acoso por parte del vendedor." },
                ].map((item) => (
                  <button key={item.type} onClick={() => chooseWebReportType(item.type)} style={{ width: "100%", display: "grid", gridTemplateColumns: "46px 1fr 22px", alignItems: "center", gap: 12, textAlign: "left", padding: "16px", marginBottom: 12, borderRadius: 16, border: "1px solid #E2E8ED", background: "#FFFFFF", boxShadow: "0 3px 12px rgba(13,43,69,0.07)", cursor: "pointer" }}>
                    <div style={{ width: 46, height: 46, borderRadius: 12, background: "#FDEDEC", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 18 }}>{item.icon}</div>
                    <div>
                      <div style={{ color: "#333333", fontWeight: 700, fontSize: 15, marginBottom: 4 }}>{item.title}</div>
                      <div style={{ color: "#8E9A8E", fontSize: 12, lineHeight: 1.45 }}>{item.desc}</div>
                    </div>
                    <span style={{ color: "#8E9A8E", fontSize: 25 }}>›</span>
                  </button>
                ))}
                <button onClick={closeWebReport} style={{ width: "100%", border: "none", background: "transparent", color: "#8E9A8E", fontWeight: 600, fontSize: 13, padding: "8px 0 0", cursor: "pointer" }}>Cancelar</button>
              </>
            )}

            {webReportStep === "reasons" && webReportType && (
              <>
                <div style={{ display: "flex", alignItems: "flex-start", gap: 10, marginBottom: 18 }}>
                  <button onClick={() => setWebReportStep("type")} style={{ border: "none", background: "#F4F7F9", color: "#0D2B45", width: 34, height: 34, borderRadius: "50%", cursor: "pointer", flexShrink: 0 }}>‹</button>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontFamily: "'Outfit', sans-serif", fontSize: 20, lineHeight: 1.25, fontWeight: 700, color: "#0D2B45" }}>
                      {webReportType === "publicacion" ? "Razones para reportar esta publicación" : "Razones para reportar al vendedor"}
                    </div>
                    <div style={{ fontSize: 12, color: "#8E9A8E", marginTop: 4 }}>Selecciona el motivo que mejor describa el problema.</div>
                  </div>
                  <button onClick={closeWebReport} aria-label="Cerrar" style={{ border: "none", background: "transparent", color: "#8E9A8E", fontSize: 18, cursor: "pointer" }}>✕</button>
                </div>

                {(webReportType === "publicacion"
                  ? [
                      ["Información engañosa o falsa", "Precio, kilometraje o datos incorrectos."],
                      ["Sospecha de fraude o estafa", ""],
                      ["Vehículo ya vendido o no disponible", ""],
                      ["Fotos de mala calidad, explícitas o robadas", ""],
                      ["Publicación duplicada", ""],
                      ["Otra razón", ""],
                    ]
                  : [
                      ["Vendedor sospechoso o posible estafador", ""],
                      ["Suplantación de identidad", ""],
                      ["Lenguaje ofensivo o acoso en mensajes", ""],
                      ["Incumplimiento de tratos o spam", ""],
                      ["Otra razón", ""],
                    ]
                ).map(([reason, desc]) => {
                  const selected = webReportReason === reason
                  return (
                    <button key={reason} onClick={() => setWebReportReason(reason)} style={{ width: "100%", display: "flex", alignItems: "flex-start", gap: 12, padding: "13px 14px", marginBottom: 8, borderRadius: 13, border: selected ? "1.5px solid #E53935" : "1px solid #E2E8ED", background: selected ? "#FDEDEC" : "#F4F7F9", cursor: "pointer", textAlign: "left" }}>
                      <span style={{ width: 18, height: 18, marginTop: 1, borderRadius: "50%", border: selected ? "5px solid #E53935" : "2px solid #B7C0C7", background: "#FFFFFF", boxSizing: "border-box", flexShrink: 0 }} />
                      <span>
                        <span style={{ display: "block", color: "#333333", fontSize: 13, fontWeight: 600 }}>{reason}</span>
                        {desc && <span style={{ display: "block", color: "#8E9A8E", fontSize: 11, marginTop: 2 }}>{desc}</span>}
                      </span>
                    </button>
                  )
                })}

                <textarea value={webReportDetails} onChange={(e) => setWebReportDetails(e.target.value)} placeholder="Escribe detalles adicionales que ayuden a la revisión (opcional)..." rows={3} style={{ width: "100%", boxSizing: "border-box", marginTop: 7, padding: "13px 14px", resize: "none", borderRadius: 13, border: "1px solid #E2E8ED", outline: "none", background: "#F4F7F9", color: "#333333", fontFamily: "'DM Sans', sans-serif", fontSize: 12.5, lineHeight: 1.45 }} />

                <button onClick={sendWebReport} disabled={!webReportReason} style={{ width: "100%", marginTop: 15, padding: "14px 0", borderRadius: 999, border: "none", background: webReportReason ? "#E53935" : "#D6DDE1", color: "#FFFFFF", fontFamily: "'Outfit', sans-serif", fontSize: 14, fontWeight: 700, cursor: webReportReason ? "pointer" : "not-allowed" }}>Enviar reporte</button>
              </>
            )}

            {webReportStep === "success" && (
              <div style={{ textAlign: "center", padding: "12px 8px 4px" }}>
                <div style={{ width: 64, height: 64, borderRadius: "50%", background: "#EAF6F6", color: "#52A3AA", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 15px", fontSize: 31, fontWeight: 700 }}>✓</div>
                <div style={{ fontFamily: "'Outfit', sans-serif", fontSize: 21, fontWeight: 700, color: "#0D2B45", marginBottom: 9 }}>Reporte enviado</div>
                <div style={{ color: "#333333", fontSize: 13, lineHeight: 1.55, margin: "0 auto 20px", maxWidth: 380 }}>Gracias por tu reporte. Nuestro equipo de administración revisará la información a la brevedad.</div>
                <button onClick={closeWebReport} style={{ width: "100%", padding: "14px 0", borderRadius: 999, border: "none", background: "#0D2B45", color: "#FFFFFF", fontFamily: "'Outfit', sans-serif", fontSize: 14, fontWeight: 700, cursor: "pointer" }}>Entendido</button>
              </div>
            )}
          </div>
        </div>
      )}

    </div>
  )
}

function CarDetailScreen({
  car,
  onBack,
  onRequireRegister,
}: {
  car: CarDetail
  onBack: () => void
  onRequireRegister: (msg: string) => void
}) {
  const userMode = useContext(UserModeContext)
  const [fav, setFav] = useState(false)
  const [activeImg, setActiveImg] = useState(0)
  const [showContactSheet, setShowContactSheet] = useState(false)
  const [showSellerProfile, setShowSellerProfile] = useState(false)
  const [reportStep, setReportStep] = useState<"closed" | "type" | "reasons" | "success">("closed")
  const [reportType, setReportType] = useState<"publicacion" | "cuenta" | null>(null)
  const [reportReason, setReportReason] = useState("")
  const [reportDetails, setReportDetails] = useState("")
  const gallery = [
    car.img,
    PROFILE_POST_IMGS[0],
    PROFILE_POST_IMGS[2],
    PROFILE_POST_IMGS[4],
  ]

  const fichaItems = [
    { label: "Modelo", value: car.model },
    { label: "Año", value: String(car.year) },
    { label: "Marca", value: car.brand },
    { label: "Transmisión", value: car.transmision },
    { label: "Kilometraje", value: car.kilometraje },
    { label: "Cilindros", value: car.cilindros },
    { label: "Caballos de fuerza", value: car.caballos },
    { label: "Tipo de carro", value: car.tipoCarro },
    { label: "Color", value: car.color },
    { label: "Cant. de dueños", value: car.cantDueños },
  ]

  const handleFav = () => {
    if (userMode === "visitor") {
      onRequireRegister("Regístrate para guardar tus favoritos")
      return
    }
    setFav(!fav)
  }

  const handleContact = () => {
    if (userMode === "visitor") {
      onRequireRegister("Regístrate para contactar al vendedor")
      return
    }
    setShowContactSheet(true)
  }

  const openReport = () => {
    if (userMode === "visitor") {
      onRequireRegister("Regístrate para realizar un reporte")
      return
    }
    setReportType(null)
    setReportReason("")
    setReportDetails("")
    setReportStep("type")
  }

  const closeReport = () => {
    setReportStep("closed")
    setReportType(null)
    setReportReason("")
    setReportDetails("")
  }

  const chooseReportType = (type: "publicacion" | "cuenta") => {
    setReportType(type)
    setReportReason("")
    setReportDetails("")
    setReportStep("reasons")
  }

  const sendReport = () => {
    if (!reportReason) return
    setReportStep("success")
  }

  return (
    <div style={{ ...phoneShell, position: "relative" }}>
      <div style={{ height: 44, flexShrink: 0, background: "#F4F7F9" }} />
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "0 20px 12px",
          background: "#F4F7F9",
          flexShrink: 0,
        }}
      >
        <button
          onClick={onBack}
          style={{
            background: "none",
            border: "none",
            cursor: "pointer",
            padding: 4,
            display: "flex",
          }}
        >
          <BackArrow />
        </button>
        <span
          style={{
            fontFamily: "'Outfit', sans-serif",
            fontWeight: 700,
            fontSize: 17,
            color: "#0D2B45",
          }}
        >
          Detalle del vehículo
        </span>
        {userMode !== "admin" && (
          <div style={{ display: "flex", alignItems: "center", gap: 8, transform: "translateX(18px)" }}>
            <button
              onClick={handleFav}
              aria-label="Agregar a favoritos"
              style={{
                background: "#FFFFFF",
                border: "1.5px solid #E2E8ED",
                borderRadius: 10,
                width: 36,
                height: 36,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                cursor: "pointer",
              }}
            >
              <HeartIcon filled={fav} size={16} />
            </button>
            <button
              onClick={openReport}
              aria-label="Reportar publicación o cuenta"
              title="Reportar"
              style={{
                background: "#FFFFFF",
                border: "1.5px solid #E2E8ED",
                borderRadius: 10,
                width: 36,
                height: 36,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                cursor: "pointer",
                flexShrink: 0,
              }}
            >
              <WarningIcon size={16} />
            </button>
          </div>
        )}
      </div>
    

      <div
        style={{
          flex: 1,
          overflowY: "auto",
          paddingBottom: 100,
          scrollbarWidth: "none",
        }}
      >
        <div
          style={{ position: "relative", height: 300, background: "#dde6ec" }}
        >
          <img
            src={gallery[activeImg]}
            alt={`${car.brand} ${car.model}`}
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
          {car.badge && (
            <div style={{ position: "absolute", top: 14, left: 14 }}>
              <StarBadge text={car.badge} />
            </div>
          )}
          <span
            style={{
              position: "absolute",
              bottom: 12,
              right: 14,
              background: "rgba(0,0,0,0.55)",
              color: "#FFF",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 11,
              fontWeight: 600,
              padding: "3px 9px",
              borderRadius: 20,
            }}
          >
            {activeImg + 1}/{gallery.length}
          </span>
        </div>

        <div style={{ display: "flex", gap: 7, padding: "10px 20px 16px" }}>
          {gallery.map((src, i) => (
            <button
              key={i}
              onClick={() => setActiveImg(i)}
              style={{
                flex: 1,
                height: 54,
                borderRadius: 10,
                overflow: "hidden",
                border: `2px solid ${
                  i === activeImg ? "#0D2B45" : "transparent"
                }`,
                padding: 0,
                cursor: "pointer",
                background: "none",
              }}
            >
              <img
                src={src}
                alt=""
                style={{ width: "100%", height: "100%", objectFit: "cover" }}
              />
            </button>
          ))}
        </div>

        <div style={{ padding: "0 20px" }}>
          <div style={{ marginBottom: 18 }}>
            <div
              style={{
                fontFamily: "'Outfit', sans-serif",
                fontSize: 20,
                fontWeight: 700,
                color: "#333333",
                marginBottom: 4,
              }}
            >
              {car.brand} {car.model} {car.year}
            </div>
            <div
              style={{
                fontFamily: "'Outfit', sans-serif",
                fontSize: 26,
                fontWeight: 700,
                color: "#52A3AA",
              }}
            >
              {car.price}{" "}
              <span style={{ fontSize: 14, fontWeight: 500, color: "#8E9A8E" }}>
                MXN
              </span>
            </div>
          </div>

          <SectionLabel>Ficha técnica</SectionLabel>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: 9,
              marginBottom: 20,
            }}
          >
            {fichaItems.map(({ label, value }) => (
              <div
                key={label}
                style={{
                  background: "#FFFFFF",
                  borderRadius: 12,
                  border: "1px solid #E2E8ED",
                  padding: "10px 12px",
                }}
              >
                <div
                  style={{
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: 11,
                    color: "#8E9A8E",
                  }}
                >
                  {label}
                </div>
                <div
                  style={{
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: 13,
                    fontWeight: 600,
                    color: "#333333",
                    marginTop: 2,
                  }}
                >
                  {value}
                </div>
              </div>
            ))}
          </div>

          <SectionLabel>Descripción</SectionLabel>
          <div
            style={{
              background: "#FFFFFF",
              borderRadius: 14,
              border: "1px solid #E2E8ED",
              padding: "14px 16px",
              marginBottom: 20,
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 13,
              color: "#333333",
              lineHeight: 1.6,
            }}
          >
            {car.descripcionLarga}
          </div>

          <SectionLabel>Detalles del auto</SectionLabel>
          <div
            style={{
              background: "#FFFFFF",
              borderRadius: 14,
              border: "1px solid #E2E8ED",
              padding: "14px 16px",
              marginBottom: 20,
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 13,
              color: "#333333",
              lineHeight: 1.6,
            }}
          >
            {car.detalles}
          </div>

          <SectionLabel>Contacto del vendedor</SectionLabel>
          <div
            style={{
              background: "#FFFFFF",
              borderRadius: 14,
              border: "1px solid #E2E8ED",
              padding: "14px 16px",
              marginBottom: 20,
              display: "flex",
              alignItems: "center",
              gap: 12,
            }}
          >
            <button
              onClick={() => setShowSellerProfile(true)}
              aria-label={`Ver perfil de ${car.contactoNombre}`}
              style={{
                width: 48,
                height: 48,
                borderRadius: "50%",
                overflow: "hidden",
                flexShrink: 0,
                border: "2px solid #E2E8ED",
                padding: 0,
                background: "#FFFFFF",
                cursor: "pointer",
              }}
            >
              <img
                src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=88&h=88&fit=crop&auto=format"
                alt={car.contactoNombre}
                style={{ width: "100%", height: "100%", objectFit: "cover" }}
              />
            </button>

            <div style={{ flex: 1 }}>
              <div
                style={{
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 14,
                  fontWeight: 700,
                  color: "#0D2B45",
                }}
              >
                {car.contactoNombre}
              </div>
              <button
                onClick={() => setShowSellerProfile(true)}
                style={{
                  border: "none",
                  background: "transparent",
                  padding: "3px 0 0",
                  color: "#52A3AA",
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 11.5,
                  fontWeight: 600,
                  cursor: "pointer",
                }}
              >
                Ver perfil
              </button>
            </div>
          </div>


        </div>
      </div>


      {showContactSheet && (
        <div
          onClick={() => setShowContactSheet(false)}
          style={{
            position: "absolute",
            inset: 0,
            zIndex: 90,
            background: "rgba(13, 43, 69, 0.5)",
            display: "flex",
            alignItems: "flex-end",
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              width: "100%",
              background: "#FFFFFF",
              borderRadius: "26px 26px 0 0",
              padding: "10px 20px 28px",
              boxSizing: "border-box",
              boxShadow: "0 -12px 40px rgba(13,43,69,0.18)",
            }}
          >
            <div style={{ width: 42, height: 4, borderRadius: 999, background: "#D9E1E6", margin: "0 auto 16px" }} />

            <div style={{ position: "relative", marginBottom: 18 }}>
              <div
                style={{
                  textAlign: "center",
                  fontFamily: "'Outfit', sans-serif",
                  fontSize: 19,
                  fontWeight: 700,
                  color: "#0D2B45",
                }}
              >
                Información de contacto
              </div>
              <button
                onClick={() => setShowContactSheet(false)}
                aria-label="Cerrar"
                style={{
                  position: "absolute",
                  right: 0,
                  top: -6,
                  width: 32,
                  height: 32,
                  borderRadius: "50%",
                  border: "none",
                  background: "transparent",
                  color: "#8E9A8E",
                  cursor: "pointer",
                  fontSize: 18,
                }}
              >
                ✕
              </button>
            </div>

            <div
              style={{
                background: "#FFFFFF",
                border: "1px solid #E2E8ED",
                borderRadius: 16,
                padding: 16,
                display: "flex",
                alignItems: "center",
                gap: 14,
                boxShadow: "0 4px 14px rgba(13,43,69,0.08)",
                marginBottom: 16,
              }}
            >
              <button
                onClick={() => {
                  setShowContactSheet(false)
                  setShowSellerProfile(true)
                }}
                aria-label="Ver perfil del vendedor"
                style={{
                  width: 56,
                  height: 56,
                  borderRadius: "50%",
                  overflow: "hidden",
                  padding: 0,
                  border: "2px solid #E2E8ED",
                  background: "#FFFFFF",
                  cursor: "pointer",
                  flexShrink: 0,
                }}
              >
                <img
                  src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=112&h=112&fit=crop&auto=format"
                  alt={car.contactoNombre}
                  style={{ width: "100%", height: "100%", objectFit: "cover" }}
                />
              </button>

              <div style={{ minWidth: 0 }}>
                <div style={{ fontFamily: "'Outfit', sans-serif", fontSize: 16, fontWeight: 700, color: "#0D2B45", marginBottom: 4 }}>
                  {car.contactoNombre}
                </div>
                <div style={{ fontFamily: "'DM Sans', sans-serif", fontSize: 12.5, fontWeight: 700, color: "#52A3AA", marginBottom: 3 }}>
                  {car.contactoTelefono}
                </div>
                <div style={{ fontFamily: "'DM Sans', sans-serif", fontSize: 12, color: "#333333" }}>
                  {car.contactoCorreo}
                </div>
              </div>
            </div>

            <div style={{ background: "#F4F7F9", borderRadius: 16, padding: 12, display: "grid", gap: 10 }}>
              <a
                href={`tel:${car.contactoTelefono.replace(/\s/g, "")}`}
                style={{
                  padding: "14px 16px",
                  borderRadius: 999,
                  background: "#0D2B45",
                  color: "#FFFFFF",
                  textDecoration: "none",
                  fontFamily: "'Outfit', sans-serif",
                  fontSize: 14,
                  fontWeight: 700,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: 8,
                }}
              >
                ☎ Llamar al vendedor
              </a>

              <a
                href={`mailto:${car.contactoCorreo}`}
                style={{
                  padding: "13px 16px",
                  borderRadius: 999,
                  background: "#FFFFFF",
                  color: "#0D2B45",
                  border: "1.5px solid #52A3AA",
                  textDecoration: "none",
                  fontFamily: "'Outfit', sans-serif",
                  fontSize: 14,
                  fontWeight: 700,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: 8,
                }}
              >
                ✉ Enviar correo
              </a>
            </div>
          </div>
        </div>
      )}

      {showSellerProfile && (
        <div
          style={{
            position: "absolute",
            inset: 0,
            zIndex: 95,
            background: "#F4F7F9",
            display: "flex",
            flexDirection: "column",
          }}
        >
          <div style={{ height: 44, flexShrink: 0, background: "#FFFFFF" }} />

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "70px 1fr 70px",
              alignItems: "center",
              padding: "0 16px 14px",
              borderBottom: "1px solid #E2E8ED",
              background: "#FFFFFF",
            }}
          >
            <button
              onClick={() => setShowSellerProfile(false)}
              aria-label="Regresar"
              style={{
                border: "none",
                background: "transparent",
                padding: 0,
                cursor: "pointer",
                display: "flex",
                alignItems: "center",
                gap: 2,
                color: "#0D2B45",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 12,
                fontWeight: 600,
              }}
            >
              <BackArrow />
              Inicio
            </button>
            <div style={{ textAlign: "center", fontFamily: "'Outfit', sans-serif", fontSize: 17, fontWeight: 700, color: "#0D2B45" }}>
              Perfil
            </div>
            <div />
          </div>

          <div style={{ flex: 1, overflowY: "auto", padding: "28px 24px 40px" }}>
            <div
              style={{
                background: "#FFFFFF",
                borderRadius: 18,
                border: "1px solid #E2E8ED",
                overflow: "hidden",
                boxShadow: "0 2px 12px rgba(13,43,69,0.07)",
                marginBottom: 16,
              }}
            >
              <div
                style={{
                  height: 82,
                  background: "linear-gradient(135deg, #0D2B45 0%, #52A3AA 100%)",
                }}
              />
              <div style={{ padding: "0 22px 22px", marginTop: -38 }}>
                <div style={{ marginBottom: 12 }}>
                  <div style={{ position: "relative", width: 76 }}>
                    <div
                      style={{
                        width: 76,
                        height: 76,
                        borderRadius: "50%",
                        overflow: "hidden",
                        border: "4px solid #FFFFFF",
                        boxShadow: "0 2px 10px rgba(13,43,69,0.2)",
                        boxSizing: "border-box",
                      }}
                    >
                      <img
                        src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=176&h=176&fit=crop&auto=format"
                        alt={car.contactoNombre}
                        style={{ width: "100%", height: "100%", objectFit: "cover" }}
                      />
                    </div>
                    <div
                      style={{
                        position: "absolute",
                        bottom: 2,
                        right: 2,
                        width: 16,
                        height: 16,
                        borderRadius: "50%",
                        background: "#52A3AA",
                        border: "3px solid #FFFFFF",
                        boxSizing: "border-box",
                      }}
                    />
                  </div>
                </div>

                <div style={{ fontFamily: "'Outfit', sans-serif", fontSize: 19, fontWeight: 700, color: "#0D2B45", marginBottom: 2 }}>
                  {car.contactoNombre}
                </div>
                <div style={{ fontFamily: "'DM Sans', sans-serif", fontSize: 13, color: "#52A3AA", fontWeight: 600, marginBottom: 8 }}>
                  @pamela
                </div>
                <div style={{ fontFamily: "'DM Sans', sans-serif", fontSize: 12.5, color: "#667085", lineHeight: 1.5 }}>
                  Vendedora de autos confiable 🚗 · Ciudad de México
                </div>
              </div>
            </div>

            <div
              style={{
                background: "#FFFFFF",
                borderRadius: 18,
                border: "1px solid #E2E8ED",
                boxShadow: "0 2px 12px rgba(13,43,69,0.07)",
                overflow: "hidden",
              }}
            >
              <div
                style={{
                  padding: "16px 18px 12px",
                  borderBottom: "1px solid #F4F7F9",
                }}
              >
                <span style={{ fontFamily: "'Outfit', sans-serif", fontSize: 15, fontWeight: 700, color: "#0D2B45" }}>
                  Publicaciones
                </span>
              </div>

              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(3, 1fr)",
                  gap: 3,
                  padding: 3,
                }}
              >
                {[car.img, ...PROFILE_POST_IMGS.slice(0, 5)].map((src, i) => (
                  <img
                    key={`${src}-${i}`}
                    src={src}
                    alt={`Publicación ${i + 1} de ${car.contactoNombre}`}
                    style={{
                      width: "100%",
                      aspectRatio: "1 / 1",
                      objectFit: "cover",
                      display: "block",
                    }}
                  />
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {reportStep !== "closed" && (
        <div
          onClick={closeReport}
          style={{
            position: "absolute",
            inset: 0,
            zIndex: 80,
            background: "rgba(13, 43, 69, 0.5)",
            display: "flex",
            alignItems: "flex-end",
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              width: "100%",
              maxHeight: "76%",
              overflowY: "auto",
              background: "#FFFFFF",
              borderRadius: "26px 26px 0 0",
              padding: "20px 20px 28px",
              boxSizing: "border-box",
              boxShadow: "0 -12px 40px rgba(13,43,69,0.18)",
              fontFamily: "'DM Sans', sans-serif",
            }}
          >
            {reportStep === "type" && (
              <>
                <div style={{ position: "relative", padding: "2px 34px 18px" }}>
                  <div style={{ textAlign: "center", fontFamily: "'Outfit', sans-serif", fontSize: 19, fontWeight: 700, color: "#0D2B45" }}>
                    ¿Qué deseas reportar?
                  </div>
                  <button onClick={closeReport} aria-label="Cerrar" style={{ position: "absolute", right: 0, top: -5, width: 32, height: 32, borderRadius: "50%", border: "none", background: "#F4F7F9", color: "#8E9A8E", cursor: "pointer", fontSize: 18 }}>
                    ✕
                  </button>
                </div>

                {[
                  { type: "publicacion" as const, icon: "🚘⚠️", title: "Reportar publicación", desc: "Información falsa, fotos inapropiadas o posible fraude en este vehículo." },
                  { type: "cuenta" as const, icon: "👤⚠️", title: "Reportar cuenta", desc: "Comportamiento sospechoso, suplantación o acoso por parte del vendedor." },
                ].map((item) => (
                  <button key={item.type} onClick={() => chooseReportType(item.type)} style={{ width: "100%", display: "grid", gridTemplateColumns: "42px 1fr 20px", alignItems: "center", gap: 10, textAlign: "left", padding: "15px 14px", marginBottom: 12, borderRadius: 16, border: "1px solid #E2E8ED", background: "#FFFFFF", boxShadow: "0 3px 12px rgba(13,43,69,0.07)", cursor: "pointer" }}>
                    <div style={{ width: 42, height: 42, borderRadius: 12, background: "#FDEDEC", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 17 }}>{item.icon}</div>
                    <div>
                      <div style={{ color: "#333333", fontWeight: 700, fontSize: 14, marginBottom: 3 }}>{item.title}</div>
                      <div style={{ color: "#8E9A8E", fontSize: 11.5, lineHeight: 1.4 }}>{item.desc}</div>
                    </div>
                    <span style={{ color: "#8E9A8E", fontSize: 24 }}>›</span>
                  </button>
                ))}

                <button onClick={closeReport} style={{ width: "100%", border: "none", background: "transparent", color: "#8E9A8E", fontWeight: 600, fontSize: 13, padding: "8px 0 0", cursor: "pointer" }}>
                  Cancelar
                </button>
              </>
            )}

            {reportStep === "reasons" && reportType && (
              <>
                <div style={{ display: "flex", alignItems: "flex-start", gap: 8, marginBottom: 16 }}>
                  <button onClick={() => setReportStep("type")} style={{ border: "none", background: "#F4F7F9", color: "#0D2B45", width: 32, height: 32, borderRadius: "50%", cursor: "pointer", flexShrink: 0 }}>‹</button>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontFamily: "'Outfit', sans-serif", fontSize: 18, lineHeight: 1.25, fontWeight: 700, color: "#0D2B45" }}>
                      {reportType === "publicacion" ? "Razones para reportar esta publicación" : "Razones para reportar al vendedor"}
                    </div>
                    <div style={{ fontSize: 11.5, color: "#8E9A8E", marginTop: 4 }}>Selecciona el motivo que mejor describa el problema.</div>
                  </div>
                  <button onClick={closeReport} aria-label="Cerrar" style={{ border: "none", background: "transparent", color: "#8E9A8E", fontSize: 18, cursor: "pointer" }}>✕</button>
                </div>

                {(reportType === "publicacion"
                  ? [
                      ["Información engañosa o falsa", "Precio, kilometraje o datos incorrectos."],
                      ["Sospecha de fraude o estafa", ""],
                      ["Vehículo ya vendido o no disponible", ""],
                      ["Fotos de mala calidad, explícitas o robadas", ""],
                      ["Publicación duplicada", ""],
                      ["Otra razón", ""],
                    ]
                  : [
                      ["Vendedor sospechoso o posible estafador", ""],
                      ["Suplantación de identidad", ""],
                      ["Lenguaje ofensivo o acoso en mensajes", ""],
                      ["Incumplimiento de tratos o spam", ""],
                      ["Otra razón", ""],
                    ]
                ).map(([reason, desc]) => {
                  const selected = reportReason === reason
                  return (
                    <button key={reason} onClick={() => setReportReason(reason)} style={{ width: "100%", display: "flex", alignItems: "flex-start", gap: 11, padding: "12px 13px", marginBottom: 8, borderRadius: 13, border: selected ? "1.5px solid #E53935" : "1px solid #E2E8ED", background: selected ? "#FDEDEC" : "#F4F7F9", cursor: "pointer", textAlign: "left" }}>
                      <span style={{ width: 18, height: 18, marginTop: 1, borderRadius: "50%", border: selected ? "5px solid #E53935" : "2px solid #B7C0C7", background: "#FFFFFF", boxSizing: "border-box", flexShrink: 0 }} />
                      <span>
                        <span style={{ display: "block", color: "#333333", fontSize: 12.5, fontWeight: 600 }}>{reason}</span>
                        {desc && <span style={{ display: "block", color: "#8E9A8E", fontSize: 10.5, marginTop: 2 }}>{desc}</span>}
                      </span>
                    </button>
                  )
                })}

                <textarea
                  value={reportDetails}
                  onChange={(e) => setReportDetails(e.target.value)}
                  placeholder="Escribe detalles adicionales que ayuden a la revisión (opcional)..."
                  rows={3}
                  style={{ width: "100%", boxSizing: "border-box", marginTop: 6, padding: "12px 13px", resize: "none", borderRadius: 13, border: "1px solid #E2E8ED", outline: "none", background: "#F4F7F9", color: "#333333", fontFamily: "'DM Sans', sans-serif", fontSize: 12, lineHeight: 1.45 }}
                />

                <button onClick={sendReport} disabled={!reportReason} style={{ width: "100%", marginTop: 14, padding: "14px 0", borderRadius: 999, border: "none", background: reportReason ? "#E53935" : "#D6DDE1", color: "#FFFFFF", fontFamily: "'Outfit', sans-serif", fontSize: 14, fontWeight: 700, cursor: reportReason ? "pointer" : "not-allowed", boxShadow: reportReason ? "0 4px 14px rgba(229,57,53,0.22)" : "none" }}>
                  Enviar reporte
                </button>
              </>
            )}

            {reportStep === "success" && (
              <div style={{ textAlign: "center", padding: "8px 4px 2px" }}>
                <div style={{ width: 62, height: 62, borderRadius: "50%", background: "#EAF6F6", color: "#52A3AA", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 14px", fontSize: 30, fontWeight: 700 }}>✓</div>
                <div style={{ fontFamily: "'Outfit', sans-serif", fontSize: 19, fontWeight: 700, color: "#0D2B45", marginBottom: 8 }}>Reporte enviado</div>
                <div style={{ color: "#333333", fontSize: 12.5, lineHeight: 1.55, margin: "0 auto 18px", maxWidth: 290 }}>
                  Gracias por tu reporte. Nuestro equipo de administración revisará la información a la brevedad.
                </div>
                <button onClick={closeReport} style={{ width: "100%", padding: "14px 0", borderRadius: 999, border: "none", background: "#0D2B45", color: "#FFFFFF", fontFamily: "'Outfit', sans-serif", fontSize: 14, fontWeight: 700, cursor: "pointer" }}>
                  Entendido
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      <div
        style={{
          position: "absolute",
          bottom: 0,
          left: 0,
          right: 0,
          padding: "12px 20px 20px",
          background: "#F4F7F9",
          borderTop: "1px solid #E2E8ED",
        }}
      >
        <PrimaryButton onClick={handleContact}>
          Contactar vendedor
        </PrimaryButton>
      </div>
    </div>
  )
}

function AdminBadge({
  children,
  tone = "neutral",
}: {
  children: React.ReactNode
  tone?: "success" | "warning" | "danger" | "info" | "neutral"
}) {
  const styles = {
    success: { bg: "#ECFDF3", color: "#16824A" },
    warning: { bg: "#FFFAEB", color: "#B54708" },
    danger: { bg: "#FEF3F2", color: "#B42318" },
    info: { bg: "#EFF8FF", color: "#175CD3" },
    neutral: { bg: "#F2F4F7", color: "#475467" },
  }[tone]

  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        padding: "5px 9px",
        borderRadius: 999,
        background: styles.bg,
        color: styles.color,
        fontSize: 11,
        fontWeight: 700,
        whiteSpace: "nowrap",
      }}
    >
      {children}
    </span>
  )
}

function AdminSectionHeader({
  title,
  description,
  action,
  onAction,
}: {
  title: string
  description: string
  action?: string
  onAction?: () => void
}) {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "flex-start",
        justifyContent: "space-between",
        gap: 16,
        marginBottom: 20,
        flexWrap: "wrap",
      }}
    >
      <div>
        <h1
          style={{
            fontFamily: "'Outfit', sans-serif",
            fontSize: 26,
            fontWeight: 700,
            color: "#0D2B45",
            margin: "0 0 4px",
          }}
        >
          {title}
        </h1>
        <p
          style={{
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 14,
            color: "#8E9A8E",
            margin: 0,
          }}
        >
          {description}
        </p>
      </div>

      {action && (
        <button
          onClick={onAction}
          style={{
            border: "none",
            borderRadius: 10,
            background: "#0D2B45",
            color: "#FFFFFF",
            padding: "10px 15px",
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 13,
            fontWeight: 700,
            cursor: "pointer",
            boxShadow: "0 4px 12px rgba(13,43,69,0.16)",
          }}
        >
          + {action}
        </button>
      )}
    </div>
  )
}

function AdminSearchBar({
  value,
  onChange,
  placeholder,
}: {
  value: string
  onChange: (value: string) => void
  placeholder: string
}) {
  return (
    <div
      style={{
        position: "relative",
        minWidth: 240,
        flex: 1,
        maxWidth: 420,
      }}
    >
      <svg
        width="17"
        height="17"
        viewBox="0 0 24 24"
        fill="none"
        stroke="#98A2B3"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        style={{
          position: "absolute",
          left: 13,
          top: "50%",
          transform: "translateY(-50%)",
        }}
      >
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-4-4" />
      </svg>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        style={{
          width: "100%",
          boxSizing: "border-box",
          border: "1px solid #E2E8ED",
          borderRadius: 10,
          padding: "10px 14px 10px 40px",
          outline: "none",
          background: "#FFFFFF",
          color: "#333333",
          fontFamily: "'DM Sans', sans-serif",
          fontSize: 13,
        }}
      />
    </div>
  )
}

function AdminCard({
  children,
  style,
}: {
  children: React.ReactNode
  style?: React.CSSProperties
}) {
  return (
    <div
      style={{
        background: "#FFFFFF",
        borderRadius: 16,
        border: "1px solid #E2E8ED",
        boxShadow: "0 1px 6px rgba(13,43,69,0.04)",
        ...style,
      }}
    >
      {children}
    </div>
  )
}

function AdminTable({
  columns,
  rows,
}: {
  columns: string[]
  rows: React.ReactNode[][]
}) {
  return (
    <div style={{ overflowX: "auto" }}>
      <table
        style={{
          width: "100%",
          borderCollapse: "collapse",
          minWidth: 720,
        }}
      >
        <thead>
          <tr>
            {columns.map((column) => (
              <th
                key={column}
                style={{
                  textAlign: "left",
                  padding: "13px 14px",
                  borderBottom: "1px solid #E2E8ED",
                  color: "#667085",
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 11,
                  fontWeight: 700,
                  textTransform: "uppercase",
                  letterSpacing: "0.04em",
                  background: "#FCFDFD",
                  whiteSpace: "nowrap",
                }}
              >
                {column}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={index}>
              {row.map((cell, cellIndex) => (
                <td
                  key={cellIndex}
                  style={{
                    padding: "14px",
                    borderBottom:
                      index === rows.length - 1
                        ? "none"
                        : "1px solid #F0F2F4",
                    color: "#344054",
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: 13,
                    verticalAlign: "middle",
                  }}
                >
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function AdminActionButton({
  children,
  onClick,
  tone = "default",
}: {
  children: React.ReactNode
  onClick?: () => void
  tone?: "default" | "danger" | "success"
}) {
  const colors = {
    default: { bg: "#F4F7F9", color: "#0D2B45", border: "#E2E8ED" },
    danger: { bg: "#FEF3F2", color: "#B42318", border: "#FECACA" },
    success: { bg: "#ECFDF3", color: "#16824A", border: "#ABEFC6" },
  }[tone]

  return (
    <button
      onClick={onClick}
      style={{
        border: `1px solid ${colors.border}`,
        background: colors.bg,
        color: colors.color,
        borderRadius: 8,
        padding: "7px 10px",
        fontFamily: "'DM Sans', sans-serif",
        fontSize: 11,
        fontWeight: 700,
        cursor: "pointer",
        whiteSpace: "nowrap",
      }}
    >
      {children}
    </button>
  )
}

function AdminEmptyState({
  icon,
  title,
  description,
}: {
  icon: string
  title: string
  description: string
}) {
  return (
    <div
      style={{
        padding: "58px 24px",
        textAlign: "center",
        color: "#667085",
      }}
    >
      <div style={{ fontSize: 34, marginBottom: 12 }}>{icon}</div>
      <div
        style={{
          fontFamily: "'Outfit', sans-serif",
          fontSize: 17,
          fontWeight: 700,
          color: "#0D2B45",
          marginBottom: 6,
        }}
      >
        {title}
      </div>
      <div
        style={{
          maxWidth: 430,
          margin: "0 auto",
          fontSize: 13,
          lineHeight: 1.6,
        }}
      >
        {description}
      </div>
    </div>
  )
}

function AdminUsersView() {
  const [search, setSearch] = useState("")
  const [filter, setFilter] = useState("Todos")
  const [selected, setSelected] = useState<string | null>(null)

  const users = [
    { name: "María González", email: "maria.gonzalez@email.com", type: "Particular", date: "15 sep 2026", status: "Activo" },
    { name: "Carlos Ramírez", email: "carlos.ramirez@email.com", type: "Particular", date: "14 sep 2026", status: "Activo" },
    { name: "Auto Premium", email: "contacto@autopremium.com", type: "Lote", date: "13 sep 2026", status: "Activo" },
    { name: "Laura Martínez", email: "laura.martinez@email.com", type: "Particular", date: "11 sep 2026", status: "Suspendido" },
    { name: "Seminuevos del Pacífico", email: "ventas@pacifico.com", type: "Lote", date: "10 sep 2026", status: "Activo" },
    { name: "Jorge Hernández", email: "jorge.hernandez@email.com", type: "Particular", date: "09 sep 2026", status: "Activo" },
  ]

  const filtered = users.filter((user) => {
    const matchesSearch =
      user.name.toLowerCase().includes(search.toLowerCase()) ||
      user.email.toLowerCase().includes(search.toLowerCase())
    const matchesFilter = filter === "Todos" || user.type === filter
    return matchesSearch && matchesFilter
  })

  return (
    <>
      <AdminSectionHeader
        title="Usuarios"
        description="Consulta y administra las cuentas registradas en Karsy."
      />
      <AdminCard style={{ overflow: "hidden" }}>
        <div
          style={{
            padding: 16,
            display: "flex",
            gap: 10,
            flexWrap: "wrap",
            borderBottom: "1px solid #E2E8ED",
          }}
        >
          <AdminSearchBar
            value={search}
            onChange={setSearch}
            placeholder="Buscar por nombre o correo..."
          />
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            style={{
              border: "1px solid #E2E8ED",
              borderRadius: 10,
              padding: "10px 12px",
              background: "#FFFFFF",
              color: "#344054",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 13,
            }}
          >
            <option>Todos</option>
            <option>Particular</option>
            <option>Lote</option>
          </select>
        </div>

        {filtered.length ? (
          <AdminTable
            columns={["Usuario", "Tipo", "Registro", "Estado", "Acciones"]}
            rows={filtered.map((user) => [
              <div>
                <div style={{ fontWeight: 700, color: "#0D2B45" }}>{user.name}</div>
                <div style={{ fontSize: 11, color: "#98A2B3", marginTop: 3 }}>{user.email}</div>
              </div>,
              <AdminBadge tone={user.type === "Lote" ? "info" : "neutral"}>{user.type}</AdminBadge>,
              user.date,
              <AdminBadge tone={user.status === "Activo" ? "success" : "warning"}>{user.status}</AdminBadge>,
              <AdminActionButton onClick={() => setSelected(user.name)}>Ver detalle</AdminActionButton>,
            ])}
          />
        ) : (
          <AdminEmptyState
            icon="🔎"
            title="No se encontraron usuarios"
            description="Prueba con otro nombre, correo o tipo de cuenta."
          />
        )}
      </AdminCard>

      {selected && (
        <AdminDetailModal
          title="Detalle del usuario"
          subtitle={selected}
          onClose={() => setSelected(null)}
        >
          <DetailRow label="Nombre" value={selected} />
          <DetailRow label="Tipo de cuenta" value={selected.includes("Auto") || selected.includes("Seminuevos") ? "Lote" : "Particular"} />
          <DetailRow label="Estado" value="Activo" />
          <DetailRow label="Publicaciones" value="12" />
          <DetailRow label="Fecha de registro" value="15 sep 2026" />
        </AdminDetailModal>
      )}
    </>
  )
}

function AdminLotesView() {
  const [search, setSearch] = useState("")
  const [selected, setSelected] = useState<string | null>(null)

  const lots = [
    { name: "Auto Premium", responsible: "Carlos Ramírez", city: "Manzanillo", vehicles: 38, status: "Activo" },
    { name: "Seminuevos del Pacífico", responsible: "Laura Torres", city: "Colima", vehicles: 26, status: "Activo" },
    { name: "Autos del Valle", responsible: "Miguel Sánchez", city: "Tecomán", vehicles: 19, status: "Activo" },
    { name: "Grupo Motor", responsible: "Andrea López", city: "Manzanillo", vehicles: 14, status: "Pendiente" },
    { name: "Karsy Motors", responsible: "Daniel Pérez", city: "Villa de Álvarez", vehicles: 9, status: "Activo" },
  ]

  const filtered = lots.filter((lot) =>
    `${lot.name} ${lot.responsible} ${lot.city}`
      .toLowerCase()
      .includes(search.toLowerCase()),
  )

  return (
    <>
      <AdminSectionHeader
        title="Lotes"
        description="Administra los lotes y agencias que publican vehículos en Karsy."
      />
      <AdminCard style={{ overflow: "hidden" }}>
        <div style={{ padding: 16, borderBottom: "1px solid #E2E8ED" }}>
          <AdminSearchBar
            value={search}
            onChange={setSearch}
            placeholder="Buscar lote, responsable o ciudad..."
          />
        </div>
        <AdminTable
          columns={["Lote", "Responsable", "Ubicación", "Vehículos", "Estado", "Acciones"]}
          rows={filtered.map((lot) => [
            <strong style={{ color: "#0D2B45" }}>{lot.name}</strong>,
            lot.responsible,
            lot.city,
            <strong>{lot.vehicles}</strong>,
            <AdminBadge tone={lot.status === "Activo" ? "success" : "warning"}>{lot.status}</AdminBadge>,
            <AdminActionButton onClick={() => setSelected(lot.name)}>Ver lote</AdminActionButton>,
          ])}
        />
      </AdminCard>

      {selected && (
        <AdminDetailModal
          title="Detalle del lote"
          subtitle={selected}
          onClose={() => setSelected(null)}
        >
          <DetailRow label="Nombre del lote" value={selected} />
          <DetailRow label="Responsable" value="Carlos Ramírez" />
          <DetailRow label="Ciudad" value="Manzanillo" />
          <DetailRow label="Vehículos publicados" value="38" />
          <DetailRow label="Estado" value="Activo" />
        </AdminDetailModal>
      )}
    </>
  )
}

function AdminVehiclesView() {
  const [search, setSearch] = useState("")
  const [status, setStatus] = useState("Todos")
  const [selected, setSelected] = useState<string | null>(null)

  const vehicles = [
    { name: "BMW Serie 3 320i", seller: "Auto Premium", year: 2023, price: "$685,000", status: "Activo" },
    { name: "Toyota Corolla LE", seller: "María González", year: 2022, price: "$325,000", status: "Activo" },
    { name: "Nissan Versa Advance", seller: "Seminuevos del Pacífico", year: 2022, price: "$245,000", status: "Activo" },
    { name: "Volkswagen Jetta Trendline", seller: "Carlos Ramírez", year: 2020, price: "$280,000", status: "Deshabilitado" },
    { name: "Mazda 3 Sedán i Sport", seller: "Autos del Valle", year: 2023, price: "$365,000", status: "Pendiente" },
    { name: "Honda Civic Sport", seller: "Laura Martínez", year: 2021, price: "$298,000", status: "Activo" },
  ]

  const filtered = vehicles.filter((vehicle) => {
    const matchesSearch =
      `${vehicle.name} ${vehicle.seller}`
        .toLowerCase()
        .includes(search.toLowerCase())
    const matchesStatus = status === "Todos" || vehicle.status === status
    return matchesSearch && matchesStatus
  })

  return (
    <>
      <AdminSectionHeader
        title="Vehículos"
        description="Revisa las publicaciones de vehículos y su estado dentro de la plataforma."
      />
      <AdminCard style={{ overflow: "hidden" }}>
        <div
          style={{
            padding: 16,
            display: "flex",
            gap: 10,
            flexWrap: "wrap",
            borderBottom: "1px solid #E2E8ED",
          }}
        >
          <AdminSearchBar
            value={search}
            onChange={setSearch}
            placeholder="Buscar marca, modelo o vendedor..."
          />
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            style={{
              border: "1px solid #E2E8ED",
              borderRadius: 10,
              padding: "10px 12px",
              background: "#FFFFFF",
              color: "#344054",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 13,
            }}
          >
            <option>Todos</option>
            <option>Activo</option>
            <option>Pendiente</option>
            <option>Deshabilitado</option>
          </select>
        </div>
        <AdminTable
          columns={["Vehículo", "Vendedor", "Año", "Precio", "Estado", "Acciones"]}
          rows={filtered.map((vehicle) => [
            <strong style={{ color: "#0D2B45" }}>{vehicle.name}</strong>,
            vehicle.seller,
            vehicle.year,
            <strong>{vehicle.price}</strong>,
            <AdminBadge
              tone={
                vehicle.status === "Activo"
                  ? "success"
                  : vehicle.status === "Pendiente"
                    ? "warning"
                    : "danger"
              }
            >
              {vehicle.status}
            </AdminBadge>,
            <AdminActionButton onClick={() => setSelected(vehicle.name)}>Revisar</AdminActionButton>,
          ])}
        />
      </AdminCard>

      {selected && (
        <AdminDetailModal
          title="Detalle del vehículo"
          subtitle={selected}
          onClose={() => setSelected(null)}
        >
          <DetailRow label="Vehículo" value={selected} />
          <DetailRow label="Transmisión" value="Automática" />
          <DetailRow label="Kilometraje" value="45,000 km" />
          <DetailRow label="Carrocería" value="Sedán" />
          <DetailRow label="Color" value="Blanco perla" />
          <DetailRow label="Estado" value="Activo" />
        </AdminDetailModal>
      )}
    </>
  )
}

function AdminReportsView() {
  const [status, setStatus] = useState("Todos")
  const [selected, setSelected] = useState<number | null>(null)

  const reports = [
    { id: 1, type: "Publicación", reporter: "María González", target: "BMW Serie 3 320i", date: "17 sep 2026", status: "Pendiente" },
    { id: 2, type: "Usuario", reporter: "Jorge Hernández", target: "Laura Martínez", date: "16 sep 2026", status: "En revisión" },
    { id: 3, type: "Publicación", reporter: "Carlos Ramírez", target: "Jetta Trendline", date: "15 sep 2026", status: "Resuelto" },
    { id: 4, type: "Usuario", reporter: "Andrea López", target: "Cuenta sospechosa", date: "14 sep 2026", status: "Pendiente" },
  ]

  const filtered = reports.filter(
    (report) => status === "Todos" || report.status === status,
  )

  return (
    <>
      <AdminSectionHeader
        title="Reportes"
        description="Revisa incidencias y reportes enviados por la comunidad de Karsy."
      />
      <AdminCard style={{ overflow: "hidden" }}>
        <div
          style={{
            padding: 16,
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: 10,
            flexWrap: "wrap",
            borderBottom: "1px solid #E2E8ED",
          }}
        >
          <div>
            <strong style={{ color: "#0D2B45", fontFamily: "'Outfit', sans-serif" }}>
              Incidencias
            </strong>
            <div style={{ color: "#98A2B3", fontSize: 12, marginTop: 3 }}>
              4 reportes registrados
            </div>
          </div>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            style={{
              border: "1px solid #E2E8ED",
              borderRadius: 10,
              padding: "10px 12px",
              background: "#FFFFFF",
              color: "#344054",
              fontFamily: "'DM Sans', sans-serif",
              fontSize: 13,
            }}
          >
            <option>Todos</option>
            <option>Pendiente</option>
            <option>En revisión</option>
            <option>Resuelto</option>
          </select>
        </div>

        <AdminTable
          columns={["Tipo", "Reportado por", "Elemento", "Fecha", "Estado", "Acciones"]}
          rows={filtered.map((report) => [
            <AdminBadge tone={report.type === "Usuario" ? "info" : "neutral"}>{report.type}</AdminBadge>,
            report.reporter,
            <strong style={{ color: "#0D2B45" }}>{report.target}</strong>,
            report.date,
            <AdminBadge
              tone={
                report.status === "Resuelto"
                  ? "success"
                  : report.status === "En revisión"
                    ? "info"
                    : "danger"
              }
            >
              {report.status}
            </AdminBadge>,
            <AdminActionButton onClick={() => setSelected(report.id)}>Revisar</AdminActionButton>,
          ])}
        />
      </AdminCard>

      {selected !== null && (
        <AdminDetailModal
          title="Revisión del reporte"
          subtitle={`Reporte #${String(selected).padStart(4, "0")}`}
          onClose={() => setSelected(null)}
        >
          <DetailRow label="Tipo" value={reports.find((x) => x.id === selected)?.type || ""} />
          <DetailRow label="Reportado por" value={reports.find((x) => x.id === selected)?.reporter || ""} />
          <DetailRow label="Elemento" value={reports.find((x) => x.id === selected)?.target || ""} />
          <DetailRow label="Estado" value={reports.find((x) => x.id === selected)?.status || ""} />
          <div style={{ display: "flex", gap: 8, marginTop: 18 }}>
            <AdminActionButton tone="success">Marcar resuelto</AdminActionButton>
            <AdminActionButton tone="danger">Deshabilitar</AdminActionButton>
          </div>
        </AdminDetailModal>
      )}
    </>
  )
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "150px 1fr",
        gap: 12,
        padding: "11px 0",
        borderBottom: "1px solid #F0F2F4",
      }}
    >
      <span style={{ color: "#98A2B3", fontSize: 12, fontWeight: 600 }}>{label}</span>
      <span style={{ color: "#344054", fontSize: 13, fontWeight: 600 }}>{value}</span>
    </div>
  )
}

function AdminDetailModal({
  title,
  subtitle,
  onClose,
  children,
}: {
  title: string
  subtitle: string
  onClose: () => void
  children: React.ReactNode
}) {
  return (
    <div
      style={{
        position: "fixed",
        inset: 0,
        zIndex: 1000,
        background: "rgba(13,43,69,0.42)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 20,
      }}
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          width: "min(560px, 100%)",
          maxHeight: "80vh",
          overflowY: "auto",
          background: "#FFFFFF",
          borderRadius: 18,
          boxShadow: "0 24px 70px rgba(13,43,69,0.28)",
          padding: 22,
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            gap: 12,
            alignItems: "flex-start",
            marginBottom: 16,
          }}
        >
          <div>
            <h2
              style={{
                margin: 0,
                color: "#0D2B45",
                fontFamily: "'Outfit', sans-serif",
                fontSize: 20,
              }}
            >
              {title}
            </h2>
            <p style={{ margin: "4px 0 0", color: "#98A2B3", fontSize: 12 }}>
              {subtitle}
            </p>
          </div>
          <button
            onClick={onClose}
            style={{
              width: 34,
              height: 34,
              borderRadius: 9,
              border: "1px solid #E2E8ED",
              background: "#F4F7F9",
              color: "#475467",
              cursor: "pointer",
              fontSize: 18,
            }}
            aria-label="Cerrar"
          >
            ×
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}



type FeaturedRequestStatus = "Pendiente" | "Aprobada" | "Rechazada"

type FeaturedRequest = {
  id: number
  vehicle: string
  year: number
  price: string
  image: string
  user: string
  time: string
  status: FeaturedRequestStatus
}

function AdminFeaturedRequestsView() {
  const initialRequests: FeaturedRequest[] = [
    {
      id: 1,
      vehicle: "Honda Civic Sport",
      year: 2021,
      price: "$298,000 MXN",
      image: "https://images.unsplash.com/photo-1590362891991-f776e747a588?w=900&h=560&fit=crop&auto=format",
      user: "Pamela Rodríguez",
      time: "Hace 2 horas",
      status: "Pendiente",
    },
    {
      id: 2,
      vehicle: "BMW Serie 3",
      year: 2022,
      price: "$625,000 MXN",
      image: "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=900&h=560&fit=crop&auto=format",
      user: "Jorge Hernández",
      time: "Hace 5 horas",
      status: "Pendiente",
    },
    {
      id: 3,
      vehicle: "Mazda 3 i Grand Touring",
      year: 2020,
      price: "$315,000 MXN",
      image: "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=900&h=560&fit=crop&auto=format",
      user: "Laura Martínez",
      time: "Ayer",
      status: "Aprobada",
    },
  ]

  const [requests, setRequests] = useState<FeaturedRequest[]>(initialRequests)
  const [filter, setFilter] = useState<FeaturedRequestStatus>("Pendiente")
  const [selected, setSelected] = useState<FeaturedRequest | null>(null)
  const [showApproved, setShowApproved] = useState(false)
  const [showReject, setShowReject] = useState(false)
  const [rejectReason, setRejectReason] = useState("")
  const [rejectComment, setRejectComment] = useState("")

  const rejectOptions = [
    ["Pago no verificado", "El comprobante de pago no coincide o no ha sido acreditado."],
    ["Calidad de imágenes", "Las fotos del vehículo no cumplen con la calidad o visibilidad requerida."],
    ["Información incompleta", "La publicación carece de detalles obligatorios o precisos."],
    ["Vehículo no permitido", "El vehículo no cumple con las políticas de publicación de Karsy."],
    ["Otro motivo", "Especifica el motivo en los comentarios adicionales."],
  ]

  const visible = requests.filter((r) => r.status === filter)

  const updateStatus = (status: FeaturedRequestStatus) => {
    if (!selected) return
    setRequests((prev) =>
      prev.map((r) => (r.id === selected.id ? { ...r, status } : r)),
    )
    setSelected((prev) => (prev ? { ...prev, status } : prev))
  }

  const approve = () => {
    updateStatus("Aprobada")
    setShowApproved(true)
  }

  const confirmReject = () => {
    if (!rejectReason) return
    updateStatus("Rechazada")
    setShowReject(false)
    setRejectReason("")
    setRejectComment("")
    setSelected(null)
    setFilter("Rechazada")
  }

  const tabButton = (value: FeaturedRequestStatus) => {
    const active = filter === value
    const count = requests.filter((r) => r.status === value).length
    return (
      <button
        onClick={() => setFilter(value)}
        style={{
          border: active ? "1.5px solid #0D2B45" : "1px solid #E2E8ED",
          background: active ? "#0D2B45" : "#FFFFFF",
          color: active ? "#FFFFFF" : "#333333",
          borderRadius: 999,
          padding: "9px 15px",
          fontFamily: "'DM Sans', sans-serif",
          fontSize: 12,
          fontWeight: 700,
          cursor: "pointer",
        }}
      >
        {value === "Pendiente" ? "Pendientes" : value === "Aprobada" ? "Aprobadas" : "Rechazadas"} ({count})
      </button>
    )
  }

  if (selected) {
    return (
      <div>
        <button
          onClick={() => setSelected(null)}
          style={{
            border: "none",
            background: "transparent",
            color: "#52A3AA",
            fontFamily: "'DM Sans', sans-serif",
            fontWeight: 700,
            cursor: "pointer",
            padding: "0 0 14px",
          }}
        >
          ‹ Volver a solicitudes
        </button>

        <AdminSectionHeader
          title="Revisión de publicación"
          description="Revisa la publicación y decide si cumple con los criterios para aparecer en Destacados."
        />

        <div style={{ maxWidth: 760, margin: "0 auto", paddingBottom: 92 }}>
          <div style={{ background: "#FFFFFF", borderRadius: 18, overflow: "hidden", border: "1px solid #E2E8ED", boxShadow: "0 4px 18px rgba(13,43,69,0.08)" }}>
            <div style={{ position: "relative" }}>
              <img src={selected.image} alt={selected.vehicle} style={{ width: "100%", height: 320, objectFit: "cover", display: "block" }} />
              {selected.status === "Aprobada" && (
                <div style={{ position: "absolute", top: 16, right: 16, background: "#FFF4CC", color: "#8A6415", borderRadius: 999, padding: "7px 12px", fontSize: 12, fontWeight: 800 }}>
                  ★ Destacado
                </div>
              )}
            </div>

            <div style={{ padding: 24 }}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: 16, alignItems: "flex-end", marginBottom: 22 }}>
                <div>
                  <div style={{ color: "#8E9A8E", fontSize: 13, marginBottom: 4 }}>{selected.year}</div>
                  <h2 style={{ fontFamily: "'Outfit', sans-serif", color: "#0D2B45", fontSize: 26, margin: 0 }}>{selected.vehicle}</h2>
                </div>
                <div style={{ fontFamily: "'Outfit', sans-serif", color: "#52A3AA", fontSize: 22, fontWeight: 800 }}>{selected.price}</div>
              </div>

              <SectionLabel>Ficha técnica</SectionLabel>
              <div style={{ background: "#F4F7F9", borderRadius: 14, padding: 16, display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, color: "#333333", fontSize: 13 }}>
                <div><b>Transmisión:</b> Automática</div>
                <div><b>Kilometraje:</b> 42,000 km</div>
                <div><b>Color:</b> Gris</div>
                <div><b>Combustible:</b> Gasolina</div>
              </div>

              <SectionLabel>Descripción</SectionLabel>
              <p style={{ color: "#667085", fontSize: 14, lineHeight: 1.65, margin: 0 }}>
                Vehículo en excelentes condiciones, documentación en regla y servicios al corriente.
              </p>

              <SectionLabel>Solicitante</SectionLabel>
              <div style={{ display: "flex", alignItems: "center", gap: 14, background: "#FFFFFF", border: "1px solid #E2E8ED", borderRadius: 14, padding: 15 }}>
                <img
                  src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=120&h=120&fit=crop&auto=format"
                  alt={selected.user}
                  style={{ width: 54, height: 54, borderRadius: "50%", objectFit: "cover" }}
                />
                <div style={{ flex: 1 }}>
                  <div style={{ fontFamily: "'Outfit', sans-serif", fontSize: 16, fontWeight: 700, color: "#0D2B45" }}>{selected.user}</div>
                  <button style={{ border: "none", background: "transparent", padding: "3px 0 0", color: "#52A3AA", fontSize: 12, fontWeight: 700, cursor: "pointer" }}>
                    Ver perfil
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        {selected.status === "Pendiente" && (
          <div style={{ position: "fixed", left: 0, right: 0, bottom: 0, zIndex: 150, background: "rgba(255,255,255,0.96)", borderTop: "1px solid #E2E8ED", padding: "14px 24px", backdropFilter: "blur(10px)" }}>
            <div style={{ maxWidth: 760, margin: "0 auto", display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
              <button onClick={() => setShowReject(true)} style={{ border: "none", borderRadius: 13, background: "#E65B5B", color: "#FFFFFF", padding: "14px 16px", fontWeight: 800, cursor: "pointer" }}>
                Rechazar
              </button>
              <button onClick={approve} style={{ border: "none", borderRadius: 13, background: "#0D2B45", color: "#FFFFFF", padding: "14px 16px", fontWeight: 800, cursor: "pointer" }}>
                Aprobar y Destacar
              </button>
            </div>
          </div>
        )}

        {showApproved && (
          <div style={{ position: "fixed", inset: 0, zIndex: 500, background: "rgba(13, 43, 69, 0.5)", display: "flex", alignItems: "center", justifyContent: "center", padding: 24 }}>
            <div style={{ width: "min(430px, 100%)", background: "#FFFFFF", borderRadius: 18, padding: 28, textAlign: "center", boxShadow: "0 24px 60px rgba(13,43,69,0.25)" }}>
              <div style={{ width: 68, height: 68, borderRadius: "50%", margin: "0 auto 16px", background: "#EDF7ED", color: "#2E7D32", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 28 }}>★✓</div>
              <h2 style={{ color: "#0D2B45", fontFamily: "'Outfit', sans-serif", margin: "0 0 10px" }}>¡Solicitud Aprobada!</h2>
              <p style={{ color: "#333333", lineHeight: 1.55, fontSize: 14 }}>
                La publicación de <b>{selected.vehicle} ({selected.year})</b> ahora aparece en la sección de Destacados en la app de Karsy.
              </p>
              <p style={{ color: "#8E9A8E", fontSize: 12, marginBottom: 20 }}>Se ha notificado al usuario solicitante.</p>
              <button
                onClick={() => {
                  setShowApproved(false)
                  setSelected(null)
                  setFilter("Aprobada")
                }}
                style={{ width: "100%", border: "none", borderRadius: 13, background: "#0D2B45", color: "#FFFFFF", padding: 14, fontWeight: 800, cursor: "pointer" }}
              >
                Volver a solicitudes
              </button>
            </div>
          </div>
        )}

        {showReject && (
          <div style={{ position: "fixed", inset: 0, zIndex: 510, background: "rgba(13, 43, 69, 0.6)", display: "flex", alignItems: "center", justifyContent: "center", padding: 24 }}>
            <div style={{ width: "min(520px, 100%)", maxHeight: "88vh", overflowY: "auto", background: "#FFFFFF", borderRadius: 20, padding: 26, boxShadow: "0 24px 60px rgba(13,43,69,0.28)" }}>
              <div style={{ width: 58, height: 58, borderRadius: "50%", margin: "0 auto 14px", background: "#FDECEC", color: "#E65B5B", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 25 }}>!</div>
              <h2 style={{ textAlign: "center", color: "#0D2B45", fontFamily: "'Outfit', sans-serif", fontSize: 21, margin: "0 0 8px" }}>Rechazar solicitud para destacar</h2>
              <p style={{ textAlign: "center", color: "#8E9A8E", fontSize: 13, lineHeight: 1.5, margin: "0 0 18px" }}>
                Selecciona o escribe el motivo por el cual no se aprobó la solicitud para informarle al usuario.
              </p>

              <div style={{ display: "grid", gap: 9 }}>
                {rejectOptions.map(([title, desc]) => {
                  const active = rejectReason === title
                  return (
                    <button
                      key={title}
                      onClick={() => setRejectReason(title)}
                      style={{
                        textAlign: "left",
                        border: active ? "1.5px solid #E65B5B" : "1px solid #E2E8ED",
                        background: active ? "#FFF7F7" : "#F4F7F9",
                        borderRadius: 12,
                        padding: "12px 13px",
                        cursor: "pointer",
                      }}
                    >
                      <div style={{ color: "#0D2B45", fontSize: 13, fontWeight: 800, marginBottom: 3 }}>
                        <span style={{ color: active ? "#E65B5B" : "#8E9A8E", marginRight: 7 }}>{active ? "●" : "○"}</span>
                        {title}
                      </div>
                      <div style={{ color: "#8E9A8E", fontSize: 11.5, lineHeight: 1.4, paddingLeft: 20 }}>{desc}</div>
                    </button>
                  )
                })}
              </div>

              <textarea
                value={rejectComment}
                onChange={(e) => setRejectComment(e.target.value)}
                placeholder="Añade detalles adicionales para el usuario (opcional)..."
                rows={3}
                style={{ width: "100%", boxSizing: "border-box", marginTop: 14, resize: "none", border: "1px solid #E2E8ED", borderRadius: 12, background: "#F4F7F9", padding: 13, color: "#333333", fontFamily: "'DM Sans', sans-serif", outline: "none" }}
              />

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginTop: 18 }}>
                <button onClick={() => setShowReject(false)} style={{ border: "none", borderRadius: 12, background: "#F4F7F9", color: "#8E9A8E", padding: 13, fontWeight: 700, cursor: "pointer" }}>Cancelar</button>
                <button
                  onClick={confirmReject}
                  disabled={!rejectReason}
                  style={{ border: "none", borderRadius: 12, background: "#E65B5B", color: "#FFFFFF", padding: 13, fontWeight: 800, cursor: rejectReason ? "pointer" : "not-allowed", opacity: rejectReason ? 1 : 0.45 }}
                >
                  Rechazar y Notificar
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    )
  }

  return (
    <div>
      <AdminSectionHeader
        title="Solicitudes para Destacar"
        description="Revisa las solicitudes enviadas por usuarios para mostrar sus vehículos en la sección de Destacados."
      />

      <div style={{ display: "flex", gap: 9, flexWrap: "wrap", marginBottom: 18 }}>
        {tabButton("Pendiente")}
        {tabButton("Aprobada")}
        {tabButton("Rechazada")}
      </div>

      {visible.length === 0 ? (
        <AdminCard style={{ padding: 32, textAlign: "center", color: "#8E9A8E" }}>
          No hay solicitudes en esta categoría.
        </AdminCard>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(270px, 1fr))", gap: 18 }}>
          {visible.map((request) => (
            <button
              key={request.id}
              onClick={() => setSelected(request)}
              style={{ padding: 0, border: "1px solid #E2E8ED", borderRadius: 16, overflow: "hidden", background: "#FFFFFF", textAlign: "left", cursor: "pointer", boxShadow: "0 3px 14px rgba(13,43,69,0.07)" }}
            >
              <div style={{ position: "relative" }}>
                <img src={request.image} alt={request.vehicle} style={{ width: "100%", height: 170, objectFit: "cover", display: "block" }} />
                <span style={{ position: "absolute", top: 12, right: 12, borderRadius: 999, padding: "6px 10px", background: request.status === "Aprobada" ? "#EDF7ED" : request.status === "Rechazada" ? "#FDECEC" : "#FFF4CC", color: request.status === "Aprobada" ? "#2E7D32" : request.status === "Rechazada" ? "#B42318" : "#8A6415", fontSize: 11, fontWeight: 800 }}>
                  {request.status === "Aprobada" ? "★ Destacado" : request.status === "Rechazada" ? "Rechazada" : "★ Solicitado"}
                </span>
              </div>
              <div style={{ padding: "14px 16px 0" }}>
                <div
                  style={{
                    fontFamily: "'Outfit', sans-serif",
                    fontSize: 15,
                    fontWeight: 700,
                    color: "#0D2B45",
                    marginBottom: 2,
                  }}
                >
                  {request.vehicle}
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 14 }}>
                  <span style={{ fontFamily: "'DM Sans', sans-serif", color: "#8E9A8E", fontSize: 12 }}>
                    {request.year}
                  </span>
                  <div style={{ fontFamily: "'Outfit', sans-serif", color: "#52A3AA", fontSize: 15, fontWeight: 700, textAlign: "right" }}>
                    {request.price.replace(" MXN", "")}
                    <div style={{ fontFamily: "'DM Sans', sans-serif", fontSize: 10, fontWeight: 400, color: "#8E9A8E" }}>
                      MXN
                    </div>
                  </div>
                </div>
              </div>
              <div style={{ background: "#F4F7F9", padding: "13px 16px", fontFamily: "'DM Sans', sans-serif" }}>
                <div style={{ color: "#333333", fontSize: 12, lineHeight: 1.5 }}>
                  ♙ El usuario <b style={{ color: "#0D2B45", fontWeight: 700 }}>{request.user}</b> ha solicitado destacar esta publicación.
                </div>
                <div style={{ color: "#8E9A8E", fontSize: 11, marginTop: 5 }}>{request.time}</div>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function AdminIcon({ name, size = 20 }: { name: string size?: number }) {
  const common = {
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.8,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
  }
  switch (name) {
    case "Inicio":
      return (
        <svg {...common}>
          <path d="M3 11.5 12 4l9 7.5" />
          <path d="M5 10v10h14V10" />
        </svg>
      )
    case "Usuarios":
      return (
        <svg {...common}>
          <path d="M17 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
          <path d="M16 3.13a4 4 0 0 1 0 7.75" />
        </svg>
      )
    case "Lotes":
      return (
        <svg {...common}>
          <path d="M3 21h18" />
          <path d="M5 21V7l7-4 7 4v14" />
          <path d="M9 21v-6h6v6" />
          <path d="M9 11h.01M15 11h.01" />
        </svg>
      )
    case "Vehículos":
      return (
        <svg {...common}>
          <path d="M5 17h14" />
          <circle cx="7" cy="17" r="2" />
          <circle cx="17" cy="17" r="2" />
          <path d="M5 17l1.5-5h11L19 17" />
        </svg>
      )
    case "Reportes":
      return (
        <svg {...common}>
          <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" />
          <path d="M4 22v-7" />
        </svg>
      )
    case "Destacados":
      return (
        <svg {...common}>
          <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3 6.4 20.2 7.5 14 3 9.6l6.2-.9L12 3z" />
        </svg>
      )
    default:
      return null
  }
}

function WebAdminDashboardView({ onBack }: { onBack: () => void }) {
  const [range, setRange] = useState<"7" | "30" | "90">("7")
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [activeSection, setActiveSection] = useState("Inicio")

  const kpis = [
    {
      icon: "👤",
      label: "Usuarios registrados",
      value: "1,248",
      change: "+12%",
      positive: true,
      color: "#3B82F6",
      bg: "#EFF6FF",
    },
    {
      icon: "🏪",
      label: "Lotes registrados",
      value: "86",
      change: "+8%",
      positive: true,
      color: "#A855F7",
      bg: "#F5F3FF",
    },
    {
      icon: "🚗",
      label: "Vehículos publicados",
      value: "3,562",
      change: "+15%",
      positive: true,
      color: "#14B8A6",
      bg: "#F0FDFA",
    },
    {
      icon: "✅",
      label: "Publicaciones activas",
      value: "3,214",
      change: "+14%",
      positive: true,
      color: "#22C55E",
      bg: "#F0FDF4",
    },
    {
      icon: "⏸️",
      label: "Publicaciones deshabilitadas",
      value: "348",
      change: "-6%",
      positive: false,
      color: "#F59E0B",
      bg: "#FFFBEB",
    },
  ]

  const alerts = [
    {
      icon: "📄",
      text: "5 publicaciones deshabilitadas recientemente",
      bg: "#FEF2F2",
      color: "#EF4444",
    },
    {
      icon: "👤",
      text: "3 cuentas desactivadas",
      bg: "#FFFBEB",
      color: "#F59E0B",
    },
    {
      icon: "🏪",
      text: "2 nuevos lotes registrados",
      bg: "#EFF6FF",
      color: "#3B82F6",
    },
    {
      icon: "🛡️",
      text: "7 publicaciones pendientes de revisión",
      bg: "#F5F3FF",
      color: "#8B5CF6",
    },
    {
      icon: "🚩",
      text: "4 reportes de usuarios",
      bg: "#FEF2F2",
      color: "#EF4444",
    },
  ]

  const chartData = {
    labels: ["3 sep", "4 sep", "5 sep", "6 sep", "7 sep", "8 sep", "9 sep"],
    usuarios: [180, 195, 210, 225, 240, 255, 268],
    lotes: [60, 68, 72, 78, 82, 85, 88],
    publicaciones: [320, 350, 420, 480, 540, 610, 680],
  }

  const salesComparison = {
    labels: ["Abr", "May", "Jun", "Jul", "Ago", "Sep"],
    viaPlataforma: [82, 96, 118, 134, 156, 182],
    fuera: [58, 64, 72, 78, 84, 92],
  }

  const totalViaPlataforma = salesComparison.viaPlataforma.reduce(
    (a, b) => a + b,
    0,
  )
  const totalFuera = salesComparison.fuera.reduce((a, b) => a + b, 0)
  const totalVentas = totalViaPlataforma + totalFuera
  const porcentajePlataforma = Math.round(
    (totalViaPlataforma / totalVentas) * 100,
  )

  const sidebarItems = [
    { label: "Inicio" },
    { label: "Usuarios" },
    { label: "Lotes" },
    { label: "Vehículos" },
    { label: "Reportes", badge: 4 },
    { label: "Destacados", badge: 2 },
  ]

  const maxChartValue = 800
  const maxSalesValue =
    Math.max(...salesComparison.viaPlataforma, ...salesComparison.fuera) * 1.15

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "#F4F7F9",
        fontFamily: "'DM Sans', sans-serif",
        position: "relative",
      }}
    >
      {sidebarOpen && (
        <div
          onClick={() => setSidebarOpen(false)}
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(13,43,69,0.35)",
            backdropFilter: "blur(2px)",
            zIndex: 200,
          }}
        />
      )}

      <aside
        style={{
          position: "fixed",
          top: 0,
          left: 0,
          height: "100vh",
          width: 64,
          background: "#0D2B45",
          zIndex: 300,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          paddingTop: 20,
          paddingBottom: 20,
          transform: sidebarOpen ? "translateX(0)" : "translateX(-100%)",
          transition: "transform 0.25s ease",
          boxShadow: sidebarOpen ? "4px 0 24px rgba(13,43,69,0.35)" : "none",
        }}
      >
        <div style={{ marginBottom: 24, flexShrink: 0 }}>
          <KarsyLogo size={40} />
        </div>

        <nav
          style={{
            flex: 1,
            display: "flex",
            flexDirection: "column",
            gap: 6,
            width: "100%",
            alignItems: "center",
            overflowY: "auto",
            overflowX: "visible",
          }}
        >
          {sidebarItems.map((item) => {
            const active = activeSection === item.label
            return (
              <div
                key={item.label}
                style={{
                  position: "relative",
                  width: "100%",
                  display: "flex",
                  justifyContent: "center",
                }}
              >
                <button
                  onClick={() => {
                    setActiveSection(item.label)
                    setSidebarOpen(false)
                  }}
                  className="sidebar-icon-btn"
                  style={{
                    width: 44,
                    height: 44,
                    borderRadius: 12,
                    background: active ? "#2563EB" : "transparent",
                    border: "none",
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: active ? "#FFFFFF" : "rgba(255,255,255,0.75)",
                    transition: "background 0.15s",
                    position: "relative",
                  }}
                  onMouseEnter={(e) => {
                    if (!active)
                      (e.currentTarget as HTMLButtonElement).style.background =
                        "rgba(255,255,255,0.12)"
                    const tip =
                      (e.currentTarget as HTMLButtonElement).querySelector(
                        ".sidebar-tooltip",
                      ) as HTMLElement
                    if (tip) tip.style.opacity = "1"
                    if (tip) tip.style.visibility = "visible"
                  }}
                  onMouseLeave={(e) => {
                    if (!active)
                      (e.currentTarget as HTMLButtonElement).style.background =
                        "transparent"
                    const tip =
                      (e.currentTarget as HTMLButtonElement).querySelector(
                        ".sidebar-tooltip",
                      ) as HTMLElement
                    if (tip) tip.style.opacity = "0"
                    if (tip) tip.style.visibility = "hidden"
                  }}
                  title={item.label}
                >
                  <AdminIcon name={item.label} size={20} />

                  <span
                    className="sidebar-tooltip"
                    style={{
                      position: "absolute",
                      left: "calc(100% + 10px)",
                      top: "50%",
                      transform: "translateY(-50%)",
                      background: "#0D2B45",
                      color: "#FFFFFF",
                      padding: "6px 12px",
                      borderRadius: 8,
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 12,
                      fontWeight: 600,
                      whiteSpace: "nowrap",
                      pointerEvents: "none",
                      opacity: 0,
                      visibility: "hidden",
                      transition: "opacity 0.15s, visibility 0.15s",
                      boxShadow: "0 4px 12px rgba(0,0,0,0.2)",
                      zIndex: 400,
                    }}
                  >
                    {item.label}
                    {item.label === "Destacados" && item.badge ? (
                      <span
                        style={{
                          position: "absolute",
                          top: 7,
                          right: 7,
                          width: 8,
                          height: 8,
                          borderRadius: "50%",
                          background: "#E65B5B",
                          border: "2px solid #0D2B45",
                          boxSizing: "content-box",
                        }}
                        aria-label={`${item.badge} solicitudes pendientes`}
                      />
                    ) : item.badge && (
                      <span
                        style={{
                          marginLeft: 8,
                          background: "#EF4444",
                          color: "#FFFFFF",
                          fontSize: 10,
                          fontWeight: 700,
                          padding: "1px 6px",
                          borderRadius: 8,
                        }}
                      >
                        {item.badge}
                      </span>
                    )}
                  </span>

                  {item.badge && (
                    <span
                      style={{
                        position: "absolute",
                        top: 6,
                        right: 6,
                        width: 8,
                        height: 8,
                        borderRadius: "50%",
                        background: "#EF4444",
                        border: "2px solid #0D2B45",
                      }}
                    />
                  )}
                </button>
              </div>
            )
          })}
        </nav>

        <div
          style={{
            width: 40,
            height: 40,
            borderRadius: "50%",
            background: "#3B82F6",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "#FFFFFF",
            fontWeight: 700,
            fontSize: 14,
            marginTop: 12,
            flexShrink: 0,
            cursor: "pointer",
          }}
          title="Administrador"
        >
          A
        </div>
      </aside>

      <main style={{ padding: "0 0 60px" }}>
        <header
          style={{
            background: "#FFFFFF",
            borderBottom: "1px solid #E2E8ED",
            padding: "14px 24px",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: 16,
            position: "sticky",
            top: 0,
            zIndex: 100,
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              title={sidebarOpen ? "Cerrar menú" : "Abrir menú"}
              style={{
                background: sidebarOpen ? "#EBF6F7" : "#F4F7F9",
                border: `1.5px solid ${sidebarOpen ? "#52A3AA" : "#E2E8ED"}`,
                borderRadius: 10,
                width: 40,
                height: 40,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                cursor: "pointer",
                color: "#0D2B45",
                flexShrink: 0,
                transition: "background 0.2s, border-color 0.2s",
              }}
            >
              {sidebarOpen ? (
                <svg
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2.2"
                  strokeLinecap="round"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              ) : (
                <svg
                  width="18"
                  height="18"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <line x1="4" y1="7" x2="20" y2="7" />
                  <line x1="4" y1="12" x2="20" y2="12" />
                  <line x1="4" y1="17" x2="14" y2="17" />
                </svg>
              )}
            </button>

            <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
              <KarsyLogo size={32} />
              <span
                style={{
                  fontFamily: "'Outfit', sans-serif",
                  fontWeight: 700,
                  fontSize: 18,
                  color: "#0D2B45",
                  letterSpacing: "0.03em",
                }}
              >
                Karsy Admin
              </span>
            </div>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <button
              onClick={onBack}
              title="Volver al inicio"
              style={{
                background: "#F4F7F9",
                border: "1.5px solid #E2E8ED",
                borderRadius: 10,
                width: 40,
                height: 40,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                cursor: "pointer",
                color: "#0D2B45",
                flexShrink: 0,
              }}
            >
              <svg
                width="19"
                height="19"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M3 11.5 12 4l9 7.5" />
                <path d="M5.5 10.5V20h13v-9.5" />
                <path d="M9.5 20v-6h5v6" />
              </svg>
            </button>
          </div>
        </header>

        <div
          style={{
            padding: "24px 24px 60px",
            maxWidth: 1400,
            margin: "0 auto",
          }}
        >
          {activeSection !== "Inicio" ? (
            <>
              {activeSection === "Usuarios" && <AdminUsersView />}
              {activeSection === "Lotes" && <AdminLotesView />}
              {activeSection === "Vehículos" && <AdminVehiclesView />}
              {activeSection === "Reportes" && <AdminReportsView />}
              {activeSection === "Destacados" && <AdminFeaturedRequestsView />}
            </>
          ) : (
            <>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "flex-start",
                  marginBottom: 20,
                  flexWrap: "wrap",
                  gap: 16,
                }}
              >
                <div>
                  <h1
                    style={{
                      fontFamily: "'Outfit', sans-serif",
                      fontSize: 26,
                      fontWeight: 700,
                      color: "#0D2B45",
                      margin: "0 0 4px",
                      letterSpacing: "-0.01em",
                    }}
                  >
                    ¡Hola, Administrador!
                  </h1>
                  <p
                    style={{
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 14,
                      color: "#8E9A8E",
                      margin: 0,
                    }}
                  >
                    Resumen de la actividad en la plataforma.
                  </p>
                </div>
                <div
                  style={{
                    display: "flex",
                    background: "#FFFFFF",
                    borderRadius: 10,
                    border: "1px solid #E2E8ED",
                    overflow: "hidden",
                  }}
                >
                  <div
                    style={{
                      padding: "9px 14px",
                      display: "flex",
                      alignItems: "center",
                      gap: 8,
                      borderRight: "1px solid #E2E8ED",
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 13,
                      color: "#333333",
                      fontWeight: 500,
                    }}
                  >
                    <svg
                      width="14"
                      height="14"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="#667085"
                      strokeWidth="1.8"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <rect x="3" y="4" width="18" height="18" rx="2" />
                      <line x1="16" y1="2" x2="16" y2="6" />
                      <line x1="8" y1="2" x2="8" y2="6" />
                      <line x1="3" y1="10" x2="21" y2="10" />
                    </svg>
                    {range === "7"
                      ? "Últimos 7 días"
                      : range === "30"
                        ? "Últimos 30 días"
                        : "Últimos 90 días"}
                  </div>
                  <select
                    value={range}
                    onChange={(e) =>
                      setRange(e.target.value as "7" | "30" | "90")
                    }
                    style={{
                      border: "none",
                      padding: "9px 30px 9px 10px",
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 13,
                      color: "#333333",
                      background: "#FFFFFF",
                      cursor: "pointer",
                    }}
                  >
                    <option value="7">Últimos 7 días</option>
                    <option value="30">Últimos 30 días</option>
                    <option value="90">Últimos 90 días</option>
                  </select>
                </div>
              </div>

              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
                  gap: 12,
                  marginBottom: 20,
                }}
              >
                {kpis.map((kpi) => (
                  <div
                    key={kpi.label}
                    style={{
                      background: "#FFFFFF",
                      borderRadius: 14,
                      border: "1px solid #E2E8ED",
                      padding: "14px 14px 12px",
                      boxShadow: "0 1px 6px rgba(13,43,69,0.04)",
                      display: "flex",
                      flexDirection: "column",
                    }}
                  >
                    <div
                      style={{
                        width: 32,
                        height: 32,
                        borderRadius: 9,
                        background: kpi.bg,
                        color: kpi.color,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontSize: 14,
                        marginBottom: 10,
                      }}
                    >
                      {kpi.icon}
                    </div>
                    <div
                      style={{
                        fontFamily: "'DM Sans', sans-serif",
                        fontSize: 11,
                        color: "#8E9A8E",
                        marginBottom: 6,
                        lineHeight: 1.35,
                      }}
                    >
                      {kpi.label}
                    </div>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "baseline",
                        justifyContent: "space-between",
                        gap: 6,
                        marginTop: "auto",
                      }}
                    >
                      <span
                        style={{
                          fontFamily: "'Outfit', sans-serif",
                          fontSize: 20,
                          fontWeight: 700,
                          color: "#0D2B45",
                          lineHeight: 1,
                        }}
                      >
                        {kpi.value}
                      </span>
                      <span
                        style={{
                          fontFamily: "'DM Sans', sans-serif",
                          fontSize: 11,
                          fontWeight: 600,
                          color: kpi.positive ? "#22C55E" : "#EF4444",
                          whiteSpace: "nowrap",
                        }}
                      >
                        {kpi.positive ? "↑" : "↓"} {kpi.change}
                      </span>
                    </div>
                  </div>
                ))}
              </div>

              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: 16,
                  marginBottom: 20,
                }}
              >
                <div
                  style={{
                    background: "#FFFFFF",
                    borderRadius: 16,
                    border: "1px solid #E2E8ED",
                    padding: "20px 22px",
                    boxShadow: "0 1px 6px rgba(13,43,69,0.04)",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      marginBottom: 16,
                      flexWrap: "wrap",
                      gap: 10,
                    }}
                  >
                    <div>
                      <div
                        style={{
                          fontFamily: "'Outfit', sans-serif",
                          fontSize: 16,
                          fontWeight: 700,
                          color: "#0D2B45",
                          marginBottom: 2,
                        }}
                      >
                        Crecimiento de la plataforma
                      </div>
                      <div
                        style={{
                          fontFamily: "'DM Sans', sans-serif",
                          fontSize: 12,
                          color: "#8E9A8E",
                        }}
                      >
                        Usuarios, lotes y publicaciones por día
                      </div>
                    </div>
                    <div style={{ display: "flex", gap: 4 }}>
                      {(["7", "30", "90"] as const).map((r) => (
                        <button
                          key={r}
                          onClick={() => setRange(r)}
                          style={{
                            padding: "4px 12px",
                            borderRadius: 8,
                            border: "1px solid #E2E8ED",
                            background: range === r ? "#2563EB" : "#FFFFFF",
                            color: range === r ? "#FFFFFF" : "#667085",
                            fontFamily: "'DM Sans', sans-serif",
                            fontSize: 11,
                            fontWeight: 600,
                            cursor: "pointer",
                          }}
                        >
                          {r}d
                        </button>
                      ))}
                    </div>
                  </div>

                  <div
                    style={{
                      display: "flex",
                      gap: 18,
                      marginBottom: 12,
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 12,
                      color: "#667085",
                    }}
                  >
                    <span
                      style={{ display: "flex", alignItems: "center", gap: 6 }}
                    >
                      <span
                        style={{
                          width: 8,
                          height: 8,
                          borderRadius: "50%",
                          background: "#3B82F6",
                        }}
                      />
                      Usuarios
                    </span>
                    <span
                      style={{ display: "flex", alignItems: "center", gap: 6 }}
                    >
                      <span
                        style={{
                          width: 8,
                          height: 8,
                          borderRadius: "50%",
                          background: "#8B5CF6",
                        }}
                      />
                      Lotes
                    </span>
                    <span
                      style={{ display: "flex", alignItems: "center", gap: 6 }}
                    >
                      <span
                        style={{
                          width: 8,
                          height: 8,
                          borderRadius: "50%",
                          background: "#22C55E",
                        }}
                      />
                      Publicaciones
                    </span>
                  </div>

                  <div style={{ position: "relative", height: 260 }}>
                    <svg
                      viewBox="0 0 600 260"
                      style={{
                        width: "100%",
                        height: "100%",
                        overflow: "visible",
                      }}
                    >
                      <defs>
                        <linearGradient
                          id="userGrad"
                          x1="0"
                          y1="0"
                          x2="0"
                          y2="1"
                        >
                          <stop
                            offset="0%"
                            stopColor="#3B82F6"
                            stopOpacity="0.22"
                          />
                          <stop
                            offset="100%"
                            stopColor="#3B82F6"
                            stopOpacity="0"
                          />
                        </linearGradient>
                        <linearGradient
                          id="pubGrad"
                          x1="0"
                          y1="0"
                          x2="0"
                          y2="1"
                        >
                          <stop
                            offset="0%"
                            stopColor="#22C55E"
                            stopOpacity="0.22"
                          />
                          <stop
                            offset="100%"
                            stopColor="#22C55E"
                            stopOpacity="0"
                          />
                        </linearGradient>
                      </defs>

                      {[0, 200, 400, 600, 800].map((v, i) => {
                        const y = 240 - (v / maxChartValue) * 220
                        return (
                          <g key={i}>
                            <line
                              x1="40"
                              y1={y}
                              x2="600"
                              y2={y}
                              stroke="#F4F7F9"
                              strokeWidth="1"
                            />
                            <text
                              x="30"
                              y={y + 4}
                              textAnchor="end"
                              fontSize="10"
                              fill="#8E9A8E"
                              fontFamily="DM Sans, sans-serif"
                            >
                              {v}
                            </text>
                          </g>
                        )
                      })}

                      {chartData.labels.map((lbl, i) => {
                        const x = 40 + (i / (chartData.labels.length - 1)) * 560
                        return (
                          <text
                            key={i}
                            x={x}
                            y="258"
                            textAnchor="middle"
                            fontSize="10"
                            fill="#8E9A8E"
                            fontFamily="DM Sans, sans-serif"
                          >
                            {lbl}
                          </text>
                        )
                      })}

                      <path
                        d={
                          chartData.usuarios
                            .map((v, i) => {
                              const x =
                                40 + (i / (chartData.usuarios.length - 1)) * 560
                              const y = 240 - (v / maxChartValue) * 220
                              return `${i === 0 ? "M" : "L"}${x},${y}`
                            })
                            .join(" ") + ` L600,240 L40,240 Z`
                        }
                        fill="url(#userGrad)"
                      />

                      <path
                        d={
                          chartData.publicaciones
                            .map((v, i) => {
                              const x =
                                40 +
                                (i / (chartData.publicaciones.length - 1)) * 560
                              const y = 240 - (v / maxChartValue) * 220
                              return `${i === 0 ? "M" : "L"}${x},${y}`
                            })
                            .join(" ") + ` L600,240 L40,240 Z`
                        }
                        fill="url(#pubGrad)"
                      />

                      {[
                        { data: chartData.usuarios, color: "#3B82F6" },
                        { data: chartData.lotes, color: "#8B5CF6" },
                        { data: chartData.publicaciones, color: "#22C55E" },
                      ].map(({ data, color }, li) => {
                        const points = data
                          .map((v, i) => {
                            const x = 40 + (i / (data.length - 1)) * 560
                            const y = 240 - (v / maxChartValue) * 220
                            return `${x},${y}`
                          })
                          .join(" ")
                        return (
                          <g key={li}>
                            <polyline
                              points={points}
                              fill="none"
                              stroke={color}
                              strokeWidth="2.5"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                            />
                            {data.map((v, i) => {
                              const x = 40 + (i / (data.length - 1)) * 560
                              const y = 240 - (v / maxChartValue) * 220
                              return (
                                <circle
                                  key={i}
                                  cx={x}
                                  cy={y}
                                  r="3.5"
                                  fill="#FFFFFF"
                                  stroke={color}
                                  strokeWidth="2"
                                />
                              )
                            })}
                          </g>
                        )
                      })}
                    </svg>
                  </div>
                </div>

                <div
                  style={{
                    background: "#FFFFFF",
                    borderRadius: 16,
                    border: "1px solid #E2E8ED",
                    padding: "20px 22px",
                    boxShadow: "0 1px 6px rgba(13,43,69,0.04)",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      marginBottom: 16,
                      flexWrap: "wrap",
                      gap: 10,
                    }}
                  >
                    <div>
                      <div
                        style={{
                          fontFamily: "'Outfit', sans-serif",
                          fontSize: 16,
                          fontWeight: 700,
                          color: "#0D2B45",
                          marginBottom: 2,
                        }}
                      >
                        Comparación de ventas
                      </div>
                      <div
                        style={{
                          fontFamily: "'DM Sans', sans-serif",
                          fontSize: 12,
                          color: "#8E9A8E",
                        }}
                      >
                        Ventas cerradas vía Karsy vs. fuera de la plataforma
                      </div>
                    </div>
                    <span
                      style={{
                        padding: "4px 12px",
                        borderRadius: 8,
                        background: "#EBF6F7",
                        color: "#0D2B45",
                        fontFamily: "'DM Sans', sans-serif",
                        fontSize: 11,
                        fontWeight: 700,
                      }}
                    >
                      {porcentajePlataforma}% vía Karsy
                    </span>
                  </div>

                  <div
                    style={{
                      display: "flex",
                      gap: 18,
                      marginBottom: 14,
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 12,
                      color: "#667085",
                      flexWrap: "wrap",
                    }}
                  >
                    <span
                      style={{ display: "flex", alignItems: "center", gap: 6 }}
                    >
                      <span
                        style={{
                          width: 10,
                          height: 10,
                          borderRadius: 3,
                          background: "#52A3AA",
                        }}
                      />
                      Vía plataforma Karsy
                    </span>
                    <span
                      style={{ display: "flex", alignItems: "center", gap: 6 }}
                    >
                      <span
                        style={{
                          width: 10,
                          height: 10,
                          borderRadius: 3,
                          background: "#D4DDE4",
                        }}
                      />
                      Fuera de la plataforma
                    </span>
                  </div>

                  <div style={{ position: "relative", height: 260 }}>
                    <svg
                      viewBox="0 0 600 260"
                      style={{
                        width: "100%",
                        height: "100%",
                        overflow: "visible",
                      }}
                    >
                      {[0, 50, 100, 150, 200].map((v, i) => {
                        const y = 240 - (v / maxSalesValue) * 220
                        if (y < 0) return null
                        return (
                          <g key={i}>
                            <line
                              x1="40"
                              y1={y}
                              x2="600"
                              y2={y}
                              stroke="#F4F7F9"
                              strokeWidth="1"
                            />
                            <text
                              x="30"
                              y={y + 4}
                              textAnchor="end"
                              fontSize="10"
                              fill="#8E9A8E"
                              fontFamily="DM Sans, sans-serif"
                            >
                              {v}
                            </text>
                          </g>
                        )
                      })}

                      {salesComparison.labels.map((lbl, i) => {
                        const groupWidth = 560 / salesComparison.labels.length
                        const groupCenter = 40 + i * groupWidth + groupWidth / 2
                        const barWidth = 26
                        const gap = 6

                        const vPlat = salesComparison.viaPlataforma[i]
                        const vFuera = salesComparison.fuera[i]

                        const hPlat = (vPlat / maxSalesValue) * 220
                        const hFuera = (vFuera / maxSalesValue) * 220
                        const yPlat = 240 - hPlat
                        const yFuera = 240 - hFuera

                        const isLast = i === salesComparison.labels.length - 1

                        return (
                          <g key={i}>
                            <rect
                              x={groupCenter - barWidth - gap / 2}
                              y={yPlat}
                              width={barWidth}
                              height={hPlat}
                              rx="5"
                              fill="#52A3AA"
                              opacity="0.95"
                            />
                            <text
                              x={groupCenter - barWidth / 2 - gap / 2}
                              y={yPlat - 6}
                              textAnchor="middle"
                              fontSize="10"
                              fill="#0D2B45"
                              fontWeight="700"
                              fontFamily="DM Sans, sans-serif"
                            >
                              {vPlat}
                            </text>

                            <rect
                              x={groupCenter + gap / 2}
                              y={yFuera}
                              width={barWidth}
                              height={hFuera}
                              rx="5"
                              fill={isLast ? "#0D2B45" : "#D4DDE4"}
                              opacity={isLast ? 1 : 0.9}
                            />
                            <text
                              x={groupCenter + barWidth / 2 + gap / 2}
                              y={yFuera - 6}
                              textAnchor="middle"
                              fontSize="10"
                              fill="#0D2B45"
                              fontWeight="700"
                              fontFamily="DM Sans, sans-serif"
                            >
                              {vFuera}
                            </text>

                            <text
                              x={groupCenter}
                              y="258"
                              textAnchor="middle"
                              fontSize="10"
                              fill="#8E9A8E"
                              fontFamily="DM Sans, sans-serif"
                            >
                              {lbl}
                            </text>
                          </g>
                        )
                      })}
                    </svg>
                  </div>

                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns:
                        "repeat(auto-fit, minmax(140px, 1fr))",
                      gap: 10,
                      marginTop: 18,
                      paddingTop: 16,
                      borderTop: "1px solid #F4F7F9",
                    }}
                  >
                    <div
                      style={{
                        background: "#EBF6F7",
                        borderRadius: 10,
                        padding: "10px 12px",
                      }}
                    >
                      <div
                        style={{
                          fontFamily: "'DM Sans', sans-serif",
                          fontSize: 11,
                          color: "#667085",
                          marginBottom: 4,
                        }}
                      >
                        Total vía Karsy
                      </div>
                      <div
                        style={{
                          fontFamily: "'Outfit', sans-serif",
                          fontSize: 18,
                          fontWeight: 700,
                          color: "#0D2B45",
                        }}
                      >
                        {totalViaPlataforma}
                      </div>
                    </div>
                    <div
                      style={{
                        background: "#F4F7F9",
                        borderRadius: 10,
                        padding: "10px 12px",
                      }}
                    >
                      <div
                        style={{
                          fontFamily: "'DM Sans', sans-serif",
                          fontSize: 11,
                          color: "#667085",
                          marginBottom: 4,
                        }}
                      >
                        Total fuera
                      </div>
                      <div
                        style={{
                          fontFamily: "'Outfit', sans-serif",
                          fontSize: 18,
                          fontWeight: 700,
                          color: "#0D2B45",
                        }}
                      >
                        {totalFuera}
                      </div>
                    </div>
                    <div
                      style={{
                        background: "#F0FDF4",
                        borderRadius: 10,
                        padding: "10px 12px",
                      }}
                    >
                      <div
                        style={{
                          fontFamily: "'DM Sans', sans-serif",
                          fontSize: 11,
                          color: "#667085",
                          marginBottom: 4,
                        }}
                      >
                        Efectividad
                      </div>
                      <div
                        style={{
                          fontFamily: "'Outfit', sans-serif",
                          fontSize: 18,
                          fontWeight: 700,
                          color: "#22C55E",
                        }}
                      >
                        {porcentajePlataforma}%
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div
                style={{
                  background: "#FFFFFF",
                  borderRadius: 14,
                  border: "1px solid #E2E8ED",
                  padding: "18px 20px",
                  boxShadow: "0 1px 6px rgba(13,43,69,0.04)",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    marginBottom: 14,
                  }}
                >
                  <span
                    style={{
                      fontFamily: "'Outfit', sans-serif",
                      fontSize: 15,
                      fontWeight: 700,
                      color: "#0D2B45",
                    }}
                  >
                    ⚠️ Requiere atención
                  </span>
                  <span
                    onClick={() => setActiveSection("Reportes")}
                    style={{
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 12,
                      color: "#2563EB",
                      cursor: "pointer",
                      fontWeight: 600,
                    }}
                  >
                    Ver todos →
                  </span>
                </div>

                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
                    gap: 10,
                  }}
                >
                  {alerts.map((a) => (
                    <div
                      key={a.text}
                      style={{
                        background: a.bg,
                        borderRadius: 10,
                        padding: "12px 14px",
                        display: "flex",
                        alignItems: "center",
                        gap: 10,
                        cursor: "pointer",
                        transition: "transform 0.15s",
                      }}
                      onMouseEnter={(e) => {
                        ;(e.currentTarget as HTMLDivElement).style.transform =
                          "translateX(3px)"
                      }}
                      onMouseLeave={(e) => {
                        ;(e.currentTarget as HTMLDivElement).style.transform =
                          "translateX(0)"
                      }}
                    >
                      <div
                        style={{
                          width: 30,
                          height: 30,
                          borderRadius: 8,
                          background: "#FFFFFF",
                          color: a.color,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          fontSize: 14,
                          flexShrink: 0,
                        }}
                      >
                        {a.icon}
                      </div>
                      <span
                        style={{
                          fontFamily: "'DM Sans', sans-serif",
                          fontSize: 12,
                          color: "#333333",
                          fontWeight: 500,
                          lineHeight: 1.4,
                        }}
                      >
                        {a.text}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>
      </main>
    </div>
  )
}

function PublishFlowView({
  onBack,
  onPanel,
}: {
  onBack: () => void
  onPanel: () => void
}) {
  const [step, setStep] = useState<1 | 2 | 3 | 4>(1)
  const [published, setPublished] = useState(false)

  const goBack = () => {
    if (step === 1) {
      onBack()
    } else if (step === 2) {
      setStep(1)
    } else if (step === 3) {
      setStep(2)
    } else {
      setStep(3)
    }
  }

  const goNext = () => {
    if (step === 1) setStep(2)
    else if (step === 2) setStep(3)
    else if (step === 3) setStep(4)
  }

  return (
    <div
      style={{
        minHeight: "calc(100vh - 48px)",
        width: "100%",
        background: PUBLISH_C.bg,
        position: "relative",
        fontFamily: "'DM Sans', sans-serif",
      }}
    >
      <div
        style={{
          width: "100%",
          height: "calc(100vh - 48px)",
          position: "relative",
          background: PUBLISH_C.bg,
        }}
      >
        {step === 1 && <DatosScreen onNext={goNext} onBack={goBack} />}
        {step === 2 && <PhotosScreen onNext={goNext} onBack={goBack} />}
        {step === 3 && <DetailsScreen onNext={goNext} onBack={goBack} />}
        {step === 4 && (
          <ConfirmScreen onPublish={() => setPublished(true)} onBack={goBack} />
        )}
        {published && (
          <SuccessOverlay
            onDone={() => {
              setPublished(false)
              onPanel()
            }}
          />
        )}
      </div>
    </div>
  )
}

function WebHomeScreen({
  userMode,
}: {
  userMode: "visitor" | "user" | "admin"
}) {
  const [menuOpen, setMenuOpen] = useState(false)
  const [showProfile, setShowProfile] = useState(false)
  const [showFavorites, setShowFavorites] = useState(false)
  const [showAdminDashboard, setShowAdminDashboard] = useState(false)
  const [showUserDashboard, setShowUserDashboard] = useState(false)
  const [showPublishFlow, setShowPublishFlow] = useState(false)
  const [selectedCar, setSelectedCar] = useState<CarDetail | null>(null)
  const [toastMsg, setToastMsg] = useState<string | null>(null)

  useEffect(() => {
    setMenuOpen(false)
  }, [
    selectedCar,
    showProfile,
    showFavorites,
    showAdminDashboard,
    showUserDashboard,
    userMode,
  ])

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "instant" as ScrollBehavior })
  }, [
    selectedCar,
    showProfile,
    showFavorites,
    showAdminDashboard,
    showUserDashboard,
    showPublishFlow,
  ])

  const requireRegister = (msg: string) => {
    if (userMode !== "visitor") return
    setToastMsg(msg)
  }

  const goToRegister = () => {
    setToastMsg(null)
    alert("Aquí se abriría la pantalla de registro.")
  }

  const renderScreen = () => {
    if (showPublishFlow && userMode !== "admin") {
      return (
        <PublishFlowView
          onBack={() => setShowPublishFlow(false)}
          onPanel={() => {
            setShowPublishFlow(false)
            setShowUserDashboard(true)
          }}
        />
      )
    }

    if (selectedCar) {
      return (
        <WebCarDetailView
          car={selectedCar}
          onBack={() => {
            setSelectedCar(null)
            setToastMsg(null)
          }}
          onRequireRegister={requireRegister}
        />
      )
    }

    if (showAdminDashboard && userMode === "admin") {
      return (
        <WebAdminDashboardView onBack={() => setShowAdminDashboard(false)} />
      )
    }

    if (showUserDashboard && userMode !== "visitor" && userMode !== "admin") {
      return (
        <div
          style={{
            minHeight: "calc(100vh - 48px)",
            width: "100%",
            background: "#F4F7F9",
            boxSizing: "border-box",
          }}
        >
          <div style={{ width: "100%" }}>
            <div
              style={{
                width: "100%",
                height: "calc(100vh - 48px)",
                background: PUBLISH_C.bg,
              }}
            >
              <DashboardScreen
                onNew={() => {
                  setShowUserDashboard(false)
                  setShowPublishFlow(true)
                }}
                onBack={() => {
                  setShowUserDashboard(false)
                  setShowProfile(true)
                }}
              />
            </div>
          </div>
        </div>
      )
    }

    if (showProfile && userMode !== "visitor") {
      return (
        <WebProfileView
          onBack={() => setShowProfile(false)}
          onPanel={() => {
            setShowProfile(false)
            if (userMode === "admin") {
              setShowAdminDashboard(true)
            } else {
              setShowUserDashboard(true)
            }
          }}
        />
      )
    }

    if (showFavorites && userMode === "user") {
      return <WebFavoritesView onBack={() => setShowFavorites(false)} />
    }

    return (
      <div
        style={{
          minHeight: "100vh",
          background: "#F4F7F9",
          fontFamily: "'DM Sans', sans-serif",
          position: "relative",
        }}
      >
        <header
          style={{
            position: "sticky",
            top: 0,
            zIndex: 100,
            background: "#FFFFFF",
            borderBottom: "1px solid #E2E8ED",
            boxShadow: "0 2px 12px rgba(13,43,69,0.07)",
          }}
        >
          <div
            style={{
              padding: "0 20px",
              height: 60,
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 9,
                flexShrink: 0,
              }}
            >
              <KarsyLogo size={34} />
              <span
                style={{
                  fontFamily: "'Outfit', sans-serif",
                  fontWeight: 700,
                  fontSize: 19,
                  color: "#0D2B45",
                  letterSpacing: "0.03em",
                }}
              >
                Karsy
              </span>
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
              {userMode === "visitor" ? (
                <button
                  style={{
                    padding: "8px 18px",
                    borderRadius: 10,
                    background: "#0D2B45",
                    border: "none",
                    color: "#FFFFFF",
                    fontFamily: "'Outfit', sans-serif",
                    fontSize: 13,
                    fontWeight: 600,
                    cursor: "pointer",
                    boxShadow: "0 2px 8px rgba(13,43,69,0.22)",
                  }}
                >
                  Regístrate
                </button>
              ) : (
                <>
                  {userMode === "admin" ? (
                    <button
                      onClick={() => {
                        setMenuOpen(false)
                        setShowAdminDashboard(true)
                      }}
                      title="Panel de administración"
                      style={{
                        background: "#F4F7F9",
                        border: "1.5px solid #E2E8ED",
                        borderRadius: 10,
                        width: 38,
                        height: 38,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        cursor: "pointer",
                        transition: "background 0.2s, border-color 0.2s",
                      }}
                      onMouseEnter={(e) => {
                        ;(e.currentTarget as HTMLButtonElement).style.background =
                          "#EBF6F7"
                        ;(e.currentTarget as HTMLButtonElement).style.borderColor =
                          "#52A3AA"
                      }}
                      onMouseLeave={(e) => {
                        ;(e.currentTarget as HTMLButtonElement).style.background =
                          "#F4F7F9"
                        ;(e.currentTarget as HTMLButtonElement).style.borderColor =
                          "#E2E8ED"
                      }}
                    >
                      <DashboardIcon size={17} />
                    </button>
                  ) : (
                    <button
                      onClick={() => setShowFavorites(true)}
                      title="Mis favoritos"
                      style={{
                        background: "#F4F7F9",
                        border: "1.5px solid #E2E8ED",
                        borderRadius: 10,
                        width: 38,
                        height: 38,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        cursor: "pointer",
                        transition: "background 0.2s",
                      }}
                      onMouseEnter={(e) => {
                        ;(e.currentTarget as HTMLButtonElement).style.background =
                          "#EBF6F7"
                        ;(e.currentTarget as HTMLButtonElement).style.borderColor =
                          "#52A3AA"
                      }}
                      onMouseLeave={(e) => {
                        ;(e.currentTarget as HTMLButtonElement).style.background =
                          "#F4F7F9"
                        ;(e.currentTarget as HTMLButtonElement).style.borderColor =
                          "#E2E8ED"
                      }}
                    >
                      <HeartIcon size={17} />
                    </button>
                  )}

                  <button
                    onClick={() => setShowProfile(true)}
                    title="Mi perfil"
                    style={{
                      background: "#F4F7F9",
                      border: "1.5px solid #E2E8ED",
                      borderRadius: 10,
                      width: 38,
                      height: 38,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      cursor: "pointer",
                      transition: "background 0.2s",
                    }}
                    onMouseEnter={(e) => {
                      ;(e.currentTarget as HTMLButtonElement).style.background =
                        "#EBF6F7"
                      ;(e.currentTarget as HTMLButtonElement).style.borderColor =
                        "#52A3AA"
                    }}
                    onMouseLeave={(e) => {
                      ;(e.currentTarget as HTMLButtonElement).style.background =
                        "#F4F7F9"
                      ;(e.currentTarget as HTMLButtonElement).style.borderColor =
                        "#E2E8ED"
                    }}
                  >
                    <svg
                      width="17"
                      height="17"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="#0D2B45"
                      strokeWidth="1.8"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                      <circle cx="12" cy="7" r="4" />
                    </svg>
                  </button>
                </>
              )}

              <button
                onClick={() => setMenuOpen(!menuOpen)}
                style={{
                  background: menuOpen ? "#EBF6F7" : "#F4F7F9",
                  border: `1.5px solid ${menuOpen ? "#52A3AA" : "#E2E8ED"}`,
                  borderRadius: 10,
                  width: 38,
                  height: 38,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  cursor: "pointer",
                  transition: "all 0.2s",
                  flexShrink: 0,
                }}
                aria-label="Filtrar"
              >
                {menuOpen ? (
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="#52A3AA"
                    strokeWidth="2.2"
                    strokeLinecap="round"
                  >
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                ) : (
                  <svg
                    width="17"
                    height="17"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="#0D2B45"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <line x1="4" y1="6" x2="20" y2="6" />
                    <line x1="8" y1="12" x2="16" y2="12" />
                    <line x1="11" y1="18" x2="13" y2="18" />
                  </svg>
                )}
              </button>
            </div>
          </div>
        </header>

        {menuOpen && (
          <>
            <div
              onClick={() => setMenuOpen(false)}
              style={{
                position: "fixed",
                inset: 0,
                background: "rgba(13,43,69,0.35)",
                zIndex: 150,
                backdropFilter: "blur(2px)",
              }}
            />
            <div
              style={{
                position: "fixed",
                top: 0,
                right: 0,
                bottom: 0,
                width: 300,
                background: "#FFFFFF",
                zIndex: 200,
                boxShadow: "-8px 0 40px rgba(13,43,69,0.18)",
                display: "flex",
                flexDirection: "column",
                animation: "slideIn 0.22s ease-out",
              }}
            >
              <div
                style={{
                  padding: "20px 20px 16px",
                  borderBottom: "1px solid #E2E8ED",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <KarsyLogo size={30} />
                  <span
                    style={{
                      fontFamily: "'Outfit', sans-serif",
                      fontWeight: 700,
                      fontSize: 17,
                      color: "#0D2B45",
                    }}
                  >
                    Karsy
                  </span>
                </div>
                <button
                  onClick={() => setMenuOpen(false)}
                  style={{
                    background: "none",
                    border: "none",
                    cursor: "pointer",
                    padding: 4,
                  }}
                >
                  <svg
                    width="18"
                    height="18"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="#8E9A8E"
                    strokeWidth="2"
                    strokeLinecap="round"
                  >
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              </div>

              <div style={{ flex: 1, overflowY: "auto", padding: "8px 0" }}>
                {(userMode === "user" || userMode === "admin") && (
                  <>
                    <div
                      style={{
                        margin: "12px 16px 8px",
                        borderTop: "1px solid #E2E8ED",
                        paddingTop: 12,
                      }}
                    >
                      <span
                        style={{
                          fontFamily: "'DM Sans', sans-serif",
                          fontSize: 10,
                          fontWeight: 700,
                          color: "#8E9A8E",
                          letterSpacing: "0.08em",
                          textTransform: "uppercase",
                        }}
                      >
                        Filtros
                      </span>
                    </div>
                    <div style={{ padding: "4px 16px" }}>
                      {[
                        {
                          label: "Marca",
                          opts: [
                            "Todas",
                            "Toyota",
                            "Honda",
                            "Mazda",
                            "Nissan",
                            "Volkswagen",
                            "Kia",
                            "BMW",
                            "Mercedes-Benz",
                          ],
                        },
                        { label: "Modelo", opts: ["Todos"] },
                        {
                          label: "Año",
                          opts: [
                            "Cualquiera",
                            "2020",
                            "2021",
                            "2022",
                            "2023",
                            "2024",
                          ],
                        },
                        {
                          label: "Tipo",
                          opts: [
                            "Todos",
                            "Sedán",
                            "SUV",
                            "Pickup",
                            "Hatchback",
                            "Deportivo",
                          ],
                        },
                      ].map(({ label, opts }) => (
                        <div key={label} style={{ marginBottom: 12 }}>
                          <label
                            style={{
                              display: "block",
                              fontFamily: "'DM Sans', sans-serif",
                              fontSize: 12,
                              fontWeight: 600,
                              color: "#333333",
                              marginBottom: 5,
                            }}
                          >
                            {label}
                          </label>
                          <select
                            style={{
                              width: "100%",
                              padding: "9px 12px",
                              borderRadius: 10,
                              border: "1.5px solid #E2E8ED",
                              background: "#F4F7F9",
                              fontFamily: "'DM Sans', sans-serif",
                              fontSize: 13,
                              color: "#333333",
                              cursor: "pointer",
                            }}
                          >
                            {opts.map((o) => (
                              <option key={o}>{o}</option>
                            ))}
                          </select>
                        </div>
                      ))}

                      <div style={{ marginBottom: 12 }}>
                        <label
                          style={{
                            display: "block",
                            fontFamily: "'DM Sans', sans-serif",
                            fontSize: 12,
                            fontWeight: 600,
                            color: "#333333",
                            marginBottom: 5,
                          }}
                        >
                          Rango de precio (MXN)
                        </label>
                        <div
                          style={{
                            display: "grid",
                            gridTemplateColumns: "1fr 1fr",
                            gap: 8,
                          }}
                        >
                          <input
                            placeholder="Mín. $0"
                            style={{
                              padding: "9px 10px",
                              borderRadius: 10,
                              border: "1.5px solid #E2E8ED",
                              background: "#F4F7F9",
                              fontFamily: "'DM Sans', sans-serif",
                              fontSize: 12,
                              color: "#333333",
                              width: "100%",
                              boxSizing: "border-box",
                            }}
                          />
                          <input
                            placeholder="Máx. $2M"
                            style={{
                              padding: "9px 10px",
                              borderRadius: 10,
                              border: "1.5px solid #E2E8ED",
                              background: "#F4F7F9",
                              fontFamily: "'DM Sans', sans-serif",
                              fontSize: 12,
                              color: "#333333",
                              width: "100%",
                              boxSizing: "border-box",
                            }}
                          />
                        </div>
                      </div>

                      <div style={{ marginBottom: 16 }}>
                        <label
                          style={{
                            display: "block",
                            fontFamily: "'DM Sans', sans-serif",
                            fontSize: 12,
                            fontWeight: 600,
                            color: "#333333",
                            marginBottom: 5,
                          }}
                        >
                          Ordenar por
                        </label>
                        <select
                          style={{
                            width: "100%",
                            padding: "9px 12px",
                            borderRadius: 10,
                            border: "1.5px solid #E2E8ED",
                            background: "#F4F7F9",
                            fontFamily: "'DM Sans', sans-serif",
                            fontSize: 13,
                            color: "#333333",
                            cursor: "pointer",
                          }}
                        >
                          <option>Más recientes</option>
                          <option>Menor precio</option>
                          <option>Mayor precio</option>
                        </select>
                      </div>

                      <div
                        style={{
                          display: "grid",
                          gridTemplateColumns: "1fr 1fr",
                          gap: 8,
                          marginBottom: 8,
                        }}
                      >
                        <button
                          style={{
                            padding: "10px 0",
                            borderRadius: 10,
                            border: "1.5px solid #E2E8ED",
                            background: "#FFFFFF",
                            fontFamily: "'DM Sans', sans-serif",
                            fontSize: 13,
                            color: "#667085",
                            cursor: "pointer",
                          }}
                        >
                          Limpiar
                        </button>
                        <button
                          onClick={() => setMenuOpen(false)}
                          style={{
                            padding: "10px 0",
                            borderRadius: 10,
                            border: "none",
                            background: "#0D2B45",
                            color: "#FFFFFF",
                            fontFamily: "'Outfit', sans-serif",
                            fontSize: 13,
                            fontWeight: 600,
                            cursor: "pointer",
                          }}
                        >
                          Aplicar
                        </button>
                      </div>
                    </div>
                  </>
                )}

                {userMode === "visitor" && (
                  <div
                    style={{
                      margin: "16px 16px 0",
                      padding: 16,
                      background: "#EBF6F7",
                      borderRadius: 14,
                      border: "1px solid #52A3AA",
                    }}
                  >
                    <p
                      style={{
                        fontFamily: "'DM Sans', sans-serif",
                        fontSize: 13,
                        color: "#0D2B45",
                        margin: "0 0 10px",
                        lineHeight: 1.5,
                      }}
                    >
                      Crea una cuenta para acceder a los filtros y funciones
                      exclusivas.
                    </p>
                    <button
                      onClick={goToRegister}
                      style={{
                        width: "100%",
                        padding: "10px 0",
                        borderRadius: 10,
                        border: "none",
                        background: "#0D2B45",
                        color: "#FFFFFF",
                        fontFamily: "'Outfit', sans-serif",
                        fontSize: 13,
                        fontWeight: 600,
                        cursor: "pointer",
                      }}
                    >
                      Regístrate gratis →
                    </button>
                  </div>
                )}
              </div>
            </div>
          </>
        )}

        <div
          style={{
            background:
              "linear-gradient(135deg, #0D2B45 0%, #1a4a6e 60%, #52A3AA 100%)",
            padding: "52px 32px",
            textAlign: "center",
          }}
        >
          <div style={{ maxWidth: 640, margin: "0 auto" }}>
            <p
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 13,
                fontWeight: 600,
                color: "#52A3AA",
                letterSpacing: "0.1em",
                textTransform: "uppercase",
                margin: "0 0 12px",
              }}
            >
              🚗 Marketplace automotriz #1 en México
            </p>
            <h1
              style={{
                fontFamily: "'Outfit', sans-serif",
                fontSize: 44,
                fontWeight: 800,
                color: "#FFFFFF",
                margin: "0 0 16px",
                letterSpacing: "-0.02em",
                lineHeight: 1.15,
              }}
            >
              Encuentra tu próximo
              <br />
              vehículo ideal
            </h1>
            <p
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 16,
                color: "rgba(255,255,255,0.72)",
                margin: "0 0 32px",
                lineHeight: 1.6,
              }}
            >
              Miles de vehículos verificados en toda la república mexicana.
              Compra, vende y negocia con confianza.
            </p>
            <div
              style={{
                display: "flex",
                gap: 12,
                justifyContent: "center",
                flexWrap: "wrap",
              }}
            >
              <div
                style={{
                  display: "flex",
                  background: "rgba(255,255,255,0.10)",
                  backdropFilter: "blur(8px)",
                  borderRadius: 12,
                  overflow: "hidden",
                  border: "1px solid rgba(255,255,255,0.18)",
                  width: 380,
                }}
              >
                <input
                  placeholder="Buscar marca, modelo o año..."
                  style={{
                    flex: 1,
                    padding: "13px 18px",
                    background: "transparent",
                    border: "none",
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: 14,
                    color: "#FFFFFF",
                    outline: "none",
                  }}
                />
                <button
                  style={{
                    padding: "13px 20px",
                    background: "#52A3AA",
                    border: "none",
                    cursor: "pointer",
                    color: "#FFFFFF",
                    fontFamily: "'Outfit', sans-serif",
                    fontSize: 14,
                    fontWeight: 600,
                  }}
                >
                  Buscar
                </button>
              </div>
            </div>
            <div
              style={{
                display: "flex",
                gap: 28,
                justifyContent: "center",
                marginTop: 28,
              }}
            >
              {[
                ["12,400+", "Vehículos"],
                ["50+", "Marcas"],
                ["100+", "Modelos"],
              ].map(([num, lbl]) => (
                <div key={lbl} style={{ textAlign: "center" }}>
                  <div
                    style={{
                      fontFamily: "'Outfit', sans-serif",
                      fontSize: 22,
                      fontWeight: 700,
                      color: "#FFFFFF",
                    }}
                  >
                    {num}
                  </div>
                  <div
                    style={{
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 12,
                      color: "rgba(255,255,255,0.6)",
                    }}
                  >
                    {lbl}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div
          style={{
            maxWidth: 1280,
            margin: "0 auto",
            padding: "48px 32px 120px",
          }}
        >
          {userMode === "visitor" && (
            <div
              style={{
                background: "linear-gradient(to right, #EBF6F7, #F4F7F9)",
                border: "1.5px solid #52A3AA",
                borderRadius: 16,
                padding: "18px 24px",
                marginBottom: 40,
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                flexWrap: "wrap",
                gap: 12,
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <span style={{ fontSize: 22 }}>👀</span>
                <div>
                  <div
                    style={{
                      fontFamily: "'Outfit', sans-serif",
                      fontSize: 15,
                      fontWeight: 700,
                      color: "#0D2B45",
                    }}
                  >
                    Estás en modo visitante
                  </div>
                  <div
                    style={{
                      fontFamily: "'DM Sans', sans-serif",
                      fontSize: 13,
                      color: "#667085",
                    }}
                  >
                    Regístrate para guardar favoritos, contactar vendedores y
                    publicar tu vehículo.
                  </div>
                </div>
              </div>
              <button
                onClick={goToRegister}
                style={{
                  padding: "9px 22px",
                  borderRadius: 10,
                  background: "#0D2B45",
                  border: "none",
                  color: "#FFFFFF",
                  fontFamily: "'Outfit', sans-serif",
                  fontSize: 14,
                  fontWeight: 600,
                  cursor: "pointer",
                  whiteSpace: "nowrap",
                }}
              >
                Crear cuenta gratis →
              </button>
            </div>
          )}

          <section style={{ marginBottom: 56 }}>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                marginBottom: 24,
              }}
            >
              <div>
                <h2
                  style={{
                    fontFamily: "'Outfit', sans-serif",
                    fontSize: 26,
                    fontWeight: 700,
                    color: "#0D2B45",
                    margin: 0,
                    letterSpacing: "-0.01em",
                  }}
                >
                  Vehículos destacados
                </h2>
                <p
                  style={{
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: 14,
                    color: "#8E9A8E",
                    margin: "4px 0 0",
                  }}
                >
                  Selección especial de nuestra plataforma
                </p>
              </div>
              <button
                style={{
                  background: "none",
                  border: "1.5px solid #E2E8ED",
                  borderRadius: 10,
                  padding: "8px 18px",
                  fontFamily: "'DM Sans', sans-serif",
                  fontSize: 13,
                  fontWeight: 600,
                  color: "#0D2B45",
                  cursor: "pointer",
                }}
              >
                Ver todos →
              </button>
            </div>
            <div
              style={{
                display: "flex",
                gap: 20,
                overflowX: "auto",
                paddingBottom: 8,
                scrollbarWidth: "none",
              }}
            >
              {FEATURED_CARS.map((car) => (
                <FeaturedCard
                  key={car.id}
                  car={car}
                  onSelect={() => setSelectedCar(enrichCar(car))}
                  onRequireRegister={requireRegister}
                />
              ))}
            </div>
          </section>

          <section>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                marginBottom: 24,
              }}
            >
              <div>
                <h2
                  style={{
                    fontFamily: "'Outfit', sans-serif",
                    fontSize: 26,
                    fontWeight: 700,
                    color: "#0D2B45",
                    margin: 0,
                    letterSpacing: "-0.01em",
                  }}
                >
                  Todos los vehículos
                </h2>
                <p
                  style={{
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: 14,
                    color: "#8E9A8E",
                    margin: "4px 0 0",
                  }}
                >
                  6 vehículos disponibles
                </p>
              </div>
              <div style={{ display: "flex", gap: 8 }}>
                <select
                  style={{
                    padding: "8px 14px",
                    borderRadius: 10,
                    border: "1.5px solid #E2E8ED",
                    background: "#FFFFFF",
                    fontFamily: "'DM Sans', sans-serif",
                    fontSize: 13,
                    color: "#667085",
                    cursor: "pointer",
                  }}
                >
                  <option>Más recientes</option>
                  <option>Menor precio</option>
                  <option>Mayor precio</option>
                </select>
              </div>
            </div>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
                gap: 24,
              }}
            >
              {ALL_CARS.map((car) => (
                <VehicleCard
                  key={car.id}
                  car={car}
                  onSelect={() => setSelectedCar(enrichCar(car))}
                  onRequireRegister={requireRegister}
                />
              ))}
            </div>
          </section>
        </div>

        {userMode === "user" && (
          <button
            style={{
              position: "fixed",
              bottom: 32,
              right: 32,
              width: 56,
              height: 56,
              borderRadius: "50%",
              background: "#52A3AA",
              border: "none",
              color: "#FFFFFF",
              fontSize: 28,
              fontWeight: 300,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              cursor: "pointer",
              boxShadow: "0 6px 24px rgba(82,163,170,0.45)",
              transition: "transform 0.2s, box-shadow 0.2s",
              zIndex: 200,
            }}
            title="Agregar vehículo"
            onClick={() => setShowPublishFlow(true)}
            onMouseEnter={(e) => {
              ;(e.currentTarget as HTMLButtonElement).style.transform =
                "scale(1.1)"
              ;(e.currentTarget as HTMLButtonElement).style.boxShadow =
                "0 10px 32px rgba(82,163,170,0.55)"
            }}
            onMouseLeave={(e) => {
              ;(e.currentTarget as HTMLButtonElement).style.transform =
                "scale(1)"
              ;(e.currentTarget as HTMLButtonElement).style.boxShadow =
                "0 6px 24px rgba(82,163,170,0.45)"
            }}
          >
            +
          </button>
        )}
        {userMode === "visitor" && (
          <button
            onClick={() =>
              requireRegister("Regístrate para publicar tu vehículo")
            }
            style={{
              position: "fixed",
              bottom: 32,
              right: 32,
              width: 56,
              height: 56,
              borderRadius: "50%",
              background: "#D0D8E0",
              border: "none",
              color: "#8E9A8E",
              fontSize: 28,
              fontWeight: 300,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              cursor: "pointer",
              boxShadow: "0 4px 12px rgba(0,0,0,0.10)",
              zIndex: 200,
            }}
            title="Inicia sesión para agregar un vehículo"
          >
            +
          </button>
        )}
      </div>
    )
  }

  return (
    <UserModeContext.Provider value={userMode}>
      {renderScreen()}
      {toastMsg && (
        <RegisterToast
          message={toastMsg}
          onClose={() => setToastMsg(null)}
          onRegister={goToRegister}
        />
      )}
    </UserModeContext.Provider>
  )
}

const PROFILE_POST_IMGS = [
  "https://images.unsplash.com/photo-1610809589386-9ea41901eb54?w=220&h=220&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1629538745524-5b748fddac9f?w=220&h=220&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1580273916550-e323be2ae537?w=220&h=220&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1767749995450-7b63ab7cd4fd?w=220&h=220&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1758411898152-5fde4b5eef56?w=220&h=220&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1602791036370-b00a495d8a58?w=220&h=220&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1571987502227-9231b837d92a?w=220&h=220&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1522770450359-3de04ff5c9e2?w=220&h=220&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1647588854348-f3b37a0f0ef7?w=220&h=220&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1770936044591-979681c051cf?w=220&h=220&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1783740486439-b4487fad649f?w=220&h=220&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1788178243401-854e12eeb8b1?w=220&h=220&fit=crop&auto=format",
]

const FAVORITES = [
  {
    id: 1,
    brand: "BMW",
    model: "Serie 3 320i",
    year: 2023,
    price: "$685,000",
    condition: "Seminuevo",
    rating: 4.8,
    img: "https://images.unsplash.com/photo-1783740486439-b4487fad649f?w=340&h=220&fit=crop&auto=format",
  },
  {
    id: 2,
    brand: "Porsche",
    model: "Cayenne S",
    year: 2022,
    price: "$1,450,000",
    condition: "Usado",
    rating: 4.9,
    img: "https://images.unsplash.com/photo-1763165524637-9067debdc80b?w=340&h=220&fit=crop&auto=format",
  },
  {
    id: 3,
    brand: "Mercedes-Benz",
    model: "GLA 200",
    year: 2023,
    price: "$798,000",
    condition: "Seminuevo",
    rating: 4.7,
    img: "https://images.unsplash.com/photo-1788178243401-854e12eeb8b1?w=340&h=220&fit=crop&auto=format",
  },
  {
    id: 4,
    brand: "Toyota",
    model: "Corolla LE",
    year: 2022,
    price: "$325,000",
    condition: "Usado",
    rating: 4.5,
    img: "https://images.unsplash.com/photo-1629538745524-5b748fddac9f?w=340&h=220&fit=crop&auto=format",
  },
  {
    id: 5,
    brand: "Honda",
    model: "Civic Sport",
    year: 2021,
    price: "$298,000",
    condition: "Usado",
    rating: 4.6,
    img: "https://images.unsplash.com/photo-1610809589386-9ea41901eb54?w=340&h=220&fit=crop&auto=format",
  },
  {
    id: 6,
    brand: "Audi",
    model: "A4 2.0 TFSI",
    year: 2022,
    price: "$730,000",
    condition: "Seminuevo",
    rating: 4.8,
    img: "https://images.unsplash.com/photo-1764013290175-2b76e9a00b2e?w=340&h=220&fit=crop&auto=format",
  },
]

const PUBLISH_C = {
  navy: "#0D2B45",
  teal: "#52A3AA",
  tealLight: "#E8F4F5",
  bg: "#F4F7F9",
  dark: "#333333",
  muted: "#8E9A8E",
  mutedBorder: "#D4DDE4",
  white: "#FFFFFF",
}

function BackIcon() {
  return (
    <svg width="9" height="15" viewBox="0 0 9 15" fill="none">
      <path
        d="M8 1L2 7.5L8 14"
        stroke="currentColor"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function Check({
  size = 10,
  color = "white",
}: {
  size?: number
  color?: string
}) {
  return (
    <svg width={size} height={size * 0.8} viewBox="0 0 12 10" fill="none">
      <path
        d="M1 5L4.5 8.5L11 1"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function Header({ title, onBack }: { title: string onBack?: () => void }) {
  return (
    <div className="flex items-center px-5 pt-6 pb-2">
      <button
        onClick={onBack}
        className="w-9 h-9 flex items-center justify-center rounded-full"
        style={{ backgroundColor: PUBLISH_C.bg, color: PUBLISH_C.navy }}
      >
        <BackIcon />
      </button>
      <h1
        className="flex-1 text-center font-bold text-[17px]"
        style={{ color: PUBLISH_C.navy, paddingRight: "2.25rem" }}
      >
        {title}
      </h1>
    </div>
  )
}

function Stepper({ step }: { step: 1 | 2 | 3 | 4 }) {
  const steps = ["Datos", "Fotos", "Detalles", "Confirmar"]
  return (
    <div className="flex items-start px-5 py-4">
      {steps.map((label, i) => {
        const num = i + 1
        const done = num < step
        const active = num === step
        const inactive = num > step
        return (
          <div
            key={i}
            className="flex items-center"
            style={{ flex: i < 3 ? "1 1 0" : "none" }}
          >
            <div className="flex flex-col items-center gap-[5px]">
              <div
                className="w-7 h-7 rounded-full flex items-center justify-center text-[11px] font-bold transition-all"
                style={{
                  backgroundColor:
                    done || active ? PUBLISH_C.navy : "transparent",
                  border: `2px solid ${
                    inactive ? PUBLISH_C.muted : PUBLISH_C.navy
                  }`,
                  color: done || active ? PUBLISH_C.white : PUBLISH_C.muted,
                }}
              >
                {done ? <Check size={10} color={PUBLISH_C.white} /> : num}
              </div>
              <span
                className="text-[10px] whitespace-nowrap"
                style={{
                  color: inactive ? PUBLISH_C.muted : PUBLISH_C.navy,
                  fontWeight: active ? 700 : 400,
                }}
              >
                {label}
              </span>
            </div>
            {i < 3 && (
              <div
                className="flex-1 h-[2px] mb-[18px] mx-[3px]"
                style={{
                  backgroundColor:
                    num < step ? PUBLISH_C.navy : PUBLISH_C.muted,
                  opacity: inactive ? 0.4 : 1,
                }}
              />
            )}
          </div>
        )
      })}
    </div>
  )
}

const marcas = [
  "BMW",
  "Mercedes-Benz",
  "Audi",
  "Toyota",
  "Honda",
  "Nissan",
  "Volkswagen",
  "Ford",
  "Chevrolet",
  "Mazda",
  "Kia",
  "Hyundai",
  "Dodge",
  "Jeep",
  "Subaru",
  "Volvo",
  "Porsche",
  "Lexus",
  "MINI",
  "Otro",
]
const tiposCarro = [
  "Sedán",
  "SUV",
  "Deportivo",
  "Pickup",
  "Convertible",
  "Hatchback",
  "Van",
]
const carColors = [
  { name: "Blanco", hex: "#FFFFFF" },
  { name: "Negro", hex: "#1A1A1A" },
  { name: "Plata", hex: "#B0B8C1" },
  { name: "Gris", hex: "#6B7280" },
  { name: "Azul", hex: "#2563EB" },
  { name: "Marino", hex: "#0D2B45" },
  { name: "Verde", hex: "#16A34A" },
  { name: "Dorado", hex: "#D97706" },
  { name: "Beige", hex: "#C9A87C" },
  { name: "Naranja", hex: "#EA580C" },
]

function FieldLabel({ children }: { children: React.ReactNode }) {
  return (
    <p
      className="text-[11px] font-bold tracking-wider mb-1.5"
      style={{ color: PUBLISH_C.muted }}
    >
      {children}
    </p>
  )
}

function TextInput({
  placeholder,
  type = "text",
  value,
  onChange,
}: {
  placeholder: string
  type?: string
  value: string
  onChange: (v: string) => void
}) {
  return (
    <input
      type={type}
      placeholder={placeholder}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="w-full px-4 py-3 rounded-xl text-[14px] outline-none"
      style={{
        backgroundColor: PUBLISH_C.white,
        border: `1.5px solid ${PUBLISH_C.mutedBorder}`,
        color: PUBLISH_C.dark,
      }}
    />
  )
}

function SectionCard({ children }: { children: React.ReactNode }) {
  return (
    <div
      className="rounded-2xl p-4 space-y-4"
      style={{
        backgroundColor: PUBLISH_C.white,
        boxShadow: "0 1px 6px rgba(13,43,69,0.06)",
      }}
    >
      {children}
    </div>
  )
}

function DatosScreen({
  onNext,
  onBack,
}: {
  onNext: () => void
  onBack: () => void
}) {
  const [marca, setMarca] = useState("")
  const [modelo, setModelo] = useState("")
  const [anio, setAnio] = useState("")
  const [precio, setPrecio] = useState("")
  const [transmision, setTransmision] = useState<"Manual" | "Automática" | "">(
    "",
  )
  const [km, setKm] = useState("")
  const [cilindros, setCilindros] = useState("")
  const [hp, setHp] = useState("")
  const [tipo, setTipo] = useState("")
  const [color, setColor] = useState("")
  const [duenos, setDuenos] = useState("")

  return (
    <div
      className="flex flex-col h-full relative"
      style={{ backgroundColor: PUBLISH_C.bg }}
    >
      <Header title="Publicar vehículo" onBack={onBack} />
      <Stepper step={1} />

      <div className="flex-1 overflow-y-auto px-4 pb-28 space-y-4">
        <SectionCard>
          <p
            className="text-[12px] font-bold"
            style={{ color: PUBLISH_C.navy }}
          >
            INFORMACIÓN DEL VEHÍCULO
          </p>

          <div>
            <FieldLabel>MARCA</FieldLabel>
            <div className="relative">
              <select
                value={marca}
                onChange={(e) => setMarca(e.target.value)}
                className="w-full px-4 py-3 rounded-xl text-[14px] outline-none appearance-none"
                style={{
                  backgroundColor: PUBLISH_C.bg,
                  border: `1.5px solid ${PUBLISH_C.mutedBorder}`,
                  color: marca ? PUBLISH_C.dark : PUBLISH_C.muted,
                }}
              >
                <option value="">Selecciona una marca</option>
                {marcas.map((m) => (
                  <option key={m} value={m}>
                    {m}
                  </option>
                ))}
              </select>
              <div
                className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none"
                style={{ color: PUBLISH_C.muted }}
              >
                <svg width="12" height="7" viewBox="0 0 12 7" fill="none">
                  <path
                    d="M1 1l5 5 5-5"
                    stroke="currentColor"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                  />
                </svg>
              </div>
            </div>
          </div>

          <div>
            <FieldLabel>MODELO</FieldLabel>
            <TextInput
              placeholder="Ej. Serie 3 320i"
              value={modelo}
              onChange={setModelo}
            />
          </div>

          <div className="flex gap-3">
            <div className="flex-1">
              <FieldLabel>AÑO</FieldLabel>
              <TextInput
                placeholder="2023"
                type="number"
                value={anio}
                onChange={setAnio}
              />
            </div>
            <div className="flex-1">
              <FieldLabel>PRECIO ($)</FieldLabel>
              <TextInput
                placeholder="685,000"
                type="number"
                value={precio}
                onChange={setPrecio}
              />
            </div>
          </div>

          <div>
            <FieldLabel>TRANSMISIÓN</FieldLabel>
            <div className="flex gap-2">
              {(["Manual", "Automática"] as const).map((t) => (
                <button
                  key={t}
                  onClick={() => setTransmision(t)}
                  className="flex-1 py-3 rounded-xl text-[13px] font-semibold transition-all"
                  style={{
                    backgroundColor:
                      transmision === t ? PUBLISH_C.navy : PUBLISH_C.bg,
                    color:
                      transmision === t ? PUBLISH_C.white : PUBLISH_C.muted,
                    border: `1.5px solid ${
                      transmision === t ? PUBLISH_C.navy : PUBLISH_C.mutedBorder
                    }`,
                  }}
                >
                  {t}
                </button>
              ))}
            </div>
          </div>

          <div>
            <FieldLabel>KILOMETRAJE</FieldLabel>
            <TextInput
              placeholder="18,500 km"
              type="number"
              value={km}
              onChange={setKm}
            />
          </div>

          <div className="flex gap-3">
            <div className="flex-1">
              <FieldLabel>CILINDROS</FieldLabel>
              <TextInput
                placeholder="4"
                type="number"
                value={cilindros}
                onChange={setCilindros}
              />
            </div>
            <div className="flex-1">
              <FieldLabel>CABALLOS DE FUERZA</FieldLabel>
              <TextInput
                placeholder="184 hp"
                type="number"
                value={hp}
                onChange={setHp}
              />
            </div>
          </div>
        </SectionCard>

        <SectionCard>
          <p
            className="text-[12px] font-bold"
            style={{ color: PUBLISH_C.navy }}
          >
            CLASIFICACIÓN
          </p>

          <div>
            <FieldLabel>TIPO DE CARRO</FieldLabel>
            <div className="flex flex-wrap gap-2">
              {tiposCarro.map((t) => (
                <button
                  key={t}
                  onClick={() => setTipo(t)}
                  className="px-4 py-2 rounded-full text-[12px] font-semibold transition-all"
                  style={{
                    backgroundColor: tipo === t ? PUBLISH_C.navy : PUBLISH_C.bg,
                    color: tipo === t ? PUBLISH_C.white : PUBLISH_C.muted,
                    border: `1.5px solid ${
                      tipo === t ? PUBLISH_C.navy : PUBLISH_C.mutedBorder
                    }`,
                  }}
                >
                  {t}
                </button>
              ))}
            </div>
          </div>

          <div>
            <FieldLabel>COLOR</FieldLabel>
            <div className="flex flex-wrap gap-3 mt-1">
              {carColors.map((c) => (
                <button
                  key={c.name}
                  onClick={() => setColor(c.name)}
                  title={c.name}
                  className="flex flex-col items-center gap-1"
                >
                  <div
                    className="w-8 h-8 rounded-full transition-all"
                    style={{
                      backgroundColor: c.hex,
                      border:
                        color === c.name
                          ? `3px solid ${PUBLISH_C.navy}`
                          : `2px solid ${PUBLISH_C.mutedBorder}`,
                      boxShadow:
                        color === c.name
                          ? `0 0 0 2px ${PUBLISH_C.teal}`
                          : "none",
                    }}
                  />
                  <span
                    className="text-[9px]"
                    style={{
                      color:
                        color === c.name ? PUBLISH_C.navy : PUBLISH_C.muted,
                      fontWeight: color === c.name ? 700 : 400,
                    }}
                  >
                    {c.name}
                  </span>
                </button>
              ))}
            </div>
          </div>
        </SectionCard>

        <SectionCard>
          <p
            className="text-[12px] font-bold"
            style={{ color: PUBLISH_C.navy }}
          >
            INFORMACIÓN ADICIONAL
          </p>

          <div>
            <FieldLabel>CANTIDAD DE DUEÑOS</FieldLabel>
            <TextInput
              placeholder="1"
              type="number"
              value={duenos}
              onChange={setDuenos}
            />
          </div>
        </SectionCard>
      </div>

      <div
        className="absolute bottom-0 left-0 right-0 px-4 pb-5 pt-3"
        style={{
          backgroundColor: PUBLISH_C.bg,
          borderTop: `1px solid ${PUBLISH_C.mutedBorder}`,
        }}
      >
        <button
          onClick={onNext}
          className="w-full py-[15px] rounded-full font-semibold text-[15px] transition-opacity active:opacity-80"
          style={{ backgroundColor: PUBLISH_C.navy, color: PUBLISH_C.white }}
        >
          Siguiente: Fotos →
        </button>
      </div>
    </div>
  )
}

const photoSlots = [
  {
    label: "Foto frontal",
    img: "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=120&h=90&fit=crop&auto=format",
  },
  { label: "Foto trasera", img: "" },
  { label: "Foto lateral izquierda", img: "" },
  { label: "Foto lateral derecha", img: "" },
  { label: "Foto del tablero", img: "" },
]

function CarSvg({ slot }: { slot: number }) {
  if (slot === 4) {
    return (
      <svg width="34" height="28" viewBox="0 0 34 28" fill="none">
        <rect
          x="2"
          y="8"
          width="30"
          height="13"
          rx="3"
          fill={PUBLISH_C.tealLight}
          stroke={PUBLISH_C.teal}
          strokeWidth="1.5"
        />
        <path
          d="M6 13 Q17 9 28 13"
          stroke={PUBLISH_C.teal}
          strokeWidth="1.5"
          fill="none"
        />
        <rect
          x="10"
          y="10"
          width="14"
          height="5"
          rx="1.5"
          fill={PUBLISH_C.teal}
          opacity="0.35"
        />
        <circle cx="8" cy="21" r="3.5" fill={PUBLISH_C.navy} />
        <circle cx="26" cy="21" r="3.5" fill={PUBLISH_C.navy} />
        <circle cx="8" cy="21" r="1.5" fill={PUBLISH_C.tealLight} />
        <circle cx="26" cy="21" r="1.5" fill={PUBLISH_C.tealLight} />
      </svg>
    )
  }
  const isLateral = slot === 2 || slot === 3
  if (isLateral) {
    return (
      <svg width="38" height="26" viewBox="0 0 38 26" fill="none">
        <rect
          x="2"
          y="11"
          width="34"
          height="11"
          rx="3"
          fill={PUBLISH_C.tealLight}
          stroke={PUBLISH_C.teal}
          strokeWidth="1.5"
        />
        <rect
          x="7"
          y="5"
          width="20"
          height="9"
          rx="2.5"
          fill={PUBLISH_C.tealLight}
          stroke={PUBLISH_C.teal}
          strokeWidth="1.5"
        />
        <rect
          x="9"
          y="6"
          width="7"
          height="6"
          rx="1"
          fill={PUBLISH_C.teal}
          opacity="0.35"
        />
        <rect
          x="18"
          y="6"
          width="7"
          height="6"
          rx="1"
          fill={PUBLISH_C.teal}
          opacity="0.35"
        />
        <circle cx="9" cy="22" r="3.5" fill={PUBLISH_C.navy} />
        <circle cx="29" cy="22" r="3.5" fill={PUBLISH_C.navy} />
        <circle cx="9" cy="22" r="1.5" fill={PUBLISH_C.tealLight} />
        <circle cx="29" cy="22" r="1.5" fill={PUBLISH_C.tealLight} />
      </svg>
    )
  }
  return (
    <svg width="38" height="28" viewBox="0 0 38 28" fill="none">
      <rect
        x="3"
        y="10"
        width="32"
        height="13"
        rx="3"
        fill={PUBLISH_C.tealLight}
        stroke={PUBLISH_C.teal}
        strokeWidth="1.5"
      />
      <rect
        x="8"
        y="4"
        width="22"
        height="9"
        rx="2.5"
        fill={PUBLISH_C.tealLight}
        stroke={PUBLISH_C.teal}
        strokeWidth="1.5"
      />
      <rect
        x="10"
        y="5"
        width="7"
        height="6"
        rx="1"
        fill={PUBLISH_C.teal}
        opacity="0.4"
      />
      <rect
        x="21"
        y="5"
        width="7"
        height="6"
        rx="1"
        fill={PUBLISH_C.teal}
        opacity="0.4"
      />
      <rect
        x="14"
        y="19"
        width="10"
        height="2.5"
        rx="1"
        fill={PUBLISH_C.teal}
        opacity="0.4"
      />
      <circle cx="10" cy="23" r="3.5" fill={PUBLISH_C.navy} />
      <circle cx="28" cy="23" r="3.5" fill={PUBLISH_C.navy} />
      <circle cx="10" cy="23" r="1.5" fill={PUBLISH_C.tealLight} />
      <circle cx="28" cy="23" r="1.5" fill={PUBLISH_C.tealLight} />
    </svg>
  )
}

function GalleryIcon() {
  return (
    <svg width="17" height="15" viewBox="0 0 17 15" fill="none">
      <rect
        x="1"
        y="2"
        width="15"
        height="12"
        rx="2"
        stroke={PUBLISH_C.muted}
        strokeWidth="1.5"
      />
      <path
        d="M1 9l4-4 4 4 3-3 4 3"
        stroke={PUBLISH_C.muted}
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
      <circle cx="5.5" cy="5.5" r="1.5" fill={PUBLISH_C.muted} />
    </svg>
  )
}

function PhotosScreen({
  onNext,
  onBack,
}: {
  onNext: () => void
  onBack?: () => void
}) {
  return (
    <div
      className="flex flex-col h-full relative"
      style={{ backgroundColor: PUBLISH_C.bg }}
    >
      <Header title="Publicar vehículo" onBack={onBack} />
      <Stepper step={2} />
      <div className="flex-1 overflow-y-auto px-4 pb-28 space-y-3">
        {photoSlots.map((slot, i) => (
          <div
            key={i}
            className="rounded-2xl p-3"
            style={{
              backgroundColor: PUBLISH_C.white,
              border: `1.5px dashed ${PUBLISH_C.muted}`,
            }}
          >
            <div className="flex items-center gap-3 mb-3">
              <div
                className="w-14 h-12 rounded-xl flex items-center justify-center flex-shrink-0"
                style={{ backgroundColor: PUBLISH_C.bg }}
              >
                <CarSvg slot={i} />
              </div>
              <span
                className="flex-1 text-[13px] font-semibold"
                style={{ color: PUBLISH_C.dark }}
              >
                {slot.label}
              </span>
              <div
                className="w-14 h-12 rounded-xl overflow-hidden flex-shrink-0 relative"
                style={{ backgroundColor: PUBLISH_C.bg }}
              >
                {slot.img ? (
                  <img
                    src={slot.img}
                    alt={slot.label}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center">
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <circle
                        cx="10"
                        cy="10"
                        r="9"
                        stroke={PUBLISH_C.muted}
                        strokeWidth="1.5"
                        strokeDasharray="3 2"
                      />
                      <path
                        d="M10 6v8M6 10h8"
                        stroke={PUBLISH_C.muted}
                        strokeWidth="1.5"
                        strokeLinecap="round"
                      />
                    </svg>
                  </div>
                )}
                {slot.img && (
                  <div
                    className="absolute bottom-1 right-1 w-5 h-5 rounded-full flex items-center justify-center shadow"
                    style={{ backgroundColor: PUBLISH_C.teal }}
                  >
                    <Check size={9} color={PUBLISH_C.white} />
                  </div>
                )}
              </div>
            </div>
            <button
              className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl text-[13px] font-medium border"
              style={{
                backgroundColor: PUBLISH_C.white,
                color: PUBLISH_C.dark,
                borderColor: PUBLISH_C.mutedBorder,
              }}
            >
              <GalleryIcon />
              Elegir de galería
            </button>
          </div>
        ))}
      </div>
      <div
        className="absolute bottom-0 left-0 right-0 px-4 pb-5 pt-3"
        style={{
          backgroundColor: PUBLISH_C.bg,
          borderTop: `1px solid ${PUBLISH_C.mutedBorder}`,
        }}
      >
        <button
          onClick={onNext}
          className="w-full py-[15px] rounded-full font-semibold text-[15px] transition-opacity active:opacity-80"
          style={{ backgroundColor: PUBLISH_C.navy, color: PUBLISH_C.white }}
        >
          Siguiente: Detalles →
        </button>
      </div>
    </div>
  )
}

function DetailsScreen({
  onNext,
  onBack,
}: {
  onNext: () => void
  onBack: () => void
}) {
  const [desc, setDesc] = useState("")
  const [imperfections, setImperfections] = useState("")
  return (
    <div
      className="flex flex-col h-full relative"
      style={{ backgroundColor: PUBLISH_C.bg }}
    >
      <Header title="Publicar vehículo" onBack={onBack} />
      <Stepper step={3} />
      <div className="flex-1 overflow-y-auto px-4 pb-28 space-y-4">
        <div>
          <p
            className="text-[13px] font-bold mb-2"
            style={{ color: PUBLISH_C.dark }}
          >
            Descripción del auto
          </p>
          <div
            className="rounded-2xl overflow-hidden"
            style={{
              backgroundColor: PUBLISH_C.white,
              border: `1px solid ${PUBLISH_C.mutedBorder}`,
            }}
          >
            <textarea
              value={desc}
              onChange={(e) => setDesc(e.target.value.slice(0, 500))}
              rows={5}
              placeholder="Una buena descripción aumenta las visitas"
              className="w-full px-4 pt-4 pb-2 text-[13px] resize-none outline-none placeholder:opacity-60"
              style={{
                backgroundColor: PUBLISH_C.white,
                color: PUBLISH_C.dark,
              }}
            />
            <div
              className="px-4 pb-3 text-right text-[11px]"
              style={{ color: PUBLISH_C.muted }}
            >
              {desc.length} / 500
            </div>
          </div>
        </div>

        <div>
          <p
            className="text-[13px] font-bold mb-2"
            style={{ color: PUBLISH_C.dark }}
          >
            Detalles o imperfecciones
          </p>
          <div
            className="rounded-2xl overflow-hidden"
            style={{
              backgroundColor: PUBLISH_C.white,
              border: `1px solid ${PUBLISH_C.mutedBorder}`,
            }}
          >
            <textarea
              value={imperfections}
              onChange={(e) => setImperfections(e.target.value.slice(0, 300))}
              rows={5}
              placeholder="La transparencia genera confianza en los compradores"
              className="w-full px-4 pt-4 pb-2 text-[13px] resize-none outline-none placeholder:opacity-60"
              style={{
                backgroundColor: PUBLISH_C.white,
                color: PUBLISH_C.dark,
              }}
            />
            <div
              className="px-4 pb-3 text-right text-[11px]"
              style={{ color: PUBLISH_C.muted }}
            >
              {imperfections.length} / 300
            </div>
          </div>
        </div>

        <div
          className="rounded-2xl p-4 flex gap-3 items-start"
          style={{
            backgroundColor: PUBLISH_C.tealLight,
            border: `1px solid ${PUBLISH_C.teal}22`,
          }}
        >
          <div
            className="w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5 font-bold text-[13px]"
            style={{ backgroundColor: PUBLISH_C.teal, color: PUBLISH_C.white }}
          >
            i
          </div>
          <p
            className="text-[12px] leading-[1.6]"
            style={{ color: PUBLISH_C.navy }}
          >
            Los anuncios con descripción completa reciben{" "}
            <strong>60% más contactos</strong> que los incompletos.
          </p>
        </div>
      </div>

      <div
        className="absolute bottom-0 left-0 right-0 px-4 pb-5 pt-3"
        style={{
          backgroundColor: PUBLISH_C.bg,
          borderTop: `1px solid ${PUBLISH_C.mutedBorder}`,
        }}
      >
        <button
          onClick={onNext}
          className="w-full py-[15px] rounded-full font-semibold text-[15px] transition-opacity active:opacity-80"
          style={{ backgroundColor: PUBLISH_C.navy, color: PUBLISH_C.white }}
        >
          Siguiente: Confirmación →
        </button>
      </div>
    </div>
  )
}

const techSpecs = [
  ["Modelo", "Serie 3 320i"],
  ["Año", "2023"],
  ["Marca", "BMW"],
  ["Transmisión", "Automática"],
  ["Kilometraje", "18,500 km"],
  ["Cilindros", "4 en línea"],
  ["Caballos de fuerza", "184 hp"],
  ["Tipo de carro", "Sedán"],
  ["Color", "Blanco Alpino"],
  ["Cant. de dueños", "1"],
]

const carImages = [
  "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=800&h=500&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1550355291-bbee04a92027?w=800&h=500&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1542282088-fe8426682b8f?w=200&h=150&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1550355291-bbee04a92027?w=200&h=150&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=200&h=150&fit=crop&auto=format",
]

function ConfirmScreen({
  onPublish,
  onBack,
}: {
  onPublish: () => void
  onBack: () => void
}) {
  const [liked, setLiked] = useState(false)
  const [imgIndex, setImgIndex] = useState(0)
  return (
    <div
      className="flex flex-col h-full relative"
      style={{ backgroundColor: PUBLISH_C.bg }}
    >
      <Header title="Publicar vehículo" onBack={onBack} />
      <Stepper step={4} />
      <div className="flex-1 overflow-y-auto pb-28">
        <div
          className="mx-4 rounded-2xl overflow-hidden relative"
          style={{ height: 210, backgroundColor: PUBLISH_C.dark }}
        >
          <img
            src={carImages[imgIndex]}
            alt="BMW Serie 3"
            className="w-full h-full object-cover"
          />
          <button
            onClick={() =>
              setImgIndex((i) => (i - 1 + carImages.length) % carImages.length)
            }
            className="absolute left-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full flex items-center justify-center shadow-md"
            style={{
              backgroundColor: "rgba(255,255,255,0.88)",
              color: PUBLISH_C.navy,
            }}
          >
            <BackIcon />
          </button>
          <button
            onClick={() => setImgIndex((i) => (i + 1) % carImages.length)}
            className="absolute right-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full flex items-center justify-center shadow-md"
            style={{
              backgroundColor: "rgba(255,255,255,0.88)",
              color: PUBLISH_C.navy,
            }}
          >
            <svg width="9" height="15" viewBox="0 0 9 15" fill="none">
              <path
                d="M1 1L7 7.5L1 14"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </button>
          <div
            className="absolute bottom-3 right-3 px-2.5 py-1 rounded-full text-[11px] font-semibold"
            style={{
              backgroundColor: "rgba(13,43,69,0.75)",
              color: PUBLISH_C.white,
            }}
          >
            {imgIndex + 1} / {carImages.length}
          </div>
        </div>

        <div className="flex gap-2 px-4 mt-2.5 overflow-x-auto">
          {carImages.slice(1).map((src, i) => (
            <button
              key={i}
              onClick={() => setImgIndex(i + 1)}
              className="w-16 h-12 rounded-xl overflow-hidden flex-shrink-0 transition-all"
              style={{
                border: `2px solid ${
                  imgIndex === i + 1 ? PUBLISH_C.navy : "transparent"
                }`,
              }}
            >
              <img
                src={src}
                alt={`thumb ${i + 1}`}
                className="w-full h-full object-cover"
              />
            </button>
          ))}
        </div>

        <div className="px-4 mt-4 flex items-start justify-between">
          <div>
            <p
              className="text-[13px] mt-0.5"
              style={{ color: PUBLISH_C.muted }}
            >
              Serie 3 320i · 2023
            </p>
            <p
              className="text-[22px] font-bold mt-1"
              style={{ color: PUBLISH_C.teal }}
            >
              $685,000 <span className="text-[14px] font-semibold">MXN</span>
            </p>
          </div>
        </div>

        <div className="px-4 mt-5">
          <p
            className="text-[11px] font-bold tracking-wider mb-2"
            style={{ color: PUBLISH_C.navy }}
          >
            FICHA TÉCNICA
          </p>
          <div
            className="rounded-2xl overflow-hidden"
            style={{
              backgroundColor: PUBLISH_C.white,
              border: `1px solid ${PUBLISH_C.mutedBorder}`,
            }}
          >
            {techSpecs.map(([key, val], i) => (
              <div
                key={i}
                className="flex items-center justify-between px-4 py-3"
                style={{
                  borderBottom:
                    i < techSpecs.length - 1
                      ? `1px solid ${PUBLISH_C.bg}`
                      : "none",
                }}
              >
                <span
                  className="text-[12px]"
                  style={{ color: PUBLISH_C.muted }}
                >
                  {key}
                </span>
                <span
                  className="text-[13px] font-semibold"
                  style={{ color: PUBLISH_C.dark }}
                >
                  {val}
                </span>
              </div>
            ))}
          </div>
        </div>

        <div className="px-4 mt-5">
          <p
            className="text-[11px] font-bold tracking-wider mb-2"
            style={{ color: PUBLISH_C.navy }}
          >
            DESCRIPCIÓN
          </p>
          <div
            className="rounded-2xl p-4"
            style={{
              backgroundColor: PUBLISH_C.white,
              border: `1px solid ${PUBLISH_C.mutedBorder}`,
            }}
          >
            <p
              className="text-[13px] leading-[1.7]"
              style={{ color: PUBLISH_C.dark }}
            >
              BMW Serie 3 320i en excelente estado, primer dueño, factura
              original. Motor 2.0L turbo de 184 hp, transmisión automática de 8
              velocidades. Cuenta con techo corredizo, Apple CarPlay, asientos
              de cuero y sistema de frenado automático.
            </p>
          </div>
          <p
            className="text-[11px] font-bold tracking-wider mb-2 mt-4"
            style={{ color: PUBLISH_C.navy }}
          >
            DETALLADO O IMPERFECCIONES
          </p>
          <div
            className="rounded-2xl p-4"
            style={{ backgroundColor: "#FFFBF0", border: `1px solid #F5E8B0` }}
          >
            <p
              className="text-[13px] leading-[1.7]"
              style={{ color: PUBLISH_C.dark }}
            >
              Pequeño rayón en parachoque trasero lado derecho (pintado). Resto
              del vehículo sin golpes ni abolladuras. Tapicería impecable, sin
              olores.
            </p>
          </div>
        </div>

        <div className="h-6" />
      </div>

      <div
        className="absolute bottom-0 left-0 right-0 px-4 pb-5 pt-3"
        style={{
          backgroundColor: PUBLISH_C.bg,
          borderTop: `1px solid ${PUBLISH_C.mutedBorder}`,
        }}
      >
        <button
          onClick={onPublish}
          className="w-full py-[15px] rounded-full font-bold text-[15px] transition-opacity active:opacity-80"
          style={{ backgroundColor: PUBLISH_C.navy, color: PUBLISH_C.white }}
        >
          Publicar
        </button>
      </div>
    </div>
  )
}

const salesMonths = [28, 42, 35, 58, 47, 65, 52, 71, 63, 80, 74, 92]
const favWeekly = [3, 7, 5, 12, 9, 15, 11]
const weekDays = ["L", "M", "X", "J", "V", "S", "D"]

function sparklinePath(
  data: number[],
  w: number,
  h: number,
  fill = false,
): string {
  const minV = Math.min(...data)
  const maxV = Math.max(...data)
  const range = maxV - minV || 1
  const pad = 6
  const xs = data.map((_, i) => (i / (data.length - 1)) * w)
  const ys = data.map((v) => h - pad - ((v - minV) / range) * (h - pad * 2))
  const line = xs
    .map((x, i) => `${i === 0 ? "M" : "L"}${x.toFixed(1)},${ys[i].toFixed(1)}`)
    .join(" ")
  if (!fill) return line
  return `${line} L${w},${h} L0,${h} Z`
}

const myListings = [
  {
    name: "BMW 320i 2023",
    price: "$685,000",
    img: "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=300&h=200&fit=crop&auto=format",
  },
  {
    name: "Mustang EcoBoost 2021",
    price: "$580,000",
    img: "https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=300&h=200&fit=crop&auto=format",
  },
  {
    name: "Audi A4 2022",
    price: "$620,000",
    img: "https://images.unsplash.com/photo-1541443131876-44b03de101c5?w=300&h=200&fit=crop&auto=format",
  },
  {
    name: "Mercedes C200 2022",
    price: "$710,000",
    img: "https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?w=300&h=200&fit=crop&auto=format",
  },
]

function DashboardScreen({
  onNew,
  onBack,
}: {
  onNew: () => void
  onBack: () => void
}) {
  const [destacarCar, setDestacarCar] = useState<string | null>(null)
  const [solicitudCar, setSolicitudCar] = useState<string | null>(null)
  const sW = 200
  const sH = 60
  const aW = 300
  const aH = 90

  return (
    <div
      className="flex flex-col h-full"
      style={{ backgroundColor: PUBLISH_C.bg }}
    >
      <div className="flex items-center justify-between px-5 pt-6 pb-2">
        <div className="flex items-center gap-3">
          <button
            onClick={onBack}
            aria-label="Volver al perfil"
            className="flex items-center justify-center p-0 border-0 bg-transparent cursor-pointer"
            style={{ width: 28, height: 32, color: PUBLISH_C.navy }}
          >
            <span style={{ fontSize: 30, lineHeight: 1, fontWeight: 300 }}>
              ‹
            </span>
          </button>
          <h1
            className="text-[22px] font-bold"
            style={{ color: PUBLISH_C.navy }}
          >
            Mi Panel
          </h1>
        </div>
        <div
          className="w-10 h-10 rounded-full overflow-hidden"
          style={{ border: `2px solid ${PUBLISH_C.teal}` }}
        >
          <img
            src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=80&h=80&fit=crop&auto=format"
            alt="Avatar"
            className="w-full h-full object-cover"
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-4 pb-8 space-y-4 pt-3">
        <div className="flex gap-3">
          <div
            className="rounded-2xl p-4 flex-1"
            style={{ backgroundColor: PUBLISH_C.white }}
          >
            <p
              className="text-[11px] font-semibold"
              style={{ color: PUBLISH_C.muted }}
            >
              VENTAS TOTALES
            </p>
            <p
              className="text-[20px] font-bold mt-0.5"
              style={{ color: PUBLISH_C.navy }}
            >
              163
            </p>
            <p
              className="text-[11px] mt-0.5 mb-3 font-medium"
              style={{ color: PUBLISH_C.teal }}
            >
              ▲ +18% este mes
            </p>
            <svg
              width={sW}
              height={sH}
              viewBox={`0 0 ${sW} ${sH}`}
              className="w-full"
            >
              <defs>
                <linearGradient id="salesGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop
                    offset="0%"
                    stopColor={PUBLISH_C.teal}
                    stopOpacity="0.2"
                  />
                  <stop
                    offset="100%"
                    stopColor={PUBLISH_C.teal}
                    stopOpacity="0.0"
                  />
                </linearGradient>
              </defs>
              <path
                d={sparklinePath(salesMonths, sW, sH, true)}
                fill="url(#salesGrad)"
              />
              <path
                d={sparklinePath(salesMonths, sW, sH, false)}
                stroke={PUBLISH_C.teal}
                strokeWidth="2"
                fill="none"
                strokeLinecap="round"
              />
            </svg>
          </div>

          <div className="flex flex-col gap-3" style={{ width: 110 }}>
            <div
              className="rounded-2xl p-4 flex flex-col justify-between flex-1"
              style={{ backgroundColor: PUBLISH_C.white }}
            >
              <p
                className="text-[11px] font-semibold"
                style={{ color: PUBLISH_C.muted }}
              >
                PUBLICADOS
              </p>
              <p
                className="text-[28px] font-bold leading-none mt-1"
                style={{ color: PUBLISH_C.navy }}
              >
                12
              </p>
              <p
                className="text-[11px] mt-1"
                style={{ color: PUBLISH_C.muted }}
              >
                Vehículos
              </p>
            </div>
            <div
              className="rounded-2xl p-4 flex flex-col justify-between flex-1"
              style={{ backgroundColor: PUBLISH_C.white }}
            >
              <p
                className="text-[11px] font-semibold"
                style={{ color: PUBLISH_C.muted }}
              >
                CONSULTAS
              </p>
              <p
                className="text-[28px] font-bold leading-none mt-1"
                style={{ color: PUBLISH_C.teal }}
              >
                84
              </p>
              <p
                className="text-[11px] mt-1"
                style={{ color: PUBLISH_C.muted }}
              >
                Contactos
              </p>
            </div>
          </div>
        </div>

        <div
          className="rounded-2xl p-4"
          style={{ backgroundColor: PUBLISH_C.white }}
        >
          <p
            className="text-[13px] font-bold mb-1"
            style={{ color: PUBLISH_C.dark }}
          >
            Interés en la semana
          </p>
          <p className="text-[11px] mb-3" style={{ color: PUBLISH_C.muted }}>
            Favoritos agregados por día
          </p>
          <svg
            width={aW}
            height={aH}
            viewBox={`0 0 ${aW} ${aH}`}
            className="w-full"
          >
            <defs>
              <linearGradient id="favGrad" x1="0" y1="0" x2="0" y2="1">
                <stop
                  offset="0%"
                  stopColor={PUBLISH_C.teal}
                  stopOpacity="0.28"
                />
                <stop
                  offset="100%"
                  stopColor={PUBLISH_C.teal}
                  stopOpacity="0.0"
                />
              </linearGradient>
            </defs>
            <path
              d={sparklinePath(favWeekly, aW, aH - 16, true)}
              fill="url(#favGrad)"
            />
            <path
              d={sparklinePath(favWeekly, aW, aH - 16, false)}
              stroke={PUBLISH_C.teal}
              strokeWidth="2.5"
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            {favWeekly.map((v, i) => {
              const minV = Math.min(...favWeekly)
              const maxV = Math.max(...favWeekly)
              const range = maxV - minV || 1
              const x = (i / (favWeekly.length - 1)) * aW
              const y = aH - 16 - 6 - ((v - minV) / range) * (aH - 16 - 12)
              return (
                <g key={i}>
                  <circle
                    cx={x}
                    cy={y}
                    r={v === maxV ? 5 : 3.5}
                    fill={PUBLISH_C.teal}
                  />
                  {v === maxV && (
                    <circle
                      cx={x}
                      cy={y}
                      r={8}
                      fill={PUBLISH_C.teal}
                      opacity="0.2"
                    />
                  )}
                </g>
              )
            })}
          </svg>
          <div className="flex justify-between mt-1">
            {weekDays.map((d) => (
              <span
                key={d}
                className="text-[11px]"
                style={{ color: PUBLISH_C.muted }}
              >
                {d}
              </span>
            ))}
          </div>
        </div>

        <div>
          <div className="flex items-center justify-between mb-3">
            <p
              className="text-[15px] font-bold"
              style={{ color: PUBLISH_C.navy }}
            >
              Mis Vehículos Publicados
            </p>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {myListings.map((car, i) => (
              <div
                key={i}
                className="rounded-2xl overflow-hidden"
                style={{ backgroundColor: PUBLISH_C.white }}
              >
                <div className="relative" style={{ height: 110 }}>
                  <img
                    src={car.img}
                    alt={car.name}
                    className="w-full h-full object-cover"
                  />
                </div>
                <div className="p-3">
                  <p
                    className="text-[12px] font-bold leading-tight"
                    style={{ color: PUBLISH_C.dark }}
                  >
                    {car.name}
                  </p>
                  <p
                    className="text-[13px] font-bold mt-1"
                    style={{ color: PUBLISH_C.teal }}
                  >
                    {car.price}
                  </p>
                  <button
                    onClick={() => setDestacarCar(car.name)}
                    className="w-full mt-2.5 py-2 rounded-xl text-[11px] font-bold transition-all active:opacity-80"
                    style={{
                      backgroundColor: PUBLISH_C.navy,
                      color: PUBLISH_C.white,
                    }}
                  >
                    Destacar
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {destacarCar && (
        <DestacarModal
          carName={destacarCar}
          onConfirm={() => {
            setSolicitudCar(destacarCar)
            setDestacarCar(null)
          }}
          onCancel={() => setDestacarCar(null)}
        />
      )}
      {solicitudCar && (
        <SolicitudEnviada
          carName={solicitudCar}
          onClose={() => setSolicitudCar(null)}
        />
      )}
    </div>
  )
}

function SuccessOverlay({ onDone }: { onDone: () => void }) {
  return (
    <div
      className="absolute inset-0 flex flex-col items-center justify-center z-50 rounded-[40px] overflow-hidden"
      style={{ backgroundColor: "rgba(13,43,69,0.94)" }}
    >
      <div
        className="w-20 h-20 rounded-full flex items-center justify-center mb-6"
        style={{ backgroundColor: PUBLISH_C.teal }}
      >
        <Check size={28} color={PUBLISH_C.white} />
      </div>
      <h2 className="text-[24px] font-bold text-white mb-2">¡Publicado!</h2>
      <p
        className="text-[14px] text-center px-10 mb-8"
        style={{ color: "rgba(255,255,255,0.7)" }}
      >
        Tu BMW Serie 3 320i ya está disponible para los compradores.
      </p>
      <button
        onClick={onDone}
        className="px-10 py-4 rounded-full font-bold text-[15px]"
        style={{ backgroundColor: PUBLISH_C.teal, color: PUBLISH_C.white }}
      >
        Ver mi panel
      </button>
    </div>
  )
}

function DestacarModal({
  carName,
  onConfirm,
  onCancel,
}: {
  carName: string
  onConfirm: () => void
  onCancel: () => void
}) {
  return (
    <div
      className="absolute inset-0 flex items-end justify-center z-50 pb-6 px-4"
      style={{ backgroundColor: "rgba(13,43,69,0.52)" }}
      onClick={onCancel}
    >
      <div
        className="w-full rounded-[28px] overflow-hidden"
        style={{
          backgroundColor: PUBLISH_C.white,
          boxShadow: "0 24px 60px rgba(13,43,69,0.22)",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex justify-center pt-3 pb-1">
          <div
            className="w-10 h-1 rounded-full"
            style={{ backgroundColor: PUBLISH_C.mutedBorder }}
          />
        </div>

        <div className="px-6 pb-7 pt-3 flex flex-col items-center text-center">
          <div className="relative mb-5">
            <div
              className="w-20 h-20 rounded-full flex items-center justify-center"
              style={{ backgroundColor: PUBLISH_C.tealLight }}
            >
              <div
                className="w-14 h-14 rounded-full flex items-center justify-center"
                style={{ backgroundColor: PUBLISH_C.teal }}
              >
                <svg width="30" height="29" viewBox="0 0 30 29" fill="none">
                  <path
                    d="M15 2l3.09 6.26L25 9.27l-5 4.87 1.18 6.88L15 17.77l-6.18 3.25L10 14.14 5 9.27l6.91-1.01L15 2z"
                    fill={PUBLISH_C.white}
                    stroke={PUBLISH_C.white}
                    strokeWidth="1.2"
                    strokeLinejoin="round"
                  />
                </svg>
              </div>
            </div>
            <div
              className="absolute inset-0 rounded-full blur-xl -z-10 opacity-40"
              style={{
                backgroundColor: PUBLISH_C.teal,
                transform: "scale(1.3)",
              }}
            />
          </div>

          <h2
            className="text-[19px] font-bold leading-tight mb-3"
            style={{ color: PUBLISH_C.navy }}
          >
            ¿Quieres destacar tu vehículo?
          </h2>

          <div
            className="px-4 py-1.5 rounded-full mb-4 text-[12px] font-semibold"
            style={{
              backgroundColor: PUBLISH_C.tealLight,
              color: PUBLISH_C.teal,
            }}
          >
            {carName}
          </div>

          <p
            className="text-[13px] leading-[1.65] mb-3"
            style={{ color: PUBLISH_C.dark }}
          >
            Haz que tu publicación tenga mayor visibilidad y aparezca en una
            sección destacada de la plataforma.
          </p>
          <p
            className="text-[12px] leading-[1.65] mb-7"
            style={{ color: PUBLISH_C.muted }}
          >
            Antes de destacarla, un administrador deberá revisar y aprobar tu
            publicación. Una vez aprobada, tu vehículo podrá aparecer como
            publicación destacada.
          </p>

          <div
            className="w-full h-px mb-6"
            style={{ backgroundColor: PUBLISH_C.bg }}
          />

          <div className="w-full flex flex-col gap-3">
            <button
              onClick={onConfirm}
              className="w-full py-[15px] rounded-full font-bold text-[15px] transition-opacity active:opacity-80"
              style={{
                backgroundColor: PUBLISH_C.navy,
                color: PUBLISH_C.white,
              }}
            >
              Solicitar destacar
            </button>
            <button
              onClick={onCancel}
              className="w-full py-[13px] rounded-full font-semibold text-[14px] transition-opacity active:opacity-70"
              style={{ color: PUBLISH_C.muted }}
            >
              Cancelar
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

function SolicitudEnviada({
  carName,
  onClose,
}: {
  carName: string
  onClose: () => void
}) {
  return (
    <div
      className="absolute inset-0 flex items-end justify-center z-50 pb-6 px-4"
      style={{ backgroundColor: "rgba(13,43,69,0.52)" }}
      onClick={onClose}
    >
      <div
        className="w-full rounded-[28px] overflow-hidden"
        style={{
          backgroundColor: PUBLISH_C.white,
          boxShadow: "0 24px 60px rgba(13,43,69,0.22)",
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex justify-center pt-3 pb-1">
          <div
            className="w-10 h-1 rounded-full"
            style={{ backgroundColor: PUBLISH_C.mutedBorder }}
          />
        </div>
        <div className="px-6 pb-8 pt-3 flex flex-col items-center text-center">
          <div
            className="w-16 h-16 rounded-full flex items-center justify-center mb-4"
            style={{ backgroundColor: PUBLISH_C.tealLight }}
          >
            <div
              className="w-11 h-11 rounded-full flex items-center justify-center"
              style={{ backgroundColor: PUBLISH_C.teal }}
            >
              <Check size={18} color={PUBLISH_C.white} />
            </div>
          </div>
          <h2
            className="text-[18px] font-bold mb-2"
            style={{ color: PUBLISH_C.navy }}
          >
            Solicitud enviada
          </h2>
          <p
            className="text-[13px] leading-[1.65] mb-1"
            style={{ color: PUBLISH_C.dark }}
          >
            Tu solicitud para destacar{" "}
            <span className="font-semibold" style={{ color: PUBLISH_C.navy }}>
              {carName}
            </span>{" "}
            fue enviada correctamente.
          </p>
          <p className="text-[12px] mb-7" style={{ color: PUBLISH_C.muted }}>
            El equipo de Karsy la revisará pronto.
          </p>
          <button
            onClick={onClose}
            className="w-full py-[14px] rounded-full font-bold text-[14px]"
            style={{ backgroundColor: PUBLISH_C.navy, color: PUBLISH_C.white }}
          >
            Entendido
          </button>
        </div>
      </div>
    </div>
  )
}

function BottomNav({
  active,
  onChange,
}: {
  active: "publish" | "dashboard"
  onChange: (v: "publish" | "dashboard") => void
}) {
  return (
    <div
      className="flex justify-around items-center px-4 py-3"
      style={{
        backgroundColor: PUBLISH_C.white,
        borderTop: `1px solid ${PUBLISH_C.mutedBorder}`,
      }}
    >
      {[
        {
          id: "publish" as const,
          label: "Publicar",
          icon: (
            <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
              <rect
                x="3"
                y="3"
                width="16"
                height="16"
                rx="3"
                stroke="currentColor"
                strokeWidth="1.8"
              />
              <path
                d="M11 7v8M7 11h8"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
              />
            </svg>
          ),
        },
        {
          id: "dashboard" as const,
          label: "Panel",
          icon: (
            <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
              <rect
                x="3"
                y="12"
                width="5"
                height="7"
                rx="1.5"
                stroke="currentColor"
                strokeWidth="1.8"
              />
              <rect
                x="9"
                y="7"
                width="5"
                height="12"
                rx="1.5"
                stroke="currentColor"
                strokeWidth="1.8"
              />
              <rect
                x="15"
                y="3"
                width="4"
                height="16"
                rx="1.5"
                stroke="currentColor"
                strokeWidth="1.8"
              />
            </svg>
          ),
        },
      ].map((item) => (
        <button
          key={item.id}
          onClick={() => onChange(item.id)}
          className="flex flex-col items-center gap-1 transition-all"
          style={{
            color: active === item.id ? PUBLISH_C.navy : PUBLISH_C.muted,
          }}
        >
          {item.icon}
          <span className="text-[10px] font-semibold">{item.label}</span>
          {active === item.id && (
            <div
              className="w-1 h-1 rounded-full"
              style={{ backgroundColor: PUBLISH_C.navy }}
            />
          )}
        </button>
      ))}
    </div>
  )
}

export default function App() {
  const [view, setView] = useState<"mobile" | "web">("mobile")
  const [userMode, setUserMode] = useState<"visitor" | "user" | "admin">("user")

  if (view === "web") {
    return (
      <div style={{ minHeight: "100vh", background: "#F4F7F9" }}>
        <div
          style={{
            background: "#0D2B45",
            padding: "8px 24px",
            display: "flex",
            alignItems: "center",
            gap: 16,
            justifyContent: "space-between",
          }}
        >
          <div style={{ display: "flex", gap: 8 }}>
            <button
              onClick={() => setView("web")}
              style={{
                padding: "5px 14px",
                borderRadius: 8,
                background: view === "web" ? "#52A3AA" : "transparent",
                border: "1px solid rgba(255,255,255,0.2)",
                color: "#FFFFFF",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 12,
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              🌐 Web Home
            </button>
            <button
              onClick={() => setView("mobile")}
              style={{
                padding: "5px 14px",
                borderRadius: 8,
                background: "transparent",
                border: "1px solid rgba(255,255,255,0.2)",
                color: "rgba(255,255,255,0.65)",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 12,
                cursor: "pointer",
              }}
            >
              📱 Pantallas móviles
            </button>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 12,
                color: "rgba(255,255,255,0.6)",
              }}
            >
              Estado:
            </span>
            <button
              onClick={() => setUserMode("visitor")}
              style={{
                padding: "5px 14px",
                borderRadius: 8,
                background: userMode === "visitor" ? "#52A3AA" : "transparent",
                border: "1px solid rgba(255,255,255,0.2)",
                color: "#FFFFFF",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 12,
                cursor: "pointer",
              }}
            >
              Visitante
            </button>
            <button
              onClick={() => setUserMode("user")}
              style={{
                padding: "5px 14px",
                borderRadius: 8,
                background: userMode === "user" ? "#52A3AA" : "transparent",
                border: "1px solid rgba(255,255,255,0.2)",
                color: "#FFFFFF",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 12,
                cursor: "pointer",
              }}
            >
              Usuario
            </button>
            <button
              onClick={() => setUserMode("admin")}
              style={{
                padding: "5px 14px",
                borderRadius: 8,
                background: userMode === "admin" ? "#52A3AA" : "transparent",
                border: "1px solid rgba(255,255,255,0.2)",
                color: "#FFFFFF",
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 12,
                cursor: "pointer",
              }}
            >
              Admin
            </button>
          </div>
        </div>
        <WebHomeScreen userMode={userMode} />
      </div>
    )
  }

  return (
    <div
      style={{
        minHeight: "100%",
        background: "linear-gradient(135deg, #e8ecf0 0%, #d4dde6 100%)",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        padding: "24px",
        gap: 40,
      }}
    >
      <div
        style={{
          background: "#0D2B45",
          padding: "8px 16px",
          borderRadius: 12,
          display: "flex",
          gap: 8,
        }}
      >
        <button
          onClick={() => setView("web")}
          style={{
            padding: "5px 14px",
            borderRadius: 8,
            background: "transparent",
            border: "1px solid rgba(255,255,255,0.2)",
            color: "rgba(255,255,255,0.65)",
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 12,
            cursor: "pointer",
          }}
        >
          🌐 Web Home
        </button>
        <button
          onClick={() => setView("mobile")}
          style={{
            padding: "5px 14px",
            borderRadius: 8,
            background: "#52A3AA",
            border: "1px solid rgba(255,255,255,0.2)",
            color: "#FFFFFF",
            fontFamily: "'DM Sans', sans-serif",
            fontSize: 12,
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          📱 Pantallas móviles
        </button>
      </div>
      <div
        style={{
          display: "flex",
          gap: 40,
          flexWrap: "wrap",
          alignItems: "flex-start",
        }}
      >
        <div
          style={{
            display: "flex",
            gap: 40,
            flexWrap: "wrap",
            alignItems: "flex-start",
          }}
        >
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 16,
            }}
          >
            <span
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 11,
                fontWeight: 600,
                color: "#8E9A8E",
                letterSpacing: "0.1em",
                textTransform: "uppercase",
              }}
            >
              Pantalla 1 — Bienvenida
            </span>
            <WelcomeScreen />
          </div>

          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 16,
            }}
          >
            <span
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 11,
                fontWeight: 600,
                color: "#8E9A8E",
                letterSpacing: "0.1em",
                textTransform: "uppercase",
              }}
            >
              Pantalla 2 — Inicio de Sesión
            </span>
            <LoginScreen />
          </div>

          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 16,
            }}
          >
            <span
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 11,
                fontWeight: 600,
                color: "#8E9A8E",
                letterSpacing: "0.1em",
                textTransform: "uppercase",
              }}
            >
              01 — Tipo de Cliente
            </span>
            <RegisterTypeScreen />
          </div>

          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 16,
            }}
          >
            <span
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 11,
                fontWeight: 600,
                color: "#8E9A8E",
                letterSpacing: "0.1em",
                textTransform: "uppercase",
              }}
            >
              02 — Registro Particular
            </span>
            <RegisterParticularScreen />
          </div>

          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 16,
            }}
          >
            <span
              style={{
                fontFamily: "'DM Sans', sans-serif",
                fontSize: 11,
                fontWeight: 600,
                color: "#8E9A8E",
                letterSpacing: "0.1em",
                textTransform: "uppercase",
              }}
            >
              03 — Registro Lote
            </span>
            <RegisterLoteScreen />
          </div>
        </div>
      </div>
    </div>
  )
}

