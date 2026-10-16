import type { KeyboardEvent, ReactNode } from 'react'
import type { Element } from '../../api/types'
import { TickIcon } from '../../components/icons'
import { kindInfo, priorityInfo } from '../../utils/labels'
import './ElementTable.css'

interface ElementTableProps {
  elements: Element[]
  selectedId: string | null
  onSelect: (elementId: string) => void
  /** Coluna de testes; as versoes nao guardam testes, entao la ela some. */
  showTests?: boolean
  label?: string
  /** Estados de carregamento e vazio, exibidos acima das linhas. */
  children?: ReactNode
}

/** Tabela de elementos (ID, descricao, prioridade, vinculos e testes) da lista do modelo e das versoes. */
export function ElementTable({
  elements,
  selectedId,
  onSelect,
  showTests = true,
  label = 'Elementos do modelo',
  children,
}: ElementTableProps) {
  return (
    <div className={`card md-table${showTests ? '' : ' no-tests'}`} role="table" aria-label={label}>
      <div className="row hd" role="row">
        <span role="columnheader">ID</span>
        <span role="columnheader">Descrição</span>
        <span role="columnheader">Prioridade</span>
        <span role="columnheader">Vínculos</span>
        {showTests && <span role="columnheader">Testes</span>}
      </div>
      <div className="md-rows">
        {children}
        {elements.map((e) => (
          <ElementRow
            key={e.id}
            element={e}
            selected={e.id === selectedId}
            showTests={showTests}
            onSelect={() => onSelect(e.id)}
          />
        ))}
      </div>
    </div>
  )
}

interface ElementRowProps {
  element: Element
  selected: boolean
  showTests: boolean
  onSelect: () => void
}

function ElementRow({ element, selected, showTests, onSelect }: ElementRowProps) {
  const kind = kindInfo(element.kind)
  const prio = element.priority ? priorityInfo(element.priority) : null
  const onKey = (e: KeyboardEvent) => (e.key === 'Enter' || e.key === ' ') && (e.preventDefault(), onSelect())

  return (
    <div
      className={selected ? 'row sel' : 'row'}
      role="row"
      tabIndex={0}
      aria-selected={selected}
      onClick={onSelect}
      onKeyDown={onKey}
    >
      <span role="cell"><span className={`chip ${kind.chip}`}>{element.code}</span></span>
      <span role="cell" className="desc" title={element.description}>{element.description}</span>
      <span role="cell">
        {prio ? (
          <span className={`prio p${prio.level}`}><b><i /><i /><i /></b>{prio.label}</span>
        ) : (
          <span className="hint">—</span>
        )}
      </span>
      <span role="cell" className="mono md-links">{element.links}</span>
      {showTests && (
        <span role="cell">
          {element.kind === 'BUSINESS_RULE' ? (
            <span className="hint">—</span>
          ) : element.tests > 0 ? (
            <span className="pill ok"><TickIcon />{element.tests === 1 ? '1 teste' : `${element.tests} testes`}</span>
          ) : (
            <span className="pill w">Sem teste</span>
          )}
        </span>
      )}
    </div>
  )
}
