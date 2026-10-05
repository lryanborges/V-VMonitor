import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ApiError } from '../../api/client'
import { elementKeys, elementsApi } from '../../api/elements'
import { projectKeys } from '../../api/projects'
import type { Element, ElementKind, Priority } from '../../api/types'
import { Drawer } from '../../components/Drawer'
import { FieldError } from '../../components/FieldError'
import { InfoIcon } from '../../components/icons'
import { KINDS, PRIORITIES, kindInfo } from '../../utils/labels'
import './ElementFormDrawer.css'

const FORM_ID = 'form-elemento'

interface ElementFormDrawerProps {
  projectId: string
  /** Ausente: cadastra um novo elemento. Presente: edita este elemento (RF14). */
  element?: Element
  onClose: () => void
  onSaved: (element: Element) => void
}

/**
 * Cadastro (RF5, RF6, UC-05, UC-07) e edicao (RF14) de requisitos e regras de negocio, em painel lateral.
 * Na edicao, tipo e codigo ficam fixos: so descricao e prioridade mudam.
 */
export function ElementFormDrawer({ projectId, element, onClose, onSaved }: ElementFormDrawerProps) {
  const queryClient = useQueryClient()
  const editing = !!element
  const [kind, setKind] = useState<ElementKind>(element?.kind ?? 'FUNCTIONAL')
  const [description, setDescription] = useState(element?.description ?? '')
  const [priority, setPriority] = useState<Priority | null>(element?.priority ?? null)
  const isRule = kind === 'BUSINESS_RULE'
  const info = kindInfo(kind)

  // previa do proximo ID (so no cadastro); nao reserva o numero, o definitivo vem na resposta
  const nextCode = useQuery({
    queryKey: elementKeys.nextCode(projectId, kind),
    queryFn: () => elementsApi.nextCode(projectId, kind),
    enabled: !editing,
  })

  const save = useMutation({
    mutationFn: () => {
      const data = { description, priority: isRule ? null : priority }
      return element
        ? elementsApi.update(projectId, element.id, data)
        : elementsApi.create(projectId, { kind, ...data })
    },
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: elementKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'next-code'] })
      queryClient.invalidateQueries({ queryKey: projectKeys.all })
      onSaved(saved)
    },
  })

  const error = save.error instanceof ApiError ? save.error : null
  const fieldErrors = error?.fieldErrors ?? {}
  const generalError = error && Object.keys(fieldErrors).length === 0 ? error.message : null
  const code = editing ? element.code : (nextCode.data?.code ?? `${info.prefix}…`)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    save.mutate()
  }

  const submitLabel = save.isPending
    ? 'Salvando…'
    : editing
      ? 'Salvar alterações'
      : isRule
        ? 'Cadastrar regra'
        : 'Cadastrar requisito'

  return (
    <Drawer
      title={editing ? `Editar ${element.code}` : 'Novo elemento'}
      onClose={onClose}
      footer={
        <>
          {/* sequencia alternativa dos UCs: cancelar volta para o modelo sem salvar */}
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button className="btn p" type="submit" form={FORM_ID} disabled={save.isPending}>{submitLabel}</button>
        </>
      }
    >
      <form id={FORM_ID} className="ef-form" onSubmit={handleSubmit} noValidate>
        <div>
          <span className="lbl" id="ef-tipo">Tipo</span>
          <div className="tp" role="group" aria-labelledby="ef-tipo">
            {KINDS.map((k) => (
              <button
                key={k.kind}
                type="button"
                aria-pressed={kind === k.kind}
                disabled={editing && kind !== k.kind}
                title={editing ? 'O tipo não pode ser alterado: o código depende dele.' : undefined}
                onClick={() => {
                  setKind(k.kind)
                  save.reset()
                }}
              >
                {k.label}
                <span>{k.prefix}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="nr-id">
          <span className={`chip ${info.chip} nr-id-code`}>{code}</span>
          <div>
            <div className="nr-id-t">{editing ? 'Identificador fixo' : 'Identificador gerado automaticamente'}</div>
            <div className="hint">
              {editing
                ? 'O código e o tipo identificam o elemento e não mudam na edição.'
                : `Próximo número livre entre ${isRule ? 'as regras de negócio' : `os requisitos ${info.plural.toLowerCase()}`}.`}
            </div>
          </div>
        </div>

        <div>
          <label className="lbl" htmlFor="ef-desc">Descrição</label>
          <textarea
            id="ef-desc"
            className={`inp${fieldErrors.description ? ' err' : ''}`}
            rows={4}
            value={description}
            onChange={(e) => {
              setDescription(e.target.value)
              // o erro some assim que o ator volta a editar
              if (error) save.reset()
            }}
            aria-invalid={!!fieldErrors.description}
            autoFocus
          />
          {fieldErrors.description ? (
            <FieldError>{fieldErrors.description}</FieldError>
          ) : (
            <div className="hint" style={{ marginTop: 6 }}>
              {isRule
                ? 'Descreva a regra que o sistema deve respeitar.'
                : 'Escreva no formato “Usuário deve ser capaz de…” ou “Sistema deve…”.'}
            </div>
          )}
        </div>

        {!isRule && (
          <div>
            <span className="lbl" id="ef-prio">Prioridade</span>
            <div role="group" aria-labelledby="ef-prio" className="opts">
              {PRIORITIES.map((p) => (
                <button
                  key={p.value}
                  className="opt"
                  type="button"
                  aria-pressed={priority === p.value}
                  onClick={() => {
                    setPriority(p.value)
                    if (error) save.reset()
                  }}
                >
                  <span className="rd" />
                  <span className="opt-text">
                    <b>{p.label}</b>
                    <span className="hint">{p.hint}</span>
                  </span>
                  <span className={`opt-prio p${p.level}`} aria-hidden="true"><i /><i /><i /></span>
                </button>
              ))}
            </div>
            {fieldErrors.priority && <FieldError>{fieldErrors.priority}</FieldError>}
          </div>
        )}

        {fieldErrors.kind && <FieldError>{fieldErrors.kind}</FieldError>}
        {generalError && <FieldError>{generalError}</FieldError>}

        <div className="nr-note">
          <InfoIcon />
          <span>
            {editing
              ? 'A alteração volta a ficar pendente: o grafo e a matriz mostram a versão nova depois que você submeter o modelo.'
              : 'O elemento entra no modelo atual. Ele aparece no grafo e na matriz depois que você submeter o modelo.'}
          </span>
        </div>
      </form>
    </Drawer>
  )
}
