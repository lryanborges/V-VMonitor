import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { ApiError } from '../../api/client'
import { relationshipKeys, relationshipsApi } from '../../api/relationships'
import type { RelationshipType } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { PlusIcon } from '../../components/icons'
import { RelationLine } from '../../components/RelationLine'
import { sortTypes, typeMeta } from '../../utils/relationships'
import './RelationshipForm.css'

interface RelationshipTypePickerProps {
  projectId: string
  value: string | null
  onChange: (type: RelationshipType) => void
  /** Mensagem de "Já existe" para tipos que gerariam relacao duplicada; null quando o tipo esta livre. */
  duplicateMessage: (type: RelationshipType) => string | null
}

/**
 * Escolha do tipo de relacionamento (RF8, UC-06 passos 4 e 5), com a criacao de tipo novo
 * (UC-06, sequencia alternativa). Usado ao adicionar e ao editar uma relacao.
 */
export function RelationshipTypePicker({ projectId, value, onChange, duplicateMessage }: RelationshipTypePickerProps) {
  const queryClient = useQueryClient()
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')

  const types = useQuery({
    queryKey: relationshipKeys.types(projectId),
    queryFn: () => relationshipsApi.listTypes(projectId),
  })

  const createType = useMutation({
    mutationFn: () => relationshipsApi.createType(projectId, name),
    onSuccess: (created) => {
      queryClient.setQueryData<RelationshipType[]>(relationshipKeys.types(projectId), (old = []) => [...old, created])
      onChange(created)
      setCreating(false)
      setName('')
    },
  })
  const typeError = createType.error instanceof ApiError ? createType.error : null

  return (
    <>
      {types.isPending && <p className="hint">Carregando tipos…</p>}
      <div className="arm-types" role="group" aria-label="Tipo de relacionamento">
        {sortTypes(types.data ?? []).map((t) => {
          const meta = typeMeta(t)
          const duplicate = duplicateMessage(t)
          return (
            <button
              key={t.id}
              className="ty"
              type="button"
              aria-pressed={t.id === value && !duplicate}
              disabled={!!duplicate}
              onClick={() => onChange(t)}
            >
              <span className="h">
                <RelationLine style={meta.line} />
                {t.name}
                {duplicate && <span className="tag">Já existe</span>}
              </span>
              <span className="hint">{duplicate ?? meta.hint}</span>
            </button>
          )
        })}
      </div>

      {creating ? (
        <div className="arm-newtype">
          <label htmlFor="rl-novo-tipo" className="sr-only">Nome do novo tipo</label>
          <input
            id="rl-novo-tipo"
            className={`inp sm${typeError ? ' err' : ''}`}
            placeholder="Nome do relacionamento, ex.: Valida"
            maxLength={60}
            value={name}
            onChange={(e) => {
              setName(e.target.value)
              createType.reset()
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault()
                createType.mutate()
              }
            }}
            autoFocus
          />
          <button className="btn sm p" type="button" disabled={createType.isPending} onClick={() => createType.mutate()}>
            Criar
          </button>
          <button className="btn sm g" type="button" onClick={() => setCreating(false)}>Cancelar</button>
        </div>
      ) : (
        <button className="arm-newtype-btn" type="button" onClick={() => setCreating(true)}>
          <PlusIcon />
          Criar novo tipo de relacionamento
        </button>
      )}
      {typeError && <FieldError>{typeError.fieldErrors.name ?? typeError.message}</FieldError>}
    </>
  )
}
