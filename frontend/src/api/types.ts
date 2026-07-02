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
