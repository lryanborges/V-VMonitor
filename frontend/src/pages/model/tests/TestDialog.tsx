import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ApiError } from '../../../api/client'
import { testsApi } from '../../../api/tests'
import type { Element, TestCase } from '../../../api/types'
import { FieldError } from '../../../components/FieldError'
import { CheckIcon } from '../../../components/icons'
import { Modal } from '../../../components/Modal'
import { RequirementPicker } from './RequirementPicker'
import { useRefreshAfterTestChange } from './useTestMutations'
import './Tests.css'

interface TestDialogProps {
  projectId: string
  elements: Element[]
  tests: TestCase[]
  /** Teste a editar; sem ele, cadastra (ou associa) um teste. */
  test?: TestCase
  /** Requisito de onde veio o "Associar teste": ja vem marcado e habilita escolher um teste existente. */
  forElement?: Element
  onClose: () => void
}

type Mode = 'new' | 'existing'

const FORM_ID = 'test-form'

/**
 * RF9: cadastra um teste (descricao e requisitos cobertos), associa um teste existente ao requisito de origem
 * ou edita um teste. Todo teste cobre ao menos um requisito.
 */
export function TestDialog({ projectId, elements, tests, test, forElement, onClose }: TestDialogProps) {
  const refresh = useRefreshAfterTestChange(projectId)
  const [mode, setMode] = useState<Mode>('new')
  const [description, setDescription] = useState(test?.description ?? '')
  const [elementIds, setElementIds] = useState<string[]>(
    test ? test.covers.map((e) => e.id) : forElement ? [forElement.id] : [],
  )
  const [existingId, setExistingId] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState(false)

  // testes que ainda nao cobrem o requisito de origem
  const linkable = forElement ? tests.filter((t) => !t.covers.some((e) => e.id === forElement.id)) : []
  const existing = tests.find((t) => t.id === existingId) ?? null

  const save = useMutation({
    mutationFn: () => {
      if (mode === 'existing' && existing && forElement) {
        return testsApi.update(projectId, existing.id, {
          description: existing.description,
          elementIds: [...existing.covers.map((e) => e.id), forElement.id],
        })
      }
      const data = { description: description.trim(), elementIds }
      return test ? testsApi.update(projectId, test.id, data) : testsApi.create(projectId, data)
    },
    onSuccess: () => {
      refresh()
      onClose()
    },
  })

  const apiError = save.error instanceof ApiError ? save.error : null
  const descriptionError = submitted && !description.trim() ? 'A descrição é obrigatória.' : apiError?.fieldErrors.description
  const requirementsError =
    submitted && elementIds.length === 0 ? 'Escolha pelo menos um requisito coberto pelo teste.' : apiError?.fieldErrors.elementIds
  const generalError = apiError && !apiError.fieldErrors.description && !apiError.fieldErrors.elementIds ? apiError.message : null

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitted(true)
    if (mode === 'existing') {
      if (existing) save.mutate()
      return
    }
    if (description.trim() && elementIds.length > 0) save.mutate()
  }

  const title = test ? `Editar ${test.code}` : forElement ? `Associar teste a ${forElement.code}` : 'Novo teste'
  const submitLabel = test ? 'Salvar' : mode === 'existing' ? 'Associar' : 'Cadastrar teste'

  return (
    <Modal
      title={title}
      icon={<CheckIcon />}
      size="lg"
      subtitle={test ? 'Troque a descrição ou os requisitos que o teste cobre.' : 'Testes cobrem requisitos funcionais e não funcionais.'}
      onClose={onClose}
      footer={
        <>
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button
            className="btn p"
            type="submit"
            form={FORM_ID}
            disabled={save.isPending || (mode === 'existing' && !existing)}
          >
            {save.isPending ? 'Salvando…' : submitLabel}
          </button>
        </>
      }
    >
      {!test && forElement && (
        <div className="seg td-modes" role="group" aria-label="Como associar">
          <button type="button" aria-pressed={mode === 'new'} onClick={() => setMode('new')}>Novo teste</button>
          <button
            type="button"
            aria-pressed={mode === 'existing'}
            disabled={linkable.length === 0}
            title={linkable.length === 0 ? 'Não há outros testes para associar' : undefined}
            onClick={() => setMode('existing')}
          >
            Teste existente <span className="n">{linkable.length}</span>
          </button>
        </div>
      )}

      <form id={FORM_ID} onSubmit={handleSubmit} noValidate style={{ display: 'contents' }}>
        {mode === 'new' ? (
          <>
            <div>
              <label className="lbl" htmlFor="td-desc">Descrição</label>
              <textarea
                id="td-desc"
                className={`inp${descriptionError ? ' err' : ''}`}
                rows={3}
                placeholder="Ex.: Entrar com senha errada e verificar a mensagem de credenciais inválidas"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                aria-invalid={!!descriptionError}
                autoFocus
              />
              {descriptionError && <FieldError>{descriptionError}</FieldError>}
            </div>
            <div>
              <span className="lbl">Requisitos cobertos</span>
              <RequirementPicker elements={elements} value={elementIds} onChange={setElementIds} invalid={!!requirementsError} />
              {requirementsError && <FieldError>{requirementsError}</FieldError>}
            </div>
          </>
        ) : (
          <ul className="td-existing" role="radiogroup" aria-label="Testes existentes">
            {linkable.map((t) => (
              <li key={t.id}>
                <label className={`td-option${existingId === t.id ? ' on' : ''}`}>
                  <input type="radio" name="td-existing" checked={existingId === t.id} onChange={() => setExistingId(t.id)} />
                  <span className="chip t">{t.code}</span>
                  <span className="td-option-text">
                    <span>{t.description}</span>
                    <span className="hint">
                      {t.covers.length === 0 ? 'Sem requisito' : `Cobre ${t.covers.map((e) => e.code).join(', ')}`}
                    </span>
                  </span>
                </label>
              </li>
            ))}
          </ul>
        )}
      </form>

      {generalError && <FieldError>{generalError}</FieldError>}
    </Modal>
  )
}
