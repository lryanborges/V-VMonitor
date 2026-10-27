import type { ReactNode } from 'react'
import type { Element, ElementKind } from '../../api/types'
import { KINDS } from '../../utils/labels'

export type KindFilter = 'all' | ElementKind

interface KindTabsProps {
  /** null: nenhuma aba de tipo selecionada (ex.: aba Testes aberta). */
  value: KindFilter | null
  onChange: (value: KindFilter) => void
  /** Elementos usados nas contagens de cada aba. */
  elements: Element[]
  label?: string
  /** Abas extras no mesmo grupo (ex.: Testes, na lista). */
  children?: ReactNode
}

/** Abas Todos / Funcionais / Nao funcionais / Regras, com a contagem de cada tipo. */
export function KindTabs({ value, onChange, elements, label = 'Filtrar por tipo', children }: KindTabsProps) {
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
      {children}
    </div>
  )
}
