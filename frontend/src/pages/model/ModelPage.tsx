import { useMemo, useState, type KeyboardEvent } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import type { Element } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { AlertIcon, PlusIcon, SearchIcon, TickIcon } from '../../components/icons'
import { useProject } from '../../layouts/useProject'
import { kindInfo, priorityInfo } from '../../utils/labels'
import { ElementFormDrawer } from './ElementFormDrawer'
import { ElementPanel } from './ElementPanel'
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
  const [search, setSearch] = useState('')
  const [onlyUntested, setOnlyUntested] = useState(false)

  const { elementsQuery: elements, elements: all, relationships: relations, drafts } = useModelData(project.id)
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

  // quem so pode visualizar nao abre o formulario, nem pelo endereco direto
  // (mantem o elemento selecionado, se houver)
  if (newElement && !canEdit) {
    return <Navigate to={{ pathname: `/projetos/${project.id}`, search: params.toString() }} replace />
  }

  const select = (id: string | null) => setParams(id ? { el: id } : {}, { replace: true })
  const base = `/projetos/${project.id}`

  return (
    <>
      <ModelHeader project={project} view="list" elements={all} relationships={relations} drafts={drafts} />

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
                {requirements.length === 1 ? 'requisito tem' : 'requisitos têm'} ao menos um teste associado. Regras de
                negócio não entram na conta.
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
            <KindTabs value={filter} onChange={setFilter} elements={all} />
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
            {canEdit && (
              <button className="btn p" type="button" onClick={() => navigate(`${base}/elementos/novo`)}>
                <PlusIcon />
                Novo elemento
              </button>
            )}
          </div>

          <div className="card md-table" role="table" aria-label="Elementos do modelo">
            <div className="row hd" role="row">
              <span role="columnheader">ID</span>
              <span role="columnheader">Descrição</span>
              <span role="columnheader">Prioridade</span>
              <span role="columnheader">Vínculos</span>
              <span role="columnheader">Testes</span>
            </div>
            <div className="md-rows">
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
              {rows.map((e) => (
                <ElementRow key={e.id} element={e} selected={e.id === selected?.id} onSelect={() => select(e.id)} />
              ))}
            </div>
          </div>
        </section>

        <ElementPanel
          projectId={project.id}
          element={selected}
          elements={all}
          relationships={relations}
          canEdit={canEdit}
          onSelect={select}
          emptyHint="Selecione um elemento na lista para ver os detalhes, os relacionamentos e os testes."
        />
      </div>

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

function ElementRow({ element, selected, onSelect }: { element: Element; selected: boolean; onSelect: () => void }) {
  const kind = kindInfo(element.kind)
  const prio = element.priority ? priorityInfo(element.priority) : null
  const onKey = (e: KeyboardEvent) => (e.key === 'Enter' || e.key === ' ') && (e.preventDefault(), onSelect())

  return (
    <div
      className={selected ? 'row sel' : 'row'}
      role="row"
      tabIndex={0}
      aria-selected={selected}
      onClick={onSelect}
      onKeyDown={onKey}
    >
      <span role="cell"><span className={`chip ${kind.chip}`}>{element.code}</span></span>
      <span role="cell" className="desc" title={element.description}>{element.description}</span>
      <span role="cell">
        {prio ? (
          <span className={`prio p${prio.level}`}><b><i /><i /><i /></b>{prio.label}</span>
        ) : (
          <span className="hint">—</span>
        )}
      </span>
      <span role="cell" className="mono md-links">{element.links}</span>
      <span role="cell">
        {element.kind === 'BUSINESS_RULE' ? (
          <span className="hint">—</span>
        ) : element.tests > 0 ? (
          <span className="pill ok"><TickIcon />{element.tests === 1 ? '1 teste' : `${element.tests} testes`}</span>
        ) : (
          <span className="pill w">Sem teste</span>
        )}
      </span>
    </div>
  )
}
