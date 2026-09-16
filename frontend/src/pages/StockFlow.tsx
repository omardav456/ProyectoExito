import { useState } from 'react'
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { Categoria, StockFlowResponse } from '@/types'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { CustomTooltip } from '@/components/ui/CustomTooltip'

const FORMULA_DEFAULT = 'I(t+1) = I(t) + Entradas(t) − Salidas(t)'

function Paso({ etiqueta, descripcion, icono, color }: { etiqueta: string; descripcion: string; icono: string; color: string }) {
  return (
    <div className={`flex-1 bg-[#0d1119] border ${color} rounded-xl px-4 py-5 text-center`}>
      <span className={`block text-2xl mb-2 ${color}`}>{icono}</span>
      <p className={`text-sm font-mono font-bold tracking-widest ${color}`}>{etiqueta}</p>
      <p className="text-[11px] font-mono text-[#6b7a99] mt-1">{descripcion}</p>
    </div>
  )
}

export default function StockFlow() {
  const [categoriaId, setCategoriaId] = useState<number | null>(null)

  const { data: categorias, loading: cargandoCat, error: errorCat } = useFetch<Categoria[]>(
    () => api.get('/categorias'),
    [],
  )
  const { data: flujo, loading: cargandoFlujo, error: errorFlujo, refetch: refetchFlujo } = useFetch<StockFlowResponse>(
    () => api.get<StockFlowResponse>(categoriaId !== null ? `/stockflow/flujo?categoriaId=${categoriaId}` : '/stockflow/flujo'),
    [categoriaId],
  )

  const loading = cargandoFlujo || cargandoCat
  const error = errorFlujo ?? errorCat

  if (loading) return <LoadingSpinner />
  if (error || !flujo) return <ErrorState message={error ?? 'No se pudieron cargar los datos de flujo.'} onRetry={refetchFlujo} />

  return (
    <div>
      <PageHeader title="Stock & Flow" subtitle="Diagrama conceptual y evolución del flujo de inventario" />

      <Panel title="Modelo conceptual" className="mb-6">
        <div className="flex flex-col lg:flex-row items-stretch lg:items-center gap-3 mb-6">
          <Paso etiqueta="REPOSICIÓN" descripcion="Reabastecimiento · entradas" icono="⇪" color="border-[#fb923c] text-[#fb923c]" />
          <span className="text-center text-[#3d4f6b] font-mono text-2xl shrink-0">→</span>
          <Paso etiqueta="INVENTARIO" descripcion="Estado del sistema" icono="▦" color="border-[#FFD100] text-[#FFD100]" />
          <span className="text-center text-[#3d4f6b] font-mono text-2xl shrink-0">→</span>
          <Paso etiqueta="VENTAS" descripcion="Salidas · demanda" icono="◎" color="border-[#34d399] text-[#34d399]" />
        </div>
        <div className="bg-[#0d1119] border border-[#1e2d45] rounded-lg px-4 py-3 text-center">
          <p className="text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1">Ecuación de inventario</p>
          <p className="font-mono text-lg text-[#FFD100]">{flujo.formula || FORMULA_DEFAULT}</p>
        </div>
      </Panel>

      <div className="flex flex-wrap gap-2 mb-6">
        <button
          onClick={() => setCategoriaId(null)}
          className={`px-3 py-2 rounded-lg text-xs font-mono font-bold border transition-colors cursor-pointer ${
            categoriaId === null
              ? 'bg-[#FFD100] text-[#090c14] border-[#FFD100]'
              : 'bg-[#111827] border-[#1e2d45] text-[#6b7a99] hover:border-[#FFD10055]'
          }`}
        >
          Todas
        </button>
        {(categorias ?? []).map((c) => (
          <button
            key={c.id}
            onClick={() => setCategoriaId(categoriaId === c.id ? null : c.id)}
            className={`px-3 py-2 rounded-lg text-xs font-mono font-bold border transition-colors cursor-pointer ${
              categoriaId === c.id
                ? 'bg-[#FFD100] text-[#090c14] border-[#FFD100]'
                : 'bg-[#111827] border-[#1e2d45] text-[#6b7a99] hover:border-[#FFD10055]'
            }`}
          >
            {c.nombre}
          </button>
        ))}
      </div>

      <Panel title={`Evolución del flujo — ${flujo.categoria}`}>
        {flujo.filas.length === 0 ? (
          <p className="text-sm font-mono text-[#6b7a99] text-center py-16">No hay datos de flujo para esta categoría.</p>
        ) : (
          <div className="h-[320px]">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={flujo.filas} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e2d45" vertical={false} />
                <XAxis dataKey="t" stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                <YAxis stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                <Tooltip content={<CustomTooltip />} />
                <Legend wrapperStyle={{ fontSize: 12, fontFamily: 'JetBrains Mono, monospace', color: '#6b7a99' }} />
                <Line type="monotone" dataKey="inventario" name="Inventario" stroke="#FFD100" strokeWidth={2.5} dot={false} />
                <Line type="monotone" dataKey="entradas" name="Entradas" stroke="#34d399" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="salidas" name="Salidas" stroke="#f87171" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </Panel>
    </div>
  )
}