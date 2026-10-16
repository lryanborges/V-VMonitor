import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { useAuth } from '../../auth/AuthContext'
import { ApiError } from '../../api/client'
import { memberKeys, membersApi } from '../../api/members'
import { projectKeys } from '../../api/projects'
import type { Member, Project } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { CloseIcon, MembersIcon } from '../../components/icons'
import { Modal } from '../../components/Modal'
import { initials } from '../../utils/format'
import { ROLE_LABEL } from '../../utils/labels'
import './ShareProjectDialog.css'

interface ShareProjectDialogProps {
  project: Project
  onClose: () => void
}

const FORM_ID = 'share-project-form'

/**
 * RF20: pessoas com acesso ao projeto. O proprietario adiciona pessoas ja cadastradas pelo e-mail (sempre como
 * editoras) e remove editores; cada editor pode sair do projeto. O proprietario nao sai.
 */
export function ShareProjectDialog({ project, onClose }: ShareProjectDialogProps) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [email, setEmail] = useState('')
  // remover e sair pedem um segundo clique para confirmar
  const [confirming, setConfirming] = useState<string | null>(null)
  const isOwner = project.role === 'OWNER'

  const members = useQuery({ queryKey: memberKeys.all(project.id), queryFn: () => membersApi.list(project.id) })

  // o card do projeto e o menu lateral mostram os membros
  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: memberKeys.all(project.id) })
    queryClient.invalidateQueries({ queryKey: projectKeys.detail(project.id), exact: true })
    queryClient.invalidateQueries({ queryKey: projectKeys.all, exact: true })
  }

  const add = useMutation({
    mutationFn: () => membersApi.add(project.id, email.trim()),
    onSuccess: () => {
      setEmail('')
      refresh()
    },
  })

  const remove = useMutation({
    mutationFn: (member: Member) => membersApi.remove(project.id, member.userId),
    onSuccess: (_, member) => {
      setConfirming(null)
      if (member.userId === user?.id) {
        // saiu do projeto: perde o acesso, volta para a lista
        queryClient.removeQueries({ queryKey: projectKeys.detail(project.id) })
        queryClient.invalidateQueries({ queryKey: projectKeys.all, exact: true })
        navigate('/projetos', { replace: true })
        return
      }
      refresh()
    },
  })

  const addError = add.error instanceof ApiError ? add.error : null
  const emailError = addError?.fieldErrors.email ?? (addError && addError.status === 409 ? addError.message : undefined)
  const generalAddError = addError && !emailError ? addError.message : undefined

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (email.trim()) add.mutate()
  }

  return (
    <Modal
      title={`Compartilhar “${project.name}”`}
      icon={<MembersIcon />}
      subtitle={
        isOwner
          ? 'Quem for adicionado pelo e-mail passa a editar o projeto. A pessoa precisa ter uma conta no V&V Monitor.'
          : 'Só o proprietário adiciona pessoas a este projeto.'
      }
      onClose={onClose}
      footer={
        <>
          <button className="btn p" type="button" onClick={onClose}>Concluído</button>
        </>
      }
    >
      {isOwner && (
        <form id={FORM_ID} className="sp-form" onSubmit={handleSubmit} noValidate>
          <div className="sp-field">
            <label className="lbl" htmlFor="sp-email">E-mail</label>
            <input
              id="sp-email"
              className={`inp${emailError ? ' err' : ''}`}
              type="email"
              placeholder="nome@exemplo.com"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value)
                if (add.error) add.reset()
              }}
              aria-invalid={!!emailError}
              autoFocus
            />
          </div>
          <div className="sp-role">
            <span className="lbl">Permissão</span>
            <span className="sp-role-fixed">{ROLE_LABEL.EDITOR}</span>
          </div>
          <button className="btn p" type="submit" disabled={!email.trim() || add.isPending}>
            {add.isPending ? 'Adicionando…' : 'Adicionar'}
          </button>
        </form>
      )}
      {emailError && <FieldError>{emailError}</FieldError>}
      {generalAddError && <FieldError>{generalAddError}</FieldError>}

      <section className="sp-people">
        <span className="eyebrow">Pessoas com acesso</span>
        {members.isPending && <p className="hint">Carregando…</p>}
        {members.isError && <FieldError>{members.error.message}</FieldError>}
        <ul>
          {members.data?.map((m) => {
            const me = m.userId === user?.id
            const canRemove = m.role !== 'OWNER' && (isOwner || me)
            const pending = remove.isPending && remove.variables?.userId === m.userId
            return (
              <li key={m.userId} className="sp-person">
                <span className="av">{initials(m.name)}</span>
                <span className="sp-who">
                  <span className="sp-name">{m.name}{me && <span className="hint"> (você)</span>}</span>
                  <span className="hint sp-email">{m.email}</span>
                </span>
                <span className="hint sp-person-role">{ROLE_LABEL[m.role]}</span>
                {canRemove &&
                  (confirming === m.userId ? (
                    <span className="sp-confirm">
                      <button className="btn sm" type="button" onClick={() => setConfirming(null)}>Cancelar</button>
                      <button className="btn sm d" type="button" disabled={pending} onClick={() => remove.mutate(m)}>
                        {me ? 'Confirmar saída' : 'Confirmar'}
                      </button>
                    </span>
                  ) : me ? (
                    <button className="btn sm g sp-remove" type="button" onClick={() => setConfirming(m.userId)}>
                      Sair do projeto
                    </button>
                  ) : (
                    <button
                      className="btn ic g sm sp-remove"
                      type="button"
                      title="Remover acesso"
                      aria-label={`Remover acesso de ${m.name}`}
                      onClick={() => setConfirming(m.userId)}
                    >
                      <CloseIcon />
                    </button>
                  ))}
              </li>
            )
          })}
        </ul>
        {remove.error instanceof ApiError && <FieldError>{remove.error.message}</FieldError>}
      </section>
    </Modal>
  )
}
