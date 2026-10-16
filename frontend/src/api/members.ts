import { api } from './client'
import type { Member } from './types'

export const memberKeys = {
  all: (projectId: string) => ['projects', projectId, 'members'] as const,
}

const base = (projectId: string) => `/api/projects/${projectId}/members`

/** Pessoas com acesso ao projeto (RF20). */
export const membersApi = {
  list: (projectId: string) => api<Member[]>(base(projectId)),

  /** So o proprietario; a pessoa (ja cadastrada) entra como editor. */
  add: (projectId: string, email: string) =>
    api<Member>(base(projectId), { method: 'POST', body: JSON.stringify({ email }) }),

  /** O proprietario remove um editor, ou a propria pessoa sai do projeto. */
  remove: (projectId: string, userId: string) => api<void>(`${base(projectId)}/${userId}`, { method: 'DELETE' }),
}
