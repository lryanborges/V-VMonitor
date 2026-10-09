import { api } from './client'
import type { CreateProjectRequest, Project, SubmissionResult } from './types'

export const projectKeys = {
  all: ['projects'] as const,
  detail: (id: string) => ['projects', id] as const,
}

export const projectsApi = {
  list: () => api<Project[]>('/api/projects'),

  get: (id: string) => api<Project>(`/api/projects/${id}`),

  create: (data: CreateProjectRequest) =>
    api<Project>('/api/projects', { method: 'POST', body: JSON.stringify(data) }),

  /** RF10: submete todos os elementos e relacoes em rascunho. */
  submit: (id: string) => api<SubmissionResult>(`/api/projects/${id}/submit`, { method: 'POST' }),
}
