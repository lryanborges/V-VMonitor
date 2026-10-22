import { EdgeLabelRenderer, useInternalNode, useViewport, type Edge, type EdgeProps } from '@xyflow/react'
import { floatingEdgeEnds } from '../../../utils/floatingEdge'

export type TestEdgeData = {
  /** Liga o elemento selecionado. */
  highlighted: boolean
  pending: boolean
}

export type TestEdgeType = Edge<TestEdgeData, 'covers'>

/** Abaixo deste zoom o rotulo some, como nas relacoes. */
const MIN_ZOOM_FOR_LABELS = 0.6

/** Cobertura (RF9, RF13): linha pontilhada verde do teste ate o requisito, com o rotulo "testa". */
export function TestEdge({ source, target, sourceX, sourceY, targetX, targetY, data }: EdgeProps<TestEdgeType>) {
  const { zoom } = useViewport()
  const sourceNode = useInternalNode(source)
  const targetNode = useInternalNode(target)

  const { sx, sy, tx, ty } =
    sourceNode?.measured.width && targetNode?.measured.width
      ? floatingEdgeEnds(sourceNode, targetNode)
      : { sx: sourceX, sy: sourceY, tx: targetX, ty: targetY }
  const state = ['test', data?.highlighted ? 'hl' : '', data?.pending ? 'pending' : ''].filter(Boolean).join(' ')

  return (
    <>
      <path d={`M${sx},${sy} L${tx},${ty}`} className={`gr-e ${state}`} markerEnd="url(#gr-arrow-test)" />
      {zoom >= MIN_ZOOM_FOR_LABELS && (
        <EdgeLabelRenderer>
          <div
            className={`gr-elabel ${state}`}
            style={{ transform: `translate(-50%, -50%) translate(${(sx + tx) / 2}px, ${(sy + ty) / 2}px)` }}
          >
            testa
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  )
}
