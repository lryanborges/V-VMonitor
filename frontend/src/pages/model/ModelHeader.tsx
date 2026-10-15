import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { Element, Project, Relationship } from '../../api/types'
import { GraphIcon, ListIcon, MatrixIcon, SaveVersionIcon, TickIcon, UploadIcon } from '../../components/icons'
import { timeAgo } from '../../utils/format'
import { SubmitModelDialog } from './SubmitModelDialog'
import './ModelHeader.css'

export type ModelView = 'list' | 'graph' | 'matrix'

interface ModelHeaderProps {
  project: Project
  view: ModelView
  elements: Element[]
  relationships: Relationship[]
  drafts: number
}

/**
 * Cabecalho das visoes do modelo: alternancia Lista / Grafo / Matriz (RF12), ultima submissao,
 * alteracoes pendentes e o botao de submeter (RF10).
 */
export function ModelHeader({ project, view, elements, relationships, drafts }: ModelHeaderProps) {
  const [submitting, setSubmitting] = useState(false)
  const canEdit = project.role !== 'VIEWER'
  const base = `/projetos/${project.id}`
  // o elemento selecionado (?el=) acompanha a troca de visualizacao
  const [params] = useSearchParams()
  const selectedId = params.get('el')
  const keep = selectedId ? `?el=${selectedId}` : ''

  return (
    <>
      <header className="md-header">
        <h1>Modelo</h1>
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
        {project.lastSubmittedAt && (
          <span className="hint md-last-submit" title={new Date(project.lastSubmittedAt).toLocaleString('pt-BR')}>
            Última submissão {timeAgo(project.lastSubmittedAt)}
            {project.lastSubmittedBy?.name && ` por ${project.lastSubmittedBy.name}`}
          </span>
        )}
        {drafts > 0 ? (
          <span className="pill w" title="Elementos e relacionamentos em rascunho, ainda não submetidos.">
            <span className="dot" />
            {drafts === 1 ? '1 alteração não submetida' : `${drafts} alterações não submetidas`}
          </span>
        ) : (
          elements.length > 0 && project.lastSubmittedAt && (
            <span className="pill ok"><TickIcon />Modelo em dia</span>
          )
        )}
        <button className="btn" type="button" disabled title="Em breve"><SaveVersionIcon />Salvar versão</button>
        {canEdit && (
          <button
            className="btn p"
            type="button"
            disabled={drafts === 0}
            title={drafts === 0 ? 'Não há alterações para submeter' : undefined}
            onClick={() => setSubmitting(true)}
          >
            <UploadIcon />
            Submeter modelo
          </button>
        )}
      </header>

      {submitting && (
        <SubmitModelDialog
          projectId={project.id}
          elements={elements}
          relationships={relationships}
          onClose={() => setSubmitting(false)}
          onSubmitted={() => setSubmitting(false)}
        />
      )}
    </>
  )
}
