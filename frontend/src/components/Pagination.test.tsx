import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Pagination } from './Pagination'

describe('Pagination', () => {
  it('renders a button per page and marks the current page', () => {
    render(
      <Pagination page={2} pageCount={3} pageSize={10} onPageChange={vi.fn()} onPageSizeChange={vi.fn()} />,
    )
    expect(screen.getByRole('button', { name: '1' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '2' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('button', { name: '3' })).not.toHaveAttribute('aria-current')
  })

  it('disables Previous on the first page and Next on the last page', () => {
    render(
      <Pagination page={1} pageCount={3} pageSize={10} onPageChange={vi.fn()} onPageSizeChange={vi.fn()} />,
    )
    expect(screen.getByLabelText('Previous page')).toBeDisabled()
    expect(screen.getByLabelText('Next page')).not.toBeDisabled()
  })

  it('calls onPageChange with the adjacent page when Next/Previous are clicked', async () => {
    const user = userEvent.setup()
    const onPageChange = vi.fn()
    render(
      <Pagination page={2} pageCount={3} pageSize={10} onPageChange={onPageChange} onPageSizeChange={vi.fn()} />,
    )
    await user.click(screen.getByLabelText('Next page'))
    expect(onPageChange).toHaveBeenCalledWith(3)
    await user.click(screen.getByLabelText('Previous page'))
    expect(onPageChange).toHaveBeenCalledWith(1)
  })

  it('calls onPageChange with the clicked page number', async () => {
    const user = userEvent.setup()
    const onPageChange = vi.fn()
    render(
      <Pagination page={1} pageCount={3} pageSize={10} onPageChange={onPageChange} onPageSizeChange={vi.fn()} />,
    )
    await user.click(screen.getByRole('button', { name: '3' }))
    expect(onPageChange).toHaveBeenCalledWith(3)
  })

  it('calls onPageSizeChange with the numeric value when the page-size select changes', async () => {
    const user = userEvent.setup()
    const onPageSizeChange = vi.fn()
    render(
      <Pagination page={1} pageCount={3} pageSize={10} onPageChange={vi.fn()} onPageSizeChange={onPageSizeChange} />,
    )
    await user.selectOptions(screen.getByLabelText('Rows per page'), '50')
    expect(onPageSizeChange).toHaveBeenCalledWith(50)
  })
})
