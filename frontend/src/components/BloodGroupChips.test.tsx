import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BloodGroupChips } from './BloodGroupChips'

describe('BloodGroupChips', () => {
  it('renders a radio option per blood group', () => {
    render(<BloodGroupChips value={null} onChange={vi.fn()} label="Blood group" />)
    const radios = screen.getAllByRole('radio')
    expect(radios).toHaveLength(8)
    expect(screen.getByRole('radiogroup', { name: 'Blood group' })).toBeInTheDocument()
  })

  it('marks the selected group as checked', () => {
    render(<BloodGroupChips value="B+" onChange={vi.fn()} label="Blood group" />)
    expect(screen.getByRole('radio', { name: 'B+' })).toHaveAttribute('aria-checked', 'true')
    expect(screen.getByRole('radio', { name: 'A+' })).toHaveAttribute('aria-checked', 'false')
  })

  it('calls onChange with the clicked group', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<BloodGroupChips value={null} onChange={onChange} label="Blood group" />)
    await user.click(screen.getByRole('radio', { name: 'O-' }))
    expect(onChange).toHaveBeenCalledWith('O-')
  })

  it('moves selection to the next group on ArrowRight and wraps around at the end', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<BloodGroupChips value="A+" onChange={onChange} label="Blood group" />)
    screen.getByRole('radio', { name: 'A+' }).focus()
    await user.keyboard('{ArrowRight}')
    expect(onChange).toHaveBeenCalledWith('A-')

    onChange.mockClear()
    render(<BloodGroupChips value="O-" onChange={onChange} label="Blood group" />)
    screen.getAllByRole('radio', { name: 'O-' })[0].focus()
    await user.keyboard('{ArrowRight}')
    expect(onChange).toHaveBeenCalledWith('A+')
  })

  it('moves selection to the previous group on ArrowLeft and wraps around at the start', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<BloodGroupChips value="A+" onChange={onChange} label="Blood group" />)
    screen.getByRole('radio', { name: 'A+' }).focus()
    await user.keyboard('{ArrowLeft}')
    expect(onChange).toHaveBeenCalledWith('O-')
  })
})
