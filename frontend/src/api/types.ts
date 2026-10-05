// Tipos espelhando os DTOs do backend (com.vvmonitor.api.dto)

export interface User {
  id: string
  name: string
  email: string
  createdAt: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: string
  user: User
}

export interface RegisterUserRequest {
  name: string
  email: string
  password: string
  passwordConfirmation: string
}

export type MemberRole = 'OWNER' | 'EDITOR' | 'VIEWER'

export interface ProjectStats {
  requirements: number
  businessRules: number
  tests: number
  requirementsWithTests: number
  /** percentual (0 a 100) de requisitos com ao menos um teste */
  coverage: number
}

/** ProjectResponse.java; role e a permissao do usuario logado (diferente de OWNER = compartilhado). */
export interface Project {
  id: string
  name: string
  description: string | null
  role: MemberRole
  owner: { id: string; name: string | null }
  members: { id: string; name: string; role: MemberRole }[]
  stats: ProjectStats
  latestVersion: number | null
  createdAt: string
  updatedAt: string
}

export interface CreateProjectRequest {
  name: string
  description: string
}

export type ElementKind = 'FUNCTIONAL' | 'NON_FUNCTIONAL' | 'BUSINESS_RULE'
export type Priority = 'MANDATORY' | 'DESIRABLE' | 'OPTIONAL'
export type SubmissionStatus = 'DRAFT' | 'SUBMITTED'

/** ElementResponse.java: requisito funcional, nao funcional ou regra de negocio. */
export interface Element {
  id: string
  code: string
  kind: ElementKind
  description: string
  /** null para regras de negocio */
  priority: Priority | null
  submissionStatus: SubmissionStatus
  links: number
  tests: number
  createdAt: string
  updatedAt: string
}

export interface CreateElementRequest {
  kind: ElementKind
  description: string
  priority: Priority | null
}

export interface NextCodeResponse {
  kind: ElementKind
  code: string
}

/** Formato padrao de erro do backend (ErrorResponse.java). */
export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors: { field: string; message: string }[]
}
