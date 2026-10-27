import { useId } from 'react'
import { InfoIcon } from './icons'
import './InfoTip.css'

interface InfoTipProps {
  /** Texto de ajuda exibido no balao. */
  children: string
  /** Lado em que o balao abre, em relacao ao botao. */
  align?: 'left' | 'right'
}

/** Botao "i" que mostra uma explicacao ao passar o mouse ou focar com o teclado. */
export function InfoTip({ children, align = 'right' }: InfoTipProps) {
  const id = useId()
  return (
    <span className={`infotip ${align}`}>
      <button className="btn ic g sm" type="button" aria-label="Como usar" aria-describedby={id}>
        <InfoIcon />
      </button>
      <span role="tooltip" id={id} className="infotip-bubble">{children}</span>
    </span>
  )
}
