import { api } from './client'
import type { CreateRelationshipRequest, Relationship, RelationshipType } from './types'

export const relationshipKeys = {
  all: (projectId: string) => ['projects', projectId, 'relationships'] as const,
  types: (projectId: string) => ['projects', projectId, 'relationship-types'] as const,
}

const base = (projectId: string) => `/api/projects/${projectId}`

export const relationshipsApi = {
  list: (projectId: string) => api<Relationship[]>(`${base(projectId)}/relationships`),

  create: (projectId: string, data: CreateRelationshipRequest) =>
    api<Relationship>(`${base(projectId)}/relationships`, { method: 'POST', body: JSON.stringify(data) }),

  /** RF14: troca o tipo e, se reversed, inverte a direcao (as pontas continuam as mesmas). */
  update: (projectId: string, relationshipId: string, data: { typeId: string; reversed: boolean }) =>
    api<Relationship>(`${base(projectId)}/relationships/${relationshipId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  remove: (projectId: string, relationshipId: string) =>
    api<void>(`${base(projectId)}/relationships/${relationshipId}`, { method: 'DELETE' }),

  listTypes: (projectId: string) => api<RelationshipType[]>(`${base(projectId)}/relationship-types`),

  createType: (projectId: string, name: string) =>
    api<RelationshipType>(`${base(projectId)}/relationship-types`, {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),
}
