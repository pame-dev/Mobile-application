import { useEffect, useState } from 'react'
import { catalogs } from '../data/api'
import { useSession } from '../session/SessionContext'

/**
 * Búsquedas recientes de este navegador (las últimas primero), de cada cuenta o del visitante:
 * en una computadora compartida, otra cuenta no ve las de esta. Se guardan al buscar; si no hay
 * almacenamiento (ventana privada), simplemente no hay recientes.
 */
const clave = (cuenta: string | null | undefined) => `karsy-busquedas-recientes-${cuenta ?? 'visitante'}`
const MAX = 6

export function busquedasRecientes(cuenta: string | null | undefined): string[] {
  try {
    const lista = JSON.parse(localStorage.getItem(clave(cuenta)) ?? '[]') as unknown
    return Array.isArray(lista) ? lista.filter((x): x is string => typeof x === 'string').slice(0, MAX) : []
  } catch {
    return []
  }
}

export function recordarBusqueda(q: string, cuenta: string | null | undefined) {
  const texto = q.trim()
  if (!texto) return
  try {
    const sinRepetir = busquedasRecientes(cuenta).filter((x) => x.toLowerCase() !== texto.toLowerCase())
    localStorage.setItem(clave(cuenta), JSON.stringify([texto, ...sinRepetir].slice(0, MAX)))
  } catch {
    /* sin almacenamiento: no se guarda */
  }
}

/**
 * Sugerencias del buscador (lista nativa del navegador): primero las búsquedas recientes y
 * luego las marcas del catálogo. El input la usa con list={id}.
 */
export function SugerenciasBusqueda({ id }: { id: string }) {
  const { account } = useSession()
  const [marcas, setMarcas] = useState<string[]>([])
  useEffect(() => {
    let vivo = true
    catalogs()
      .then((c) => vivo && setMarcas(c.marcas.filter((m) => !m.esOtro).map((m) => m.nombre)))
      .catch(() => undefined)
    return () => {
      vivo = false
    }
  }, [])
  const recientes = busquedasRecientes(account?.id)
  const opciones = [...recientes, ...marcas.filter((m) => !recientes.some((r) => r.toLowerCase() === m.toLowerCase()))]
  return (
    <datalist id={id}>
      {opciones.map((v) => (
        <option key={v} value={v} />
      ))}
    </datalist>
  )
}
