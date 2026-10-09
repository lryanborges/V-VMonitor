import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { authApi } from '../api/auth'
import { ApiError } from '../api/client'
import { FieldError } from '../components/FieldError'
import { PasswordInput } from '../components/PasswordInput'
import { CheckIcon } from '../components/icons'
import { useAuth } from '../auth/AuthContext'
import { AuthLayout, type HeroEdge, type HeroNode } from '../layouts/AuthLayout'

/** Estado de navegacao recebido de outras telas (ex.: cadastro concluido). */
export interface LoginLocationState {
  email?: string
  registered?: boolean
  from?: string
}

const nodes: HeroNode[] = [
  { label: 'RN1', kind: 'rn', x: 60, y: 50 },
  { label: 'RF1', kind: 'rf', x: 210, y: 130 },
  { label: 'RF2', kind: 'rf', x: 370, y: 70 },
  { label: 'RNF3', kind: 'rnf', x: 200, y: 240 },
  { label: 'T1', kind: 't', x: 380, y: 220 },
  { label: 'RF5', kind: 'rf', x: 520, y: 160 },
]

const edges: HeroEdge[] = [
  { from: [60, 50], to: [210, 130] },
  { from: [210, 130], to: [370, 70] },
  { from: [210, 130], to: [200, 240], style: 'ref' },
  { from: [210, 130], to: [380, 220], style: 'ts' },
  { from: [370, 70], to: [520, 160] },
  { from: [520, 160], to: [380, 220], style: 'ts' },
]

/** RF2, UC-02: autenticar-se no sistema. */
export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = (location.state ?? {}) as LoginLocationState
  const { signIn } = useAuth()

  const [email, setEmail] = useState(state.email ?? '')
  const [password, setPassword] = useState('')

  const login = useMutation({
    mutationFn: authApi.login,
    onSuccess: (response) => {
      signIn(response)
      // UC-02, passo 5: apresenta a tela inicial dos projetos (ou a pagina que o ator tentou abrir)
      navigate(state.from ?? '/projetos', { replace: true })
    },
  })

  const error = login.error instanceof ApiError ? login.error : null
  const fieldErrors = error?.fieldErrors ?? {}
  // 401: credenciais erradas; os dois campos ficam marcados, sem indicar qual esta errado
  const invalidCredentials = error?.status === 401

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    login.mutate({ email, password })
  }

  return (
    <AuthLayout
      eyebrow="Validação e verificação de requisitos"
      title="Cada requisito ligado à sua regra e ao seu teste."
      subtitle="Cadastre requisitos e regras de negócio, relacione os elementos e acompanhe a rastreabilidade em grafo ou em matriz."
      nodes={nodes}
      edges={edges}
    >
      <form className="auth-form" style={{ maxWidth: 380, gap: 20 }} onSubmit={handleSubmit} noValidate>
        <div className="auth-form-head">
          <h2>Entrar</h2>
          <p>Acesse seus projetos de requisitos.</p>
        </div>

        {state.registered && !error && (
          <div className="notice" role="status">
            <CheckIcon />
            <span>Conta criada com sucesso. Entre com seu e-mail e senha.</span>
          </div>
        )}

        <div>
          <label className="lbl" htmlFor="lg-email">E-mail</label>
          <input
            id="lg-email"
            className={`inp${fieldErrors.email || invalidCredentials ? ' err' : ''}`}
            type="email"
            autoComplete="email"
            placeholder="voce@empresa.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            aria-invalid={!!fieldErrors.email || invalidCredentials}
            autoFocus={!state.email}
          />
          {fieldErrors.email && <FieldError>{fieldErrors.email}</FieldError>}
        </div>

        <div>
          <label className="lbl" htmlFor="lg-senha">Senha</label>
          <PasswordInput
            id="lg-senha"
            className={`inp${fieldErrors.password || invalidCredentials ? ' err' : ''}`}
            autoComplete="current-password"
            placeholder="Sua senha"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            aria-invalid={!!fieldErrors.password || invalidCredentials}
            autoFocus={!!state.email}
          />
          {fieldErrors.password && <FieldError>{fieldErrors.password}</FieldError>}
        </div>

        {/* credenciais erradas, servidor fora do ar ou erro inesperado */}
        {error && Object.keys(fieldErrors).length === 0 && <FieldError>{error.message}</FieldError>}

        <button className="btn p lg" type="submit" style={{ width: '100%' }} disabled={login.isPending}>
          {login.isPending ? 'Entrando…' : 'Entrar'}
        </button>

        <p className="auth-footnote">
          Ainda não tem conta? <Link className="lnk" to="/cadastro">Criar conta</Link>
        </p>
      </form>
    </AuthLayout>
  )
}
