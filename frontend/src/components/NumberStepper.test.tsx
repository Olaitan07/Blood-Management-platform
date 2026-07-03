import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { NumberStepper } from './NumberStepper'

describe('NumberStepper', () => {
  it('increments and decrements the value via the +/- buttons', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<NumberStepper label="Units" value={5} onChange={onChange} />)

    await user.click(screen.getByLabelText('Increase units'))
    expect(onChange).toHaveBeenCalledWith(6)

    await user.click(screen.getByLabelText('Decrease units'))
    expect(onChange).toHaveBeenCalledWith(4)
  })

  it('clamps decrementing below min (default 0)', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<NumberStepper label="Units" value={0} onChange={onChange} />)

    await user.click(screen.getByLabelText('Decrease units'))
    expect(onChange).toHaveBeenCalledWith(0)
  })

  it('clamps incrementing above max', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<NumberStepper label="Units" value={10} onChange={onChange} max={10} />)

    await user.click(screen.getByLabelText('Increase units'))
    expect(onChange).toHaveBeenCalledWith(10)
  })

  it('clamps a directly typed value to the min/max bounds', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<NumberStepper label="Units" value={5} onChange={onChange} min={0} max={10} />)

    const input = screen.getByLabelText('Units')
    await user.clear(input)
    await user.type(input, '999')
    expect(onChange).toHaveBeenLastCalledWith(10)
  })

  it('renders the error message and marks the input as invalid', () => {
    render(<NumberStepper label="Units" value={5} onChange={vi.fn()} error="Required" />)
    const input = screen.getByLabelText('Units')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('alert')).toHaveTextContent('Required')
  })
})
