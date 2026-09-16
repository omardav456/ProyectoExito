interface KpiCardProps {
  label: string
  value: string | number
  unit?: string
  icon: string
  color: string
  sub?: string
}

export function KpiCard({ label, value, unit, icon, color, sub }: KpiCardProps) {
  return (
    <div className="bg-[#111827] border border-[#1e2d45] rounded-xl p-5">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-xs font-mono uppercase tracking-widest text-[#6b7a99]">{label}</h3>
        <span className={`w-9 h-9 rounded-lg flex items-center justify-center text-sm ${color}`}>{icon}</span>
      </div>
      <p className="text-3xl font-bold text-[#f0f4f8] font-mono leading-none">
        {value}
        {unit && <span className="text-sm text-[#6b7a99] ml-1.5 font-normal">{unit}</span>}
      </p>
      {sub && <p className="text-[11px] font-mono text-[#6b7a99] mt-2.5 leading-snug">{sub}</p>}
    </div>
  )
}