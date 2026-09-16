import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import { useAuth } from '@/auth/AuthContext'
import type { Compra, CompraItemRequest, Producto } from '@/types'
import { moneda, numero, trunca } from '@/lib/format'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { EmptyState } from '@/components/ui/EmptyState'

type Carrito = Record<number, number>

export default function Tienda() {
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const [carrito, setCarrito] = useState<Carrito>({})
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const [ultimaCompra, setUltimaCompra] = useState<Compra | null>(null)

  const { data: productos, loading, error: errorProd, refetch } = useFetch<Producto[]>(
    () => api.get('/productos?activo=true'),
    [],
  )

  const agregar = (producto: Producto, delta: number) => {
    setError('')
    setCarrito((prev) => {
      const actual = prev[producto.id] ?? 0
      const siguiente = Math.min(Math.max(1, actual + delta), producto.stockActual)
      const copia = { ...prev }
      if (siguiente === 0) delete copia[producto.id]
      else copia[producto.id] = siguiente
      return copia
    })
  }

  const quitar = (id: number) => {
    setCarrito((prev) => {
      const copia = { ...prev }
      delete copia[id]
      return copia
    })
  }

  const itemIds = Object.keys(carrito).map(Number)
  const itemsDetalle = (productos ?? [])
    .filter((p) => itemIds.includes(p.id))
    .map((p) => ({ producto: p, cantidad: carrito[p.id] }))
  const total = itemsDetalle.reduce((acc, i) => acc + i.producto.precio * i.cantidad, 0)

  const comprar = async () => {
    if (!usuario) {
      navigate('/login')
      return
    }
    setError('')
    setEnviando(true)
    setUltimaCompra(null)
    try {
      const request: CompraItemRequest[] = itemsDetalle.map((i) => ({ productoId: i.producto.id, cantidad: i.cantidad }))
      const compra = await api.post<Compra>('/compras', { items: request })
      setUltimaCompra(compra)
      setCarrito({})
      refetch()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo completar la compra')
    } finally {
      setEnviando(false)
    }
  }

  if (loading) return <LoadingSpinner />
  if (errorProd || !productos) {
    return <ErrorState message={errorProd ?? 'No se pudieron cargar los productos.'} onRetry={refetch} />
  }

  return (
    <div>
      {!usuario && (
        <div className="mb-6 rounded-xl border border-primary/40 bg-primary-dim px-4 py-3 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <span className="text-primary text-lg leading-none">◫</span>
            <div>
              <p className="text-sm font-bold text-text">Explorando como invitado</p>
              <p className="text-xs font-mono text-muted mt-0.5">El catálogo es público — inicia sesión para completar una compra.</p>
            </div>
          </div>
          <button
            onClick={() => navigate('/login')}
            className="shrink-0 px-4 py-2 rounded-lg bg-primary text-bg font-bold text-xs font-mono hover:opacity-90 transition-opacity cursor-pointer"
          >
            Iniciar sesión
          </button>
        </div>
      )}
      <PageHeader title="Tienda" subtitle="Carrito de compras simulado — cada venta descuenta inventario y queda auditada" />

      {ultimaCompra && (
        <div className="mb-6 rounded-xl border border-green/40 bg-green-dim px-4 py-3 flex items-start gap-3">
          <span className="text-green text-lg leading-none mt-0.5">✓</span>
          <div>
            <p className="text-sm font-bold text-green">Compra #{ultimaCompra.id} realizada</p>
            <p className="text-xs font-mono text-muted mt-0.5">
              {ultimaCompra.items.map((it) => `${it.cantidad} × ${it.productoNombre}`).join(' · ')} — total {moneda(ultimaCompra.total)}. El stock se actualizó automáticamente.
            </p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2">
          {productos.length === 0 ? (
            <Panel>
              <EmptyState message="No hay productos disponibles" />
            </Panel>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
              {productos.map((p) => {
                const enCarrito = carrito[p.id] ?? 0
                const agotado = p.stockActual <= 0
                const maximo = enCarrito >= p.stockActual
                return (
                  <div key={p.id} className="bg-[#111827] border border-[#1e2d45] rounded-xl overflow-hidden flex flex-col">
                    <div
                      className="h-20 flex items-center justify-center relative"
                      style={{
                        background: `linear-gradient(135deg, ${p.categoriaColor}22, ${p.categoriaColor}08)`,
                        borderBottom: '1px solid #1e2d45',
                      }}
                    >
                      <span
                        className="w-10 h-10 rounded-lg flex items-center justify-center text-lg font-black font-mono"
                        style={{ backgroundColor: `${p.categoriaColor}22`, color: p.categoriaColor }}
                      >
                        {p.nombre.charAt(0).toUpperCase()}
                      </span>
                      {agotado && (
                        <span className="absolute top-2 right-2 text-[10px] font-mono font-bold bg-[#f8717122] text-[#f87171] border border-[#f8717133] px-2 py-0.5 rounded">
                          AGOTADO
                        </span>
                      )}
                    </div>
                    <div className="p-4 flex-1 flex flex-col">
                      <div className="flex items-center justify-between mb-1.5">
                        <span className="inline-flex items-center gap-1.5 text-[10px] font-mono text-muted">
                          <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: p.categoriaColor }} />
                          {p.categoriaNombre}
                        </span>
                        <StatusBadge estado={p.estado} />
                      </div>
                      <h3 className="font-semibold text-sm text-text mb-1">{p.nombre}</h3>
                      <p className="text-xs text-muted leading-relaxed mb-3 flex-1">
                        {trunca(p.descripcion ?? 'Sin descripción', 90)}
                      </p>
                      <div className="flex items-end justify-between mb-3">
                        <p className="text-lg font-bold text-primary">{moneda(p.precio)}</p>
                        <p className="text-[11px] font-mono text-muted2">
                          {agotado ? 'Sin stock' : `${numero(p.stockActual)} ${p.unidad}`}
                        </p>
                      </div>
                      {agotado ? (
                        <p className="text-center text-[11px] font-mono text-[#f87171] border border-[#f8717133] rounded-lg py-2">
                          Pedido urgente requerido
                        </p>
                      ) : enCarrito > 0 ? (
                        <div className="flex items-center justify-between rounded-lg border border-border bg-[#0d1119] px-2 py-1.5">
                          <button
                            onClick={() => agregar(p, -1)}
                            className="w-8 h-8 rounded-md text-muted hover:text-text hover:bg-surface2 transition-colors cursor-pointer"
                            aria-label="Disminuir"
                          >
                            −
                          </button>
                          <span className="text-sm font-mono font-bold">{numero(enCarrito)}</span>
                          <button
                            onClick={() => agregar(p, maximo ? Math.min(0, p.stockActual - enCarrito) : 1)}
                            disabled={maximo}
                            className="w-8 h-8 rounded-md text-muted hover:text-text hover:bg-surface2 transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                            aria-label="Aumentar"
                          >
                            +
                          </button>
                        </div>
                      ) : (
                        <button
                          onClick={() => agregar(p, 1)}
                          disabled={maximo}
                          className="w-full py-2 rounded-lg text-xs font-mono font-bold bg-primary text-bg hover:opacity-90 transition-opacity cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                        >
                          Agregar al carrito
                        </button>
                      )}
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        <Panel title="Carrito" className="h-fit lg:sticky lg:top-0">
          {error && (
            <div className="mb-4 rounded-lg bg-red-dim border border-red/40 text-red px-3 py-2.5 text-xs font-mono">{error}</div>
          )}
          {itemIds.length === 0 ? (
            <EmptyState message="El carrito está vacío" />
          ) : (
            <>
              <ul className="space-y-3 mb-5">
                {itemsDetalle.map(({ producto, cantidad }) => (
                  <li key={producto.id} className="flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm text-text truncate">{producto.nombre}</p>
                      <p className="text-[11px] font-mono text-muted2 mt-0.5">
                        {cantidad} × {moneda(producto.precio)}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        onClick={() => agregar(producto, -1)}
                        className="w-7 h-7 rounded-md text-muted hover:text-text hover:bg-surface2 transition-colors cursor-pointer"
                        aria-label="Disminuir"
                      >
                        −
                      </button>
                      <span className="text-xs font-mono font-bold w-6 text-center">{numero(cantidad)}</span>
                      <button
                        onClick={() => agregar(producto, 1)}
                        disabled={cantidad >= producto.stockActual}
                        className="w-7 h-7 rounded-md text-muted hover:text-text hover:bg-surface2 transition-colors cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                        aria-label="Aumentar"
                      >
                        +
                      </button>
                      <button
                        onClick={() => quitar(producto.id)}
                        className="w-7 h-7 rounded-md text-muted hover:text-[#f87171] hover:bg-red-dim transition-colors cursor-pointer"
                        aria-label="Quitar"
                      >
                        ✕
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
              <div className="border-t border-border pt-4 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono uppercase tracking-widest text-muted">Total</span>
                  <span className="text-xl font-bold text-primary">{moneda(total)}</span>
                </div>
                <button
                  onClick={comprar}
                  disabled={enviando || itemIds.length === 0}
                  className="w-full py-3 rounded-lg bg-primary text-bg font-bold text-sm tracking-wide hover:opacity-90 transition-opacity cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {enviando ? 'Procesando…' : !usuario ? 'Iniciar sesión para comprar' : 'Comprar'}
                </button>
                <p className="text-[10px] font-mono text-muted2 leading-relaxed">
                  La compra descontará el stock de cada producto (verificado en el servidor) y quedará registrada en la
                  auditoría como COMPRA_SIMULADA.
                </p>
              </div>
            </>
          )}
        </Panel>
      </div>
    </div>
  )
}