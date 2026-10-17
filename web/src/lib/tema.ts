import { supabase } from './supabase'

export type Tema = 'light' | 'dark' | ''

export function temaGuardado(): Tema {
  try {
    const t = localStorage.getItem('karsy-theme')
    return t === 'light' || t === 'dark' ? t : ''
  } catch {
    return ''
  }
}

/** Claro, oscuro o el del sistema; se recuerda en este navegador. */
export function aplicarTema(t: Tema) {
  if (t) document.documentElement.setAttribute('data-theme', t)
  else document.documentElement.removeAttribute('data-theme')
  try {
    if (t) localStorage.setItem('karsy-theme', t)
    else localStorage.removeItem('karsy-theme')
  } catch {
    /* sin almacenamiento: solo dura esta visita */
  }
}

/** Hay un tema elegido aquí que todavía no se pudo guardar en la cuenta. */
const PENDIENTE = 'karsy-theme-pendiente'

function marcarPendiente(si: boolean) {
  try {
    if (si) localStorage.setItem(PENDIENTE, '1')
    else localStorage.removeItem(PENDIENTE)
  } catch {
    /* sin almacenamiento */
  }
}

function pendiente(): boolean {
  try {
    return localStorage.getItem(PENDIENTE) === '1'
  } catch {
    return false
  }
}

/**
 * Lo guarda también en la cuenta (cuentas.tema) para que sea el mismo en la app y en
 * otros navegadores. Si falla (sin internet), queda pendiente y se reintenta al volver a
 * entrar; mientras, el de la cuenta no reemplaza al elegido aquí.
 */
export async function guardarTemaEnCuenta(idCuenta: string, t: Tema): Promise<void> {
  const valor = t === 'light' ? 'claro' : t === 'dark' ? 'oscuro' : 'sistema'
  marcarPendiente(true)
  const { error } = await supabase.from('cuentas').update({ tema: valor }).eq('id_cuenta', idCuenta)
  if (!error) marcarPendiente(false)
}

/**
 * Al iniciar sesión: si la cuenta tiene un tema guardado, se usa ese (null = no tocar el de
 * este navegador). Si aquí hay uno pendiente de guardar, se reintenta en vez de reemplazarlo.
 */
export function aplicarTemaDeCuenta(valor: string | null | undefined, idCuenta: string) {
  if (pendiente()) {
    void guardarTemaEnCuenta(idCuenta, temaGuardado()).catch(() => {})
    return
  }
  if (valor === 'claro') aplicarTema('light')
  else if (valor === 'oscuro') aplicarTema('dark')
  else if (valor === 'sistema') aplicarTema('')
}
