import type { ReactNode } from 'react'
import type { TestCase } from '../../../api/types'
import { EditIcon, TrashIcon } from '../../../components/icons'
import { kindInfo } from '../../../utils/labels'
import '../ElementTable.css'
import './Tests.css'

interface TestTableProps {
  tests: TestCase[]
  canEdit: boolean
  onEdit: (test: TestCase) => void
  onDelete: (test: TestCase) => void
  /** Clicar num requisito coberto abre o elemento no painel. */
  onSelectElement: (elementId: string) => void
  children?: ReactNode
}

/** Aba Testes da lista (RF9): codigo, descricao e requisitos cobertos de cada teste. */
export function TestTable({ tests, canEdit, onEdit, onDelete, onSelectElement, children }: TestTableProps) {
  return (
    <div className="card md-table tt-table" role="table" aria-label="Testes do projeto">
      <div className="row hd" role="row">
        <span role="columnheader">ID</span>
        <span role="columnheader">Descrição</span>
        <span role="columnheader">Cobre</span>
        <span role="columnheader" />
      </div>
      <div className="md-rows">
        {children}
        {tests.map((t) => (
          <div key={t.id} className="row" role="row" style={{ cursor: 'default' }}>
            <span role="cell"><span className="chip t">{t.code}</span></span>
            <span role="cell" className="desc" title={t.description}>{t.description}</span>
            <span role="cell" className="tt-covers">
              {t.covers.length === 0 ? (
                <span className="pill w" title="O requisito que ele cobria foi excluído.">Sem requisito</span>
              ) : (
                t.covers.map((e) => (
                  <button
                    key={e.id}
                    type="button"
                    className={`chip ${kindInfo(e.kind).chip}`}
                    title={`Ver ${e.code}: ${e.description}`}
                    style={{ border: 0 }}
                    onClick={() => onSelectElement(e.id)}
                  >
                    {e.code}
                  </button>
                ))
              )}
            </span>
            <span role="cell" className="tt-row-actions">
              {canEdit && (
                <>
                  <button className="btn ic g sm" type="button" title="Editar teste" aria-label={`Editar ${t.code}`} onClick={() => onEdit(t)}>
                    <EditIcon />
                  </button>
                  <button className="btn ic g sm" type="button" title="Excluir teste" aria-label={`Excluir ${t.code}`} onClick={() => onDelete(t)}>
                    <TrashIcon />
                  </button>
                </>
              )}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
