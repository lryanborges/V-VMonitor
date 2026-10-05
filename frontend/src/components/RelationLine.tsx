import type { LineStyle } from '../utils/relationships'

/**
 * Amostra de linha que identifica o tipo de relacionamento, no padrao do design:
 * dependencia solida, refinamento tracejada, similaridade pontilhada, conflito vermelha.
 */
export function RelationLine({ style, width = 28 }: { style: LineStyle; width?: number }) {
  return (
    <svg width={width} height="8" aria-hidden="true" className="rl-line">
      <path className={`rl ${style}`} d={`M1 4H${width - 1}`} />
    </svg>
  )
}
