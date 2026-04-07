export type CurrentUser = {
  fullName: string
  email: string
}

const STORAGE_KEY = 'peerprep.currentUser'

type SessionListener = (user: CurrentUser | null) => void

class SessionManager {
  private static instance: SessionManager | null = null
  private listeners = new Set<SessionListener>()

  private constructor() {}

  static getInstance() {
    if (!SessionManager.instance) {
      SessionManager.instance = new SessionManager()
    }
    return SessionManager.instance
  }

  getCurrentUser(): CurrentUser | null {
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

  setCurrentUser(user: CurrentUser) {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    }
    this.notify(user)
  }

  clearCurrentUser() {
    if (typeof window !== 'undefined') {
      window.localStorage.removeItem(STORAGE_KEY)
    }
    this.notify(null)
  }

  subscribe(listener: SessionListener) {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  private notify(user: CurrentUser | null) {
    this.listeners.forEach((listener) => listener(user))
  }
}

const sessionManager = SessionManager.getInstance()

export function getCurrentUser(): CurrentUser | null {
  return sessionManager.getCurrentUser()
}

export function setCurrentUser(user: CurrentUser) {
  sessionManager.setCurrentUser(user)
}

export function clearCurrentUser() {
  sessionManager.clearCurrentUser()
}

export function subscribeToSessionChanges(listener: SessionListener) {
  return sessionManager.subscribe(listener)
}
