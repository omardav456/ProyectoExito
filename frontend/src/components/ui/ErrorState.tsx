interface ErrorStateProps {
  message: string
  onRetry?: () => void
}

export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-24 text-center">
      <div className="w-12 h-12 rounded-xl bg-[#f8717122] flex items-center justify-center text-2xl text-[#f87171] mb-4">!</div>
      <p className="text-[#f87171] font-mono text-sm mb-5 max-w-md break-words">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="text-xs font-mono uppercase tracking-widest px-4 py-2 rounded-lg bg-[#111827] border border-[#1e2d45] text-[#6b7a99] hover:border-[#FFD10055] hover:text-[#FFD100] transition-colors cursor-pointer"
        >
          Reintentar
        </button>
      )}
    </div>
  )
}