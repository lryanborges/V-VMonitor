import { useQuery } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { Link, NavLink, Outlet, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { projectKeys, projectsApi } from '../api/projects'
import type { MemberRole } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { AccountMenu } from '../components/AccountMenu'
import { FieldError } from '../components/FieldError'
import { BackIcon, GraphIcon, HistoryIcon, MembersIcon, PlugIcon } from '../components/icons'
import { ThemeToggle } from '../components/ThemeToggle'
import './ProjectLayout.css'

const ROLE_LABEL: Record<MemberRole, string> = {
  OWNER: 'Proprietário',
  EDITOR: 'Pode editar',
  VIEWER: 'Pode visualizar',
}

/** Moldura das telas de um projeto: menu lateral (secoes do projeto e conta) e conteudo a direita. */
export function ProjectLayout() {
  const { projectId = '' } = useParams()
  const { user } = useAuth()
  const project = useQuery({ queryKey: projectKeys.detail(projectId), queryFn: () => projectsApi.get(projectId) })
  const notFound = project.error instanceof ApiError && (project.error.status === 404 || project.error.status === 400)

  const data = project.data
  const elements = data ? data.stats.requirements + data.stats.businessRules : null

  return (
    <div className="pl">
      <aside className="pl-side">
        <Link to="/projetos" className="pl-brand" aria-label="V&V Monitor, ir para projetos">
          <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" style={{ color: 'var(--ac)' }}>
            <circle cx="6" cy="17" r="3.2" fill="none" stroke="currentColor" strokeWidth="2" />
            <circle cx="18" cy="7" r="3.2" fill="currentColor" />
            <path d="M8.6 15.2 15.4 8.8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
          <span>V&amp;V Monitor</span>
        </Link>

        <div className="pl-project">
          <Link className="nav" to="/projetos">
            <BackIcon />
            Todos os projetos
          </Link>
          <div className="pl-project-card">
            <div className="eyebrow">Projeto</div>
            <div className="pl-project-name">{data?.name ?? '…'}</div>
          </div>
        </div>

        <nav aria-label="Seções do projeto" className="pl-nav">
          <NavLink className={({ isActive }) => `nav${isActive ? ' on' : ''}`} to={`/projetos/${projectId}`} end>
            <GraphIcon />
            Modelo
            {elements !== null && <span className="n">{elements}</span>}
          </NavLink>
          <SoonNav icon={<HistoryIcon />} label="Versões" count={data?.latestVersion ? `v${data.latestVersion}` : undefined} />
          <SoonNav icon={<MembersIcon />} label="Membros" count={data ? String(data.members.length) : undefined} />
          <SoonNav icon={<PlugIcon />} label="Integrações" />
        </nav>

        <div className="pl-account">
          <AccountMenu placement="up">
            <span className="pl-who">
              <span className="pl-who-name">{user?.name}</span>
              <span className="hint">{data ? ROLE_LABEL[data.role] : ''}</span>
            </span>
          </AccountMenu>
          <ThemeToggle />
        </div>
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
        {data && <Outlet context={data} />}
      </div>
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
