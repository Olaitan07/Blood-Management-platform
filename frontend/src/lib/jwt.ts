import { jwtDecode } from 'jwt-decode'
import type { JwtClaims } from '@/api/types'

export function decodeToken(token: string): JwtClaims {
  return jwtDecode<JwtClaims>(token)
}

export function isExpired(claims: JwtClaims): boolean {
  return claims.exp * 1000 <= Date.now()
}
