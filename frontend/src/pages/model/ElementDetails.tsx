import type { Element, Relationship } from '../../api/types'
import { EditIcon, PlusIcon, TrashIcon } from '../../components/icons'
import { KIND_TITLE, kindInfo, priorityInfo } from '../../utils/labels'
import { groupRelationsOf } from '../../utils/relationships'

interface ElementDetailsProps {
  element: Element | null
  relationships: Relationship[]
  canEdit: boolean
  /** Seleciona outro elemento (ao clicar numa relacao). */
  onSelect: (elementId: string) => void
  /** RF14: editar o elemento. */
  onEdit: () => void
  /** RF15: excluir o elemento (com o alerta do RF16). */
  onDelete: () => void
  /** UC-06 passo 1: "Adicionar relacionamento" a partir deste elemento. */
  onAddRelationship: () => void
  onEditRelationship: (relationship: Relationship) => void
  onRemoveRelationship: (relationship: Relationship) => void
}

/** Painel lateral com os detalhes do elemento selecionado, suas relacoes e testes. */
export function ElementDetails({
  element,
  relationships,
  canEdit,
  onSelect,
  onEdit,
  onDelete,
  onAddRelationship,
  onEditRelationship,
  onRemoveRelationship,
}: ElementDetailsProps) {
  if (!element) {
    return (
      <aside className="md-details md-details-empty" aria-label="Detalhes do elemento">
        <p className="hint">Selecione um elemento na lista para ver os detalhes, os relacionamentos e os testes.</p>
      </aside>
    )
  }

  const kind = kindInfo(element.kind)
  const prio = element.priority ? priorityInfo(element.priority) : null
  const groups = groupRelationsOf(element.id, relationships)
  const total = groups.reduce((n, g) => n + g.items.length, 0)

  return (
    <aside className="md-details" aria-label={`Detalhes de ${element.code}`}>
      <div className="md-details-head">
        <div className="md-details-title">
          <span className={`chip ${kind.chip}`} style={{ height: 26, fontSize: 13 }}>{element.code}</span>
          <span className="hint">{KIND_TITLE[element.kind]}</span>
          <div style={{ flex: 1 }} />
          {canEdit && (
            <>
              <button className="btn ic g" type="button" title="Editar" aria-label={`Editar ${element.code}`} onClick={onEdit}>
                <EditIcon />
              </button>
              <button className="btn ic g" type="button" title="Excluir" aria-label={`Excluir ${element.code}`} onClick={onDelete}>
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
          <span>Relacionamentos <span className="mono hint">{total}</span></span>
          {canEdit && (
            <button className="btn sm" type="button" onClick={onAddRelationship}><PlusIcon />Adicionar</button>
          )}
        </div>
        {total === 0 && <p className="hint">Nenhum relacionamento ainda.</p>}
        {groups.map((group) => (
          <div key={group.label} className="md-rel-group">
            <span className="eyebrow">{group.label}</span>
            {group.items.map(({ relationship, other }) => (
              <div key={relationship.id} className="rel-row">
                <button type="button" className="rel" title={`Ver ${other.code}`} onClick={() => onSelect(other.id)}>
                  <span className={`chip ${kindInfo(other.kind).chip}`}>{other.code}</span>
                  <span className="desc">{other.description}</span>
                </button>
                {canEdit && (
                  <div className="rel-actions">
                    <button
                      className="btn ic g sm"
                      type="button"
                      title="Trocar tipo ou inverter"
                      aria-label={`Editar relacionamento com ${other.code}`}
                      onClick={() => onEditRelationship(relationship)}
                    >
                      <EditIcon />
                    </button>
                    <button
                      className="btn ic g sm"
                      type="button"
                      title="Remover relacionamento"
                      aria-label={`Remover relacionamento com ${other.code}`}
                      onClick={() => onRemoveRelationship(relationship)}
                    >
                      <TrashIcon />
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        ))}

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
