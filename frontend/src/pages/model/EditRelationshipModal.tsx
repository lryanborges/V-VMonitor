import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ApiError } from '../../api/client'
import { elementKeys } from '../../api/elements'
import { projectKeys } from '../../api/projects'
import { relationshipKeys, relationshipsApi } from '../../api/relationships'
import type { Relationship, RelationshipType } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { Modal } from '../../components/Modal'
import { relationshipExists, typeMeta } from '../../utils/relationships'
import { RelationshipSentence } from './RelationshipSentence'
import { RelationshipTypePicker } from './RelationshipTypePicker'

interface EditRelationshipModalProps {
  projectId: string
  relationship: Relationship
  relationships: Relationship[]
  onClose: () => void
  onSaved: () => void
}

/**
 * RF14: troca o tipo e/ou inverte a direcao de uma relacao. As pontas continuam as mesmas;
 * para ligar a outro elemento, remove-se a relacao e cria-se outra.
 */
export function EditRelationshipModal({ projectId, relationship, relationships, onClose, onSaved }: EditRelationshipModalProps) {
  const queryClient = useQueryClient()
  const [type, setType] = useState<RelationshipType>(relationship.type)
  const [reversed, setReversed] = useState(false)

  const effectiveReversed = reversed && !type.symmetric
  const [source, target] = effectiveReversed
    ? [relationship.target, relationship.source]
    : [relationship.source, relationship.target]
  const others = relationships.filter((r) => r.id !== relationship.id)
  const unchanged = type.id === relationship.type.id && !effectiveReversed

  const save = useMutation({
    mutationFn: () => relationshipsApi.update(projectId, relationship.id, { typeId: type.id, reversed: effectiveReversed }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: relationshipKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: elementKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: projectKeys.all })
      onSaved()
    },
  })

  const duplicateMessage = (t: RelationshipType) =>
    relationshipExists(others, source.id, target.id, t)
      ? `${source.code} já ${typeMeta(t).verb} ${target.code}. Registro duplicado não é permitido.`
      : null

  const error = save.error instanceof ApiError ? save.error : null
  const canSubmit = !unchanged && !duplicateMessage(type) && !save.isPending

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (canSubmit) save.mutate()
  }

  return (
    <Modal
      title="Editar relacionamento"
      size="lg"
      subtitle="Troque o tipo ou inverta a direção. Para ligar a outro elemento, remova este relacionamento e crie outro."
      onClose={onClose}
      footer={
        <>
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button className="btn p" type="submit" form="edit-rel" disabled={!canSubmit}>
            {save.isPending ? 'Salvando…' : 'Salvar alterações'}
          </button>
        </>
      }
    >
      <form id="edit-rel" onSubmit={handleSubmit} noValidate style={{ display: 'contents' }}>
        <RelationshipSentence
          source={source}
          target={target}
          type={type}
          onSwap={() => {
            setReversed((v) => !v)
            save.reset()
          }}
        />
        <section>
          <div className="st">Tipo de relacionamento</div>
          <RelationshipTypePicker
            projectId={projectId}
            value={type.id}
            onChange={(t) => {
              setType(t)
              save.reset()
            }}
            duplicateMessage={duplicateMessage}
          />
        </section>
        {error && <FieldError>{error.message}</FieldError>}
      </form>
    </Modal>
  )
}
