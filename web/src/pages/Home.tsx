import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { CarPhoto } from '../components/CarPhoto'
import { Icon } from '../components/Icon'
import { VehicleCard } from '../components/VehicleCard'
import { catalogs, heroStats, visibleCars } from '../data/api'
import { mensajeUsuario } from '../data/auth'
import { busquedaVacia, guardarBusqueda, type FiltrosBusqueda } from '../data/busquedas'
import type { Car } from '../data/types'
import { useI18n } from '../i18n/I18nContext'
import type { Clave } from '../i18n/es'
import { FILTER_KEYS, ORDEN, filtrarCars, filtrosDeUrl } from '../lib/filtros'
import { entero, km, precio } from '../lib/format'
import { useSession } from '../session/SessionContext'
import { useRecargar } from '../lib/tiempoReal'
import { ModalHead, useUi } from '../components/Ui'
import { miUbicacion } from '../data/account'
import { estadosMunicipios, mismoLugar, type EstadoMunicipios } from '../lib/mexico'

/** Cada cuánto avanzan solos los destacados (una tarjeta a la vez, como la app). */
const AUTOPLAY_MS = 3000
/** Duración del deslizamiento entre una tarjeta y la siguiente. */
const DESLIZ_MS = 700
/** Separación entre tarjetas del carrusel (px). */
const GAP_CARRUSEL = 20

export function Home() {
  const { mode, account } = useSession()
  const { toast, openModal } = useUi()
  const [guardando, setGuardando] = useState(false)
  const [buscandoLugar, setBuscandoLugar] = useState(false)
  const { t } = useI18n()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const [cars, setCars] = useState<Car[] | null>(null)
  const [error, setError] = useState<Clave | null>(null)
  const [stats, setStats] = useState<{ vehiculos: number; marcas: number; modelos: number } | null>(null)

  const cargar = useCallback(() => {
    setError(null)
    visibleCars()
      .then((c) => {
        setCars(c)
        heroStats(c).then(setStats).catch(() => setStats({ vehiculos: c.length, marcas: 0, modelos: 0 }))
      })
      .catch(() => setError('home.loadError'))
  }, [])
  useEffect(() => {
    cargar()
  }, [cargar])
  useRecargar(cargar, ['publicaciones', 'fotos_publicacion', 'solicitudes_destacado'])

  /** "Cerca de mí" (?estado=…&municipio=… en la URL, como los demás filtros). */
  const estadoCerca = params.get('estado') ?? ''
  const municipioCerca = params.get('municipio') ?? ''
  const lista = useMemo(() => {
    if (!cars) return []
    const filtrada = filtrarCars(cars, filtrosDeUrl(params), params.get('q') ?? '', params.get('orden') ?? '')
    if (!estadoCerca) return filtrada
    // Solo los del mismo estado y, primero, los del mismo municipio (sort es estable: respeta el orden elegido).
    return filtrada
      .filter((c) => mismoLugar(c.estado, estadoCerca))
      .sort((a, b) => Number(!mismoLugar(a.municipio, municipioCerca)) - Number(!mismoLugar(b.municipio, municipioCerca)))
  }, [cars, params, estadoCerca, municipioCerca])
  const destacados = useMemo(() => (cars ?? []).filter((c) => c.featured), [cars])
  const hayFiltros = params.has('q') || params.has('estado') || FILTER_KEYS.some((k) => params.has(k))

  const ponerLugar = (estado: string, municipio: string) => {
    const next = new URLSearchParams(params)
    next.set('estado', estado)
    if (municipio) next.set('municipio', municipio)
    else next.delete('municipio')
    setParams(next, { replace: true })
  }
  const quitarLugar = () => {
    const next = new URLSearchParams(params)
    next.delete('estado')
    next.delete('municipio')
    setParams(next, { replace: true })
  }
  /**
   * Como en la app: con sesión, el estado y municipio del perfil (sin pedir permiso de
   * ubicación); sin sesión o sin ubicación en el perfil, se eligen en una ventana.
   */
  const cercaDeMi = async () => {
    const elegir = () => openModal(<ElegirLugarModal onElegir={ponerLugar} />)
    if (!account) return elegir()
    setBuscandoLugar(true)
    const lugar = await miUbicacion(account.id).catch(() => null)
    setBuscandoLugar(false)
    if (lugar) ponerLugar(lugar.estado, lugar.municipio)
    else elegir()
  }

  /**
   * Guarda los filtros activos como búsqueda con alerta (el texto del buscador no se
   * guarda: la alerta usa solo los filtros). El tipo viene por nombre en la URL.
   */
  const filtrosActivos = filtrosDeUrl(params)
  const hayFiltrosGuardables = !!estadoCerca || FILTER_KEYS.some((k) => filtrosActivos[k])
  const avisarme = async () => {
    if (!account || guardando) return
    setGuardando(true)
    try {
      const f = filtrosActivos
      const cat = await catalogs()
      const num = (v: string) => (v && Number(v) ? Number(v) : null)
      const busqueda: FiltrosBusqueda = {
        id_marca: num(f.marca),
        id_modelo: num(f.modelo),
        anio: num(f.anio),
        id_carroceria: cat.carrocerias.find((c) => !c.esOtro && c.nombre === f.tipo)?.id ?? null,
        precio_min: num(f.min),
        precio_max: num(f.max),
        estado_ubicacion: estadoCerca || null,
      }
      if (busquedaVacia(busqueda)) return
      await guardarBusqueda(account.id, busqueda)
      toast(t('saved.saved'), { label: t('saved.view'), onClick: () => navigate('/busquedas') })
    } catch (e) {
      toast(mensajeUsuario(e))
    } finally {
      setGuardando(false)
    }
  }

  const setOrden = (v: string) => {
    const next = new URLSearchParams(params)
    if (v === 'recientes') next.delete('orden')
    else next.set('orden', v)
    setParams(next, { replace: true })
  }

  return (
    <>
      <section className="hero">
        <div className="wrap heroc">
          <span className="eyebrow">{t('home.eyebrow')}</span>
          <h1>
            {t('home.title1')}
            <br />
            {t('home.title2')}
          </h1>
          <p>{t('home.lead')}</p>
          <div className="stats">
            <div>
              <b className="num">{stats ? entero(stats.vehiculos) : '—'}</b>
              <span>{t('home.statVehicles')}</span>
            </div>
            <div>
              <b className="num">{stats ? entero(stats.marcas) : '—'}</b>
              <span>{t('home.statBrands')}</span>
            </div>
            <div>
              <b className="num">{stats ? entero(stats.modelos) : '—'}</b>
              <span>{t('home.statModels')}</span>
            </div>
          </div>
        </div>
      </section>

      <div className="wrap">
        {mode === 'guest' && (
          <div className="visitor">
            <div className="nic" style={{ background: 'var(--navy)', color: '#fff' }}>
              <Icon name="user" />
            </div>
            <div className="grow">
              <b style={{ color: 'var(--ink)', fontFamily: 'var(--f-display)', fontSize: 16 }}>{t('home.visitorTitle')}</b>
              <p className="muted">{t('home.visitorText')}</p>
            </div>
            <button className="btn btn-primary" onClick={() => navigate('/registro')}>
              {t('home.visitorCta')}
            </button>
          </div>
        )}

        {/* Mientras se busca o filtra, los destacados se ocultan para que los resultados queden arriba (igual que la app). */}
        {!hayFiltros && destacados.length > 0 && (
          <>
            <CarruselDestacados cars={destacados} onOpen={(id) => navigate(`/vehiculo/${id}`)} />
          </>
        )}

        <div className="sechead">
          <div>
            <h2>{hayFiltros ? t('home.results') : t('home.all')}</h2>
            <p className="muted">
              {cars ? t(lista.length === 1 ? 'home.countOne' : 'home.countMany', { n: entero(lista.length) }) : t('common.loading')}
            </p>
          </div>
          <div className="row" style={{ flexWrap: 'wrap' }}>
            {estadoCerca ? (
              <button className="chip" onClick={quitarLugar} style={{ background: 'var(--teal)', borderColor: 'var(--teal)', color: '#fff' }}>
                {t('home.nearMeActive', { lugar: [municipioCerca, estadoCerca].filter(Boolean).join(', ') })}
                <Icon name="x" size={14} />
              </button>
            ) : (
              <button className="chip" onClick={() => void cercaDeMi()} disabled={buscandoLugar}>
                {buscandoLugar ? t('home.nearMeSearching') : t('home.nearMe')}
              </button>
            )}
            {account && hayFiltrosGuardables && (
              <button className="chip" onClick={() => void avisarme()} disabled={guardando} style={{ color: 'var(--teal)', borderColor: 'var(--teal)' }}>
                <Icon name="bell" size={14} />
                {guardando ? t('saved.saving') : t('saved.save')}
              </button>
            )}
            {hayFiltros && (
              <button className="chip" onClick={() => setParams(new URLSearchParams(), { replace: true })}>
                <Icon name="x" size={14} />
                {t('home.clearFilters')}
              </button>
            )}
            <select
              className="input"
              aria-label={t('sort.label')}
              value={params.get('orden') ?? 'recientes'}
              onChange={(e) => setOrden(e.target.value)}
              style={{ height: 36, width: 'auto', borderRadius: 999 }}
            >
              {ORDEN.map(([v, clave]) => (
                <option key={v} value={v}>
                  {t(clave)}
                </option>
              ))}
            </select>
          </div>
        </div>

        {error ? (
          <div className="card empty">
            <p className="muted">{t(error)}</p>
            <button className="btn btn-ghost" onClick={cargar}>
              {t('common.retry')}
            </button>
          </div>
        ) : !cars ? (
          <div className="vgrid">
            {Array.from({ length: 8 }, (_, i) => (
              <div key={i} className="skeleton" style={{ height: 300 }} />
            ))}
          </div>
        ) : lista.length === 0 ? (
          <div className="card empty">
            <p className="muted">{cars.length === 0 ? t('home.empty') : t('home.noMatch')}</p>
            {cars.length > 0 && (
              <button className="btn btn-ghost" onClick={() => setParams(new URLSearchParams(), { replace: true })}>
                {t('home.clearFilters')}
              </button>
            )}
          </div>
        ) : (
          <div className="vgrid">
            {lista.map((c) => (
              <VehicleCard key={c.id} car={c} />
            ))}
          </div>
        )}
      </div>
    </>
  )
}

/** Ventana para elegir estado y municipio (visitantes o perfiles sin ubicación). */
function ElegirLugarModal({ onElegir }: { onElegir: (estado: string, municipio: string) => void }) {
  const { t } = useI18n()
  const { closeModal } = useUi()
  const [catalogo, setCatalogo] = useState<EstadoMunicipios[]>([])
  const [estado, setEstado] = useState('')
  const [municipio, setMunicipio] = useState('')
  useEffect(() => {
    estadosMunicipios().then(setCatalogo).catch(() => setCatalogo([]))
  }, [])
  const municipios = catalogo.find((e) => e.estado === estado)?.municipios ?? []

  return (
    <div className="modal">
      <ModalHead title={t('home.nearMePickTitle')} sub={t('home.nearMePickSub')} />
      <div className="grid2" style={{ marginBottom: 16 }}>
        <div className="field">
          <label htmlFor="cerca-estado">{t('perfil.state')}</label>
          <select className="input" id="cerca-estado" value={estado} onChange={(e) => (setEstado(e.target.value), setMunicipio(''))}>
            <option value="">{t('perfil.select')}</option>
            {catalogo.map((x) => (
              <option key={x.estado}>{x.estado}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="cerca-municipio">{t('perfil.city')}</label>
          <select className="input" id="cerca-municipio" value={municipio} disabled={!estado} onChange={(e) => setMunicipio(e.target.value)}>
            <option value="">{t('perfil.select')}</option>
            {municipios.map((m) => (
              <option key={m}>{m}</option>
            ))}
          </select>
        </div>
      </div>
      {/* El municipio es opcional: solo con el estado ya se filtra. */}
      <button
        className="btn btn-primary btn-block"
        disabled={!estado}
        onClick={() => {
          onElegir(estado, municipio)
          closeModal()
        }}
      >
        {t('home.nearMeApply')}
      </button>
    </div>
  )
}

/** Tarjetas visibles a la vez según el ancho: 3 en computadora, 2 en tableta, 1 en celular. */
function useTarjetasVisibles(): number {
  const calcular = () => (window.matchMedia('(max-width: 640px)').matches ? 1 : window.matchMedia('(max-width: 1000px)').matches ? 2 : 3)
  const [visibles, setVisibles] = useState(calcular)
  useEffect(() => {
    const onResize = () => setVisibles(calcular())
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [])
  return visibles
}

/**
 * Destacados en una sola fila que se desliza una tarjeta a la vez y da la vuelta sin fin
 * (como el carrusel de la app). Para el bucle, al principio y al final van copias de las
 * últimas y primeras tarjetas: al llegar a una copia se salta, sin animación, a la tarjeta
 * real equivalente, así que nunca se nota el regreso. Se pausa con el cursor o el foco.
 */
function CarruselDestacados({ cars, onOpen }: { cars: Car[]; onOpen: (id: number) => void }) {
  const { t } = useI18n()
  const visibles = useTarjetasVisibles()
  const n = cars.length
  /** Con pocas tarjetas (caben todas) no hay nada que deslizar. */
  const mueve = n > visibles
  const copias = mueve ? visibles : 0
  const pista = mueve ? [...cars.slice(n - copias), ...cars, ...cars.slice(0, copias)] : cars
  /** Posición en la pista; las reales van de [copias] a [copias + n - 1]. */
  const [pos, setPos] = useState(copias)
  const [animar, setAnimar] = useState(true)
  const [pausado, setPausado] = useState(false)

  // Si cambia el ancho o la cantidad de destacados, se vuelve a la primera sin animación.
  useEffect(() => {
    setAnimar(false)
    setPos(copias)
  }, [copias, n])

  // Tras un salto sin animación, se vuelve a animar en el siguiente cuadro.
  useEffect(() => {
    if (animar) return
    let id = requestAnimationFrame(() => {
      id = requestAnimationFrame(() => setAnimar(true))
    })
    return () => cancelAnimationFrame(id)
  }, [animar])

  // Al terminar de deslizarse hacia una copia, salta a la tarjeta real que se ve igual.
  useEffect(() => {
    if (!mueve || (pos >= copias && pos < copias + n)) return
    const id = window.setTimeout(
      () => {
        setAnimar(false)
        setPos((p) => (p >= copias + n ? p - n : p + n))
      },
      animar ? DESLIZ_MS : 0,
    )
    return () => window.clearTimeout(id)
  }, [pos, mueve, copias, n, animar])

  // Avanza sola; cada movimiento (también con las flechas) reinicia la cuenta.
  useEffect(() => {
    if (!mueve || pausado) return
    const id = window.setTimeout(() => setPos((p) => Math.min(p + 1, copias + n)), AUTOPLAY_MS)
    return () => window.clearTimeout(id)
  }, [pos, mueve, pausado, copias, n])

  const pausa = {
    onMouseEnter: () => setPausado(true),
    onMouseLeave: () => setPausado(false),
    onFocus: () => setPausado(true),
    onBlur: () => setPausado(false),
  }
  // Las flechas no pasan de las copias (si se pica muy rápido, esperan al salto).
  const anterior = () => setPos((p) => Math.max(p - 1, copias - 1))
  const siguiente = () => setPos((p) => Math.min(p + 1, copias + n))

  return (
    <>
      <div className="sechead">
        <div>
          <h2>{t('home.featured')}</h2>
          <p className="muted">{t('home.featuredSub')}</p>
        </div>
        {mueve && (
          <div className="row" {...pausa}>
            <button className="iconbtn" aria-label={t('home.prev')} onClick={anterior}>
              <Icon name="back" size={18} />
            </button>
            <button className="iconbtn" aria-label={t('home.next')} onClick={siguiente}>
              <Icon name="chev" size={18} />
            </button>
          </div>
        )}
      </div>
      <div className="featviewport" {...pausa}>
        <div
          className={`feattrack ${animar ? 'anim' : ''}`}
          style={{
            gap: GAP_CARRUSEL,
            ['--visibles' as string]: visibles,
            ['--gap' as string]: `${GAP_CARRUSEL}px`,
            // Cada paso mueve una tarjeta: (ancho de la fila + separación) / tarjetas visibles.
            transform: `translateX(calc(${-pos} * (100% + ${GAP_CARRUSEL}px) / ${visibles}))`,
          }}
        >
          {pista.map((c, i) => {
            const copia = mueve && (i < copias || i >= copias + n)
            return (
              <button
                key={`${i}-${c.id}`}
                className="fcard"
                onClick={() => onOpen(c.id)}
                // Las copias solo son para el bucle: no las lee el lector de pantalla ni se enfocan.
                aria-hidden={copia || undefined}
                tabIndex={copia ? -1 : undefined}
              >
                <CarPhoto src={c.imageUrl} alt={`${c.brand} ${c.model}`} />
                <div className="shade" />
                <span className="badge b-star" style={{ position: 'absolute', left: 14, top: 14 }}>
                  {t('home.featuredBadge')}
                </span>
                <div className="info">
                  <div>
                    <b>
                      {c.brand} {c.model}
                    </b>
                    <span>
                      {c.year} · {km(c.km)}
                      {c.city ? ` · ${c.city}` : ''}
                    </span>
                  </div>
                  <div className="pr num">{precio(c.priceValue)}</div>
                </div>
              </button>
            )
          })}
        </div>
      </div>
    </>
  )
}
