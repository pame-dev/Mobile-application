import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { traducir } from '../i18n/I18nContext'
import { cerrarSesionLocal, supabase } from '../lib/supabase'
import type { Account } from '../data/types'
import { aplicarTemaDeCuenta } from '../lib/tema'

/** Visitante (sin sesión), usuario (particular o lote) o administrador. */
export type Mode = 'guest' | 'user' | 'admin'

interface SessionValue {
  account: Account | null
  mode: Mode
  /** true mientras se restaura la sesión guardada al abrir la web. */
  loading: boolean
  /** Mensaje si la sesión se cerró sola (cuenta suspendida o sin cuenta de Karsy). */
  notice: string | null
  clearNotice: () => void
  /** Vuelve a leer la cuenta; devuelve la cuenta (o null) y el aviso si se cerró la sesión. */
  reload: () => Promise<{ account: Account | null; notice: string | null }>
  signOut: () => Promise<void>
}

const SessionContext = createContext<SessionValue | null>(null)

/**
 * Durante "¿Olvidaste tu contraseña?" el código abre una sesión temporal que solo
 * sirve para cambiar la contraseña: mientras tanto la web sigue como visitante.
 */
let recoveryMode = false
export function setRecoveryMode(on: boolean) {
  recoveryMode = on
}

/**
 * Lee la cuenta de public.cuentas y el rol con es_administrador(), igual que
 * AuthRepository.loadAccountOrSignOut en la app: sin cuenta o suspendida → se cierra la sesión.
 */
async function loadAccount(): Promise<{ account: Account | null; notice: string | null }> {
  const { data: userData } = await supabase.auth.getUser()
  const user = userData.user
  if (!user) return { account: null, notice: null }

  const { data: cuenta } = await supabase
    .from('cuentas')
    .select('id_cuenta,nombre_mostrar,tipo_cuenta,estado_cuenta,foto_perfil')
    .eq('id_cuenta', user.id)
    .maybeSingle()

  if (!cuenta) {
    await cerrarSesionLocal()
    return { account: null, notice: traducir('vehiculo.session.noProfile') }
  }
  if (cuenta.estado_cuenta === 'suspendida') {
    await cerrarSesionLocal()
    return { account: null, notice: traducir('vehiculo.session.suspended') }
  }
  const { data: esAdmin } = await supabase.rpc('es_administrador', { p_id_cuenta: user.id })
  // El tema elegido en otro dispositivo, aparte: si falla (o aún no existe la columna), no
  // impide iniciar sesión.
  void supabase
    .from('cuentas')
    .select('tema')
    .eq('id_cuenta', user.id)
    .maybeSingle()
    .then(
      ({ data }) => aplicarTemaDeCuenta(data?.tema as string | null | undefined, user.id),
      () => {},
    )
  return {
    account: {
      id: cuenta.id_cuenta,
      nombre: cuenta.nombre_mostrar,
      tipoCuenta: cuenta.tipo_cuenta,
      esAdmin: esAdmin === true,
      correo: user.email ?? '',
      fotoPerfil: cuenta.foto_perfil,
    },
    notice: null,
  }
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [account, setAccount] = useState<Account | null>(null)
  const [loading, setLoading] = useState(true)
  const [notice, setNotice] = useState<string | null>(null)

  const reload = useCallback(async () => {
    if (recoveryMode) return { account: null, notice: null }
    const r = await loadAccount()
    setAccount(r.account)
    if (r.notice) setNotice(r.notice)
    return r
  }, [])

  useEffect(() => {
    reload().finally(() => setLoading(false))
    // Inicio/cierre de sesión (también en otra pestaña) → se vuelve a leer la cuenta.
    const { data } = supabase.auth.onAuthStateChange((event) => {
      if (event === 'SIGNED_IN' || event === 'SIGNED_OUT' || event === 'USER_UPDATED') {
        // Fuera del callback: Supabase no permite llamar a la API dentro de él.
        setTimeout(() => void reload(), 0)
      }
    })
    return () => data.subscription.unsubscribe()
  }, [reload])

  const signOut = useCallback(async () => {
    await cerrarSesionLocal()
    setAccount(null)
  }, [])

  const value = useMemo<SessionValue>(
    () => ({
      account,
      mode: !account ? 'guest' : account.esAdmin ? 'admin' : 'user',
      loading,
      notice,
      clearNotice: () => setNotice(null),
      reload,
      signOut,
    }),
    [account, loading, notice, reload, signOut],
  )
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}

export function useSession(): SessionValue {
  const ctx = useContext(SessionContext)
  if (!ctx) throw new Error('useSession debe usarse dentro de <SessionProvider>')
  return ctx
}
