import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'
import { ApiError } from '../../api/client'
import { elementKeys, elementsApi } from '../../api/elements'
import { projectKeys } from '../../api/projects'
import type { Element, ElementKind, Priority } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { CloseIcon, InfoIcon } from '../../components/icons'
import { KINDS, PRIORITIES, kindInfo } from '../../utils/labels'
import './NewElementDrawer.css'

const FORM_ID = 'novo-elemento'

interface NewElementDrawerProps {
  projectId: string
  onClose: () => void
  onCreated: (element: Element) => void
}

/** RF5 e RF6, UC-05 e UC-07: cadastrar requisito ou regra de negocio, em painel lateral. */
export function NewElementDrawer({ projectId, onClose, onCreated }: NewElementDrawerProps) {
  const queryClient = useQueryClient()
  const [kind, setKind] = useState<ElementKind>('FUNCTIONAL')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState<Priority | null>(null)
  const isRule = kind === 'BUSINESS_RULE'
  const info = kindInfo(kind)

  // previa do proximo ID; nao reserva o numero (o definitivo vem na resposta do cadastro)
  const nextCode = useQuery({
    queryKey: elementKeys.nextCode(projectId, kind),
    queryFn: () => elementsApi.nextCode(projectId, kind),
  })

  const create = useMutation({
    mutationFn: () => elementsApi.create(projectId, { kind, description, priority: isRule ? null : priority }),
    onSuccess: (element) => {
      queryClient.invalidateQueries({ queryKey: elementKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'next-code'] })
      queryClient.invalidateQueries({ queryKey: projectKeys.all })
      onCreated(element)
    },
  })

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose])

  const error = create.error instanceof ApiError ? create.error : null
  const fieldErrors = error?.fieldErrors ?? {}
  const generalError = error && Object.keys(fieldErrors).length === 0 ? error.message : null

  function changeKind(next: ElementKind) {
    setKind(next)
    create.reset()
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    create.mutate()
  }

  return (
    <div className="drawer-root">
      <div className="drawer-overlay" onClick={onClose} />
      <div className="drawer" role="dialog" aria-modal="true" aria-labelledby="nr-t">
        <div className="drawer-head">
          <h2 id="nr-t">Novo elemento</h2>
          <button className="btn ic g" type="button" aria-label="Fechar" onClick={onClose}>
            <CloseIcon />
          </button>
        </div>

        <form id={FORM_ID} className="drawer-body" onSubmit={handleSubmit} noValidate>
          <div>
            <span className="lbl" id="nr-tipo">Tipo</span>
            <div className="tp" role="group" aria-labelledby="nr-tipo">
              {KINDS.map((k) => (
                <button key={k.kind} type="button" aria-pressed={kind === k.kind} onClick={() => changeKind(k.kind)}>
                  {k.label}
                  <span>{k.prefix}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="nr-id">
            <span className={`chip ${info.chip} nr-id-code`}>{nextCode.data?.code ?? `${info.prefix}…`}</span>
            <div>
              <div className="nr-id-t">Identificador gerado automaticamente</div>
              <div className="hint">
                Próximo número livre entre {isRule ? 'as regras de negócio' : `os requisitos ${info.plural.toLowerCase()}`}.
              </div>
            </div>
          </div>

          <div>
            <label className="lbl" htmlFor="nr-desc">Descrição</label>
            <textarea
              id="nr-desc"
              className={`inp${fieldErrors.description ? ' err' : ''}`}
              rows={4}
              value={description}
              onChange={(e) => {
                setDescription(e.target.value)
                // o erro some assim que o ator volta a editar
                if (error) create.reset()
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
              <span className="lbl" id="nr-prio">Prioridade</span>
              <div role="group" aria-labelledby="nr-prio" className="opts">
                {PRIORITIES.map((p) => (
                  <button
                    key={p.value}
                    className="opt"
                    type="button"
                    aria-pressed={priority === p.value}
                    onClick={() => {
                      setPriority(p.value)
                      if (error) create.reset()
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
            <span>O elemento entra no modelo atual. Ele aparece no grafo e na matriz depois que você submeter o modelo.</span>
          </div>
        </form>

        <div className="drawer-foot">
          {/* UC-05 e UC-07, sequencia alternativa: cancelar volta para o modelo */}
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button className="btn p" type="submit" form={FORM_ID} disabled={create.isPending}>
            {create.isPending ? 'Cadastrando…' : isRule ? 'Cadastrar regra' : 'Cadastrar requisito'}
          </button>
        </div>
      </div>
    </div>
  )
}
