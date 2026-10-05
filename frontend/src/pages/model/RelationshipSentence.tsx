import type { ElementSummary, RelationshipType } from '../../api/types'
import { SwapIcon } from '../../components/icons'
import { RelationLine } from '../../components/RelationLine'
import { kindInfo } from '../../utils/labels'
import { typeMeta } from '../../utils/relationships'
import './RelationshipForm.css'

interface RelationshipSentenceProps {
  source: ElementSummary
  target: ElementSummary
  type: RelationshipType
  /** Mostra o botao de inverter (so faz sentido em tipos com direcao). */
  onSwap?: () => void
}

/** Previa da frase do relacionamento, ex.: "RF7 —depende de— RF5", com opcao de inverter a direcao. */
export function RelationshipSentence({ source, target, type, onSwap }: RelationshipSentenceProps) {
  const meta = typeMeta(type)
  return (
    <div className="arm-preview">
      <span className={`chip ${kindInfo(source.kind).chip}`}>{source.code}</span>
      <span className={`arm-verb ${meta.line}`}>
        <RelationLine style={meta.line} width={32} />
        {meta.verb.replace(/^—|→$/g, '')}
        <RelationLine style={meta.line} width={32} />
      </span>
      <span className={`chip ${kindInfo(target.kind).chip}`}>{target.code}</span>
      {onSwap && !type.symmetric && (
        <button
          className="btn ic g arm-swap"
          type="button"
          title="Inverter direção"
          aria-label="Inverter direção"
          onClick={onSwap}
        >
          <SwapIcon />
        </button>
      )}
    </div>
  )
}
