import { useState, useRef } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, getApiError } from '../api/client'
import { Source, UploadBatch } from '../types'
import { UploadCloud, Loader2, Play, ArrowRight, RefreshCcw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

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
      // Auto-ingest if the backend supports it or if required by the flow
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
    if (selectedFile && !sourceId) {
      alert("Please select a source first")
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-ink-900">File Uploads</h1>
        {hasRole('FINANCE_EXECUTIVE') && (
          <button
            onClick={() => runMatchingMutation.mutate()}
            disabled={runMatchingMutation.isPending}
            className="flex items-center gap-2 bg-accent-500 text-white px-4 py-2 rounded-md hover:bg-accent-600 disabled:opacity-50"
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

        <form onSubmit={handleUpload} className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">Source</label>
            <select
              className="w-full rounded-md border border-ink-300 p-2"
              value={sourceId}
              onChange={(e) => setSourceId(e.target.value)}
              required
            >
              <option value="">Select Source</option>
              {sources.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-ink-700 mb-1">File Type</label>
            <select
              className="w-full rounded-md border border-ink-300 p-2"
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
          <div
            onClick={() => fileInputRef.current?.click()}
            className="border-2 border-dashed border-ink-200 rounded-md p-2 text-center cursor-pointer hover:bg-ink-50 transition-colors h-[42px] flex items-center justify-center overflow-hidden"
          >
            <span className="text-xs text-ink-500 truncate">
              {file ? file.name : "Click to browse or drop file"}
            </span>
            <input
              type="file"
              ref={fileInputRef}
              className="hidden"
              onChange={handleFileChange}
            />
          </div>
          <button
            type="submit"
            disabled={uploadMutation.isPending}
            className="bg-accent-500 text-white px-4 py-2 rounded-md hover:bg-accent-600 disabled:opacity-50 flex items-center justify-center gap-2"
          >
            {uploadMutation.isPending ? <Loader2 className="animate-spin" size={18} /> : <UploadCloud size={18} />}
            {file ? "Upload" : "Select File"}
          </button>
        </form>

        {uploadMutation.isError && (
          <div className="mt-4 p-3 bg-red-50 text-red-700 rounded-md text-sm">
            {getApiError(uploadMutation.error).message}
          </div>
        )}

        {uploadMutation.isSuccess && (
          <div className="mt-4 p-4 bg-green-50 text-green-700 rounded-md flex items-center justify-between">
            <span className="text-sm font-medium">File uploaded and queued for ingestion successfully!</span>
            <Link to="/mapping" className="flex items-center gap-1 text-sm font-bold hover:underline">
              Go to Mapping <ArrowRight size={16} />
            </Link>
          </div>
        )}
      </div>

      <div className="card">
        <div className="p-6 border-b border-ink-100 font-semibold text-ink-900">Recent Uploads</div>
        {loadingUploads ? (
          <div className="p-8 text-center text-ink-500">Loading...</div>
        ) : (
          <table className="w-full">
            <thead className="bg-ink-50 text-ink-600 text-xs uppercase">
              <tr>
                <th className="px-6 py-3 text-left">File Name</th>
                <th className="px-6 py-3 text-left">Type</th>
                <th className="px-6 py-3 text-center">Status</th>
                <th className="px-6 py-3 text-right">Rows (V/E)</th>
              </tr>
            </thead>
            <tbody>
              {Array.isArray(uploads) && uploads.map(u => (
                <tr key={u.id} className="border-t border-ink-100 hover:bg-ink-50">
                  <td className="px-6 py-4 text-sm font-medium text-ink-900">{u.fileName}</td>
                  <td className="px-6 py-4 text-sm text-ink-500">{u.fileType}</td>
                  <td className="px-6 py-4 text-center">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      u.status === 'COMPLETED' ? 'bg-green-100 text-green-700' :
                      u.status === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-blue-100 text-blue-700'
                    }`}>
                      {u.status}</span>
                  </td>
                  <td className="px-6 py-4 text-sm text-right font-mono text-ink-600">
                    {u.validRowCount} / {u.errorCount}</td>
                </tr>
              ))}
              {uploads.length === 0 && (
                <tr>
                  <td colSpan={4} className="p-8 text-center text-ink-400">No uploads found</td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
