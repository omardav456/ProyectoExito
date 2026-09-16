import { NavLink, Route, Routes, Navigate, useLocation } from 'react-router-dom'
import { useState } from 'react'
import { LoadingSpinner } from '@/components/ui/Spinner'
import Dashboard from '@/pages/Dashboard'
import Inventario from '@/pages/Inventario'
import Tienda from '@/pages/Tienda'
import Prediccion from '@/pages/Prediccion'
import StockFlow from '@/pages/StockFlow'
import Simulador from '@/pages/Simulador'
import Recomendaciones from '@/pages/Recomendaciones'
import Alertas from '@/pages/Alertas'
import Reportes from '@/pages/Reportes'
import ModelosCatalogo from '@/pages/ModelosCatalogo'
import ModeloDetalle from '@/pages/ModeloDetalle'
import EjecutarModelo from '@/pages/EjecutarModelo'
import Auditoria from '@/pages/Auditoria'
import Usuarios from '@/pages/Usuarios'
import Login from '@/pages/Login'
import { Suspense } from 'react'
import { AuthProvider, useAuth } from '@/auth/AuthContext'
import { RequiereAuth, RequiereRol } from '@/auth/guards'

const ROL_ADMIN = 'ADMINISTRADOR'
const ROL_EMPLEADO = 'EMPLEADO'
const ROLES_GESTION = [ROL_ADMIN, ROL_EMPLEADO]

const navBase = [
  { to: '/dashboard',   icon: '▦', label: 'Dashboard' },
  { to: '/inventario',  icon: '≡', label: 'Inventario' },
  { to: '/tienda',      icon: '◫', label: 'Tienda' },
  { to: '/prediccion',  icon: '◈', label: 'Predicción' },
  { to: '/stockflow',   icon: '⇄', label: 'Stock & Flow' },
  { to: '/simulador',   icon: '◉', label: 'Simulador' },
  { to: '/recomendaciones', icon: '✦', label: 'Recomendaciones' },
  { to: '/alertas',     icon: '⚠', label: 'Alertas' },
  { to: '/reportes',    icon: '▤', label: 'Reportes' },
]

const navClientes = [
  { to: '/tienda',      icon: '◫', label: 'Tienda' },
]

const navAdmin = [
  { to: '/modelos',     icon: '∑', label: 'Modelos' },
  { to: '/ejecutar-modelo', icon: '⚙', label: 'Ejecutar modelo' },
  { to: '/auditoria',   icon: '✎', label: 'Auditoría' },
  { to: '/usuarios',    icon: '⌘', label: 'Usuarios' },
]

const ROL_ETIQUETA: Record<string, string> = {
  ADMINISTRADOR: 'Administrador',
  EMPLEADO: 'Empleado',
  CLIENTE: 'Cliente',
  PROVEEDOR: 'Proveedor',
}

export default function App() {
  return (
    <AuthProvider>
      <Shell />
    </AuthProvider>
  )
}

function Cargando() {
  return (
    <div className="flex h-full items-center justify-center">
      <LoadingSpinner />
    </div>
  )
}

function Shell() {
  const { usuario, cargando, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()

  if (cargando) return <Cargando />

  if (!usuario) {
    return (
      <Routes>
        <Route path="/tienda" element={<Tienda />} />
        <Route path="/login" element={<Login />} />
        <Route path="*" element={<Navigate to="/tienda" replace />} />
      </Routes>
    )
  }

  const esAdmin = usuario.rol === ROL_ADMIN
  const esGestion = ROLES_GESTION.includes(usuario.rol)
  const navItems = esAdmin
    ? [...navBase, ...navAdmin]
    : esGestion
      ? navBase
      : navClientes
  const tituloPagina =
    navItems.find((n) => n.to !== '/' && location.pathname.startsWith(n.to))?.label ??
    (location.pathname.startsWith('/modelos/') ? 'Modelos' : 'Dashboard')

  return (
    <div className="flex h-full bg-bg text-text overflow-hidden">
      <aside className={`${menuOpen ? 'translate-x-0' : '-translate-x-full'} lg:translate-x-0 fixed lg:static inset-y-0 left-0 z-40 w-60 bg-[#080b12] border-r border-border flex flex-col transition-transform duration-300`}>
        <div className="px-5 py-5 border-b border-border">
          <div className="flex items-center gap-2 mb-0.5">
            <div className="w-7 h-7 bg-primary rounded-md flex items-center justify-center">
              <span className="text-bg text-xs font-black font-mono">E</span>
            </div>
            <span className="font-bold tracking-tight text-sm">ÉXITO STOCK AI</span>
          </div>
          <p className="text-[10px] font-mono text-muted2 ml-9">Fusagasugá · v1.0</p>
        </div>

        <nav className="flex-1 py-4 px-3 space-y-1 overflow-y-auto">
          {navItems.map(({ to, icon, label }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) =>
                `w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all ${
                  isActive ? 'bg-primary text-bg font-semibold' : 'text-muted hover:text-text hover:bg-surface2'
                }`
              }
            >
              <span className="font-mono text-base w-5 text-center">{icon}</span>
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="px-5 py-4 border-t border-border space-y-2">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-full bg-primary/20 border border-primary/40 flex items-center justify-center text-primary text-xs font-bold shrink-0">
              {usuario.nombre.charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-text truncate">{usuario.nombre}</p>
              <p className="text-[10px] font-mono text-muted truncate">{ROL_ETIQUETA[usuario.rol] ?? usuario.rol}</p>
            </div>
          </div>
          <p className="text-[10px] font-mono text-muted2 leading-relaxed">
            Sistema de gestión predictiva de inventario para demostración académica
          </p>
        </div>
      </aside>

      {menuOpen && <div className="fixed inset-0 z-30 bg-black/60 lg:hidden" onClick={() => setMenuOpen(false)} />}

      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <header className="flex items-center justify-between px-5 py-3 border-b border-border bg-bg flex-shrink-0">
          <div className="flex items-center gap-3">
            <button onClick={() => setMenuOpen((o) => !o)} className="lg:hidden text-muted hover:text-text text-xl">☰</button>
            <span className="text-xs font-mono text-muted uppercase tracking-widest">{tituloPagina}</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-xs font-mono text-muted2 hidden sm:block">
              {new Date().toLocaleDateString('es-CO', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
            </span>
            <span className="hidden sm:block text-xs font-mono text-muted">{ROL_ETIQUETA[usuario.rol] ?? usuario.rol}</span>
            <button
              onClick={logout}
              className="text-xs font-mono px-3 py-1.5 rounded-lg border border-border text-muted hover:text-[#f87171] hover:border-[#f87171] transition-colors cursor-pointer"
            >
              Salir
            </button>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-5 lg:p-7">
          <Suspense fallback={<LoadingSpinner />}>
            <Routes>
              <Route index element={<Navigate to="/tienda" replace />} />
              <Route path="dashboard" element={<RequiereAuth>{esGestion ? <Dashboard /> : <Navigate to="/tienda" replace />}</RequiereAuth>} />
              <Route path="inventario" element={<RequiereAuth><RequiereRol roles={ROLES_GESTION}><Inventario /></RequiereRol></RequiereAuth>} />
              <Route path="tienda" element={<RequiereAuth><Tienda /></RequiereAuth>} />
              <Route path="prediccion" element={<RequiereAuth><RequiereRol roles={ROLES_GESTION}><Prediccion /></RequiereRol></RequiereAuth>} />
              <Route path="stockflow" element={<RequiereAuth><RequiereRol roles={ROLES_GESTION}><StockFlow /></RequiereRol></RequiereAuth>} />
              <Route path="simulador" element={<RequiereAuth><RequiereRol roles={ROLES_GESTION}><Simulador /></RequiereRol></RequiereAuth>} />
              <Route path="recomendaciones" element={<RequiereAuth><RequiereRol roles={ROLES_GESTION}><Recomendaciones /></RequiereRol></RequiereAuth>} />
              <Route path="alertas" element={<RequiereAuth><RequiereRol roles={ROLES_GESTION}><Alertas /></RequiereRol></RequiereAuth>} />
              <Route path="reportes" element={<RequiereAuth><RequiereRol roles={ROLES_GESTION}><Reportes /></RequiereRol></RequiereAuth>} />
              <Route path="modelos" element={<RequiereAuth><RequiereRol roles={[ROL_ADMIN]}><ModelosCatalogo /></RequiereRol></RequiereAuth>} />
              <Route path="modelos/:id" element={<RequiereAuth><RequiereRol roles={[ROL_ADMIN]}><ModeloDetalle /></RequiereRol></RequiereAuth>} />
              <Route path="ejecutar-modelo" element={<RequiereAuth><RequiereRol roles={[ROL_ADMIN]}><EjecutarModelo /></RequiereRol></RequiereAuth>} />
              <Route path="auditoria" element={<RequiereAuth><RequiereRol roles={[ROL_ADMIN]}><Auditoria /></RequiereRol></RequiereAuth>} />
              <Route path="usuarios" element={<RequiereAuth><RequiereRol roles={[ROL_ADMIN]}><Usuarios /></RequiereRol></RequiereAuth>} />
              <Route path="*" element={<div className="text-center py-20 text-muted">Página no encontrada</div>} />
            </Routes>
          </Suspense>
        </main>
      </div>
    </div>
  )
}