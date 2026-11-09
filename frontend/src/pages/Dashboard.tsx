import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { Skeleton } from '../components/Skeleton'
import { formatINR, formatDate } from '../utils/format'
import { useEffect, useState, useMemo } from 'react'
import {
  ShoppingCart,
  AlertCircle,
  TrendingUp,
  CheckCircle,
  Banknote,
  BarChart3,
  Calendar,
  Wallet
} from 'lucide-react'
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  BarChart as BarChartLib,
  Bar,
  Cell
} from 'recharts'

interface MoneyAtRiskBucket {
  partnerCode: string
  partnerType?: string
  bucket0to7: number
  bucket8to15: number
  bucket16to30: number
  bucket31plus: number
  total: number
}

const STATUS_COLORS: Record<string, string> = {
  'SETTLED': '#10b981',
  'SHORT_PAID': '#f59e0b',
  'UNPAID_PAST_DUE': '#ef4444',
  'FREIGHT_OVERCHARGED': '#8b5cf6',
  'RECONCILED': '#3b82f6',
  'REVIEW': '#6b7280',
  'PENDING': '#f97316'
}

export default function Dashboard() {
  const [isMounted, setIsMounted] = useState(false)

  const { data: summary, isLoading, isError, error } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: async () => (await api.get('/dashboard/summary')).data,
    staleTime: 60000,
  })

  const { data: riskData, isLoading: isLoadingRisk, isError: isRiskError } = useQuery({
    queryKey: ['dashboard-risk'],
    queryFn: async () => (await api.get('/dashboard/money-at-risk')).data,
    staleTime: 60000,
  })

  useEffect(() => {
    setIsMounted(true)
  }, [])

  const chartData = useMemo(() => {
    if (!summary?.byStatus) return []
    return Object.entries(summary.byStatus).map(([status, count]) => ({
      name: status.replace(/_/g, ' ').toUpperCase(),
      value: count as number,
      color: STATUS_COLORS[status] || '#6b7280'
    }))
  }, [summary?.byStatus])

  const ageBucketData = useMemo(() => {
    if (!riskData?.byAgeBucket) return []
    return [
      { name: '0-7 days', value: parseFloat(riskData.byAgeBucket['0to7']?.toString() || '0') || 0 },
      { name: '8-15 days', value: parseFloat(riskData.byAgeBucket['8to15']?.toString() || '0') || 0 },
      { name: '16-30 days', value: parseFloat(riskData.byAgeBucket['16to30']?.toString() || '0') || 0 },
      { name: '31+ days', value: parseFloat(riskData.byAgeBucket['31plus']?.toString() || '0') || 0 }
    ]
  }, [riskData?.byAgeBucket])

  const topPartners = useMemo(() => {
    return (riskData?.byPartner as MoneyAtRiskBucket[])?.slice(0, 5) || []
  }, [riskData?.byPartner])

  if (isLoading || isLoadingRisk) {
    return (
      <div className="min-h-screen bg-ink-50 p-4 sm:p-6">
        <div className="max-w-7xl mx-auto">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="card p-5 animate-pulse">
                <Skeleton className="h-3 w-20 mb-2" />
                <Skeleton className="h-6 w-28 mb-1" />
                <Skeleton className="h-4 w-16" />
              </div>
            ))}
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 sm:gap-6 mt-6">
            <div className="lg:col-span-2">
              <div className="card p-6 sm:p-8 animate-pulse">
                <Skeleton className="h-5 w-40 mb-4" />
                <div className="grid grid-cols-4 gap-3 mb-4">
                  {[1,2,3,4].map((i) => (
                    <div key={i} className="bg-ink-100 rounded h-8" />
                  ))}
                </div>
                <Skeleton className="h-60 w-full" />
              </div>
            </div>
            <div className="space-y-4 sm:space-y-6">
              <div className="card p-5 sm:p-6 animate-pulse">
                <Skeleton className="h-4 w-24 mb-2" />
                <Skeleton className="h-32 w-32 rounded-full mx-auto" />
              </div>
              <div className="card p-5 sm:p-6 animate-pulse">
                <Skeleton className="h-4 w-32 mb-3" />
                <Skeleton className="h-8 w-16" />
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  if (isError || isRiskError) {
    return (
      <div className="min-h-screen bg-ink-50 flex items-center justify-center p-4">
        <div className="card p-8 max-w-md w-full text-center">
          <AlertCircle className="w-12 h-12 text-ink-400 mx-auto mb-4" />
          <h2 className="text-xl font-semibold text-ink-900 mb-2">Failed to load dashboard</h2>
          <p className="text-ink-500 mb-4">{isError ? error?.message : 'Error loading risk data'}</p>
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 bg-accent-600 hover:bg-accent-700 text-white rounded-md font-medium"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  if (!isMounted) return null

  return (
    <div className="min-h-screen bg-ink-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header Section */}
        <div className="pt-6 pb-4">
          <h1 className="text-2xl sm:text-3xl font-display font-bold text-ink-900">
            Dashboard
          </h1>
          <p className="text-sm text-ink-500 mt-1">
            Overview of reconciliation metrics as of {formatDate(new Date().toISOString())}
          </p>
        </div>

        {/* KPI Cards Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6 mb-6">
          <KPICard
            icon={ShoppingCart}
            iconColor="text-blue-600"
            bgColor="bg-blue-50"
            borderColor="border-blue-100"
            label="Total Orders"
            value={summary?.totalOrders ?? 0}
          />
          <KPICard
            icon={Wallet}
            iconColor="text-emerald-600"
            bgColor="bg-emerald-50"
            borderColor="border-emerald-100"
            label="Money at Risk"
            value={summary?.moneyAtRisk ?? 0}
            isCurrency
          />
          <KPICard
            icon={AlertCircle}
            iconColor="text-red-600"
            bgColor="bg-red-50"
            borderColor="border-red-100"
            label="Total Leakage"
            value={summary?.totalLeakage ?? 0}
            isCurrency
          />
          <KPICard
            icon={CheckCircle}
            iconColor="text-amber-600"
            bgColor="bg-amber-50"
            borderColor="border-amber-100"
            label="Auto-Matched"
            value={`${(summary?.autoMatchRate ?? 0).toFixed(1)}%`}
            sub={`${summary?.autoMatched ?? 0} of ${summary?.totalOrders ?? 0} orders`}
          />
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Money at Risk Table - Left Side (2/3 width) */}
          <div className="lg:col-span-2">
            <div className="card overflow-hidden">
              <div className="px-5 py-4 border-b border-ink-100 flex items-center justify-between bg-ink-50/50">
                <h3 className="font-semibold text-ink-900 flex items-center gap-2">
                  <Banknote className="w-5 h-5 text-accent-600" />
                  Money at Risk by Partner
                </h3>
                <span className="text-xs font-medium px-2.5 py-1 bg-white text-ink-600 rounded-full border border-ink-200">
                  Last 30 days
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="bg-ink-50/70 text-ink-500 text-[11px] uppercase tracking-wider font-semibold">
                      <th className="text-left px-4 py-3">Partner</th>
                      <th className="text-right px-3 py-3">0-7 Days</th>
                      <th className="text-right px-3 py-3">8-15 Days</th>
                      <th className="text-right px-3 py-3">16-30 Days</th>
                      <th className="text-right px-3 py-3">31+ Days</th>
                      <th className="text-right px-4 py-3 font-bold tabular text-ink-900 border-l border-ink-200 bg-ink-100/50">Total</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {topPartners.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="px-4 py-8 text-center text-ink-400 italic">
                          No exposure detected
                        </td>
                      </tr>
                    ) : topPartners.map((row) => (
                      <tr key={row.partnerCode} className="hover:bg-ink-50/50 transition-colors">
                        <td className="px-4 py-3">
                          <span className="font-semibold text-ink-900">{row.partnerCode}</span>
                          {row.partnerType && (
                            <span className="text-xs text-ink-400 ml-2">({row.partnerType})</span>
                          )}
                        </td>
                        <td className="text-right px-3 py-3 tabular">
                          <span className={row.bucket0to7 > 0 ? 'text-amber-600 font-medium' : 'text-ink-400'}>
                            {formatINR(row.bucket0to7)}
                          </span>
                        </td>
                        <td className="text-right px-3 py-3 tabular">
                          <span className={row.bucket8to15 > 0 ? 'text-orange-600 font-medium' : 'text-ink-400'}>
                            {formatINR(row.bucket8to15)}
                          </span>
                        </td>
                        <td className="text-right px-3 py-3 tabular">
                          <span className={row.bucket16to30 > 0 ? 'text-red-500 font-semibold' : 'text-ink-400'}>
                            {formatINR(row.bucket16to30)}
                          </span>
                        </td>
                        <td className="text-right px-3 py-3 tabular">
                          <span className={row.bucket31plus > 0 ? 'text-red-700 font-bold' : 'text-ink-400'}>
                            {formatINR(row.bucket31plus)}
                          </span>
                        </td>
                        <td className="text-right px-4 py-3 tabular font-bold text-ink-900 border-l border-ink-200 bg-ink-50/50">
                          {formatINR(row.total)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Status Breakdown & Charts - Right Side (1/3 width) */}
          <div className="space-y-4 sm:space-y-6">
            {/* Reconciliation Status Distribution */}
            <div className="card">
              <div className="px-4 sm:px-5 py-4 border-b border-ink-100 bg-ink-50/50">
                <h3 className="font-semibold text-ink-900 flex items-center gap-2">
                  <BarChart3 className="w-5 h-5 text-accent-600" />
                  Reconciliation Status
                </h3>
              </div>
              <div className="p-4 sm:p-5">
                <div className="h-56 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChartLib data={chartData} margin={{ top: 20, right: 20, left: 0, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                      <XAxis
                        dataKey="name"
                        fontSize={10}
                        tick={{ fill: '#6b7280' }}
                        interval={0}
                        angle={-45}
                        textAnchor="end"
                      />
                      <YAxis
                        fontSize={10}
                        tick={{ fill: '#6b7280' }}
                        tickLine={false}
                      />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: '#ffffff',
                          border: '1px solid #e5e7eb',
                          borderRadius: '8px',
                          boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
                        }}
                      />
                      <Bar
                        dataKey="value"
                        fill="#3b82f6"
                        radius={[4, 4, 0, 0]}
                      >
                        {chartData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Bar>
                    </BarChartLib>
                  </ResponsiveContainer>
                </div>
              </div>
            </div>

            {/* Trend Analysis Chart */}
            <div className="card">
              <div className="px-4 sm:px-5 py-4 border-b border-ink-100 bg-ink-50/50">
                <h3 className="font-semibold text-ink-900 flex items-center gap-2">
                  <TrendingUp className="w-5 h-5 text-emerald-600" />
                  Risk Distribution Trend
                </h3>
              </div>
              <div className="p-4 sm:p-5">
                <ResponsiveContainer width="100%" height={200}>
                  <AreaChart data={ageBucketData} margin={{ top: 5, right: 10, left: 0, bottom: 0 }}>
                    <defs>
                      <linearGradient id="colorRisk" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0" stopColor="#f59e0b" stopOpacity={0.5} />
                        <stop offset="1" stopColor="#f59e0b" stopOpacity={0.1} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                    <XAxis
                      dataKey="name"
                      fontSize={10}
                      tick={{ fill: '#6b7280' }}
                    />
                    <YAxis
                      fontSize={10}
                      tick={{ fill: '#6b7280' }}
                      tickLine={false}
                    />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#ffffff',
                        border: '1px solid #e5e7eb',
                        borderRadius: '8px'
                      }}
                      formatter={(value: number) => [formatINR(value), 'Amount']}
                    />
                    <Area
                      type="monotone"
                      dataKey="value"
                      stroke="#f59e0b"
                      fillOpacity={1}
                      fill="url(#colorRisk)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>
        </div>

        {/* Financial Summary Section */}
        <div className="mt-6 grid grid-cols-1 gap-4 sm:gap-6">
          <div className="card">
            <div className="px-5 py-4 border-b border-ink-100">
              <h3 className="font-semibold text-ink-900 flex items-center gap-2">
                <Calendar className="w-5 h-5 text-accent-600" />
                Financial Summary
              </h3>
            </div>
            <div className="p-4 sm:p-5">
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                {summary && [
                  { label: 'Total Orders', value: summary.totalOrders, icon: ShoppingCart, color: 'text-blue-600', bg: 'bg-blue-50' },
                  { label: 'Auto-Matched', value: summary.autoMatched, icon: CheckCircle, color: 'text-emerald-600', bg: 'bg-emerald-50' },
                  { label: 'Match Rate', value: `${summary.autoMatchRate.toFixed(1)}%`, icon: TrendingUp, color: 'text-amber-600', bg: 'bg-amber-50' },
                  { label: 'Money at Risk', value: formatINR(summary.moneyAtRisk), icon: AlertCircle, color: 'text-red-600', bg: 'bg-red-50' }
                ].map((item, idx) => (
                  <div key={idx} className="bg-ink-50/50 rounded-lg p-4">
                    <div className="flex items-center gap-3">
                      <div className={`p-2.5 rounded-full ${item.bg}`}>
                        <item.icon className={`w-5 h-5 ${item.color}`} />
                      </div>
                      <div>
                        <div className="text-xs text-ink-500 uppercase tracking-wide font-medium">
                          {item.label}
                        </div>
                        <div className="text-lg font-bold text-ink-900 mt-0.5">
                          {item.value}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

// KPI Card Component
function KPICard({
  icon: Icon,
  iconColor,
  bgColor,
  borderColor,
  label,
  value,
  isCurrency = false,
  sub
}: {
  icon: React.ElementType
  iconColor: string
  bgColor: string
  borderColor: string
  label: string
  value: number | string
  isCurrency?: boolean
  sub?: string
}) {
  const displayValue = typeof value === 'number' && isCurrency ? formatINR(value) : value

  return (
    <div className={`card p-4 sm:p-5 border ${borderColor} hover:shadow-md transition-all`}>
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <div className="text-xs text-ink-500 uppercase tracking-wide font-medium mb-1">
            {label}
          </div>
          <div className="text-2xl sm:text-2xl font-bold leading-none text-ink-900">
            {displayValue}
          </div>
          {sub && (
            <div className="text-xs text-ink-500 mt-1">
              {sub}
            </div>
          )}
        </div>
        <div className={`p-3 rounded-full ${bgColor}`}>
          <Icon className={`w-6 h-6 ${iconColor}`} />
        </div>
      </div>
    </div>
  )
}