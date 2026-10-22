import type { VersionedTest } from '../../../api/types'
import { EditIcon, TrashIcon } from '../../../components/icons'
import './Tests.css'

interface TestItemProps {
  test: VersionedTest
  /** Sem as acoes: visualizador ou versao antiga. */
  onEdit?: () => void
  onDelete?: () => void
}

/** Teste no painel do elemento, ex.: "T7 Relacionar dois requisitos...", com editar e excluir ao passar o mouse. */
export function TestItem({ test, onEdit, onDelete }: TestItemProps) {
  return (
    <div className="tt-row">
      <span className="chip t">{test.code}</span>
      <span className="desc-full">{test.description}</span>
      {(onEdit || onDelete) && (
        <span className="tt-actions">
          {onEdit && (
            <button className="btn ic g sm" type="button" title="Editar teste" aria-label={`Editar ${test.code}`} onClick={onEdit}>
              <EditIcon />
            </button>
          )}
          {onDelete && (
            <button className="btn ic g sm" type="button" title="Excluir teste" aria-label={`Excluir ${test.code}`} onClick={onDelete}>
              <TrashIcon />
            </button>
          )}
        </span>
      )}
    </div>
  )
}
