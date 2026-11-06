import { formatINR } from '../utils/format'
import type { MoneyAtRiskBucket } from '../types'

export function MoneyAtRiskTable({ rows }: { rows: MoneyAtRiskBucket[] }) {
  return (
    <div className="card overflow-hidden">
      <div className="px-5 py-3 border-b border-ink-200">
        <h3 className="text-sm font-semibold text-ink-900">Money at risk by partner & age</h3>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-ink-500 text-xs uppercase tracking-wide">
              <th className="text-left px-5 py-2 font-medium">Partner</th>
              <th className="text-right px-3 py-2 font-medium tabular">0–7 days</th>
              <th className="text-right px-3 py-2 font-medium tabular">8–15 days</th>
              <th className="text-right px-3 py-2 font-medium tabular">16–30 days</th>
              <th className="text-right px-3 py-2 font-medium tabular">31+ days</th>
              <th className="text-right px-5 py-2 font-medium tabular">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-ink-100">
            {rows.length === 0 ? (
              <tr><td colSpan={6} className="px-5 py-8 text-center text-ink-500">No data yet</td></tr>
            ) : rows.map((r) => (
              <tr key={r.partnerCode} className="hover:bg-ink-50">
                <td className="px-5 py-2.5 text-ink-900 font-medium">{r.partnerCode}</td>
                <td className="px-3 py-2.5 text-right tabular">{formatINR(r.bucket0to7)}</td>
                <td className="px-3 py-2.5 text-right tabular">{formatINR(r.bucket8to15)}</td>
                <td className="px-3 py-2.5 text-right tabular">{formatINR(r.bucket16to30)}</td>
                <td className="px-3 py-2.5 text-right tabular">{formatINR(r.bucket31plus)}</td>
                <td className="px-5 py-2.5 text-right tabular font-semibold text-ink-900">{formatINR(r.total)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}