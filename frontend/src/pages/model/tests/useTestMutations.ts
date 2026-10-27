import { useQueryClient } from '@tanstack/react-query'
import { elementKeys } from '../../../api/elements'
import { projectKeys } from '../../../api/projects'
import { testKeys } from '../../../api/tests'

/**
 * Depois de criar, editar ou excluir um teste: a lista de testes, a contagem de testes de cada requisito
 * (RF13) e a cobertura do card e do cabecalho do projeto mudam.
 */
export function useRefreshAfterTestChange(projectId: string) {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: testKeys.all(projectId) })
    queryClient.invalidateQueries({ queryKey: elementKeys.all(projectId) })
    queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId), exact: true })
    queryClient.invalidateQueries({ queryKey: projectKeys.all, exact: true })
  }
}
