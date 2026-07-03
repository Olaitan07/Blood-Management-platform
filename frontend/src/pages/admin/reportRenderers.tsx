import { formatDate } from '@/lib/dateFormat'
import type { ExpiryWasteRow, ReportResult, StockLevelRow, TransferStatus } from '@/api/types'

// Canonical display order — the backend's byStatus map has no guaranteed
// iteration order once serialized, so this keeps the table stable regardless
// of what order the API happens to return rows in.
const STATUS_ORDER: TransferStatus[] = [
  'PENDING',
  'APPROVED',
  'COMPLETED',
  'REJECTED',
  'CANCELLED',
  'EXPIRED',
  'INSUFFICIENT_STOCK',
]

export function MetricCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-lg border border-gray-800 bg-gray-950 p-4">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-gray-100">{value}</p>
    </div>
  )
}

export function NoteBanner({ note }: { note: string | null }) {
  if (!note) return null
  return (
    <div className="mb-4 rounded-lg border border-gray-800 bg-gray-900 px-4 py-3 text-sm text-gray-300">
      {note}
    </div>
  )
}

function SimpleTable({ headers, rows }: { headers: string[]; rows: (string | number)[][] }) {
  return (
    <div className="overflow-x-auto rounded-lg border border-gray-800">
      <table className="w-full text-sm">
        <thead className="bg-gray-900 text-left text-xs text-gray-500">
          <tr>
            {headers.map((h) => (
              <th key={h} className="px-4 py-3 font-medium">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-800 bg-gray-950">
          {rows.map((row, i) => (
            <tr key={i}>
              {row.map((cell, j) => (
                <td key={j} className="px-4 py-3 text-gray-200">
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function formatMinutes(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h === 0) return `${m}m`
  return `${h}h ${m}m`
}

export function TransfersResult({ result }: { result: ReportResult }) {
  const rows = result.rows
  const total = Number(rows.find((r) => r.metric === 'Total Transfers')?.value ?? 0)
  const avgRow = rows.find((r) => r.metric === 'Avg Approval Time (minutes)')
  const avgMinutes = avgRow ? Number(avgRow.value) : null

  const byStatus = new Map<string, number>()
  for (const r of rows) {
    if (typeof r.metric === 'string' && r.metric.startsWith('Status: ')) {
      byStatus.set(r.metric.slice('Status: '.length), Number(r.value))
    }
  }
  const completed = byStatus.get('COMPLETED') ?? 0
  const rejected = byStatus.get('REJECTED') ?? 0

  return (
    <div className="flex flex-col gap-4">
      <NoteBanner note={result.note} />
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <MetricCard label="Total" value={total} />
        <MetricCard label="Completed" value={completed} />
        <MetricCard label="Rejected" value={rejected} />
        <MetricCard label="Avg approval time" value={avgMinutes !== null ? formatMinutes(avgMinutes) : '—'} />
      </div>
      <SimpleTable
        headers={['Status', 'Count']}
        rows={STATUS_ORDER.filter((s) => byStatus.has(s)).map((s) => [titleCase(s), byStatus.get(s) as number])}
      />
    </div>
  )
}

// The backend labels these with the Java enum constant name (verified live:
// "Blood Group A_POSITIVE"), not the A+/B+ symbol used everywhere else in
// this app — map it rather than show the raw enum token to the admin.
const BLOOD_GROUP_ENUM_LABELS: Record<string, string> = {
  A_POSITIVE: 'A+',
  A_NEGATIVE: 'A-',
  B_POSITIVE: 'B+',
  B_NEGATIVE: 'B-',
  AB_POSITIVE: 'AB+',
  AB_NEGATIVE: 'AB-',
  O_POSITIVE: 'O+',
  O_NEGATIVE: 'O-',
}

export function DonorsResult({ result }: { result: ReportResult }) {
  const rows = result.rows
  const totalAllTime = Number(rows.find((r) => r.metric === 'Total Donors (all time)')?.value ?? 0)
  const registeredInRange = Number(rows.find((r) => r.metric === 'Registered in Range')?.value ?? 0)
  const bloodGroupRows = rows
    .filter((r) => typeof r.metric === 'string' && r.metric.startsWith('Blood Group '))
    .map((r) => {
      const enumName = String(r.metric).replace('Blood Group ', '')
      return [BLOOD_GROUP_ENUM_LABELS[enumName] ?? enumName, Number(r.value)] as [string, number]
    })

  return (
    <div className="flex flex-col gap-4">
      <NoteBanner note={result.note} />
      <div className="grid grid-cols-2 gap-3">
        <MetricCard label="Total donors (all time)" value={totalAllTime} />
        <MetricCard label="Registered in this range" value={registeredInRange} />
      </div>
      <SimpleTable headers={['Blood group', 'Count']} rows={bloodGroupRows} />
    </div>
  )
}

export function StockLevelsResult({ result }: { result: ReportResult }) {
  const rows = result.rows as unknown as StockLevelRow[]
  const totalNet = rows.reduce((sum, r) => sum + r.netAvailable, 0)
  const hospitalCount = new Set(rows.map((r) => r.hospitalId)).size

  return (
    <div className="flex flex-col gap-4">
      <NoteBanner note={result.note} />
      <div className="grid grid-cols-2 gap-3">
        <MetricCard label="Hospitals reporting stock" value={hospitalCount} />
        <MetricCard label="Total net units available" value={totalNet} />
      </div>
      <SimpleTable
        headers={['Hospital', 'Blood group', 'Available', 'Reserved', 'Net available']}
        rows={rows.map((r) => [r.hospitalName, r.bloodGroup, r.unitsAvailable, r.unitsReserved, r.netAvailable])}
      />
    </div>
  )
}

export function ExpiryWasteResult({ result }: { result: ReportResult }) {
  const rows = result.rows as unknown as ExpiryWasteRow[]
  const totalWasted = rows.reduce((sum, r) => sum + r.wastedUnits, 0)

  return (
    <div className="flex flex-col gap-4">
      <NoteBanner note={result.note} />
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <MetricCard label="Total units wasted" value={totalWasted} />
      </div>
      <SimpleTable
        headers={['Hospital', 'Blood group', 'Units wasted', 'Expiry date']}
        rows={rows.map((r) => [r.hospitalName, r.bloodGroup, r.wastedUnits, formatDate(r.expiryDate)])}
      />
    </div>
  )
}

function titleCase(value: string): string {
  return value.charAt(0) + value.slice(1).toLowerCase().replace(/_/g, ' ')
}
