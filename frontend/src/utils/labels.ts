import type { ElementKind, Priority } from '../api/types'

/** Rotulos e classes visuais dos tipos de elemento, na ordem exibida nas abas e no seletor. */
export const KINDS: { kind: ElementKind; label: string; plural: string; prefix: string; chip: string }[] = [
  { kind: 'FUNCTIONAL', label: 'Funcional', plural: 'Funcionais', prefix: 'RF', chip: 'rf' },
  { kind: 'NON_FUNCTIONAL', label: 'Não funcional', plural: 'Não funcionais', prefix: 'RNF', chip: 'rnf' },
  { kind: 'BUSINESS_RULE', label: 'Regra de negócio', plural: 'Regras', prefix: 'RN', chip: 'rn' },
]

export const kindInfo = (kind: ElementKind) => KINDS.find((k) => k.kind === kind)!

/** Nome completo do tipo, usado no painel de detalhes (ex.: "Requisito funcional"). */
export const KIND_TITLE: Record<ElementKind, string> = {
  FUNCTIONAL: 'Requisito funcional',
  NON_FUNCTIONAL: 'Requisito não funcional',
  BUSINESS_RULE: 'Regra de negócio',
}

/** Prioridades do RF5 com a explicacao exibida no formulario; level alimenta as barrinhas (.prio.p1..p3). */
export const PRIORITIES: { value: Priority; label: string; hint: string; level: 1 | 2 | 3 }[] = [
  { value: 'MANDATORY', label: 'Obrigatório', hint: 'Sem ele a entrega não atende ao objetivo.', level: 3 },
  { value: 'DESIRABLE', label: 'Desejável', hint: 'Agrega valor e pode ficar para uma versão seguinte.', level: 2 },
  { value: 'OPTIONAL', label: 'Opcional', hint: 'Implementar se houver tempo.', level: 1 },
]

export const priorityInfo = (priority: Priority) => PRIORITIES.find((p) => p.value === priority)!
