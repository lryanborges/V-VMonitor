import { EdgeLabelRenderer, useInternalNode, useViewport, type Edge, type EdgeProps } from '@xyflow/react'
import type { Relationship } from '../../../api/types'
import { floatingEdgeEnds } from '../../../utils/floatingEdge'
import { relationSentence, typeMeta } from '../../../utils/relationships'

export type RelationEdgeData = {
  relationship: Relationship
  /** Destacada quando liga o elemento selecionado. */
  highlighted: boolean
  /** Desvio lateral (px) para separar varias relacoes entre o mesmo par. */
  offset: number
}

export type RelationEdgeType = Edge<RelationEdgeData, 'relation'>

/** Abaixo deste zoom os rotulos somem para nao poluir a visao geral. */
const MIN_ZOOM_FOR_LABELS = 0.6

/**
 * Aresta flutuante: sai e chega pela borda dos nos voltada um para o outro, em qualquer posicao
 * (inclusive depois de arrastar). Tem o traco do tipo (solida, tracejada, pontilhada, vermelha), seta nos tipos com direcao,
 * o nome da relacao no meio (ex.: "depende de") e transparencia quando ainda e rascunho.
 * Uma faixa invisivel mais larga facilita o clique.
 */
export function RelationEdge({ source, target, sourceX, sourceY, targetX, targetY, data }: EdgeProps<RelationEdgeType>) {
  const { zoom } = useViewport()
  const sourceNode = useInternalNode(source)
  const targetNode = useInternalNode(target)
  if (!data) return null

  // com os nos medidos, usa as bordas; antes disso, os pontos fixos dos handles
  const ends =
    sourceNode?.measured.width && targetNode?.measured.width
      ? floatingEdgeEnds(sourceNode, targetNode)
      : { sx: sourceX, sy: sourceY, tx: targetX, ty: targetY }
  const { relationship, highlighted, offset } = data
  const meta = typeMeta(relationship.type)
  const pending = relationship.submissionStatus === 'DRAFT'

  // curva quadratica com o ponto de controle deslocado na perpendicular (offset 0 = reta)
  const { sx, sy, tx, ty } = ends
  const dx = tx - sx
  const dy = ty - sy
  const len = Math.hypot(dx, dy) || 1
  const cx = (sx + tx) / 2 + (-dy / len) * offset * 2
  const cy = (sy + ty) / 2 + (dx / len) * offset * 2
  const path = `M${sx},${sy} Q${cx},${cy} ${tx},${ty}`
  // ponto do meio da curva (t = 0,5), onde fica o rotulo
  const lx = 0.25 * sx + 0.5 * cx + 0.25 * tx
  const ly = 0.25 * sy + 0.5 * cy + 0.25 * ty

  const state = [meta.line, highlighted ? 'hl' : '', pending ? 'pending' : ''].filter(Boolean).join(' ')
  const marker = relationship.type.symmetric ? undefined : highlighted ? 'url(#gr-arrow-hl)' : 'url(#gr-arrow)'
  const title = relationSentence(relationship) + (pending ? ' (pendente)' : '')

  return (
    <>
      <g>
        <path d={path} className={`gr-e ${state}`} markerEnd={marker} />
        <path d={path} className="gr-hit">
          <title>{title}</title>
        </path>
      </g>
      {zoom >= MIN_ZOOM_FOR_LABELS && (
        <EdgeLabelRenderer>
          <div
            className={`gr-elabel ${state}`}
            style={{ transform: `translate(-50%, -50%) translate(${lx}px, ${ly}px)` }}
          >
            {meta.short}
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  )
}
