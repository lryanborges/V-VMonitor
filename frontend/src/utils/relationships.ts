import type { Relationship, RelationshipType } from '../api/types'

export type LineStyle = 'dep' | 'ref' | 'cf' | 'sim' | 'custom'

interface TypeMeta {
  /** Verbo da frase "origem [verbo] destino", ex.: "depende de". */
  verb: string
  /** Rotulo do grupo no painel do elemento de origem (ex.: "Depende de"). */
  outgoing: string
  /** Rotulo do grupo no painel do elemento de destino (ex.: "Necessário para"). */
  incoming: string
  hint: string
  line: LineStyle
}

/** Tipos pre-definidos do RF8, na ordem do UC-06 (Dependencia, Conflito, Refinamento, Similaridade). */
const PREDEFINED: Record<string, TypeMeta> = {
  'Dependência': {
    verb: 'depende de',
    outgoing: 'Depende de',
    incoming: 'Necessário para',
    hint: 'Um precisa do outro para ser atendido.',
    line: 'dep',
  },
  Conflito: {
    verb: 'está em conflito com',
    outgoing: 'Em conflito com',
    incoming: 'Em conflito com',
    hint: 'Atender um dificulta atender o outro.',
    line: 'cf',
  },
  Refinamento: {
    verb: 'refina',
    outgoing: 'Refina',
    incoming: 'Refinado por',
    hint: 'Um detalha ou especializa o outro.',
    line: 'ref',
  },
  Similaridade: {
    verb: 'é similar a',
    outgoing: 'Similar a',
    incoming: 'Similar a',
    hint: 'Tratam de necessidades parecidas.',
    line: 'sim',
  },
}

const PREDEFINED_ORDER = Object.keys(PREDEFINED)

/** Textos e estilo de linha de um tipo; tipos personalizados usam o proprio nome com setas. */
export function typeMeta(type: RelationshipType): TypeMeta {
  if (!type.custom && PREDEFINED[type.name]) return PREDEFINED[type.name]
  return {
    verb: `—${type.name}→`,
    outgoing: `${type.name} →`,
    incoming: `← ${type.name}`,
    hint: 'Tipo criado neste projeto.',
    line: 'custom',
  }
}

/** Pre-definidos na ordem do UC-06, depois os personalizados em ordem alfabetica. */
export function sortTypes(types: RelationshipType[]): RelationshipType[] {
  const rank = (t: RelationshipType) => (t.custom ? PREDEFINED_ORDER.length : PREDEFINED_ORDER.indexOf(t.name))
  return [...types].sort((a, b) => rank(a) - rank(b) || a.name.localeCompare(b.name, 'pt-BR'))
}

/** Ja existe essa relacao? Em tipos simetricos, A-B e B-A sao a mesma. */
export function relationshipExists(
  relationships: Relationship[],
  sourceId: string,
  targetId: string,
  type: RelationshipType,
): boolean {
  return relationships.some(
    (r) =>
      r.type.id === type.id &&
      ((r.source.id === sourceId && r.target.id === targetId) ||
        (type.symmetric && r.source.id === targetId && r.target.id === sourceId)),
  )
}

export interface RelationGroup {
  label: string
  items: { relationship: Relationship; other: Relationship['source'] }[]
}

/** Relacoes de um elemento agrupadas pela leitura a partir dele ("Depende de", "Necessário para"...). */
export function groupRelationsOf(elementId: string, relationships: Relationship[]): RelationGroup[] {
  const groups = new Map<string, RelationGroup>()
  for (const r of relationships) {
    const isSource = r.source.id === elementId
    if (!isSource && r.target.id !== elementId) continue
    const meta = typeMeta(r.type)
    const label = isSource ? meta.outgoing : meta.incoming
    const group = groups.get(label) ?? { label, items: [] }
    group.items.push({ relationship: r, other: isSource ? r.target : r.source })
    groups.set(label, group)
  }
  return [...groups.values()]
}
