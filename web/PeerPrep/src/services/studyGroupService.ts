export type StudyGroup = {
  id: number
  subject: string
  description: string
  day: string
  meetingTime: string
  location: string
  maxMembers: number
  currentMembers: number
  status: string
  joined: boolean
  ownedByCurrentUser: boolean
  joinable: boolean
  createdByName: string
  createdByEmail: string
  createdAt: string
  memberNames: string[]
}

export type StudyPartner = {
  fullName: string
  email: string
  university: string
  major: string
  sharedGroups: number
}

export type StudyGroupDashboard = {
  currentUserName: string
  currentUserEmail: string
  activeGroups: number
  availableGroups: number
  myGroups: number
  partnerCount: number
  nextSession: StudyGroup | null
  availableStudyGroups: StudyGroup[]
  myStudyGroups: StudyGroup[]
  studyPartners: StudyPartner[]
}

export type StudyGroupCreatePayload = {
  creatorEmail: string
  subject: string
  description: string
  day: string
  meetingTime: string
  location: string
  maxMembers: number
}

export type StudyGroupJoinPayload = {
  userEmail: string
}

export type ApiResponse = {
  success: boolean
  message: string
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

export function fetchStudyGroupDashboard(userEmail: string) {
  return requestJson<StudyGroupDashboard>(`/api/study-groups/dashboard?userEmail=${encodeURIComponent(userEmail)}`)
}

export function fetchStudyGroup(groupId: number, userEmail: string) {
  return requestJson<StudyGroup>(`/api/study-groups/${groupId}?userEmail=${encodeURIComponent(userEmail)}`)
}

export function createStudyGroup(payload: StudyGroupCreatePayload) {
  return requestJson<ApiResponse>('/api/study-groups', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function joinStudyGroup(groupId: number, payload: StudyGroupJoinPayload) {
  return requestJson<ApiResponse>(`/api/study-groups/${groupId}/join`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function leaveStudyGroup(groupId: number, payload: StudyGroupJoinPayload) {
  return requestJson<ApiResponse>(`/api/study-groups/${groupId}/leave`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function deleteStudyGroup(groupId: number, userEmail: string) {
  return requestJson<ApiResponse>(`/api/study-groups/${groupId}/delete`, {
    method: 'POST',
    body: JSON.stringify({ userEmail }),
  })
}
