import { describe, expect, it } from 'vitest'
import { decodeToken, isExpired } from './jwt'
import type { JwtClaims } from '@/api/types'

// Builds a syntactically valid (unsigned) JWT string from a claims object —
// decodeToken only ever reads the payload segment, never verifies the
// signature client-side (that's the backend's job on every request).
function fakeJwt(claims: JwtClaims): string {
  const base64url = (obj: object) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  return `${base64url({ alg: 'HS384', typ: 'JWT' })}.${base64url(claims)}.fake-signature`
}

describe('decodeToken', () => {
  it('decodes the payload claims from a JWT string', () => {
    const claims: JwtClaims = {
      sub: 'officer@test.com',
      role: 'OFFICER',
      userId: 42,
      name: 'Test Officer',
      iat: 1_751_500_000,
      exp: 1_751_600_000,
    }

    const decoded = decodeToken(fakeJwt(claims))

    expect(decoded).toEqual(claims)
  })
})

describe('isExpired', () => {
  it('returns true when exp is in the past', () => {
    const claims = { exp: Math.floor(Date.now() / 1000) - 60 } as JwtClaims
    expect(isExpired(claims)).toBe(true)
  })

  it('returns false when exp is in the future', () => {
    const claims = { exp: Math.floor(Date.now() / 1000) + 3600 } as JwtClaims
    expect(isExpired(claims)).toBe(false)
  })
})
