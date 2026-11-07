import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { formatINR } from '../utils/format'
import { Loader2, Edit2 } from 'lucide-react'
import { useAuth } from '../auth/useAuth'

interface WeightSlab {
  id: number
  minWeightKg: number
  maxWeightKg: number
  rate: number
}

interface ChannelRateCard {
  id: number
  channelCode: string
  commissionPct: number
  fixedFee: number
  collectionFeePct: number
  minCollectionFee: number
  effectiveFrom: string
}

interface CourierRateCard {
  id: number
  courierCode: string
  codFeePct: number
  minCodFee: number
  returnFee: number
  effectiveFrom: string
  slabs: WeightSlab[]
}

interface ChannelRateCardRequest {
  channelCode: string
  commissionPct: number
  fixedFee: number
  collectionFeePct: number
  minCollectionFee: number
}

interface CourierRateCardRequest {
  courierCode: string
  codFeePct: number
  minCodFee: number
  returnFee: number
}

function RateCards() {
  const { hasRole } = useAuth()
  const queryClient = useQueryClient()
  const isExecutive = hasRole('FINANCE_EXECUTIVE')
  const [editingChannel, setEditingChannel] = useState<ChannelRateCard | null>(null)
  const [editingCourier, setEditingCourier] = useState<CourierRateCard | null>(null)

  const { data: channels, isLoading: channelsLoading } = useQuery({
    queryKey: ['channel-rate-cards'],
    queryFn: async () => (await api.get<ChannelRateCard[]>('/ratecards')).data,
  })

  const { data: couriers, isLoading: couriersLoading } = useQuery({
    queryKey: ['courier-rate-cards'],
    queryFn: async () => (await api.get<CourierRateCard[]>('/ratecards/courier')).data,
  })

  const updateChannelMutation = useMutation({
    mutationFn: (card: ChannelRateCardRequest) => api.put(`/ratecards/channel/${card.channelCode}`, card),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['channel-rate-cards'] })
      setEditingChannel(null)
    },
  })

  const updateCourierMutation = useMutation({
    mutationFn: (card: CourierRateCardRequest) => api.put(`/ratecards/courier/${card.courierCode}`, card),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['courier-rate-cards'] })
      setEditingCourier(null)
    },
  })

  const isLoading = channelsLoading || couriersLoading

  if (isLoading) {
    return (
      <div className="min-h-screen bg-ink-50 p-8">
        <header className="border-b border-ink-200 p-4 mb-6">
          <h1 className="text-2xl font-semibold text-ink-900">Rate Cards</h1>
        </header>
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

  return (
    <div className="min-h-screen bg-ink-50">
      <header className="border-b border-ink-200 p-4">
        <h1 className="text-2xl font-semibold text-ink-900">Rate Cards</h1>
        <p className="text-ink-500 text-sm">Manage channel and courier rate cards for reconciliation calculations</p>
      </header>

      <div className="p-4 space-y-6">
        {/* Channel Rate Cards */}
        <div className="card">
          <div className="p-4 border-b border-ink-200">
            <h2 className="text-lg font-semibold text-ink-900">Channel Rate Cards</h2>
            <p className="text-ink-500 text-sm">Commission, fixed fee, and collection fee per channel</p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-ink-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Channel</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Commission %</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Fixed Fee</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Collection %</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Min Collection</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Effective From</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100">
                {channels?.map((channel: ChannelRateCard) => {
                  const isEditing = editingChannel?.channelCode === channel.channelCode
                  return (
                    <tr key={channel.id} className="hover:bg-ink-50">
                      <td className="px-4 py-3 font-medium text-ink-900">{channel.channelCode}</td>
                      {isEditing ? (
                        <ChannelEditRow
                          channel={channel}
                          onSave={(data: ChannelRateCardRequest) => updateChannelMutation.mutate(data)}
                          onCancel={() => setEditingChannel(null)}
                          isSaving={updateChannelMutation.isPending}
                        />
                      ) : (
                        <>
                          <td className="px-4 py-3 text-right tabular">{channel.commissionPct.toFixed(2)}%</td>
                          <td className="px-4 py-3 text-right tabular">{formatINR(channel.fixedFee)}</td>
                          <td className="px-4 py-3 text-right tabular">{channel.collectionFeePct.toFixed(2)}%</td>
                          <td className="px-4 py-3 text-right tabular">{formatINR(channel.minCollectionFee)}</td>
                          <td className="px-4 py-3">{channel.effectiveFrom}</td>
                          <td className="px-4 py-3">
                            {isExecutive && (
                              <button
                                onClick={() => setEditingChannel(channel)}
                                className="text-accent-600 hover:text-accent-700 flex items-center gap-1 text-sm"
                              >
                                <Edit2 size={16} /> Edit
                              </button>
                            )}
                          </td>
                        </>
                      )}
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Courier Rate Cards */}
        <div className="card">
          <div className="p-4 border-b border-ink-200">
            <h2 className="text-lg font-semibold text-ink-900">Courier Rate Cards</h2>
            <p className="text-ink-500 text-sm">COD fees, return fees, and weight slabs per courier</p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-ink-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Courier</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">COD Fee %</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Min COD Fee</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider tabular">Return Fee</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider">Effective From</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-ink-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100">
                {couriers?.map((courier: CourierRateCard) => {
                  const isEditing = editingCourier?.courierCode === courier.courierCode
                  return (
                    <tr key={courier.id} className="hover:bg-ink-50">
                      <td className="px-4 py-3 font-medium text-ink-900">{courier.courierCode}</td>
                      {isEditing ? (
                        <CourierEditRow
                          courier={courier}
                          onSave={(data: CourierRateCardRequest) => updateCourierMutation.mutate(data)}
                          onCancel={() => setEditingCourier(null)}
                          isSaving={updateCourierMutation.isPending}
                        />
                      ) : (
                        <>
                          <td className="px-4 py-3 text-right tabular">{courier.codFeePct.toFixed(2)}%</td>
                          <td className="px-4 py-3 text-right tabular">{formatINR(courier.minCodFee)}</td>
                          <td className="px-4 py-3 text-right tabular">{formatINR(courier.returnFee)}</td>
                          <td className="px-4 py-3">{courier.effectiveFrom}</td>
                          <td className="px-4 py-3">
                            {courier.slabs && courier.slabs.length > 0 && (
                              <details className="group">
                                <summary className="cursor-pointer text-accent-600 hover:text-accent-700 text-sm font-medium flex items-center gap-1">
                                  <span>View {courier.slabs.length} slabs</span>
                                </summary>
                                <div className="absolute z-10 bg-white border border-ink-200 rounded-lg shadow-lg p-3 mt-1 min-w-[300px]">
                                  <div className="text-xs font-medium text-ink-900 mb-2">Weight Slabs</div>
                                  <div className="overflow-x-auto">
                                    <table className="w-full text-xs">
                                      <thead>
                                        <tr className="text-ink-500">
                                          <th className="text-left pb-1">Min Weight (kg)</th>
                                          <th className="text-right pb-1 tabular">Max Weight (kg)</th>
                                          <th className="text-right pb-1 tabular">Rate</th>
                                        </tr>
                                      </thead>
                                      <tbody>
                                        {courier.slabs.map((slab: WeightSlab) => (
                                          <tr key={slab.id} className="border-t border-ink-100">
                                            <td className="py-1 text-ink-900">{slab.minWeightKg.toFixed(3)}</td>
                                            <td className="py-1 text-right tabular">{slab.maxWeightKg.toFixed(3)}</td>
                                            <td className="py-1 text-right tabular">{formatINR(slab.rate)}</td>
                                          </tr>
                                        ))}
                                      </tbody>
                                    </table>
                                  </div>
                                </div>
                              </details>
                            )}
                            {isExecutive && (
                              <button
                                onClick={() => setEditingCourier(courier)}
                                className="text-accent-600 hover:text-accent-700 flex items-center gap-1 text-sm ml-2"
                              >
                                <Edit2 size={16} /> Edit
                              </button>
                            )}
                          </td>
                        </>
                      )}
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  )
}

interface ChannelEditRowProps {
  channel: ChannelRateCard
  onSave: (data: ChannelRateCardRequest) => void
  onCancel: () => void
  isSaving: boolean
}

function ChannelEditRow({ channel, onSave, onCancel, isSaving }: ChannelEditRowProps) {
  const [commissionPct, setCommissionPct] = useState(channel.commissionPct.toFixed(2))
  const [fixedFee, setFixedFee] = useState(channel.fixedFee.toFixed(2))
  const [collectionFeePct, setCollectionFeePct] = useState(channel.collectionFeePct.toFixed(2))
  const [minCollectionFee, setMinCollectionFee] = useState(channel.minCollectionFee.toFixed(2))

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSave({
      channelCode: channel.channelCode,
      commissionPct: parseFloat(commissionPct),
      fixedFee: parseFloat(fixedFee),
      collectionFeePct: parseFloat(collectionFeePct),
      minCollectionFee: parseFloat(minCollectionFee),
    })
  }

  return (
    <td colSpan={7} className="px-4 py-3 bg-amber-50">
      <form onSubmit={handleSubmit} className="flex flex-wrap gap-4 items-end">
        <div className="flex flex-col gap-1">
          <label className="text-xs text-ink-500">Commission %</label>
          <input
            type="number"
            step="0.01"
            min="0"
            value={commissionPct}
            onChange={(e) => setCommissionPct(e.target.value)}
            className="px-2 py-1 rounded border border-ink-300 text-sm w-28"
          />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs text-ink-500">Fixed Fee</label>
          <input
            type="number"
            step="0.01"
            min="0"
            value={fixedFee}
            onChange={(e) => setFixedFee(e.target.value)}
            className="px-2 py-1 rounded border border-ink-300 text-sm w-28"
          />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs text-ink-500">Collection %</label>
          <input
            type="number"
            step="0.01"
            min="0"
            value={collectionFeePct}
            onChange={(e) => setCollectionFeePct(e.target.value)}
            className="px-2 py-1 rounded border border-ink-300 text-sm w-28"
          />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs text-ink-500">Min Collection</label>
          <input
            type="number"
            step="0.01"
            min="0"
            value={minCollectionFee}
            onChange={(e) => setMinCollectionFee(e.target.value)}
            className="px-2 py-1 rounded border border-ink-300 text-sm w-28"
          />
        </div>
        <div className="flex gap-2">
          <button
            type="submit"
            disabled={isSaving}
            className="px-3 py-1.5 rounded bg-emerald-600 text-white text-sm hover:bg-emerald-700 disabled:opacity-50"
          >
            {isSaving ? <Loader2 size={14} className="animate-spin" /> : 'Save'}
          </button>
          <button
            type="button"
            onClick={onCancel}
            className="px-3 py-1.5 rounded border border-ink-300 text-sm text-ink-600 hover:bg-ink-100"
          >
            Cancel
          </button>
        </div>
      </form>
    </td>
  )
}

interface CourierEditRowProps {
  courier: CourierRateCard
  onSave: (data: CourierRateCardRequest) => void
  onCancel: () => void
  isSaving: boolean
}

function CourierEditRow({ courier, onSave, onCancel, isSaving }: CourierEditRowProps) {
  const [codFeePct, setCodFeePct] = useState(courier.codFeePct.toFixed(2))
  const [minCodFee, setMinCodFee] = useState(courier.minCodFee.toFixed(2))
  const [returnFee, setReturnFee] = useState(courier.returnFee.toFixed(2))

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSave({
      courierCode: courier.courierCode,
      codFeePct: parseFloat(codFeePct),
      minCodFee: parseFloat(minCodFee),
      returnFee: parseFloat(returnFee),
    })
  }

  return (
    <td colSpan={6} className="px-4 py-3 bg-amber-50">
      <form onSubmit={handleSubmit} className="flex flex-wrap gap-4 items-end">
        <div className="flex flex-col gap-1">
          <label className="text-xs text-ink-500">COD Fee %</label>
          <input
            type="number"
            step="0.01"
            min="0"
            value={codFeePct}
            onChange={(e) => setCodFeePct(e.target.value)}
            className="px-2 py-1 rounded border border-ink-300 text-sm w-28"
          />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs text-ink-500">Min COD Fee</label>
          <input
            type="number"
            step="0.01"
            min="0"
            value={minCodFee}
            onChange={(e) => setMinCodFee(e.target.value)}
            className="px-2 py-1 rounded border border-ink-300 text-sm w-28"
          />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs text-ink-500">Return Fee</label>
          <input
            type="number"
            step="0.01"
            min="0"
            value={returnFee}
            onChange={(e) => setReturnFee(e.target.value)}
            className="px-2 py-1 rounded border border-ink-300 text-sm w-28"
          />
        </div>
        <div className="flex gap-2">
          <button
            type="submit"
            disabled={isSaving}
            className="px-3 py-1.5 rounded bg-emerald-600 text-white text-sm hover:bg-emerald-700 disabled:opacity-50"
          >
            {isSaving ? <Loader2 size={14} className="animate-spin" /> : 'Save'}
          </button>
          <button
            type="button"
            onClick={onCancel}
            className="px-3 py-1.5 rounded border border-ink-300 text-sm text-ink-600 hover:bg-ink-100"
          >
            Cancel
          </button>
        </div>
      </form>
    </td>
  )
}

export default RateCards
