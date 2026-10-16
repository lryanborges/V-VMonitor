import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import { elementKeys, elementsApi } from '../../api/elements'
import { relationshipKeys, relationshipsApi } from '../../api/relationships'
import { versionKeys, versionsApi } from '../../api/versions'

/**
 * Elementos e relacoes do projeto, compartilhados pelas visoes do modelo (lista, grafo e matriz), e o que
 * mudou desde a ultima versao. O cache do TanStack Query evita buscar de novo ao alternar entre as visoes.
 */
export function useModelData(projectId: string) {
  const elementsQuery = useQuery({ queryKey: elementKeys.all(projectId), queryFn: () => elementsApi.list(projectId) })
  const relationshipsQuery = useQuery({
    queryKey: relationshipKeys.all(projectId),
    queryFn: () => relationshipsApi.list(projectId),
  })
  // as mudancas acompanham o modelo: qualquer edicao que recarregue elementos ou relacoes recarrega tambem
  // as mudancas, sem que cada formulario precise invalida-las
  const changesQuery = useQuery({
    queryKey: [...versionKeys.changes(projectId), elementsQuery.dataUpdatedAt, relationshipsQuery.dataUpdatedAt],
    queryFn: () => versionsApi.changes(projectId),
    enabled: elementsQuery.isSuccess && relationshipsQuery.isSuccess,
    placeholderData: keepPreviousData,
  })
  const elements = useMemo(() => elementsQuery.data ?? [], [elementsQuery.data])
  const relationships = useMemo(() => relationshipsQuery.data ?? [], [relationshipsQuery.data])

  return { elementsQuery, relationshipsQuery, elements, relationships, changes: changesQuery.data }
}
