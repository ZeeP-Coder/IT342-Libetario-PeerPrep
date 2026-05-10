export type CurrentUser = {
  fullName: string
  email: string
}

const STORAGE_KEY = 'peerprep.currentUser'

export function getCurrentUser(): CurrentUser | null {
  if (typeof window === 'undefined') {
    return null
  }

  const rawUser = window.localStorage.getItem(STORAGE_KEY)
  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser) as CurrentUser
  } catch {
    window.localStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export function setCurrentUser(user: CurrentUser) {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
}

export function clearCurrentUser() {
  window.localStorage.removeItem(STORAGE_KEY)
}