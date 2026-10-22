import { Handle, Position, type Node, type NodeProps } from '@xyflow/react'
import type { TestCase } from '../../../api/types'

export type TestNodeData = {
  test: TestCase
  /** Liga o elemento selecionado (cobre o requisito em foco). */
  highlighted: boolean
}

export type TestNodeType = Node<TestNodeData, 'test'>

/** No de teste (RF9): codigo T em verde; tracejado em rascunho e em alerta quando nao cobre mais nenhum requisito. */
export function TestNode({ data }: NodeProps<TestNodeType>) {
  const { test, highlighted } = data
  const classes = [
    'gr-node',
    't',
    highlighted ? 'hl' : '',
    test.submissionStatus === 'DRAFT' ? 'pending' : '',
    test.covers.length === 0 ? 'orphan' : '',
  ]
  const covers = test.covers.length === 0 ? 'sem requisito' : `cobre ${test.covers.map((e) => e.code).join(', ')}`

  return (
    <div className={classes.filter(Boolean).join(' ')} title={`${test.code}: ${test.description} (${covers})`}>
      <Handle type="target" position={Position.Right} className="gr-handle" isConnectable={false} />
      {test.code}
      <Handle type="source" position={Position.Left} className="gr-handle" isConnectable={false} />
    </div>
  )
}
