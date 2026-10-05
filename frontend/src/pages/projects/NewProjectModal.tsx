import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { ApiError } from '../../api/client'
import { projectKeys, projectsApi } from '../../api/projects'
import { FieldError } from '../../components/FieldError'
import { Modal } from '../../components/Modal'

const FORM_ID = 'novo-projeto'

/** RF4, UC-03: criar projeto. */
export function NewProjectModal({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const create = useMutation({
    mutationFn: projectsApi.create,
    onSuccess: (project) => {
      queryClient.invalidateQueries({ queryKey: projectKeys.all })
      queryClient.setQueryData(projectKeys.detail(project.id), project)
      // UC-03, passo 6: apresenta a interface do projeto criado
      navigate(`/projetos/${project.id}`, { replace: true })
    },
  })

  const error = create.error instanceof ApiError ? create.error : null
  const nameError = error?.fieldErrors.name
  const generalError = error && !nameError ? error.message : null

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    create.mutate({ name, description })
  }

  return (
    <Modal
      title="Novo projeto"
      subtitle="Depois de criar, você já pode cadastrar requisitos e regras."
      onClose={onClose}
      footer={
        <>
          {/* UC-03, sequencia alternativa: cancelar fecha a interface de criacao */}
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button className="btn p" type="submit" form={FORM_ID} disabled={create.isPending}>
            {create.isPending ? 'Criando…' : 'Criar projeto'}
          </button>
        </>
      }
    >
      <form id={FORM_ID} onSubmit={handleSubmit} noValidate style={{ display: 'contents' }}>
        <div>
          <label className="lbl" htmlFor="np-nome">Nome do projeto</label>
          <input
            id="np-nome"
            className={`inp${nameError ? ' err' : ''}`}
            type="text"
            maxLength={150}
            placeholder="Ex.: Sistema de biblioteca"
            value={name}
            onChange={(e) => setName(e.target.value)}
            aria-invalid={!!nameError}
            autoFocus
          />
          {nameError && <FieldError>{nameError}</FieldError>}
        </div>

        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
            <label className="lbl" htmlFor="np-desc">Descrição</label>
            <span className="hint">Opcional</span>
          </div>
          <textarea
            id="np-desc"
            className="inp"
            rows={4}
            placeholder="Para que serve o sistema e quem vai usar"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>

        {/* UC-03, excecao do passo 5: falha ao armazenar */}
        {generalError && <FieldError>{generalError}</FieldError>}
      </form>
    </Modal>
  )
}
