import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../api/client'
import { elementKeys } from '../../api/elements'
import { projectKeys } from '../../api/projects'
import { relationshipKeys, relationshipsApi } from '../../api/relationships'
import type { Relationship } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { TrashIcon } from '../../components/icons'
import { Modal } from '../../components/Modal'
import { RelationshipSentence } from './RelationshipSentence'

interface RemoveRelationshipDialogProps {
  projectId: string
  relationship: Relationship
  onClose: () => void
  onRemoved: () => void
}

/** RF15: remove uma relacao, com confirmacao simples (os dois elementos continuam no modelo). */
export function RemoveRelationshipDialog({ projectId, relationship, onClose, onRemoved }: RemoveRelationshipDialogProps) {
  const queryClient = useQueryClient()
  const remove = useMutation({
    mutationFn: () => relationshipsApi.remove(projectId, relationship.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: relationshipKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: elementKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: projectKeys.all })
      onRemoved()
    },
  })
  const error = remove.error instanceof ApiError ? remove.error : null

  return (
    <Modal
      title="Remover relacionamento?"
      tone="danger"
      icon={<TrashIcon />}
      subtitle={`${relationship.source.code} e ${relationship.target.code} continuam no modelo; só o vínculo entre eles é removido.`}
      onClose={onClose}
      footer={
        <>
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button className="btn d" type="button" disabled={remove.isPending} onClick={() => remove.mutate()}>
            <TrashIcon />
            {remove.isPending ? 'Removendo…' : 'Remover'}
          </button>
        </>
      }
    >
      <RelationshipSentence source={relationship.source} target={relationship.target} type={relationship.type} />
      {error && <FieldError>{error.message}</FieldError>}
    </Modal>
  )
}
