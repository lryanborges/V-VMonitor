import { useOutletContext } from 'react-router'
import type { Project } from '../api/types'

/** Projeto carregado pelo ProjectLayout, disponivel para as paginas dentro de um projeto. */
export function useProject(): Project {
  return useOutletContext<Project>()
}
