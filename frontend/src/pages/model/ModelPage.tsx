import { useMemo, useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import type { Element, TestCase } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { AlertIcon, PlusIcon, SearchIcon } from '../../components/icons'
import { useProject } from '../../layouts/useProject'
import { ElementFormDrawer } from './ElementFormDrawer'
import { ElementPanel } from './ElementPanel'
import { ElementTable } from './ElementTable'
import { DeleteTestDialog } from './tests/DeleteTestDialog'
import { TestDialog } from './tests/TestDialog'
import { TestTable } from './tests/TestTable'
import { KindTabs, type KindFilter } from './KindTabs'
import { ModelHeader } from './ModelHeader'
import { useModelData } from './useModelData'
import './ModelPage.css'

const isRequirement = (e: Element) => e.kind !== 'BUSINESS_RULE'

/** Busca por ID ou descricao, sem diferenciar maiusculas nem acentos. */
function normalize(text: string) {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

interface ModelPageProps {
  /** Abre o painel de novo elemento (rota elementos/novo). */
  newElement?: boolean
}

/** Tela "Modelo" em lista: requisitos, regras de negocio e seus relacionamentos (RF5, RF6, RF7, RF13). */
export function ModelPage({ newElement = false }: ModelPageProps) {
  const project = useProject()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const [filter, setFilter] = useState<KindFilter>('all')
  // RF9: aba Testes, no lugar da tabela de elementos
  const [showTests, setShowTests] = useState(false)
  const [testAction, setTestAction] = useState<{ mode: 'new' } | { mode: 'edit' | 'delete'; test: TestCase } | null>(null)
  const [search, setSearch] = useState('')
  const [onlyUntested, setOnlyUntested] = useState(false)

  const {
    elementsQuery: elements,
    testsQuery,
    elements: all,
    relationships: relations,
    tests,
    changes,
  } = useModelData(project.id)
  const canEdit = project.role !== 'VIEWER'
  const selected = all.find((e) => e.id === params.get('el')) ?? null

  // RF13: cobertura calculada sobre os requisitos; regras de negocio nao entram na conta
  const requirements = all.filter(isRequirement)
  const untested = requirements.filter((e) => e.tests === 0)
  const coverage = requirements.length === 0 ? 0 : Math.round((100 * (requirements.length - untested.length)) / requirements.length)

  const rows = useMemo(() => {
    const term = normalize(search.trim())
    return all.filter(
      (e) =>
        (filter === 'all' || e.kind === filter) &&
        (!onlyUntested || (isRequirement(e) && e.tests === 0)) &&
        (!term || normalize(`${e.code} ${e.description}`).includes(term)),
    )
  }, [all, filter, search, onlyUntested])

  const testRows = useMemo(() => {
    const term = normalize(search.trim())
    return tests.filter(
      (t) => !term || normalize(`${t.code} ${t.description} ${t.covers.map((e) => e.code).join(' ')}`).includes(term),
    )
  }, [tests, search])

  // quem so pode visualizar nao abre o formulario, nem pelo endereco direto
  // (mantem o elemento selecionado, se houver)
  if (newElement && !canEdit) {
    return <Navigate to={{ pathname: `/projetos/${project.id}`, search: params.toString() }} replace />
  }

  const select = (id: string | null) => setParams(id ? { el: id } : {}, { replace: true })
  const base = `/projetos/${project.id}`

  return (
    <>
      <ModelHeader project={project} view="list" elements={all} changes={changes} />

      <div className="md-body">
        <section className="md-content">
          <div className="card md-cov">
            <div className="md-cov-n">
              <span className="eyebrow">Cobertura de testes</span>
              <span className="mono">{coverage}%</span>
            </div>
            <div className="md-cov-bar">
              <div className={coverage < 20 && requirements.length > 0 ? 'bar lo' : 'bar'}>
                <i style={{ width: `${coverage}%` }} />
              </div>
              <span className="hint">
                {requirements.length - untested.length} de {requirements.length}{' '}
                {requirements.length === 1 ? 'requisito tem' : 'requisitos têm'} ao menos um teste associado.
              </span>
            </div>
            {untested.length > 0 && (
              <button
                className="btn"
                type="button"
                style={{ color: 'var(--wr)' }}
                aria-pressed={onlyUntested}
                onClick={() => setOnlyUntested((v) => !v)}
              >
                <AlertIcon />
                {onlyUntested ? 'Mostrar todos' : `Ver ${untested.length} sem teste`}
              </button>
            )}
          </div>

          <div className="md-toolbar">
            <KindTabs
              value={showTests ? null : filter}
              onChange={(value) => {
                setFilter(value)
                setShowTests(false)
              }}
              elements={all}
            >
              <button type="button" aria-pressed={showTests} onClick={() => setShowTests(true)}>
                Testes <span className="n">{tests.length}</span>
              </button>
            </KindTabs>
            <div className="md-search">
              <SearchIcon />
              <label htmlFor="rq-busca" className="sr-only">Buscar por ID ou descrição</label>
              <input
                id="rq-busca"
                className="inp sm with-icon"
                type="search"
                placeholder="Buscar por ID ou descrição"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            {canEdit &&
              (showTests ? (
                <button className="btn p" type="button" onClick={() => setTestAction({ mode: 'new' })}>
                  <PlusIcon />
                  Novo teste
                </button>
              ) : (
                <button className="btn p" type="button" onClick={() => navigate(`${base}/elementos/novo`)}>
                  <PlusIcon />
                  Novo elemento
                </button>
              ))}
          </div>

          {showTests ? (
            <TestTable
              tests={testRows}
              canEdit={canEdit}
              onEdit={(test) => setTestAction({ mode: 'edit', test })}
              onDelete={(test) => setTestAction({ mode: 'delete', test })}
              onSelectElement={(id) => select(id)}
            >
              {testsQuery.isPending && <p className="hint md-empty">Carregando testes…</p>}
              {testsQuery.isSuccess && tests.length === 0 && (
                <div className="md-empty">
                  <strong>Nenhum teste cadastrado ainda.</strong>
                  <span className="hint">Testes cobrem requisitos funcionais e não funcionais e alimentam a cobertura.</span>
                </div>
              )}
              {tests.length > 0 && testRows.length === 0 && (
                <p className="hint md-empty">Nenhum teste encontrado com essa busca.</p>
              )}
            </TestTable>
          ) : (
          <ElementTable elements={rows} selectedId={selected?.id ?? null} onSelect={(id) => select(id)}>
            {elements.isPending && <p className="hint md-empty">Carregando elementos…</p>}
            {elements.isError && <div className="md-empty"><FieldError>{elements.error.message}</FieldError></div>}
            {elements.isSuccess && all.length === 0 && (
              <div className="md-empty">
                <strong>Nenhum elemento cadastrado ainda.</strong>
                <span className="hint">Cadastre requisitos e regras de negócio para montar o modelo do projeto.</span>
                {canEdit && (
                  <button className="btn p" type="button" onClick={() => navigate(`${base}/elementos/novo`)}>
                    <PlusIcon />
                    Cadastrar o primeiro elemento
                  </button>
                )}
              </div>
            )}
            {elements.isSuccess && all.length > 0 && rows.length === 0 && (
              <p className="hint md-empty">Nenhum elemento encontrado com esses filtros.</p>
            )}
          </ElementTable>
          )}
        </section>

        <ElementPanel
          projectId={project.id}
          element={selected}
          elements={all}
          relationships={relations}
          tests={tests}
          canEdit={canEdit}
          onSelect={select}
          emptyHint="Selecione um elemento na lista para ver os detalhes, os relacionamentos e os testes."
        />
      </div>

      {testAction && testAction.mode !== 'delete' && (
        <TestDialog
          projectId={project.id}
          elements={all}
          tests={tests}
          test={testAction.mode === 'edit' ? testAction.test : undefined}
          onClose={() => setTestAction(null)}
        />
      )}

      {testAction?.mode === 'delete' && (
        <DeleteTestDialog projectId={project.id} test={testAction.test} onClose={() => setTestAction(null)} />
      )}

      {newElement && (
        <ElementFormDrawer
          projectId={project.id}
          onClose={() => navigate(base)}
          // UC-05 passo 7 e UC-07 passo 6: apresenta o elemento cadastrado
          onSaved={(created) => navigate(`${base}?el=${created.id}`)}
        />
      )}
    </>
  )
}

