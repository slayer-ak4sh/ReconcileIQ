import { Skeleton } from './Skeleton'
import { formatINR } from '../utils/format'

interface Props {
  label: string
  value: number | string
  currency?: boolean
  sub?: string
}

export function StatCard({ label, value, currency, sub }: Props) {
  const v = typeof value === 'number' && currency ? formatINR(value) : value
  return (
    <div className="card p-5">
      <div className="text-ink-500 text-xs uppercase tracking-wide">{label}</div>
      <div className="mt-2 text-2xl font-semibold tabular text-ink-900">{v}</div>
      {sub && <div className="mt-1 text-xs text-ink-500">{sub}</div>}
    </div>
  )
}

export function StatCardSkeleton() {
  return (
    <div className="card p-5">
      <Skeleton className="h-3 w-24 mb-2" />
      <Skeleton className="h-7 w-32" />
    </div>
  )
}