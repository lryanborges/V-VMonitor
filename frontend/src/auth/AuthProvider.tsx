import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api/auth'
import { setUnauthorizedHandler, tokenStorage } from '../api/client'
import type { LoginResponse, User } from '../api/types'
import { AuthContext, type AuthStatus } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<User | null>(null)
  const [status, setStatus] = useState<AuthStatus>(() => (tokenStorage.get() ? 'loading' : 'anonymous'))

  const signOut = useCallback(() => {
    tokenStorage.clear()
    queryClient.clear()
    setUser(null)
    setStatus('anonymous')
  }, [queryClient])

  const signIn = useCallback((response: LoginResponse) => {
    tokenStorage.set(response.accessToken)
    setUser(response.user)
    setStatus('authenticated')
  }, [])

  // token expirado ou usuario removido: qualquer 401 em requisicao autenticada encerra a sessao
  useEffect(() => setUnauthorizedHandler(signOut), [signOut])

  // ao abrir o app com token salvo, confirma com o backend que ele ainda vale
  useEffect(() => {
    if (!tokenStorage.get()) return
    authApi
      .me()
      .then((me) => {
        setUser(me)
        setStatus('authenticated')
      })
      .catch(signOut)
  }, [signOut])

  const value = useMemo(() => ({ status, user, signIn, signOut }), [status, user, signIn, signOut])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
