import { Graph, layout } from '@dagrejs/dagre'
import type { Element } from '../api/types'

export const NODE_HEIGHT = 30

/** Largura aproximada da pilula: codigo em fonte mono (~8px por caractere) + espaco da bolinha de cobertura. */
export function nodeWidth(element: Element): number {
  return element.code.length * 8 + 46
}

/** Largura do no de teste (RF9): so o codigo T, sem bolinha de cobertura. */
export function testNodeWidth(code: string): number {
  return code.length * 8 + 26
}

/** No e ligacao a posicionar: elementos e testes, relacoes e coberturas (teste -> requisito). */
export type LayoutNode = { id: string; width: number }
export type LayoutEdge = { source: string; target: string }

/**
 * Disposicao em camadas (RF11) calculada pelo dagre. Com rankdir RL a origem de cada ligacao fica a direita
 * e o destino a esquerda: requisitos de base e regras a esquerda, quem depende deles mais a direita, como no
 * design; os testes ficam a direita dos requisitos que cobrem. Devolve o canto superior esquerdo de cada no
 * (o formato que o React Flow espera).
 */
export function layoutGraph(nodes: LayoutNode[], edges: LayoutEdge[]): Map<string, { x: number; y: number }> {
  const g = new Graph()
  g.setGraph({ rankdir: 'RL', nodesep: 26, ranksep: 90, marginx: 20, marginy: 20 })
  g.setDefaultEdgeLabel(() => ({}))

  const ids = new Set(nodes.map((n) => n.id))
  nodes.forEach((n) => g.setNode(n.id, { width: n.width, height: NODE_HEIGHT }))
  edges.filter((e) => ids.has(e.source) && ids.has(e.target)).forEach((e) => g.setEdge(e.source, e.target))

  layout(g)

  const positions = new Map<string, { x: number; y: number }>()
  nodes.forEach((n) => {
    const p = g.node(n.id) as { x: number; y: number }
    positions.set(n.id, { x: p.x - n.width / 2, y: p.y - NODE_HEIGHT / 2 })
  })
  return positions
}
