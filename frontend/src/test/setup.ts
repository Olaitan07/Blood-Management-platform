import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// @testing-library/react's auto-cleanup relies on a global `afterEach`, which
// doesn't exist since vite.config.ts intentionally omits `globals: true`
// (see comment there) — so it's registered explicitly here instead.
afterEach(() => {
  cleanup()
})
