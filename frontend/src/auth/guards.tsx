import { Navigate, Outlet, useLocation } from 'react-router'
import type { LoginLocationState } from '../pages/LoginPage'
import { useAuth } from './AuthContext'

function Loading() {
  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', color: 'var(--mt)' }}>
      Carregando…
    </div>
  )
}

/** RNF1: rotas internas exigem login; guarda a pagina pedida para voltar a ela depois. */
export function RequireAuth() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <Loading />
  if (status === 'anonymous') {
    const state: LoginLocationState = { from: location.pathname }
    return <Navigate to="/login" replace state={state} />
  }
  return <Outlet />
}

/** Login e cadastro nao fazem sentido para quem ja esta autenticado. */
export function RedirectIfAuthenticated() {
  const { status } = useAuth()

  if (status === 'loading') return <Loading />
  if (status === 'authenticated') return <Navigate to="/projetos" replace />
  return <Outlet />
}
