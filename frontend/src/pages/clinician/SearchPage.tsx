import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { searchBlood } from '@/api/search'
import { BloodGroupChips } from '@/components/BloodGroupChips'
import { Pagination } from '@/components/Pagination'
import { CreateTransferModal } from './CreateTransferModal'
import { formatRelativeTime } from '@/lib/dateFormat'
import type { BloodGroup, BloodSearchResult } from '@/api/types'

export function SearchPage() {
  const [bloodGroup, setBloodGroup] = useState<BloodGroup | null>(null)
  const [page, setPage] = useState(1) // 1-based for display; converted to 0-based for the API
  const [pageSize, setPageSize] = useState(10)
  const [requestTarget, setRequestTarget] = useState<BloodSearchResult | null>(null)

  const searchQuery = useQuery({
    queryKey: ['search', 'blood', bloodGroup, page, pageSize],
    queryFn: () => searchBlood(bloodGroup as BloodGroup, page - 1, pageSize),
    enabled: bloodGroup !== null,
  })

  function selectGroup(group: BloodGroup) {
    setBloodGroup(group)
    setPage(1)
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-6 sm:px-6 sm:py-8">
      <div className="sticky top-0 z-10 -mx-4 bg-gray-950 px-4 pb-4 pt-1 sm:static sm:mx-0 sm:px-0">
        <h1 className="mb-4 text-2xl font-semibold text-gray-100">Search blood availability</h1>
        <BloodGroupChips label="Blood group" value={bloodGroup} onChange={selectGroup} />
      </div>

      <div className="mt-4">
        {bloodGroup === null && (
          <p className="rounded-lg border border-gray-800 px-4 py-8 text-center text-gray-400">
            Select a blood group to search availability.
          </p>
        )}

        {bloodGroup !== null && searchQuery.isLoading && <ResultsSkeleton />}

        {bloodGroup !== null && searchQuery.isError && (
          <div className="rounded-lg border border-gray-800 px-4 py-8 text-center">
            <p className="text-red-400">Couldn&apos;t load results.</p>
            <button
              type="button"
              onClick={() => searchQuery.refetch()}
              className="mt-2 text-sm font-medium text-brand-500 hover:underline"
            >
              Retry
            </button>
          </div>
        )}

        {searchQuery.isSuccess && searchQuery.data.results.length === 0 && (
          <NoResults data={searchQuery.data} onSelectSuggestion={selectGroup} />
        )}

        {searchQuery.isSuccess && searchQuery.data.results.length > 0 && (
          <>
            <p className="mb-3 text-sm text-gray-400">
              Showing {searchQuery.data.totalResults} hospital
              {searchQuery.data.totalResults === 1 ? '' : 's'} with {bloodGroup} available
            </p>
            <ul className="flex flex-col gap-3">
              {searchQuery.data.results.map((result) => (
                <ResultCard key={result.hospitalId} result={result} onRequest={() => setRequestTarget(result)} />
              ))}
            </ul>
            <Pagination
              page={page}
              pageCount={Math.max(1, searchQuery.data.totalPages)}
              pageSize={pageSize}
              onPageChange={setPage}
              onPageSizeChange={(size) => {
                setPageSize(size)
                setPage(1)
              }}
            />
          </>
        )}
      </div>

      <CreateTransferModal target={requestTarget} onClose={() => setRequestTarget(null)} />
    </div>
  )
}

function ResultCard({ result, onRequest }: { result: BloodSearchResult; onRequest: () => void }) {
  return (
    <li className="rounded-lg border border-gray-800 bg-gray-900 p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-semibold text-gray-100">{result.hospitalName}</p>
          <p className="mt-0.5 text-sm text-gray-400">
            {result.city}, {result.state} · Updated {formatRelativeTime(result.lastUpdated)}
          </p>
        </div>
        <span className="shrink-0 rounded-full bg-green-950 px-2.5 py-1 text-sm font-semibold text-green-400">
          {result.availableUnits} unit{result.availableUnits === 1 ? '' : 's'}
        </span>
      </div>
      <button
        type="button"
        onClick={onRequest}
        aria-label={`Request transfer from ${result.hospitalName}`}
        className="mt-3 w-full rounded-lg bg-white px-4 py-2.5 text-sm font-semibold text-gray-950 hover:bg-gray-200 sm:w-auto"
      >
        Request →
      </button>
    </li>
  )
}

function NoResults({
  data,
  onSelectSuggestion,
}: {
  data: { bloodGroup: BloodGroup; suggestions: BloodGroup[] | null }
  onSelectSuggestion: (group: BloodGroup) => void
}) {
  const suggestions = data.suggestions ?? []
  return (
    <div className="rounded-lg border border-gray-800 px-4 py-10 text-center">
      <p aria-hidden="true" className="text-3xl text-gray-600">
        ⃠
      </p>
      <p className="mt-2 font-semibold text-gray-200">No {data.bloodGroup} available right now.</p>
      {suggestions.length > 0 && (
        <>
          <p className="mt-1 text-sm text-gray-400">
            {suggestions.join(', ')} {suggestions.length === 1 ? 'is' : 'are'} compatible and might be available.
          </p>
          <div className="mt-4 flex flex-wrap justify-center gap-2">
            {suggestions.map((group) => (
              <button
                key={group}
                type="button"
                onClick={() => onSelectSuggestion(group)}
                className="rounded-lg border border-gray-700 px-4 py-2 text-sm font-semibold text-gray-100 hover:bg-gray-800"
              >
                Search {group} instead
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

function ResultsSkeleton() {
  return (
    <div className="animate-pulse space-y-3">
      {[1, 2, 3].map((i) => (
        <div key={i} className="h-24 rounded-lg bg-gray-900" />
      ))}
    </div>
  )
}
