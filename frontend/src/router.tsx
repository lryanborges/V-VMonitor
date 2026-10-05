import { createBrowserRouter, Navigate } from 'react-router'
import { RedirectIfAuthenticated, RequireAuth } from './auth/guards'
import { AppLayout } from './layouts/AppLayout'
import { ProjectLayout } from './layouts/ProjectLayout'
import { LoginPage } from './pages/LoginPage'
import { ModelPage } from './pages/model/ModelPage'
import { ProjectsPage } from './pages/projects/ProjectsPage'
import { RegisterPage } from './pages/RegisterPage'

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
          { path: 'elementos/editar', element: <ModelPage editElement /> },
          { path: 'elementos/excluir', element: <ModelPage deleteElement /> },
          { path: 'relacionamentos/novo', element: <ModelPage addRelationship /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/projetos" replace /> },
])
