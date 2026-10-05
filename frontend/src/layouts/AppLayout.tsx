import { Outlet } from 'react-router'
import { AppHeader } from '../components/AppHeader'
import './AppLayout.css'

/** Moldura das telas fora de um projeto: cabecalho padrao e o conteudo da pagina. */
export function AppLayout() {
  return (
    <div className="app">
      <AppHeader />
      <Outlet />
    </div>
  )
}
