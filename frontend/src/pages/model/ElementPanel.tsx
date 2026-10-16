import { useState } from 'react'
import type { Element, Relationship } from '../../api/types'
import { EditIcon, PlusIcon, TrashIcon } from '../../components/icons'
import { KIND_TITLE, kindInfo, priorityInfo } from '../../utils/labels'
import { groupRelationsOf } from '../../utils/relationships'
import { AddRelationshipModal } from './AddRelationshipModal'
import { DeleteElementDialog } from './DeleteElementDialog'
import { EditRelationshipModal } from './EditRelationshipModal'
import { ElementActions } from './ElementActions'
import { ElementFormDrawer } from './ElementFormDrawer'
import { RemoveRelationshipDialog } from './RemoveRelationshipDialog'
import './ElementPanel.css'

interface ElementPanelProps {
  projectId: string
  /** Elemento selecionado (via ?el=); null mostra a orientacao de vazio. */
  element: Element | null
  elements: Element[]
  relationships: Relationship[]
  canEdit: boolean
  /** Seleciona outro elemento (ao clicar num vinculo) ou limpa a selecao (null, apos excluir). */
  onSelect: (elementId: string | null) => void
  /** Texto do estado vazio, conforme a visualizacao. */
  emptyHint: string
  /**
   * Elemento de uma versao (RF18): so leitura, sem a situacao (rascunho/submetido) e sem testes, que nao fazem
   * parte da copia guardada.
   */
  fromVersion?: boolean
}

type PanelAction =
  | { mode: 'editElement' }
  | { mode: 'deleteElement' }
  | { mode: 'addRelationship' }
  | { mode: 'editRelationship'; relationship: Relationship }
  | { mode: 'removeRelationship'; relationship: Relationship }

/**
 * Painel lateral do elemento selecionado, comum a lista, ao grafo e a matriz: detalhes, numeros de
 * dependencia e testes, vinculos e as acoes de editar e excluir (RF14, RF15/RF16) e de relacionar (RF7, RF8).
 * As janelas dessas acoes abrem dentro do proprio painel.
 */
export function ElementPanel({
  projectId,
  element,
  elements,
  relationships,
  canEdit,
  onSelect,
  emptyHint,
  fromVersion = false,
}: ElementPanelProps) {
  const [action, setAction] = useState<PanelAction | null>(null)
  const close = () => setAction(null)

  if (!element) {
    return (
      <aside className="ep ep-empty" aria-label="Elemento selecionado">
        <p className="hint">{emptyHint}</p>
      </aside>
    )
  }

  const prio = element.priority ? priorityInfo(element.priority) : null
  const isRule = element.kind === 'BUSINESS_RULE'
  const own = relationships.filter((r) => r.source.id === element.id || r.target.id === element.id)
  const isDependency = (r: Relationship) => !r.type.custom && r.type.name === 'Dependência'
  const dependsOn = own.filter((r) => isDependency(r) && r.source.id === element.id).length
  const dependents = own.filter((r) => isDependency(r) && r.target.id === element.id).length
  const groups = groupRelationsOf(element.id, relationships)

  return (
    <>
    <aside className="ep" aria-label={`Elemento selecionado: ${element.code}`}>
      <div className="ep-head">
        <div className="ep-title">
          <span className={`chip ${kindInfo(element.kind).chip}`} style={{ height: 26, fontSize: 13 }}>{element.code}</span>
          <span className="hint">{KIND_TITLE[element.kind]}</span>
          <div style={{ flex: 1 }} />
          {!fromVersion &&
            (element.submissionStatus === 'DRAFT' ? (
              <span className="pill w" title="Alteração ainda não submetida.">Rascunho</span>
            ) : (
              <span className="pill ok">Submetido</span>
            ))}
        </div>
        <p className="ep-desc">{element.description}</p>
        {prio && (
          <span className={`prio p${prio.level}`}><b><i /><i /><i /></b>{prio.label}</span>
        )}
        {canEdit && (
          <ElementActions
            onEdit={() => setAction({ mode: 'editElement' })}
            onDelete={() => setAction({ mode: 'deleteElement' })}
          />
        )}
        <div className={`ep-stats${fromVersion ? ' two' : ''}`}>
          <div><b className="mono">{dependsOn}</b><span className="hint">Depende de</span></div>
          <div><b className="mono">{dependents}</b><span className="hint">Dependentes</span></div>
          {!fromVersion && (
            <div>
              <b className={`mono${element.tests > 0 ? ' ok' : ''}`}>{isRule ? '—' : element.tests}</b>
              <span className="hint">{element.tests === 1 ? 'Teste' : 'Testes'}</span>
            </div>
          )}
        </div>
      </div>

      <div className="ep-body">
        <div className="ep-section-head">
          <span>Relacionamentos <span className="mono hint">{own.length}</span></span>
          {canEdit && (
            <button className="btn sm" type="button" onClick={() => setAction({ mode: 'addRelationship' })}>
              <PlusIcon />Adicionar
            </button>
          )}
        </div>
        {own.length === 0 && <p className="hint">Nenhum relacionamento ainda.</p>}
        {groups.map((group) => (
          <div key={group.label} className="ep-rel-group">
            <span className="eyebrow">{group.label}</span>
            {group.items.map(({ relationship, other }) => (
              <div key={relationship.id} className="ep-rel-row">
                <button type="button" className="rel" title={`Ver ${other.code}`} onClick={() => onSelect(other.id)}>
                  <span className={`chip ${kindInfo(other.kind).chip}`}>{other.code}</span>
                  <span className="desc">{other.description}</span>
                </button>
                {canEdit && (
                  <div className="ep-rel-actions">
                    <button
                      className="btn ic g sm"
                      type="button"
                      title="Trocar tipo ou inverter"
                      aria-label={`Editar relacionamento com ${other.code}`}
                      onClick={() => setAction({ mode: 'editRelationship', relationship })}
                    >
                      <EditIcon />
                    </button>
                    <button
                      className="btn ic g sm"
                      type="button"
                      title="Remover relacionamento"
                      aria-label={`Remover relacionamento com ${other.code}`}
                      onClick={() => setAction({ mode: 'removeRelationship', relationship })}
                    >
                      <TrashIcon />
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        ))}

        {!isRule && !fromVersion && (
          <>
            <div className="ep-section-head ep-section-sep">
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

      {action?.mode === 'editElement' && (
        <ElementFormDrawer projectId={projectId} element={element} onClose={close} onSaved={close} />
      )}

      {action?.mode === 'deleteElement' && (
        <DeleteElementDialog
          projectId={projectId}
          element={element}
          onClose={close}
          // o elemento deixa de existir: limpa a selecao
          onDeleted={() => {
            close()
            onSelect(null)
          }}
        />
      )}

      {action?.mode === 'addRelationship' && (
        <AddRelationshipModal
          projectId={projectId}
          origin={element}
          elements={elements}
          relationships={relationships}
          onClose={close}
          // UC-06 passo 6: o novo vinculo aparece no painel
          onCreated={close}
        />
      )}

      {action?.mode === 'editRelationship' && (
        <EditRelationshipModal
          projectId={projectId}
          relationship={action.relationship}
          relationships={relationships}
          onClose={close}
          onSaved={close}
        />
      )}

      {action?.mode === 'removeRelationship' && (
        <RemoveRelationshipDialog
          projectId={projectId}
          relationship={action.relationship}
          onClose={close}
          onRemoved={close}
        />
      )}
    </>
  )
}
