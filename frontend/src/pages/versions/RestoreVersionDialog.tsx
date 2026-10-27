import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { ApiError } from '../../api/client'
import { elementKeys } from '../../api/elements'
import { graphKeys } from '../../api/graph'
import { projectKeys } from '../../api/projects'
import { relationshipKeys } from '../../api/relationships'
import { testKeys } from '../../api/tests'
import type { ChangeGroup, ModelChanges } from '../../api/types'
import { versionKeys, versionsApi } from '../../api/versions'
import { FieldError } from '../../components/FieldError'
import { AlertIcon, HistoryIcon } from '../../components/icons'
import { Modal } from '../../components/Modal'
import { ModelChangesView } from '../model/ModelChangesView'
import { ChangeCounts } from './ChangeCounts'
// aviso e lista de mudancas no mesmo estilo da janela de submissao
import '../model/SubmitVersionDialog.css'

interface RestoreVersionDialogProps {
  projectId: string
  number: number
  onClose: () => void
}

/** Le a comparacao "versao -> modelo atual" no sentido inverso: o que a restauracao faz no modelo atual. */
function reverse<T>(group: ChangeGroup<T>): ChangeGroup<T> {
  return {
    added: group.removed,
    removed: group.added,
    modified: group.modified.map(({ before, after }) => ({ before: after, after: before })),
  }
}

/**
 * RF22 (proposto para a v2 do documento): confirma a volta do modelo atual a uma versao. Mostra o que vai
 * mudar e avisa que as alteracoes nao salvas em versao serao perdidas. Nao cria versao nova.
 */
export function RestoreVersionDialog({ projectId, number, onClose }: RestoreVersionDialogProps) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  // o que mudou da versao ate o modelo atual; restaurar desfaz exatamente isso
  const comparison = useQuery({
    queryKey: versionKeys.compare(projectId, number, 'current'),
    queryFn: () => versionsApi.compare(projectId, number, 'current'),
    gcTime: 0,
  })
  // alteracoes ainda nao submetidas (desde a ultima versao): sao as que se perdem
  const pending = useQuery({ queryKey: [...versionKeys.changes(projectId), 'restore'], queryFn: () => versionsApi.changes(projectId), gcTime: 0 })

  const restore = useMutation({
    mutationFn: () => versionsApi.restore(projectId, number),
    onSuccess: () => {
      for (const key of [elementKeys.all(projectId), relationshipKeys.all(projectId), testKeys.all(projectId),
        graphKeys.positions(projectId), versionKeys.changes(projectId)]) {
        queryClient.invalidateQueries({ queryKey: key })
      }
      queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId), exact: true })
      queryClient.invalidateQueries({ queryKey: projectKeys.all, exact: true })
      onClose()
      navigate(`/projetos/${projectId}`)
    },
  })

  const data: ModelChanges | undefined = comparison.data
  const effect = data && {
    elements: reverse(data.elements),
    relationships: reverse(data.relationships),
    tests: reverse(data.tests),
    summary: { added: data.summary.removed, modified: data.summary.modified, removed: data.summary.added },
  }
  const nothing = !!effect && effect.summary.added + effect.summary.modified + effect.summary.removed === 0
  const unsaved = pending.data ? pending.data.summary.added + pending.data.summary.modified + pending.data.summary.removed : 0

  return (
    <Modal
      title={`Restaurar a v${number}?`}
      icon={<HistoryIcon />}
      size="lg"
      subtitle="O modelo atual volta a ficar igual a esta versão. As versões salvas não mudam."
      onClose={onClose}
      footer={
        <>
          <button className="btn" type="button" onClick={onClose}>Cancelar</button>
          <button
            className="btn p"
            type="button"
            disabled={!effect || nothing || restore.isPending}
            onClick={() => restore.mutate()}
          >
            <HistoryIcon />
            {restore.isPending ? 'Restaurando…' : `Restaurar a v${number}`}
          </button>
        </>
      }
    >
      {unsaved > 0 && (
        <div className="sm-warning" role="alert">
          <AlertIcon />
          <span>
            {unsaved === 1 ? 'Há 1 alteração que ainda não foi salva' : `Há ${unsaved} alterações que ainda não foram salvas`}{' '}
            em uma versão. {unsaved === 1 ? 'Ela será perdida' : 'Elas serão perdidas'} ao restaurar.
          </span>
        </div>
      )}

      {comparison.isPending && <p className="hint">Calculando o que vai mudar…</p>}
      {comparison.isError && <FieldError>{comparison.error.message}</FieldError>}
      {nothing && <p className="hint">O modelo atual já está igual à v{number}. Não há nada a restaurar.</p>}
      {effect && !nothing && (
        <>
          <div className="vs-compare-head">
            <strong>O que muda no modelo atual</strong>
            <ChangeCounts changes={effect.summary} />
          </div>
          <div className="sm-changes">
            <ModelChangesView elements={effect.elements} relationships={effect.relationships} tests={effect.tests} />
          </div>
          <p className="hint" style={{ margin: 0 }}>
            Itens excluídos depois da v{number} voltam com o mesmo código. Para registrar o resultado como uma versão
            nova, submeta o modelo depois.
          </p>
        </>
      )}

      {restore.error instanceof ApiError && <FieldError>{restore.error.message}</FieldError>}
    </Modal>
  )
}
