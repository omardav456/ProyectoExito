import { useEffect, useMemo, useState } from 'react'
import { api, ApiError } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { EjecutarModelo, ModeloDetail, ModeloListResponse, ModeloResumen, Producto } from '@/types'
import { numero } from '@/lib/format'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { EmptyState } from '@/components/ui/EmptyState'
import { CustomTooltip } from '@/components/ui/CustomTooltip'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

const COLORES = ['#FFD100', '#60a5fa', '#34d399', '#f472b6', '#fb923c']

interface FilaAbc {
  nombre: string
  costo: number
  demanda: number
}

interface CampoGenerico {
  key: string
  label: string
  simbolo: string
  tipo: string
  porDefecto: string
  descripcion: string | null
}

const PILOTOS_PROP = ['EOQ', 'ABC', 'STOCKFLOW_UHT']

function claveMotor(simbolo: string): string {
  const norm = simbolo.replace(/\|/g, '_')
  if (norm === 'y_t') return 'serie'
  return norm
}

function parsearArray(raw: string): unknown[] {
  const t = raw.trim()
  if (t.startsWith('[')) {
    try {
      const arr = JSON.parse(t)
      if (Array.isArray(arr)) return arr.map(Number)
    } catch {
      /* seguir con CSV */
    }
  }
  if (!t) return []
  return t.split(/[\s,;]+/).filter(Boolean).map(Number)
}

export default function EjecutarModelo() {
  const [modeloId, setModeloId] = useState('')
  const [fuente, setFuente] = useState<'SIMULADA' | 'REAL'>('SIMULADA')
  const [resultado, setResultado] = useState<EjecutarModelo | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')

  const [eoq, setEoq] = useState({ demandaAnual: '1200', costoPedido: '15000', costoAlmacenamiento: '2500' })
  const [abc, setAbc] = useState<FilaAbc[]>([])
  const [sf, setSf] = useState({ inventarioInicial: '100', reposicion: '90', dias: '14' })
  const [ventasSemana, setVentasSemana] = useState(['8', '12', '15', '10', '14', '18', '9'])
  const [gen, setGen] = useState<Record<string, string>>({})

  const { data: pilotoLista, error: errorPiloto } = useFetch<ModeloListResponse>(
    () => api.get('/modelos?estado=PILOTO&size=200'),
    [],
  )
  const { data: implLista, error: errorImpl } = useFetch<ModeloListResponse>(
    () => api.get('/modelos?estado=IMPLEMENTADO&size=200'),
    [],
  )
  const { data: modelo, loading: cargandoModelo } = useFetch<ModeloDetail | null>(
    () => (modeloId ? api.get(`/modelos/${modeloId}`) : Promise.resolve(null)),
    [modeloId],
  )
  const { data: productos } = useFetch<Producto[]>(() => api.get('/productos?activo=true'), [])
  const { data: historial, refetch: refetchHistorial } = useFetch<EjecutarModelo[]>(
    () => api.get('/modelos/ejecuciones'),
    [],
  )

  const ejecutable = useMemo(() => {
    const porId = new Map<number, ModeloResumen>()
    for (const m of [...(pilotoLista?.modelos ?? []), ...(implLista?.modelos ?? [])]) {
      porId.set(m.id, m)
    }
    return [...porId.values()].sort((a, b) => a.codigo.localeCompare(b.codigo))
  }, [pilotoLista, implLista])

  const motor = modelo?.motorImpl

  const campos = useMemo(() => {
    if (!modelo || (motor && PILOTOS_PROP.includes(motor))) return []
    const out: CampoGenerico[] = []
    for (const p of modelo.parametros ?? []) {
      out.push({ key: claveMotor(p.simbolo), label: p.nombre, simbolo: p.simbolo, tipo: 'float', porDefecto: p.valorPorDefecto ?? '', descripcion: p.descripcion })
    }
    for (const v of modelo.variables ?? []) {
      if (v.rol !== 'ENTRADA') continue
      const k = claveMotor(v.simbolo)
      if (out.some((o) => o.key === k)) continue
      const esArray = (v.tipoDato ?? '').toUpperCase() === 'ARRAY'
      out.push({ key: k, label: v.nombre, simbolo: v.simbolo, tipo: esArray ? 'array' : v.tipoDato ?? 'float', porDefecto: '', descripcion: v.descripcion })
    }
    return out
  }, [modelo, motor])

  useEffect(() => {
    if (campos.length === 0) {
      setGen({})
      return
    }
    setGen(Object.fromEntries(campos.map((c) => [c.key, c.porDefecto])))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [modeloId, motor])

  useEffect(() => {
    if (motor === 'ABC' && abc.length === 0 && productos && productos.length > 0) {
      setAbc(productos.slice(0, 8).map((p) => ({ nombre: p.nombre, costo: p.precio, demanda: p.tasaReposicion })))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [motor, productos])

  const alCambiarModelo = (v: string) => {
    setModeloId(v)
    setResultado(null)
    setError('')
    setAbc([])
  }

  const ejecutar = async () => {
    setError('')
    setEnviando(true)
    setResultado(null)
    try {
      let parametros: Record<string, unknown> = {}
      if (motor === 'EOQ') {
        parametros = {
          demandaAnual: Number(eoq.demandaAnual),
          costoPedido: Number(eoq.costoPedido),
          costoAlmacenamiento: Number(eoq.costoAlmacenamiento),
        }
      } else if (motor === 'ABC') {
        parametros = {
          items: abc
            .filter((f) => f.nombre.trim())
            .map((f) => ({ nombre: f.nombre.trim(), costo: Number(f.costo), demanda: Number(f.demanda) })),
        }
      } else if (motor === 'STOCKFLOW_UHT') {
        parametros = {
          inventarioInicial: Number(sf.inventarioInicial),
          reposicion: Number(sf.reposicion),
          dias: Number(sf.dias),
          ...(fuente === 'SIMULADA' ? { ventasSemana: ventasSemana.map(Number) } : {}),
        }
      } else {
        parametros = {}
        for (const c of campos) {
          const raw = gen[c.key] ?? ''
          if (c.tipo === 'array') {
            const arr = parsearArray(raw)
            if (arr.length > 0) parametros[c.key] = arr
          } else if (raw.trim() !== '') {
            parametros[c.key] = Number(raw)
          }
        }
      }
      const res = await api.post<EjecutarModelo>(`/modelos/${modeloId}/ejecutar`, { parametros, fuente })
      setResultado(res)
      refetchHistorial()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo ejecutar el modelo')
    } finally {
      setEnviando(false)
    }
  }

  const inputClase =
    'w-full bg-[#0d1119] border border-[#1e2d45] rounded-lg px-3 py-2 text-xs font-mono text-[#f0f4f8] placeholder:text-[#3d4f6b] focus:outline-none focus:border-[#FFD10055] transition-colors'

  const selectClase = `${inputClase} cursor-pointer appearance-none [&>option]:bg-[#111827]`

  const clavesSerie = useMemo(() => {
    const filas = resultado?.serie ?? []
    if (filas.length === 0) return { x: 'dia', numericas: [] as string[] }
    const keys = Object.keys(filas[0])
    const numericas = keys.filter((k) => typeof filas[0][k] === 'number')
    const x = keys.find((k) => !numericas.includes(k)) ?? numericas[0] ?? 'dia'
    return { x, numericas }
  }, [resultado])

  const resultadoEntradas = useMemo(() => {
    if (!resultado) return []
    return Object.entries(resultado.resultado).filter(([, v]) => v === null || v === undefined || typeof v !== 'object')
  }, [resultado])

  if (errorPiloto || errorImpl) {
    return <ErrorState message={errorPiloto ?? errorImpl ?? ''} onRetry={() => window.location.reload()} />
  }

  return (
    <div>
      <PageHeader title="Ejecutar modelo" subtitle="Motor de cálculo matemático aplicado a los modelos PILOTO e IMPLEMENTADOS del catálogo" />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="space-y-4">
          <Panel title="Modelo y fuente">
            <div className="space-y-4">
              <div>
                <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Modelo ejecutable</label>
                <select value={modeloId} onChange={(e) => alCambiarModelo(e.target.value)} className={selectClase}>
                  <option value="">Selecciona un modelo…</option>
                  {ejecutable.map((m) => (
                    <option key={m.id} value={m.id}>{m.codigo} — {m.nombre}</option>
                  ))}
                </select>
                {ejecutable.length === 0 && (
                  <p className="text-[10px] font-mono text-muted2 mt-1.5">No hay modelos PILOTO o IMPLEMENTADOS disponibles.</p>
                )}
              </div>
              {modelo && (
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <StatusBadge estado={modelo.estado} />
                    <span className="text-[11px] font-mono text-muted">{modelo.codigo}</span>
                  </div>
                  <p className="text-xs text-muted leading-relaxed">{modelo.descripcion}</p>
                  <p className="text-[10px] font-mono text-muted2 leading-relaxed">
                    Motor: {modelo.motorImpl ?? '—'} · Parámetros en blanco usan valores por defecto del motor.
                  </p>
                </div>
              )}
              <div>
                <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Fuente de datos</label>
                <select value={fuente} onChange={(e) => setFuente(e.target.value as 'SIMULADA' | 'REAL')} className={selectClase}>
                  <option value="SIMULADA">SIMULADA</option>
                  <option value="REAL">REAL (movimientos del sistema)</option>
                </select>
                <p className="text-[10px] font-mono text-muted2 mt-1.5 leading-relaxed">
                  En fuente REAL, Stock &amp; Flow usa la demanda de los últimos 14 días y los motores de pronóstico inyectan la serie real de ventas.
                </p>
              </div>
            </div>
          </Panel>

          {cargandoModelo && <LoadingSpinner />}

          {motor === 'EOQ' && (
            <Panel title="Parámetros EOQ">
              <div className="space-y-4">
                {[
                  { k: 'demandaAnual', label: 'Demanda anual (unidades)', v: eoq.demandaAnual, set: (v: string) => setEoq({ ...eoq, demandaAnual: v }) },
                  { k: 'costoPedido', label: 'Costo por pedido (COP)', v: eoq.costoPedido, set: (v: string) => setEoq({ ...eoq, costoPedido: v }) },
                  { k: 'costoAlmacenamiento', label: 'Costo almacenamiento / unidad / año', v: eoq.costoAlmacenamiento, set: (v: string) => setEoq({ ...eoq, costoAlmacenamiento: v }) },
                ].map((campo) => (
                  <div key={campo.k}>
                    <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">{campo.label}</label>
                    <input type="number" value={campo.v} onChange={(e) => campo.set(e.target.value)} className={inputClase} />
                  </div>
                ))}
              </div>
            </Panel>
          )}

          {motor === 'ABC' && (
            <Panel title="Artículos ABC">
              <div className="space-y-3">
                {abc.map((f, i) => (
                  <div key={i} className="grid grid-cols-[1fr_64px_64px_28px] gap-2 items-center">
                    <input
                      value={f.nombre}
                      onChange={(e) => setAbc((prev) => prev.map((x, j) => (j === i ? { ...x, nombre: e.target.value } : x)))}
                      placeholder="Nombre"
                      className={inputClase}
                    />
                    <input
                      type="number"
                      value={f.costo}
                      onChange={(e) => setAbc((prev) => prev.map((x, j) => (j === i ? { ...x, costo: Number(e.target.value) } : x)))}
                      className={inputClase}
                    />
                    <input
                      type="number"
                      value={f.demanda}
                      onChange={(e) => setAbc((prev) => prev.map((x, j) => (j === i ? { ...x, demanda: Number(e.target.value) } : x)))}
                      className={inputClase}
                    />
                    <button
                      onClick={() => setAbc((prev) => prev.filter((_, j) => j !== i))}
                      className="w-7 h-7 rounded-md text-muted hover:text-[#f87171] hover:bg-red-dim transition-colors cursor-pointer"
                      aria-label="Quitar"
                    >
                      ✕
                    </button>
                  </div>
                ))}
                <button
                  onClick={() => setAbc((prev) => [...prev, { nombre: '', costo: 0, demanda: 1 }])}
                  className="w-full py-2 rounded-lg text-xs font-mono font-bold bg-[#111827] border border-[#1e2d45] text-[#6b7a99] hover:border-[#FFD10055] hover:text-[#FFD100] transition-colors cursor-pointer"
                >
                  + Añadir artículo
                </button>
                <p className="text-[10px] font-mono text-muted2 leading-relaxed">
                  Valor = costo × demanda anual. Prellenado desde el inventario actual.
                </p>
              </div>
            </Panel>
          )}

          {motor === 'STOCKFLOW_UHT' && (
            <Panel title="Parámetros Stock & Flow">
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Inventario inicial</label>
                  <input type="number" value={sf.inventarioInicial} onChange={(e) => setSf({ ...sf, inventarioInicial: e.target.value })} className={inputClase} />
                </div>
                <div>
                  <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Reposición</label>
                  <input type="number" value={sf.reposicion} onChange={(e) => setSf({ ...sf, reposicion: e.target.value })} className={inputClase} />
                </div>
                <div>
                  <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Días</label>
                  <input type="number" value={sf.dias} onChange={(e) => setSf({ ...sf, dias: e.target.value })} className={inputClase} />
                </div>
              </div>
              {fuente === 'SIMULADA' && (
                <div className="mt-4">
                  <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">Ventas por día (semana)</label>
                  <div className="grid grid-cols-7 gap-2">
                    {ventasSemana.map((v, i) => (
                      <input
                        key={i}
                        type="number"
                        value={v}
                        onChange={(e) => setVentasSemana((prev) => prev.map((x, j) => (j === i ? e.target.value : x)))}
                        className={inputClase}
                      />
                    ))}
                  </div>
                </div>
              )}
            </Panel>
          )}

          {campos.length > 0 && (
            <Panel title={`Parámetros · ${motor ?? ''}`}>
              <div className="space-y-4">
                {campos.map((c) => (
                  <div key={c.key}>
                    <label className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">
                      {c.label} <span className="text-muted2">({c.simbolo})</span>
                    </label>
                    {c.tipo === 'array' ? (
                      <>
                        <textarea
                          rows={2}
                          value={gen[c.key] ?? ''}
                          onChange={(e) => setGen((prev) => ({ ...prev, [c.key]: e.target.value }))}
                          placeholder="Valores separados por coma, espacio o salto de línea (o JSON [a,b,c])"
                          className={`${inputClase} resize-none`}
                        />
                        <p className="text-[10px] font-mono text-muted2 mt-1 leading-relaxed">Serie/arreglo separado por comas.</p>
                      </>
                    ) : (
                      <input
                        type="number"
                        step="any"
                        value={gen[c.key] ?? ''}
                        placeholder="vacío = valor por defecto"
                        onChange={(e) => setGen((prev) => ({ ...prev, [c.key]: e.target.value }))}
                        className={inputClase}
                      />
                    )}
                  </div>
                ))}
              </div>
            </Panel>
          )}

          {modeloId && motor && (
            <>
              {error && <div className="bg-red-dim border border-red/40 text-red px-3 py-2.5 text-xs font-mono rounded-lg">{error}</div>}
              <button
                onClick={ejecutar}
                disabled={enviando}
                className="w-full py-3 rounded-lg bg-primary text-bg font-bold text-sm tracking-wide hover:opacity-90 transition-opacity cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {enviando ? 'Ejecutando…' : 'Ejecutar modelo'}
              </button>
            </>
          )}
        </div>

        <div className="lg:col-span-2 space-y-4">
          {resultado ? (
            <>
              <Panel title={`Resultado — ${resultado.modeloCodigo} · ${resultado.motor}`}>
                <div className="flex flex-wrap items-center gap-3 mb-4">
                  <StatusBadge estado={resultado.estado} />
                  <span className="text-[11px] font-mono text-muted">Fuente: {resultado.fuente}</span>
                  <span className="text-[11px] font-mono text-muted2">
                    {new Date(resultado.creadaEn).toLocaleString('es-CO')}
                  </span>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 mb-5">
                  {resultadoEntradas.map(([k, v]) => (
                    <div key={k} className="rounded-lg bg-[#0d1119] border border-border px-3 py-2.5">
                      <p className="text-[10px] font-mono uppercase tracking-widest text-muted2 mb-1 truncate">{k}</p>
                      <p className="text-sm font-mono font-bold text-[#f0f4f8]">
                        {typeof v === 'number' ? (Number.isInteger(v) ? numero(v) : v.toLocaleString('es-CO')) : String(v ?? '—')}
                      </p>
                    </div>
                  ))}
                </div>
                {resultado.interpretacion && (
                  <div className="rounded-lg bg-[#FFD10011] border border-[#FFD10033] px-4 py-3">
                    <p className="text-[10px] font-mono uppercase tracking-widest text-[#FFD100] mb-1.5">Interpretación</p>
                    <p className="text-sm text-[#f0f4f8] leading-relaxed">{resultado.interpretacion}</p>
                  </div>
                )}
              </Panel>

              {resultado.serie.length > 0 && (
                <Panel title="Serie calculada">
                  <div className="h-[260px]">
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={resultado.serie} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#1e2d45" vertical={false} />
                        <XAxis dataKey={clavesSerie.x} stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                        <YAxis stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                        <Tooltip content={<CustomTooltip />} />
                        {clavesSerie.numericas.map((k, i) => (
                          <Line key={k} type="monotone" dataKey={k} name={k} stroke={COLORES[i % COLORES.length]} strokeWidth={2} dot={false} />
                        ))}
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </Panel>
              )}
            </>
          ) : (
            <Panel>
              <EmptyState message="Ejecuta un modelo PILOTO o IMPLEMENTADO para ver su resultado, serie e interpretación" />
            </Panel>
          )}

          <Panel title="Historial de ejecuciones">
            {!historial || historial.length === 0 ? (
              <EmptyState message="Aún no hay ejecuciones registradas" />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Modelo</th>
                      <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Motor</th>
                      <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Parámetros</th>
                      <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Estado</th>
                      <th className="px-4 py-3 text-[10px] font-mono uppercase tracking-widest text-muted">Fecha</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60">
                    {historial.map((h) => (
                      <tr key={h.id} className="hover:bg-[#0d1119] transition-colors">
                        <td className="px-4 py-3 text-xs font-mono text-text">{h.modeloCodigo}</td>
                        <td className="px-4 py-3 text-xs font-mono text-muted">{h.motor}</td>
                        <td className="px-4 py-3 text-xs font-mono text-muted2 max-w-[220px] truncate">
                          {Object.entries(h.parametros).map(([k, v]) => `${k}=${Array.isArray(v) ? `[${v.length}]` : v}`).join(' · ')}
                        </td>
                        <td className="px-4 py-3"><StatusBadge estado={h.estado} /></td>
                        <td className="px-4 py-3 text-xs font-mono text-muted whitespace-nowrap">
                          {new Date(h.creadaEn).toLocaleString('es-CO')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Panel>
        </div>
      </div>
    </div>
  )
}