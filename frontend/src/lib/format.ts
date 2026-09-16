export function numero(n: number | null | undefined): string {
  if (n === null || n === undefined) return '—'
  return n.toLocaleString('es-CO', { maximumFractionDigits: 0 })
}

export function numeroDecimal(n: number | null | undefined): string {
  if (n === null || n === undefined) return '—'
  return n.toLocaleString('es-CO', { maximumFractionDigits: 1 })
}

export function moneda(n: number | null | undefined): string {
  if (n === null || n === undefined) return '—'
  return n.toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
}

const DIAS_ES = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado']
const DIAS_CORTOS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb']

export function fechaLarga(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return d.toLocaleDateString('es-CO', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
}

export function hoyLargo(): string {
  return fechaLarga(new Date().toISOString())
}

export function diaNombre(dia: number): string {
  return DIAS_ES[dia % 7]
}

export function diaCorto(dia: number): string {
  return DIAS_CORTOS[dia % 7]
}

export function trunca(s: string, max: number): string {
  return s.length > max ? s.slice(0, max - 1) + '…' : s
}