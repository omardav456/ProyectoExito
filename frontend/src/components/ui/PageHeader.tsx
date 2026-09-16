interface PageHeaderProps {
  title: string
  subtitle: string
}

export function PageHeader({ title, subtitle }: PageHeaderProps) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
      <div>
        <h1 className="text-2xl font-bold text-[#f0f4f8] tracking-tight">{title}</h1>
        <p className="text-sm font-mono text-[#6b7a99] mt-1">{subtitle}</p>
      </div>
      <span className="self-start text-xs font-mono bg-[#FFD10022] text-[#FFD100] border border-[#FFD10033] px-3 py-1 rounded-full whitespace-nowrap">
        Datos simulados para demostración académica
      </span>
    </div>
  )
}