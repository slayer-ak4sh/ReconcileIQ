export type Role = 'FINANCE_EXECUTIVE' | 'COFOUNDER'

export interface AuthUser {
  email: string
  role: Role
  fullName: string
}

export interface User {
  email: string
  role: Role
  fullName: string
}

export interface Order {
  id: number
  orderId: string
  channelCode: string
  orderDate: string
  orderValue: number
  paymentType: 'COD' | 'PREPAID'
  deliveryStatus: 'DELIVERED' | 'RETURNED_TO_WAREHOUSE' | 'IN_TRANSIT' | 'CANCELLED'
  customerName?: string | null
  customerPhone?: string | null
  customerAddress?: string | null
  maskedName?: string
  maskedPhone?: string
}

export interface OrderReconciliation {
  id: number
  orderId: string
  dueDate: string
  expectedSettlement: number
  expectedFreight: number
  receivedSettlement: number
  receivedFreight: number
  settlementDiff: number
  freightDiff: number
  totalTcs: number
  totalTds: number
  reconciliationStatus: 'SETTLED' | 'SHORT_PAID' | 'UNPAID_PAST_DUE' | 'FREIGHT_OVERCHARGED'
  isMatched: boolean
  matchedAt?: string
}

export interface SettlementRow {
  id: number
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

export interface CodRemittanceRow {
  id: number
  utrNo?: string | null
  remittanceDate: string
  codCollected: number
  feeDeducted: number
  netRemitted: number
}

export interface FreightInvoiceRow {
  id: number
  invoiceNo?: string | null
  invoiceDate: string
  billedWeight: number
  freightCharge: number
  codFee: number
  returnFee: number
  totalFreight: number
}

export interface OrderDetail extends Order {
  reconciliation: OrderReconciliation
  settlements: SettlementRow[]
  codRemittances: CodRemittanceRow[]
  freightInvoices: FreightInvoiceRow[]
}

export interface UploadBatch {
  id: number
  sourceId: number
  fileType: string
  fileName: string
  fileHash: string
  rowCount: number
  validRowCount: number
  errorCount: number
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED'
  uploadedBy: string
  createdAt: string
}

export interface Source {
  id: number
  name: string
  code: string
  partnerType: 'CHANNEL' | 'COURIER'
  channelCode?: string | null
  courierCode?: string | null
}

export interface ColumnMapping {
  id: number
  sourceId: number
  fileType: string
  version: number
  mappings: Record<string, string>
  isActive: boolean
  createdBy: string
  approvedAt: string
}

export interface MappingSuggestion {
  header: string
  canonicalField: string
  confidence: number
  reason: string
}

export interface MappingSuggestionResponse {
  sourceId: number
  fileType: string
  suggestions: MappingSuggestion[]
  previewRows: Record<string, string>[]
  allMapped: boolean
}

export interface ChannelRateCard {
  id: number
  channelCode: string
  commissionPct: number
  fixedFee: number
  collectionFeePct: number
  minCollectionFee: number
  effectiveFrom: string
}

export interface CourierRateCard {
  id: number
  courierCode: string
  codFeePct: number
  minCodFee: number
  returnFee: number
  effectiveFrom: string
  slabs: { id: number; minWeightKg: number; maxWeightKg: number; rate: number }[]
}

export interface ReviewQueueItem {
  id: number
  batchId: number
  fileType: string
  sourceId: number
  suggestedOrderId?: string | null
  confidence?: number | null
  reason?: string | null
  status: 'PENDING' | 'RESOLVED' | 'IGNORED'
  reviewedBy?: string | null
  reviewedAt?: string | null
}

export interface Claim {
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

export interface ClaimItem {
  id: number
  orderId: string
  claimType: string
  amount: number
  evidenceNotes?: string | null
}

export interface ClaimEvent {
  id: number
  fromStatus: string
  toStatus: string
  actorEmail: string
  notes?: string | null
  createdAt: string
}

export interface DashboardSummary {
  totalOrders: number
  autoMatched: number
  autoMatchRate: number
  totalLeakage: number
  moneyAtRisk: number
  openClaimsValue: number
  recoveredValue: number
  identifiedValue: number
  byStatus: Record<string, number>
}

export interface MoneyAtRiskBucket {
  partnerCode: string
  partnerType: string
  bucket0to7: number
  bucket8to15: number
  bucket16to30: number
  bucket31plus: number
  total: number
}

export interface TaxSummary {
  partnerCode: string
  partnerType: string
  totalTcs: number
  totalTds: number
}
