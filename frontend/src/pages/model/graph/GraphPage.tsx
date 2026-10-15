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
} from '@xyflow/react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { Element, ElementKind, Relationship, RelationshipType } from '../../../api/types'
import { FieldError } from '../../../components/FieldError'
import { PlusIcon, SearchIcon } from '../../../components/icons'
import { RelationLine } from '../../../components/RelationLine'
import { useProject } from '../../../layouts/useProject'
import { layoutGraph } from '../../../utils/graphLayout'
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
import './GraphPage.css'

/** Zoom maximo ao enquadrar (ao abrir e no botao): evita nos gigantes em modelos pequenos. */
const FIT_OPTIONS = { padding: 0.2, maxZoom: 1.5 }
const MIN_ZOOM = 0.2
const MAX_ZOOM = 2.5

const nodeTypes = { element: ElementNode }
const edgeTypes = { relation: RelationEdge }

type GraphAction =
  | { mode: 'add'; origin: Element }
  | { mode: 'pair'; source: Element; target: Element }
  | { mode: 'edit'; relationship: Relationship }
  | { mode: 'remove'; relationship: Relationship }

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
  const { elementsQuery, relationshipsQuery, elements, relationships, drafts } = useModelData(project.id)
  // o layout depende das relacoes: so monta o grafo com elementos e relacoes carregados
  const ready = elementsQuery.isSuccess && relationshipsQuery.isSuccess
  const [params, setParams] = useSearchParams()
  const [hiddenKinds, setHiddenKinds] = useState<Set<ElementKind>>(new Set())
  const [showCoverage, setShowCoverage] = useState(true)
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

  // layout automatico; arrastar e permitido, mas a posicao nao e salva (recalcula quando o modelo muda)
  const positions = useMemo(() => layoutGraph(visible, visibleRelations), [visible, visibleRelations])
  const [nodes, setNodes, onNodesChange] = useNodesState<ElementNodeType>([])
  // posicoes que o usuario arrastou nesta sessao; os demais nos seguem o layout automatico
  const dragged = useRef(new Map<string, { x: number; y: number }>())

  const handleNodesChange = (changes: NodeChange<ElementNodeType>[]) => {
    changes.forEach((ch) => {
      if (ch.type === 'position' && ch.dragging && ch.position) dragged.current.set(ch.id, ch.position)
    })
    onNodesChange(changes)
  }

  useEffect(() => {
    setNodes(
      visible.map((element) => ({
        id: element.id,
        type: 'element' as const,
        position: dragged.current.get(element.id) ?? positions.get(element.id) ?? { x: 0, y: 0 },
        data: { element, selected: element.id === selectedId, showCoverage },
      })),
    )
  }, [visible, positions, selectedId, showCoverage, setNodes])

  // varias relacoes entre o mesmo par ganham desvios diferentes para nao se sobreporem
  const edges = useMemo<RelationEdgeType[]>(() => {
    const seen = new Map<string, number>()
    return visibleRelations.map((r) => {
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
  }, [visibleRelations, selectedId])

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

  const onNodeClick: NodeMouseHandler<ElementNodeType> = (_, node) => select(node.id)
  const onEdgeClick: EdgeMouseHandler<RelationEdgeType> = (_, edge) => {
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
      <ModelHeader project={project} view="graph" elements={elements} relationships={relationships} drafts={drafts} />

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

          {ready && elements.length > 0 && nodes.length > 0 && (
            <>
              <svg className="gr-defs" aria-hidden="true">
                <defs>
                  <marker id="gr-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
                    <path d="M0,0 L10,5 L0,10 z" className="gr-arrow" />
                  </marker>
                  <marker id="gr-arrow-hl" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
                    <path d="M0,0 L10,5 L0,10 z" className="gr-arrow hl" />
                  </marker>
                </defs>
              </svg>

              <ReactFlow
                nodes={nodes}
                edges={edges}
                nodeTypes={nodeTypes}
                edgeTypes={edgeTypes}
                onNodesChange={handleNodesChange}
                onNodeClick={onNodeClick}
                onEdgeClick={onEdgeClick}
                onPaneClick={() => select(null)}
                nodesConnectable={false}
                fitView
                fitViewOptions={FIT_OPTIONS}
                minZoom={MIN_ZOOM}
                maxZoom={MAX_ZOOM}
                attributionPosition="top-right"
              />

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
                <button type="button" className="gr-tg" aria-pressed={false} disabled title="Em breve (cadastro de testes, RF9)">
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
                  <span className="gr-lg"><span className="gr-pending-sample" />Rascunho</span>
                </div>
              </div>

              <ZoomControls />
            </>
          )}
        </section>

        <ElementPanel
          projectId={project.id}
          element={selected}
          elements={elements}
          relationships={relationships}
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
