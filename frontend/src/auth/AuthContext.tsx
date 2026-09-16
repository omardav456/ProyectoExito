import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, getToken, setToken } from '@/api/client'
import type { LoginResponse, Usuario } from '@/types'

interface AuthState {
  usuario: Usuario | null
  cargando: boolean
  login: (email: string, password: string) => Promise<Usuario>
  logout: () => void
}

const AuthContext = createContext<AuthState | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [cargando, setCargando] = useState(Boolean(getToken()))

  useEffect(() => {
    if (!getToken()) {
      setCargando(false)
      return
    }
    api
      .get<Usuario>('/auth/me')
      .then(setUsuario)
      .catch(() => {
        setToken(null)
        setUsuario(null)
      })
      .finally(() => setCargando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const login = async (email: string, password: string) => {
    const res = await api.post<LoginResponse>('/auth/login', { email, password })
    setToken(res.token)
    setUsuario(res.usuario)
    return res.usuario
  }

  const logout = () => {
    setToken(null)
    setUsuario(null)
    if (window.location.pathname !== '/login') {
      window.location.assign('/login')
    }
  }

  return <AuthContext.Provider value={{ usuario, cargando, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}