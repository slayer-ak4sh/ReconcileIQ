import { formatINR } from '../utils/format'
import type { MoneyAtRiskBucket } from '../types'

export function MoneyAtRiskTable({ rows }: { rows: MoneyAtRiskBucket[] }) {
  return (
    <div className="card overflow-hidden">
      <div className="px-5 py-4 border-b border-ink-100 flex items-center justify-between">
        <h3 className="font-semibold text-ink-900">Money at risk by partner & age</h3>
        <span className="text-xs font-medium px-2 py-1 bg-ink-50 text-ink-600 rounded-full border border-ink-100">
          Last 30 days
        </span>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-ink-50/50 text-ink-500 text-[10px] uppercase tracking-wider">
              <th className="text-left px-5 py-3 font-semibold">Partner</th>
              <th className="text-right px-3 py-3 font-semibold tabular">0–7 days</th>
              <th className="text-right px-3 py-3 font-semibold tabular">8–15 days</th>
              <th className="text-right px-3 py-3 font-semibold tabular">16–30 days</th>
              <th className="text-right px-3 py-3 font-semibold tabular">31+ days</th>
              <th className="text-right px-5 py-3 font-bold tabular text-ink-900 border-l border-ink-100">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-ink-100">
            {rows.length === 0 ? (
              <tr><td colSpan={6} className="px-5 py-12 text-center text-ink-400 italic">No exposure detected</td></tr>
            ) : rows.map((r) => (
              <tr key={r.partnerCode} className="hover:bg-ink-50/50 group transition-colors">
                <td className="px-5 py-3.5 text-ink-900 font-semibold">{r.partnerCode}</td>
                <td className={`px-3 py-3.5 text-right tabular ${r.bucket0to7 > 0 ? 'text-amber-600' : 'text-ink-400'}`}>
                  {formatINR(r.bucket0to7)}
                </td>
                <td className={`px-3 py-3.5 text-right tabular ${r.bucket8to15 > 0 ? 'text-orange-600 font-medium' : 'text-ink-400'}`}>
                  {formatINR(r.bucket8to15)}
                </td>
                <td className={`px-3 py-3.5 text-right tabular ${r.bucket16to30 > 0 ? 'text-red-500 font-semibold' : 'text-ink-400'}`}>
                  {formatINR(r.bucket16to30)}
                </td>
                <td className={`px-3 py-3.5 text-right tabular ${r.bucket31plus > 0 ? 'text-red-700 font-bold bg-red-50/30' : 'text-ink-400'}`}>
                  {formatINR(r.bucket31plus)}
                </td>
                <td className="px-5 py-3.5 text-right tabular font-bold text-ink-900 border-l border-ink-100 bg-ink-50/30">
                  {formatINR(r.total)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
