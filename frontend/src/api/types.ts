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
  /** RF10 + RF17: numero da ultima versao (v1, v2...); null antes da primeira */
  latestVersion: number | null
  /** autor e data da ultima versao; nulos antes da primeira */
  lastSubmittedAt: string | null
  lastSubmittedBy: { id: string; name: string | null } | null
  createdAt: string
  updatedAt: string
}

export interface CreateProjectRequest {
  name: string
  description: string
}

export type ElementKind = 'FUNCTIONAL' | 'NON_FUNCTIONAL' | 'BUSINESS_RULE'
export type Priority = 'MANDATORY' | 'DESIRABLE' | 'OPTIONAL'
/** DRAFT: adicionado ou alterado desde a ultima versao (calculado pelo backend). */
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

/** RF14: tipo e codigo sao fixos; so descricao e prioridade mudam. */
export interface UpdateElementRequest {
  description: string
  priority: Priority | null
}

export type ImpactLevel = 'LOW' | 'MEDIUM' | 'HIGH'

/** DeleteImpactResponse.java: o que muda no modelo se o elemento for excluido (RF16). */
export interface DeleteImpact {
  level: ImpactLevel
  /** outgoing: o elemento excluido e a origem da relacao */
  relationships: { id: string; typeName: string; outgoing: boolean; other: ElementSummary }[]
  /** elementos que ficam sem nenhum vinculo */
  orphans: ElementSummary[]
  /** elementos que dependem do excluido */
  dependents: ElementSummary[]
  testsLeftWithoutRequirement: { id: string; code: string; description: string }[]
  tests: number
}

export interface NextCodeResponse {
  kind: ElementKind
  code: string
}

/** RelationshipTypeResponse.java; custom = criado no projeto (os quatro pre-definidos sao globais). */
export interface RelationshipType {
  id: string
  name: string
  symmetric: boolean
  custom: boolean
}

export interface ElementSummary {
  id: string
  code: string
  kind: ElementKind
  description: string
}

/** RelationshipResponse.java: le-se "source [tipo] target", ex.: RF7 depende de RF5. */
export interface Relationship {
  id: string
  type: RelationshipType
  source: ElementSummary
  target: ElementSummary
  submissionStatus: SubmissionStatus
  createdAt: string
}

export interface CreateRelationshipRequest {
  sourceId: string
  targetId: string
  typeId: string
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

/** ChangeSummary.java: itens (elementos e relacoes somados) adicionados, alterados e removidos. */
export interface ChangeSummary {
  added: number
  modified: number
  removed: number
}

/** ModelSnapshot.ElementSnapshot: elemento como esta (ou estava) numa versao. */
export interface ElementSnapshot {
  id: string
  code: string
  kind: ElementKind
  description: string
  priority: Priority | null
}

/** Relacao com pontas e tipo como estao (ou estavam) numa versao. */
export interface VersionedRelationship {
  id: string
  type: RelationshipType
  source: ElementSummary
  target: ElementSummary
}

/** Teste com os requisitos que cobre (ou cobria, numa versao). */
export interface VersionedTest {
  id: string
  code: string
  description: string
  covers: ElementSummary[]
}

export interface ChangeGroup<T> {
  added: T[]
  modified: { before: T; after: T }[]
  removed: T[]
}

/**
 * ModelChangesResponse.java: mudancas de baseVersion (null = antes da primeira versao) para targetVersion
 * (null = modelo atual). Sem comparacao explicita, e o que mudou desde a ultima versao.
 */
export interface ModelChanges {
  baseVersion: number | null
  targetVersion: number | null
  summary: ChangeSummary
  elements: ChangeGroup<ElementSnapshot>
  relationships: ChangeGroup<VersionedRelationship>
  tests: ChangeGroup<VersionedTest>
}

/** VersionSummaryResponse.java: versao na lista (RF18). */
export interface VersionSummary {
  id: string
  number: number
  label: string | null
  createdBy: { id: string; name: string | null }
  createdAt: string
  elements: number
  relationships: number
  /** 0 em versoes anteriores aos testes (RF9) */
  tests: number
  /** mudancas em relacao a versao anterior */
  changes: ChangeSummary | null
}

/** VersionResponse.java: versao com o modelo completo daquele momento (RF18). */
export interface Version {
  version: VersionSummary
  elements: ElementSnapshot[]
  relationships: VersionedRelationship[]
  tests: VersionedTest[]
}

export interface CreateVersionRequest {
  label: string | null
}

/** MemberResponse.java: pessoa com acesso ao projeto (RF20). */
export interface Member {
  userId: string
  name: string
  email: string
  role: MemberRole
  addedAt: string
}

/** TestResponse.java: teste com os requisitos que cobre (RF9). covers vazio = ficou sem requisito. */
export interface TestCase extends VersionedTest {
  submissionStatus: SubmissionStatus
  createdAt: string
  updatedAt: string
}

/** RF9: descricao e requisitos (RF/RNF) cobertos; usado ao criar e ao editar. */
export interface TestRequest {
  description: string
  elementIds: string[]
}
