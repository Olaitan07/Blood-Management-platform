import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getDonationHistory, getMyDonorProfile } from '@/api/donors'
import { ApiError } from '@/api/client'
import { Pagination } from '@/components/Pagination'
import type { DonationResponse } from '@/api/types'

export function DonationHistoryPage() {
  const [page, setPage] = useState(1) // 1-based for display; converted to 0-based for the API
  const [pageSize, setPageSize] = useState(20)

  const profileQuery = useQuery({
    queryKey: ['donor', 'me'],
    queryFn: getMyDonorProfile,
    retry: false,
  })

  const donorId = profileQuery.data?.id

  const historyQuery = useQuery({
    queryKey: ['donor', 'donations', donorId, page, pageSize],
    queryFn: () => getDonationHistory(donorId as number, page - 1, pageSize),
    enabled: donorId !== undefined,
  })

  if (profileQuery.isError && (profileQuery.error as ApiError).status === 404) {
    return <Navigate to="/donor" replace />
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      <Link to="/donor" className="text-sm text-brand-500 hover:underline">
        ← Back to profile
      </Link>

      <h1 className="mt-4 text-xl font-semibold text-gray-100">Donation history</h1>

      <div className="mt-4">
        {(profileQuery.isLoading || historyQuery.isLoading) && <HistorySkeleton />}

        {historyQuery.isError && (
          <div className="rounded-lg border border-gray-800 px-4 py-8 text-center">
            <p className="text-red-400">Couldn&apos;t load your donation history.</p>
            <button
              type="button"
              onClick={() => historyQuery.refetch()}
              className="mt-2 text-sm font-medium text-brand-500 hover:underline"
            >
              Retry
            </button>
          </div>
        )}

        {historyQuery.isSuccess && historyQuery.data.empty && (
          <div className="rounded-lg border border-gray-800 px-4 py-8 text-center text-gray-500">
            No donations yet. Your history will appear here after your first donation.
          </div>
        )}

        {historyQuery.isSuccess && !historyQuery.data.empty && (
          <>
            {/* Card layout below sm — donor screens prioritize mobile over the
                horizontal-scroll-table pattern used on Admin/Officer screens. */}
            <ul className="flex flex-col gap-3 sm:hidden">
              {historyQuery.data.content.map((donation) => (
                <DonationCard key={donation.id} donation={donation} />
              ))}
            </ul>

            <div className="hidden overflow-x-auto rounded-lg border border-gray-800 sm:block">
              <table className="min-w-full divide-y divide-gray-800 text-sm">
                <thead className="bg-gray-900">
                  <tr>
                    <th scope="col" className="px-4 py-3 text-left font-medium text-gray-400">
                      Date
                    </th>
                    <th scope="col" className="px-4 py-3 text-left font-medium text-gray-400">
                      Hospital
                    </th>
                    <th scope="col" className="px-4 py-3 text-left font-medium text-gray-400">
                      Units
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-800 bg-gray-950">
                  {historyQuery.data.content.map((donation) => (
                    <tr key={donation.id}>
                      <td className="px-4 py-3 text-gray-200">{formatDate(donation.donationDate)}</td>
                      <td className="px-4 py-3 text-gray-200">{donation.hospitalName}</td>
                      <td className="px-4 py-3 text-gray-200">{donation.units}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Pagination
              page={page}
              pageCount={Math.max(1, historyQuery.data.totalPages)}
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
    </div>
  )
}

function DonationCard({ donation }: { donation: DonationResponse }) {
  return (
    <li className="rounded-lg border border-gray-800 bg-gray-950 p-4">
      <div className="flex items-center justify-between">
        <span className="font-medium text-gray-100">{formatDate(donation.donationDate)}</span>
        <span className="text-sm text-gray-400">{donation.units} unit{donation.units === 1 ? '' : 's'}</span>
      </div>
      <p className="mt-1 text-sm text-gray-400">{donation.hospitalName}</p>
    </li>
  )
}

function HistorySkeleton() {
  return (
    <div className="animate-pulse space-y-2">
      {[1, 2, 3].map((i) => (
        <div key={i} className="h-14 rounded-lg bg-gray-900" />
      ))}
    </div>
  )
}

function formatDate(isoDate: string): string {
  return new Date(`${isoDate}T00:00:00`).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}
