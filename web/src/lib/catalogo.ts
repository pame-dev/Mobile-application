import { idiomaActual } from '../i18n/I18nContext'

// Catálogos de la BD (colores, transmisiones, carrocerías, combustibles): se guardan en español,
// la app de Android los lee así, y aquí solo se traducen para mostrarlos. Si se agrega un valor
// nuevo al catálogo, hay que agregar su traducción aquí; mientras tanto se muestra tal cual.
const EN: Record<string, string> = {
  // Colores
  Blanco: 'White',
  Negro: 'Black',
  Gris: 'Gray',
  Plata: 'Silver',
  Rojo: 'Red',
  Azul: 'Blue',
  Verde: 'Green',
  Beige: 'Beige',
  Café: 'Brown',
  Amarillo: 'Yellow',
  Naranja: 'Orange',
  Vino: 'Burgundy',
  // Transmisiones
  Manual: 'Manual',
  Automática: 'Automatic',
  CVT: 'CVT',
  'Doble embrague': 'Dual-clutch',
  // Carrocerías
  Sedán: 'Sedan',
  Hatchback: 'Hatchback',
  SUV: 'SUV',
  Pickup: 'Pickup',
  Coupé: 'Coupe',
  Minivan: 'Minivan',
  Convertible: 'Convertible',
  Van: 'Van',
  // Combustibles
  Gasolina: 'Gasoline',
  Diésel: 'Diesel',
  Híbrido: 'Hybrid',
  Eléctrico: 'Electric',
  // Opción "otro" de cada catálogo
  Otros: 'Other',
}

/** Valor de catálogo en el idioma de la web (marcas, modelos y textos escritos a mano se muestran igual). */
export const nombreCatalogo = (valor: string): string => (idiomaActual() === 'en' && EN[valor.trim()]) || valor
