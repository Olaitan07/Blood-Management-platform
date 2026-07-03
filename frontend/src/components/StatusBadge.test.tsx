import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatusBadge } from './StatusBadge'

describe('StatusBadge', () => {
  it.each([
    ['PENDING_APPROVAL', 'Pending'],
    ['ACTIVE', 'Active'],
    ['SUSPENDED', 'Suspended'],
    ['INACTIVE', 'Inactive'],
    ['AVAILABLE', 'Available'],
    ['EXPIRING_SOON', 'Expiring'],
    ['EXPIRED', 'Expired'],
    ['NO_STOCK', 'No stock'],
    ['PENDING', 'Pending'],
    ['APPROVED', 'Approved'],
    ['REJECTED', 'Rejected'],
    ['CANCELLED', 'Cancelled'],
    ['COMPLETED', 'Completed'],
    ['INSUFFICIENT_STOCK', 'Insufficient stock'],
  ])('renders the mapped label for %s', (status, label) => {
    render(<StatusBadge status={status} />)
    expect(screen.getByText(label)).toBeInTheDocument()
  })

  it('falls back to the raw status string for an unknown status', () => {
    render(<StatusBadge status="SOME_NEW_STATUS" />)
    expect(screen.getByText('SOME_NEW_STATUS')).toBeInTheDocument()
  })
})
