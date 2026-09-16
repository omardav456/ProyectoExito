const ESTILOS: Record<string, string> = {
  CRITICO: 'bg-[#f8717122] text-[#f87171]',
  AGOTADO: 'bg-[#f8717122] text-[#f87171]',
  BAJO: 'bg-[#fb923c22] text-[#fb923c]',
  SOBRESTOCK: 'bg-[#60a5fa22] text-[#60a5fa]',
  OK: 'bg-[#34d39922] text-[#34d399]',
  ACTIVO: 'bg-[#34d39922] text-[#34d399]',
  PILOTO: 'bg-[#FFD10022] text-[#FFD100]',
  EN_DESARROLLO: 'bg-[#fb923c22] text-[#fb923c]',
  IMPLEMENTADO: 'bg-[#60a5fa22] text-[#60a5fa]',
  VALIDADO: 'bg-[#34d39922] text-[#34d399]',
  PROPUESTO: 'bg-[#60a5fa22] text-[#60a5fa]',
  DEPRECADO: 'bg-[#6b7a9922] text-[#6b7a99]',
}

interface StatusBadgeProps {
  estado: string
}

export function StatusBadge({ estado }: StatusBadgeProps) {
  const estilo = ESTILOS[estado.toUpperCase()] ?? 'bg-[#60a5fa22] text-[#60a5fa]'
  return <span className={`text-xs font-mono font-bold px-2 py-0.5 rounded ${estilo}`}>{estado}</span>
}