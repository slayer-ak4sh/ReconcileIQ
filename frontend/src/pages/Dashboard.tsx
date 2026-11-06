import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { StatCard } from '../components/StatCard'
import { Skeleton } from '../components/Skeleton'
import { MoneyAtRiskTable } from '../components/MoneyAtRiskTable'
import { StatusBreakdown } from '../components/StatusBreakdown'

export default function Dashboard() {
  const { data: summary, isLoading } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: async () => (await api.get('/dashboard/summary')).data,
  })

  const { data: risk } = useQuery({
    queryKey: ['dashboard-risk'],
    queryFn: async () => (await api.get('/dashboard/money-at-risk')).data,
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="card p-5"><Skeleton className="h-16 w-full" /></div>
          ))}
        </div>
        <div className="card p-5"><Skeleton className="h-48 w-full" /></div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-semibold text-ink-900">Dashboard</h2>
        <p className="text-ink-500 text-sm">Reconciliation snapshot — money at risk, leakage and claims.</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Total orders" value={summary?.totalOrders ?? 0} />
        <StatCard label="Auto-matched" value={`${(summary?.autoMatchRate ?? 0).toFixed(1)}%`} sub={`${summary?.autoMatched ?? 0} of ${summary?.totalOrders ?? 0}`} />
        <StatCard label="Money at risk" value={summary?.moneyAtRisk ?? 0} currency />
        <StatCard label="Total leakage" value={summary?.totalLeakage ?? 0} currency />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2">
          <MoneyAtRiskTable rows={risk || []} />
        </div>
        <div>
          <StatusBreakdown data={summary?.byStatus || {}} />
        </div>
      </div>
    </div>
  )
}