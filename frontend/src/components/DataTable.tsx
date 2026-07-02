import type { ReactNode } from 'react'

export interface DataTableColumn<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  className?: string
}

interface DataTableProps<T> {
  columns: DataTableColumn<T>[]
  rows: T[]
  rowKey: (row: T) => string | number
  isLoading?: boolean
  error?: string | null
  emptyMessage?: string
  onRetry?: () => void
}

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  isLoading,
  error,
  emptyMessage = 'No results found.',
  onRetry,
}: DataTableProps<T>) {
  return (
    <div className="overflow-x-auto rounded-lg border border-gray-800">
      <table className="min-w-full divide-y divide-gray-800 text-sm">
        <thead className="bg-gray-900">
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                scope="col"
                className="px-4 py-3 text-left font-medium text-gray-400"
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-800 bg-gray-950">
          {isLoading && (
            <tr>
              <td colSpan={columns.length} className="px-4 py-8 text-center text-gray-500">
                Loading…
              </td>
            </tr>
          )}

          {!isLoading && error && (
            <tr>
              <td colSpan={columns.length} className="px-4 py-8 text-center">
                <p className="text-red-400">{error}</p>
                {onRetry && (
                  <button
                    type="button"
                    onClick={onRetry}
                    className="mt-2 text-sm font-medium text-brand-500 hover:underline"
                  >
                    Retry
                  </button>
                )}
              </td>
            </tr>
          )}

          {!isLoading && !error && rows.length === 0 && (
            <tr>
              <td colSpan={columns.length} className="px-4 py-8 text-center text-gray-500">
                {emptyMessage}
              </td>
            </tr>
          )}

          {!isLoading &&
            !error &&
            rows.map((row) => (
              <tr key={rowKey(row)} className="hover:bg-gray-900/60">
                {columns.map((col) => (
                  <td key={col.key} className={`px-4 py-3 text-gray-200 ${col.className ?? ''}`}>
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  )
}
