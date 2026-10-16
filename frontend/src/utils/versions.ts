import type { Element, Relationship, Version } from '../api/types'

/**
 * Converte a copia guardada numa versao para o formato das telas do modelo (tabela e painel do elemento).
 * Vinculos sao contados a partir das relacoes da propria versao; testes nao fazem parte da copia (0) e a
 * situacao e sempre "submetido", ja que uma versao e o proprio modelo submetido.
 */
export function versionModel(version: Version): { elements: Element[]; relationships: Relationship[] } {
  const at = version.version.createdAt
  const links = new Map<string, number>()
  for (const r of version.relationships) {
    links.set(r.source.id, (links.get(r.source.id) ?? 0) + 1)
    links.set(r.target.id, (links.get(r.target.id) ?? 0) + 1)
  }
  return {
    elements: version.elements.map((e) => ({
      ...e,
      submissionStatus: 'SUBMITTED',
      links: links.get(e.id) ?? 0,
      tests: 0,
      createdAt: at,
      updatedAt: at,
    })),
    relationships: version.relationships.map((r) => ({ ...r, submissionStatus: 'SUBMITTED', createdAt: at })),
  }
}
