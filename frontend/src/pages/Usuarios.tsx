import { useState, type FormEvent } from 'react'
import { api, ApiError } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { Usuario } from '@/types'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { EmptyState } from '@/components/ui/EmptyState'

const ROLES = ['ADMINISTRADOR', 'EMPLEADO', 'CLIENTE', 'PROVEEDOR']

const ROL_COLOR: Record<string, string> = {
  ADMINISTRADOR: 'bg-[#FFD10022] text-[#FFD100]',
  EMPLEADO: 'bg-[#60a5fa22] text-[#60a5fa]',
  CLIENTE: 'bg-[#34d39922] text-[#34d399]',
  PROVEEDOR: 'bg-[#c084fc22] text-[#c084fc]',
}

interface FormUsuario {
  id: number | null
  nombre: string
  email: string
  password: string
  rol: string
  activo: boolean
}

const FORM_VACIO: FormUsuario = { id: null, nombre: '', email: '', password: '', rol: 'EMPLEADO', activo: false }

export default function Usuarios() {
  const [form, setForm] = useState<FormUsuario>(FORM_VACIO)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  const { data: usuarios, loading, error: errorUsuarios, refetch } = useFetch<Usuario[]>(() => api.get('/usuarios'), [])

  const abrirEdicion = (u: Usuario) => {
    setError('')
    setForm({ id: u.id, nombre: u.nombre, email: u.email, password: '', rol: u.rol, activo: u.activo })
  }

  const guardar = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      const body = {
        nombre: form.nombre,
        email: form.email,
        password: form.password,
        rol: form.rol,
        activo: form.activo,
      }
      if (form.id === null) {
        await api.post<Usuario>('/usuarios', body)
      } else {
        await api.put<Usuario>(`/usuarios/${form.id}`, body)
      }
      setForm(FORM_VACIO)
      refetch()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo guardar el usuario')
    } finally {
      setGuardando(false)
    }
  }

  const inputClase =
    'w-full bg-[#0d1119] border border-[#1e2d45] rounded-lg px-3 py-2 text-xs font-mono text-[#f0f4f8] placeholder:text-[#3d4f6b] focus:outline-none focus:border-[#FFD10055] transition-colors'

  const selectClase = `${inputClase} cursor-pointer appearance-none [&>option]:bg-[#111827]`

  if (loading) return <LoadingSpinner />
  if (errorUsuarios || !usuarios) {
    return <ErrorState message={errorUsuarios ?? 'No se pudieron cargar los usuarios.'} onRetry={refetch} />
  }

  return (
    <div>
      <PageHeader title="Usuarios" subtitle="Cuentas con rol ADMINISTRADOR, EMPLEADO, CLIENTE o PROVEEDOR — los empleados nuevos nacen pendientes de aprobación por el administrador" />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2">
          <Panel className="p-0 overflow-hidden">
            {usuarios.length === 0 ? (
              <EmptyState message="No hay usuarios registrados" />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">ID</th>
                      <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Nombre</th>
                      <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Email</th>
                      <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Rol</th>
                      <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Estado</th>
                      <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Creado</th>
                      <th className="px-4 py-3"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60">
                    {usuarios.map((u) => (
                      <tr key={u.id} className="hover:bg-[#0d1119] transition-colors">
                        <td className="px-4 py-3 text-xs font-mono text-muted">{u.id}</td>
                        <td className="px-4 py-3 text-sm text-text">{u.nombre}</td>
                        <td className="px-4 py-3 text-xs font-mono text-muted">{u.email}</td>
                        <td className="px-4 py-3">
                          <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${ROL_COLOR[u.rol] ?? 'bg-[#6b7a9922] text-[#6b7a99]'}`}>
                            {u.rol}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-xs font-mono">
                          {u.activo ? (
                            <span className="text-[#34d399]">Activo</span>
                          ) : u.rol === 'EMPLEADO' ? (
                            <span className="text-[#fbbf24]">Pendiente de aprobación</span>
                          ) : (
                            <span className="text-[#f87171]">Inactivo</span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-xs font-mono text-muted">
                          {u.createdAt ? new Date(u.createdAt).toLocaleDateString('es-CO') : '—'}
                        </td>
                        <td className="px-4 py-3">
                          <button
                            onClick={() => abrirEdicion(u)}
                            className="text-xs font-mono text-[#FFD100] hover:underline cursor-pointer"
                          >
                            Editar
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Panel>
        </div>

        <Panel title={form.id === null ? 'Nuevo usuario' : `Editar usuario #${form.id}`} className="h-fit lg:sticky lg:top-0">
          <form onSubmit={guardar} className="space-y-4">
            <div>
              <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Nombre</label>
              <input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} className={inputClase} required />
            </div>
            <div>
              <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Email</label>
              <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className={inputClase} required />
            </div>
            <div>
              <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">
                Contraseña {form.id !== null && <span className="normal-case text-muted2">(vacía = sin cambios)</span>}
              </label>
              <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} className={inputClase} required={form.id === null} />
            </div>
            <div>
              <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Rol</label>
              <select value={form.rol} onChange={(e) => setForm({ ...form, rol: e.target.value })} className={selectClase}>
                {ROLES.map((r) => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
            </div>
            <label className="flex items-center gap-2.5 text-xs font-mono text-muted cursor-pointer">
              <input
                type="checkbox"
                checked={form.activo}
                onChange={(e) => setForm({ ...form, activo: e.target.checked })}
                className="accent-[#FFD100] w-4 h-4"
              />
              Activo
            </label>
            {form.id === null && form.rol === 'EMPLEADO' && (
              <p className="text-[10px] font-mono text-[#fbbf24] leading-relaxed">
                Los empleados nacen pendientes de aprobación — debes activar la cuenta para que puedan operar.
              </p>
            )}

            {error && <div className="bg-red-dim border border-red/40 text-red px-3 py-2.5 text-xs font-mono rounded-lg">{error}</div>}

            <div className="flex gap-2">
              <button
                type="submit"
                disabled={guardando}
                className="flex-1 py-2.5 rounded-lg bg-primary text-bg font-bold text-xs tracking-wide hover:opacity-90 transition-opacity cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {guardando ? 'Guardando…' : form.id === null ? 'Crear usuario' : 'Guardar cambios'}
              </button>
              {form.id !== null && (
                <button
                  type="button"
                  onClick={() => setForm(FORM_VACIO)}
                  className="px-3 py-2.5 rounded-lg border border-border text-muted hover:text-text transition-colors cursor-pointer text-xs font-mono"
                >
                  Cancelar
                </button>
              )}
            </div>
          </form>
        </Panel>
      </div>
    </div>
  )
}