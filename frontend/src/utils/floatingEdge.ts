import type { InternalNode, Node } from '@xyflow/react'

type Point = { x: number; y: number }

function center(node: InternalNode<Node>): Point {
  const { x, y } = node.internals.positionAbsolute
  return { x: x + (node.measured.width ?? 0) / 2, y: y + (node.measured.height ?? 0) / 2 }
}

/**
 * Ponto onde a reta entre os centros de dois nos cruza a borda do primeiro (retangulo do no).
 * E o que faz a aresta "flutuar": sair e chegar pela borda voltada para o outro no, em qualquer posicao.
 */
function borderPoint(node: InternalNode<Node>, toward: Point): Point {
  const c = center(node)
  const w = (node.measured.width ?? 0) / 2
  const h = (node.measured.height ?? 0) / 2
  const dx = toward.x - c.x
  const dy = toward.y - c.y
  if (dx === 0 && dy === 0) return c
  // escala ate tocar o lado vertical ou o horizontal do retangulo, o que vier primeiro
  const scale = Math.min(w / Math.abs(dx || Number.EPSILON), h / Math.abs(dy || Number.EPSILON))
  return { x: c.x + dx * scale, y: c.y + dy * scale }
}

/** Inicio e fim da aresta nas bordas dos dois nos, recuando um pouco o fim para a ponta da seta ficar visivel. */
export function floatingEdgeEnds(source: InternalNode<Node>, target: InternalNode<Node>, gap = 3) {
  const s = borderPoint(source, center(target))
  const t = borderPoint(target, center(source))
  const dx = t.x - s.x
  const dy = t.y - s.y
  const len = Math.hypot(dx, dy) || 1
  return { sx: s.x, sy: s.y, tx: t.x - (dx / len) * gap, ty: t.y - (dy / len) * gap }
}
