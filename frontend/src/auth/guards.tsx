import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '@/auth/AuthContext'

export function RequiereAuth({ children }: { children: ReactNode }) {
  const { usuario, cargando } = useAuth()
  const location = useLocation()

  if (cargando) return null
  if (!usuario) return <Navigate to="/login" state={{ from: location }} replace />
  return <>{children}</>
}

export function RequiereRol({ roles, children }: { roles: string[]; children: ReactNode }) {
  const { usuario } = useAuth()
  if (!usuario || !roles.includes(usuario.rol)) return <Navigate to="/" replace />
  return <>{children}</>
}