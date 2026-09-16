const BASE = (import.meta.env.VITE_API_URL ?? '/api').replace(/\/+$/, '')
const TOKEN_KEY = 'exito_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export class ApiError extends Error {
  status: number
  detalles: string[]

  constructor(status: number, message: string, detalles: string[] = []) {
    super(message)
    this.status = status
    this.detalles = detalles
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getToken()
  const res = await fetch(`${BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options?.headers ?? {}),
    },
    ...options,
  })

  if (!res.ok) {
    if (res.status === 401 && token && !path.startsWith('/auth/login')) {
      setToken(null)
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    let message = `Error ${res.status}`
    let detalles: string[] = []
    try {
      const body = await res.json()
      if (body?.message) message = body.message
      if (Array.isArray(body?.detalles)) detalles = body.detalles
    } catch {
      /* no-op */
    }
    throw new ApiError(res.status, message, detalles)
  }

  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

const get = <T>(path: string) => request<T>(path)

const post = <T>(path: string, body?: unknown) =>
  request<T>(path, {
    method: 'POST',
    body: body ? JSON.stringify(body) : undefined,
  })

const put = <T>(path: string, body?: unknown) =>
  request<T>(path, {
    method: 'PUT',
    body: body ? JSON.stringify(body) : undefined,
  })

const patch = <T>(path: string) => request<T>(path, { method: 'PATCH' })

const del = <T>(path: string) => request<T>(path, { method: 'DELETE' })

export const api = { get, post, put, patch, del }