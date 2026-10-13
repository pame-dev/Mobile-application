import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { favoriteIds, setFavorite } from '../data/api'
import { useUi } from '../components/Ui'
import { useSession } from './SessionContext'

interface FavoritesValue {
  ids: Set<number>
  isFavorite: (id: number) => boolean
  /** Guarda o quita; un visitante ve el aviso para registrarse. */
  toggle: (id: number) => void
}

const FavoritesContext = createContext<FavoritesValue | null>(null)

export function FavoritesProvider({ children }: { children: ReactNode }) {
  const { account, mode } = useSession()
  const { toast } = useUi()
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

  const toggle = useCallback(
    (id: number) => {
      if (!account) {
        toast('Regístrate para guardar tus favoritos', { label: 'Registrarme', onClick: () => navigate('/registro') })
        return
      }
      if (mode === 'admin') {
        toast('Los administradores no guardan favoritos')
        return
      }
      const agregar = !ids.has(id)
      // Cambio inmediato; si la BD falla se revierte (igual que la app).
      setIds((s) => {
        const n = new Set(s)
        if (agregar) n.add(id)
        else n.delete(id)
        return n
      })
      setFavorite(account.id, id, agregar).catch(() => {
        setIds((s) => {
          const n = new Set(s)
          if (agregar) n.delete(id)
          else n.add(id)
          return n
        })
        toast('No se pudo guardar el favorito. Intenta de nuevo.')
      })
    },
    [account, ids, mode, navigate, toast],
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
