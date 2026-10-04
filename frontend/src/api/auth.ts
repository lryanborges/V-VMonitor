import { api } from './client'
import type { LoginRequest, LoginResponse, RegisterUserRequest, User } from './types'

export const authApi = {
  login: (data: LoginRequest) =>
    api<LoginResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify(data) }),

  register: (data: RegisterUserRequest) =>
    api<User>('/api/auth/register', { method: 'POST', body: JSON.stringify(data) }),

  me: () => api<User>('/api/users/me'),
}
