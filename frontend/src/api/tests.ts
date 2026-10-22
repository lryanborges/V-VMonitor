import { api } from './client'
import type { TestCase, TestRequest } from './types'

export const testKeys = {
  all: (projectId: string) => ['projects', projectId, 'tests'] as const,
}

const base = (projectId: string) => `/api/projects/${projectId}/tests`

/** Testes e a cobertura dos requisitos (RF9, RF13). */
export const testsApi = {
  list: (projectId: string) => api<TestCase[]>(base(projectId)),

  create: (projectId: string, data: TestRequest) =>
    api<TestCase>(base(projectId), { method: 'POST', body: JSON.stringify(data) }),

  update: (projectId: string, testId: string, data: TestRequest) =>
    api<TestCase>(`${base(projectId)}/${testId}`, { method: 'PUT', body: JSON.stringify(data) }),

  remove: (projectId: string, testId: string) => api<void>(`${base(projectId)}/${testId}`, { method: 'DELETE' }),
}
