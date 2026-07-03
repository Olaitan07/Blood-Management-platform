import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { formatDate, formatDateTime, formatRelativeTime } from './dateFormat'

describe('formatDate', () => {
  it('formats a plain ISO date with the short month by default', () => {
    expect(formatDate('2026-07-03')).toBe('Jul 3, 2026')
  })

  it('formats with the long month when requested', () => {
    expect(formatDate('2026-09-01', 'long')).toBe('September 1, 2026')
  })
})

describe('formatDateTime', () => {
  it('formats an ISO datetime with date and time', () => {
    // 10:32 UTC — assert on the date portion only, since the hour renders in
    // the test runner's local timezone and would otherwise make this test
    // environment-dependent.
    const result = formatDateTime('2026-01-15T10:32:00Z')
    expect(result).toContain('Jan 15, 2026')
  })
})

describe('formatRelativeTime', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-03T12:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns "just now" for a timestamp less than a minute ago', () => {
    expect(formatRelativeTime('2026-07-03T11:59:45Z')).toBe('just now')
  })

  it('returns minutes-ago for under an hour', () => {
    expect(formatRelativeTime('2026-07-03T11:45:00Z')).toBe('15 min ago')
  })

  it('returns hours-ago, pluralized correctly, for under a day', () => {
    expect(formatRelativeTime('2026-07-03T11:00:00Z')).toBe('1 hr ago')
    expect(formatRelativeTime('2026-07-03T09:00:00Z')).toBe('3 hrs ago')
  })

  it('returns "yesterday" for exactly one day ago', () => {
    expect(formatRelativeTime('2026-07-02T12:00:00Z')).toBe('yesterday')
  })

  it('returns days-ago for more than one day', () => {
    expect(formatRelativeTime('2026-06-28T12:00:00Z')).toBe('5 days ago')
  })
})
