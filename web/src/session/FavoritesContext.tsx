import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { favoriteIds, setFavorite } from '../data/api'
import { useUi } from '../components/Ui'
import { useI18n } from '../i18n/I18nContext'
import { useSession } from './SessionContext'
import { useRecargar } from '../lib/tiempoReal'

interface FavoritesValue {
  ids: Set<number>
  isFavorite: (id: number) => boolean
  /** Guarda o quita; un visitante ve el aviso para registrarse. */
  toggle: (id: number) => void
}

const FavoritesContext = createContext<FavoritesValue | null>(null)

export function FavoritesProvider({ children }: { children: ReactNode }) {
  const { account } = useSession()
  const { toast } = useUi()
  const { t } = useI18n()
  const navigate = useNavigate()
  const [ids, setIds] = useState<Set<number>>(new Set())

  useEffect(() => {
    let vivo = true
    favoriteIds(account?.id ?? null)
      .then((s) => vivo && setIds(s))
      .catch(() => vivo && setIds(new Set()))
    return () => {
      vivo = false
    }
  }, [account?.id])
  // Favoritos guardados o quitados en la app con la misma cuenta.
  useRecargar(() => {
    if (account) void favoriteIds(account.id).then(setIds).catch(() => undefined)
  }, ['favoritos'])

  /** Guarda o quita [id]. Al quitarlo se ofrece "Deshacer" (lo vuelve a guardar). */
  const cambiar = useCallback(
    (userId: string, id: number, agregar: boolean) => {
      // Cambio inmediato; si la BD falla se revierte (igual que la app).
      setIds((s) => {
        const n = new Set(s)
        if (agregar) n.add(id)
        else n.delete(id)
        return n
      })
      setFavorite(userId, id, agregar)
        .then(() => {
          if (!agregar) toast(t('vehiculo.fav.removed'), { label: t('common.undo'), onClick: () => cambiarRef.current(userId, id, true) })
        })
        .catch(() => {
          setIds((s) => {
            const n = new Set(s)
            if (agregar) n.delete(id)
            else n.add(id)
            return n
          })
          toast(t('vehiculo.fav.error'))
        })
    },
    [toast, t],
  )
  // "Deshacer" llama a la versión más reciente de cambiar.
  const cambiarRef = useRef(cambiar)
  cambiarRef.current = cambiar

  const toggle = useCallback(
    (id: number) => {
      if (!account) {
        toast(t('nav.favoritesGuest'), { label: t('nav.registerMe'), onClick: () => navigate('/registro') })
        return
      }
      cambiar(account.id, id, !ids.has(id))
    },
    [account, ids, navigate, toast, t, cambiar],
  )

  return (
    <FavoritesContext.Provider value={{ ids, isFavorite: (id) => ids.has(id), toggle }}>{children}</FavoritesContext.Provider>
  )
}

export function useFavorites(): FavoritesValue {
  const ctx = useContext(FavoritesContext)
  if (!ctx) throw new Error('useFavorites debe usarse dentro de <FavoritesProvider>')
  return ctx
}
