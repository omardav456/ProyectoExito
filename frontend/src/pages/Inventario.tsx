import { useEffect, useState } from 'react'
import { api, ApiError } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { Categoria, Producto } from '@/types'
import { numero } from '@/lib/format'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { EmptyState } from '@/components/ui/EmptyState'

function construirQuery(q: string, categoria: number | null): string {
  const params = new URLSearchParams()
  params.set('activo', 'true')
  if (q) params.set('q', q)
  if (categoria !== null) params.set('categoriaId', String(categoria))
  return `/productos?${params.toString()}`
}

function colorDias(dias: number): string {
  if (dias <= 1) return 'text-[#f87171] font-bold'
  if (dias <= 3) return 'text-[#fb923c] font-bold'
  return 'text-[#34d399]'
}

export default function Inventario() {
  const [q, setQ] = useState('')
  const [qFiltrada, setQFiltrada] = useState('')
  const [categoria, setCategoria] = useState<number | null>(null)
  const [modalProducto, setModalProducto] = useState<Producto | null>(null)
  const [movTipo, setMovTipo] = useState<'ENTRADA' | 'SALIDA' | 'AJUSTE'>('ENTRADA')
  const [movCantidad, setMovCantidad] = useState('1')
  const [movDescripcion, setMovDescripcion] = useState('')
  const [guardandoMov, setGuardandoMov] = useState(false)
  const [movError, setMovError] = useState('')

  useEffect(() => {
    const t = setTimeout(() => setQFiltrada(q), 300)
    return () => clearTimeout(t)
  }, [q])

  const { data: categorias, loading: cargandoCat, error: errorCat } = useFetch<Categoria[]>(
    () => api.get('/categorias'),
    [],
  )
  const { data: productos, loading: cargandoProd, error: errorProd, refetch: refetchProd } = useFetch<Producto[]>(
    () => api.get<Producto[]>(construirQuery(qFiltrada, categoria)),
    [qFiltrada, categoria],
  )

  const loading = cargandoProd || cargandoCat
  const error = errorProd ?? errorCat

  const registrarMovimiento = async () => {
    if (!modalProducto) return
    setMovError('')
    setGuardandoMov(true)
    try {
      await api.post('/movimientos', {
        productoId: modalProducto.id,
        tipo: movTipo,
        cantidad: Number(movCantidad),
        descripcion: movDescripcion || undefined,
      })
      setModalProducto(null)
      setMovCantidad('1')
      setMovDescripcion('')
      setMovTipo('ENTRADA')
      refetchProd()
    } catch (err) {
      setMovError(err instanceof ApiError ? err.message : 'No se pudo registrar el movimiento')
    } finally {
      setGuardandoMov(false)
    }
  }

  if (loading) return <LoadingSpinner />
  if (error || !productos) return <ErrorState message={error ?? 'No se pudieron cargar los productos.'} onRetry={refetchProd} />

  return (
    <div>
      <PageHeader title="Inventario" subtitle="Estado actual de productos, niveles de stock y acciones recomendadas" />

      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3 mb-5">
        <input
          type="text"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Buscar producto…"
          className="w-full md:w-72 bg-[#111827] border border-[#1e2d45] rounded-lg px-3 py-2 text-sm text-[#f0f4f8] placeholder:text-[#3d4f6b] focus:outline-none focus:border-[#FFD10055] transition-colors"
        />
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setCategoria(null)}
            className={`px-3 py-2 rounded-lg text-xs font-mono font-bold border transition-colors cursor-pointer ${
              categoria === null
                ? 'bg-[#FFD100] text-[#090c14] border-[#FFD100]'
                : 'bg-[#111827] border-[#1e2d45] text-[#6b7a99] hover:border-[#FFD10055]'
            }`}
          >
            Todas
          </button>
          {(categorias ?? []).map((c) => (
            <button
              key={c.id}
              onClick={() => setCategoria(categoria === c.id ? null : c.id)}
              className={`px-3 py-2 rounded-lg text-xs font-mono font-bold border transition-colors cursor-pointer ${
                categoria === c.id
                  ? 'bg-[#FFD100] text-[#090c14] border-[#FFD100]'
                  : 'bg-[#111827] border-[#1e2d45] text-[#6b7a99] hover:border-[#FFD10055]'
              }`}
            >
              {c.nombre}
            </button>
          ))}
        </div>
      </div>

      <Panel className="p-0 overflow-hidden">
        {productos.length === 0 ? (
          <EmptyState message="No se encontraron productos con los filtros actuales" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-[#1e2d45]">
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">ID</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Producto</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Categoría</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Descripción</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Stock</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Vtas/día</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Demanda</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Stock mín</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Días inv.</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Estado</th>
                  <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-[#6b7a99]">Acción</th>
                  <th className="px-4 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#1e2d45]/60">
                {productos.map((p) => (
                  <tr key={p.id} className="hover:bg-[#0d1119] transition-colors">
                    <td className="px-4 py-3 text-xs font-mono text-[#6b7a99]">{p.id}</td>
                    <td className="px-4 py-3 text-sm text-[#f0f4f8] font-medium">{p.nombre}</td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center gap-1.5 text-xs text-[#6b7a99]">
                        <span className="w-2 h-2 rounded-full" style={{ backgroundColor: p.categoriaColor }} />
                        {p.categoriaNombre}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-[#6b7a99] max-w-[200px] truncate">{p.descripcion ?? '—'}</td>
                    <td className={`px-4 py-3 text-sm font-mono ${p.stockActual < p.stockMinimo ? 'text-[#f87171] font-bold' : 'text-[#f0f4f8]'}`}>
                      {numero(p.stockActual)}
                    </td>
                    <td className="px-4 py-3 text-xs font-mono text-[#f0f4f8]">{numero(p.ventasPromedio)}</td>
                    <td className="px-4 py-3 text-xs font-mono text-[#f0f4f8]">{numero(p.demandaPrevista)}</td>
                    <td className="px-4 py-3 text-xs font-mono text-[#6b7a99]">{numero(p.stockMinimo)}</td>
                    <td className={`px-4 py-3 text-sm font-mono ${colorDias(p.diasInventario)}`}>{numero(p.diasInventario)}</td>
                    <td className="px-4 py-3"><StatusBadge estado={p.estado} /></td>
                    <td className="px-4 py-3 text-xs font-mono text-[#FFD100]">{p.accionRecomendada}</td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => {
                          setModalProducto(p)
                          setMovTipo('ENTRADA')
                          setMovCantidad('1')
                          setMovDescripcion('')
                          setMovError('')
                        }}
                        className="text-xs font-mono text-[#60a5fa] hover:underline cursor-pointer"
                      >
                        Movimiento
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      {modalProducto && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" onClick={() => !guardandoMov && setModalProducto(null)}>
          <div className="w-full max-w-md bg-[#111827] border border-[#1e2d45] rounded-xl p-6" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <div>
                <h3 className="text-sm font-bold text-[#f0f4f8]">Registrar movimiento</h3>
                <p className="text-[11px] font-mono text-[#6b7a99] mt-1">{modalProducto.nombre}</p>
              </div>
              <button
                onClick={() => setModalProducto(null)}
                disabled={guardandoMov}
                className="text-muted hover:text-text text-lg cursor-pointer disabled:opacity-40"
                aria-label="Cerrar"
              >
                ✕
              </button>
            </div>

            <div className="mb-4">
              <p className="text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Tipo</p>
              <div className="grid grid-cols-3 gap-2">
                {(['ENTRADA', 'SALIDA', 'AJUSTE'] as const).map((t) => (
                  <button
                    key={t}
                    onClick={() => setMovTipo(t)}
                    className={`px-3 py-2 rounded-lg text-xs font-mono font-bold border transition-colors cursor-pointer ${
                      movTipo === t
                        ? 'bg-[#FFD100] text-[#090c14] border-[#FFD100]'
                        : 'bg-[#0d1119] border-[#1e2d45] text-[#6b7a99] hover:border-[#FFD10055]'
                    }`}
                  >
                    {t}
                  </button>
                ))}
              </div>
            </div>

            <div className="mb-4">
              <label className="block text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Cantidad</label>
              <input
                type="number"
                min={1}
                value={movCantidad}
                onChange={(e) => setMovCantidad(e.target.value)}
                className="w-full bg-[#0d1119] border border-[#1e2d45] rounded-lg px-3 py-2 text-sm font-mono text-[#f0f4f8] focus:outline-none focus:border-[#FFD10055]"
              />
              <p className="text-[10px] font-mono text-[#3d4f6b] mt-1.5">
                Stock actual: {numero(modalProducto.stockActual)} {modalProducto.unidad}.
                {movTipo === 'SALIDA' && ' La salida valida que no supere el stock disponible.'}
              </p>
            </div>

            <div className="mb-5">
              <label className="block text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Observación</label>
              <input
                type="text"
                value={movDescripcion}
                onChange={(e) => setMovDescripcion(e.target.value)}
                placeholder="Ej. Reposición matutina"
                className="w-full bg-[#0d1119] border border-[#1e2d45] rounded-lg px-3 py-2 text-sm text-[#f0f4f8] placeholder:text-[#3d4f6b] focus:outline-none focus:border-[#FFD10055]"
              />
            </div>

            {movError && (
              <div className="mb-4 rounded-lg bg-[#f8717122] border border-[#f8717140] text-[#f87171] px-3 py-2.5 text-xs font-mono">
                {movError}
              </div>
            )}

            <div className="flex gap-2">
              <button
                onClick={() => setModalProducto(null)}
                disabled={guardandoMov}
                className="flex-1 py-2.5 rounded-lg border border-[#1e2d45] text-xs font-mono text-[#6b7a99] hover:text-[#f0f4f8] transition-colors cursor-pointer disabled:opacity-40"
              >
                Cancelar
              </button>
              <button
                onClick={registrarMovimiento}
                disabled={guardandoMov}
                className="flex-1 py-2.5 rounded-lg bg-[#FFD100] text-[#090c14] text-xs font-bold tracking-wide hover:opacity-90 transition-opacity cursor-pointer disabled:opacity-50"
              >
                {guardandoMov ? 'Registrando…' : 'Registrar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}