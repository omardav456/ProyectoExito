import { Area, AreaChart, Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { DashboardSummary, Producto } from '@/types'
import { moneda, numero } from '@/lib/format'
import { KpiCard } from '@/components/ui/KpiCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { EmptyState } from '@/components/ui/EmptyState'
import { CustomTooltip } from '@/components/ui/CustomTooltip'

const ESTADOS_RIESGO = ['CRITICO', 'BAJO', 'SOBRESTOCK']

export default function Dashboard() {
  const { data: resumen, loading: cargandoResumen, error: errorResumen, refetch: refetchResumen } = useFetch<DashboardSummary>(
    () => api.get('/dashboard/summary'),
    [],
  )
  const { data: productos, loading: cargandoProductos, error: errorProductos, refetch: refetchProductos } = useFetch<Producto[]>(
    () => api.get('/productos?activo=true'),
    [],
  )

  const loading = cargandoResumen || cargandoProductos
  const error = errorResumen ?? errorProductos

  if (loading) return <LoadingSpinner />
  if (error || !resumen) {
    return (
      <ErrorState
        message={error ?? 'No se pudieron cargar los datos.'}
        onRetry={() => {
          refetchResumen()
          refetchProductos()
        }}
      />
    )
  }

  const stockTotal = resumen.stockTotal ?? 0
  const valorInventario = (productos ?? []).reduce((acc, p) => acc + p.stockActual * p.precio, 0)
  const enRiesgo = (productos ?? []).filter((p) => ESTADOS_RIESGO.includes(p.estado)).slice(0, 8)

  const kpis = [
    { label: 'Valor inventario', value: moneda(valorInventario), icon: '$', color: 'bg-[#FFD10022] text-[#FFD100]', sub: `${numero(stockTotal)} unidades en bodega` },
    { label: 'Stock total', value: numero(stockTotal), icon: '▦', color: 'bg-[#34d39922] text-[#34d399]', sub: 'Unidades disponibles' },
    { label: 'Productos críticos', value: numero(resumen.productosCriticos), icon: '⚠', color: 'bg-[#f8717122] text-[#f87171]', sub: 'Stock por debajo del mínimo' },
    { label: 'Productos bajos', value: numero(resumen.productosBajos), icon: '▁', color: 'bg-[#fb923c22] text-[#fb923c]', sub: 'Nivel bajo de inventario' },
    { label: 'Sobrestock', value: numero(resumen.productosSobrestock), icon: '↥', color: 'bg-[#60a5fa22] text-[#60a5fa]', sub: 'Exceso de inventario' },
    { label: 'Alertas activas', value: numero(resumen.totalAlertasActivas), icon: '!', color: 'bg-[#f8717122] text-[#f87171]', sub: 'Requieren atención' },
    { label: 'Recomendaciones', value: numero(resumen.totalRecomendacionesActivas), icon: '✦', color: 'bg-[#FFD10022] text-[#FFD100]', sub: 'Acciones sugeridas' },
    { label: 'Total productos', value: numero(resumen.totalProductos), icon: '#', color: 'bg-[#34d39922] text-[#34d399]', sub: 'Productos registrados' },
  ]

  return (
    <div>
      <PageHeader title="Dashboard" subtitle="Panorama general del sistema de gestión predictiva de inventario" />

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        {kpis.map((k) => (
          <KpiCard key={k.label} label={k.label} value={k.value} icon={k.icon} color={k.color} sub={k.sub} />
        ))}
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-4 mb-6">
        <Panel title="Demanda semanal — unidades vs pronóstico" className="xl:col-span-2">
          {resumen.demandaSemana.length === 0 ? (
            <EmptyState message="No hay datos de demanda semanal" />
          ) : (
            <div className="h-[280px]">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={resumen.demandaSemana} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
                  <defs>
                    <linearGradient id="gradUnidades" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#FFD100" stopOpacity={0.35} />
                      <stop offset="95%" stopColor="#FFD100" stopOpacity={0} />
                    </linearGradient>
                    <linearGradient id="gradPronostico" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#60a5fa" stopOpacity={0.35} />
                      <stop offset="95%" stopColor="#60a5fa" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e2d45" vertical={false} />
                  <XAxis dataKey="dia" stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                  <YAxis stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                  <Tooltip content={<CustomTooltip />} />
                  <Area type="monotone" dataKey="unidades" name="Ventas" stroke="#FFD100" strokeWidth={2} fill="url(#gradUnidades)" />
                  <Area type="monotone" dataKey="forecast" name="Pronóstico" stroke="#60a5fa" strokeWidth={2} fill="url(#gradPronostico)" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </Panel>

        <Panel title="Productos en estado crítico">
          {enRiesgo.length === 0 ? (
            <EmptyState message="Sin productos en estado crítico" />
          ) : (
            <ul className="space-y-2.5">
              {enRiesgo.map((p) => (
                <li key={p.id} className="flex items-center justify-between gap-3 bg-[#0d1119] border border-[#1e2d45] rounded-lg px-3 py-2.5">
                  <div className="min-w-0">
                    <p className="text-sm text-[#f0f4f8] truncate">{p.nombre}</p>
                    <p className="text-[11px] font-mono text-[#6b7a99] mt-0.5">
                      {numero(p.stockActual)} u · {p.diasInventario} días inv.
                    </p>
                  </div>
                  <StatusBadge estado={p.estado} />
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>

      <Panel title="Stock por categoría">
        {resumen.stockPorCategoria.length === 0 ? (
          <EmptyState message="No hay datos de stock por categoría" />
        ) : (
          <div className="h-[240px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={resumen.stockPorCategoria} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e2d45" vertical={false} />
                <XAxis dataKey="nombre" stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} interval={0} />
                <YAxis stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="stockTotal" name="Stock total" fill="#FFD100" radius={[4, 4, 0, 0]} barSize={22} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </Panel>
    </div>
  )
}