import { api } from './client'
import type { CreateElementRequest, Element, ElementKind, NextCodeResponse } from './types'

export const elementKeys = {
  all: (projectId: string) => ['projects', projectId, 'elements'] as const,
  nextCode: (projectId: string, kind: ElementKind) => ['projects', projectId, 'next-code', kind] as const,
}

export const elementsApi = {
  list: (projectId: string) => api<Element[]>(`/api/projects/${projectId}/elements`),

  create: (projectId: string, data: CreateElementRequest) =>
    api<Element>(`/api/projects/${projectId}/elements`, { method: 'POST', body: JSON.stringify(data) }),

  nextCode: (projectId: string, kind: ElementKind) =>
    api<NextCodeResponse>(`/api/projects/${projectId}/elements/next-code?kind=${kind}`),
}
