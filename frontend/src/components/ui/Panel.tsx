import type { ReactNode } from 'react'

interface PanelProps {
  title?: string
  className?: string
  children: ReactNode
}

export function Panel({ title, className = '', children }: PanelProps) {
  return (
    <div className={`bg-[#111827] border border-[#1e2d45] rounded-xl p-5 ${className}`}>
      {title && (
        <h3 className="text-sm font-mono uppercase tracking-widest text-[#6b7a99] mb-4">{title}</h3>
      )}
      {children}
    </div>
  )
}