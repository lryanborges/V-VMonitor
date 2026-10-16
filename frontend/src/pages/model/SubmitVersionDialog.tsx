import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ApiError } from '../../api/client'
import { elementKeys } from '../../api/elements'
import { projectKeys } from '../../api/projects'
import { relationshipKeys } from '../../api/relationships'
import type { Element, ModelChanges } from '../../api/types'
import { versionKeys, versionsApi } from '../../api/versions'
import { FieldError } from '../../components/FieldError'
import { AlertIcon, UploadIcon } from '../../components/icons'
import { Modal } from '../../components/Modal'
import { ModelChangesView } from './ModelChangesView'
import './SubmitVersionDialog.css'

interface SubmitVersionDialogProps {
  projectId: string
  /** Mudancas desde a ultima versao (o que entra na nova). */
  changes: ModelChanges
  elements: Element[]
  onClose: () => void
  onSubmitted: () => void
}

const FORM_ID = 'submit-version-form'
const plural = (n: number, one: string, many: string) => `${n} ${n === 1 ? one : many}`

/**
 * RF10 + RF17: submete o modelo gravando a proxima versao (v1, v2...), com nome opcional. Mostra o que mudou
 * desde a ultima versao e avisa (sem impedir) sobre elementos sem nenhum relacionamento.
 */
export function SubmitVersionDialog({ projectId, changes, elements, onClose, onSubmitted }: SubmitVersionDialogProps) {
  const queryClient = useQueryClient()
  const [label, setLabel] = useState('')
  const next = (changes.baseVersion ?? 0) + 1
  const isolated = elements.filter((e) => e.links === 0)
  const { added, modified, removed } = changes.summary

  const submit = useMutation({
    mutationFn: () => versionsApi.create(projectId, { label: label.trim() || null }),
    onSuccess: () => {
      // a situacao (rascunho) de elementos e relacoes, o cabecalho e a lista de versoes mudam
      queryClient.invalidateQueries({ queryKey: elementKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: relationshipKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: versionKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: projectKeys.all })
      onSubmitted()
    },
  })
  const error = submit.error instanceof ApiError ? submit.error : null
  const labelError = error?.fieldErrors.label

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    submit.mutate()
  }

  return (
    <Modal
      title={`Submeter versão v${next}`}
      icon={<UploadIcon />}
      size="lg"
      subtitle={
        changes.baseVersion
          ? `Grava o modelo atual como v${next}. A v${changes.baseVersion} continua guardada, sem alterações.`
          : 'Grava o modelo atual como a primeira versão do projeto.'
      }
      onClose={onClose}
      footer={
        <>
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button className="btn p" type="submit" form={FORM_ID} disabled={submit.isPending}>
            <UploadIcon />
            {submit.isPending ? 'Submetendo…' : `Submeter v${next}`}
          </button>
        </>
      }
    >
      <form id={FORM_ID} onSubmit={handleSubmit} noValidate style={{ display: 'contents' }}>
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
            <label className="lbl" htmlFor="sv-nome">Nome da versão</label>
            <span className="hint">Opcional</span>
          </div>
          <input
            id="sv-nome"
            className={`inp${labelError ? ' err' : ''}`}
            type="text"
            maxLength={100}
            placeholder="Ex.: Revisão com o cliente"
            value={label}
            onChange={(e) => setLabel(e.target.value)}
            aria-invalid={!!labelError}
            autoFocus
          />
          {labelError && <FieldError>{labelError}</FieldError>}
        </div>
      </form>

      <div className="sm-summary">
        <div className="stat add"><b>{added}</b><span>{added === 1 ? 'Adicionado' : 'Adicionados'}</span></div>
        <div className="stat mod"><b>{modified}</b><span>{modified === 1 ? 'Alterado' : 'Alterados'}</span></div>
        <div className="stat rem"><b>{removed}</b><span>{removed === 1 ? 'Removido' : 'Removidos'}</span></div>
      </div>

      <div className="sm-changes">
        <ModelChangesView elements={changes.elements} relationships={changes.relationships} />
      </div>

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

      {error && !labelError && <FieldError>{error.message}</FieldError>}
    </Modal>
  )
}
