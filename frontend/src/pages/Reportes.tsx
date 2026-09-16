import { Area, AreaChart, Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { DashboardSummary } from '@/types'
import { numero } from '@/lib/format'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { EmptyState } from '@/components/ui/EmptyState'
import { CustomTooltip } from '@/components/ui/CustomTooltip'

export default function Reportes() {
  const { data: resumen, loading, error, refetch } = useFetch<DashboardSummary>(
    () => api.get('/dashboard/summary'),
    [],
  )

  if (loading) return <LoadingSpinner />
  if (error || !resumen) return <ErrorState message={error ?? 'No se pudieron cargar los reportes.'} onRetry={refetch} />

  const filas = [
    { etiqueta: 'Total productos', valor: numero(resumen.totalProductos), color: 'text-[#FFD100]' },
    { etiqueta: 'Stock total', valor: numero(resumen.stockTotal), color: 'text-[#f0f4f8]' },
    { etiqueta: 'Productos críticos', valor: numero(resumen.productosCriticos), color: 'text-[#f87171]' },
    { etiqueta: 'Productos bajos', valor: numero(resumen.productosBajos), color: 'text-[#fb923c]' },
    { etiqueta: 'Sobrestock', valor: numero(resumen.productosSobrestock), color: 'text-[#60a5fa]' },
    { etiqueta: 'Alertas activas', valor: numero(resumen.totalAlertasActivas), color: 'text-[#f87171]' },
    { etiqueta: 'Recomendaciones activas', valor: numero(resumen.totalRecomendacionesActivas), color: 'text-[#f0f4f8]' },
  ]

  return (
    <div>
      <PageHeader title="Reportes" subtitle="Resumen ejecutivo del estado actual del inventario" />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-6">
        <Panel title="Resumen general">
          <table className="w-full text-left border-collapse">
            <tbody className="divide-y divide-[#1e2d45]/60">
              {filas.map((f) => (
                <tr key={f.etiqueta}>
                  <td className="py-3 text-xs font-mono uppercase tracking-widest text-[#6b7a99]">{f.etiqueta}</td>
                  <td className={`py-3 text-right text-sm font-mono font-bold ${f.color}`}>{f.valor}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>

        <Panel title="Stock por categoría">
          {resumen.stockPorCategoria.length === 0 ? (
            <EmptyState message="No hay datos de stock por categoría" />
          ) : (
            <div className="h-[280px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={resumen.stockPorCategoria} layout="vertical" margin={{ top: 8, right: 16, left: 8, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e2d45" horizontal={false} />
                  <XAxis type="number" stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                  <YAxis type="category" dataKey="nombre" stroke="#6b7a99" fontSize={11} tickLine={false} axisLine={false} width={110} />
                  <Tooltip content={<CustomTooltip />} />
                  <Bar dataKey="stockTotal" name="Stock total" fill="#FFD100" radius={[0, 4, 4, 0]} barSize={18} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </Panel>
      </div>

      <Panel title="Tendencia de demanda semanal">
        {resumen.demandaSemana.length === 0 ? (
          <EmptyState message="No hay datos de demanda semanal" />
        ) : (
          <div className="h-[280px]">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={resumen.demandaSemana} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
                <defs>
                  <linearGradient id="gradReporte" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#FFD100" stopOpacity={0.35} />
                    <stop offset="95%" stopColor="#FFD100" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e2d45" vertical={false} />
                <XAxis dataKey="dia" stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                <YAxis stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                <Tooltip content={<CustomTooltip />} />
                <Area type="monotone" dataKey="unidades" name="Ventas" stroke="#FFD100" strokeWidth={2.5} fill="url(#gradReporte)" />
                <Area type="monotone" dataKey="forecast" name="Pronóstico" stroke="#60a5fa" strokeWidth={2} fill="url(#gradReporte)" fillOpacity={0.15} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </Panel>
    </div>
  )
}