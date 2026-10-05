import { useEffect, useId, type ReactNode } from 'react'
import { CloseIcon } from './icons'
import './Modal.css'

interface ModalProps {
  title: string
  subtitle?: string
  onClose: () => void
  /** Rodape com os botoes de acao (Cancelar / Confirmar). */
  footer: ReactNode
  children: ReactNode
}

/** Dialogo no padrao do design: fundo escurecido, cabecalho, corpo e rodape com acoes. Esc fecha. */
export function Modal({ title, subtitle, onClose, footer, children }: ModalProps) {
  const titleId = useId()

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    // impede a pagina de fundo de rolar enquanto o modal esta aberto
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = overflow
    }
  }, [onClose])

  return (
    <div className="modal-root">
      <div className="modal-overlay" onClick={onClose} />
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby={titleId}>
        <div className="modal-head">
          <div className="modal-titles">
            <h2 id={titleId}>{title}</h2>
            {subtitle && <p>{subtitle}</p>}
          </div>
          <button className="btn ic g" type="button" aria-label="Fechar" onClick={onClose}>
            <CloseIcon />
          </button>
        </div>
        <div className="modal-body">{children}</div>
        <div className="modal-foot">{footer}</div>
      </div>
    </div>
  )
}
