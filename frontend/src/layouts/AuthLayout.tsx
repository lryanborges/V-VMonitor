import type { ReactNode } from 'react'
import { Logo } from '../components/Logo'
import { ThemeToggle } from '../components/ThemeToggle'
import './AuthLayout.css'

export interface HeroNode {
  label: string
  kind: 'rf' | 'rnf' | 'rn' | 't'
  x: number
  y: number
}

export interface HeroEdge {
  from: [number, number]
  to: [number, number]
  /** ref: tracejada (refinamento); ts: verde (teste) */
  style?: 'ref' | 'ts'
}

interface AuthLayoutProps {
  eyebrow: string
  title: string
  subtitle: string
  nodes: HeroNode[]
  edges: HeroEdge[]
  children: ReactNode
}

/** Layout das telas de login e cadastro: painel ilustrativo a esquerda, formulario a direita. */
export function AuthLayout({ eyebrow, title, subtitle, nodes, edges, children }: AuthLayoutProps) {
  return (
    <div className="auth">
      <section className="auth-hero dots">
        <Logo />
        <div className="auth-graph" aria-hidden="true">
          <svg width="600" height="290">
            {edges.map((e, i) => (
              <path
                key={i}
                className={e.style ? `e ${e.style}` : 'e'}
                d={`M${e.from[0]} ${e.from[1]} L${e.to[0]} ${e.to[1]}`}
              />
            ))}
          </svg>
          {nodes.map((n) => (
            <span key={n.label} className={`node ${n.kind}`} style={{ left: n.x, top: n.y }}>
              {n.label}
            </span>
          ))}
        </div>
        <div className="auth-copy">
          <div className="eyebrow">{eyebrow}</div>
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-toolbar">
          <ThemeToggle />
        </div>
        <div className="auth-body">{children}</div>
      </section>
    </div>
  )
}
