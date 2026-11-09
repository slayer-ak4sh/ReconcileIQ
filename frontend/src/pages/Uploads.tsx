import { useState, useRef } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, getApiError } from '../api/client'
import { Source, UploadBatch } from '../types'
import { UploadCloud, Loader2, Play, ArrowRight, RefreshCcw, Info, AlertCircle } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

const FILE_TYPE_HINTS: Record<string, string> = {
  ORDERS: 'CSV with: OrderID, ChannelCode, OrderDate, OrderValue, PaymentType, DeliveryStatus, CustomerName, CustomerPhone, CustomerAddress, expected_amount_override',
  SETTLEMENT: 'CSV with: OrderID, ReferenceNo, SettleDate, GrossAmt, Commission, FixedFee, CollFee, TCS, TDS, NetAmt',
  COD_REMITTANCE: 'CSV with: OrderID, CourierCode, UTRNo, RemitDate, CODCollected, FeeDeducted, NetRemitted',
  FREIGHT_INVOICE: 'CSV with: OrderID, CourierCode, InvoiceNo, InvoiceDate, BilledWeight, FreightCharge, CODFee, ReturnFee, TotalFreight',
}

export default function Uploads() {
  const queryClient = useQueryClient()
  const { hasRole } = useAuth()
  const [file, setFile] = useState<File | null>(null)
  const [sourceId, setSourceId] = useState<string>('')
  const [fileType, setFileType] = useState<string>('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  const { data: sources = [], error: sourceError, refetch: refetchSources } = useQuery<Source[]>({
    queryKey: ['sources'],
    queryFn: () => api.get('/uploads/sources').then((res) => {
      if (!Array.isArray(res.data)) return []
      return res.data
    }),
  })

  const { data: uploads = [], isLoading: loadingUploads } = useQuery<UploadBatch[]>({
    queryKey: ['uploads'],
    queryFn: () => api.get('/uploads').then((res) => {
      if (!Array.isArray(res.data)) return []
      return res.data
    }),
  })

  const uploadMutation = useMutation({
    mutationFn: (formData: FormData) => api.post('/uploads', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['uploads'] })
      setFile(null)
      if (response.data.batchId) {
        api.post(`/uploads/${response.data.batchId}/ingest`).catch(err => console.error('Auto-ingest failed', err))
      }
    }
  })

  const runMatchingMutation = useMutation({
    mutationFn: () => api.post(`/matching/run?asOfDate=${new Date().toISOString().split('T')[0]}`),
    onSuccess: (res) => {
      alert(`Matching Complete!\nTotal Orders: ${res.data.totalOrders}\nMatched: ${res.data.autoMatched}`)
      queryClient.invalidateQueries({ queryKey: ['uploads'] })
    }
  })

  const handleUpload = (e: React.FormEvent) => {
    e.preventDefault()
    if (!file) {
      fileInputRef.current?.click()
      return
    }
    if (!sourceId || !fileType) return

    const formData = new FormData()
    formData.append('file', file)
    formData.append('sourceId', sourceId)
    formData.append('fileType', fileType)
    uploadMutation.mutate(formData)
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0] || null
    setFile(selectedFile)
  }

  const canUpload = !!file && !!sourceId && !!fileType && !uploadMutation.isPending
  const showSelectFirstHint = !sourceId || !fileType

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4">
        <h1 className="text-2xl font-bold text-ink-900">File Uploads</h1>
        {hasRole('FINANCE_EXECUTIVE') && (
          <button
            onClick={() => runMatchingMutation.mutate()}
            disabled={runMatchingMutation.isPending}
            className="flex items-center gap-2 bg-accent-600 text-white px-4 py-2 rounded-md hover:bg-accent-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {runMatchingMutation.isPending ? <Loader2 className="animate-spin" size={18} /> : <Play size={18} />}
            Run Matching
          </button>
        )}
      </div>

      <div className="card p-6">
        <h2 className="text-lg font-semibold text-ink-900 mb-4">Upload New File</h2>

        {sourceError ? (
          <div className="p-4 bg-red-50 text-red-700 rounded-md flex items-center justify-between mb-4">
            <span>Failed to load sources.</span>
            <button onClick={() => refetchSources()} className="flex items-center gap-1 font-medium hover:underline">
              <RefreshCcw size={16} /> Retry
            </button>
          </div>
        ) : null}

        <form onSubmit={handleUpload} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-ink-700 mb-1">Source <span className="text-red-500">*</span></label>
              <select
                className="w-full rounded-md border border-ink-300 p-2 focus:ring-2 focus:ring-accent-500 focus:border-accent-500 outline-none"
                value={sourceId}
                onChange={(e) => setSourceId(e.target.value)}
                required
              >
                <option value="">Select Source</option>
                {sources.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-ink-700 mb-1">File Type <span className="text-red-500">*</span></label>
              <select
                className="w-full rounded-md border border-ink-300 p-2 focus:ring-2 focus:ring-accent-500 focus:border-accent-500 outline-none"
                value={fileType}
                onChange={(e) => setFileType(e.target.value)}
                required
              >
                <option value="">Select Type</option>
                <option value="ORDERS">Orders</option>
                <option value="SETTLEMENT">Settlement</option>
                <option value="COD_REMITTANCE">COD Remittance</option>
                <option value="FREIGHT_INVOICE">Freight Invoice</option>
              </select>
            </div>
          </div>

          {showSelectFirstHint && (
            <div className="p-3 bg-ink-50 text-ink-600 rounded-md flex items-center gap-2">
              <Info size={18} />
              <span className="text-sm">Select a source and file type first</span>
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">File <span className="text-red-500">*</span></label>
            {fileType && (
              <p className="text-xs text-ink-500 mb-2 font-mono">{FILE_TYPE_HINTS[fileType]}</p>
            )}
            <div
              onClick={() => { if (!showSelectFirstHint) fileInputRef.current?.click() }}
              className={`border-2 border-dashed rounded-md p-4 text-center cursor-pointer transition-colors flex items-center justify-center min-h-[120px] ${
                showSelectFirstHint ? 'border-ink-200 bg-ink-50 cursor-not-allowed' :
                file ? 'border-green-400 bg-green-50' : 'border-ink-200 hover:border-accent-400 hover:bg-ink-50'
              }`}
            >
              <input
                type="file"
                ref={fileInputRef}
                className="hidden"
                onChange={handleFileChange}
                disabled={showSelectFirstHint}
              />
              <div className="flex flex-col items-center gap-2">
                <UploadCloud size={32} className={file ? 'text-green-500' : 'text-ink-400'} />
                <span className="text-sm font-medium text-ink-700">
                  {file ? file.name : (showSelectFirstHint ? 'Select source and file type first' : 'Click to browse or drag & drop')}
                </span>
                {file && (
                  <button
                    type="button"
                    onClick={(e) => { e.preventDefault(); setFile(null) }}
                    className="text-xs text-ink-500 hover:text-ink-700 underline"
                  >
                    Remove
                  </button>
                )}
              </div>
            </div>
          </div>

          <button
            type="submit"
            disabled={!canUpload}
            className="w-full sm:w-auto flex items-center justify-center gap-2 bg-accent-600 text-white px-6 py-2.5 rounded-md hover:bg-accent-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {uploadMutation.isPending ? <Loader2 className="animate-spin" size={18} /> : <UploadCloud size={18} />}
            {file ? "Upload" : "Select File"}
          </button>
        </form>

        {uploadMutation.isError && (
          <div className="p-3 bg-red-50 text-red-700 rounded-md text-sm flex items-center gap-2">
            <AlertCircle size={18} />
            <span>{getApiError(uploadMutation.error).message}</span>
          </div>
        )}

        {uploadMutation.isSuccess && (
          <div className="p-4 bg-green-50 text-green-700 rounded-md flex items-center justify-between">
            <span className="text-sm font-medium">File uploaded and queued for ingestion successfully!</span>
            <Link to="/mapping" className="flex items-center gap-1 text-sm font-bold hover:underline">
              Go to Mapping <ArrowRight size={16} />
            </Link>
          </div>
        )}
      </div>

      <div className="card overflow-hidden">
        <div className="p-4 border-b border-ink-100 font-semibold text-ink-900">Recent Uploads</div>
        {loadingUploads ? (
          <div className="p-8 text-center text-ink-500">Loading...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-ink-50 text-ink-600 text-xs uppercase">
                <tr>
                  <th className="px-4 py-3 text-left">File Name</th>
                  <th className="px-4 py-3 text-left">Type</th>
                  <th className="px-4 py-3 text-center">Status</th>
                  <th className="px-4 py-3 text-right tabular">Rows (V/E)</th>
                  <th className="px-4 py-3 text-left">Uploaded Date</th>
                  <th className="px-4 py-3 text-left">Uploaded By</th>
                </tr>
              </thead>
              <tbody>
                {Array.isArray(uploads) && uploads.length > 0 ? (
                  uploads.map(u => (
                    <tr key={u.id} className="border-t border-ink-100 hover:bg-ink-50">
                      <td className="px-4 py-3 text-sm font-medium text-ink-900 truncate max-w-xs">{u.fileName}</td>
                      <td className="px-4 py-3 text-sm text-ink-500">{u.fileType}</td>
                      <td className="px-4 py-3 text-center">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                          u.status === 'COMPLETED' ? 'bg-green-100 text-green-700' :
                          u.status === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-blue-100 text-blue-700'
                        }`}>
                          {u.status}</span>
                      </td>
                      <td className="px-4 py-3 text-right tabular text-sm font-mono text-ink-600">
                        {u.validRowCount} / {u.errorCount}</td>
                      <td className="px-4 py-3 text-sm text-ink-600">
                        {u.createdAt ? new Date(u.createdAt).toLocaleString() : '—'}</td>
                      <td className="px-4 py-3 text-sm text-ink-600">
                        {u.uploadedBy || '—'}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="p-8 text-center text-ink-400">No uploads found</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
