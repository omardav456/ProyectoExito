import { useState } from 'react'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '@/api/client'
import type { ResultadoSimulacion } from '@/types'
import { numero } from '@/lib/format'
import { KpiCard } from '@/components/ui/KpiCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { CustomTooltip } from '@/components/ui/CustomTooltip'

const DIAS_SEMANA = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
const VENTAS_DEFAULT = [90, 85, 90, 95, 115, 140, 125]

interface CampoNumeroProps {
  label: string
  value: number
  min: number
  max: number
  step?: number
  onChange: (v: number) => void
}

function CampoNumero({ label, value, min, max, step = 1, onChange }: CampoNumeroProps) {
  return (
    <div>
      <label className="block text-[11px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">{label}</label>
      <input
        type="number"
        value={value}
        min={min}
        max={max}
        step={step}
        onChange={(e) => onChange(Number(e.target.value))}
        className="w-full bg-[#0d1119] border border-[#1e2d45] rounded-lg px-3 py-2 text-sm font-mono text-[#f0f4f8] focus:outline-none focus:border-[#FFD10055] transition-colors"
      />
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="w-full mt-3"
      />
    </div>
  )
}

export default function Simulador() {
  const [inventarioInicial, setInventarioInicial] = useState(400)
  const [reposicion, setReposicion] = useState(120)
  const [ventasSemana, setVentasSemana] = useState<number[]>(VENTAS_DEFAULT)
  const [dias, setDias] = useState(30)
  const [resultado, setResultado] = useState<ResultadoSimulacion | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [ejecutando, setEjecutando] = useState(false)

  const setVenta = (i: number, v: number) => {
    setVentasSemana((prev) => prev.map((x, j) => (j === i ? v : x)))
  }

  const ejecutar = async () => {
    setEjecutando(true)
    setError(null)
    try {
      const res = await api.post<ResultadoSimulacion>('/simulaciones/ejecutar', {
        inventarioInicial,
        reposicion,
        ventasSemana,
        dias,
      })
      setResultado(res)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo ejecutar la simulación.')
    } finally {
      setEjecutando(false)
    }
  }

  return (
    <div>
      <PageHeader title="Simulador" subtitle="Simula el comportamiento del inventario con reposiciones y ventas semanales" />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
        <Panel title="Parámetros de simulación" className="lg:col-span-2">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5 mb-6">
            <CampoNumero label="Inventario inicial" value={inventarioInicial} min={0} max={2000} step={10} onChange={setInventarioInicial} />
            <CampoNumero label="Reposición" value={reposicion} min={0} max={1000} step={10} onChange={setReposicion} />
            <CampoNumero label="Días de simulación" value={dias} min={7} max={90} step={1} onChange={setDias} />
          </div>
          <div>
            <p className="text-[11px] font-mono uppercase tracking-widest text-[#6b7a99] mb-2">Ventas por día de la semana</p>
            <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-7 gap-3">
              {DIAS_SEMANA.map((dia, i) => (
                <div key={dia}>
                  <label className="block text-[10px] font-mono text-[#3d4f6b] mb-1">{dia}</label>
                  <input
                    type="number"
                    value={ventasSemana[i]}
                    min={0}
                    onChange={(e) => setVenta(i, Number(e.target.value))}
                    className="w-full bg-[#0d1119] border border-[#1e2d45] rounded-lg px-2 py-2 text-sm font-mono text-[#f0f4f8] focus:outline-none focus:border-[#FFD10055] transition-colors"
                  />
                </div>
              ))}
            </div>
          </div>
        </Panel>

        <Panel title="Acción">
          <p className="text-xs font-mono text-[#6b7a99] mb-4 leading-relaxed">
            Ejecuta la simulación con los parámetros configurados. El motor replica la dinámica inventario → ventas día a día.
          </p>
          <button
            onClick={ejecutar}
            disabled={ejecutando}
            className="w-full px-4 py-3 rounded-lg bg-[#FFD100] text-[#090c14] border border-[#FFD100] font-bold text-sm font-mono uppercase tracking-widest hover:opacity-90 disabled:opacity-50 transition-opacity cursor-pointer"
          >
            {ejecutando ? 'Ejecutando…' : 'Ejecutar simulación'}
          </button>
          {error && (
            <div className="mt-4 bg-[#f8717122] border border-[#f87171] rounded-lg px-3 py-2.5">
              <p className="text-xs font-mono text-[#f87171]">{error}</p>
            </div>
          )}
        </Panel>
      </div>

      {ejecutando && <LoadingSpinner />}

      {!ejecutando && resultado && (
        <div>
          {resultado.diaAgotamiento !== null && (
            <div className="flex items-start gap-3 bg-[#f8717122] border border-[#f87171] rounded-xl p-4 mb-6">
              <span className="text-xl leading-none text-[#f87171]">⚠</span>
              <div>
                <p className="text-sm font-bold font-mono text-[#f87171]">AGOTAMIENTO EN EL DÍA {numero(resultado.diaAgotamiento)}</p>
                <p className="text-xs font-mono text-[#f87171]/80 mt-1">
                  El inventario se agota antes de finalizar el horizonte de simulación. Incrementa la reposición o reduce las ventas.
                </p>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
            <KpiCard
              label="Inventario final"
              value={numero(resultado.inventarioFinal)}
              unit="u"
              icon="▦"
              color={resultado.balanceOk ? 'bg-[#34d39922] text-[#34d399]' : 'bg-[#f8717122] text-[#f87171]'}
              sub={`Día ${resultado.dias}`}
            />
            <KpiCard label="Inventario mínimo" value={numero(resultado.inventarioMin)} unit="u" icon="▁" color="bg-[#fb923c22] text-[#fb923c]" sub={`Día ${numero(resultado.diaMin)}`} />
            <KpiCard label="Día de agotamiento" value={resultado.diaAgotamiento !== null ? `Día ${numero(resultado.diaAgotamiento)}` : '—'} icon="⏱" color="bg-[#f8717122] text-[#f87171]" sub="Si el stock llega a cero" />
            <KpiCard
              label="Balance"
              value={resultado.balanceOk ? 'OK' : 'DÉFICIT'}
              icon="✓"
              color={resultado.balanceOk ? 'bg-[#34d39922] text-[#34d399]' : 'bg-[#f8717122] text-[#f87171]'}
              sub={`${numero(resultado.ventasTotales)} ventas · ${numero(resultado.reposicionTotal)} reposiciones`}
            />
          </div>

          <Panel title="Inventario durante la simulación">
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={resultado.filas} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
                  <defs>
                    <linearGradient id="gradSimulacion" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#FFD100" stopOpacity={0.35} />
                      <stop offset="95%" stopColor="#FFD100" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e2d45" vertical={false} />
                  <XAxis dataKey="dia" stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                  <YAxis stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
                  <Tooltip content={<CustomTooltip />} />
                  <Area type="monotone" dataKey="inventarioFinal" name="Inventario final" stroke="#FFD100" strokeWidth={2.5} fill="url(#gradSimulacion)" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </Panel>
        </div>
      )}

      {!ejecutando && !resultado && (
        <Panel>
          <p className="text-sm font-mono text-[#6b7a99] text-center py-12">Configura los parámetros y ejecuta la simulación para ver los resultados.</p>
        </Panel>
      )}
    </div>
  )
}