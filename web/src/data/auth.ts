import { AuthError } from '@supabase/supabase-js'
import { BUCKET, supabase } from '../lib/supabase'
import { traducir } from '../i18n/I18nContext'
import { UserError } from './api'

// Inicio de sesión, registro y códigos por correo: lo mismo que AuthRepository.kt.
// Con "Confirm email" activo, las cuentas nuevas no tienen sesión hasta escribir el
// código de 6 dígitos que manda Supabase (plantillas en supabase/templates).

/** Debe coincidir con "Email OTP Length" en Supabase. */
export const EMAIL_CODE_LENGTH = 6

/** La cuenta existe pero falta verificar el correo: hay que pedir el código. */
export class EmailNotVerifiedError extends Error {
  constructor(public email: string) {
    super(traducir('auth.err.verifyEmail'))
  }
}

/**
 * "Ya existe una cuenta con ese correo." en el idioma actual. Es una función (no una constante)
 * para que el registro compare contra el texto en el mismo idioma con el que se armó el error.
 */
export const msgEmailExiste = () => traducir('auth.err.emailExists')

/** Mensaje en el idioma actual para cualquier error de Supabase o de red (mensajeUsuario() de la app). */
export function mensajeUsuario(e: unknown): string {
  if (e instanceof UserError || e instanceof EmailNotVerifiedError) return e.message
  if (e instanceof AuthError) {
    switch (e.code) {
      case 'invalid_credentials':
      case 'invalid_grant':
        return traducir('auth.err.invalidCredentials')
      case 'user_already_exists':
      case 'email_exists':
        return msgEmailExiste()
      case 'weak_password':
        return traducir('auth.err.weakPassword')
      case 'email_not_confirmed':
        return traducir('auth.err.emailNotConfirmed')
      case 'otp_expired':
        return traducir('auth.err.otpExpired')
      case 'same_password':
        return traducir('auth.err.samePassword')
      case 'over_email_send_rate_limit':
      case 'over_request_rate_limit':
        return traducir('auth.err.rateLimit')
      case 'validation_failed':
      case 'email_address_invalid':
        return traducir('auth.err.invalidEmail')
      case 'email_address_not_authorized':
        return traducir('auth.err.emailNotAuthorized')
    }
    if (e.status === 0 || e.name === 'AuthRetryableFetchError') return traducir('auth.err.offline')
  }
  if (e instanceof TypeError) return traducir('auth.err.offline')
  console.warn('Karsy: error sin mensaje para el usuario', e)
  return traducir('auth.err.generic')
}

/**
 * Lanza EmailNotVerifiedError si la contraseña es correcta pero falta verificar el correo.
 * La cuenta (rol, suspendida…) la lee SessionContext al detectar la sesión nueva.
 */
export async function signIn(email: string, password: string): Promise<void> {
  const { error } = await supabase.auth.signInWithPassword({ email: email.trim(), password })
  if (error) {
    if (error.code === 'email_not_confirmed') throw new EmailNotVerifiedError(email.trim())
    throw error
  }
}

/**
 * Crea el usuario. El trigger trg_auth_crear_cuenta crea la fila de public.cuentas
 * (y perfiles_lote / telefonos_contacto) con [data].
 * Lanza EmailNotVerifiedError si hay que verificar el correo (el código ya se envió).
 */
export async function signUp(email: string, password: string, data: Record<string, string | null>): Promise<void> {
  const { data: r, error } = await supabase.auth.signUp({ email: email.trim(), password, options: { data } })
  if (error) throw error
  if (!r.session) {
    // Si el correo ya tiene cuenta verificada, Supabase responde un usuario sin
    // identidades y no manda nada (para no revelar qué correos existen).
    if (r.user && r.user.identities?.length === 0) throw new UserError(msgEmailExiste())
    throw new EmailNotVerifiedError(email.trim())
  }
}

/** Genera un código nuevo y lo manda al correo (el anterior deja de servir). */
export async function sendVerificationCode(email: string): Promise<void> {
  const { error } = await supabase.auth.resend({ type: 'signup', email: email.trim() })
  if (error) throw error
}

/** Valida el código del correo; si es correcto Supabase abre la sesión. */
export async function verifyEmail(email: string, code: string): Promise<void> {
  const { error } = await supabase.auth.verifyOtp({ type: 'signup', email: email.trim(), token: code })
  if (error) throw error
}

// ── Olvidé mi contraseña ─────────────────────────────────────────
// 1. Código al correo (si el correo no tiene cuenta Supabase responde igual).
// 2. El código abre una sesión temporal. 3. Con ella se cambia la contraseña.

export async function sendPasswordResetCode(email: string): Promise<void> {
  const { error } = await supabase.auth.resetPasswordForEmail(email.trim())
  if (error) throw error
}

export async function verifyPasswordResetCode(email: string, code: string): Promise<void> {
  const { error } = await supabase.auth.verifyOtp({ type: 'recovery', email: email.trim(), token: code })
  if (error) throw error
}

/** Guarda la contraseña nueva y cierra la sesión temporal para entrar con ella. */
export async function resetPassword(newPassword: string): Promise<void> {
  const { error } = await supabase.auth.updateUser({ password: newPassword })
  if (error) throw error
  await supabase.auth.signOut()
}

/** true si el número ya está registrado en otra cuenta (función telefono_en_uso). */
export async function telefonoEnUso(numero: string): Promise<boolean> {
  const { data, error } = await supabase.rpc('telefono_en_uso', { p_numero: numero })
  if (error) throw error
  return data === true
}

// ── Logo / foto de perfil del registro ──
// Sin sesión (falta verificar el correo) no se puede subir: se guarda en memoria
// hasta que se valide el código, igual que holdAvatarUntilVerified en la app.

let pendingAvatar: { email: string; blob: Blob } | null = null

export function holdAvatarUntilVerified(email: string, blob: Blob) {
  pendingAvatar = { email: email.trim().toLowerCase(), blob }
}

export async function uploadPendingAvatar(email: string): Promise<void> {
  const p = pendingAvatar
  pendingAvatar = null
  if (p && p.email === email.trim().toLowerCase()) await uploadAvatar(p.blob)
}

/** Sube la foto de perfil (o logo del lote) y la guarda en la cuenta. */
export async function uploadAvatar(blob: Blob): Promise<void> {
  const { data } = await supabase.auth.getUser()
  const uid = data.user?.id
  if (!uid) throw new UserError(traducir('auth.err.signInFirst'))
  const ruta = `perfiles/${uid}/${crypto.randomUUID()}.jpg`
  const up = await supabase.storage.from(BUCKET).upload(ruta, blob, { upsert: false, contentType: 'image/jpeg' })
  if (up.error) throw up.error
  const { error } = await supabase.from('cuentas').update({ foto_perfil: ruta }).eq('id_cuenta', uid)
  if (error) throw error
}
