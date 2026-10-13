import { useNavigate } from 'react-router-dom'
import { Icon } from '../components/Icon'

/** Mensaje centrado con botón al inicio (pantallas pendientes, 404). */
export function Placeholder({ title, message = 'Esta pantalla llega en la siguiente fase de la versión web.' }: { title: string; message?: string }) {
  const navigate = useNavigate()
  return (
    <div className="wrap" style={{ maxWidth: 820 }}>
      <div className="card empty" style={{ marginTop: 40 }}>
        <span className="nic" style={{ width: 56, height: 56, borderRadius: 16, background: 'var(--teal-light)', color: 'var(--teal)' }}>
          <Icon name="car" size={26} />
        </span>
        <h2>{title}</h2>
        <p className="muted">{message}</p>
        <button className="btn btn-primary" onClick={() => navigate('/')}>
          Ir al inicio
        </button>
      </div>
    </div>
  )
}

export function NotFound() {
  return <Placeholder title="No encontramos esta página" message="Puede que el enlace esté mal escrito o que la página ya no exista." />
}
