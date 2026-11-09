import { useState, useEffect } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { Source, ColumnMapping, MappingSuggestion } from '../types'
import { CheckCircle2, Loader2, Save } from 'lucide-react'
import { AxiosError } from 'axios'

export default function Mapping() {
  const [sourceId, setSourceId] = useState<string>('')
  const [fileType, setFileType] = useState<string>('')
  const [mappings, setMappings] = useState<Record<string, string>>({})
  const [isAuthenticated, setIsAuthenticated] = useState(false)

  // Check authentication status
  useEffect(() => {
    const token = localStorage.getItem('reconcileiq_token')
    setIsAuthenticated(!!token)
    if (!token) {
      window.location.href = '/login'
    }
  }, [])

  const { data: sources = [] } = useQuery<Source[]>({
    queryKey: ['sources'],
    queryFn: () => api.get('/uploads/sources').then((res) => res.data),
  })

  const { data: activeMapping, refetch: refetchMapping } = useQuery<ColumnMapping>({
    queryKey: ['mapping', sourceId, fileType],
    queryFn: () => api.get(`/mappings/${sourceId}/${fileType}/active`).then((res) => res.data),
    enabled: !!sourceId && !!fileType && isAuthenticated,
  })

  const suggestMutation = useMutation({
    mutationFn: () => api.post(`/mappings/suggest?sourceId=${sourceId}&fileType=${fileType}`),
    onSuccess: (res) => {
      if (res.data && res.data.suggestions && Array.isArray(res.data.suggestions)) {
        const mapObj: Record<string, string> = {}
        res.data.suggestions.forEach((s: MappingSuggestion) => {
          if (s.canonicalField) {
            mapObj[s.canonicalField] = s.header
          }
        })
        setMappings(mapObj)
      }
    },
    onError: (error: Error) => {
      console.error('Mapping suggestion error:', error)
      const axiosError = error as AxiosError
      if (axiosError.response?.status === 401) {
        localStorage.removeItem('reconcileiq_token')
        window.location.href = '/login'
      }
    }
  })

  const approveMutation = useMutation({
    mutationFn: () => api.post('/mappings/approve', { sourceId: Number(sourceId), fileType, mappings }),
    onSuccess: () => {
      refetchMapping()
    },
    onError: (error) => {
      console.error('Mapping approval error:', error)
    }
  })

  if (!isAuthenticated) {
    return (
      <div className="card p-6 text-center">
        <h2 className="text-lg font-semibold text-ink-900 mb-2">Please log in first</h2>
        <p className="text-ink-500">You need to be authenticated to access column mapping.</p>
        <button
          onClick={() => window.location.href = '/login'}
          className="mt-4 bg-accent-500 text-white px-4 py-2 rounded-md hover:bg-accent-600"
        >
          Go to Login
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="card p-6">
        <div className="flex gap-4 items-end mb-6">
          <div className="flex-1">
            <label className="block text-sm font-medium text-ink-700 mb-1">Source</label>
            <select className="w-full rounded-md border border-ink-300 p-2" value={sourceId} onChange={(e) => setSourceId(e.target.value)}>
              <option value="">Select Source</option>
              {sources.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div className="flex-1">
            <label className="block text-sm font-medium text-ink-700 mb-1">File Type</label>
            <select className="w-full rounded-md border border-ink-300 p-2" value={fileType} onChange={(e) => setFileType(e.target.value)}>
              <option value="">Select Type</option>
              <option value="ORDERS">Orders</option>
              <option value="SETTLEMENT">Settlement</option>
              <option value="COD_REMITTANCE">COD Remittance</option>
              <option value="FREIGHT_INVOICE">Freight Invoice</option>
            </select>
          </div>
          <button
            disabled={!sourceId || !fileType || suggestMutation.isPending}
            onClick={() => suggestMutation.mutate()}
            className="bg-ink-100 text-ink-700 px-4 py-2 rounded-md hover:bg-ink-200"
          >
            {suggestMutation.isPending ? <Loader2 className="animate-spin" size={18} /> : 'Suggest'}
          </button>
        </div>

        {Object.keys(mappings).length > 0 && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              {Object.entries(mappings).map(([field, value]) => (
                <div key={field}>
                  <label className="block text-xs font-semibold text-ink-500 uppercase">{field}</label>
                  <input
                    className="w-full rounded-md border border-ink-300 p-2 text-sm"
                    value={value}
                    onChange={(e) => setMappings(m => ({ ...m, [field]: e.target.value }))}
                  />
                </div>
              ))}
            </div>
            <button
              onClick={() => approveMutation.mutate()}
              className="flex items-center gap-2 bg-accent-500 text-white px-4 py-2 rounded-md hover:bg-accent-600"
            >
              <Save size={18} /> Save Mapping
            </button>
          </div>
        )}

        {activeMapping && (
          <div className="mt-6 p-4 bg-green-50 text-green-700 rounded-md flex items-center gap-2">
            <CheckCircle2 size={18} /> Active mapping version {activeMapping.version} loaded
          </div>
        )}
      </div>
    </div>
  )
}
