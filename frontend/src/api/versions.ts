import { api } from './client'
import type { CreateVersionRequest, ModelChanges, Version, VersionSummary } from './types'

/** Com o que comparar uma versao: a anterior, o modelo atual ou o numero de outra versao. */
export type CompareWith = 'previous' | 'current' | number

export const versionKeys = {
  all: (projectId: string) => ['projects', projectId, 'versions'] as const,
  detail: (projectId: string, number: number) => ['projects', projectId, 'versions', number] as const,
  changes: (projectId: string) => ['projects', projectId, 'changes'] as const,
  compare: (projectId: string, number: number, against: CompareWith) =>
    ['projects', projectId, 'versions', number, 'compare', against] as const,
}

const base = (projectId: string) => `/api/projects/${projectId}`

/** Submissao do modelo como versao (RF10 + RF17) e consulta das versoes (RF18). */
export const versionsApi = {
  /** O que mudou desde a ultima versao: entra na proxima submissao. */
  changes: (projectId: string) => api<ModelChanges>(`${base(projectId)}/changes`),

  /** Submete o modelo, gravando a proxima versao (nome opcional). */
  create: (projectId: string, data: CreateVersionRequest) =>
    api<VersionSummary>(`${base(projectId)}/versions`, { method: 'POST', body: JSON.stringify(data) }),

  list: (projectId: string) => api<VersionSummary[]>(`${base(projectId)}/versions`),

  get: (projectId: string, number: number) => api<Version>(`${base(projectId)}/versions/${number}`),

  /** RF22 (proposto): o modelo atual volta a ser igual ao da versao; nao cria versao nova. */
  restore: (projectId: string, number: number) =>
    api<void>(`${base(projectId)}/versions/${number}/restore`, { method: 'POST' }),

  /** RF18: mudancas entre a versao e outra (sempre da mais antiga para a mais nova) ou o modelo atual. */
  compare: (projectId: string, number: number, against: CompareWith) =>
    api<ModelChanges>(`${base(projectId)}/versions/${number}/compare?with=${against}`),
}
