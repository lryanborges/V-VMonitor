import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { versionKeys, versionsApi } from '../../api/versions'
import { FieldError } from '../../components/FieldError'
import { PageHeader } from '../../components/PageHeader'
import { useProject } from '../../layouts/useProject'
import { timeAgo } from '../../utils/format'
import { ChangeCounts } from './ChangeCounts'
import './Versions.css'

/** RF18: versoes do projeto, mais recentes primeiro. Cada uma abre o modelo como estava naquele momento. */
export function VersionsPage() {
  const project = useProject()
  const base = `/projetos/${project.id}`
  const versions = useQuery({ queryKey: versionKeys.all(project.id), queryFn: () => versionsApi.list(project.id) })

  return (
    <>
      <PageHeader title="Versões" />

      <section className="vs-content">
        {versions.isPending && <p className="hint">Carregando versões…</p>}
        {versions.isError && <FieldError>{versions.error.message}</FieldError>}

        {versions.isSuccess && versions.data.length === 0 && (
          <div className="card vs-empty">
            <strong>Nenhuma versão ainda.</strong>
            <span className="hint">A primeira versão (v1) é criada ao submeter o modelo.</span>
            <Link className="btn" to={base}>Ir para o modelo</Link>
          </div>
        )}

        {versions.isSuccess && versions.data.length > 0 && (
          <ol className="card vs-list">
            {versions.data.map((v, i) => (
              <li key={v.id}>
                <Link className="vs-item" to={`${base}/versoes/${v.number}`}>
                  <span className="pill mono vs-number">v{v.number}</span>
                  <span className="vs-main">
                    <span className="vs-label">
                      {v.label ?? <span className="hint">Sem nome</span>}
                      {i === 0 && <span className="pill ok">Mais recente</span>}
                    </span>
                    <span className="hint" title={new Date(v.createdAt).toLocaleString('pt-BR')}>
                      {v.createdBy.name ?? 'Usuário removido'} · {timeAgo(v.createdAt)}
                    </span>
                  </span>
                  <span className="hint vs-totals">
                    {v.elements} {v.elements === 1 ? 'elemento' : 'elementos'} · {v.relationships}{' '}
                    {v.relationships === 1 ? 'relacionamento' : 'relacionamentos'}
                    {v.tests > 0 && ` · ${v.tests} ${v.tests === 1 ? 'teste' : 'testes'}`}
                  </span>
                  <ChangeCounts changes={v.changes} />
                </Link>
              </li>
            ))}
          </ol>
        )}
      </section>
    </>
  )
}
