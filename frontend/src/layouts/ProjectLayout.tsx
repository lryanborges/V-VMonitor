import { useQuery } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { Link, NavLink, Outlet, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { projectKeys, projectsApi } from '../api/projects'
import { AppHeader } from '../components/AppHeader'
import { FieldError } from '../components/FieldError'
import { BackIcon, GraphIcon, HistoryIcon, MembersIcon, PlugIcon } from '../components/icons'
import { ShareProjectDialog } from '../pages/members/ShareProjectDialog'
import { ROLE_LABEL } from '../utils/labels'
import { ShareContext } from './shareContext'
import './ProjectLayout.css'

/** Moldura das telas de um projeto: cabecalho padrao, menu lateral do projeto e conteudo. */
export function ProjectLayout() {
  const { projectId = '' } = useParams()
  const project = useQuery({ queryKey: projectKeys.detail(projectId), queryFn: () => projectsApi.get(projectId) })
  const notFound = project.error instanceof ApiError && (project.error.status === 404 || project.error.status === 400)

  const data = project.data
  // RF20: janela de compartilhamento, aberta pelo menu (Membros) ou pelo cabecalho das telas (Compartilhar)
  const [sharing, setSharing] = useState(false)
  const elements = data ? data.stats.requirements + data.stats.businessRules : null

  return (
    <div className="pl">
      <AppHeader />
      <div className="pl-row">
        <aside className="pl-side" aria-label="Navegação do projeto">
          <div className="pl-project">
            <Link className="nav" to="/projetos">
              <BackIcon />
              Todos os projetos
            </Link>
            <div className="pl-project-card">
              <div className="eyebrow">Projeto</div>
              <div className="pl-project-name">{data?.name ?? '…'}</div>
              {data && <div className="hint pl-project-role">{ROLE_LABEL[data.role]}</div>}
            </div>
          </div>

          <nav aria-label="Seções do projeto" className="pl-nav">
            <NavLink className={({ isActive }) => `nav${isActive ? ' on' : ''}`} to={`/projetos/${projectId}`} end>
              <GraphIcon />
              Modelo
              {elements !== null && <span className="n">{elements}</span>}
            </NavLink>
            <NavLink className={({ isActive }) => `nav${isActive ? ' on' : ''}`} to={`/projetos/${projectId}/versoes`}>
              <HistoryIcon />
              Versões
              {data?.latestVersion && <span className="n">v{data.latestVersion}</span>}
            </NavLink>
            <button className="nav" type="button" disabled={!data} onClick={() => setSharing(true)}>
              <MembersIcon />
              Membros
              {data && <span className="n">{data.members.length}</span>}
            </button>
            <SoonNav icon={<PlugIcon />} label="Integrações" />
          </nav>
        </aside>

        <div className="pl-main">
          {project.isPending && <p className="hint pl-msg">Carregando projeto…</p>}
          {notFound && (
            <div className="pl-msg">
              <h1 style={{ margin: 0, fontSize: 24 }}>Projeto não encontrado</h1>
              <p className="hint">Ele não existe ou você não tem acesso a ele.</p>
              <Link className="btn" to="/projetos" style={{ alignSelf: 'flex-start' }}>Ver meus projetos</Link>
            </div>
          )}
          {project.isError && !notFound && (
            <div className="pl-msg"><FieldError>{project.error.message}</FieldError></div>
          )}
          {data && (
            <ShareContext.Provider value={() => setSharing(true)}>
              <Outlet context={data} />
            </ShareContext.Provider>
          )}
        </div>
      </div>

      {sharing && data && <ShareProjectDialog project={data} onClose={() => setSharing(false)} />}
    </div>
  )
}

/** Secao ainda nao implementada: aparece no menu, desabilitada. */
function SoonNav({ icon, label, count }: { icon: ReactNode; label: string; count?: string }) {
  return (
    <span className="nav disabled" aria-disabled="true" title="Em breve">
      {icon}
      {label}
      {count && <span className="n">{count}</span>}
    </span>
  )
}
