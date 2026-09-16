import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { ModeloDetail } from '@/types'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { EmptyState } from '@/components/ui/EmptyState'

const COMPLEJIDAD_PILL: Record<string, string> = {
  BAJA: 'bg-[#34d39922] text-[#34d399]',
  MEDIA: 'bg-[#fb923c22] text-[#fb923c]',
  ALTA: 'bg-[#f8717122] text-[#f87171]',
}

const ROL_PILL: Record<string, string> = {
  ENTRADA: 'bg-[#34d39922] text-[#34d399]',
  SALIDA: 'bg-[#f8717122] text-[#f87171]',
  ESTADO: 'bg-[#60a5fa22] text-[#60a5fa]',
}

const TABS = [
  { key: 'variables', label: 'Variables' },
  { key: 'parametros', label: 'Parámetros' },
  { key: 'metodos', label: 'Métodos' },
  { key: 'aplicaciones', label: 'Aplicaciones' },
] as const

type TabActiva = (typeof TABS)[number]['key']

export default function ModeloDetalle() {
  const { id } = useParams()
  const { data: modelo, loading, error, refetch } = useFetch<ModeloDetail>(
    () => api.get<ModeloDetail>(`/modelos/${id}`),
    [id],
  )
  const [tab, setTab] = useState<TabActiva>('variables')

  if (loading) return <LoadingSpinner />
  if (error || !modelo) return <ErrorState message={error ?? 'No se pudo cargar el modelo.'} onRetry={refetch} />

  return (
    <div>
      <Link
        to="/modelos"
        className="inline-block text-xs font-mono uppercase tracking-widest text-[#6b7a99] hover:text-[#FFD100] transition-colors mb-4"
      >
        ← Volver al catálogo
      </Link>

      <PageHeader title={modelo.nombre} subtitle={`${modelo.codigo} · ${modelo.tipoModelo} · ${modelo.metodoSugerido}`} />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-4">
        <Panel className="lg:col-span-2">
          <div className="flex flex-wrap items-center gap-2 mb-4">
            <span className="text-[11px] font-mono font-bold text-[#FFD100] bg-[#FFD10022] border border-[#FFD10033] px-2 py-0.5 rounded">
              {modelo.codigo}
            </span>
            <StatusBadge estado={modelo.estado} />
            <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${COMPLEJIDAD_PILL[modelo.complejidad.toUpperCase()] ?? 'bg-[#60a5fa22] text-[#60a5fa]'}`}>
              {modelo.complejidad}
            </span>
          </div>
          <p className="text-sm text-[#f0f4f8] leading-relaxed mb-4">{modelo.descripcion}</p>
          <p className="text-xs font-mono leading-relaxed text-[#6b7a99]">
            <span className="uppercase tracking-widest text-[#3d4f6b] mr-2">Problema:</span>
            {modelo.problema}
          </p>
        </Panel>

        <Panel title="Clasificación">
          <div className="space-y-3">
            <div>
              <p className="text-[10px] font-mono uppercase tracking-widest text-[#3d4f6b] mb-1">Ubicación taxonómica</p>
              <p className="text-xs font-mono text-[#f0f4f8]">{modelo.area} › {modelo.categoria} › {modelo.subcategoria}</p>
            </div>
            <div>
              <p className="text-[10px] font-mono uppercase tracking-widest text-[#3d4f6b] mb-1">Motor de implementación</p>
              <p className="text-xs font-mono text-[#6b7a99]">{modelo.motorImpl ?? 'No definido'}</p>
            </div>
            {modelo.documentacion && (
              <div>
                <p className="text-[10px] font-mono uppercase tracking-widest text-[#3d4f6b] mb-1">Documentación</p>
                <p className="text-xs font-mono text-[#6b7a99]">{modelo.documentacion}</p>
              </div>
            )}
          </div>
        </Panel>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-4">
        <Panel title="Entrada esperada" className="border-l-[#34d399]">
          <p className="text-xs font-mono text-[#f0f4f8] leading-relaxed">{modelo.entradaEsperada}</p>
        </Panel>
        <Panel title="Salida esperada" className="border-l-[#f87171]">
          <p className="text-xs font-mono text-[#f0f4f8] leading-relaxed">{modelo.salidaEsperada}</p>
        </Panel>
        <Panel title="Problemática">
          <p className="text-xs font-mono text-[#f0f4f8] leading-relaxed">{modelo.problema}</p>
        </Panel>
      </div>

      <div className="flex flex-wrap gap-2 mb-4">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2 rounded-lg text-xs font-mono font-bold border transition-colors cursor-pointer ${
              tab === t.key
                ? 'bg-[#FFD100] text-[#090c14] border-[#FFD100]'
                : 'bg-[#111827] border-[#1e2d45] text-[#6b7a99] hover:border-[#FFD10055]'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'variables' && (
        <Panel title="Variables del modelo">
          {modelo.variables.length === 0 ? (
            <EmptyState message="El modelo no define variables" />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-[#1e2d45]">
                    <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Símbolo</th>
                    <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Nombre</th>
                    <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Rol</th>
                    <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Tipo</th>
                    <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Unidad</th>
                    <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Descripción</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#1e2d45]/60">
                  {modelo.variables.map((v) => (
                    <tr key={v.id}>
                      <td className="px-4 py-3 text-xs font-mono font-bold text-[#FFD100]">{v.simbolo}</td>
                      <td className="px-4 py-3 text-sm text-[#f0f4f8]">{v.nombre}</td>
                      <td className="px-4 py-3">
                        <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${ROL_PILL[v.rol] ?? ''}`}>{v.rol}</span>
                      </td>
                      <td className="px-4 py-3 text-xs font-mono text-[#6b7a99]">{v.tipoDato}</td>
                      <td className="px-4 py-3 text-xs font-mono text-[#6b7a99]">{v.unidad || '—'}</td>
                      <td className="px-4 py-3 text-xs text-[#f0f4f8]">{v.descripcion}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Panel>
      )}

      {tab === 'parametros' && (
        <Panel title="Parámetros del modelo">
          {modelo.parametros.length === 0 ? (
            <EmptyState message="El modelo no define parámetros" />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-[#1e2d45]">
                    <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Símbolo</th>
                    <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Nombre</th>
                    <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Valor por defecto</th>
                    <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Unidad</th>
                    <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Descripción</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#1e2d45]/60">
                  {modelo.parametros.map((p) => (
                    <tr key={p.id}>
                      <td className="px-4 py-3 text-xs font-mono font-bold text-[#FFD100]">{p.simbolo}</td>
                      <td className="px-4 py-3 text-sm text-[#f0f4f8]">{p.nombre}</td>
                      <td className="px-4 py-3 text-xs font-mono text-[#f0f4f8]">{p.valorPorDefecto}</td>
                      <td className="px-4 py-3 text-xs font-mono text-[#6b7a99]">{p.unidad || '—'}</td>
                      <td className="px-4 py-3 text-xs text-[#f0f4f8]">{p.descripcion}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Panel>
      )}

      {tab === 'metodos' && (
        <Panel title="Métodos sugeridos">
          {modelo.metodos.length === 0 ? (
            <EmptyState message="El modelo no define métodos" />
          ) : (
            <ol className="space-y-3">
              {modelo.metodos.map((m, i) => (
                <li key={i} className="flex items-start gap-3">
                  <span className="w-6 h-6 rounded-lg bg-[#FFD10022] text-[#FFD100] border border-[#FFD10033] flex items-center justify-center text-[10px] font-mono font-bold shrink-0">
                    {i + 1}
                  </span>
                  <span className="text-sm text-[#f0f4f8]">{m}</span>
                </li>
              ))}
            </ol>
          )}
        </Panel>
      )}

      {tab === 'aplicaciones' && (
        <Panel title="Aplicaciones">
          {modelo.aplicaciones.length === 0 ? (
            <EmptyState message="El modelo no define aplicaciones" />
          ) : (
            <ul className="space-y-3">
              {modelo.aplicaciones.map((a, i) => (
                <li key={i} className="flex items-start gap-3">
                  <span className="mt-2 w-1.5 h-1.5 rounded-full bg-[#60a5fa] shrink-0" />
                  <span className="text-sm text-[#f0f4f8]">{a}</span>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      )}
    </div>
  )
}