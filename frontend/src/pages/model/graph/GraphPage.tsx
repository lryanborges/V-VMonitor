import '@xyflow/react/dist/style.css'
import {
  ReactFlow,
  ReactFlowProvider,
  useNodesState,
  useReactFlow,
  useViewport,
  type NodeMouseHandler,
  type EdgeMouseHandler,
  type NodeChange,
  type OnNodeDrag,
} from '@xyflow/react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { Element, ElementKind, Relationship, RelationshipType, TestCase } from '../../../api/types'
import { FieldError } from '../../../components/FieldError'
import { PlusIcon, SearchIcon } from '../../../components/icons'
import { RelationLine } from '../../../components/RelationLine'
import { useProject } from '../../../layouts/useProject'
import { layoutGraph, nodeWidth, testNodeWidth } from '../../../utils/graphLayout'
import { KINDS } from '../../../utils/labels'
import { sortTypes, typeMeta } from '../../../utils/relationships'
import { AddRelationshipModal } from '../AddRelationshipModal'
import { CellRelationsDialog } from '../CellRelationsDialog'
import { EditRelationshipModal } from '../EditRelationshipModal'
import { ElementPanel } from '../ElementPanel'
import { ModelHeader } from '../ModelHeader'
import { RemoveRelationshipDialog } from '../RemoveRelationshipDialog'
import { useModelData } from '../useModelData'
import { ElementNode, type ElementNodeType } from './ElementNode'
import { RelationEdge, type RelationEdgeType } from './RelationEdge'
import { TestEdge, type TestEdgeType } from './TestEdge'
import { TestNode, type TestNodeType } from './TestNode'
import { TestDialog } from '../tests/TestDialog'
import { useGraphPositions } from './useGraphPositions'
import './GraphPage.css'

/** Zoom maximo ao enquadrar (ao abrir e no botao): evita nos gigantes em modelos pequenos. */
const FIT_OPTIONS = { padding: 0.2, maxZoom: 1.5 }
const MIN_ZOOM = 0.2
const MAX_ZOOM = 2.5

const nodeTypes = { element: ElementNode, test: TestNode }
const edgeTypes = { relation: RelationEdge, covers: TestEdge }

type GraphNode = ElementNodeType | TestNodeType
type GraphEdge = RelationEdgeType | TestEdgeType

/** Prefixo dos nos de teste, para nao colidir com os ids dos elementos. */
const TEST_NODE = 'test:'

/** Id do elemento ou do teste por tras de um no do grafo (o usado nas posicoes salvas). */
const nodeIdOf = (id: string) => (id.startsWith(TEST_NODE) ? id.slice(TEST_NODE.length) : id)

type GraphAction =
  | { mode: 'add'; origin: Element }
  | { mode: 'pair'; source: Element; target: Element }
  | { mode: 'edit'; relationship: Relationship }
  | { mode: 'remove'; relationship: Relationship }
  | { mode: 'editTest'; test: TestCase }

/** Representacao visual em grafo (RF11, RF12) do modelo atual, inclusive rascunhos. */
export function GraphPage() {
  return (
    <ReactFlowProvider>
      <GraphView />
    </ReactFlowProvider>
  )
}

function GraphView() {
  const project = useProject()
  const { elementsQuery, relationshipsQuery, testsQuery, elements, relationships, tests, changes } = useModelData(project.id)
  // o layout depende das relacoes e dos testes: so monta o grafo com tudo carregado
  // RF21: posicoes arrumadas pelos membros, salvas ao soltar um no (proprietario e editores)
  const savedPositions = useGraphPositions(project.id, project.role !== 'VIEWER')
  const ready = elementsQuery.isSuccess && relationshipsQuery.isSuccess && testsQuery.isSuccess && savedPositions.ready
  const [params, setParams] = useSearchParams()
  const [hiddenKinds, setHiddenKinds] = useState<Set<ElementKind>>(new Set())
  const [showCoverage, setShowCoverage] = useState(true)
  // RF9: testes como nos ligados aos requisitos que cobrem
  const [showTests, setShowTests] = useState(true)
  const [action, setAction] = useState<GraphAction | null>(null)
  const canEdit = project.role !== 'VIEWER'
  const base = `/projetos/${project.id}`
  const selectedId = params.get('el')
  const selected = elements.find((e) => e.id === selectedId) ?? null

  const visible = useMemo(() => elements.filter((e) => !hiddenKinds.has(e.kind)), [elements, hiddenKinds])
  const visibleIds = useMemo(() => new Set(visible.map((e) => e.id)), [visible])
  const visibleRelations = useMemo(
    () => relationships.filter((r) => visibleIds.has(r.source.id) && visibleIds.has(r.target.id)),
    [relationships, visibleIds],
  )

  // testes visiveis: os que cobrem algum requisito visivel, e os que ficaram sem requisito
  const visibleTests = useMemo(
    () => (showTests ? tests.filter((t) => t.covers.length === 0 || t.covers.some((e) => visibleIds.has(e.id))) : []),
    [tests, showTests, visibleIds],
  )

  // layout automatico, usado por quem ainda nao foi arrumado a mao (as posicoes salvas tem prioridade)
  const positions = useMemo(
    () =>
      layoutGraph(
        [
          ...visible.map((e) => ({ id: e.id, width: nodeWidth(e) })),
          ...visibleTests.map((t) => ({ id: TEST_NODE + t.id, width: testNodeWidth(t.code) })),
        ],
        [
          ...visibleRelations.map((r) => ({ source: r.source.id, target: r.target.id })),
          ...visibleTests.flatMap((t) => t.covers.map((e) => ({ source: TEST_NODE + t.id, target: e.id }))),
        ],
      ),
    [visible, visibleRelations, visibleTests],
  )
  const [nodes, setNodes, onNodesChange] = useNodesState<GraphNode>([])
  // posicoes arrastadas nesta tela (valem tambem para quem so visualiza); depois, as salvas no projeto;
  // os demais nos (ex.: recem-cadastrados) seguem o layout automatico
  const dragged = useRef(new Map<string, { x: number; y: number }>())

  const handleNodesChange = (changes: NodeChange<GraphNode>[]) => {
    changes.forEach((ch) => {
      if (ch.type === 'position' && ch.dragging && ch.position) dragged.current.set(ch.id, ch.position)
    })
    onNodesChange(changes)
  }

  useEffect(() => {
    const at = (id: string) =>
      dragged.current.get(id) ?? savedPositions.saved.get(nodeIdOf(id)) ?? positions.get(id) ?? { x: 0, y: 0 }
    setNodes([
      ...visible.map((element) => ({
        id: element.id,
        type: 'element' as const,
        position: at(element.id),
        data: { element, selected: element.id === selectedId, showCoverage },
      })),
      ...visibleTests.map((test) => ({
        id: TEST_NODE + test.id,
        type: 'test' as const,
        position: at(TEST_NODE + test.id),
        data: { test, highlighted: !!selectedId && test.covers.some((e) => e.id === selectedId) },
      })),
    ])
  }, [visible, visibleTests, positions, savedPositions.saved, selectedId, showCoverage, setNodes])

  // ao soltar: salva a posicao do no (e dos demais, se arrastou uma selecao de varios)
  const onNodeDragStop: OnNodeDrag<GraphNode> = (_, __, moved) => {
    moved.forEach((n) => savedPositions.queue(nodeIdOf(n.id), n.position))
  }

  // varias relacoes entre o mesmo par ganham desvios diferentes para nao se sobreporem
  const edges = useMemo<GraphEdge[]>(() => {
    const seen = new Map<string, number>()
    const relationEdges: RelationEdgeType[] = visibleRelations.map((r) => {
      const key = [r.source.id, r.target.id].sort().join('|')
      const index = seen.get(key) ?? 0
      seen.set(key, index + 1)
      const offset = index === 0 ? 0 : (index % 2 === 1 ? 1 : -1) * Math.ceil(index / 2) * 14
      return {
        id: r.id,
        source: r.source.id,
        target: r.target.id,
        type: 'relation' as const,
        data: {
          relationship: r,
          highlighted: !!selectedId && (r.source.id === selectedId || r.target.id === selectedId),
          offset,
        },
      }
    })
    const coverageEdges: TestEdgeType[] = visibleTests.flatMap((t) =>
      t.covers
        .filter((e) => visibleIds.has(e.id))
        .map((e) => ({
          id: `cov:${t.id}:${e.id}`,
          source: TEST_NODE + t.id,
          target: e.id,
          type: 'covers' as const,
          data: { highlighted: e.id === selectedId, pending: t.submissionStatus === 'DRAFT' },
        })),
    )
    return [...relationEdges, ...coverageEdges]
  }, [visibleRelations, visibleTests, visibleIds, selectedId])

  const usedTypes = useMemo(() => {
    const map = new Map<string, RelationshipType>()
    relationships.forEach((r) => map.set(r.type.id, r.type))
    return sortTypes([...map.values()])
  }, [relationships])

  const select = (id: string | null) => setParams(id ? { el: id } : {}, { replace: true })
  const toggleKind = (kind: ElementKind) =>
    setHiddenKinds((prev) => {
      const next = new Set(prev)
      if (next.has(kind)) next.delete(kind)
      else next.add(kind)
      return next
    })

  // clicar num teste abre a edicao dele (quem so visualiza ve a descricao ao passar o mouse)
  const onNodeClick: NodeMouseHandler<GraphNode> = (_, node) => {
    if (node.type === 'test') {
      if (canEdit) setAction({ mode: 'editTest', test: node.data.test })
      return
    }
    select(node.id)
  }
  const onEdgeClick: EdgeMouseHandler<GraphEdge> = (_, edge) => {
    if (edge.type !== 'relation') return
    const r = edge.data?.relationship
    if (!r) return
    const source = elements.find((e) => e.id === r.source.id)
    const target = elements.find((e) => e.id === r.target.id)
    if (source && target) setAction({ mode: 'pair', source, target })
  }

  const pairRelations =
    action?.mode === 'pair'
      ? relationships.filter(
          (r) =>
            (r.source.id === action.source.id && r.target.id === action.target.id) ||
            (r.source.id === action.target.id && r.target.id === action.source.id),
        )
      : []

  return (
    <>
      <ModelHeader project={project} view="graph" elements={elements} changes={changes} />

      <div className="gr-body">
        <section className="gr-canvas dots" aria-label="Grafo de rastreabilidade">
          {elementsQuery.isPending && <p className="hint gr-msg">Carregando o modelo…</p>}
          {elementsQuery.isError && <div className="gr-msg"><FieldError>{elementsQuery.error.message}</FieldError></div>}
          {elementsQuery.isSuccess && elements.length === 0 && (
            <div className="gr-msg card">
              <strong>Nenhum elemento cadastrado ainda.</strong>
              <span className="hint">O grafo aparece quando o projeto tiver requisitos ou regras de negócio.</span>
              {canEdit && (
                <Link className="btn p" to={`${base}/elementos/novo`}><PlusIcon />Novo elemento</Link>
              )}
            </div>
          )}

          {ready && elements.length > 0 && (
            <>
              <svg className="gr-defs" aria-hidden="true">
                <defs>
                  <marker id="gr-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
                    <path d="M0,0 L10,5 L0,10 z" className="gr-arrow" />
                  </marker>
                  <marker id="gr-arrow-hl" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
                    <path d="M0,0 L10,5 L0,10 z" className="gr-arrow hl" />
                  </marker>
                  <marker id="gr-arrow-test" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
                    <path d="M0,0 L10,5 L0,10 z" className="gr-arrow test" />
                  </marker>
                </defs>
              </svg>

              {/* sem elementos visiveis, o canvas fica vazio; filtros e legenda continuam para reativar os tipos */}
              {nodes.length > 0 && (
                <ReactFlow
                  nodes={nodes}
                  edges={edges}
                  nodeTypes={nodeTypes}
                  edgeTypes={edgeTypes}
                  onNodesChange={handleNodesChange}
                  onNodeDragStop={onNodeDragStop}
                  onNodeClick={onNodeClick}
                  onEdgeClick={onEdgeClick}
                  onPaneClick={() => select(null)}
                  nodesConnectable={false}
                  fitView
                  fitViewOptions={FIT_OPTIONS}
                  minZoom={MIN_ZOOM}
                  maxZoom={MAX_ZOOM}
                  // licenca MIT permite esconder; os avisos de licenca vao num arquivo de terceiros ao distribuir
                  proOptions={{ hideAttribution: true }}
                />
              )}

              <div className="gr-filters">
                {KINDS.map((k) => (
                  <button
                    key={k.kind}
                    type="button"
                    className="gr-tg"
                    aria-pressed={!hiddenKinds.has(k.kind)}
                    onClick={() => toggleKind(k.kind)}
                  >
                    <span className={`gr-sw ${k.chip}`} />
                    {k.plural}
                  </button>
                ))}
                <button type="button" className="gr-tg" aria-pressed={showTests} onClick={() => setShowTests((v) => !v)}>
                  <span className="gr-sw t" />
                  Testes
                </button>
                <div style={{ flex: 1 }} />
                <button type="button" className="gr-tg" aria-pressed={showCoverage} onClick={() => setShowCoverage((v) => !v)}>
                  <span className="gr-cov no" />
                  Destacar sem teste
                </button>
              </div>

              <div className="gr-legend card">
                <div className="gr-legend-row">
                  {usedTypes.map((t) => (
                    <span key={t.id} className="gr-lg">
                      <RelationLine style={typeMeta(t).line} width={26} />
                      {t.name}
                    </span>
                  ))}
                </div>
                <div className="gr-legend-row">
                  <span className="gr-lg"><span className="gr-cov" />Com teste</span>
                  <span className="gr-lg"><span className="gr-cov no" />Sem teste</span>
                  {showTests && tests.length > 0 && (
                    <span className="gr-lg"><span className="gr-test-sample" />Testa</span>
                  )}
                  <span className="gr-lg"><span className="gr-pending-sample" />Rascunho</span>
                </div>
              </div>

              {nodes.length > 0 && <ZoomControls />}
            </>
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
          emptyHint="Clique num elemento do grafo para ver os detalhes e os vínculos dele."
        />
      </div>

      {action?.mode === 'add' && (
        <AddRelationshipModal
          projectId={project.id}
          origin={action.origin}
          elements={elements}
          relationships={relationships}
          onClose={() => setAction(null)}
          onCreated={() => setAction(null)}
        />
      )}

      {action?.mode === 'pair' && pairRelations.length > 0 && (
        <CellRelationsDialog
          row={action.source}
          column={action.target}
          relationships={pairRelations}
          canEdit={canEdit}
          onClose={() => setAction(null)}
          onAdd={() => setAction({ mode: 'add', origin: action.source })}
          onEdit={(relationship) => setAction({ mode: 'edit', relationship })}
          onRemove={(relationship) => setAction({ mode: 'remove', relationship })}
        />
      )}

      {action?.mode === 'editTest' && (
        <TestDialog
          projectId={project.id}
          elements={elements}
          tests={tests}
          test={action.test}
          onClose={() => setAction(null)}
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

/** Aproximar, afastar e enquadrar tudo; clicar na porcentagem permite digitar o zoom. */
function ZoomControls() {
  const { zoomIn, zoomOut, zoomTo, fitView } = useReactFlow()
  const { zoom } = useViewport()
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState('')

  function startEditing() {
    setDraft(String(Math.round(zoom * 100)))
    setEditing(true)
  }

  function apply() {
    const value = Number(draft.replace('%', '').replace(',', '.'))
    if (Number.isFinite(value) && value > 0) {
      zoomTo(Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, value / 100)), { duration: 150 })
    }
    setEditing(false)
  }

  return (
    <div className="gr-zoom card" role="group" aria-label="Zoom">
      <button className="btn ic g" type="button" aria-label="Aproximar" onClick={() => zoomIn()}>
        <PlusIcon />
      </button>
      {editing ? (
        <input
          className="gr-zoom-input"
          aria-label="Zoom em porcentagem"
          inputMode="numeric"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onBlur={apply}
          onKeyDown={(e) => {
            if (e.key === 'Enter') apply()
            if (e.key === 'Escape') setEditing(false)
          }}
          onFocus={(e) => e.target.select()}
          autoFocus
        />
      ) : (
        <button type="button" className="mono gr-zoom-n" title="Clique para digitar o zoom" onClick={startEditing}>
          {Math.round(zoom * 100)}%
        </button>
      )}
      <button className="btn ic g" type="button" aria-label="Afastar" onClick={() => zoomOut()}>
        <svg className="ico" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14" /></svg>
      </button>
      <button className="btn ic g gr-zoom-fit" type="button" aria-label="Enquadrar tudo" onClick={() => fitView(FIT_OPTIONS)}>
        <SearchIcon />
      </button>
    </div>
  )
}
