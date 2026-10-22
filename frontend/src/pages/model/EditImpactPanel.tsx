import type { Relationship, VersionedTest } from '../../api/types'
import { AlertIcon } from '../../components/icons'
import { editImpact } from '../../utils/impact'
import { kindInfo } from '../../utils/labels'
import './EditImpactPanel.css'

interface EditImpactPanelProps {
  elementId: string
  relationships: Relationship[]
  tests: VersionedTest[]
}

/**
 * RF16 (ajuste para a v2 do documento): na edicao, mostra o que pode ser afetado pela alteracao, so como
 * informacao (nao bloqueia). Dependentes diretos e indiretos, demais vinculos e testes que cobrem o elemento.
 */
export function EditImpactPanel({ elementId, relationships, tests }: EditImpactPanelProps) {
  const impact = editImpact(elementId, relationships, tests)
  const total = impact.direct.length + impact.indirect.length + impact.others.length + impact.tests.length

  return (
    <section className="eip" aria-label="Pode ser afetado por esta alteração">
      <div className="eip-head">
        <AlertIcon />
        <strong>Pode ser afetado por esta alteração</strong>
      </div>

      {total === 0 && <p className="hint">Nenhum outro elemento ou teste é afetado diretamente.</p>}

      {impact.direct.length > 0 && (
        <div className="eip-group">
          <span className="eyebrow">Dependem deste elemento</span>
          {impact.direct.map((e) => (
            <span key={e.id} className="eip-item">
              <span className={`chip ${kindInfo(e.kind).chip}`}>{e.code}</span>
              <span className="eip-desc" title={e.description}>{e.description}</span>
            </span>
          ))}
        </div>
      )}

      {impact.indirect.length > 0 && (
        <div className="eip-group">
          <span className="eyebrow">Dependem indiretamente</span>
          {impact.indirect.map(({ element, via }) => (
            <span key={element.id} className="eip-item">
              <span className={`chip ${kindInfo(element.kind).chip}`}>{element.code}</span>
              <span className="eip-desc" title={element.description}>{element.description}</span>
              <span className="hint eip-via">via {via.code}</span>
            </span>
          ))}
        </div>
      )}

      {impact.others.length > 0 && (
        <div className="eip-group">
          <span className="eyebrow">Outros vínculos</span>
          {impact.others.map(({ element, label }, i) => (
            <span key={`${element.id}-${i}`} className="eip-item">
              <span className="hint eip-label">{label}</span>
              <span className={`chip ${kindInfo(element.kind).chip}`}>{element.code}</span>
              <span className="eip-desc" title={element.description}>{element.description}</span>
            </span>
          ))}
        </div>
      )}

      {impact.tests.length > 0 && (
        <div className="eip-group">
          <span className="eyebrow">Testes que podem precisar de ajuste</span>
          {impact.tests.map((t) => (
            <span key={t.id} className="eip-item">
              <span className="chip t">{t.code}</span>
              <span className="eip-desc" title={t.description}>{t.description}</span>
            </span>
          ))}
        </div>
      )}
    </section>
  )
}
