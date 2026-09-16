interface TooltipEntry {
  name?: string | number
  value?: number | string
  color?: string
  unit?: string
}

interface CustomTooltipProps {
  active?: boolean
  payload?: TooltipEntry[]
  label?: string | number
}

export function CustomTooltip({ active, payload, label }: CustomTooltipProps) {
  if (!active || !payload || payload.length === 0) return null
  return (
    <div className="bg-[#111827] border border-[#1e2d45] rounded-lg px-3 py-2 text-xs font-mono shadow-xl">
      {label !== undefined && label !== '' && (
        <p className="text-[#6b7a99] uppercase tracking-widest mb-1.5">{String(label)}</p>
      )}
      <div className="space-y-1">
        {payload.map((entry, i) => (
          <div key={i} className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full shrink-0" style={{ backgroundColor: entry.color }} />
            <span className="text-[#6b7a99]">{String(entry.name)}</span>
            <span className="font-bold text-[#f0f4f8]">{String(entry.value)}{entry.unit ? ` ${entry.unit}` : ''}</span>
          </div>
        ))}
      </div>
    </div>
  )
}