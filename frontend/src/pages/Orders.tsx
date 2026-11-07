import { useState, useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  useReactTable,
  SortingState,
  ColumnFiltersState,
  PaginationState,
} from '@tanstack/react-table'
import { api } from '../api/client'
import { formatINR, formatDate, statusLabel, statusClass } from '../utils/format'
import { Search, ChevronUp, ChevronDown, X } from 'lucide-react'

interface OrderRow {
  orderId: string
  channelCode: string
  orderDate: string
  orderValue: number
  paymentType: string
  deliveryStatus: string
  reconciliationStatus: string
  dueDate: string
  expectedSettlement: number
  receivedSettlement: number
  settlementDiff: number
  totalTcs: number
  totalTds: number
}

interface OrdersResponse {
  content: OrderRow[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export default function Orders() {
  const [sorting, setSorting] = useState<SortingState>([{ id: 'dueDate', desc: true }])
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 })
  const [globalFilter, setGlobalFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [channelFilter, setChannelFilter] = useState('')

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['orders', sorting, columnFilters, pagination, globalFilter, statusFilter, channelFilter],
    queryFn: async () => {
      const params = new URLSearchParams()
      params.set('page', pagination.pageIndex.toString())
      params.set('size', pagination.pageSize.toString())
      if (sorting.length > 0) {
        params.set('sortBy', sorting[0].id)
        params.set('sortDir', sorting[0].desc ? 'desc' : 'asc')
      }
      if (globalFilter) params.set('search', globalFilter)
      if (statusFilter) params.set('status', statusFilter)
      if (channelFilter) params.set('channel', channelFilter)
      const res = await api.get<OrdersResponse>(`/orders?${params.toString()}`)
      return res.data
    },
    placeholderData: (prev) => prev,
  })

  const columns = useMemo<ColumnDef<OrderRow>[]>(() => [
    {
      accessorKey: 'orderId',
      header: 'Order ID',
      cell: info => <span className="font-mono text-sm">{info.getValue() as string}</span>,
      size: 160,
    },
    {
      accessorKey: 'channelCode',
      header: 'Channel',
      cell: info => <span className="text-sm">{info.getValue() as string}</span>,
      size: 140,
    },
    {
      accessorKey: 'orderDate',
      header: 'Order Date',
      cell: info => <span className="text-sm">{formatDate(info.getValue() as string)}</span>,
      size: 120,
    },
    {
      accessorKey: 'orderValue',
      header: 'Order Value',
      cell: info => <span className="text-right tabular text-sm">{formatINR(info.getValue() as number)}</span>,
      size: 130,
    },
    {
      accessorKey: 'paymentType',
      header: 'Payment',
      cell: info => <span className="text-sm capitalize">{info.getValue() as string}</span>,
      size: 100,
    },
    {
      accessorKey: 'deliveryStatus',
      header: 'Delivery',
      cell: info => <span className="text-sm">{(info.getValue() as string)?.replace('_', ' ').toLowerCase()}</span>,
      size: 140,
    },
    {
      accessorKey: 'reconciliationStatus',
      header: 'Status',
      cell: info => {
        const status = info.getValue() as string
        return (
          <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${statusClass(status)}`}>
            {statusLabel(status)}
          </span>
        )
      },
      size: 140,
    },
    {
      accessorKey: 'dueDate',
      header: 'Due Date',
      cell: info => <span className="text-sm">{formatDate(info.getValue() as string)}</span>,
      size: 120,
    },
    {
      accessorKey: 'expectedSettlement',
      header: 'Expected',
      cell: info => <span className="text-right tabular text-sm">{formatINR(info.getValue() as number)}</span>,
      size: 130,
    },
    {
      accessorKey: 'receivedSettlement',
      header: 'Received',
      cell: info => <span className="text-right tabular text-sm">{formatINR(info.getValue() as number)}</span>,
      size: 130,
    },
    {
      accessorKey: 'settlementDiff',
      header: 'Difference',
      cell: info => {
        const diff = info.getValue() as number
        const isNegative = diff < 0
        return (
          <span className={`text-right tabular text-sm font-medium ${isNegative ? 'text-red-600' : 'text-emerald-600'}`}>
            {isNegative ? '−' : '+'}{formatINR(Math.abs(diff))}
          </span>
        )
      },
      size: 130,
    },
    {
      accessorKey: 'totalTcs',
      header: 'TCS',
      cell: info => <span className="text-right tabular text-sm text-ink-500">{formatINR(info.getValue() as number)}</span>,
      size: 100,
    },
    {
      accessorKey: 'totalTds',
      header: 'TDS',
      cell: info => <span className="text-right tabular text-sm text-ink-500">{formatINR(info.getValue() as number)}</span>,
      size: 100,
    },
  ], [])

  const table = useReactTable({
    data: data?.content || [],
    columns,
    state: { sorting, columnFilters, pagination, globalFilter },
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    onPaginationChange: setPagination,
    onGlobalFilterChange: setGlobalFilter,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    pageCount: data?.totalPages || 1,
  })

  if (isLoading && !data) {
    return (
      <div className="min-h-screen bg-ink-50 p-8">
        <header className="border-b border-ink-200 p-4 mb-6">
          <h1 className="text-2xl font-semibold text-ink-900">Orders</h1>
          <p className="text-ink-500 text-sm">Order-level reconciliation with filters and sorting</p>
        </header>
        <div className="card p-6">
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="flex items-center gap-4">
                <div className="w-32 h-4 bg-ink-200 animate-pulse rounded" />
                <div className="w-24 h-4 bg-ink-200 animate-pulse rounded" />
                <div className="w-20 h-4 bg-ink-200 animate-pulse rounded" />
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
        <p className="text-ink-600 text-lg">{error?.message || 'Failed to load orders'}</p>
        <button onClick={() => window.location.reload()} className="mt-4 bg-accent-600 hover:bg-accent-700 text-white px-4 py-2 rounded-md text-sm font-medium">
          Retry
        </button>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-ink-50">
      <header className="border-b border-ink-200 p-4">
        <h1 className="text-2xl font-semibold text-ink-900">Orders</h1>
        <p className="text-ink-500 text-sm">Order-level reconciliation with filters and sorting</p>
      </header>

      <div className="p-4">
        <div className="card p-4 mb-6">
          <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center">
            <div className="relative flex-1 max-w-xs">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" size={18} />
              <input
                type="text"
                placeholder="Search orders..."
                value={globalFilter}
                onChange={(e) => setGlobalFilter(e.target.value)}
                className="w-full pl-10 pr-4 py-2 rounded-md border border-ink-300 focus:ring-2 focus:ring-accent-500 focus:border-accent-500 outline-none text-sm"
              />
            </div>
            <div className="flex gap-3 flex-wrap">
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="px-3 py-2 rounded-md border border-ink-300 focus:ring-2 focus:ring-accent-500 focus:border-accent-500 outline-none text-sm"
              >
                <option value="">All Statuses</option>
                <option value="SETTLED">Settled</option>
                <option value="SHORT_PAID">Short Paid</option>
                <option value="UNPAID_PAST_DUE">Unpaid Past Due</option>
                <option value="FREIGHT_OVERCHARGED">Freight Overcharged</option>
              </select>
              <select
                value={channelFilter}
                onChange={(e) => setChannelFilter(e.target.value)}
                className="px-3 py-2 rounded-md border border-ink-300 focus:ring-2 focus:ring-accent-500 focus:border-accent-500 outline-none text-sm"
              >
                <option value="">All Channels</option>
                <option value="Website">Website</option>
                <option value="Marketplace 1">Marketplace 1</option>
                <option value="Marketplace 2">Marketplace 2</option>
                <option value="QuickCommerce">QuickCommerce</option>
                <option value="Supermarket">Supermarket</option>
              </select>
              {(globalFilter || statusFilter || channelFilter) && (
                <button
                  onClick={() => { setGlobalFilter(''); setStatusFilter(''); setChannelFilter(''); }}
                  className="flex items-center gap-1 px-3 py-2 rounded-md border border-ink-300 text-ink-600 hover:bg-ink-100 text-sm"
                >
                  <X size={16} /> Clear
                </button>
              )}
            </div>
          </div>
        </div>

        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-ink-50">
                {table.getHeaderGroups().map(headerGroup => (
                  <tr key={headerGroup.id}>
                    {headerGroup.headers.map(header => (
                      <th
                        key={header.id}
                        className="px-4 py-3 text-left text-xs font-medium text-ink-500 uppercase tracking-wider cursor-pointer hover:bg-ink-100 select-none"
                        style={{ width: header.getSize() }}
                        onClick={header.column.getToggleSortingHandler()}
                      >
                        <div className="flex items-center gap-1">
                          <span>{flexRender(header.column.columnDef.header, header.getContext()) as React.ReactNode}</span>
                          {header.column.getIsSorted() && (
                            header.column.getIsSorted() === 'asc' ? (
                              <ChevronUp className="w-4 h-4 text-ink-500" />
                            ) : (
                              <ChevronDown className="w-4 h-4 text-ink-500" />
                            )
                          )}
                        </div>
                      </th>
                    ))}
                  </tr>
                ))}
              </thead>
              <tbody className="divide-y divide-ink-100">
                {table.getRowModel().rows.length === 0 ? (
                  <tr>
                    <td colSpan={columns.length} className="px-4 py-12 text-center text-ink-500">
                      No orders found
                    </td>
                  </tr>
                ) : (
                  table.getRowModel().rows.map(row => (
                    <tr key={row.id} className="hover:bg-ink-50">
                      {row.getVisibleCells().map(cell => (
                        <td key={cell.id} className="px-4 py-3">
                          {flexRender(cell.column.columnDef.cell, cell.getContext()) as React.ReactNode}
                        </td>
                      ))}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <div className="border-t border-ink-200 px-4 py-3 flex items-center justify-between">
            <div className="text-sm text-ink-500">
              Page {table.getState().pagination.pageIndex + 1} of {table.getPageCount()}
              (Total: {data?.totalElements || 0})
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => table.previousPage()}
                disabled={!table.getCanPreviousPage()}
                className="px-3 py-1 rounded border border-ink-300 text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-ink-50"
              >
                Previous
              </button>
              <button
                onClick={() => table.nextPage()}
                disabled={!table.getCanNextPage()}
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