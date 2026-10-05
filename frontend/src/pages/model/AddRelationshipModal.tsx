import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState, type FormEvent } from 'react'
import { ApiError } from '../../api/client'
import { elementKeys } from '../../api/elements'
import { projectKeys } from '../../api/projects'
import { relationshipKeys, relationshipsApi } from '../../api/relationships'
import type { Element, Relationship, RelationshipType } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { SearchIcon, TickIcon } from '../../components/icons'
import { Modal } from '../../components/Modal'
import { kindInfo } from '../../utils/labels'
import { relationshipExists, typeMeta } from '../../utils/relationships'
import { RelationshipSentence } from './RelationshipSentence'
import { RelationshipTypePicker } from './RelationshipTypePicker'
import './AddRelationshipModal.css'

interface AddRelationshipModalProps {
  projectId: string
  origin: Element
  elements: Element[]
  relationships: Relationship[]
  onClose: () => void
  onCreated: () => void
}

function normalize(text: string) {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

/** RF7 e RF8, UC-06: relacionar o elemento de origem a outro, escolhendo o tipo. */
export function AddRelationshipModal({
  projectId,
  origin,
  elements,
  relationships,
  onClose,
  onCreated,
}: AddRelationshipModalProps) {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [targetId, setTargetId] = useState<string | null>(null)
  const [type, setType] = useState<RelationshipType | null>(null)
  // a origem e o sujeito da frase; inverter troca as pontas em tipos com direcao
  const [inverted, setInverted] = useState(false)

  // UC-06 passo 2: demais requisitos e regras do projeto, com identificadores
  const candidates = useMemo(() => {
    const term = normalize(search.trim())
    return elements.filter(
      (e) => e.id !== origin.id && (!term || normalize(`${e.code} ${e.description}`).includes(term)),
    )
  }, [elements, origin.id, search])

  const target = elements.find((e) => e.id === targetId) ?? null
  const swapped = inverted && !!type && !type.symmetric && !!target
  const [source, dest] = swapped ? [target!, origin] : [origin, target]

  const create = useMutation({
    mutationFn: () => relationshipsApi.create(projectId, { sourceId: source.id, targetId: dest!.id, typeId: type!.id }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: relationshipKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: elementKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: projectKeys.all })
      onCreated()
    },
  })

  const duplicateMessage = (t: RelationshipType) =>
    dest && relationshipExists(relationships, source.id, dest.id, t)
      ? `${source.code} já ${typeMeta(t).verb} ${dest.code}. Registro duplicado não é permitido.`
      : null

  const error = create.error instanceof ApiError ? create.error : null
  const canSubmit = !!dest && !!type && !duplicateMessage(type) && !create.isPending

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (canSubmit) create.mutate()
  }

  return (
    <Modal
      title="Adicionar relacionamento"
      size="lg"
      subtitle={
        <>
          <span>Origem</span>
          <span className={`chip ${kindInfo(origin.kind).chip}`}>{origin.code}</span>
          <span className="arm-origin-desc">{origin.description}</span>
        </>
      }
      onClose={onClose}
      footer={
        <>
          {/* UC-06: cancelar fecha a interface sem registrar */}
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button className="btn p" type="submit" form="add-rel" disabled={!canSubmit}>
            {create.isPending ? 'Adicionando…' : 'Adicionar relacionamento'}
          </button>
        </>
      }
    >
      <form id="add-rel" onSubmit={handleSubmit} noValidate style={{ display: 'contents' }}>
        <section>
          <div className="st"><b>1</b>Elemento de destino</div>
          <div className="arm-search">
            <SearchIcon />
            <label htmlFor="rl-busca" className="sr-only">Buscar elemento</label>
            <input
              id="rl-busca"
              className="inp sm with-icon"
              type="search"
              placeholder="Buscar por ID ou descrição"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              autoFocus
            />
          </div>
          <div className="arm-list" role="group" aria-label="Elementos disponíveis">
            {candidates.length === 0 && (
              <p className="hint arm-empty">
                {elements.length <= 1 ? 'Cadastre outro elemento para poder relacionar.' : 'Nenhum elemento encontrado.'}
              </p>
            )}
            {candidates.map((e) => (
              <button
                key={e.id}
                className="it"
                type="button"
                aria-pressed={e.id === targetId}
                onClick={() => {
                  setTargetId(e.id)
                  create.reset()
                }}
              >
                <span className={`chip ${kindInfo(e.kind).chip}`}>{e.code}</span>
                <span className="d">{e.description}</span>
                {e.id === targetId && <TickIcon style={{ color: 'var(--ac)' }} />}
              </button>
            ))}
          </div>
        </section>

        <section>
          <div className="st"><b>2</b>Tipo de relacionamento</div>
          <RelationshipTypePicker
            projectId={projectId}
            value={type?.id ?? null}
            onChange={(t) => {
              setType(t)
              create.reset()
            }}
            duplicateMessage={duplicateMessage}
          />
        </section>

        {dest && type && (
          <RelationshipSentence
            source={source}
            target={dest}
            type={type}
            onSwap={() => {
              setInverted((v) => !v)
              create.reset()
            }}
          />
        )}

        {/* UC-06, excecoes do passo 6: duplicado ou falha ao registrar */}
        {error && <FieldError>{error.fieldErrors.targetId ?? error.message}</FieldError>}
      </form>
    </Modal>
  )
}
