import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { formatINR, formatDate, statusLabel, statusClass } from '../utils/format'
import { ArrowLeft } from 'lucide-react'

interface SettlementRow {
  referenceNo?: string | null
  settlementDate: string
  grossAmount: number
  commission: number
  fixedFee: number
  collectionFee: number
  tcsAmount: number
  tdsAmount: number
  netAmount: number
}

interface CodRemittanceRow {
  utrNo?: string | null
  remittanceDate: string
  codCollected: number
  feeDeducted: number
  netRemitted: number
}

interface FreightRow {
  invoiceNo?: string | null
  invoiceDate: string
  billedWeight: number
  freightCharge: number
  codFee: number
  returnFee: number
  totalFreight: number
}

interface RateCardDetail {
  channelCode: string
  commissionPct: number
  fixedFee: number
  collectionFeePct: number
  minCollectionFee: number
  effectiveFrom: string
}

interface CalculationBreakdown {
  expected: number
  received: number
  difference: number
  tcs: number
  tds: number
  settlements: SettlementRow[]
  codRemittances: CodRemittanceRow[]
  freight: FreightRow | null
  rateCard: RateCardDetail | null
}

interface OrderDetailResponse {
  orderId: string
  reconciliationStatus: string
  dueDate: string
  maskedName: string
  maskedPhone: string
  maskedAddress: string
  calculation: CalculationBreakdown
}

export default function OrderDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const { data: order, isLoading, isError, error } = useQuery({
    queryKey: ['order-detail', id],
    queryFn: async () => (await api.get<OrderDetailResponse>(`/orders/${id}`)).data,
    enabled: !!id,
    retry: false,
  })

  if (isLoading) {
    return (
      <div className="min-h-screen bg-ink-50 p-8">
        <div className="flex items-center justify-center gap-4 mb-8">
          <Link to="/orders" className="text-accent-600 hover:text-accent-700 flex items-center gap-1">
            <ArrowLeft size={20} /> Back
          </Link>
          <h1 className="text-2xl font-semibold text-ink-900">Order Detail</h1>
        </div>
        <div className="card p-6">
          <div className="space-y-4">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="flex items-center gap-4">
                <div className="w-32 h-4 bg-ink-200 animate-pulse rounded" />
                <div className="w-24 h-4 bg-ink-200 animate-pulse rounded" />
                <div className="w-20 h-4 bg-ink-200 animate-pulse rounded" />
              </div>
            ))}
          </div>
        </div>
      </div>
    )
  }

  if (isError) {
    return (
      <div className="min-h-screen bg-ink-50 p-8 flex flex-col items-center justify-center">
        <Link to="/orders" className="text-accent-600 hover:text-accent-700 flex items-center gap-1 mb-6">
          <ArrowLeft size={20} /> Back
        </Link>
        <h2 className="text-2xl font-semibold text-ink-900">Error</h2>
        <p className="text-ink-600 text-lg">{error?.message || 'Failed to load order detail'}</p>
        <button onClick={() => window.location.reload()} className="mt-4 bg-accent-600 hover:bg-accent-700 text-white px-4 py-2 rounded-md text-sm font-medium">
          Retry
        </button>
      </div>
    )
  }

  if (!order) {
    return (
      <div className="min-h-screen bg-ink-50 p-8 flex flex-col items-center justify-center">
        <Link to="/orders" className="text-accent-600 hover:text-accent-700 flex items-center gap-1 mb-6">
          <ArrowLeft size={20} /> Back
        </Link>
        <h2 className="text-2xl font-semibold text-ink-900">Order not found</h2>
        <button onClick={() => navigate('/orders')} className="mt-4 bg-accent-600 hover:bg-accent-700 text-white px-4 py-2 rounded-md text-sm font-medium">
          Back to Orders
        </button>
      </div>
    )
  }

  const { calculation, ...orderInfo } = order

  const renderCalculationSection = () => {
    if (!calculation) return null

    return (
      <div className="space-y-6">
        {/* Expected vs Received vs Difference */}
        <div className="card p-6">
          <h3 className="text-lg font-semibold text-ink-900 mb-4">Settlement Calculation</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-emerald-50 p-4 rounded-lg">
              <div className="text-xs text-ink-500 uppercase tracking-wide">Expected Settlement</div>
              <div className="text-2xl font-bold text-emerald-700 tabular mt-1">{formatINR(calculation.expected)}</div>
            </div>
            <div className="bg-amber-50 p-4 rounded-lg">
              <div className="text-xs text-ink-500 uppercase tracking-wide">Received Settlement</div>
              <div className="text-2xl font-bold text-amber-700 tabular mt-1">{formatINR(calculation.received)}</div>
            </div>
            <div className={`p-4 rounded-lg ${calculation.difference < 0 ? 'bg-red-50' : 'bg-emerald-50'}`}>
              <div className="text-xs text-ink-500 uppercase tracking-wide">Difference</div>
              <div className={`text-2xl font-bold tabular mt-1 ${calculation.difference < 0 ? 'text-red-700' : 'text-emerald-700'}`}>
                {calculation.difference < 0 ? '−' : '+'}{formatINR(Math.abs(calculation.difference))}
              </div>
            </div>
          </div>
        </div>

        {/* Tax Details */}
        <div className="card p-6">
          <h3 className="text-lg font-semibold text-ink-900 mb-4">Tax Details (Excluded from Leakage)</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-ink-50 p-4 rounded-lg">
              <div className="text-xs text-ink-500 uppercase tracking-wide">TCS</div>
              <div className="text-xl font-bold text-ink-900 tabular mt-1">{formatINR(calculation.tcs)}</div>
            </div>
            <div className="bg-ink-50 p-4 rounded-lg">
              <div className="text-xs text-ink-500 uppercase tracking-wide">TDS</div>
              <div className="text-xl font-bold text-ink-900 tabular mt-1">{formatINR(calculation.tds)}</div>
            </div>
          </div>
        </div>

        {/* Settlement Rows */}
        {calculation.settlements && calculation.settlements.length > 0 && (
          <div className="card p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-ink-900">Settlement Rows</h3>
              <span className="text-sm text-ink-500">{calculation.settlements.length} row(s)</span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-ink-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Ref No</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Date</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Gross</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Commission</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Fixed Fee</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Collection</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">TCS</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">TDS</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Net</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {calculation.settlements.map((s, i) => (
                    <tr key={i} className="hover:bg-ink-50">
                      <td className="px-4 py-3 text-ink-900 font-mono text-sm">{s.referenceNo || '-'}</td>
                      <td className="px-4 py-3 text-ink-700">{formatDate(s.settlementDate)}</td>
                      <td className="px-4 py-3 text-right tabular">{formatINR(s.grossAmount)}</td>
                      <td className="px-4 py-3 text-right tabular">{formatINR(s.commission)}</td>
                      <td className="px-4 py-3 text-right tabular">{formatINR(s.fixedFee)}</td>
                      <td className="px-4 py-3 text-right tabular">{formatINR(s.collectionFee)}</td>
                      <td className="px-4 py-3 text-right tabular text-ink-500">{formatINR(s.tcsAmount)}</td>
                      <td className="px-4 py-3 text-right tabular text-ink-500">{formatINR(s.tdsAmount)}</td>
                      <td className="px-4 py-3 text-right tabular font-medium">{formatINR(s.netAmount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* COD Remittances */}
        {calculation.codRemittances && calculation.codRemittances.length > 0 && (
          <div className="card p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-ink-900">COD Remittances</h3>
              <span className="text-sm text-ink-500">{calculation.codRemittances.length} row(s)</span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-ink-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">UTR No</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Date</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">COD Collected</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Fee Deducted</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Net Remitted</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ink-100">
                  {calculation.codRemittances.map((c, i) => (
                    <tr key={i} className="hover:bg-ink-50">
                      <td className="px-4 py-3 text-ink-900 font-mono text-sm">{c.utrNo || '-'}</td>
                      <td className="px-4 py-3 text-ink-700">{formatDate(c.remittanceDate)}</td>
                      <td className="px-4 py-3 text-right tabular">{formatINR(c.codCollected)}</td>
                      <td className="px-4 py-3 text-right tabular">{formatINR(c.feeDeducted)}</td>
                      <td className="px-4 py-3 text-right tabular font-medium">{formatINR(c.netRemitted)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Freight */}
        {calculation.freight && (
          <div className="card p-6">
            <h3 className="text-lg font-semibold text-ink-900 mb-4">Freight Invoice</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="bg-ink-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">Invoice No</div>
                <div className="text-sm font-medium text-ink-900 mt-1">{calculation.freight.invoiceNo || '-'}</div>
              </div>
              <div className="bg-ink-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">Date</div>
                <div className="text-sm font-medium text-ink-900 mt-1">{formatDate(calculation.freight.invoiceDate)}</div>
              </div>
              <div className="bg-ink-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">Billed Weight</div>
                <div className="text-sm font-medium text-ink-900 mt-1">{calculation.freight.billedWeight} kg</div>
              </div>
              <div className="bg-ink-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">Total Freight</div>
                <div className="text-sm font-medium text-ink-900 mt-1">{formatINR(calculation.freight.totalFreight)}</div>
              </div>
            </div>
            <div className="mt-4 grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-violet-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">Freight Charge</div>
                <div className="text-sm font-medium text-violet-700 mt-1">{formatINR(calculation.freight.freightCharge)}</div>
              </div>
              <div className="bg-violet-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">COD Fee</div>
                <div className="text-sm font-medium text-violet-700 mt-1">{formatINR(calculation.freight.codFee)}</div>
              </div>
              <div className="bg-violet-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">Return Fee</div>
                <div className="text-sm font-medium text-violet-700 mt-1">{formatINR(calculation.freight.returnFee)}</div>
              </div>
            </div>
          </div>
        )}

        {/* Rate Card Applied */}
        {calculation.rateCard && (
          <div className="card p-6">
            <h3 className="text-lg font-semibold text-ink-900 mb-4">Rate Card Applied</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
              <div className="bg-ink-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">Channel</div>
                <div className="text-sm font-medium text-ink-900 mt-1">{calculation.rateCard.channelCode}</div>
              </div>
              <div className="bg-ink-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">Commission %</div>
                <div className="text-sm font-medium text-ink-900 mt-1">{calculation.rateCard.commissionPct}%</div>
              </div>
              <div className="bg-ink-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">Fixed Fee</div>
                <div className="text-sm font-medium text-ink-900 mt-1">{formatINR(calculation.rateCard.fixedFee)}</div>
              </div>
              <div className="bg-ink-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">Collection %</div>
                <div className="text-sm font-medium text-ink-900 mt-1">{calculation.rateCard.collectionFeePct}%</div>
              </div>
              <div className="bg-ink-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wider">Min Collection</div>
                <div className="text-sm font-medium text-ink-900 mt-1">{formatINR(calculation.rateCard.minCollectionFee)}</div>
              </div>
            </div>
            <div className="mt-4 text-xs text-ink-500">Effective from: {formatDate(calculation.rateCard.effectiveFrom)}</div>
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-ink-50">
      <header className="border-b border-ink-200 p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link to="/orders" className="text-accent-600 hover:text-accent-700 flex items-center gap-1">
              <ArrowLeft size={20} /> Back
            </Link>
            <div>
              <h1 className="text-2xl font-semibold text-ink-900">Order Detail</h1>
              <p className="text-ink-500 text-sm">Order ID: {orderInfo.orderId}</p>
            </div>
          </div>
          <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium ${statusClass(orderInfo.reconciliationStatus)}`}>
            {statusLabel(orderInfo.reconciliationStatus)}
          </span>
        </div>
      </header>

      <div className="p-6">
        {/* Order Info */}
        <div className="card p-6 mb-6">
          <h2 className="text-lg font-semibold text-ink-900 mb-4">Order Information</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <div className="text-xs text-ink-500 uppercase tracking-wider">Due Date</div>
              <div className="text-sm font-medium text-ink-900 mt-1">{formatDate(orderInfo.dueDate)}</div>
            </div>
            <div>
              <div className="text-xs text-ink-500 uppercase tracking-wider">Customer</div>
              <div className="text-sm text-ink-700 mt-1">{orderInfo.maskedName}</div>
            </div>
            <div>
              <div className="text-xs text-ink-500 uppercase tracking-wider">Phone</div>
              <div className="text-sm text-ink-700 mt-1">{orderInfo.maskedPhone}</div>
            </div>
            <div>
              <div className="text-xs text-ink-500 uppercase tracking-wider">Address</div>
              <div className="text-sm text-ink-700 mt-1">{orderInfo.maskedAddress}</div>
            </div>
          </div>
        </div>

        {/* Calculation Breakdown */}
        {renderCalculationSection()}
      </div>
    </div>
  )
}