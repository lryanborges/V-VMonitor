import type { ChangeGroup, ElementSnapshot, VersionedRelationship, VersionedTest } from '../../api/types'
import { kindInfo, priorityInfo } from '../../utils/labels'
import { typeMeta } from '../../utils/relationships'
import './ModelChangesView.css'

interface ModelChangesViewProps {
  elements: ChangeGroup<ElementSnapshot>
  relationships: ChangeGroup<VersionedRelationship>
  tests: ChangeGroup<VersionedTest>
}

/**
 * Mudancas no modelo em tres grupos (adicionados, alterados e removidos), separando elementos, relacoes e testes.
 * Usado ao submeter (desde a ultima versao) e ao comparar versoes.
 */
export function ModelChangesView({ elements, relationships, tests }: ModelChangesViewProps) {
  const groups = [
    { key: 'added', title: 'Adicionados', els: elements.added, rels: relationships.added, tests: tests.added },
    { key: 'modified', title: 'Alterados', els: elements.modified, rels: relationships.modified, tests: tests.modified },
    { key: 'removed', title: 'Removidos', els: elements.removed, rels: relationships.removed, tests: tests.removed },
  ] as const

  return (
    <div className="chg">
      {groups.map((g) => {
        const count = g.els.length + g.rels.length + g.tests.length
        if (count === 0) return null
        return (
          <section key={g.key} className={`chg-group ${g.key}`}>
            <span className="eyebrow">
              {g.title} <span className="mono">{count}</span>
            </span>
            <ul className="chg-list">
              {g.key === 'modified'
                ? elements.modified.map(({ before, after }) => (
                    <li key={after.id}><ModifiedElement before={before} after={after} /></li>
                  ))
                : (g.els as ElementSnapshot[]).map((e) => (
                    <li key={e.id}><ElementLine element={e} /></li>
                  ))}
              {g.key === 'modified'
                ? relationships.modified.map(({ before, after }) => (
                    <li key={after.id} className="chg-rel-change">
                      <RelationLineText relationship={before} className="chg-old" />
                      <span className="chg-arrow" aria-label="passou a ser">→</span>
                      <RelationLineText relationship={after} />
                    </li>
                  ))
                : (g.rels as VersionedRelationship[]).map((r) => (
                    <li key={r.id}><RelationLineText relationship={r} /></li>
                  ))}
              {g.key === 'modified'
                ? tests.modified.map(({ before, after }) => (
                    <li key={after.id}><ModifiedTest before={before} after={after} /></li>
                  ))
                : (g.tests as VersionedTest[]).map((t) => (
                    <li key={t.id}><TestLine test={t} /></li>
                  ))}
            </ul>
          </section>
        )
      })}
    </div>
  )
}

function ElementLine({ element }: { element: ElementSnapshot }) {
  return (
    <span className="chg-el">
      <span className={`chip ${kindInfo(element.kind).chip}`}>{element.code}</span>
      <span className="chg-desc" title={element.description}>{element.description}</span>
    </span>
  )
}

/** Elemento alterado: so o que mudou (descricao e/ou prioridade), antes riscado e depois. */
function ModifiedElement({ before, after }: { before: ElementSnapshot; after: ElementSnapshot }) {
  const prio = (e: ElementSnapshot) => (e.priority ? priorityInfo(e.priority).label : '—')
  return (
    <span className="chg-el chg-modified">
      <span className={`chip ${kindInfo(after.kind).chip}`}>{after.code}</span>
      <span className="chg-diff">
        {before.description !== after.description && (
          <span>
            <s className="chg-old">{before.description}</s> <span className="chg-arrow">→</span> {after.description}
          </span>
        )}
        {before.priority !== after.priority && (
          <span className="hint">
            Prioridade: <s>{prio(before)}</s> <span className="chg-arrow">→</span> {prio(after)}
          </span>
        )}
      </span>
    </span>
  )
}

const codes = (t: VersionedTest) => (t.covers.length === 0 ? 'sem requisito' : t.covers.map((e) => e.code).join(', '))

function TestLine({ test }: { test: VersionedTest }) {
  return (
    <span className="chg-el">
      <span className="chip t">{test.code}</span>
      <span className="chg-desc" title={test.description}>{test.description}</span>
      <span className="hint chg-covers">cobre {codes(test)}</span>
    </span>
  )
}

/** Teste alterado: descricao e/ou requisitos cobertos, antes riscado e depois. */
function ModifiedTest({ before, after }: { before: VersionedTest; after: VersionedTest }) {
  return (
    <span className="chg-el chg-modified">
      <span className="chip t">{after.code}</span>
      <span className="chg-diff">
        {before.description !== after.description && (
          <span>
            <s className="chg-old">{before.description}</s> <span className="chg-arrow">→</span> {after.description}
          </span>
        )}
        {codes(before) !== codes(after) && (
          <span className="hint">
            Cobre: <s>{codes(before)}</s> <span className="chg-arrow">→</span> {codes(after)}
          </span>
        )}
      </span>
    </span>
  )
}

/** Frase da relacao, ex.: "RF2 depende de RF1". */
function RelationLineText({ relationship: r, className }: { relationship: VersionedRelationship; className?: string }) {
  return (
    <span className={`chg-rel ${className ?? ''}`}>
      <span className={`chip ${kindInfo(r.source.kind).chip}`}>{r.source.code}</span>
      <span className="chg-verb">{typeMeta(r.type).verb.replace(/^—|→$/g, '')}</span>
      <span className={`chip ${kindInfo(r.target.kind).chip}`}>{r.target.code}</span>
    </span>
  )
}
