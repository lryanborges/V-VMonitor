import { Fragment, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { Element, ElementKind, Relationship, RelationshipType } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { InfoTip } from '../../components/InfoTip'
import { useProject } from '../../layouts/useProject'
import { kindInfo } from '../../utils/labels'
import { relationSentence, sortTypes, typeAbbr, typeMeta } from '../../utils/relationships'
import { AddRelationshipModal } from './AddRelationshipModal'
import { CellRelationsDialog } from './CellRelationsDialog'
import { EditRelationshipModal } from './EditRelationshipModal'
import { ElementPanel } from './ElementPanel'
import { KindTabs, type KindFilter } from './KindTabs'
import { ModelHeader } from './ModelHeader'
import { RemoveRelationshipDialog } from './RemoveRelationshipDialog'
import { useModelData } from './useModelData'
import './MatrixPage.css'

type LinkFilter = 'all' | 'linked' | 'unlinked'

/** Ordem das colunas no design: regras de negocio, nao funcionais, requisitos funcionais. */
const COLUMN_ORDER: ElementKind[] = ['BUSINESS_RULE', 'NON_FUNCTIONAL', 'FUNCTIONAL']

const GROUP_LABEL: Record<ElementKind, string> = {
  BUSINESS_RULE: 'Regras de negócio',
  NON_FUNCTIONAL: 'Não funcionais',
  FUNCTIONAL: 'Requisitos funcionais',
}

/** Classe de cor da celula pelo tipo (pre-definidos) ou "x" para personalizados. */
const CELL_TONE: Record<string, string> = { dep: 'd', ref: 'r', sim: 's', cf: 'c', custom: 'x' }

const pairKey = (a: string, b: string) => `${a}|${b}`

type CellAction =
  | { mode: 'add'; row: Element; column: Element }
  | { mode: 'cell'; row: Element; column: Element }
  | { mode: 'edit'; relationship: Relationship }
  | { mode: 'remove'; relationship: Relationship }

/**
 * Matriz de rastreabilidade (RF11, RF12): linhas e colunas sao elementos do modelo e cada celula mostra
 * os relacionamentos entre o par. Reflete o modelo atual, inclusive rascunhos (com borda tracejada).
 */
export function MatrixPage() {
  const project = useProject()
  const { elementsQuery, elements, relationships, tests, changes } = useModelData(project.id)
  const [rowFilter, setRowFilter] = useState<KindFilter>('FUNCTIONAL')
  const [linkFilter, setLinkFilter] = useState<LinkFilter>('all')
  const [hoverCol, setHoverCol] = useState<string | null>(null)
  const [action, setAction] = useState<CellAction | null>(null)
  const [params, setParams] = useSearchParams()
  const canEdit = project.role !== 'VIEWER'
  const base = `/projetos/${project.id}`
  const selected = elements.find((e) => e.id === params.get('el')) ?? null
  const select = (id: string | null) => setParams(id ? { el: id } : {}, { replace: true })

  // relacoes indexadas pelos dois sentidos do par, para a celula (linha, coluna) achar as suas
  const byPair = useMemo(() => {
    const map = new Map<string, Relationship[]>()
    for (const r of relationships) {
      for (const key of [pairKey(r.source.id, r.target.id), pairKey(r.target.id, r.source.id)]) {
        map.set(key, [...(map.get(key) ?? []), r])
      }
    }
    return map
  }, [relationships])

  const linked = useMemo(() => {
    const ids = new Set<string>()
    relationships.forEach((r) => (ids.add(r.source.id), ids.add(r.target.id)))
    return ids
  }, [relationships])

  const rows = useMemo(
    () =>
      elements.filter(
        (e) =>
          (rowFilter === 'all' || e.kind === rowFilter) &&
          (linkFilter === 'all' || (linkFilter === 'linked' ? linked.has(e.id) : !linked.has(e.id))),
      ),
    [elements, rowFilter, linkFilter, linked],
  )

  const columns = useMemo(() => {
    const ordered = COLUMN_ORDER.flatMap((k) => elements.filter((e) => e.kind === k))
    // "com vinculos": so as colunas que tem alguma relacao com as linhas visiveis
    if (linkFilter !== 'linked') return ordered
    return ordered.filter((c) => rows.some((r) => byPair.has(pairKey(r.id, c.id))))
  }, [elements, linkFilter, rows, byPair])

  const groups = COLUMN_ORDER.map((kind) => ({ kind, count: columns.filter((c) => c.kind === kind).length })).filter(
    (g) => g.count > 0,
  )
  // primeira coluna de cada grupo recebe a borda mais forte (separador de grupos)
  const groupStart = new Set(groups.map((g) => columns.find((c) => c.kind === g.kind)!.id))

  const usedTypes = useMemo(() => {
    const map = new Map<string, RelationshipType>()
    relationships.forEach((r) => map.set(r.type.id, r.type))
    return sortTypes([...map.values()])
  }, [relationships])

  const template = `minmax(220px, 260px) repeat(${columns.length}, 36px) 96px`

  function cellOf(row: Element, column: Element) {
    if (row.id === column.id) return { cls: 'mc self', text: '', title: '', rels: [] as Relationship[] }
    const rels = byPair.get(pairKey(row.id, column.id)) ?? []
    if (rels.length === 0) {
      return { cls: canEdit ? 'mc can-add' : 'mc', text: '', title: canEdit ? `Relacionar ${row.code} e ${column.code}` : '', rels }
    }
    // letras juntas na ordem dos tipos (ex.: "DR"); a cor so quando ha um unico tipo
    const types = sortTypes([...new Map(rels.map((r) => [r.type.id, r.type])).values()])
    const tone = types.length === 1 ? CELL_TONE[typeMeta(types[0]).line] : 'm'
    const pending = rels.some((r) => r.submissionStatus === 'DRAFT')
    const title = rels.map((r) => relationSentence(r) + (r.submissionStatus === 'DRAFT' ? ' (pendente)' : '')).join('\n')
    return { cls: `mc ${tone}${pending ? ' pending' : ''}`, text: types.map(typeAbbr).join(''), title, rels }
  }

  function openCell(row: Element, column: Element) {
    if (row.id === column.id) return
    const rels = byPair.get(pairKey(row.id, column.id)) ?? []
    if (rels.length > 0) setAction({ mode: 'cell', row, column })
    else if (canEdit) setAction({ mode: 'add', row, column })
  }

  const cellRels =
    action?.mode === 'cell' ? (byPair.get(pairKey(action.row.id, action.column.id)) ?? []) : []

  return (
    <>
      <ModelHeader project={project} view="matrix" elements={elements} changes={changes} />

      <div className="mx-body">
      <section className="mx-content">
        <div className="mx-toolbar">
          <KindTabs value={rowFilter} onChange={setRowFilter} elements={elements} label="Linhas da matriz" />
          <div className="seg" role="group" aria-label="Filtrar por vínculos">
            {(
              [
                ['all', 'Todos'],
                ['linked', 'Com vínculos'],
                ['unlinked', 'Sem vínculos'],
              ] as [LinkFilter, string][]
            ).map(([value, label]) => (
              <button key={value} type="button" aria-pressed={linkFilter === value} onClick={() => setLinkFilter(value)}>
                {label}
              </button>
            ))}
          </div>
          <div style={{ flex: 1 }} />
          <div className="mx-legend" aria-label="Legenda">
            {usedTypes.map((t) => (
              <span key={t.id} className="key">
                <b className={`mc-key ${CELL_TONE[typeMeta(t).line]}`}>{typeAbbr(t)}</b>
                {t.name}
              </span>
            ))}
            <span className="key">
              <b className="mc-key pending" />
              Pendente de submissão
            </span>
          </div>
          <InfoTip>
            {canEdit
              ? 'Clique num código para ver o elemento ao lado, numa célula vazia para relacionar, ou numa preenchida para trocar o tipo, inverter ou remover.'
              : 'Clique num código para ver o elemento ao lado, ou passe o mouse sobre uma célula para ver a relação.'}
          </InfoTip>
        </div>

        {elementsQuery.isPending && <p className="hint">Carregando o modelo…</p>}
        {elementsQuery.isError && <FieldError>{elementsQuery.error.message}</FieldError>}

        {elementsQuery.isSuccess && elements.length === 0 && (
          <div className="card mx-empty">
            <strong>Nenhum elemento cadastrado ainda.</strong>
            <span className="hint">A matriz aparece quando o projeto tiver requisitos ou regras de negócio.</span>
            <Link className="btn" to={base}>Ir para a lista</Link>
          </div>
        )}

        {elementsQuery.isSuccess && elements.length > 0 && rows.length === 0 && (
          <div className="card mx-empty">
            <span className="hint">
              {linkFilter === 'unlinked'
                ? 'Nenhum elemento sem vínculos neste filtro.'
                : 'Nenhum elemento encontrado com esses filtros.'}
            </span>
          </div>
        )}

        {rows.length > 0 && (
          <div className="mx-scroll card" onMouseLeave={() => setHoverCol(null)}>
            <div role="table" aria-label="Matriz de rastreabilidade" className="mx-table">
              <div className="mgrid mx-groups" role="row" style={{ gridTemplateColumns: template }}>
                <div className="mcorner mx-sticky-corner" />
                {groups.map((g) => (
                  <div
                    key={g.kind}
                    className={`mg ${kindInfo(g.kind).chip}`}
                    style={{ gridColumn: `span ${g.count}` }}
                    title={GROUP_LABEL[g.kind]}
                  >
                    <span className="mg-label">{GROUP_LABEL[g.kind]}</span>
                  </div>
                ))}
                <div className="mg">Cobertura</div>
              </div>

              <div className="mgrid mx-heads" role="row" style={{ gridTemplateColumns: template }}>
                <div className="mcorner mx-sticky-corner" role="columnheader">Elemento</div>
                {columns.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    role="columnheader"
                    aria-selected={c.id === selected?.id}
                    title={`${c.code}: ${c.description}`}
                    className={`mhc ${kindInfo(c.kind).chip}${groupStart.has(c.id) ? ' gs' : ''}${hoverCol === c.id ? ' on' : ''}${c.id === selected?.id ? ' sel' : ''}`}
                    onClick={() => select(c.id)}
                  >
                    {c.code}
                  </button>
                ))}
                <div className="mhc mt-head" role="columnheader">Testes</div>
              </div>

              {rows.map((row) => (
                <div key={row.id} className={`mgrid mrow${row.id === selected?.id ? ' sel' : ''}`} role="row" style={{ gridTemplateColumns: template }}>
                  <button
                    type="button"
                    className="mh mx-sticky-left"
                    role="rowheader"
                    aria-selected={row.id === selected?.id}
                    title={`${row.code}: ${row.description}`}
                    onClick={() => select(row.id)}
                  >
                    <span className={`chip ${kindInfo(row.kind).chip}`}>{row.code}</span>
                    <span className="mh-desc">{row.description}</span>
                  </button>
                  {columns.map((column) => {
                    const cell = cellOf(row, column)
                    const clickable = cell.rels.length > 0 || (canEdit && row.id !== column.id)
                    return (
                      <Fragment key={column.id}>
                        {clickable ? (
                          <button
                            type="button"
                            role="cell"
                            className={`${cell.cls}${groupStart.has(column.id) ? ' gs' : ''}`}
                            title={cell.title}
                            onMouseEnter={() => setHoverCol(column.id)}
                            onClick={() => openCell(row, column)}
                          >
                            {cell.text}
                          </button>
                        ) : (
                          <div
                            role="cell"
                            className={`${cell.cls}${groupStart.has(column.id) ? ' gs' : ''}`}
                            onMouseEnter={() => setHoverCol(column.id)}
                          />
                        )}
                      </Fragment>
                    )
                  })}
                  <div role="cell" className={row.kind === 'BUSINESS_RULE' ? 'mt na' : row.tests > 0 ? 'mt ok' : 'mt no'}>
                    {row.kind === 'BUSINESS_RULE' ? '—' : row.tests === 0 ? 'Sem teste' : row.tests === 1 ? '1 teste' : `${row.tests} testes`}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

      </section>

        <ElementPanel
          projectId={project.id}
          element={selected}
          elements={elements}
          relationships={relationships}
          tests={tests}
          canEdit={canEdit}
          onSelect={select}
          emptyHint="Clique no código de uma linha ou coluna para ver os detalhes do elemento."
        />
      </div>

      {action?.mode === 'add' && (
        <AddRelationshipModal
          projectId={project.id}
          origin={action.row}
          initialTargetId={action.column.id}
          elements={elements}
          relationships={relationships}
          onClose={() => setAction(null)}
          onCreated={() => setAction(null)}
        />
      )}

      {action?.mode === 'cell' && cellRels.length > 0 && (
        <CellRelationsDialog
          row={action.row}
          column={action.column}
          relationships={cellRels}
          canEdit={canEdit}
          onClose={() => setAction(null)}
          onAdd={() => setAction({ mode: 'add', row: action.row, column: action.column })}
          onEdit={(relationship) => setAction({ mode: 'edit', relationship })}
          onRemove={(relationship) => setAction({ mode: 'remove', relationship })}
        />
      )}

      {action?.mode === 'edit' && (
        <EditRelationshipModal
          projectId={project.id}
          relationship={action.relationship}
          relationships={relationships}
          onClose={() => setAction(null)}
          onSaved={() => setAction(null)}
        />
      )}

      {action?.mode === 'remove' && (
        <RemoveRelationshipDialog
          projectId={project.id}
          relationship={action.relationship}
          onClose={() => setAction(null)}
          onRemoved={() => setAction(null)}
        />
      )}
    </>
  )
}
