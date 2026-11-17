// Vista previa al compartir un auto (WhatsApp, Facebook, Telegram…).
//
// Esas apps no ejecutan JavaScript: leen las etiquetas og:* del HTML. La web es una SPA
// (index.html igual para todas las rutas), así que en /vehiculo/<id> esta Edge Function
// pide el auto a Supabase y cambia el bloque <!-- og --> … <!-- /og --> de index.html por
// su título, precio y foto. Las personas reciben la misma página de siempre.
//
// Usa las mismas variables que la web (VITE_SUPABASE_URL y VITE_SUPABASE_ANON_KEY,
// configuradas en Netlify). Solo lee autos visibles (v_anuncios con visible = true).
//
// Foto: WhatsApp solo muestra la tarjeta GRANDE si la imagen es horizontal; con una foto
// vertical (muy común desde el celular) pone una miniatura chica. No se recorta a propósito:
// render/image de Supabase se cobra arriba de la cuota del plan.

import type { Config, Context } from '@netlify/edge-functions'

interface Anuncio {
  titulo: string
  marca: string | null
  modelo: string | null
  anio: number | null
  precio: number | null
  moneda: string | null
  kilometraje: number | null
  municipio_ubicacion: string | null
  estado_ubicacion: string | null
  fotos: string[] | null
}

const escapar = (t: string) =>
  t.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')

const numero = (n: number) => new Intl.NumberFormat('es-MX', { maximumFractionDigits: 0 }).format(n)

/** Etiquetas del bloque <!-- og --> (las mismas para un auto y para la vista genérica). */
function etiquetas(e: { url: string; titulo: string; descripcion: string; imagen: string; alt: string; generica: boolean }) {
  return [
    `<meta name="description" content="${escapar(e.descripcion)}" />`,
    `<meta property="og:site_name" content="Karsy" />`,
    `<meta property="og:type" content="website" />`,
    `<meta property="og:url" content="${escapar(e.url)}" />`,
    `<meta property="og:title" content="${escapar(e.titulo)}" />`,
    `<meta property="og:description" content="${escapar(e.descripcion)}" />`,
    `<meta property="og:image" content="${escapar(e.imagen)}" />`,
    `<meta property="og:image:secure_url" content="${escapar(e.imagen)}" />`,
    // Tipo y medida solo en la imagen genérica (se saben exactos); los de la foto del auto dependen del original.
    ...(e.generica
      ? [`<meta property="og:image:type" content="image/jpeg" />`, `<meta property="og:image:width" content="1200" />`, `<meta property="og:image:height" content="630" />`]
      : []),
    `<meta property="og:image:alt" content="${escapar(e.alt)}" />`,
    `<meta name="twitter:card" content="summary_large_image" />`,
    `<link rel="canonical" href="${escapar(e.url)}" />`,
  ].join('\n    ')
}

export default async (req: Request, context: Context) => {
  const respuesta = await context.next()
  const origen = new URL(req.url).origin
  const id = Number(new URL(req.url).pathname.split('/')[2])
  const base = Netlify.env.get('VITE_SUPABASE_URL')
  const llave = Netlify.env.get('VITE_SUPABASE_ANON_KEY')
  if (!Number.isInteger(id) || id <= 0 || !base || !llave) return respuesta
  if (!(respuesta.headers.get('content-type') ?? '').includes('text/html')) return respuesta

  let auto: Anuncio | undefined
  let consultado = false
  try {
    const columnas = 'titulo,marca,modelo,anio,precio,moneda,kilometraje,municipio_ubicacion,estado_ubicacion,fotos'
    const r = await fetch(`${base}/rest/v1/v_anuncios?select=${columnas}&id_publicacion=eq.${id}&visible=eq.true`, {
      headers: { apikey: llave, Authorization: `Bearer ${llave}` },
    })
    if (r.ok) {
      auto = ((await r.json()) as Anuncio[])[0]
      consultado = true
    }
  } catch {
    // Sin datos: vista previa genérica de Karsy (y sin guardarla en caché).
  }

  // Enlace canónico: sin ?v=, filtros ni utm.
  const url = `${origen}/vehiculo/${id}`
  const generica = `${origen}/og-karsy.jpg`
  let bloque: string
  let tituloPagina: string | null = null
  if (auto) {
    const titulo = [auto.marca, auto.modelo, auto.anio].filter(Boolean).join(' ') || auto.titulo
    const descripcion = [
      auto.precio != null ? `$${numero(auto.precio)} ${auto.moneda ?? 'MXN'}` : null,
      auto.kilometraje != null ? `${numero(auto.kilometraje)} km` : null,
      [auto.municipio_ubicacion, auto.estado_ubicacion].filter(Boolean).join(', ') || null,
    ]
      .filter(Boolean)
      .join(' · ')
    // Foto principal (la primera de la galería), tal cual está en Storage (sin render/image: las
    // transformaciones de Supabase se cobran). La ruta viene de la BD: con "..", "?", "#" o "\" se
    // descarta y cada tramo va codificado. Una URL completa solo se usa si es del mismo Supabase.
    const ruta = auto.fotos?.[0] ?? ''
    const rutaValida = !!ruta && !/\.\.|[?#\\]/.test(ruta)
    const foto = !rutaValida
      ? null
      : /^https?:\/\//.test(ruta)
        ? ruta.startsWith(`${base}/`) ? ruta : null
        : `${base}/storage/v1/object/public/karsy/${ruta.split('/').map(encodeURIComponent).join('/')}`
    tituloPagina = `${titulo} | Karsy`
    bloque = etiquetas({ url, titulo: tituloPagina, descripcion, imagen: foto ?? generica, alt: titulo, generica: !foto })
  } else {
    // No existe o no está visible: los textos genéricos de index.html con la imagen genérica.
    bloque = etiquetas({
      url,
      titulo: 'Karsy — Tu próximo auto, a un toque de distancia',
      descripcion: 'Compra y vende autos seminuevos y usados en México: lotes y particulares.',
      imagen: generica,
      alt: 'Karsy',
      generica: true,
    })
  }

  // Con función de reemplazo: un "$" en el título (texto del usuario) no se lee como $&, $' …
  const pagina = tituloPagina
  let html = (await respuesta.text()).replace(/<!-- og -->[\s\S]*?<!-- \/og -->/, () => `<!-- og -->\n    ${bloque}\n    <!-- /og -->`)
  if (pagina) html = html.replace(/<title>[^<]*<\/title>/, () => `<title>${escapar(pagina)}</title>`)

  const headers = new Headers(respuesta.headers)
  headers.delete('content-length')
  // 5 min en el CDN de Netlify para no consultar Supabase en cada visita. Solo el CDN (se borra
  // en cada deploy): el navegador sigue pidiendo index.html nuevo y no se queda con assets viejos.
  if (consultado) headers.set('Netlify-CDN-Cache-Control', 'public, s-maxage=300, stale-while-revalidate=60')
  return new Response(html, { status: respuesta.status, headers })
}

export const config: Config = { path: '/vehiculo/*', cache: 'manual' }
