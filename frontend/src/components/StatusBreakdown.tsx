import { formatINR } from '../utils/format'

export function StatusBreakdown({ data }: { data: Record<string, number> }) {
  const entries = Object.entries(data)
  const total = entries.reduce((acc, [, v]) => acc + (v || 0), 0)

  if (entries.length === 0) {
    return (
      <div className="card p-5 h-full flex items-center justify-center text-ink-500 text-sm">No data</div>
    )
  }

  return (
    <div className="card p-5">
      <h3 className="text-sm font-semibold text-ink-900 mb-3">Leakage by status</h3>
      <ul className="space-y-3">
        {entries.map(([status, amount]) => (
          <li key={status} className="flex items-center justify-between">
            <span className="text-sm text-ink-700">{status.replace(/_/g, ' ').toLowerCase()}</span>
            <span className="text-sm font-medium tabular">{formatINR(amount)}</span>
          </li>
        ))}
      </ul>
      <div className="mt-4 pt-3 border-t border-ink-200 flex justify-between text-sm font-semibold">
        <span>Total</span>
        <span className="tabular">{formatINR(total)}</span>
      </div>
    </div>
  )
}