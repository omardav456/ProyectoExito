import { api } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { Alerta } from '@/types'
import { fechaLarga } from '@/lib/format'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { KpiCard } from '@/components/ui/KpiCard'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { EmptyState } from '@/components/ui/EmptyState'

const TIPO_BORDE: Record<string, string> = {
  CRITICO: 'border-l-[#f87171]',
  ADVERTENCIA: 'border-l-[#fb923c]',
  INFO: 'border-l-[#60a5fa]',
}

const TIPO_PILL: Record<string, string> = {
  CRITICO: 'bg-[#f8717122] text-[#f87171]',
  ADVERTENCIA: 'bg-[#fb923c22] text-[#fb923c]',
  INFO: 'bg-[#60a5fa22] text-[#60a5fa]',
}

const PRIORIDAD_PILL: Record<string, string> = {
  ALTA: 'bg-[#f8717122] text-[#f87171]',
  MEDIA: 'bg-[#fb923c22] text-[#fb923c]',
  BAJA: 'bg-[#60a5fa22] text-[#60a5fa]',
}

const ORDEN: Record<string, number> = { CRITICO: 0, ADVERTENCIA: 1, INFO: 2 }

export default function Alertas() {
  const { data: alertas, loading: cargandoAlertas, error: errorAlertas, refetch: refetchAlertas } = useFetch<Alerta[]>(
    () => api.get('/alertas'),
    [],
  )
  const { data: contadores, loading: cargandoContadores, error: errorContadores, refetch: refetchContadores } = useFetch<number[]>(
    () => api.get('/alertas/contar-activas'),
    [],
  )

  const loading = cargandoAlertas || cargandoContadores
  const error = errorAlertas ?? errorContadores

  if (loading) return <LoadingSpinner />
  if (error || !alertas || !contadores) {
    return (
      <ErrorState
        message={error ?? 'No se pudieron cargar las alertas.'}
        onRetry={() => {
          refetchAlertas()
          refetchContadores()
        }}
      />
    )
  }

  const criticas = contadores[0] ?? 0
  const advertencias = contadores[1] ?? 0
  const infos = contadores[2] ?? 0

  const ordenadas = [...alertas].sort((a, b) => (ORDEN[a.tipo] ?? 3) - (ORDEN[b.tipo] ?? 3))

  return (
    <div>
      <PageHeader title="Alertas" subtitle="Alertas activas del sistema según el estado del inventario" />

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <KpiCard label="Críticas" value={criticas} icon="⚠" color="bg-[#f8717122] text-[#f87171]" sub="Requieren acción inmediata" />
        <KpiCard label="Advertencias" value={advertencias} icon="▁" color="bg-[#fb923c22] text-[#fb923c]" sub="Nivel bajo de stock" />
        <KpiCard label="Informativas" value={infos} icon="i" color="bg-[#60a5fa22] text-[#60a5fa]" sub="Seguimiento y monitoreo" />
      </div>

      {ordenadas.length === 0 ? (
        <Panel><EmptyState message="No hay alertas activas" /></Panel>
      ) : (
        <div className="space-y-4">
          {ordenadas.map((a) => (
            <div key={a.id} className={`bg-[#111827] border border-[#1e2d45] border-l-4 rounded-xl p-5 ${TIPO_BORDE[a.tipo] ?? 'border-l-[#60a5fa]'}`}>
              <div className="flex flex-wrap items-center gap-2 mb-2">
                <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${TIPO_PILL[a.tipo] ?? ''}`}>{a.tipo}</span>
                <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${PRIORIDAD_PILL[a.prioridad] ?? 'bg-[#60a5fa22] text-[#60a5fa]'}`}>{a.prioridad}</span>
                <span className={`text-[10px] font-mono px-2 py-0.5 rounded bg-[#34d39922] text-[#34d399]`}>{a.estado}</span>
                <span className="text-[11px] font-mono text-[#3d4f6b] ml-auto">{fechaLarga(a.hora)}</span>
              </div>
              <p className="text-sm text-[#f0f4f8] leading-relaxed">{a.mensaje}</p>
              {a.productoNombre && <p className="text-[11px] font-mono text-[#FFD100] mt-1.5">{a.productoNombre}</p>}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}