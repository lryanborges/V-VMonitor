import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { projectKeys, projectsApi } from '../../api/projects'
import type { Project } from '../../api/types'
import { FieldError } from '../../components/FieldError'
import { PlusIcon, SearchIcon } from '../../components/icons'
import { NewProjectModal } from './NewProjectModal'
import { ProjectCard } from './ProjectCard'
import { ProjectsEmpty } from './ProjectsEmpty'
import './ProjectsPage.css'

type Filter = 'all' | 'mine' | 'shared'

const FILTERS: { value: Filter; label: string; match: (p: Project) => boolean }[] = [
  { value: 'all', label: 'Todos', match: () => true },
  { value: 'mine', label: 'Meus', match: (p) => p.role === 'OWNER' },
  { value: 'shared', label: 'Compartilhados comigo', match: (p) => p.role !== 'OWNER' },
]

/** Busca sem diferenciar maiusculas nem acentos ("clinica" encontra "Clínica"). */
function normalize(text: string) {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

/** RF3, UC-04: projetos proprios e compartilhados. Com newProject, abre o modal de criacao (RF4). */
export function ProjectsPage({ newProject = false }: { newProject?: boolean }) {
  const navigate = useNavigate()
  const [filter, setFilter] = useState<Filter>('all')
  const [search, setSearch] = useState('')
  const projects = useQuery({ queryKey: projectKeys.all, queryFn: projectsApi.list })

  const all = useMemo(() => projects.data ?? [], [projects.data])
  const visible = useMemo(() => {
    const term = normalize(search.trim())
    const byFilter = FILTERS.find((f) => f.value === filter)!.match
    return all.filter(
      (p) => byFilter(p) && (!term || normalize(`${p.name} ${p.description ?? ''}`).includes(term)),
    )
  }, [all, filter, search])

  const isEmpty = projects.isSuccess && all.length === 0

  return (
    <main className="page">
      <div className="page-head">
        <div>
          <h1>Projetos</h1>
          <p>Projetos criados por você e compartilhados com você.</p>
        </div>
        {!isEmpty && (
          <Link className="btn p" to="/projetos/novo" style={{ height: 40 }}>
            <PlusIcon />
            Novo projeto
          </Link>
        )}
      </div>

      {projects.isPending && <p className="hint">Carregando projetos…</p>}
      {projects.isError && <FieldError>{projects.error.message}</FieldError>}

      {isEmpty && <ProjectsEmpty />}

      {projects.isSuccess && !isEmpty && (
        <>
          <div className="pj-toolbar">
            <div className="seg" role="group" aria-label="Filtrar projetos">
              {FILTERS.map((f) => (
                <button
                  key={f.value}
                  type="button"
                  aria-pressed={filter === f.value}
                  onClick={() => setFilter(f.value)}
                >
                  {f.label} <span className="n">{all.filter(f.match).length}</span>
                </button>
              ))}
            </div>
            <div className="pj-search">
              <SearchIcon />
              <label htmlFor="pj-busca" className="sr-only">Buscar projetos</label>
              <input
                id="pj-busca"
                className="inp sm with-icon"
                type="search"
                placeholder="Buscar projetos"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>

          <div className="pj-grid">
            {visible.map((p) => (
              <ProjectCard key={p.id} project={p} />
            ))}
            {filter !== 'shared' && !search && (
              <Link className="newc" to="/projetos/novo">
                <PlusIcon style={{ width: 22, height: 22 }} />
                Novo projeto
              </Link>
            )}
          </div>

          {visible.length === 0 && (
            <p className="hint" style={{ textAlign: 'center' }}>
              {search ? `Nenhum projeto encontrado para “${search}”.` : 'Nenhum projeto compartilhado com você ainda.'}
            </p>
          )}
        </>
      )}

      {newProject && <NewProjectModal onClose={() => navigate('/projetos')} />}
    </main>
  )
}
