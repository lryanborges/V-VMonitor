import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent, type ReactNode } from 'react'
import { ApiError } from '../../api/client'
import { elementKeys, elementsApi } from '../../api/elements'
import { projectKeys } from '../../api/projects'
import { relationshipKeys } from '../../api/relationships'
import type { Element, ElementSummary, ImpactLevel } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { AlertIcon, GraphIcon, InfoIcon, TrashIcon } from '../../components/icons'
import { Modal } from '../../components/Modal'
import { kindInfo } from '../../utils/labels'
import './DeleteElementDialog.css'

const LEVELS: Record<ImpactLevel, { label: string; bars: number }> = {
  LOW: { label: 'Baixo', bars: 1 },
  MEDIUM: { label: 'Médio', bars: 2 },
  HIGH: { label: 'Alto', bars: 3 },
}

/** "RF10", "RF10 e RF11", "RF1, RF2 e RF3". */
function joinCodes(items: { code: string }[]) {
  const codes = items.map((i) => i.code)
  return codes.length <= 1 ? codes.join('') : `${codes.slice(0, -1).join(', ')} e ${codes[codes.length - 1]}`
}

const Chip = ({ el }: { el: ElementSummary }) => <span className={`chip ${kindInfo(el.kind).chip}`}>{el.code}</span>

function Impact({ icon, warn, title, children }: { icon: ReactNode; warn?: boolean; title: ReactNode; children?: ReactNode }) {
  return (
    <div className="imp">
      <span className={warn ? 'imp-ic w' : 'imp-ic'}>{icon}</span>
      <div className="imp-body">
        <b>{title}</b>
        {children}
      </div>
    </div>
  )
}

interface DeleteElementDialogProps {
  projectId: string
  element: Element
  onClose: () => void
  onDeleted: () => void
}

/**
 * RF15 e RF16: exclusao com alerta de impacto. Mostra o nivel (baixo, medio, alto) e o que muda no modelo;
 * no nivel alto, pede para digitar o codigo do elemento antes de excluir.
 */
export function DeleteElementDialog({ projectId, element, onClose, onDeleted }: DeleteElementDialogProps) {
  const queryClient = useQueryClient()
  const [typed, setTyped] = useState('')

  const impact = useQuery({
    queryKey: elementKeys.impact(projectId, element.id),
    queryFn: () => elementsApi.deleteImpact(projectId, element.id),
    // o impacto muda a cada alteracao do modelo: sempre recalcula ao abrir
    gcTime: 0,
  })

  const remove = useMutation({
    mutationFn: () => elementsApi.remove(projectId, element.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: elementKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: relationshipKeys.all(projectId) })
      queryClient.invalidateQueries({ queryKey: projectKeys.all })
      onDeleted()
    },
  })

  const data = impact.data
  const level = data ? LEVELS[data.level] : null
  const needsTyping = data?.level === 'HIGH'
  const confirmed = !needsTyping || typed.trim().toUpperCase() === element.code
  const canDelete = !!data && confirmed && !remove.isPending
  const error = remove.error instanceof ApiError ? remove.error : null

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (canDelete) remove.mutate()
  }

  return (
    <Modal
      title={`Excluir ${element.code}?`}
      tone="danger"
      icon={<TrashIcon />}
      subtitle={
        <span>
          {element.description}
          {data && data.level !== 'LOW' && ' Esta exclusão muda a estrutura do modelo.'}
        </span>
      }
      onClose={onClose}
      footer={
        <>
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button className="btn d" type="submit" form="excluir-elemento" disabled={!canDelete}>
            <TrashIcon />
            {remove.isPending ? 'Excluindo…' : `Excluir ${element.code}`}
          </button>
        </>
      }
    >
      <form id="excluir-elemento" onSubmit={handleSubmit} noValidate style={{ display: 'contents' }}>
        {impact.isPending && <p className="hint">Calculando o impacto da exclusão…</p>}
        {impact.isError && <FieldError>{impact.error.message}</FieldError>}

        {data && level && (
          <>
            <div className={`lv-box ${data.level.toLowerCase()}`}>
              <div className="lv-head">
                <span>Nível de impacto</span>
                <strong>{level.label}</strong>
              </div>
              <div className="lv" aria-hidden="true">
                {[1, 2, 3].map((n) => <span key={n} className={n <= level.bars ? 'on' : ''} />)}
              </div>
              <div className="lv-scale hint"><span>Baixo</span><span>Médio</span><span>Alto</span></div>
            </div>

            <div>
              {data.relationships.length === 0 && data.tests === 0 && (
                <Impact icon={<InfoIcon />} title="Nenhum outro elemento é afetado.">
                  <span className="hint">{element.code} não tem relacionamentos nem testes associados.</span>
                </Impact>
              )}

              {data.relationships.length > 0 && (
                <Impact
                  icon={<GraphIcon />}
                  title={
                    data.relationships.length === 1
                      ? '1 relacionamento será removido'
                      : `${data.relationships.length} relacionamentos serão removidos`
                  }
                >
                  <div className="imp-chips">
                    {data.relationships.map((r) => <Chip key={r.id} el={r.other} />)}
                  </div>
                </Impact>
              )}

              {data.orphans.length > 0 && (
                <Impact
                  warn
                  icon={<AlertIcon />}
                  title={
                    <>
                      <span className="mono">{joinCodes(data.orphans)}</span>{' '}
                      {data.orphans.length === 1 ? 'ficará órfão' : 'ficarão órfãos'}
                    </>
                  }
                >
                  <span className="hint">
                    {data.orphans.length === 1 ? 'Ele só se relaciona' : 'Eles só se relacionam'} com {element.code} e{' '}
                    {data.orphans.length === 1 ? 'ficará' : 'ficarão'} sem nenhum vínculo no modelo.
                  </span>
                </Impact>
              )}

              {data.testsLeftWithoutRequirement.length > 0 && (
                <Impact
                  warn
                  icon={<AlertIcon />}
                  title={
                    <>
                      {data.testsLeftWithoutRequirement.length === 1 ? 'O teste ' : 'Os testes '}
                      <span className="mono">{joinCodes(data.testsLeftWithoutRequirement)}</span>{' '}
                      {data.testsLeftWithoutRequirement.length === 1 ? 'ficará sem requisito' : 'ficarão sem requisito'}
                    </>
                  }
                >
                  <span className="hint">
                    {data.testsLeftWithoutRequirement.length === 1 ? 'Ele cobre' : 'Eles cobrem'} apenas {element.code} e{' '}
                    {data.testsLeftWithoutRequirement.length === 1 ? 'deixa' : 'deixam'} de rastrear algum requisito.
                  </span>
                </Impact>
              )}

              {data.dependents.length > 0 && (
                <Impact
                  icon={<InfoIcon />}
                  title={
                    <>
                      <span className="mono">{joinCodes(data.dependents)}</span>{' '}
                      {data.dependents.length === 1 ? 'perde uma dependência' : 'perdem uma dependência'}
                    </>
                  }
                >
                  <span className="hint">
                    {data.dependents.length === 1 ? 'Continua' : 'Continuam'} no modelo, mas deixa
                    {data.dependents.length === 1 ? '' : 'm'} de depender de {element.code}.
                  </span>
                </Impact>
              )}
            </div>

            {needsTyping && (
              <div>
                <label className="lbl" htmlFor="ex-conf">
                  Digite <span className="mono">{element.code}</span> para confirmar
                </label>
                <input
                  id="ex-conf"
                  className="inp"
                  type="text"
                  autoComplete="off"
                  value={typed}
                  onChange={(e) => setTyped(e.target.value)}
                  autoFocus
                />
              </div>
            )}
          </>
        )}

        {error && <FieldError>{error.message}</FieldError>}
      </form>
    </Modal>
  )
}
