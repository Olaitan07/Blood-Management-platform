import { Link, useLocation } from 'react-router-dom'

interface LocationState {
  hospitalName?: string
  bloodGroup?: string
}

// Module 6 (Transfer Workflow) builds the real request form. This confirms
// the hand-off from Search carries the right context forward in the meantime.
export function NewTransferPlaceholderPage() {
  const location = useLocation()
  const { hospitalName, bloodGroup } = (location.state as LocationState | null) ?? {}

  return (
    <div className="mx-auto flex max-w-xl flex-col items-center px-6 py-24 text-center">
      <h1 className="text-xl font-semibold text-gray-100">Create transfer request</h1>
      <p className="mt-2 text-sm text-gray-400">
        {hospitalName && bloodGroup
          ? `Coming in Module 6. You selected ${bloodGroup} from ${hospitalName}.`
          : 'Coming in Module 6 — search for a blood group first to carry that selection forward.'}
      </p>
      <Link to="/search" className="mt-6 text-sm text-brand-500 hover:underline">
        ← Back to search
      </Link>
    </div>
  )
}
