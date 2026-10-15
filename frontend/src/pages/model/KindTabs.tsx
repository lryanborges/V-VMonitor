import type { Element, ElementKind } from '../../api/types'
import { KINDS } from '../../utils/labels'

export type KindFilter = 'all' | ElementKind

interface KindTabsProps {
  value: KindFilter
  onChange: (value: KindFilter) => void
  /** Elementos usados nas contagens de cada aba. */
  elements: Element[]
  label?: string
}

/** Abas Todos / Funcionais / Nao funcionais / Regras, com a contagem de cada tipo. */
export function KindTabs({ value, onChange, elements, label = 'Filtrar por tipo' }: KindTabsProps) {
  return (
    <div className="seg" role="group" aria-label={label}>
      <button type="button" aria-pressed={value === 'all'} onClick={() => onChange('all')}>
        Todos <span className="n">{elements.length}</span>
      </button>
      {KINDS.map((k) => (
        <button key={k.kind} type="button" aria-pressed={value === k.kind} onClick={() => onChange(k.kind)}>
          {k.plural} <span className="n">{elements.filter((e) => e.kind === k.kind).length}</span>
        </button>
      ))}
    </div>
  )
}
