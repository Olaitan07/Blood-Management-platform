import { describe, expect, it } from 'vitest'
import { getNotificationLink } from './notificationLinks'
import type { NotificationResponse } from '@/api/types'

function notification(overrides: Partial<NotificationResponse>): NotificationResponse {
  return {
    id: 1,
    recipient: 'hospital:8',
    message: 'placeholder',
    status: 'SENT',
    sentAt: '2026-07-03T10:00:00Z',
    type: 'TRANSFER',
    donorId: null,
    transferId: 1,
    read: false,
    ...overrides,
  }
}

describe('getNotificationLink', () => {
  it('routes DONOR notifications to the donor profile', () => {
    const n = notification({ type: 'DONOR', message: 'Welcome Jane! Your donor registration is confirmed.' })
    expect(getNotificationLink(n)).toBe('/donor')
  })

  it('routes a "New blood transfer request" notification to Pending Requests (source hospital)', () => {
    const n = notification({
      type: 'TRANSFER',
      message: 'New blood transfer request #6: 1 units of A+ requested from your hospital by hospital #4',
    })
    expect(getNotificationLink(n)).toBe('/transfers/pending')
  })

  it('routes an approved-transfer notification to Requests (requesting hospital)', () => {
    const n = notification({
      type: 'TRANSFER',
      message: 'Transfer request #6 approved: 1 units of A+ are being dispatched from hospital #8',
    })
    expect(getNotificationLink(n)).toBe('/transfers/my-requests')
  })

  it('routes a rejected-transfer notification to Requests', () => {
    const n = notification({
      type: 'TRANSFER',
      message: 'Transfer request #6 rejected by hospital #8. Reason: out of stock. Please try another hospital.',
    })
    expect(getNotificationLink(n)).toBe('/transfers/my-requests')
  })

  it('routes a completed-transfer notification to Requests', () => {
    const n = notification({
      type: 'TRANSFER',
      message: 'Transfer #6 completed: 1 of 1 units of A+ received from hospital #8',
    })
    expect(getNotificationLink(n)).toBe('/transfers/my-requests')
  })

  it('routes a cancelled-transfer notification to Requests', () => {
    const n = notification({
      type: 'TRANSFER',
      message: 'Transfer request #6 for 1 units of A+ has been cancelled',
    })
    expect(getNotificationLink(n)).toBe('/transfers/my-requests')
  })

  it('returns null for an unrecognized notification type', () => {
    // @ts-expect-error — exercising the defensive fallback for a value
    // outside the real DONOR|TRANSFER union.
    const n = notification({ type: 'SOMETHING_ELSE' })
    expect(getNotificationLink(n)).toBeNull()
  })
})
