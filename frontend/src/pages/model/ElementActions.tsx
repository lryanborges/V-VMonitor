import { EditIcon, TrashIcon } from '../../components/icons'

interface ElementActionsProps {
  /** RF14: editar descricao e prioridade. */
  onEdit: () => void
  /** RF15/RF16: excluir com alerta de impacto. */
  onDelete: () => void
}

/** Botoes "Editar" e "Excluir" do elemento, usados nos paineis laterais da lista e do grafo. */
export function ElementActions({ onEdit, onDelete }: ElementActionsProps) {
  return (
    <div className="el-actions">
      <button className="btn sm" type="button" onClick={onEdit}>
        <EditIcon />
        Editar
      </button>
      <button className="btn sm el-delete" type="button" onClick={onDelete}>
        <TrashIcon />
        Excluir
      </button>
    </div>
  )
}
