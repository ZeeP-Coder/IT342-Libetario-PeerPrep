export type UserProfile = {
  fullName: string
  email: string
  university: string
  major: string
  googleAuth: boolean
}

export type UpdateUserProfilePayload = {
  email: string
  fullName: string
  university: string
  major: string
}

const API_BASE_URL =
  ((import.meta as unknown) as { env?: Record<string, string> }).env?.VITE_API_BASE_URL ??
  'http://localhost:8081'

async function requestJson<TResponse>(path: string, init?: RequestInit): Promise<TResponse> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
    credentials: 'include',
    ...init,
  })

  const responseText = await response.text()
  const responseBody = responseText ? (JSON.parse(responseText) as Record<string, unknown>) : {}

  if (!response.ok) {
    throw new Error(
      typeof responseBody.message === 'string'
        ? responseBody.message
        : 'Unable to complete the request right now',
    )
  }

  return responseBody as TResponse
}

export function fetchUserProfile(email: string) {
  return requestJson<UserProfile>(`/api/users/profile?email=${encodeURIComponent(email)}`)
}

export function updateUserProfile(payload: UpdateUserProfilePayload) {
  return requestJson<UserProfile>('/api/users/profile', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}
