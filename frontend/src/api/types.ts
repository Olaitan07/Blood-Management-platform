// Mirrors the actual backend DTOs verified against source — not an idealized contract.

export type Role = 'DONOR' | 'CLINICIAN' | 'OFFICER' | 'ADMIN'

export type AccountStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'SUSPENDED'

export const ROLES_REQUIRING_HOSPITAL: Role[] = ['CLINICIAN', 'OFFICER']

export interface ApiResponse<T> {
  success: boolean
  message: string
  data?: T
  // Either a Map<string,string> of field/object-name -> message, or absent.
  errors?: Record<string, string>
  timestamp: string
}

export interface UserResponse {
  id: number
  name: string
  email: string
  role: Role
  hospitalId: number | null
  status: AccountStatus
  createdAt: string
}

export interface AuthResponse {
  token: string
  type: string
  userId: number
  name: string
  email: string
  role: Role
  accountStatus: AccountStatus
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
  role: Role
  hospitalId?: number | null
}

export interface LoginRequest {
  email: string
  password: string
}

export interface JwtClaims {
  sub: string
  role: Role
  userId: number
  name: string
  iat: number
  exp: number
}

export type HospitalStatus = 'ACTIVE' | 'INACTIVE'

export interface HospitalResponse {
  id: number
  name: string
  address: string
  state: string
  city: string
  contact: string
  status: HospitalStatus
  createdAt: string
}

export interface HospitalRequest {
  name: string
  address: string
  state: string
  city: string
  contact: string
}

export type BloodGroup = 'A+' | 'A-' | 'B+' | 'B-' | 'AB+' | 'AB-' | 'O+' | 'O-'

export const BLOOD_GROUPS: BloodGroup[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']

export type EligibilityStatus = 'ELIGIBLE' | 'NOT_ELIGIBLE'

export interface DonorResponse {
  id: number
  fullName: string
  bloodGroup: BloodGroup
  phone: string
  eligibilityStatus: EligibilityStatus
  eligibleFrom: string | null
  lastDonationDate: string | null
  createdAt: string
}

// No email field — the backend derives user_email from the authenticated
// principal on POST /api/donors, never from the request body.
export interface DonorRequest {
  fullName: string
  bloodGroup: BloodGroup
  phone: string
}

export interface DonationResponse {
  id: number
  donationDate: string
  hospitalName: string
  units: number
  createdAt: string
}

// Mirrors Spring Data's Page<T> JSON shape (GET /api/donors/{id}/donations
// is genuinely server-paginated, unlike most other list endpoints).
export interface PageResponse<T> {
  content: T[]
  totalPages: number
  totalElements: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
}

export type InventoryStatus = 'AVAILABLE' | 'EXPIRING_SOON' | 'EXPIRED'

export interface InventoryResponse {
  id: number
  hospitalId: number
  bloodGroup: BloodGroup
  unitsAvailable: number
  unitsReserved: number
  expiryDate: string
  status: InventoryStatus
  lastUpdated: string
  shelfLifeWarning: boolean
}

export interface AddInventoryRequest {
  bloodGroup: BloodGroup
  units: number
  expiryDate: string
  confirmShelfLife: boolean
}

export interface UpdateInventoryRequest {
  units: number
  reason: string
}

export interface AuditLogResponse {
  id: number
  inventoryId: number
  hospitalId: number
  bloodGroup: BloodGroup
  oldUnits: number
  newUnits: number
  reason: string
  changedBy: string
  changedAt: string
}

export interface BloodSearchResult {
  hospitalId: number
  hospitalName: string
  city: string
  state: string
  bloodGroup: BloodGroup
  availableUnits: number
  lastUpdated: string
}

// Real page-shaped response, but no distance/mileage field exists anywhere in
// it — the backend has no hospital coordinates, so "sorted by proximity" is a
// coarse same-city/same-state/other bucket, not a computed distance.
export interface BloodSearchResponse {
  bloodGroup: BloodGroup
  page: number
  size: number
  totalResults: number
  totalPages: number
  results: BloodSearchResult[]
  // Compatible donor groups to try instead — only populated when results is
  // empty, and can be zero (e.g. searching O-, which has no other donors),
  // one, or several (e.g. AB+ accepts from all 8 groups).
  suggestions: BloodGroup[] | null
}

// INSUFFICIENT_STOCK exists in the backend's state machine but no code path
// ever sets it today (the stock check happens at create time and rejects
// before a row is even persisted) — included defensively, not expected live.
export type TransferStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'COMPLETED'
  | 'EXPIRED'
  | 'INSUFFICIENT_STOCK'

// No hospital names anywhere on this DTO — only IDs. Resolve names client-side
// via the hospital list. Also no requestedByUserId — GET .../my-requests is
// genuinely hospital-scoped, not personal, so there's no way to filter this
// down to "requests I personally made" without a backend change.
export interface TransferResponse {
  id: number
  requestingHospitalId: number
  sourceHospitalId: number
  bloodGroup: BloodGroup
  quantity: number
  status: TransferStatus
  requestDate: string
  approvalDate?: string
  completionDate?: string
  rejectionReason?: string
  unitsReceived?: number
  idempotencyKey: string
}

export interface CreateTransferRequest {
  sourceHospitalId: number
  bloodGroup: BloodGroup
  quantity: number
  idempotencyKey: string
}

export interface RejectTransferRequest {
  reason: string
}

// DONOR notifications only ever have donorId set (a welcome message to that
// donor); TRANSFER notifications only ever have hospitalId set. "status" here
// is the SMS/email dispatch status (PENDING/SENT/FAILED/DEAD_LETTER) — it has
// nothing to do with read/unread, which is the separate `read` field.
export type NotificationType = 'DONOR' | 'TRANSFER'

export interface NotificationResponse {
  id: number
  recipient: string
  message: string
  status: string
  sentAt: string
  type: NotificationType
  donorId: number | null
  transferId: number | null
  read: boolean
}

export interface CompleteTransferRequest {
  unitsReceived: number
}

// Exhaustive — verified against every @ApplicationModuleListener in
// AuditEventListener. Inventory events are never audited (no dependency on
// inventory::events exists in the audit module at all), so there is no
// "Inventory" value and never will be under the current architecture.
export type AuditEventType =
  | 'BloodTransferRequestedEvent'
  | 'BloodTransferApprovedEvent'
  | 'BloodTransferRejectedEvent'
  | 'BloodTransferCompletedEvent'
  | 'BloodTransferCancelledEvent'
  | 'DonorRegisteredEvent'
  | 'HospitalRegisteredEvent'
  | 'HospitalDeactivatedEvent'
  | 'UserRegisteredEvent'
  | 'AdminAuditEvent'

export type AuditTargetType = 'TRANSFER' | 'DONOR' | 'HOSPITAL' | 'USER'

export interface AuditRecordResponse {
  id: number
  eventType: AuditEventType
  actor: string
  targetId: string
  targetType: AuditTargetType
  payload: string
  occurredAt: string
  receivedAt: string
}

export type ReportType = 'STOCK_LEVELS' | 'TRANSFERS' | 'DONORS' | 'EXPIRY_WASTE'

// One generic envelope for all 4 report types — there are no per-type DTOs.
// `rows` shape varies by type (verified against source):
//   STOCK_LEVELS / EXPIRY_WASTE -> structured objects (hospitalId, bloodGroup, ...)
//   TRANSFERS / DONORS          -> generic {metric, value} pairs, flattened
// `note` is non-null when the range had no matching data — render it, don't
// treat a low/zero totalRows as an error.
export interface ReportResult {
  type: ReportType
  from: string
  to: string
  totalRows: number
  rows: Record<string, string | number>[]
  note: string | null
}

export interface StockLevelRow {
  hospitalId: number
  hospitalName: string
  bloodGroup: BloodGroup
  unitsAvailable: number
  unitsReserved: number
  netAvailable: number
}

export interface ExpiryWasteRow {
  hospitalId: number
  hospitalName: string
  bloodGroup: BloodGroup
  wastedUnits: number
  expiryDate: string
}

// TRANSFERS/DONORS rows are {metric: string, value: number} — no fixed field
// set to type beyond that.
export interface MetricRow {
  metric: string
  value: number
}
