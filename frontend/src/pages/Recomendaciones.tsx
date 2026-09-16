import { api } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { Recomendacion } from '@/types'
import { fechaLarga } from '@/lib/format'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { EmptyState } from '@/components/ui/EmptyState'

const PRIORIDAD_BORDE: Record<string, string> = {
  URGENTE: 'border-l-[#f87171]',
  ALTA: 'border-l-[#FFD100]',
  MEDIA: 'border-l-[#fb923c]',
  BAJA: 'border-l-[#60a5fa]',
}

const PRIORIDAD_PILL: Record<string, string> = {
  URGENTE: 'bg-[#f8717122] text-[#f87171]',
  ALTA: 'bg-[#FFD10022] text-[#FFD100]',
  MEDIA: 'bg-[#fb923c22] text-[#fb923c]',
  BAJA: 'bg-[#60a5fa22] text-[#60a5fa]',
}

const ORDEN: Record<string, number> = { URGENTE: 0, ALTA: 1, MEDIA: 2, BAJA: 3 }

export default function Recomendaciones() {
  const { data: recomendaciones, loading, error, refetch } = useFetch<Recomendacion[]>(
    () => api.get('/recomendaciones'),
    [],
  )

  if (loading) return <LoadingSpinner />
  if (error || !recomendaciones) return <ErrorState message={error ?? 'No se pudieron cargar las recomendaciones.'} onRetry={refetch} />

  const ordenadas = [...recomendaciones].sort((a, b) => (ORDEN[a.prioridad.toUpperCase()] ?? 9) - (ORDEN[b.prioridad.toUpperCase()] ?? 9))

  return (
    <div>
      <PageHeader title="Recomendaciones" subtitle="Acciones sugeridas por el motor analítico según el estado del inventario" />

      <div className="flex items-start gap-3 bg-[#60a5fa22] border border-[#60a5fa33] rounded-xl px-4 py-3 mb-6">
        <span className="text-[#60a5fa] text-lg leading-none mt-0.5">ℹ</span>
        <p className="text-xs font-mono text-[#60a5fa] leading-relaxed">
          Las recomendaciones no modifican horarios ni realizan acciones automáticamente. Todas requieren aprobación del equipo.
        </p>
      </div>

      {ordenadas.length === 0 ? (
        <Panel><EmptyState message="No hay recomendaciones activas" /></Panel>
      ) : (
        <div className="space-y-4">
          {ordenadas.map((r) => {
            const clave = r.prioridad.toUpperCase()
            return (
              <div key={r.id} className={`bg-[#111827] border border-[#1e2d45] border-l-4 rounded-xl p-5 ${PRIORIDAD_BORDE[clave] ?? 'border-l-[#60a5fa]'}`}>
                <div className="flex flex-wrap items-center gap-2 mb-2.5">
                  <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${PRIORIDAD_PILL[clave] ?? 'bg-[#60a5fa22] text-[#60a5fa]'}`}>{r.prioridad}</span>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-[#1e2d45] text-[#6b7a99]">{r.tipo}</span>
                  <span className="text-[11px] font-mono text-[#3d4f6b] ml-auto">{fechaLarga(r.fecha)}</span>
                </div>
                <h3 className="font-semibold text-[#f0f4f8] mb-1">{r.titulo}</h3>
                <p className="text-xs text-[#6b7a99] leading-relaxed mb-3">{r.descripcion}</p>
                <div className="flex items-center gap-2 bg-[#0d1119] border border-[#1e2d45] rounded-lg px-3 py-2">
                  <span className="text-[10px] font-mono uppercase tracking-widest text-[#3d4f6b] shrink-0">Acción</span>
                  <p className="text-xs font-mono text-[#FFD100]">{r.accionSugerida}</p>
                  {r.productoNombre && <span className="text-[11px] font-mono text-[#6b7a99] ml-auto">{r.productoNombre}</span>}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}