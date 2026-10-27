import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import { elementKeys, elementsApi } from '../../api/elements'
import { relationshipKeys, relationshipsApi } from '../../api/relationships'
import { testKeys, testsApi } from '../../api/tests'
import { versionKeys, versionsApi } from '../../api/versions'

/**
 * Elementos, relacoes e testes do projeto, compartilhados pelas visoes do modelo (lista, grafo e matriz), e o
 * que mudou desde a ultima versao. O cache do TanStack Query evita buscar de novo ao alternar entre as visoes.
 */
export function useModelData(projectId: string) {
  const elementsQuery = useQuery({ queryKey: elementKeys.all(projectId), queryFn: () => elementsApi.list(projectId) })
  const relationshipsQuery = useQuery({
    queryKey: relationshipKeys.all(projectId),
    queryFn: () => relationshipsApi.list(projectId),
  })
  const testsQuery = useQuery({ queryKey: testKeys.all(projectId), queryFn: () => testsApi.list(projectId) })
  // as mudancas acompanham o modelo: qualquer edicao que recarregue elementos, relacoes ou testes recarrega
  // tambem as mudancas, sem que cada formulario precise invalida-las
  const changesQuery = useQuery({
    queryKey: [
      ...versionKeys.changes(projectId),
      elementsQuery.dataUpdatedAt,
      relationshipsQuery.dataUpdatedAt,
      testsQuery.dataUpdatedAt,
    ],
    queryFn: () => versionsApi.changes(projectId),
    enabled: elementsQuery.isSuccess && relationshipsQuery.isSuccess && testsQuery.isSuccess,
    placeholderData: keepPreviousData,
  })
  const elements = useMemo(() => elementsQuery.data ?? [], [elementsQuery.data])
  const relationships = useMemo(() => relationshipsQuery.data ?? [], [relationshipsQuery.data])
  const tests = useMemo(() => testsQuery.data ?? [], [testsQuery.data])

  return { elementsQuery, relationshipsQuery, testsQuery, elements, relationships, tests, changes: changesQuery.data }
}
