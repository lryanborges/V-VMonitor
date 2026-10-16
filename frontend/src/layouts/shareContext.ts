import { createContext, useContext } from 'react'

/** Abre a janela de compartilhamento (RF20) a partir de qualquer tela do projeto (menu lateral, cabecalho). */
export const ShareContext = createContext<() => void>(() => {})

export function useOpenShare(): () => void {
  return useContext(ShareContext)
}
