export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse bg-ink-200 rounded ${className}`} />
}

export function SkeletonRow() {
  return (
    <div className="flex items-center gap-3 py-2">
      <Skeleton className="h-4 w-12" />
      <Skeleton className="h-4 w-32" />
      <Skeleton className="h-4 w-24" />
      <Skeleton className="h-4 w-20" />
    </div>
  )
}