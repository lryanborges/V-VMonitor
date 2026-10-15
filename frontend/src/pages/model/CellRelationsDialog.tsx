import type { Element, Relationship } from '../../api/types'
import { EditIcon, PlusIcon, TrashIcon } from '../../components/icons'
import { Modal } from '../../components/Modal'
import { kindInfo } from '../../utils/labels'
import { RelationshipSentence } from './RelationshipSentence'

interface CellRelationsDialogProps {
  row: Element
  column: Element
  relationships: Relationship[]
  canEdit: boolean
  onClose: () => void
  onAdd: () => void
  onEdit: (relationship: Relationship) => void
  onRemove: (relationship: Relationship) => void
}

/** Relacoes entre dois elementos (uma celula da matriz), com as acoes de trocar tipo, inverter e remover. */
export function CellRelationsDialog({
  row,
  column,
  relationships,
  canEdit,
  onClose,
  onAdd,
  onEdit,
  onRemove,
}: CellRelationsDialogProps) {
  return (
    <Modal
      title="Relacionamentos"
      subtitle={
        <>
          <span className={`chip ${kindInfo(row.kind).chip}`}>{row.code}</span>
          <span>e</span>
          <span className={`chip ${kindInfo(column.kind).chip}`}>{column.code}</span>
        </>
      }
      onClose={onClose}
      footer={
        <>
          {canEdit && (
            <button className="btn" type="button" onClick={onAdd}>
              <PlusIcon />
              Adicionar outro
            </button>
          )}
          <button className="btn p" type="button" onClick={onClose}>Fechar</button>
        </>
      }
    >
      {relationships.map((r) => (
        <div key={r.id} className="cr-item">
          <RelationshipSentence source={r.source} target={r.target} type={r.type} />
          <div className="cr-meta">
            {r.submissionStatus === 'DRAFT' ? (
              <span className="pill w">Pendente de submissão</span>
            ) : (
              <span className="pill ok">Submetido</span>
            )}
            <div style={{ flex: 1 }} />
            {canEdit && (
              <>
                <button className="btn sm" type="button" onClick={() => onEdit(r)}>
                  <EditIcon />
                  Trocar tipo ou inverter
                </button>
                <button className="btn sm" type="button" style={{ color: 'var(--wr)' }} onClick={() => onRemove(r)}>
                  <TrashIcon />
                  Remover
                </button>
              </>
            )}
          </div>
        </div>
      ))}
    </Modal>
  )
}
