import { useMutation } from '@tanstack/react-query'
import { ApiError } from '../../../api/client'
import { testsApi } from '../../../api/tests'
import type { TestCase } from '../../../api/types'
import { FieldError } from '../../../components/FieldError'
import { TrashIcon } from '../../../components/icons'
import { Modal } from '../../../components/Modal'
import { useRefreshAfterTestChange } from './useTestMutations'

interface DeleteTestDialogProps {
  projectId: string
  test: TestCase
  onClose: () => void
}

/** Confirma a exclusao de um teste; os requisitos que ele cobre continuam no modelo. */
export function DeleteTestDialog({ projectId, test, onClose }: DeleteTestDialogProps) {
  const refresh = useRefreshAfterTestChange(projectId)
  const remove = useMutation({
    mutationFn: () => testsApi.remove(projectId, test.id),
    onSuccess: () => {
      refresh()
      onClose()
    },
  })

  const covers = test.covers.map((e) => e.code).join(', ')
  return (
    <Modal
      title={`Excluir ${test.code}?`}
      icon={<TrashIcon />}
      tone="danger"
      subtitle={test.description}
      onClose={onClose}
      footer={
        <>
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button className="btn d" type="button" disabled={remove.isPending} onClick={() => remove.mutate()}>
            {remove.isPending ? 'Excluindo…' : `Excluir ${test.code}`}
          </button>
        </>
      }
    >
      <p className="hint" style={{ margin: 0 }}>
        {test.covers.length === 0
          ? 'O teste não cobre nenhum requisito.'
          : `${covers} ${test.covers.length === 1 ? 'deixa' : 'deixam'} de ter este teste associado. Os requisitos continuam no modelo.`}
      </p>
      {remove.error instanceof ApiError && <FieldError>{remove.error.message}</FieldError>}
    </Modal>
  )
}
