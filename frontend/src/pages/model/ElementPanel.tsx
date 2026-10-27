import { useState } from 'react'
import type { Element, Relationship, TestCase, VersionedTest } from '../../api/types'
import { EditIcon, PlusIcon, TrashIcon } from '../../components/icons'
import { KIND_TITLE, kindInfo, priorityInfo } from '../../utils/labels'
import { groupRelationsOf } from '../../utils/relationships'
import { AddRelationshipModal } from './AddRelationshipModal'
import { DeleteElementDialog } from './DeleteElementDialog'
import { EditRelationshipModal } from './EditRelationshipModal'
import { ElementActions } from './ElementActions'
import { ElementFormDrawer } from './ElementFormDrawer'
import { RemoveRelationshipDialog } from './RemoveRelationshipDialog'
import { DeleteTestDialog } from './tests/DeleteTestDialog'
import { TestDialog } from './tests/TestDialog'
import { TestItem } from './tests/TestItem'
import './ElementPanel.css'

interface ElementPanelProps {
  projectId: string
  /** Elemento selecionado (via ?el=); null mostra a orientacao de vazio. */
  element: Element | null
  elements: Element[]
  relationships: Relationship[]
  /** Testes do projeto (RF9); numa versao, os testes guardados nela. */
  tests: VersionedTest[]
  canEdit: boolean
  /** Seleciona outro elemento (ao clicar num vinculo) ou limpa a selecao (null, apos excluir). */
  onSelect: (elementId: string | null) => void
  /** Texto do estado vazio, conforme a visualizacao. */
  emptyHint: string
  /**
   * Elemento de uma versao (RF18): so leitura e sem a situacao (rascunho/submetido). Os testes sao os da versao
   * (versoes anteriores aos testes, RF9, nao tem nenhum).
   */
  fromVersion?: boolean
}

type PanelAction =
  | { mode: 'editElement' }
  | { mode: 'deleteElement' }
  | { mode: 'addRelationship' }
  | { mode: 'editRelationship'; relationship: Relationship }
  | { mode: 'removeRelationship'; relationship: Relationship }
  | { mode: 'addTest' }
  | { mode: 'editTest'; test: TestCase }
  | { mode: 'deleteTest'; test: TestCase }

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
  tests,
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
  // RF13: testes que cobrem este requisito
  const ownTests = tests.filter((t) => t.covers.some((e) => e.id === element.id))
  const editable = canEdit && !fromVersion

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
        <div className="ep-stats">
          <div><b className="mono">{dependsOn}</b><span className="hint">Depende de</span></div>
          <div><b className="mono">{dependents}</b><span className="hint">Dependentes</span></div>
          <div>
            <b className={`mono${ownTests.length > 0 ? ' ok' : ''}`}>{isRule ? '—' : ownTests.length}</b>
            <span className="hint">{ownTests.length === 1 ? 'Teste' : 'Testes'}</span>
          </div>
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

        {!isRule && (
          <>
            <div className="ep-section-head ep-section-sep">
              <span>Testes <span className="mono hint">{ownTests.length}</span></span>
              {editable && (
                <button className="btn sm" type="button" onClick={() => setAction({ mode: 'addTest' })}>
                  <PlusIcon />Associar teste
                </button>
              )}
            </div>
            {/* RF13: indicacao em texto de requisito sem teste */}
            {ownTests.length === 0 && (
              <p className={fromVersion ? 'hint' : 'hint ep-untested'}>
                {fromVersion ? 'Nenhum teste nesta versão.' : 'Nenhum teste associado: este requisito não está coberto.'}
              </p>
            )}
            {ownTests.map((t) =>
              editable ? (
                <TestItem
                  key={t.id}
                  test={t}
                  onEdit={() => setAction({ mode: 'editTest', test: t as TestCase })}
                  onDelete={() => setAction({ mode: 'deleteTest', test: t as TestCase })}
                />
              ) : (
                <TestItem key={t.id} test={t} />
              ),
            )}
          </>
        )}
      </div>
    </aside>

      {action?.mode === 'editElement' && (
        <ElementFormDrawer
          projectId={projectId}
          element={element}
          relationships={relationships}
          tests={tests}
          onClose={close}
          onSaved={close}
        />
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

      {(action?.mode === 'addTest' || action?.mode === 'editTest') && (
        <TestDialog
          projectId={projectId}
          elements={elements}
          tests={tests as TestCase[]}
          test={action.mode === 'editTest' ? action.test : undefined}
          forElement={action.mode === 'addTest' ? element : undefined}
          onClose={close}
        />
      )}

      {action?.mode === 'deleteTest' && <DeleteTestDialog projectId={projectId} test={action.test} onClose={close} />}

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
