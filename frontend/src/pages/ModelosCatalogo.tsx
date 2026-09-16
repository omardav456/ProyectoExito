import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { ComplejidadModelo, EstadoModelo, ModeloListResponse, ModeloResumen, Taxonomia } from '@/types'
import { numero } from '@/lib/format'
import { KpiCard } from '@/components/ui/KpiCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { EmptyState } from '@/components/ui/EmptyState'

interface EstadisticasModelos {
  totalModelos: number
  porTipo: Record<string, number>
  porEstado: Record<string, number>
}

const TAM_PAGINA = 20
const COMPLEJIDADES: ComplejidadModelo[] = ['BAJA', 'MEDIA', 'ALTA']
const ESTADOS: EstadoModelo[] = ['PROPUESTO', 'PILOTO', 'EN_DESARROLLO', 'IMPLEMENTADO', 'VALIDADO']

const COMPLEJIDAD_PILL: Record<string, string> = {
  BAJA: 'bg-[#34d39922] text-[#34d399]',
  MEDIA: 'bg-[#fb923c22] text-[#fb923c]',
  ALTA: 'bg-[#f8717122] text-[#f87171]',
}

const ESTADO_ACTIVO = 'bg-[#FFD100] text-[#090c14] border-[#FFD100]'
const ESTADO_INACTIVO = 'bg-[#111827] border-[#1e2d45] text-[#6b7a99] hover:border-[#FFD10055]'

function PillComplejidad({ complejidad }: { complejidad: string }) {
  const clave = complejidad.toUpperCase()
  return (
    <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${COMPLEJIDAD_PILL[clave] ?? 'bg-[#60a5fa22] text-[#60a5fa]'}`}>
      {complejidad}
    </span>
  )
}

export default function ModelosCatalogo() {
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const [qBusqueda, setQBusqueda] = useState('')
  const [areaId, setAreaId] = useState<number | null>(null)
  const [categoriaId, setCategoriaId] = useState<number | null>(null)
  const [subcategoriaId, setSubcategoriaId] = useState<number | null>(null)
  const [tipo, setTipo] = useState('')
  const [complejidad, setComplejidad] = useState('')
  const [estado, setEstado] = useState('')
  const [page, setPage] = useState(0)
  const [tipos, setTipos] = useState<string[]>([])

  useEffect(() => {
    const t = setTimeout(() => setQBusqueda(q), 300)
    return () => clearTimeout(t)
  }, [q])

  const { data: taxonomia, loading: cargandoTax, error: errorTax, refetch: refetchTax } = useFetch<Taxonomia>(
    () => api.get('/modelos/taxonomia'),
    [],
  )
  const { data: stats, loading: cargandoStats, error: errorStats, refetch: refetchStats } = useFetch<EstadisticasModelos>(
    () => api.get('/modelos/estadisticas'),
    [],
  )
  const { data: lista, loading: cargandoLista, error: errorLista, refetch: refetchLista } = useFetch<ModeloListResponse>(
    () => {
      const params = new URLSearchParams()
      if (qBusqueda) params.set('q', qBusqueda)
      if (areaId !== null) params.set('areaId', String(areaId))
      if (categoriaId !== null) params.set('categoriaId', String(categoriaId))
      if (subcategoriaId !== null) params.set('subcategoriaId', String(subcategoriaId))
      if (tipo) params.set('tipo', tipo)
      if (complejidad) params.set('complejidad', complejidad)
      if (estado) params.set('estado', estado)
      params.set('page', String(page))
      params.set('size', String(TAM_PAGINA))
      return api.get<ModeloListResponse>(`/modelos?${params.toString()}`)
    },
    [qBusqueda, areaId, categoriaId, subcategoriaId, tipo, complejidad, estado, page],
  )

  useEffect(() => {
    setPage(0)
  }, [qBusqueda, areaId, categoriaId, subcategoriaId, tipo, complejidad, estado])

  useEffect(() => {
    if (lista) {
      setTipos((prev) => Array.from(new Set([...prev, ...lista.modelos.map((m) => m.tipoModelo)])))
    }
  }, [lista])

  const loading = cargandoLista || cargandoTax || cargandoStats
  const error = errorLista ?? errorTax ?? errorStats

  if (loading) return <LoadingSpinner />
  if (error || !lista) {
    return (
      <ErrorState
        message={error ?? 'No se pudieron cargar los modelos.'}
        onRetry={() => {
          refetchLista()
          refetchTax()
          refetchStats()
        }}
      />
    )
  }

  const areaSeleccionada = areaId !== null ? (taxonomia?.areas ?? []).find((a) => a.id === areaId) : undefined
  const categoriaSeleccionada =
    areaSeleccionada && categoriaId !== null ? areaSeleccionada.categorias.find((c) => c.id === categoriaId) : undefined

  const cambiarArea = (v: string) => {
    setAreaId(v ? Number(v) : null)
    setCategoriaId(null)
    setSubcategoriaId(null)
  }

  const cambiarCategoria = (v: string) => {
    setCategoriaId(v ? Number(v) : null)
    setSubcategoriaId(null)
  }

  const totalPaginas = Math.max(1, Math.ceil(lista.total / TAM_PAGINA))
  const inicio = lista.total === 0 ? 0 : page * TAM_PAGINA + 1
  const fin = Math.min((page + 1) * TAM_PAGINA, lista.total)

  const inputClase = 'w-full bg-[#0d1119] border border-[#1e2d45] rounded-lg px-3 py-2 text-xs font-mono text-[#f0f4f8] placeholder:text-[#3d4f6b] focus:outline-none focus:border-[#FFD10055] transition-colors [&>option]:bg-[#111827]'
  const selectClase = `${inputClase} appearance-none cursor-pointer`

  return (
    <div>
      <PageHeader title="Catálogo de modelos" subtitle="Modelos matemáticos documentados por área, categoría y complejidad" />

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <KpiCard label="Modelos" value={(stats?.totalModelos ?? 0)} unit="" icon="∑" color="bg-[#FFD10022] text-[#FFD100]" sub="Total documentados" />
        <KpiCard label="Ejecutables" value={((stats?.porEstado?.PILOTO ?? 0) + (stats?.porEstado?.IMPLEMENTADO ?? 0))} unit="" icon="⚙" color="bg-[#34d39922] text-[#34d399]" sub="Con motor ejecutable" />
        <KpiCard label="Validados" value={(stats?.porEstado?.VALIDADO ?? 0)} unit="" icon="✓" color="bg-[#60a5fa22] text-[#60a5fa]" sub="Verificados" />
        <KpiCard label="Propuestos" value={(stats?.porEstado?.PROPUESTO ?? 0)} unit="" icon="✎" color="bg-[#fb923c22] text-[#fb923c]" sub="En evaluación" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
        <Panel title="Filtros" className="h-fit lg:sticky lg:top-0">
          <div className="space-y-5">
            <div>
              <label className="block text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Buscar</label>
              <input type="text" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Código o nombre…" className={inputClase} />
            </div>

            <div>
              <label className="block text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Área</label>
              <select value={areaId ?? ''} onChange={(e) => cambiarArea(e.target.value)} className={selectClase}>
                <option value="">Todas las áreas</option>
                {(taxonomia?.areas ?? []).map((a) => (
                  <option key={a.id} value={a.id}>{a.nombre}</option>
                ))}
              </select>
            </div>

            {areaSeleccionada && (
              <div>
                <label className="block text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Categoría</label>
                <select value={categoriaId ?? ''} onChange={(e) => cambiarCategoria(e.target.value)} className={selectClase}>
                  <option value="">Todas las categorías</option>
                  {areaSeleccionada.categorias.map((c) => (
                    <option key={c.id} value={c.id}>{c.nombre}</option>
                  ))}
                </select>
              </div>
            )}

            {categoriaSeleccionada && (
              <div>
                <label className="block text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Subcategoría</label>
                <select value={subcategoriaId ?? ''} onChange={(e) => setSubcategoriaId(e.target.value ? Number(e.target.value) : null)} className={selectClase}>
                  <option value="">Todas las subcategorías</option>
                  {categoriaSeleccionada.subcategorias.map((s) => (
                    <option key={s.id} value={s.id}>{s.nombre}</option>
                  ))}
                </select>
              </div>
            )}

            {tipos.length > 0 && (
              <div>
                <label className="block text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Tipo de modelo</label>
                <select value={tipo} onChange={(e) => setTipo(e.target.value)} className={selectClase}>
                  <option value="">Todos los tipos</option>
                  {tipos.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </div>
            )}

            <div>
              <label className="block text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Complejidad</label>
              <div className="flex flex-wrap gap-1.5">
                {COMPLEJIDADES.map((c) => (
                  <button
                    key={c}
                    onClick={() => setComplejidad(complejidad === c ? '' : c)}
                    className={`px-2.5 py-1.5 rounded-lg text-[10px] font-mono font-bold border transition-colors cursor-pointer ${complejidad === c ? ESTADO_ACTIVO : ESTADO_INACTIVO}`}
                  >
                    {c}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Estado</label>
              <div className="flex flex-wrap gap-1.5">
                {ESTADOS.map((e) => (
                  <button
                    key={e}
                    onClick={() => setEstado(estado === e ? '' : e)}
                    className={`px-2.5 py-1.5 rounded-lg text-[10px] font-mono font-bold border transition-colors cursor-pointer ${estado === e ? ESTADO_ACTIVO : ESTADO_INACTIVO}`}
                  >
                    {e}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </Panel>

        <div className="lg:col-span-3">
          {lista.modelos.length === 0 ? (
            <Panel><EmptyState message="No se encontraron modelos con los filtros actuales" /></Panel>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
              {lista.modelos.map((m: ModeloResumen) => (
                <button
                  key={m.id}
                  onClick={() => navigate(`/modelos/${m.id}`)}
                  className="text-left w-full bg-[#111827] border border-[#1e2d45] rounded-xl p-5 hover:border-[#FFD10055] transition-colors cursor-pointer"
                >
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-[11px] font-mono font-bold text-[#FFD100] bg-[#FFD10022] border border-[#FFD10033] px-2 py-0.5 rounded">
                      {m.codigo}
                    </span>
                    <PillComplejidad complejidad={m.complejidad} />
                  </div>
                  <h3 className="font-semibold text-[#f0f4f8] mb-1 line-clamp-1">{m.nombre}</h3>
                  <p className="text-xs font-mono text-[#6b7a99] mb-3 truncate">{m.tipoModelo} · {m.metodoSugerido}</p>
                  <div className="mb-3"><StatusBadge estado={m.estado} /></div>
                  <p className="text-[11px] font-mono text-[#3d4f6b] truncate">{m.area} › {m.categoria} › {m.subcategoria}</p>
                </button>
              ))}
            </div>
          )}

          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mt-5">
            <p className="text-xs font-mono text-[#6b7a99]">
              Mostrando {numero(inicio)}–{numero(fin)} de {numero(lista.total)} modelos
            </p>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-2 rounded-lg text-xs font-mono font-bold border transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed bg-[#111827] border-[#1e2d45] text-[#6b7a99] hover:border-[#FFD10055]"
              >
                ← Anterior
              </button>
              <span className="text-xs font-mono text-[#6b7a99]">
                Pág. {numero(page + 1)} / {numero(totalPaginas)}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPaginas - 1, p + 1))}
                disabled={page >= totalPaginas - 1}
                className="px-3 py-2 rounded-lg text-xs font-mono font-bold border transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed bg-[#111827] border-[#1e2d45] text-[#6b7a99] hover:border-[#FFD10055]"
              >
                Siguiente →
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}