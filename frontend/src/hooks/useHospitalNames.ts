import { useQuery } from '@tanstack/react-query'
import { listHospitals } from '@/api/hospitals'

// Transfer responses only carry hospital IDs, never names (verified against
// source — no name field exists on TransferResponse). Fetches the full list
// once (including inactive hospitals, since a transfer can reference one
// deactivated after the fact) and exposes a simple id -> name lookup.
export function useHospitalNames() {
  const query = useQuery({
    queryKey: ['hospitals', 'all'],
    queryFn: () => listHospitals(true),
    staleTime: 60_000,
  })

  function nameFor(hospitalId: number): string {
    return query.data?.find((h) => h.id === hospitalId)?.name ?? `Hospital #${hospitalId}`
  }

  return { nameFor, isLoading: query.isLoading }
}
