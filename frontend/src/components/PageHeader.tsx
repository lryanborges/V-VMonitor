import type { ReactNode } from 'react'
import './PageHeader.css'

interface PageHeaderProps {
  title: ReactNode
  /** Conteudo a direita do titulo (alternancias, selos, botoes). */
  children?: ReactNode
}

/** Barra de 60px no topo das telas de um projeto (modelo, versoes): titulo e acoes. */
export function PageHeader({ title, children }: PageHeaderProps) {
  return (
    <header className="page-header">
      <h1>{title}</h1>
      {children}
    </header>
  )
}
