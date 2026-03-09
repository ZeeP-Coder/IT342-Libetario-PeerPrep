export type LoginPayload = {
  email: string
  password: string
}

export type RegisterPayload = {
  fullName: string
  email: string
  university: string
  major: string
  password: string
}

export type AuthResponse = {
  success: boolean
  message: string
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081'

async function postAuth<TPayload>(path: string, payload: TPayload): Promise<AuthResponse> {
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(payload),
    })

    const data = (await response.json()) as Partial<AuthResponse>

    if (!response.ok) {
      throw new Error(typeof data.message === 'string' ? data.message : 'Authentication request failed')
    }

    return {
      success: Boolean(data.success),
      message: typeof data.message === 'string' ? data.message : 'Authentication request successful',
    }
  } catch (requestError) {
    if (requestError instanceof Error) {
      throw requestError
    }

    throw new Error('Authentication request failed')
  }
}

export async function login(payload: LoginPayload) {
  return postAuth('/api/auth/login', payload)
}

export async function register(payload: RegisterPayload) {
  return postAuth('/api/auth/register', payload)
}

export function getGoogleAuthUrl() {
  return `${API_BASE_URL}/api/auth/google`
}
