import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend } from 'recharts'
import { api } from '@/api/client'
import { useFetch } from '@/hooks/useFetch'
import type { Demanda } from '@/types'
import { numero } from '@/lib/format'
import { KpiCard } from '@/components/ui/KpiCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { Panel } from '@/components/ui/Panel'
import { LoadingSpinner } from '@/components/ui/Spinner'
import { ErrorState } from '@/components/ui/ErrorState'
import { CustomTooltip } from '@/components/ui/CustomTooltip'

const HOY = new Date()
const DESDE = new Date(HOY)
DESDE.setDate(HOY.getDate() - 14)
const HASTA = new Date(HOY)
HASTA.setDate(HOY.getDate() + 7)
const ISO = (d: Date) => d.toISOString().slice(0, 10)
const Q_REAL = `/demanda?tipo=REAL&desde=${ISO(DESDE)}&hasta=${ISO(HASTA)}`
const Q_PREVISTA = `/demanda?tipo=PREVISTA&desde=${ISO(HOY)}&hasta=${ISO(HASTA)}`

const DIAS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb']

function agregarPorDia(lista: Demanda[]): number[] {
  const suma = [0, 0, 0, 0, 0, 0, 0]
  const conteo = [0, 0, 0, 0, 0, 0, 0]
  for (const d of lista) {
    const numeroDia = new Date(d.fecha).getDay()
    suma[numeroDia] += d.unidades
    conteo[numeroDia] += 1
  }
  return suma.map((s, i) => (conteo[i] ? Math.round(s / conteo[i]) : 0))
}

export default function Prediccion() {
  const { data: real, loading: cargandoReal, error: errorReal, refetch: refetchReal } = useFetch<Demanda[]>(
    () => api.get<Demanda[]>(Q_REAL),
    [],
  )
  const { data: prevista, loading: cargandoPrevista, error: errorPrevista, refetch: refetchPrevista } = useFetch<Demanda[]>(
    () => api.get<Demanda[]>(Q_PREVISTA),
    [],
  )

  const loading = cargandoReal || cargandoPrevista
  const error = errorReal ?? errorPrevista

  if (loading) return <LoadingSpinner />
  if (error || !real || !prevista) {
    return (
      <ErrorState
        message={error ?? 'No se pudieron cargar los datos de demanda.'}
        onRetry={() => {
          refetchReal()
          refetchPrevista()
        }}
      />
    )
  }

  const realDias = agregarPorDia(real)
  const prevDias = agregarPorDia(prevista)
  const chartData = DIAS.map((dia, i) => ({ dia, real: realDias[i], prevista: prevDias[i] }))

  const todas = [...real, ...prevista].map((d) => d.unidades)
  const pico = todas.length ? Math.max(...todas) : 0
  const valle = todas.length ? Math.min(...todas) : 0
  const promedio = real.length ? Math.round(real.reduce((acc, d) => acc + d.unidades, 0) / real.length) : 0
  const picoDia = DIAS[realDias.indexOf(Math.max(...realDias))] ?? '—'
  const valleDia = DIAS[realDias.indexOf(Math.min(...realDias))] ?? '—'
  const tendencia = promedio > 0 ? Math.round(((pico - promedio) / promedio) * 100) : 0

  return (
    <div>
      <PageHeader title="Predicción de demanda" subtitle="Demanda real (14 días) vs pronóstico (próximos 7 días)" />

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        <KpiCard label="Pico de demanda" value={numero(pico)} unit="u" icon="▲" color="bg-[#f8717122] text-[#f87171]" sub={`Máximo el ${picoDia}`} />
        <KpiCard label="Valle de demanda" value={numero(valle)} unit="u" icon="▼" color="bg-[#60a5fa22] text-[#60a5fa]" sub={`Mínimo el ${valleDia}`} />
        <KpiCard label="Promedio semanal" value={numero(promedio)} unit="u/día" icon="∅" color="bg-[#FFD10022] text-[#FFD100]" sub={`Variación pico ${tendencia}%`} />
      </div>

      <Panel title="Demanda por día de la semana — real vs pronóstico">
        <div className="h-[300px]">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1e2d45" vertical={false} />
              <XAxis dataKey="dia" stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
              <YAxis stroke="#3d4f6b" fontSize={11} tickLine={false} axisLine={false} />
              <Tooltip content={<CustomTooltip />} />
              <Legend wrapperStyle={{ fontSize: 12, fontFamily: 'JetBrains Mono, monospace', color: '#6b7a99' }} />
              <Bar dataKey="real" name="Real" fill="#FFD100" radius={[4, 4, 0, 0]} barSize={18} />
              <Bar dataKey="prevista" name="Pronóstico" fill="#60a5fa" radius={[4, 4, 0, 0]} barSize={18} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Panel>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
        <div className="bg-[#111827] border border-[#1e2d45] border-l-[#f87171] rounded-xl p-4">
          <p className="text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Pico de demanda</p>
          <p className="text-xs font-mono text-[#f0f4f8] leading-relaxed">
            La mayor demanda promedio se registra el <span className="text-[#f87171] font-bold">{picoDia}</span>. Asegura
            reposiciones previas para cubrir este pico.
          </p>
        </div>
        <div className="bg-[#111827] border border-[#1e2d45] border-l-[#60a5fa] rounded-xl p-4">
          <p className="text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Valle de demanda</p>
          <p className="text-xs font-mono text-[#f0f4f8] leading-relaxed">
            El menor consumo se da el <span className="text-[#60a5fa] font-bold">{valleDia}</span>. Es el momento ideal para
            programar inventarios y despachos.
          </p>
        </div>
        <div className="bg-[#111827] border border-[#1e2d45] border-l-[#34d399] rounded-xl p-4">
          <p className="text-[10px] font-mono uppercase tracking-widest text-[#6b7a99] mb-1.5">Pronóstico vs real</p>
          <p className="text-xs font-mono text-[#f0f4f8] leading-relaxed">
            El pronóstico diario promedio es de <span className="text-[#34d399] font-bold">{numero(prevDias.reduce((a, b) => a + b, 0) / Math.max(prevDias.filter(v => v > 0).length, 1))}</span>{" "}
            unidades, alineado con la demanda real observada.
          </p>
        </div>
      </div>
    </div>
  )
}