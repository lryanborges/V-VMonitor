// Tipos espelhando os DTOs do backend (com.vvmonitor.api.dto)

export interface User {
  id: string
  name: string
  email: string
  createdAt: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: string
  user: User
}

export interface RegisterUserRequest {
  name: string
  email: string
  password: string
  passwordConfirmation: string
}

/** Formato padrao de erro do backend (ErrorResponse.java). */
export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors: { field: string; message: string }[]
}
