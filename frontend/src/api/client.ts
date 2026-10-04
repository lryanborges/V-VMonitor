import type { ErrorResponse } from './types'

const TOKEN_KEY = 'vv-token'

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

/** Erro devolvido pela API, com as mensagens por campo ja indexadas pelo nome do campo. */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

/** Chamado quando uma requisicao autenticada recebe 401 (token expirado ou usuario removido). */
let onUnauthorized: () => void = () => {}

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = tokenStorage.get()
  const headers = new Headers(init.headers)
  if (init.body !== undefined) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  let response: Response
  try {
    response = await fetch(path, { ...init, headers })
  } catch {
    throw new ApiError(0, 'Não foi possível conectar ao servidor. Verifique se o backend está rodando.')
  }

  if (response.ok) {
    return (response.status === 204 ? undefined : await response.json()) as T
  }

  const body = (await response.json().catch(() => null)) as ErrorResponse | null
  if (response.status === 401 && token) onUnauthorized()

  const fieldErrors = Object.fromEntries((body?.fieldErrors ?? []).map((e) => [e.field, e.message]))
  throw new ApiError(response.status, body?.message ?? 'Erro inesperado. Tente novamente.', fieldErrors)
}
