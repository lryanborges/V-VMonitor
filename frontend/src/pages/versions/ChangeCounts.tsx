import type { ChangeSummary } from '../../api/types'

/** Resumo compacto das mudancas de uma versao, ex.: "+3 ~1 −2" (zeros omitidos). */
export function ChangeCounts({ changes }: { changes: ChangeSummary | null }) {
  if (!changes) return null
  const parts = [
    { n: changes.added, sign: '+', cls: 'add', label: 'adicionados' },
    { n: changes.modified, sign: '~', cls: 'mod', label: 'alterados' },
    { n: changes.removed, sign: '−', cls: 'rem', label: 'removidos' },
  ].filter((p) => p.n > 0)
  if (parts.length === 0) return <span className="hint">Sem mudanças</span>
  return (
    <span className="vs-counts">
      {parts.map((p) => (
        <span key={p.cls} className={`mono ${p.cls}`} title={`${p.n} ${p.label}`}>
          {p.sign}{p.n}
        </span>
      ))}
    </span>
  )
}
