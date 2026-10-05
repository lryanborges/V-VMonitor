import { api } from './client'
import type { CreateProjectRequest, Project } from './types'

export const projectKeys = {
  all: ['projects'] as const,
  detail: (id: string) => ['projects', id] as const,
}

export const projectsApi = {
  list: () => api<Project[]>('/api/projects'),

  get: (id: string) => api<Project>(`/api/projects/${id}`),

  create: (data: CreateProjectRequest) =>
    api<Project>('/api/projects', { method: 'POST', body: JSON.stringify(data) }),
}
