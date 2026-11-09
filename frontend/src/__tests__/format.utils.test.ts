import { describe, it, expect } from 'vitest'
import { formatINR, formatINRPlain, formatDate, maskName, maskPhone, maskAddress, statusClass, statusLabel } from '../utils/format'

describe('formatINR', () => {
  it('formats large numbers as Cr', () => {
    expect(formatINR(1234567890)).toBe('₹123.46 Cr')
  })

  it('formats numbers as L', () => {
    expect(formatINR(123456)).toBe('₹1.23 L')
  })

  it('formats small numbers with en-IN formatting', () => {
    expect(formatINR(1234)).toBe('₹1,234')
  })

  it('returns dash for null', () => {
    expect(formatINR(null)).toBe('—')
  })

  it('returns dash for NaN', () => {
    expect(formatINR(NaN)).toBe('—')
  })
})

describe('formatINRPlain', () => {
  it('formats without currency prefix', () => {
    expect(formatINRPlain(123456)).toBe('1,23,456')
  })

  it('returns empty string for null', () => {
    expect(formatINRPlain(null)).toBe('')
  })
})

describe('formatDate', () => {
  it('formats date string', () => {
    expect(formatDate('2024-01-15')).toBe('15 Jan 2024')
  })

  it('returns original for invalid date', () => {
    expect(formatDate('invalid')).toBe('invalid')
  })
})

describe('maskName', () => {
  it('masks full name', () => {
    expect(maskName('John Doe')).toBe('J***')
  })

  it('masks partial name', () => {
    expect(maskName('Al')).toBe('A***')
  })

  it('returns dash for empty', () => {
    expect(maskName('')).toBe('—')
  })
})

describe('maskPhone', () => {
  it('masks phone showing last 4', () => {
    expect(maskPhone('+919876543210')).toBe('****3210')
  })

  it('masks short phone', () => {
    expect(maskPhone('1234')).toBe('****')
  })

  it('returns dash for empty', () => {
    expect(maskPhone('')).toBe('—')
  })
})

describe('maskAddress', () => {
  it('masks address showing first 3', () => {
    expect(maskAddress('123 Main St, City')).toBe('123***')
  })

  it('masks very short address', () => {
    expect(maskAddress('AB')).toBe('***')
  })

  it('returns dash for empty', () => {
    expect(maskAddress('')).toBe('—')
  })
})

describe('statusClass', () => {
  it('returns class for SETTLED', () => {
    expect(statusClass('SETTLED')).toBe('cell-settled')
  })

  it('returns class for SHORT_PAID', () => {
    expect(statusClass('SHORT_PAID')).toBe('cell-short')
  })

  it('returns class for UNPAID_PAST_DUE', () => {
    expect(statusClass('UNPAID_PAST_DUE')).toBe('cell-overdue')
  })

  it('returns class for FREIGHT_OVERCHARGED', () => {
    expect(statusClass('FREIGHT_OVERCHARGED')).toBe('cell-freight')
  })

  it('returns default class', () => {
    expect(statusClass('UNKNOWN')).toBe('bg-ink-100 text-ink-600')
  })
})

describe('statusLabel', () => {
  it('returns label for SETTLED', () => {
    expect(statusLabel('SETTLED')).toBe('Settled')
  })

  it('returns label for SHORT_PAID', () => {
    expect(statusLabel('SHORT_PAID')).toBe('Short paid')
  })

  it('returns label for UNPAID_PAST_DUE', () => {
    expect(statusLabel('UNPAID_PAST_DUE')).toBe('Unpaid past due')
  })

  it('returns label for FREIGHT_OVERCHARGED', () => {
    expect(statusLabel('FREIGHT_OVERCHARGED')).toBe('Freight overcharged')
  })

  it('returns the status itself for unknown', () => {
    expect(statusLabel('UNKNOWN')).toBe('UNKNOWN')
  })

  it('returns dash for empty', () => {
    expect(statusLabel('')).toBe('—')
  })
})