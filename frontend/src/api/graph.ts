import { api } from './client'

/** Posicao salva de um no do grafo (elemento ou teste), canto superior esquerdo nas coordenadas do React Flow. */
export interface GraphPosition {
  nodeId: string
  x: number
  y: number
}

export const graphKeys = {
  positions: (projectId: string) => ['projects', projectId, 'graph-positions'] as const,
}

const base = (projectId: string) => `/api/projects/${projectId}/graph-positions`

/** RF21: posicao dos nos no grafo, compartilhada entre os membros (so proprietario e editores salvam). */
export const graphApi = {
  positions: (projectId: string) => api<GraphPosition[]>(base(projectId)),

  save: (projectId: string, positions: GraphPosition[]) =>
    api<void>(base(projectId), { method: 'PUT', body: JSON.stringify({ positions }) }),
}
