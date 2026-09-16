import { useState } from 'react'
import { api } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { Auditoria, Producto, Usuario } from '@/types'
import { numero } from '@/lib/format'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { EmptyState } from '@/components/ui/EmptyState'

const OPERACIONES = ['ENTRADA', 'SALIDA', 'AJUSTE', 'COMPRA_SIMULADA'] as const

const OPERACION_COLOR: Record<string, string> = {
  ENTRADA: 'text-[#34d399]',
  SALIDA: 'text-[#fb923c]',
  AJUSTE: 'text-[#60a5fa]',
  COMPRA_SIMULADA: 'text-[#f472b6]',
}

const ROL_COLOR: Record<string, string> = {
  ADMINISTRADOR: 'bg-[#FFD10022] text-[#FFD100]',
  EMPLEADO: 'bg-[#60a5fa22] text-[#60a5fa]',
}

export default function Auditoria() {
  const [operacion, setOperacion] = useState('')
  const [productoId, setProductoId] = useState('')
  const [usuarioId, setUsuarioId] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')

  const query = () => {
    const params = new URLSearchParams()
    if (operacion) params.set('operacion', operacion)
    if (productoId) params.set('productoId', productoId)
    if (usuarioId) params.set('usuarioId', usuarioId)
    if (desde) params.set('desde', desde)
    if (hasta) params.set('hasta', hasta)
    return `/auditoria?${params.toString()}`
  }

  const { data: registros, loading, error, refetch } = useFetch<Auditoria[]>(() => api.get(query()), [
    operacion,
    productoId,
    usuarioId,
    desde,
    hasta,
  ])
  const { data: productos } = useFetch<Producto[]>(() => api.get('/productos?activo=false'), [])
  const { data: usuarios } = useFetch<Usuario[]>(() => api.get('/usuarios'), [])

  const inputClase =
    'w-full bg-[#0d1119] border border-[#1e2d45] rounded-lg px-3 py-2 text-xs font-mono text-[#f0f4f8] placeholder:text-[#3d4f6b] focus:outline-none focus:border-[#FFD10055] transition-colors [&>option]:bg-[#111827]'

  if (loading) return <LoadingSpinner />
  if (error || !registros) return <ErrorState message={error ?? 'No se pudieron cargar los registros.'} onRetry={refetch} />

  return (
    <div>
      <PageHeader title="Auditoría" subtitle="Bitácora inmutable de operaciones de inventario y compras simuladas" />

      <Panel className="mb-5 p-4">
        <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Operación</label>
            <select value={operacion} onChange={(e) => setOperacion(e.target.value)} className={`${inputClase} cursor-pointer`}>
              <option value="">Todas</option>
              {OPERACIONES.map((o) => (
                <option key={o} value={o}>{o}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Producto</label>
            <select value={productoId} onChange={(e) => setProductoId(e.target.value)} className={`${inputClase} cursor-pointer`}>
              <option value="">Todos</option>
              {(productos ?? []).map((p) => (
                <option key={p.id} value={p.id}>{p.nombre}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Usuario</label>
            <select value={usuarioId} onChange={(e) => setUsuarioId(e.target.value)} className={`${inputClase} cursor-pointer`}>
              <option value="">Todos</option>
              {(usuarios ?? []).map((u) => (
                <option key={u.id} value={u.id}>{u.nombre}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Desde</label>
            <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} className={inputClase} />
          </div>
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Hasta</label>
            <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} className={inputClase} />
          </div>
        </div>
      </Panel>

      <Panel className="p-0 overflow-hidden">
        {registros.length === 0 ? (
          <EmptyState message="No hay registros de auditoría con los filtros actuales" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-border">
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Fecha</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Usuario</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Rol</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Operación</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Producto</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Stock antes</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Cantidad</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Stock después</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Observación</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60">
                {registros.map((r) => (
                  <tr key={r.id} className="hover:bg-[#0d1119] transition-colors">
                    <td className="px-4 py-3 text-xs font-mono text-muted whitespace-nowrap">
                      {new Date(r.fechaHora).toLocaleString('es-CO', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
                    </td>
                    <td className="px-4 py-3 text-xs text-text whitespace-nowrap">{r.usuarioNombre}</td>
                    <td className="px-4 py-3">
                      <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${ROL_COLOR[r.rol] ?? 'bg-[#6b7a9922] text-[#6b7a99]'}`}>
                        {r.rol}
                      </span>
                    </td>
                    <td className={`px-4 py-3 text-xs font-mono font-bold ${OPERACION_COLOR[r.operacion] ?? 'text-muted'}`}>
                      {r.operacion}
                    </td>
                    <td className="px-4 py-3 text-xs text-text">{r.productoNombre ?? '—'}</td>
                    <td className="px-4 py-3 text-xs font-mono text-muted">{numero(r.stockAnterior)}</td>
                    <td className="px-4 py-3 text-xs font-mono text-text">+{numero(r.cantidad)}</td>
                    <td className="px-4 py-3 text-xs font-mono text-muted">{numero(r.stockPosterior)}</td>
                    <td className="px-4 py-3 text-xs text-muted max-w-[260px] truncate">{r.observacion || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
    </div>
  )
}