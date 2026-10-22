import type { ElementSummary, Relationship, VersionedTest } from '../api/types'
import { typeMeta } from './relationships'

const isDependency = (r: Relationship) => !r.type.custom && r.type.name === 'Dependência'

export interface EditImpact {
  /** Quem depende do elemento diretamente ("X depende de <elemento>"). */
  direct: ElementSummary[]
  /** Quem depende dele por uma cadeia de dependencias; via = o elemento pelo qual a dependencia chega. */
  indirect: { element: ElementSummary; via: ElementSummary }[]
  /** Demais vinculos (refinamento, conflito, similaridade, personalizados), com o rotulo lido a partir do elemento. */
  others: { element: ElementSummary; label: string }[]
  /** Testes que cobrem o elemento. */
  tests: VersionedTest[]
}

/**
 * RF16 (ajuste para a v2 do documento): o que pode ser afetado ao editar um elemento. Segue as dependencias no
 * sentido inverso (quem depende dele, e quem depende desses, ...), sem repetir elementos e sem entrar em ciclo.
 * "Depende de" no sentido contrario (o elemento editado depende de outro) nao afeta o outro, entao fica de fora.
 */
export function editImpact(elementId: string, relationships: Relationship[], tests: VersionedTest[]): EditImpact {
  // dependentes de cada elemento: target -> [source]
  const dependents = new Map<string, ElementSummary[]>()
  relationships.filter(isDependency).forEach((r) => {
    dependents.set(r.target.id, [...(dependents.get(r.target.id) ?? []), r.source])
  })

  const direct = dependents.get(elementId) ?? []
  const seen = new Set<string>([elementId, ...direct.map((e) => e.id)])
  const indirect: EditImpact['indirect'] = []
  // busca em largura a partir dos dependentes diretos; via guarda o dependente direto de onde a cadeia partiu
  let frontier = direct.map((e) => ({ element: e, via: e }))
  while (frontier.length > 0) {
    const next: typeof frontier = []
    for (const { element, via } of frontier) {
      for (const dependent of dependents.get(element.id) ?? []) {
        if (seen.has(dependent.id)) continue
        seen.add(dependent.id)
        indirect.push({ element: dependent, via: element })
        next.push({ element: dependent, via })
      }
    }
    frontier = next
  }

  const others = relationships
    .filter((r) => !isDependency(r) && (r.source.id === elementId || r.target.id === elementId))
    .map((r) => {
      const outgoing = r.source.id === elementId
      const meta = typeMeta(r.type)
      return { element: outgoing ? r.target : r.source, label: outgoing ? meta.outgoing : meta.incoming }
    })

  return { direct, indirect, others, tests: tests.filter((t) => t.covers.some((e) => e.id === elementId)) }
}
