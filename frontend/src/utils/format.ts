export function formatINR(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) return '—'
  const abs = Math.abs(value)
  const sign = value < 0 ? '−' : ''
  if (abs >= 10000000) {
    return `${sign}₹${(abs / 10000000).toFixed(2)} Cr`
  }
  if (abs >= 100000) {
    return `${sign}₹${(abs / 100000).toFixed(2)} L`
  }
  return `${sign}₹${abs.toLocaleString('en-IN', { maximumFractionDigits: 2 })}`
}

export function formatINRPlain(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) return ''
  return value.toLocaleString('en-IN', { maximumFractionDigits: 2 })
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}

export function maskName(name?: string | null): string {
  if (!name) return '—'
  const parts = name.trim().split(/\s+/)
  const first = parts[0]
  if (!first) return '—'
  return first[0] + '***'
}

export function maskPhone(phone?: string | null): string {
  if (!phone) return '—'
  const digits = phone.replace(/\D/g, '')
  if (digits.length <= 4) return '****'
  return `****${digits.slice(-4)}`
}

export function maskAddress(address?: string | null): string {
  if (!address) return '—'
  if (address.length <= 3) return '***'
  return address.slice(0, 3) + '***'
}

export function statusClass(status?: string): string {
  switch (status) {
    case 'SETTLED': return 'cell-settled'
    case 'SHORT_PAID': return 'cell-short'
    case 'UNPAID_PAST_DUE': return 'cell-overdue'
    case 'FREIGHT_OVERCHARGED': return 'cell-freight'
    case 'DRAFT': return 'bg-ink-100 text-ink-600'
    case 'SUBMITTED': return 'bg-blue-100 text-blue-700'
    case 'APPROVED': return 'bg-emerald-100 text-emerald-700'
    case 'REJECTED': return 'bg-red-100 text-red-700'
    case 'RECOVERED': return 'bg-teal-100 text-teal-700'
    default: return 'bg-ink-100 text-ink-600'
  }
}

export function statusLabel(status?: string): string {
  switch (status) {
    case 'SETTLED': return 'Settled'
    case 'SHORT_PAID': return 'Short paid'
    case 'UNPAID_PAST_DUE': return 'Unpaid past due'
    case 'FREIGHT_OVERCHARGED': return 'Freight overcharged'
    case 'RETURN_NOT_RECEIVED': return 'Return not received'
    case 'DRAFT': return 'Draft'
    case 'SUBMITTED': return 'Submitted'
    case 'APPROVED': return 'Approved'
    case 'REJECTED': return 'Rejected'
    case 'RECOVERED': return 'Recovered'
    default: return status || '—'
  }
}