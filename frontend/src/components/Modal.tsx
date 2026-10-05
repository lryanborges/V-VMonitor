import { useEffect, useId, type ReactNode } from 'react'
import { CloseIcon } from './icons'
import './Modal.css'

interface ModalProps {
  title: string
  /** Texto ou conteudo livre abaixo do titulo (ex.: o elemento de origem). */
  subtitle?: ReactNode
  /** md: 540px (padrao); lg: 660px, para formularios com mais de uma etapa. */
  size?: 'md' | 'lg'
  /** Icone ao lado do titulo; com tone 'danger', em destaque de alerta (ex.: confirmar exclusao). */
  icon?: ReactNode
  tone?: 'default' | 'danger'
  onClose: () => void
  /** Rodape com os botoes de acao (Cancelar / Confirmar). */
  footer: ReactNode
  children: ReactNode
}

/** Dialogo no padrao do design: fundo escurecido, cabecalho, corpo e rodape com acoes. Esc fecha. */
export function Modal({ title, subtitle, size = 'md', icon, tone = 'default', onClose, footer, children }: ModalProps) {
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
      <div className={`modal ${size}`} role={tone === 'danger' ? 'alertdialog' : 'dialog'} aria-modal="true" aria-labelledby={titleId}>
        <div className="modal-head">
          {icon && <span className={`modal-icon ${tone}`}>{icon}</span>}
          <div className="modal-titles">
            <h2 id={titleId}>{title}</h2>
            {subtitle && <div className="modal-subtitle">{subtitle}</div>}
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
