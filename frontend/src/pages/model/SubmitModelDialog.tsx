import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '../../api/client'
import { elementKeys } from '../../api/elements'
import { projectKeys, projectsApi } from '../../api/projects'
import { relationshipKeys } from '../../api/relationships'
import type { Element, Relationship } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { AlertIcon, UploadIcon } from '../../components/icons'
import { Modal } from '../../components/Modal'
import { kindInfo } from '../../utils/labels'
import { typeMeta } from '../../utils/relationships'
import './SubmitModelDialog.css'

interface SubmitModelDialogProps {
  projectId: string
  elements: Element[]
  relationships: Relationship[]
  onClose: () => void
  onSubmitted: () => void
}

const plural = (n: number, one: string, many: string) => `${n} ${n === 1 ? one : many}`

/**
 * RF10: confirma a submissao do modelo, resumindo o que vai entrar no grafo e na matriz.
 * Avisa (sem impedir) sobre elementos que ficariam sem nenhum relacionamento.
 */
export function SubmitModelDialog({ projectId, elements, relationships, onClose, onSubmitted }: SubmitModelDialogProps) {
  const queryClient = useQueryClient()
  const draftElements = elements.filter((e) => e.submissionStatus === 'DRAFT')
  const draftRelationships = relationships.filter((r) => r.submissionStatus === 'DRAFT')
  const isolated = elements.filter((e) => e.links === 0)

  const submit = useMutation({
    mutationFn: () => projectsApi.submit(projectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: elementKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: relationshipKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: projectKeys.all })
      onSubmitted()
    },
  })
  const error = submit.error instanceof ApiError ? submit.error : null

  return (
    <Modal
      title="Submeter modelo"
      icon={<UploadIcon />}
      subtitle="As alterações pendentes passam a fazer parte do modelo e aparecem no grafo e na matriz."
      onClose={onClose}
      footer={
        <>
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button className="btn p" type="button" disabled={submit.isPending} onClick={() => submit.mutate()}>
            <UploadIcon />
            {submit.isPending ? 'Submetendo…' : 'Submeter modelo'}
          </button>
        </>
      }
    >
      <div className="sm-summary">
        <div className="stat"><b>{draftElements.length}</b><span>{draftElements.length === 1 ? 'Elemento' : 'Elementos'}</span></div>
        <div className="stat"><b>{draftRelationships.length}</b><span>{draftRelationships.length === 1 ? 'Relacionamento' : 'Relacionamentos'}</span></div>
      </div>

      {draftElements.length > 0 && (
        <section className="sm-section">
          <span className="eyebrow">Elementos novos ou editados</span>
          <div className="sm-chips">
            {draftElements.map((e) => (
              <span key={e.id} className={`chip ${kindInfo(e.kind).chip}`} title={e.description}>{e.code}</span>
            ))}
          </div>
        </section>
      )}

      {draftRelationships.length > 0 && (
        <section className="sm-section">
          <span className="eyebrow">Relacionamentos novos ou editados</span>
          <ul className="sm-rels">
            {draftRelationships.map((r) => (
              <li key={r.id}>
                <span className="mono">{r.source.code}</span> {typeMeta(r.type).verb.replace(/^—|→$/g, '')}{' '}
                <span className="mono">{r.target.code}</span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* aviso de qualidade do modelo: nao impede a submissao */}
      {isolated.length > 0 && (
        <div className="sm-warning">
          <AlertIcon />
          <span>
            {plural(isolated.length, 'elemento está', 'elementos estão')} sem nenhum relacionamento:{' '}
            <span className="mono">{isolated.map((e) => e.code).join(', ')}</span>. Você pode submeter mesmo assim.
          </span>
        </div>
      )}

      {error && <FieldError>{error.message}</FieldError>}
    </Modal>
  )
}
