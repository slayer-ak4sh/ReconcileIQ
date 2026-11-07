import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { formatINR, formatDate, statusLabel, statusClass } from '../utils/format'
import { Download, ChevronRight, Loader2, X } from 'lucide-react'
import { useAuth } from '../auth/useAuth'

interface ClaimItem {
  orderId: string
  claimType: string
  amount: number
  evidenceNotes?: string | null
}

interface Claim {
  id: number
  claimNumber: string
  partnerCode: string
  partnerType: 'CHANNEL' | 'COURIER'
  totalAmount: number
  status: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'RECOVERED'
  createdBy: string
  submittedAt?: string | null
  decidedBy?: string | null
  decidedAt?: string | null
  decisionNotes?: string | null
  recoveredAt?: string | null
  createdAt: string
  items: ClaimItem[]
  events: ClaimEvent[]
}

interface ClaimEvent {
  id: number
  fromStatus: string
  toStatus: string
  actorEmail: string
  notes?: string | null
  createdAt: string
}

interface ClaimsResponse {
  content: Claim[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export default function Claims() {
  const { hasRole } = useAuth()
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState('')
  const [partnerFilter, setPartnerFilter] = useState('')
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [selectedClaim, setSelectedClaim] = useState<Claim | null>(null)

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['claims', statusFilter, partnerFilter, page, size],
    queryFn: async () => {
      const params = new URLSearchParams()
      params.set('page', page.toString())
      params.set('size', size.toString())
      if (statusFilter) params.set('status', statusFilter)
      if (partnerFilter) params.set('partner', partnerFilter)
      const res = await api.get<ClaimsResponse>(`/claims?${params.toString()}`)
      return res.data
    },
    placeholderData: (prev) => prev,
  })

  const submitMutation = useMutation({
    mutationFn: (claimId: number) => api.post(`/claims/${claimId}/submit`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['claims'] }); setSelectedClaim(null); },
  })

  const approveMutation = useMutation({
    mutationFn: ({ claimId, notes }: { claimId: number; notes?: string }) =>
      api.post(`/claims/${claimId}/approve`, null, { params: { notes } }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['claims'] }); setSelectedClaim(null); },
  })

  const rejectMutation = useMutation({
    mutationFn: ({ claimId, notes }: { claimId: number; notes?: string }) =>
      api.post(`/claims/${claimId}/reject`, null, { params: { notes } }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['claims'] }); setSelectedClaim(null); },
  })

  const recoverMutation = useMutation({
    mutationFn: (claimId: number) => api.post(`/claims/${claimId}/recover`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['claims'] }); setSelectedClaim(null); },
  })

  const downloadCsv = async (claimId: number) => {
    const res = await api.get(`/claims/${claimId}/evidence.csv`, { responseType: 'blob' })
    const blob = new Blob([res.data], { type: 'text/csv' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `evidence-${claimId}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
  }

  const canSubmit = hasRole('FINANCE_EXECUTIVE')
  const canApprove = hasRole('COFOUNDER')
  const canRecover = hasRole('FINANCE_EXECUTIVE')

  if (isLoading && !data) {
    return (
      <div className="min-h-screen bg-ink-50 p-8">
        <header className="border-b border-ink-200 p-4 mb-6">
          <h1 className="text-2xl font-semibold text-ink-900">Claims</h1>
        </header>
        <div className="card p-6">
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
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
        <h2 className="text-2xl font-semibold text-ink-900">Error</h2>
        <p className="text-ink-600 text-lg">{error?.message || 'Failed to load claims'}</p>
        <button onClick={() => window.location.reload()} className="mt-4 bg-accent-600 hover:bg-accent-700 text-white px-4 py-2 rounded-md text-sm font-medium">
          Retry
        </button>
      </div>
    )
  }

  // Detail modal
  if (selectedClaim) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
        <div className="bg-white rounded-lg max-w-3xl w-full max-h-[90vh] overflow-y-auto">
          <div className="p-6 border-b border-ink-200 flex items-center justify-between">
            <h2 className="text-xl font-semibold text-ink-900">Claim {selectedClaim.claimNumber}</h2>
            <button onClick={() => setSelectedClaim(null)} className="text-ink-400 hover:text-ink-600">
              <X size={24} />
            </button>
          </div>
          <div className="p-6 space-y-6">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <div className="text-xs text-ink-500 uppercase tracking-wide">Partner</div>
                <div className="text-sm font-medium text-ink-900 mt-1">{selectedClaim.partnerCode} ({selectedClaim.partnerType})</div>
              </div>
              <div>
                <div className="text-xs text-ink-500 uppercase tracking-wide">Total Amount</div>
                <div className="text-sm font-medium text-ink-900 mt-1">{formatINR(selectedClaim.totalAmount)}</div>
              </div>
              <div>
                <div className="text-xs text-ink-500 uppercase tracking-wide">Status</div>
                <div className="mt-1">
                  <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${statusClass(selectedClaim.status)}`}>
                    {statusLabel(selectedClaim.status)}
                  </span>
                </div>
              </div>
              <div>
                <div className="text-xs text-ink-500 uppercase tracking-wide">Created By</div>
                <div className="text-sm text-ink-700 mt-1">{selectedClaim.createdBy}</div>
              </div>
              {selectedClaim.submittedAt && (
                <div>
                  <div className="text-xs text-ink-500 uppercase tracking-wide">Submitted</div>
                  <div className="text-sm text-ink-700 mt-1">{formatDate(selectedClaim.submittedAt)}</div>
                </div>
              )}
              {selectedClaim.decidedAt && (
                <div>
                  <div className="text-xs text-ink-500 uppercase tracking-wide">Decided</div>
                  <div className="text-sm text-ink-700 mt-1">{formatDate(selectedClaim.decidedAt)} by {selectedClaim.decidedBy}</div>
                </div>
              )}
              {selectedClaim.recoveredAt && (
                <div>
                  <div className="text-xs text-ink-500 uppercase tracking-wide">Recovered</div>
                  <div className="text-sm text-emerald-700 mt-1">{formatDate(selectedClaim.recoveredAt)}</div>
                </div>
              )}
            </div>

            {selectedClaim.decisionNotes && (
              <div className="bg-ink-50 p-4 rounded-lg">
                <div className="text-xs text-ink-500 uppercase tracking-wide">Decision Notes</div>
                <div className="text-sm text-ink-700 mt-1">{selectedClaim.decisionNotes}</div>
              </div>
            )}

            <div>
              <div className="text-xs text-ink-500 uppercase tracking-wide mb-2">Items</div>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-ink-50">
                    <tr>
                      <th className="px-4 py-2 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Order ID</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Type</th>
                      <th className="px-4 py-2 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Amount</th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Notes</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ink-100">
                    {selectedClaim.items.map((item, i) => (
                      <tr key={i} className="hover:bg-ink-50">
                        <td className="px-4 py-2 font-mono text-sm">{item.orderId}</td>
                        <td className="px-4 py-2 text-ink-700">{item.claimType}</td>
                        <td className="px-4 py-2 text-right tabular">{formatINR(item.amount)}</td>
                        <td className="px-4 py-2 text-ink-500">{item.evidenceNotes || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div>
              <div className="text-xs text-ink-500 uppercase tracking-wide mb-2">Event History</div>
              <div className="space-y-2">
                {selectedClaim.events.map((event) => (
                  <div key={event.id} className="flex items-center gap-3 text-sm">
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-amber-100 text-amber-700">
                      {event.fromStatus} → {event.toStatus}
                    </span>
                    <span className="text-ink-500">by {event.actorEmail}</span>
                    <span className="text-ink-400">{formatDate(event.createdAt)}</span>
                    {event.notes && <span className="text-ink-600 ml-auto">{event.notes}</span>}
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-4 border-t border-ink-200 flex items-center justify-end gap-3">
              <button
                onClick={() => setSelectedClaim(null)}
                className="px-4 py-2 rounded border border-ink-300 text-sm hover:bg-ink-50"
              >
                Close
              </button>
              <button
                onClick={() => downloadCsv(selectedClaim.id)}
                className="px-4 py-2 rounded border border-ink-300 text-sm hover:bg-ink-50 flex items-center gap-2"
              >
                <Download size={16} /> Export CSV
              </button>
              {selectedClaim.status === 'DRAFT' && canSubmit && (
                <button
                  onClick={() => submitMutation.mutate(selectedClaim.id)}
                  disabled={submitMutation.isPending}
                  className="px-4 py-2 rounded bg-accent-600 text-white text-sm hover:bg-accent-700 disabled:opacity-50"
                >
                  {submitMutation.isPending ? <Loader2 size={16} className="animate-spin" /> : 'Submit'}
                </button>
              )}
              {selectedClaim.status === 'SUBMITTED' && canApprove && (
                <div className="flex gap-2">
                  <button
                    onClick={() => approveMutation.mutate({ claimId: selectedClaim.id, notes: 'Approved' })}
                    disabled={approveMutation.isPending}
                    className="px-4 py-2 rounded bg-emerald-600 text-white text-sm hover:bg-emerald-700 disabled:opacity-50"
                  >
                    {approveMutation.isPending ? <Loader2 size={16} className="animate-spin" /> : 'Approve'}
                  </button>
                  <button
                    onClick={() => rejectMutation.mutate({ claimId: selectedClaim.id, notes: 'Rejected' })}
                    disabled={rejectMutation.isPending}
                    className="px-4 py-2 rounded bg-red-600 text-white text-sm hover:bg-red-700 disabled:opacity-50"
                  >
                    {rejectMutation.isPending ? <Loader2 size={16} className="animate-spin" /> : 'Reject'}
                  </button>
                </div>
              )}
              {selectedClaim.status === 'APPROVED' && canRecover && (
                <button
                  onClick={() => recoverMutation.mutate(selectedClaim.id)}
                  disabled={recoverMutation.isPending}
                  className="px-4 py-2 rounded bg-accent-600 text-white text-sm hover:bg-accent-700 disabled:opacity-50"
                >
                  {recoverMutation.isPending ? <Loader2 size={16} className="animate-spin" /> : 'Mark Recovered'}
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-ink-50">
      <header className="border-b border-ink-200 p-4">
        <h1 className="text-2xl font-semibold text-ink-900">Claims</h1>
        <p className="text-ink-500 text-sm">Manage partner claims with evidence and audit trail</p>
      </header>

      <div className="p-4">
        {/* Filter Bar */}
        <div className="card p-4 mb-6">
          <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center">
            <div className="flex gap-3 flex-wrap">
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="px-3 py-2 rounded-md border border-ink-300 focus:ring-2 focus:ring-accent-500 focus:border-accent-500 outline-none text-sm"
              >
                <option value="">All Statuses</option>
                <option value="DRAFT">Draft</option>
                <option value="SUBMITTED">Submitted</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
                <option value="RECOVERED">Recovered</option>
              </select>
              <select
                value={partnerFilter}
                onChange={(e) => setPartnerFilter(e.target.value)}
                className="px-3 py-2 rounded-md border border-ink-300 focus:ring-2 focus:ring-accent-500 focus:border-accent-500 outline-none text-sm"
              >
                <option value="">All Partners</option>
                <option value="Website">Website</option>
                <option value="Marketplace 1">Marketplace 1</option>
                <option value="Marketplace 2">Marketplace 2</option>
                <option value="QuickCommerce">QuickCommerce</option>
                <option value="Supermarket">Supermarket</option>
                <option value="Courier A">Courier A</option>
                <option value="Courier B">Courier B</option>
              </select>
              {(statusFilter || partnerFilter) && (
                <button
                  onClick={() => { setStatusFilter(''); setPartnerFilter(''); }}
                  className="flex items-center gap-1 px-3 py-2 rounded-md border border-ink-300 text-ink-600 hover:bg-ink-100 text-sm"
                >
                  <X size={16} /> Clear
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-ink-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Claim Number</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Partner</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Amount</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Created</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100">
                {data?.content.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-4 py-12 text-center text-ink-500">No claims found</td>
                  </tr>
                ) : (
                  data?.content.map((claim) => (
                    <tr key={claim.id} className="hover:bg-ink-50 cursor-pointer" onClick={() => setSelectedClaim(claim)}>
                      <td className="px-4 py-3 font-mono text-sm">{claim.claimNumber}</td>
                      <td className="px-4 py-3">{claim.partnerCode} ({claim.partnerType})</td>
                      <td className="px-4 py-3 text-right tabular">{formatINR(claim.totalAmount)}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${statusClass(claim.status)}`}>
                          {statusLabel(claim.status)}
                        </span>
                      </td>
                      <td className="px-4 py-3">{formatDate(claim.createdAt)}</td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-ink-100 text-ink-600 hover:bg-ink-200">
                          <ChevronRight size={14} /> View
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="border-t border-ink-200 px-4 py-3 flex items-center justify-between">
            <div className="text-sm text-ink-500">
              Page {page + 1} of {data?.totalPages || 1}
              (Total: {data?.totalElements || 0})
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1 rounded border border-ink-300 text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-ink-50"
              >
                Previous
              </button>
              <button
                onClick={() => setPage(p => Math.min((data?.totalPages || 1) - 1, p + 1))}
                disabled={page >= (data?.totalPages || 1) - 1}
                className="px-3 py-1 rounded border border-ink-300 text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-ink-50"
              >
                Next
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}