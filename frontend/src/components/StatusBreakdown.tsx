import { formatINR } from '../utils/format'

export function StatusBreakdown({ data }: { data: Record<string, number> }) {
  const entries = Object.entries(data)
  const total = entries.reduce((acc, [, v]) => acc + (v || 0), 0)

  if (entries.length === 0) {
    return (
      <div className="card p-8 h-full flex flex-col items-center justify-center text-ink-400">
        <div className="w-12 h-12 rounded-full bg-ink-50 flex items-center justify-center mb-3">
          📊
        </div>
        <p className="text-sm italic">No leakage data available</p>
      </div>
    )
  }

  return (
    <div className="card h-full flex flex-col">
      <div className="px-5 py-4 border-b border-ink-100">
        <h3 className="font-semibold text-ink-900">Leakage by status</h3>
      </div>

      <div className="p-6 flex-1 flex flex-col justify-center">
        <div className="relative h-4 w-full bg-ink-100 rounded-full overflow-hidden flex mb-8">
          {entries.map(([status, amount], idx) => {
            const percent = total > 0 ? (amount / total) * 100 : 0
            if (percent < 0.5) return null
            const colors = ['bg-amber-500', 'bg-red-500', 'bg-emerald-500', 'bg-violet-500', 'bg-blue-500']
            return (
              <div
                key={status}
                className={`${colors[idx % colors.length]} h-full transition-all duration-500`}
                style={{ width: `${percent}%` }}
                title={`${status}: ${percent.toFixed(1)}%`}
              />
            )
          })}
        </div>

        <ul className="space-y-4">
          {entries.map(([status, amount], idx) => {
            const colors = ['bg-amber-500', 'bg-red-500', 'bg-emerald-500', 'bg-violet-500', 'bg-blue-500']
            return (
              <li key={status} className="flex items-center justify-between group">
                <div className="flex items-center gap-3">
                  <span className={`w-2.5 h-2.5 rounded-full ${colors[idx % colors.length]}`} />
                  <span className="text-sm text-ink-700 capitalize group-hover:text-ink-900 transition-colors">
                    {status.replace(/_/g, ' ').toLowerCase()}
                  </span>
                </div>
                <div className="text-right">
                  <div className="text-sm font-bold tabular text-ink-900">{formatINR(amount)}</div>
                  <div className="text-[10px] text-ink-400 font-medium tabular">
                    {total > 0 ? ((amount / total) * 100).toFixed(1) : 0}%
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      </div>

      <div className="mt-auto px-6 py-4 bg-ink-50/50 border-t border-ink-100 flex justify-between items-center rounded-b-lg">
        <span className="text-xs font-semibold text-ink-500 uppercase tracking-wider">Total Leakage</span>
        <span className="text-lg font-bold tabular text-ink-900">{formatINR(total)}</span>
      </div>
    </div>
  )
}
