import { Link } from 'react-router'
import { PlusIcon } from '../../components/icons'

/** UC-04, excecao do passo 2: sem projetos, o sistema informa e sugere a criacao. */
export function ProjectsEmpty() {
  return (
    <section className="empty dots">
      <div className="empty-graph" aria-hidden="true">
        <svg width="300" height="120">
          <path d="M150 60 L50 30" />
          <path d="M150 60 L60 98" />
          <path d="M150 60 L250 34" />
          <path d="M150 60 L244 96" />
        </svg>
        <span className="empty-node" style={{ left: 50, top: 30 }}>RN?</span>
        <span className="empty-node" style={{ left: 60, top: 98 }}>RNF?</span>
        <span className="empty-node on" style={{ left: 150, top: 60 }}>RF1</span>
        <span className="empty-node" style={{ left: 250, top: 34 }}>RF?</span>
        <span className="empty-node" style={{ left: 244, top: 96 }}>T?</span>
      </div>

      <div className="empty-copy">
        <h2>Você ainda não tem projetos</h2>
        <p>
          Crie um projeto para cadastrar requisitos e regras de negócio. Projetos que outras pessoas compartilharem com
          você também aparecem aqui.
        </p>
      </div>

      <Link className="btn p lg" to="/projetos/novo">
        <PlusIcon />
        Criar primeiro projeto
      </Link>

      <div className="empty-steps">
        <div className="step">
          <b>1</b>
          <div>
            <div className="step-t">Crie o projeto</div>
            <div className="step-d">Nome e uma descrição curta.</div>
          </div>
        </div>
        <div className="step">
          <b>2</b>
          <div>
            <div className="step-t">Cadastre os elementos</div>
            <div className="step-d">Requisitos e regras de negócio recebem ID automático.</div>
          </div>
        </div>
        <div className="step">
          <b>3</b>
          <div>
            <div className="step-t">Relacione e acompanhe</div>
            <div className="step-d">Veja vínculos e cobertura no grafo ou na matriz.</div>
          </div>
        </div>
      </div>
    </section>
  )
}
