import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useRef } from 'react'
import { graphApi, graphKeys, type GraphPosition } from '../../../api/graph'

/** Intervalo para juntar varios nos arrastados em sequencia num unico envio. */
const SAVE_DELAY_MS = 500

type Point = { x: number; y: number }

/**
 * RF21: posicoes salvas dos nos do grafo (ids de elementos e testes) e o salvamento automatico ao soltar um no.
 * So quem pode editar salva; para quem so visualiza, arrastar vale apenas na tela.
 */
export function useGraphPositions(projectId: string, canSave: boolean) {
  const queryClient = useQueryClient()
  const query = useQuery({ queryKey: graphKeys.positions(projectId), queryFn: () => graphApi.positions(projectId) })
  const saved = useMemo(() => new Map((query.data ?? []).map((p) => [p.nodeId, { x: p.x, y: p.y }])), [query.data])

  const pending = useRef(new Map<string, Point>())
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const save = useMutation({
    mutationFn: (positions: GraphPosition[]) => graphApi.save(projectId, positions),
    // o cache passa a ter as novas posicoes, sem buscar tudo de novo
    onSuccess: (_, positions) =>
      queryClient.setQueryData<GraphPosition[]>(graphKeys.positions(projectId), (old = []) => {
        const byId = new Map(old.map((p) => [p.nodeId, p]))
        positions.forEach((p) => byId.set(p.nodeId, p))
        return [...byId.values()]
      }),
  })
  const { mutate } = save

  const flush = useCallback(() => {
    if (timer.current) clearTimeout(timer.current)
    timer.current = null
    if (pending.current.size === 0) return
    const positions = [...pending.current].map(([nodeId, p]) => ({ nodeId, x: p.x, y: p.y }))
    pending.current.clear()
    mutate(positions)
  }, [mutate])

  /** Agenda o salvamento da posicao de um no (id do elemento ou do teste) que acabou de ser solto. */
  const queue = useCallback(
    (nodeId: string, position: Point) => {
      if (!canSave) return
      pending.current.set(nodeId, position)
      if (timer.current) clearTimeout(timer.current)
      timer.current = setTimeout(flush, SAVE_DELAY_MS)
    },
    [canSave, flush],
  )

  // ao sair do grafo com um salvamento agendado, envia na hora
  useEffect(() => flush, [flush])

  return { saved, ready: query.isSuccess, queue, saving: save.isPending, error: save.error }
}
