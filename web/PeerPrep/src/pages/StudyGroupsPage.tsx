import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
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
import './StudyGroupsPage.css'

type StudyGroupTab = 'available' | 'my' | 'partners'

type CreateGroupFormState = {
  subject: string
  description: string
  day: string
  meetingTime: string
  location: string
  maxMembers: string
}

const defaultCreateForm: CreateGroupFormState = {
  subject: '',
  description: '',
  day: '',
  meetingTime: '',
  location: '',
  maxMembers: '6',
}

const dayOptions = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']

function StudyGroupsPage() {
  const navigate = useNavigate()
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
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [isViewLoading, setIsViewLoading] = useState(false)
  const [isViewActionLoading, setIsViewActionLoading] = useState(false)
  const [activeGroupId, setActiveGroupId] = useState<number | null>(null)
  const [selectedGroup, setSelectedGroup] = useState<StudyGroup | null>(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [form, setForm] = useState<CreateGroupFormState>(defaultCreateForm)
  const [formError, setFormError] = useState('')

  useEffect(() => {
    if (!currentUser) {
      navigate('/login', { replace: true })
      return
    }

    void refreshDashboard(true)
  }, [currentUser, navigate])

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

    if (form.description.trim().length < 20) {
      setFormError('Please add a longer description for the study group')
      return
    }

    if (!form.day || !form.meetingTime || !form.location) {
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
      const response = await createStudyGroup({
        creatorEmail: currentUser.email,
        subject: form.subject.trim(),
        description: form.description.trim(),
        day: form.day,
        meetingTime: form.meetingTime,
        location: form.location.trim(),
        maxMembers,
      })

      setMessage(response.message)
      setIsCreateModalOpen(false)
      setForm(defaultCreateForm)

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

  const dashboardGroups = activeTab === 'partners' ? [] : visibleGroups
  const activeTabCount = activeTab === 'available' ? stats.availableGroups : activeTab === 'my' ? stats.myGroups : stats.partnerCount

  return (
    <AppShell
      title={`Welcome back, ${dashboardUserName}!`}
      subtitle="Find study partners, create focused groups, and keep every session connected to the same live data."
      userName={dashboardUserName}
      actions={
        <>
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

              <div className="form-grid two-col">
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

                <label>
                  Time *
                  <input
                    type="text"
                    placeholder="e.g. 8:30 AM - 10:15 AM"
                    value={form.meetingTime}
                    onChange={(event) => setForm({ ...form, meetingTime: event.target.value })}
                    required
                  />
                </label>
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
    </AppShell>
  )
}

export default StudyGroupsPage
