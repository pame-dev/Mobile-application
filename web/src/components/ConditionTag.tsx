import { useI18n } from '../i18n/I18nContext'

/**
 * Etiqueta de condición ("Seminuevo" / "Usado") junto al nombre del vehículo.
 * [compact] = versión chica de las tarjetas de las listas. Sin condición no muestra nada.
 */
export function ConditionTag({ condicion, compact = false }: { condicion: string | null | undefined; compact?: boolean }) {
  const { t } = useI18n()
  if (condicion !== 'seminuevo' && condicion !== 'usado') return null
  return (
    <span className={`condition-tag ${condicion} ${compact ? 'compact' : ''}`}>
      {t(condicion === 'seminuevo' ? 'vehiculo.conditionSeminuevo' : 'vehiculo.conditionUsado')}
    </span>
  )
}
