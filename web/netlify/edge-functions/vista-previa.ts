// Vista previa al compartir un auto (WhatsApp, Facebook, Telegram…).
//
// Esas apps no ejecutan JavaScript: leen las etiquetas og:* del HTML. La web es una SPA
// (index.html igual para todas las rutas), así que en /vehiculo/<id> esta Edge Function
// pide el auto a Supabase y cambia el bloque <!-- og --> … <!-- /og --> de index.html por
// su título, precio y foto. Las personas reciben la misma página de siempre.
//
// Usa las mismas variables que la web (VITE_SUPABASE_URL y VITE_SUPABASE_ANON_KEY,
// configuradas en Netlify). Solo lee autos visibles (v_anuncios con visible = true).

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

export default async (req: Request, context: Context) => {
  const respuesta = await context.next()
  const id = Number(new URL(req.url).pathname.split('/')[2])
  const base = Netlify.env.get('VITE_SUPABASE_URL')
  const llave = Netlify.env.get('VITE_SUPABASE_ANON_KEY')
  if (!Number.isInteger(id) || id <= 0 || !base || !llave) return respuesta
  if (!(respuesta.headers.get('content-type') ?? '').includes('text/html')) return respuesta

  let auto: Anuncio | undefined
  try {
    const columnas = 'titulo,marca,modelo,anio,precio,moneda,kilometraje,municipio_ubicacion,estado_ubicacion,fotos'
    const r = await fetch(`${base}/rest/v1/v_anuncios?select=${columnas}&id_publicacion=eq.${id}&visible=eq.true`, {
      headers: { apikey: llave, Authorization: `Bearer ${llave}` },
    })
    if (r.ok) auto = ((await r.json()) as Anuncio[])[0]
  } catch {
    // Sin datos: se queda la vista previa genérica de Karsy.
  }
  if (!auto) return respuesta

  const titulo = [auto.marca, auto.modelo, auto.anio].filter(Boolean).join(' ') || auto.titulo
  const descripcion = [
    auto.precio != null ? `$${numero(auto.precio)} ${auto.moneda ?? 'MXN'}` : null,
    auto.kilometraje != null ? `${numero(auto.kilometraje)} km` : null,
    [auto.municipio_ubicacion, auto.estado_ubicacion].filter(Boolean).join(', ') || null,
  ]
    .filter(Boolean)
    .join(' · ')
  const ruta = auto.fotos?.[0]
  const foto = !ruta ? `${new URL(req.url).origin}/logo.png` : ruta.startsWith('http') ? ruta : `${base}/storage/v1/object/public/karsy/${ruta}`

  const etiquetas = [
    `<meta name="description" content="${escapar(descripcion)}" />`,
    `<meta property="og:site_name" content="Karsy" />`,
    `<meta property="og:type" content="website" />`,
    `<meta property="og:url" content="${escapar(req.url)}" />`,
    `<meta property="og:title" content="${escapar(`${titulo} | Karsy`)}" />`,
    `<meta property="og:description" content="${escapar(descripcion)}" />`,
    `<meta property="og:image" content="${escapar(foto)}" />`,
    `<meta name="twitter:card" content="summary_large_image" />`,
  ].join('\n    ')

  const html = (await respuesta.text())
    .replace(/<!-- og -->[\s\S]*?<!-- \/og -->/, `<!-- og -->\n    ${etiquetas}\n    <!-- /og -->`)
    .replace(/<title>[^<]*<\/title>/, `<title>${escapar(`${titulo} | Karsy`)}</title>`)

  const headers = new Headers(respuesta.headers)
  headers.delete('content-length')
  return new Response(html, { status: respuesta.status, headers })
}

export const config: Config = { path: '/vehiculo/*' }
