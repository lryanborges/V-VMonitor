import { useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import { elementKeys, elementsApi } from '../../api/elements'
import { relationshipKeys, relationshipsApi } from '../../api/relationships'

/**
 * Elementos e relacoes do projeto, compartilhados pelas visoes do modelo (lista, matriz e, depois, grafo).
 * O cache do TanStack Query evita buscar de novo ao alternar entre as visoes.
 */
export function useModelData(projectId: string) {
  const elementsQuery = useQuery({ queryKey: elementKeys.all(projectId), queryFn: () => elementsApi.list(projectId) })
  const relationshipsQuery = useQuery({
    queryKey: relationshipKeys.all(projectId),
    queryFn: () => relationshipsApi.list(projectId),
  })
  const elements = useMemo(() => elementsQuery.data ?? [], [elementsQuery.data])
  const relationships = useMemo(() => relationshipsQuery.data ?? [], [relationshipsQuery.data])

  // alteracoes nao submetidas (RF10): elementos e relacionamentos em rascunho
  const drafts =
    elements.filter((e) => e.submissionStatus === 'DRAFT').length +
    relationships.filter((r) => r.submissionStatus === 'DRAFT').length

  return { elementsQuery, relationshipsQuery, elements, relationships, drafts }
}
