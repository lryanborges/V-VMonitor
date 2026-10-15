import { Graph, layout } from '@dagrejs/dagre'
import type { Element, Relationship } from '../api/types'

export const NODE_HEIGHT = 30

/** Largura aproximada da pilula: codigo em fonte mono (~8px por caractere) + espaco da bolinha de cobertura. */
export function nodeWidth(element: Element): number {
  return element.code.length * 8 + 46
}

/**
 * Disposicao em camadas (RF11) calculada pelo dagre. Com rankdir RL a origem de cada relacao fica a direita
 * e o destino a esquerda: requisitos de base e regras a esquerda, quem depende deles mais a direita, como no design.
 * Devolve o canto superior esquerdo de cada no (o formato que o React Flow espera).
 */
export function layoutGraph(elements: Element[], relationships: Relationship[]): Map<string, { x: number; y: number }> {
  const g = new Graph()
  g.setGraph({ rankdir: 'RL', nodesep: 26, ranksep: 90, marginx: 20, marginy: 20 })
  g.setDefaultEdgeLabel(() => ({}))

  const ids = new Set(elements.map((e) => e.id))
  elements.forEach((e) => g.setNode(e.id, { width: nodeWidth(e), height: NODE_HEIGHT }))
  relationships
    .filter((r) => ids.has(r.source.id) && ids.has(r.target.id))
    .forEach((r) => g.setEdge(r.source.id, r.target.id))

  layout(g)

  const positions = new Map<string, { x: number; y: number }>()
  elements.forEach((e) => {
    const n = g.node(e.id) as { x: number; y: number }
    positions.set(e.id, { x: n.x - nodeWidth(e) / 2, y: n.y - NODE_HEIGHT / 2 })
  })
  return positions
}
