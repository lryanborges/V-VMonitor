import { api } from './client'
import type {
  CreateElementRequest,
  DeleteImpact,
  Element,
  ElementKind,
  NextCodeResponse,
  UpdateElementRequest,
} from './types'

export const elementKeys = {
  all: (projectId: string) => ['projects', projectId, 'elements'] as const,
  nextCode: (projectId: string, kind: ElementKind) => ['projects', projectId, 'next-code', kind] as const,
  impact: (projectId: string, elementId: string) => ['projects', projectId, 'delete-impact', elementId] as const,
}

const base = (projectId: string) => `/api/projects/${projectId}/elements`

export const elementsApi = {
  list: (projectId: string) => api<Element[]>(base(projectId)),

  create: (projectId: string, data: CreateElementRequest) =>
    api<Element>(base(projectId), { method: 'POST', body: JSON.stringify(data) }),

  update: (projectId: string, elementId: string, data: UpdateElementRequest) =>
    api<Element>(`${base(projectId)}/${elementId}`, { method: 'PUT', body: JSON.stringify(data) }),

  deleteImpact: (projectId: string, elementId: string) =>
    api<DeleteImpact>(`${base(projectId)}/${elementId}/delete-impact`),

  remove: (projectId: string, elementId: string) =>
    api<void>(`${base(projectId)}/${elementId}`, { method: 'DELETE' }),

  nextCode: (projectId: string, kind: ElementKind) =>
    api<NextCodeResponse>(`${base(projectId)}/next-code?kind=${kind}`),
}
