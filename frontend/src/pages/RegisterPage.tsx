import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { authApi } from '../api/auth'
import { ApiError } from '../api/client'
import type { RegisterUserRequest } from '../api/types'
import { FieldError } from '../components/FieldError'
import { PasswordInput } from '../components/PasswordInput'
import { AuthLayout, type HeroEdge, type HeroNode } from '../layouts/AuthLayout'
import type { LoginLocationState } from './LoginPage'

type Field = keyof RegisterUserRequest

const nodes: HeroNode[] = [
  { label: 'RF4', kind: 'rf', x: 90, y: 150 },
  { label: 'RF5', kind: 'rf', x: 240, y: 80 },
  { label: 'RF6', kind: 'rf', x: 250, y: 210 },
  { label: 'RNF8', kind: 'rnf', x: 410, y: 60 },
  { label: 'RF7', kind: 'rf', x: 410, y: 170 },
  { label: 'T7', kind: 't', x: 540, y: 240 },
]

const edges: HeroEdge[] = [
  { from: [90, 150], to: [240, 80] },
  { from: [90, 150], to: [250, 210] },
  { from: [240, 80], to: [410, 60], style: 'ref' },
  { from: [250, 210], to: [410, 170] },
  { from: [410, 170], to: [540, 240], style: 'ts' },
]

const emptyForm: RegisterUserRequest = { name: '', email: '', password: '', passwordConfirmation: '' }

/** Converte a resposta de erro do backend em mensagens por campo do formulario. */
function toFieldErrors(error: ApiError): Partial<Record<Field, string>> {
  const errors: Partial<Record<Field, string>> = { ...error.fieldErrors }
  // o backend valida a confirmacao no metodo isPasswordConfirmed()
  if (error.fieldErrors.passwordConfirmed) errors.passwordConfirmation = error.fieldErrors.passwordConfirmed
  // 409: e-mail ja cadastrado (UC-01, excecao do passo 4)
  if (error.status === 409) errors.email = error.message
  return errors
}

/** RF1, UC-01: cadastrar-se no sistema. */
export function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState(emptyForm)
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({})
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [emailTaken, setEmailTaken] = useState(false)

  const register = useMutation({
    mutationFn: authApi.register,
    onSuccess: (user) => {
      // UC-01, passo 6: redireciona para o login com confirmacao
      const state: LoginLocationState = { email: user.email, registered: true }
      navigate('/login', { state })
    },
    onError: (error) => {
      if (!(error instanceof ApiError)) return
      const fieldErrors = toFieldErrors(error)
      setErrors(fieldErrors)
      setEmailTaken(error.status === 409)
      setGeneralError(Object.keys(fieldErrors).length === 0 ? error.message : null)
    },
  })

  function update(field: Field, value: string) {
    setForm((f) => ({ ...f, [field]: value }))
    // o erro do campo some assim que o ator volta a editar
    setErrors((e) => ({ ...e, [field]: undefined }))
    if (field === 'email') setEmailTaken(false)
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setGeneralError(null)
    register.mutate(form)
  }

  const inputClass = (field: Field) => `inp${errors[field] ? ' err' : ''}`

  return (
    <AuthLayout
      eyebrow="Comece em poucos minutos"
      title="Crie a conta, abra um projeto e cadastre o primeiro requisito."
      subtitle="Projetos podem ser compartilhados com a equipe por e-mail depois."
      nodes={nodes}
      edges={edges}
    >
      <form className="auth-form" style={{ maxWidth: 420, gap: 18 }} onSubmit={handleSubmit} noValidate>
        <div className="auth-form-head">
          <h2>Criar conta</h2>
          <p>Informe seus dados para acessar o V&amp;V Monitor.</p>
        </div>

        <div>
          <label className="lbl" htmlFor="cd-nome">Nome</label>
          <input
            id="cd-nome"
            className={inputClass('name')}
            type="text"
            autoComplete="name"
            value={form.name}
            onChange={(e) => update('name', e.target.value)}
            aria-invalid={!!errors.name}
            autoFocus
          />
          {errors.name && <FieldError>{errors.name}</FieldError>}
        </div>

        <div>
          <label className="lbl" htmlFor="cd-email">E-mail</label>
          <input
            id="cd-email"
            className={inputClass('email')}
            type="email"
            autoComplete="email"
            placeholder="voce@empresa.com"
            value={form.email}
            onChange={(e) => update('email', e.target.value)}
            aria-invalid={!!errors.email}
          />
          {errors.email && (
            <FieldError>
              {emailTaken ? 'Este e-mail já está cadastrado.' : errors.email}{' '}
              {emailTaken && (
                <Link className="lnk" to="/login" state={{ email: form.email } satisfies LoginLocationState}>
                  Entrar com ele
                </Link>
              )}
            </FieldError>
          )}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 12 }}>
          <div>
            <label className="lbl" htmlFor="cd-senha">Senha</label>
            <PasswordInput
              id="cd-senha"
              className={inputClass('password')}
              autoComplete="new-password"
              placeholder="Mínimo de 8 caracteres"
              value={form.password}
              onChange={(e) => update('password', e.target.value)}
              aria-invalid={!!errors.password}
            />
            {errors.password && <FieldError>{errors.password}</FieldError>}
          </div>
          <div>
            <label className="lbl" htmlFor="cd-conf">Confirmar senha</label>
            <PasswordInput
              id="cd-conf"
              className={inputClass('passwordConfirmation')}
              autoComplete="new-password"
              value={form.passwordConfirmation}
              onChange={(e) => update('passwordConfirmation', e.target.value)}
              aria-invalid={!!errors.passwordConfirmation}
            />
            {errors.passwordConfirmation && <FieldError>{errors.passwordConfirmation}</FieldError>}
          </div>
        </div>

        {generalError && <FieldError>{generalError}</FieldError>}

        <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
          {/* UC-01, sequencia alternativa: cancelar volta para o login */}
          <Link className="btn lg" to="/login">Cancelar</Link>
          <button className="btn p lg" type="submit" style={{ flex: 1 }} disabled={register.isPending}>
            {register.isPending ? 'Criando conta…' : 'Criar conta'}
          </button>
        </div>

        <p className="auth-footnote">
          Já tem conta? <Link className="lnk" to="/login">Entrar</Link>
        </p>
      </form>
    </AuthLayout>
  )
}
