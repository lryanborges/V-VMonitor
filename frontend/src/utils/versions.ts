import type { Element, Relationship, Version, VersionedTest } from '../api/types'

/**
 * Converte a copia guardada numa versao para o formato das telas do modelo (tabela e painel do elemento).
 * Vinculos e testes sao contados a partir das relacoes e testes da propria versao (versoes anteriores aos testes,
 * RF9, nao tem nenhum) e a situacao e sempre "submetido", ja que uma versao e o proprio modelo submetido.
 */
export function versionModel(version: Version): {
  elements: Element[]
  relationships: Relationship[]
  tests: VersionedTest[]
} {
  const at = version.version.createdAt
  const links = new Map<string, number>()
  for (const r of version.relationships) {
    links.set(r.source.id, (links.get(r.source.id) ?? 0) + 1)
    links.set(r.target.id, (links.get(r.target.id) ?? 0) + 1)
  }
  const tests = new Map<string, number>()
  for (const t of version.tests) {
    t.covers.forEach((e) => tests.set(e.id, (tests.get(e.id) ?? 0) + 1))
  }
  return {
    elements: version.elements.map((e) => ({
      ...e,
      submissionStatus: 'SUBMITTED',
      links: links.get(e.id) ?? 0,
      tests: tests.get(e.id) ?? 0,
      createdAt: at,
      updatedAt: at,
    })),
    relationships: version.relationships.map((r) => ({ ...r, submissionStatus: 'SUBMITTED', createdAt: at })),
    tests: version.tests,
  }
}
