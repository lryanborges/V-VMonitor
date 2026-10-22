import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { ChangeSummary, Element, ModelChanges, Project } from '../../api/types'
import { PageHeader } from '../../components/PageHeader'
import { GraphIcon, ListIcon, MatrixIcon, TickIcon, UploadIcon } from '../../components/icons'
import { timeAgo } from '../../utils/format'
import { SubmitVersionDialog } from './SubmitVersionDialog'
import './ModelHeader.css'

export type ModelView = 'list' | 'graph' | 'matrix'

interface ModelHeaderProps {
  project: Project
  view: ModelView
  elements: Element[]
  /** Mudancas desde a ultima versao; undefined enquanto carrega. */
  changes: ModelChanges | undefined
}

const total = (s: ChangeSummary) => s.added + s.modified + s.removed

/** Ex.: "2 adicionados, 1 removido" (itens: elementos e relacionamentos). */
function describe(s: ChangeSummary) {
  const part = (n: number, one: string, many: string) => (n === 0 ? null : `${n} ${n === 1 ? one : many}`)
  return [part(s.added, 'adicionado', 'adicionados'), part(s.modified, 'alterado', 'alterados'), part(s.removed, 'removido', 'removidos')]
    .filter(Boolean)
    .join(', ')
}

/**
 * Cabecalho das visoes do modelo: alternancia Lista / Grafo / Matriz (RF12), ultima versao, mudancas
 * desde ela (inclusive remocoes) e o botao de submeter, que grava a proxima versao (RF10 + RF17).
 */
export function ModelHeader({ project, view, elements, changes }: ModelHeaderProps) {
  const [submitting, setSubmitting] = useState(false)
  const canEdit = project.role !== 'VIEWER'
  const base = `/projetos/${project.id}`
  // o elemento selecionado (?el=) acompanha a troca de visualizacao
  const [params] = useSearchParams()
  const selectedId = params.get('el')
  const keep = selectedId ? `?el=${selectedId}` : ''
  const pending = changes ? total(changes.summary) : 0

  return (
    <>
      <PageHeader title="Modelo">
        <nav className="seg" aria-label="Modo de visualização">
          <Link to={`${base}${keep}`} aria-current={view === 'list' ? 'page' : undefined} className={view === 'list' ? 'on' : ''}>
            <ListIcon />Lista
          </Link>
          <Link
            to={`${base}/grafo${keep}`}
            aria-current={view === 'graph' ? 'page' : undefined}
            className={view === 'graph' ? 'on' : ''}
          >
            <GraphIcon />Grafo
          </Link>
          <Link
            to={`${base}/matriz${keep}`}
            aria-current={view === 'matrix' ? 'page' : undefined}
            className={view === 'matrix' ? 'on' : ''}
          >
            <MatrixIcon />Matriz
          </Link>
        </nav>
        <div style={{ flex: 1 }} />
        {project.latestVersion ? (
          <span className="md-last-submit" title={project.lastSubmittedAt ? new Date(project.lastSubmittedAt).toLocaleString('pt-BR') : undefined}>
            <span className="pill mono">v{project.latestVersion}</span>
            {project.lastSubmittedAt && (
              <span className="hint">
                {timeAgo(project.lastSubmittedAt)}
                {project.lastSubmittedBy?.name && ` por ${project.lastSubmittedBy.name}`}
              </span>
            )}
          </span>
        ) : (
          <span className="hint md-last-submit">Nenhuma versão ainda</span>
        )}
        {changes && pending > 0 && (
          <span className="pill w" title={describe(changes.summary)}>
            <span className="dot" />
            {pending === 1 ? '1 alteração' : `${pending} alterações`}
            {changes.baseVersion ? ` desde a v${changes.baseVersion}` : ' não submetidas'}
          </span>
        )}
        {changes && pending === 0 && project.latestVersion && (
          <span className="pill ok"><TickIcon />Modelo em dia</span>
        )}
        {canEdit && (
          <button
            className="btn p"
            type="button"
            disabled={pending === 0}
            title={pending === 0 ? 'Não há alterações desde a última versão' : undefined}
            onClick={() => setSubmitting(true)}
          >
            <UploadIcon />
            Submeter versão
          </button>
        )}
      </PageHeader>

      {submitting && changes && (
        <SubmitVersionDialog
          projectId={project.id}
          changes={changes}
          elements={elements}
          onClose={() => setSubmitting(false)}
          onSubmitted={() => setSubmitting(false)}
        />
      )}
    </>
  )
}
