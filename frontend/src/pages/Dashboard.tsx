import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { StatCard } from '../components/StatCard'
import { Skeleton } from '../components/Skeleton'
import { MoneyAtRiskTable } from '../components/MoneyAtRiskTable'
import { StatusBreakdown } from '../components/StatusBreakdown'

export default function Dashboard() {
  const { data: summary, isLoading, isError, error } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: async () => (await api.get('/dashboard/summary')).data,
    retry: false,
  })

  const { data: risk, isLoading: isLoading2, isError: isError2, error: error2 } = useQuery({
    queryKey: ['dashboard-risk'],
    queryFn: async () => (await api.get('/dashboard/money-at-risk')).data,
    retry: false,
  })

  // Show combined loading state
  if (isLoading || isLoading2) {
    return (
      <div className="min-h-screen bg-ink-50 p-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div>
            <h1 className="text-2xl font-semibold text-ink-900">ReconcileIQ</h1>
            <p className="text-ink-500 text-sm">Finance reconciliation</p>
          </div>
          <div className="flex justify-center items-center py-12">
            <div className="grid grid-cols-2 gap-4">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="card p-5">
                  <Skeleton className="h-6 w-24 mb-2" />
                  <Skeleton className="h-7 w-32" />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    )
  }

  // Error state
  if (isError || isError2) {
    return (
      <div className="min-h-screen bg-ink-50 p-8">
        <div className="flex flex-col items-center justify-center gap-6">
          <h2 className="text-2xl font-semibold text-ink-900">Error</h2>
          <p className="text-ink-600 text-lg">{isError ? error?.message || 'Failed to load dashboard summary' : error2?.message || 'Failed to load money at risk'}</p>
          <button
            onClick={() => window.location.reload()}
            className="bg-accent-600 hover:bg-accent-700 text-white px-4 py-2 rounded-md text-sm font-medium"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-ink-50">
      <header className="border-b border-ink-200 p-4">
        <h1 className="text-2xl font-semibold text-ink-900">Dashboard</h1>
        <p className="text-ink-500 text-sm">Reconciliation snapshot — money at risk, leakage and claims.</p>
      </header>

      <div className="p-6 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Total orders" value={summary?.totalOrders ?? 0} />
        <StatCard label="Money at risk" value={summary?.moneyAtRisk ?? 0} currency />
        <StatCard label="Total leakage" value={summary?.totalLeakage ?? 0} currency />
        <StatCard label="Auto-matched" value={`${(summary?.autoMatchRate ?? 0).toFixed(1)}%`} sub={`${summary?.autoMatched ?? 0} of ${summary?.totalOrders ?? 0}`} />
      </div>

      <div className="p-6 lg:col-span-2 grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div>
          <MoneyAtRiskTable rows={risk?.byPartner || []} />
        </div>
        <div>
          <StatusBreakdown data={summary?.byStatus || {}} />
        </div>
      </div>
    </div>
  )
}