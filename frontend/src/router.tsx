import { createBrowserRouter, Navigate } from 'react-router'
import { RedirectIfAuthenticated, RequireAuth } from './auth/guards'
import { AppLayout } from './layouts/AppLayout'
import { ProjectLayout } from './layouts/ProjectLayout'
import { LoginPage } from './pages/LoginPage'
import { GraphPage } from './pages/model/graph/GraphPage'
import { MatrixPage } from './pages/model/MatrixPage'
import { ModelPage } from './pages/model/ModelPage'
import { ProjectsPage } from './pages/projects/ProjectsPage'
import { RegisterPage } from './pages/RegisterPage'
import { VersionPage } from './pages/versions/VersionPage'
import { VersionsPage } from './pages/versions/VersionsPage'

export const router = createBrowserRouter([
  {
    element: <RedirectIfAuthenticated />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/cadastro', element: <RegisterPage /> },
    ],
  },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: '/projetos', element: <ProjectsPage /> },
          // o modal abre sobre a lista; ter rota propria permite voltar com o botao do navegador
          { path: '/projetos/novo', element: <ProjectsPage newProject /> },
        ],
      },
      {
        // telas dentro de um projeto: menu lateral do projeto
        path: '/projetos/:projectId',
        element: <ProjectLayout />,
        children: [
          { index: true, element: <ModelPage /> },
          { path: 'elementos/novo', element: <ModelPage newElement /> },
          // RF11, RF12: grafo e matriz de rastreabilidade
          { path: 'grafo', element: <GraphPage /> },
          { path: 'matriz', element: <MatrixPage /> },
          // RF18: versoes gravadas a cada submissao (RF10 + RF17)
          { path: 'versoes', element: <VersionsPage /> },
          { path: 'versoes/:number', element: <VersionPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/projetos" replace /> },
])
