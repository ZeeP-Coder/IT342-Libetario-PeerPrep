import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import AppShell from '../components/AppShell'
import { clearCurrentUser, getCurrentUser } from '../services/sessionService'
import {
  createStudyGroup,
  deleteStudyGroup,
  fetchStudyGroup,
  fetchStudyGroupDashboard,
  joinStudyGroup,
  leaveStudyGroup,
  type StudyGroup,
  type StudyPartner,
} from '../services/studyGroupService'
import { fetchUserProfile, type UserProfile } from '../services/userProfileService'
import './StudyGroupsPage.css'

type StudyGroupTab = 'available' | 'my' | 'partners'

type CreateGroupFormState = {
  subject: string
  description: string
  day: string
  location: string
  maxMembers: string
}

type TimeRangeState = {
  startHour: number
  startMinute: number
  startPeriod: 'AM' | 'PM'
  endHour: number
  endMinute: number
  endPeriod: 'AM' | 'PM'
}

type NotificationHistoryItem = {
  id: string
  title: string
  body: string
  createdAt: string
  read: boolean
}

const defaultCreateForm: CreateGroupFormState = {
  subject: '',
  description: '',
  day: '',
  location: '',
  maxMembers: '6',
}

const defaultTimeRange: TimeRangeState = {
  startHour: 8,
  startMinute: 30,
  startPeriod: 'AM',
  endHour: 10,
  endMinute: 15,
  endPeriod: 'AM',
}

const dayOptions = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
const SESSION_NOTIFICATION_STORAGE_KEY = 'peerprep.sessionNotificationsSent'
const NOTIFICATION_HISTORY_STORAGE_KEY = 'peerprep.notificationHistory'
const FIVE_MINUTES_MS = 5 * 60 * 1000
const MAX_NOTIFICATION_HISTORY = 40

type NotificationPermissionState = NotificationPermission | 'unsupported'

const dayToIndex: Record<string, number> = {
  sunday: 0,
  monday: 1,
  tuesday: 2,
  wednesday: 3,
  thursday: 4,
  friday: 5,
  saturday: 6,
}

function parseMeetingStartTime(meetingTime: string) {
  const firstTimeSegment = meetingTime.split('-')[0]?.trim() ?? ''
  const match = firstTimeSegment.match(/(\d{1,2})(?::(\d{2}))?\s*(AM|PM)?/i)
  if (!match) {
    return null
  }

  let hours = Number(match[1])
  const minutes = Number(match[2] ?? '0')
  const meridiem = (match[3] ?? '').toUpperCase()

  if (Number.isNaN(hours) || Number.isNaN(minutes) || minutes < 0 || minutes > 59) {
    return null
  }

  if (meridiem) {
    if (hours < 1 || hours > 12) {
      return null
    }
    if (meridiem === 'AM') {
      hours = hours === 12 ? 0 : hours
    } else {
      hours = hours === 12 ? 12 : hours + 12
    }
  } else if (hours < 0 || hours > 23) {
    return null
  }

  return { hours, minutes }
}

function getNextSessionStart(group: StudyGroup, fromDate = new Date()) {
  const dayIndex = dayToIndex[group.day.trim().toLowerCase()]
  const startTime = parseMeetingStartTime(group.meetingTime)

  if (dayIndex === undefined || !startTime) {
    return null
  }

  const candidate = new Date(fromDate)
  const daysUntil = (dayIndex - fromDate.getDay() + 7) % 7
  candidate.setDate(fromDate.getDate() + daysUntil)
  candidate.setHours(startTime.hours, startTime.minutes, 0, 0)

  if (candidate.getTime() <= fromDate.getTime()) {
    candidate.setDate(candidate.getDate() + 7)
  }

  return candidate
}

function readNotificationLedger() {
  if (typeof window === 'undefined') {
    return {} as Record<string, true>
  }

  const rawLedger = window.localStorage.getItem(SESSION_NOTIFICATION_STORAGE_KEY)
  if (!rawLedger) {
    return {} as Record<string, true>
  }

  try {
    return JSON.parse(rawLedger) as Record<string, true>
  } catch {
    return {} as Record<string, true>
  }
}

function readNotificationHistory(userEmail: string) {
  if (typeof window === 'undefined') {
    return [] as NotificationHistoryItem[]
  }

  const rawHistoryMap = window.localStorage.getItem(NOTIFICATION_HISTORY_STORAGE_KEY)
  if (!rawHistoryMap) {
    return [] as NotificationHistoryItem[]
  }

  try {
    const parsed = JSON.parse(rawHistoryMap) as Record<string, NotificationHistoryItem[]>
    return Array.isArray(parsed[userEmail]) ? parsed[userEmail] : []
  } catch {
    return [] as NotificationHistoryItem[]
  }
}

function saveNotificationHistory(userEmail: string, items: NotificationHistoryItem[]) {
  if (typeof window === 'undefined') {
    return
  }

  const existingRaw = window.localStorage.getItem(NOTIFICATION_HISTORY_STORAGE_KEY)
  let existingMap: Record<string, NotificationHistoryItem[]> = {}

  if (existingRaw) {
    try {
      existingMap = JSON.parse(existingRaw) as Record<string, NotificationHistoryItem[]>
    } catch {
      existingMap = {}
    }
  }

  existingMap[userEmail] = items
  window.localStorage.setItem(NOTIFICATION_HISTORY_STORAGE_KEY, JSON.stringify(existingMap))
}

function formatNotificationTimestamp(createdAt: string) {
  const createdDate = new Date(createdAt)
  const now = new Date()
  const diffMs = now.getTime() - createdDate.getTime()
  const diffMinutes = Math.floor(diffMs / (60 * 1000))

  if (diffMinutes < 1) {
    return 'Just now'
  }

  if (diffMinutes < 60) {
    return `${diffMinutes}m ago`
  }

  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) {
    return `${diffHours}h ago`
  }

  const diffDays = Math.floor(diffHours / 24)
  if (diffDays < 7) {
    return `${diffDays}d ago`
  }

  return createdDate.toLocaleDateString()
}

function formatTimeRange(timeRange: TimeRangeState) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${pad(timeRange.startHour)}:${pad(timeRange.startMinute)} ${timeRange.startPeriod} - ${pad(timeRange.endHour)}:${pad(timeRange.endMinute)} ${timeRange.endPeriod}`
}

function cycleHour(current: number, delta: 1 | -1) {
  const next = ((current - 1 + delta + 12) % 12) + 1
  return next
}

function cycleMinute(current: number, delta: 1 | -1) {
  const step = 5
  return (current + delta * step + 60) % 60
}

function saveNotificationLedger(ledger: Record<string, true>) {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(SESSION_NOTIFICATION_STORAGE_KEY, JSON.stringify(ledger))
}

function needsProfileCompletion(profile: UserProfile) {
  const normalizeValue = (value: string) => value.trim().toLowerCase()
  const university = normalizeValue(profile.university)
  const major = normalizeValue(profile.major)

  return (
    university.length === 0 ||
    major.length === 0 ||
    university === 'not set' ||
    major === 'not set' ||
    university === 'google oauth'
  )
}

function StudyGroupsPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const notificationMenuRef = useRef<HTMLDivElement>(null)
  const [currentUser] = useState(() => getCurrentUser())
  const [activeTab, setActiveTab] = useState<StudyGroupTab>('available')
  const [dashboardUserName, setDashboardUserName] = useState(currentUser?.fullName ?? 'Student')
  const [availableGroups, setAvailableGroups] = useState<StudyGroup[]>([])
  const [myGroups, setMyGroups] = useState<StudyGroup[]>([])
  const [partners, setPartners] = useState<StudyPartner[]>([])
  const [nextSession, setNextSession] = useState<StudyGroup | null>(null)
  const [stats, setStats] = useState({ activeGroups: 0, availableGroups: 0, myGroups: 0, partnerCount: 0 })
  const [searchTerm, setSearchTerm] = useState('')
  const [subjectFilter, setSubjectFilter] = useState('all')
  const [dayFilter, setDayFilter] = useState('all')
  const [timeFilter, setTimeFilter] = useState('')
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [isViewModalOpen, setIsViewModalOpen] = useState(false)
  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false)
  const [isProfilePromptOpen, setIsProfilePromptOpen] = useState(false)
  const [isNotificationMenuOpen, setIsNotificationMenuOpen] = useState(false)
  const [notificationPermission, setNotificationPermission] = useState<NotificationPermissionState>(() => {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      return 'unsupported'
    }

    return Notification.permission
  })
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [isViewLoading, setIsViewLoading] = useState(false)
  const [isViewActionLoading, setIsViewActionLoading] = useState(false)
  const [activeGroupId, setActiveGroupId] = useState<number | null>(null)
  const [selectedGroup, setSelectedGroup] = useState<StudyGroup | null>(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [notificationHistory, setNotificationHistory] = useState<NotificationHistoryItem[]>([])
  const [form, setForm] = useState<CreateGroupFormState>(defaultCreateForm)
  const [timeRange, setTimeRange] = useState<TimeRangeState>(defaultTimeRange)
  const [formError, setFormError] = useState('')

  useEffect(() => {
    if (!currentUser) {
      navigate('/login', { replace: true })
      return
    }

    void refreshDashboard(true)
  }, [currentUser, navigate])

  useEffect(() => {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      return
    }

    setNotificationPermission(Notification.permission)
  }, [])

  useEffect(() => {
    if (!currentUser) {
      return
    }

    setNotificationHistory(readNotificationHistory(currentUser.email))
  }, [currentUser])

  useEffect(() => {
    if (!isNotificationMenuOpen) {
      return
    }

    const handleOutsideClick = (event: MouseEvent) => {
      if (!notificationMenuRef.current) {
        return
      }

      if (!notificationMenuRef.current.contains(event.target as Node)) {
        setIsNotificationMenuOpen(false)
      }
    }

    document.addEventListener('mousedown', handleOutsideClick)
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick)
    }
  }, [isNotificationMenuOpen])

  useEffect(() => {
    if (!currentUser) {
      return
    }

    let isCancelled = false

    const shouldPromptFromUrl = searchParams.get('profile') === 'required'
    if (shouldPromptFromUrl) {
      setIsProfilePromptOpen(true)
      navigate('/groups', { replace: true })
    }

    const checkProfileCompleteness = async () => {
      try {
        const profile = await fetchUserProfile(currentUser.email)
        if (!isCancelled && profile.googleAuth && needsProfileCompletion(profile)) {
          setIsProfilePromptOpen(true)
        }
      } catch {
        // Non-blocking: dashboard data still loads even if profile check fails.
      }
    }

    void checkProfileCompleteness()

    return () => {
      isCancelled = true
    }
  }, [currentUser, navigate, searchParams])

  const refreshDashboard = async (showLoading = false) => {
    if (!currentUser) {
      return
    }

    if (showLoading) {
      setIsLoading(true)
    }

    setError('')

    try {
      const dashboard = await fetchStudyGroupDashboard(currentUser.email)
      setDashboardUserName(dashboard.currentUserName || currentUser.fullName)
      setAvailableGroups(dashboard.availableStudyGroups)
      setMyGroups(dashboard.myStudyGroups)
      setPartners(dashboard.studyPartners)
      setNextSession(dashboard.nextSession)
      setStats({
        activeGroups: dashboard.activeGroups,
        availableGroups: dashboard.availableGroups,
        myGroups: dashboard.myGroups,
        partnerCount: dashboard.partnerCount,
      })
    } catch (dashboardError) {
      if (dashboardError instanceof Error) {
        setError(dashboardError.message)
      } else {
        setError('Unable to load study groups right now')
      }
    } finally {
      if (showLoading) {
        setIsLoading(false)
      }
    }
  }

  useEffect(() => {
    if (!currentUser || typeof window === 'undefined' || !('Notification' in window) || Notification.permission !== 'granted') {
      return
    }

    const appendNotificationHistory = (title: string, body: string) => {
      const item: NotificationHistoryItem = {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        title,
        body,
        createdAt: new Date().toISOString(),
        read: false,
      }

      setNotificationHistory((previous) => {
        const updated = [item, ...previous].slice(0, MAX_NOTIFICATION_HISTORY)
        saveNotificationHistory(currentUser.email, updated)
        return updated
      })
    }

    const maybeSendSessionNotifications = () => {
      const now = Date.now()
      const ledger = readNotificationLedger()
      let changed = false

      for (const group of myGroups) {
        const nextStart = getNextSessionStart(group)
        if (!nextStart) {
          continue
        }

        const startAt = nextStart.getTime()
        const remindAt = startAt - FIVE_MINUTES_MS
        const reminderKey = `${group.id}:${startAt}:fiveMin`
        const startKey = `${group.id}:${startAt}:start`

        if (now >= remindAt && now < startAt && !ledger[reminderKey]) {
          const title = 'Study session starts in 5 minutes'
          const body = `${group.subject} starts at ${group.meetingTime}.`
          new Notification(title, { body })
          appendNotificationHistory(title, body)
          ledger[reminderKey] = true
          changed = true
        }

        if (now >= startAt && now < startAt + FIVE_MINUTES_MS && !ledger[startKey]) {
          const title = 'Study session started'
          const body = `${group.subject} is starting now.`
          new Notification(title, { body })
          appendNotificationHistory(title, body)
          ledger[startKey] = true
          changed = true
        }
      }

      if (changed) {
        saveNotificationLedger(ledger)
      }
    }

    maybeSendSessionNotifications()
    const intervalId = window.setInterval(maybeSendSessionNotifications, 30 * 1000)

    return () => {
      window.clearInterval(intervalId)
    }
  }, [currentUser, myGroups])

  const handleEnableNotifications = async () => {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      setError('This browser does not support notifications.')
      return
    }

    setError('')

    if (Notification.permission === 'granted') {
      setNotificationPermission('granted')
      setMessage('Notifications are already enabled.')
      return
    }

    if (Notification.permission === 'denied') {
      setNotificationPermission('denied')
      setError('Notifications are blocked in your browser. Enable them in site settings.')
      return
    }

    const permission = await Notification.requestPermission()
    setNotificationPermission(permission)

    if (permission === 'granted') {
      setMessage('Notifications enabled. You will get session reminders.')
      return
    }

    setError('Notification permission was not granted.')
  }

  const allGroups = [...availableGroups, ...myGroups]
  const subjectOptions = ['all', ...new Map(allGroups.map((group) => [group.subject.toLowerCase(), group.subject])).values()]

  const filterGroup = (group: StudyGroup) => {
    const query = searchTerm.trim().toLowerCase()
    const matchesQuery =
      query.length === 0 ||
      [group.subject, group.description, group.day, group.meetingTime, group.location, group.createdByName]
        .join(' ')
        .toLowerCase()
        .includes(query)

    const matchesSubject = subjectFilter === 'all' || group.subject.toLowerCase() === subjectFilter
    const matchesDay = dayFilter === 'all' || group.day.toLowerCase() === dayFilter
    const normalizedTimeFilter = timeFilter.trim().toLowerCase()
    const matchesTime =
      normalizedTimeFilter.length === 0 ||
      group.meetingTime.toLowerCase().includes(normalizedTimeFilter)

    return matchesQuery && matchesSubject && matchesDay && matchesTime
  }

  const visibleGroups = (activeTab === 'available' ? availableGroups : myGroups).filter(filterGroup)
  const visiblePartners = partners.filter((partner: StudyPartner) => {
    const query = searchTerm.trim().toLowerCase()
    if (query.length === 0) {
      return true
    }

    return [partner.fullName, partner.email, partner.university, partner.major]
      .join(' ')
      .toLowerCase()
      .includes(query)
  })

  const handleCreateSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setFormError('')
    setMessage('')

    if (!currentUser) {
      navigate('/login', { replace: true })
      return
    }

    if (form.subject.trim().length < 3) {
      setFormError('Subject should be at least 3 characters long')
      return
    }

    if (!form.day || !form.location) {
      setFormError('Please complete all required fields')
      return
    }

    const maxMembers = Number(form.maxMembers)
    if (Number.isNaN(maxMembers) || maxMembers < 2) {
      setFormError('Maximum members must be at least 2')
      return
    }

    setIsSaving(true)
    try {
      const meetingTime = formatTimeRange(timeRange)

      const response = await createStudyGroup({
        creatorEmail: currentUser.email,
        subject: form.subject.trim(),
        description: form.description.trim(),
        day: form.day,
        meetingTime,
        location: form.location.trim(),
        maxMembers,
      })

      setMessage(response.message)
      setIsCreateModalOpen(false)
      setForm(defaultCreateForm)
      setTimeRange(defaultTimeRange)

      await refreshDashboard()
    } catch (submitError) {
      if (submitError instanceof Error) {
        setFormError(submitError.message)
      } else {
        setFormError('Unable to create the study group right now')
      }
    } finally {
      setIsSaving(false)
    }
  }

  const handleJoin = async (groupId: number) => {
    if (!currentUser) {
      navigate('/login', { replace: true })
      return
    }

    setActiveGroupId(groupId)
    setError('')
    setMessage('')

    try {
      const response = await joinStudyGroup(groupId, { userEmail: currentUser.email })
      setMessage(response.message)
      await refreshDashboard()
    } catch (joinError) {
      if (joinError instanceof Error) {
        setError(joinError.message)
      } else {
        setError('Unable to join the study group right now')
      }
    } finally {
      setActiveGroupId(null)
    }
  }

  const handleLeave = async (groupId: number) => {
    if (!currentUser) {
      navigate('/login', { replace: true })
      return
    }

    const confirmed = window.confirm('Leave this study group?')
    if (!confirmed) {
      return
    }

    setActiveGroupId(groupId)
    setError('')
    setMessage('')

    try {
      const response = await leaveStudyGroup(groupId, { userEmail: currentUser.email })
      setMessage(response.message)
      await refreshDashboard()
    } catch (leaveError) {
      if (leaveError instanceof Error) {
        setError(leaveError.message)
      } else {
        setError('Unable to leave the study group right now')
      }
    } finally {
      setActiveGroupId(null)
    }
  }

  const handleLogout = () => {
    clearCurrentUser()
    navigate('/', { replace: true })
  }

  const handleViewDetails = async (groupId: number) => {
    if (!currentUser) {
      navigate('/login', { replace: true })
      return
    }

    setIsViewModalOpen(true)
    setIsViewLoading(true)
    setError('')

    try {
      const group = await fetchStudyGroup(groupId, currentUser.email)
      setSelectedGroup(group)
    } catch (detailsError) {
      if (detailsError instanceof Error) {
        setError(detailsError.message)
      } else {
        setError('Unable to load group details')
      }
      setIsViewModalOpen(false)
    } finally {
      setIsViewLoading(false)
    }
  }

  const handleDeleteGroup = async () => {
    if (!selectedGroup) {
      return
    }

    setIsDeleteConfirmOpen(true)
  }

  const handleDeleteFromCard = (group: StudyGroup) => {
    setSelectedGroup(group)
    setIsDeleteConfirmOpen(true)
  }

  const handleConfirmDeleteGroup = async () => {
    if (!currentUser || !selectedGroup) {
      return
    }

    setIsViewActionLoading(true)
    setError('')
    setMessage('')

    try {
      const response = await deleteStudyGroup(selectedGroup.id, currentUser.email)
      setMessage(response.message)
      setIsDeleteConfirmOpen(false)
      setIsViewModalOpen(false)
      setSelectedGroup(null)
      await refreshDashboard()
    } catch (deleteError) {
      if (deleteError instanceof Error) {
        setError(deleteError.message)
      } else {
        setError('Unable to delete this study group right now')
      }
    } finally {
      setIsViewActionLoading(false)
    }
  }

  const handleLeaveFromDetails = async () => {
    if (!currentUser || !selectedGroup) {
      return
    }

    setIsViewActionLoading(true)
    setError('')
    setMessage('')

    try {
      const response = await leaveStudyGroup(selectedGroup.id, { userEmail: currentUser.email })
      setMessage(response.message)
      setIsViewModalOpen(false)
      setSelectedGroup(null)
      await refreshDashboard()
    } catch (leaveError) {
      if (leaveError instanceof Error) {
        setError(leaveError.message)
      } else {
        setError('Unable to leave the study group right now')
      }
    } finally {
      setIsViewActionLoading(false)
    }
  }

  const updateTimeRange = (updater: (prev: TimeRangeState) => TimeRangeState) => {
    setTimeRange((prev) => updater(prev))
  }

  const unreadNotificationCount = notificationHistory.filter((item) => !item.read).length

  const toggleNotificationMenu = () => {
    setIsNotificationMenuOpen((previous) => !previous)
  }

  const handleMarkAllNotificationsRead = () => {
    if (!currentUser) {
      return
    }

    const updated = notificationHistory.map((item) => ({ ...item, read: true }))
    setNotificationHistory(updated)
    saveNotificationHistory(currentUser.email, updated)
  }

  const handleNotificationClick = (notificationId: string) => {
    if (!currentUser) {
      return
    }

    const updated = notificationHistory.map((item) =>
      item.id === notificationId ? { ...item, read: true } : item,
    )
    setNotificationHistory(updated)
    saveNotificationHistory(currentUser.email, updated)
  }

  const dashboardGroups = activeTab === 'partners' ? [] : visibleGroups
  const activeTabCount = activeTab === 'available' ? stats.availableGroups : activeTab === 'my' ? stats.myGroups : stats.partnerCount

  return (
    <AppShell
      title={`Welcome back, ${dashboardUserName}!`}
      subtitle="Find study partners, create focused groups, and keep every session connected to the same live data."
      userName={dashboardUserName}
      leftActions={
        <div className="notification-bell-wrap" ref={notificationMenuRef}>
          <button
            className="notification-bell-button"
            type="button"
            aria-label="Open notifications"
            aria-expanded={isNotificationMenuOpen}
            onClick={toggleNotificationMenu}
          >
            <span aria-hidden="true">🔔</span>
            {unreadNotificationCount > 0 ? (
              <span className="notification-bell-badge">
                {unreadNotificationCount > 9 ? '9+' : unreadNotificationCount}
              </span>
            ) : null}
          </button>

          {isNotificationMenuOpen ? (
            <div className="notification-dropdown" role="menu" aria-label="Notification history">
              <div className="notification-dropdown-header">
                <h3>Notifications</h3>
                {notificationHistory.length > 0 ? (
                  <button className="text-link-button" type="button" onClick={handleMarkAllNotificationsRead}>
                    Mark all read
                  </button>
                ) : null}
              </div>

              {notificationPermission !== 'granted' && notificationPermission !== 'unsupported' ? (
                <button className="notification-enable-button" type="button" onClick={handleEnableNotifications}>
                  Enable browser notifications
                </button>
              ) : null}

              {notificationHistory.length === 0 ? (
                <p className="notification-empty">No notifications yet.</p>
              ) : (
                <div className="notification-history-list">
                  {notificationHistory.map((item) => (
                    <button
                      key={item.id}
                      type="button"
                      className={item.read ? 'notification-item' : 'notification-item unread'}
                      onClick={() => handleNotificationClick(item.id)}
                    >
                      <strong>{item.title}</strong>
                      <span>{item.body}</span>
                      <small>{formatNotificationTimestamp(item.createdAt)}</small>
                    </button>
                  ))}
                </div>
              )}
            </div>
          ) : null}
        </div>
      }
      actions={
        <>
          <button className="shell-link-button secondary" type="button" onClick={() => navigate('/profile')}>
            Profile
          </button>
          <button className="shell-link-button" type="button" onClick={() => setIsCreateModalOpen(true)}>
            + Create Group
          </button>
          <button className="shell-link-button secondary" type="button" onClick={handleLogout}>
            Logout
          </button>
        </>
      }
    >
      <section className="dashboard-summary-grid">
        <article className="summary-card highlight">
          <span className="summary-label">Your Study Groups</span>
          <strong>{stats.myGroups}</strong>
          <span>Active groups</span>
        </article>
        <article className="summary-card">
          <span className="summary-label">Available Groups</span>
          <strong>{stats.availableGroups}</strong>
          <span>Groups to join</span>
        </article>
        <article className="summary-card">
          <span className="summary-label">Study Partners</span>
          <strong>{stats.partnerCount}</strong>
          <span>Matches from your groups</span>
        </article>
        <article className="summary-card session-card">
          <span className="summary-label">Next Session</span>
          {nextSession ? (
            <>
              <strong>{nextSession.day}</strong>
              <span>{nextSession.meetingTime} - {nextSession.subject}</span>
            </>
          ) : (
            <>
              <strong>None yet</strong>
              <span>Create or join a group to schedule your next session</span>
            </>
          )}
        </article>
      </section>

      <section className="toolbar-panel">
        <label className="search-bar" htmlFor="group-search">
          <span className="search-icon">⌕</span>
          <input
            id="group-search"
            type="search"
            placeholder="Search for subjects, topics, or study groups..."
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
          />
        </label>

        <div className="filters-card">
          <div className="filters-header">
            <span>Filters</span>
            <button
              className="text-link-button"
              type="button"
              onClick={() => {
                setSearchTerm('')
                setSubjectFilter('all')
                setDayFilter('all')
                setTimeFilter('')
              }}
            >
              Clear all
            </button>
          </div>

          <div className="filters-grid">
            <label>
              Subject
              <select value={subjectFilter} onChange={(event) => setSubjectFilter(event.target.value)}>
                <option value="all">All subjects</option>
                {subjectOptions
                  .filter((subject) => subject !== 'all')
                  .map((subject) => (
                    <option key={subject} value={subject}>
                      {subject}
                    </option>
                  ))}
              </select>
            </label>

            <label>
              Day
              <select value={dayFilter} onChange={(event) => setDayFilter(event.target.value)}>
                <option value="all">All days</option>
                {dayOptions.map((day) => (
                  <option key={day} value={day.toLowerCase()}>
                    {day}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Time
              <input
                type="text"
                placeholder="e.g. 8:00 AM or 13:00"
                value={timeFilter}
                onChange={(event) => setTimeFilter(event.target.value)}
              />
            </label>
          </div>
        </div>
      </section>

      <section className="tab-bar" aria-label="Study group sections">
        {(['available', 'my', 'partners'] as StudyGroupTab[]).map((tab) => (
          <button
            key={tab}
            type="button"
            className={tab === activeTab ? 'tab-button active' : 'tab-button'}
            onClick={() => setActiveTab(tab)}
          >
            {tab === 'available' ? 'Available' : tab === 'my' ? 'My Groups' : 'Partners'}
          </button>
        ))}
      </section>

      {message ? <p className="feedback success">{message}</p> : null}
      {error ? <p className="feedback error">{error}</p> : null}

      <section className="section-header">
        <div>
          <h2>
            {activeTab === 'available'
              ? `Available Study Groups (${activeTabCount})`
              : activeTab === 'my'
                ? `My Groups (${activeTabCount})`
                : `Study Partners (${activeTabCount})`}
          </h2>
          <p>
            {activeTab === 'available'
              ? 'Join open groups and keep the discussion moving.'
              : activeTab === 'my'
                ? 'Track the groups you already belong to and jump back in quickly.'
                : 'Students surfaced from your current study circles.'}
          </p>
        </div>
      </section>

      {isLoading ? (
        <div className="empty-state">Loading your study network...</div>
      ) : activeTab === 'partners' ? (
        visiblePartners.length > 0 ? (
          <div className="partners-grid">
            {visiblePartners.map((partner) => (
              <article key={partner.email} className="partner-card">
                <div className="partner-avatar">{partner.fullName.charAt(0)}</div>
                <div>
                  <h3>{partner.fullName}</h3>
                  <p>{partner.major}</p>
                  <span>{partner.university}</span>
                </div>
                <strong>{partner.sharedGroups} shared groups</strong>
              </article>
            ))}
          </div>
        ) : (
          <div className="empty-state">No partners match the current filters yet.</div>
        )
      ) : dashboardGroups.length > 0 ? (
        <div className="group-grid">
          {dashboardGroups.map((group) => (
            <article key={group.id} className="group-card">
              <div className="group-card-top">
                <div className="group-card-heading">
                  <h3>{group.subject}</h3>
                  <span className={group.joinable ? 'status-pill open' : 'status-pill full'}>{group.status}</span>
                </div>
                <p>{group.description}</p>
              </div>

              <ul className="group-meta-list">
                <li>{group.day}</li>
                <li>{group.meetingTime}</li>
                <li>{group.location}</li>
                <li>{group.currentMembers}/{group.maxMembers} members</li>
              </ul>

              <div className="topic-strip">
                <span>{group.ownedByCurrentUser ? 'Created by you' : `Hosted by ${group.createdByName}`}</span>
              </div>

              <div className="group-actions">
                <button type="button" className="text-button" onClick={() => handleViewDetails(group.id)}>
                  View Details
                </button>
                {group.ownedByCurrentUser ? (
                  <button
                    type="button"
                    className="action-button danger"
                    disabled={isViewActionLoading}
                    onClick={() => handleDeleteFromCard(group)}
                  >
                    Delete
                  </button>
                ) : group.joined ? (
                  <button
                    type="button"
                    className="action-button danger"
                    disabled={activeGroupId === group.id}
                    onClick={() => handleLeave(group.id)}
                  >
                    {activeGroupId === group.id ? 'Leaving...' : 'Leave'}
                  </button>
                ) : (
                  <button
                    type="button"
                    className="action-button"
                    disabled={!group.joinable || activeGroupId === group.id}
                    onClick={() => handleJoin(group.id)}
                  >
                    {group.joinable ? (activeGroupId === group.id ? 'Joining...' : 'Join Group') : 'Group Full'}
                  </button>
                )}
              </div>
            </article>
          ))}
        </div>
      ) : (
        <div className="empty-state">
          No groups found. Try a different filter or create a new study group.
        </div>
      )}

      {isCreateModalOpen ? (
        <div className="modal-backdrop" role="presentation" onClick={() => setIsCreateModalOpen(false)}>
          <div
            className="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="create-group-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="modal-header">
              <div>
                <h2 id="create-group-title">Create Study Group</h2>
                <p>Create a new study group and invite others to join.</p>
              </div>
              <button className="icon-button" type="button" onClick={() => setIsCreateModalOpen(false)}>
                ×
              </button>
            </div>

            <form className="create-group-form" onSubmit={handleCreateSubmit}>
              <label>
                Subject *
                <input
                  type="text"
                  placeholder="e.g. Data Structures"
                  value={form.subject}
                  onChange={(event) => setForm({ ...form, subject: event.target.value })}
                  required
                />
              </label>

              <label>
                Description
                <textarea
                  rows={4}
                  placeholder="What topics will you cover?"
                  value={form.description}
                  onChange={(event) => setForm({ ...form, description: event.target.value })}
                  required
                />
              </label>

              <div className="form-grid two-col time-row-grid">
                <label>
                  Day *
                  <select value={form.day} onChange={(event) => setForm({ ...form, day: event.target.value })} required>
                    <option value="">Select day</option>
                    {dayOptions.map((day) => (
                      <option key={day} value={day}>
                        {day}
                      </option>
                    ))}
                  </select>
                </label>

                <div className="time-picker-field">
                  <span className="time-picker-label">Time *</span>
                  <div className="time-range-picker" role="group" aria-label="Select study group time range">
                    <div className="time-column">
                      <span>Start</span>
                      <div className="time-spinner">
                        <div className="time-unit-spinner">
                          <button
                            type="button"
                            className="time-arrow"
                            onClick={() => updateTimeRange((prev) => ({ ...prev, startHour: cycleHour(prev.startHour, 1) }))}
                          >
                            ▲
                          </button>
                          <strong>{String(timeRange.startHour).padStart(2, '0')}</strong>
                          <button
                            type="button"
                            className="time-arrow"
                            onClick={() => updateTimeRange((prev) => ({ ...prev, startHour: cycleHour(prev.startHour, -1) }))}
                          >
                            ▼
                          </button>
                        </div>

                        <span className="time-separator">:</span>

                        <div className="time-unit-spinner">
                          <button
                            type="button"
                            className="time-arrow"
                            onClick={() => updateTimeRange((prev) => ({ ...prev, startMinute: cycleMinute(prev.startMinute, 1) }))}
                          >
                            ▲
                          </button>
                          <strong>{String(timeRange.startMinute).padStart(2, '0')}</strong>
                          <button
                            type="button"
                            className="time-arrow"
                            onClick={() => updateTimeRange((prev) => ({ ...prev, startMinute: cycleMinute(prev.startMinute, -1) }))}
                          >
                            ▼
                          </button>
                        </div>

                        <button
                          type="button"
                          className="time-period-toggle"
                          onClick={() =>
                            updateTimeRange((prev) => ({
                              ...prev,
                              startPeriod: prev.startPeriod === 'AM' ? 'PM' : 'AM',
                            }))
                          }
                        >
                          {timeRange.startPeriod}
                        </button>
                      </div>
                    </div>

                    <span className="time-range-dash" aria-hidden="true">to</span>

                    <div className="time-column">
                      <span>End</span>
                      <div className="time-spinner">
                        <div className="time-unit-spinner">
                          <button
                            type="button"
                            className="time-arrow"
                            onClick={() => updateTimeRange((prev) => ({ ...prev, endHour: cycleHour(prev.endHour, 1) }))}
                          >
                            ▲
                          </button>
                          <strong>{String(timeRange.endHour).padStart(2, '0')}</strong>
                          <button
                            type="button"
                            className="time-arrow"
                            onClick={() => updateTimeRange((prev) => ({ ...prev, endHour: cycleHour(prev.endHour, -1) }))}
                          >
                            ▼
                          </button>
                        </div>

                        <span className="time-separator">:</span>

                        <div className="time-unit-spinner">
                          <button
                            type="button"
                            className="time-arrow"
                            onClick={() => updateTimeRange((prev) => ({ ...prev, endMinute: cycleMinute(prev.endMinute, 1) }))}
                          >
                            ▲
                          </button>
                          <strong>{String(timeRange.endMinute).padStart(2, '0')}</strong>
                          <button
                            type="button"
                            className="time-arrow"
                            onClick={() => updateTimeRange((prev) => ({ ...prev, endMinute: cycleMinute(prev.endMinute, -1) }))}
                          >
                            ▼
                          </button>
                        </div>

                        <button
                          type="button"
                          className="time-period-toggle"
                          onClick={() =>
                            updateTimeRange((prev) => ({
                              ...prev,
                              endPeriod: prev.endPeriod === 'AM' ? 'PM' : 'AM',
                            }))
                          }
                        >
                          {timeRange.endPeriod}
                        </button>
                      </div>
                    </div>
                  </div>
                  <span className="time-preview">{formatTimeRange(timeRange)}</span>
                </div>
              </div>

              <div className="form-grid two-col">
                <label>
                  Location *
                  <input
                    type="text"
                    placeholder="Library Room 203"
                    value={form.location}
                    onChange={(event) => setForm({ ...form, location: event.target.value })}
                    required
                  />
                </label>

                <label>
                  Max Members
                  <input
                    type="number"
                    min={2}
                    max={50}
                    value={form.maxMembers}
                    onChange={(event) => setForm({ ...form, maxMembers: event.target.value })}
                    required
                  />
                </label>
              </div>

              {formError ? <p className="feedback error">{formError}</p> : null}

              <div className="modal-actions">
                <button className="action-button ghost" type="button" onClick={() => setIsCreateModalOpen(false)}>
                  Cancel
                </button>
                <button className="action-button" type="submit" disabled={isSaving}>
                  {isSaving ? 'Creating...' : 'Create Group'}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}

      {isViewModalOpen ? (
        <div className="modal-backdrop" role="presentation" onClick={() => setIsViewModalOpen(false)}>
          <div
            className="modal-card modal-card-large"
            role="dialog"
            aria-modal="true"
            aria-labelledby="view-group-title"
            onClick={(event) => event.stopPropagation()}
          >
            {isViewLoading || !selectedGroup ? (
              <p className="empty-state">Loading group details...</p>
            ) : (
              <>
                <div className="modal-header">
                  <div>
                    <h2 id="view-group-title">{selectedGroup.subject}</h2>
                    <p>{selectedGroup.description}</p>
                  </div>
                  <button className="icon-button" type="button" onClick={() => setIsViewModalOpen(false)}>
                    ×
                  </button>
                </div>

                <div className="details-modal-body">
                  <div>
                    <span>Day</span>
                    <strong>{selectedGroup.day}</strong>
                  </div>
                  <div>
                    <span>Time</span>
                    <strong>{selectedGroup.meetingTime}</strong>
                  </div>
                  <div>
                    <span>Location</span>
                    <strong>{selectedGroup.location}</strong>
                  </div>
                  <div>
                    <span>Members</span>
                    <strong>
                      {selectedGroup.currentMembers}/{selectedGroup.maxMembers}
                    </strong>
                  </div>
                </div>

                <p className="details-metadata">
                  {selectedGroup.ownedByCurrentUser
                    ? 'You created this group'
                    : `Created by ${selectedGroup.createdByName}`}
                </p>

                <div className="details-members">
                  <span>Members</span>
                  <div className="details-member-list">
                    {selectedGroup.memberNames.length > 0
                      ? selectedGroup.memberNames.map((memberName) => (
                          <p key={memberName}>{memberName}</p>
                        ))
                      : <p>No members yet</p>}
                  </div>
                </div>

                <div className="details-actions">
                  <button className="action-button ghost" type="button" onClick={() => setIsViewModalOpen(false)}>
                    Close
                  </button>

                  {selectedGroup.ownedByCurrentUser ? (
                    <button
                      className="action-button danger"
                      type="button"
                      onClick={handleDeleteGroup}
                      disabled={isViewActionLoading}
                    >
                      {isViewActionLoading ? 'Deleting...' : 'Delete Group'}
                    </button>
                  ) : selectedGroup.joined ? (
                    <button
                      className="action-button danger"
                      type="button"
                      onClick={handleLeaveFromDetails}
                      disabled={isViewActionLoading}
                    >
                      {isViewActionLoading ? 'Leaving...' : 'Leave Group'}
                    </button>
                  ) : null}
                </div>
              </>
            )}
          </div>
        </div>
      ) : null}

      {isDeleteConfirmOpen ? (
        <div className="modal-backdrop top-layer" role="presentation" onClick={() => setIsDeleteConfirmOpen(false)}>
          <div
            className="modal-card confirm-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-confirm-title"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 id="delete-confirm-title">Delete this group permanently?</h2>
            <p>This action cannot be undone and all group memberships will be removed.</p>

            <div className="details-actions">
              <button className="action-button ghost" type="button" onClick={() => setIsDeleteConfirmOpen(false)}>
                Cancel
              </button>
              <button
                className="action-button danger"
                type="button"
                onClick={handleConfirmDeleteGroup}
                disabled={isViewActionLoading}
              >
                {isViewActionLoading ? 'Deleting...' : 'Yes, Delete Group'}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {isProfilePromptOpen ? (
        <div className="modal-backdrop top-layer" role="presentation" onClick={() => setIsProfilePromptOpen(false)}>
          <div
            className="modal-card confirm-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="profile-prompt-title"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 id="profile-prompt-title">Complete your profile</h2>
            <p>
              Your Google account was created with default values for university and/or major. Update your profile so
              others can discover you in study groups.
            </p>

            <div className="details-actions">
              <button className="action-button ghost" type="button" onClick={() => setIsProfilePromptOpen(false)}>
                Later
              </button>
              <button
                className="action-button"
                type="button"
                onClick={() => {
                  setIsProfilePromptOpen(false)
                  navigate('/profile')
                }}
              >
                Go to Profile
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </AppShell>
  )
}

export default StudyGroupsPage
