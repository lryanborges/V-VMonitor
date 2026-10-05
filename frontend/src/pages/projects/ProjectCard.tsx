import { Link } from 'react-router'
import type { Project } from '../../api/types'
import { activityLabel, initials } from '../../utils/format'

const MAX_AVATARS = 4

/** Card da tela de projetos: versao, descricao, numeros, cobertura, membros e atividade. */
export function ProjectCard({ project }: { project: Project }) {
  const { stats } = project
  const shared = project.role !== 'OWNER'
  const hidden = project.members.length - MAX_AVATARS

  return (
    <Link className="pc" to={`/projetos/${project.id}`}>
      <div className="pc-top">
        <div className="pc-title">
          <span>{project.name}</span>
          <span className="pill mono">{project.latestVersion ? `v${project.latestVersion}` : 'rascunho'}</span>
        </div>
        <p className="pc-desc">{project.description ?? 'Sem descrição.'}</p>
      </div>

      <div className="pc-stats">
        <div className="stat"><b>{stats.requirements}</b><span>Requisitos</span></div>
        <div className="stat"><b>{stats.businessRules}</b><span>Regras</span></div>
        <div className="stat"><b>{stats.tests}</b><span>Testes</span></div>
      </div>

      {stats.requirements > 0 ? (
        <div className="pc-cov">
          <span className="hint" style={{ width: 118 }}>Cobertura de testes</span>
          <div className={stats.coverage < 20 ? 'bar lo' : 'bar'}>
            <i style={{ width: `${stats.coverage}%` }} />
          </div>
          <span className="mono pc-cov-n">{stats.coverage}%</span>
        </div>
      ) : (
        <div className="hint pc-cov">Nenhum requisito cadastrado ainda</div>
      )}

      <div className="pc-foot">
        <div className="avs" title={project.members.map((m) => m.name).join(', ')}>
          {project.members.slice(0, MAX_AVATARS).map((m) => (
            <span key={m.id} className="av">{initials(m.name)}</span>
          ))}
          {hidden > 0 && <span className="av">+{hidden}</span>}
        </div>
        {shared && project.owner.name && <span className="pill a">De {project.owner.name}</span>}
        <span className="hint">{activityLabel(project.createdAt, project.updatedAt)}</span>
      </div>
    </Link>
  )
}
