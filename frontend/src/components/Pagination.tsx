interface PaginationProps {
  page: number
  pageCount: number
  pageSize: number
  onPageChange: (page: number) => void
  onPageSizeChange: (size: number) => void
  pageSizeOptions?: number[]
}

// Purely client-side paging control — several backend list endpoints return
// unbounded raw lists (see CLAUDE.md notes on inconsistent pagination), so
// screens built against them page the already-fetched array in memory.
export function Pagination({
  page,
  pageCount,
  pageSize,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = [10, 20, 50],
}: PaginationProps) {
  return (
    <div className="flex items-center justify-between px-1 py-3 text-sm text-gray-400">
      <div className="flex items-center gap-1">
        <button
          type="button"
          aria-label="Previous page"
          disabled={page <= 1}
          onClick={() => onPageChange(page - 1)}
          className="rounded p-1.5 hover:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-40"
        >
          ‹
        </button>
        {Array.from({ length: pageCount }, (_, i) => i + 1).map((p) => (
          <button
            key={p}
            type="button"
            aria-current={p === page ? 'page' : undefined}
            onClick={() => onPageChange(p)}
            className={`h-7 w-7 rounded-md text-sm ${
              p === page ? 'bg-white font-semibold text-gray-950' : 'hover:bg-gray-800'
            }`}
          >
            {p}
          </button>
        ))}
        <button
          type="button"
          aria-label="Next page"
          disabled={page >= pageCount}
          onClick={() => onPageChange(page + 1)}
          className="rounded p-1.5 hover:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-40"
        >
          ›
        </button>
      </div>

      <select
        aria-label="Rows per page"
        value={pageSize}
        onChange={(e) => onPageSizeChange(Number(e.target.value))}
        className="rounded-md border border-gray-700 bg-gray-900 px-2 py-1 text-gray-300"
      >
        {pageSizeOptions.map((size) => (
          <option key={size} value={size}>
            {size} per page
          </option>
        ))}
      </select>
    </div>
  )
}
