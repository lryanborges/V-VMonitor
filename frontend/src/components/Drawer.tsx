import { useEffect, useId, type ReactNode } from 'react'
import { CloseIcon } from './icons'
import './Drawer.css'

interface DrawerProps {
  title: string
  onClose: () => void
  /** Rodape com os botoes de acao (Cancelar / Confirmar). */
  footer: ReactNode
  children: ReactNode
}

/** Painel que desliza da direita, no padrao do design (formularios longos, como o de elemento). Esc fecha. */
export function Drawer({ title, onClose, footer, children }: DrawerProps) {
  const titleId = useId()

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="drawer-root">
      <div className="drawer-overlay" onClick={onClose} />
      <div className="drawer" role="dialog" aria-modal="true" aria-labelledby={titleId}>
        <div className="drawer-head">
          <h2 id={titleId}>{title}</h2>
          <button className="btn ic g" type="button" aria-label="Fechar" onClick={onClose}>
            <CloseIcon />
          </button>
        </div>
        <div className="drawer-body">{children}</div>
        <div className="drawer-foot">{footer}</div>
      </div>
    </div>
  )
}
