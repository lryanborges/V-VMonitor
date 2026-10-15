import { Handle, Position, type Node, type NodeProps } from '@xyflow/react'
import type { Element } from '../../../api/types'
import { kindInfo } from '../../../utils/labels'

export type ElementNodeData = {
  element: Element
  selected: boolean
  /** Mostra a bolinha de cobertura (com teste / sem teste). */
  showCoverage: boolean
}

export type ElementNodeType = Node<ElementNodeData, 'element'>

/**
 * No do grafo: pilula com o codigo, colorida pelo tipo e com a bolinha de cobertura de testes (RF13).
 * Elemento em rascunho tem borda tracejada. As conexoes saem pela esquerda e chegam pela direita (layout RL).
 */
export function ElementNode({ data }: NodeProps<ElementNodeType>) {
  const { element, selected, showCoverage } = data
  const isRequirement = element.kind !== 'BUSINESS_RULE'
  const classes = [
    'gr-node',
    kindInfo(element.kind).chip,
    selected ? 'sel' : '',
    element.submissionStatus === 'DRAFT' ? 'pending' : '',
    showCoverage && isRequirement && element.tests === 0 ? 'untested' : '',
  ]

  return (
    <div className={classes.filter(Boolean).join(' ')} title={`${element.code}: ${element.description}`}>
      <Handle type="target" position={Position.Right} className="gr-handle" isConnectable={false} />
      {showCoverage && isRequirement && <span className={element.tests > 0 ? 'gr-cov' : 'gr-cov no'} />}
      {element.code}
      <Handle type="source" position={Position.Left} className="gr-handle" isConnectable={false} />
    </div>
  )
}
