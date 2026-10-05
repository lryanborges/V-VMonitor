import type { Element } from '../../api/types'
import { EditIcon, PlusIcon, TrashIcon } from '../../components/icons'
import { KIND_TITLE, kindInfo, priorityInfo } from '../../utils/labels'

/** Painel lateral com os detalhes do elemento selecionado. */
export function ElementDetails({ element, canEdit }: { element: Element | null; canEdit: boolean }) {
  if (!element) {
    return (
      <aside className="md-details md-details-empty" aria-label="Detalhes do elemento">
        <p className="hint">Selecione um elemento na lista para ver os detalhes, os relacionamentos e os testes.</p>
      </aside>
    )
  }

  const kind = kindInfo(element.kind)
  const prio = element.priority ? priorityInfo(element.priority) : null

  return (
    <aside className="md-details" aria-label={`Detalhes de ${element.code}`}>
      <div className="md-details-head">
        <div className="md-details-title">
          <span className={`chip ${kind.chip}`} style={{ height: 26, fontSize: 13 }}>{element.code}</span>
          <span className="hint">{KIND_TITLE[element.kind]}</span>
          <div style={{ flex: 1 }} />
          {canEdit && (
            <>
              <button className="btn ic g" type="button" disabled title="Em breve" aria-label={`Editar ${element.code}`}>
                <EditIcon />
              </button>
              <button className="btn ic g" type="button" disabled title="Em breve" aria-label={`Excluir ${element.code}`}>
                <TrashIcon />
              </button>
            </>
          )}
        </div>
        <p className="md-details-desc">{element.description}</p>
        <div className="md-details-meta">
          <div>
            <div className="hint">Prioridade</div>
            {prio ? (
              <span className={`prio p${prio.level}`}><b><i /><i /><i /></b>{prio.label}</span>
            ) : (
              <span className="hint">Regras de negócio não têm prioridade</span>
            )}
          </div>
          <div>
            <div className="hint">Situação</div>
            {element.submissionStatus === 'DRAFT' ? (
              <span className="pill w" title="Aparece no grafo e na matriz depois da submissão do modelo.">Rascunho</span>
            ) : (
              <span className="pill ok">Submetido</span>
            )}
          </div>
        </div>
      </div>

      <div className="md-details-body">
        <div className="md-section-head">
          <span>Relacionamentos <span className="mono hint">{element.links}</span></span>
          {canEdit && (
            <button className="btn sm" type="button" disabled title="Em breve"><PlusIcon />Adicionar</button>
          )}
        </div>
        <p className="hint">
          {element.links === 0 ? 'Nenhum relacionamento ainda.' : 'A lista de relacionamentos chega na próxima etapa.'}
        </p>

        {element.kind !== 'BUSINESS_RULE' && (
          <>
            <div className="md-section-head md-section-sep">
              <span>Testes <span className="mono hint">{element.tests}</span></span>
              {canEdit && (
                <button className="btn sm" type="button" disabled title="Em breve"><PlusIcon />Associar teste</button>
              )}
            </div>
            <p className="hint">
              {element.tests === 0 ? 'Nenhum teste associado ainda.' : 'A lista de testes chega em uma próxima etapa.'}
            </p>
          </>
        )}
      </div>
    </aside>
  )
}
