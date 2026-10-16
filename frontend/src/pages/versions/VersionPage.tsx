import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { ApiError } from '../../api/client'
import { versionKeys, versionsApi, type CompareWith } from '../../api/versions'
import { FieldError } from '../../components/FieldError'
import { PageHeader } from '../../components/PageHeader'
import { useProject } from '../../layouts/useProject'
import { versionModel } from '../../utils/versions'
import { ElementPanel } from '../model/ElementPanel'
import { ElementTable } from '../model/ElementTable'
import { KindTabs, type KindFilter } from '../model/KindTabs'
import { ModelChangesView } from '../model/ModelChangesView'
import { ChangeCounts } from './ChangeCounts'
import './Versions.css'

type Tab = 'elements' | 'compare'

/**
 * RF18: o modelo como estava numa versao, so leitura (lista e painel do elemento), e a comparacao com a
 * versao anterior (padrao), com o modelo atual ou com outra versao.
 */
export function VersionPage() {
  const number = Number(useParams().number)
  // ao ir de uma versao para outra, aba, filtro e comparacao voltam ao padrao
  return <VersionView key={number} number={number} />
}

function VersionView({ number }: { number: number }) {
  const project = useProject()
  const [params, setParams] = useSearchParams()
  const [tab, setTab] = useState<Tab>('elements')
  const [filter, setFilter] = useState<KindFilter>('all')
  const [against, setAgainst] = useState<CompareWith>('previous')
  const base = `/projetos/${project.id}`

  const version = useQuery({
    queryKey: versionKeys.detail(project.id, number),
    queryFn: () => versionsApi.get(project.id, number),
    enabled: Number.isInteger(number),
  })
  const versions = useQuery({ queryKey: versionKeys.all(project.id), queryFn: () => versionsApi.list(project.id) })
  const comparison = useQuery({
    queryKey: versionKeys.compare(project.id, number, against),
    queryFn: () => versionsApi.compare(project.id, number, against),
    enabled: tab === 'compare' && version.isSuccess,
  })

  const model = useMemo(() => (version.data ? versionModel(version.data) : null), [version.data])
  const elements = model?.elements ?? []
  const rows = elements.filter((e) => filter === 'all' || e.kind === filter)
  const selected = elements.find((e) => e.id === params.get('el')) ?? null
  const select = (id: string | null) => setParams(id ? { el: id } : {}, { replace: true })

  const notFound = version.error instanceof ApiError && version.error.status === 404
  const info = version.data?.version
  const others = (versions.data ?? []).filter((v) => v.number !== number && v.number !== number - 1)

  const title = (
    <>
      <Link to={`${base}/versoes`} className="vs-crumb">Versões</Link>
      <span className="hint">/</span>
      <span className="mono">v{number}</span>
      {info?.label && <span className="vs-title-label">{info.label}</span>}
    </>
  )

  return (
    <>
      <PageHeader title={title}>
        {info && (
          <span className="hint" title={new Date(info.createdAt).toLocaleString('pt-BR')}>
            por {info.createdBy.name ?? 'usuário removido'} em {new Date(info.createdAt).toLocaleString('pt-BR', {
              dateStyle: 'short',
              timeStyle: 'short',
            })}
          </span>
        )}
        <div style={{ flex: 1 }} />
        <span className="pill" title="Versões não mudam depois de gravadas (RNF2).">Só leitura</span>
      </PageHeader>

      <div className="vs-body">
        <section className="vs-content">
          {version.isPending && <p className="hint">Carregando a versão…</p>}
          {notFound && (
            <div className="card vs-empty">
              <strong>Versão v{number} não encontrada.</strong>
              <Link className="btn" to={`${base}/versoes`}>Ver as versões</Link>
            </div>
          )}
          {version.isError && !notFound && <FieldError>{version.error.message}</FieldError>}

          {model && (
            <>
              <div className="vs-toolbar">
                <div className="seg" role="group" aria-label="Conteúdo da versão">
                  <button type="button" aria-pressed={tab === 'elements'} onClick={() => setTab('elements')}>
                    Elementos
                  </button>
                  <button type="button" aria-pressed={tab === 'compare'} onClick={() => setTab('compare')}>
                    Comparação
                  </button>
                </div>
                {tab === 'elements' && <KindTabs value={filter} onChange={setFilter} elements={elements} />}
                {tab === 'compare' && (
                  <label className="vs-against">
                    <span className="hint">Comparar com</span>
                    <select
                      className="inp sm"
                      value={String(against)}
                      onChange={(e) => {
                        const v = e.target.value
                        setAgainst(v === 'previous' || v === 'current' ? v : Number(v))
                      }}
                    >
                      <option value="previous">
                        {number > 1 ? `Versão anterior (v${number - 1})` : 'Antes da v1 (tudo o que ela trouxe)'}
                      </option>
                      <option value="current">Modelo atual</option>
                      {others.map((v) => (
                        <option key={v.number} value={v.number}>
                          v{v.number}{v.label ? ` · ${v.label}` : ''}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
              </div>

              {tab === 'elements' && (
                <ElementTable
                  elements={rows}
                  selectedId={selected?.id ?? null}
                  onSelect={(id) => select(id)}
                  showTests={false}
                  label={`Elementos da v${number}`}
                >
                  {rows.length === 0 && (
                    <p className="hint md-empty">
                      {elements.length === 0 ? 'Esta versão não tem elementos.' : 'Nenhum elemento deste tipo.'}
                    </p>
                  )}
                </ElementTable>
              )}

              {tab === 'compare' && (
                <div className="card vs-compare">
                  {comparison.isPending && <p className="hint">Comparando…</p>}
                  {comparison.isError && <FieldError>{comparison.error.message}</FieldError>}
                  {comparison.data && (
                    <>
                      <div className="vs-compare-head">
                        <strong>
                          {comparison.data.baseVersion ? `Da v${comparison.data.baseVersion}` : 'Do início'} para{' '}
                          {comparison.data.targetVersion ? `a v${comparison.data.targetVersion}` : 'o modelo atual'}
                        </strong>
                        <ChangeCounts changes={comparison.data.summary} />
                      </div>
                      <ModelChangesView
                        elements={comparison.data.elements}
                        relationships={comparison.data.relationships}
                      />
                    </>
                  )}
                </div>
              )}
            </>
          )}
        </section>

        {model && (
          <ElementPanel
            projectId={project.id}
            element={selected}
            elements={model.elements}
            relationships={model.relationships}
            canEdit={false}
            onSelect={select}
            emptyHint={`Selecione um elemento para ver como ele estava na v${number}.`}
            fromVersion
          />
        )}
      </div>
    </>
  )
}
