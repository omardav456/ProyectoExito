interface EmptyStateProps {
  message: string
}

export function EmptyState({ message }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="w-10 h-10 rounded-full border border-[#1e2d45] flex items-center justify-center text-[#3d4f6b] mb-3 text-lg">∅</div>
      <p className="text-sm font-mono text-[#6b7a99]">{message}</p>
    </div>
  )
}