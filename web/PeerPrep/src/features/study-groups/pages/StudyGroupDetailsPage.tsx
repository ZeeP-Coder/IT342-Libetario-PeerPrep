import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import AppShell from '../../../shared/components/AppShell'
import { clearCurrentUser, getCurrentUser } from '../../../shared/session/sessionService'
import { fetchStudyGroup, joinStudyGroup, leaveStudyGroup, type StudyGroup } from '../api/studyGroupService'
import './StudyGroupDetailsPage.css'

function StudyGroupDetailsPage() {
  const navigate = useNavigate()
  const params = useParams()
  const [currentUser] = useState(() => getCurrentUser())
  const groupId = Number(params.groupId)
  const [group, setGroup] = useState<StudyGroup | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (!currentUser) {
      navigate('/login', { replace: true })
      return
    }

    if (!Number.isFinite(groupId)) {
      setError('Study group not found')
      setIsLoading(false)
      return
    }

    const loadGroup = async () => {
      setIsLoading(true)
      setError('')

      try {
        const response = await fetchStudyGroup(groupId, currentUser.email)
        setGroup(response)
      } catch (loadError) {
        if (loadError instanceof Error) {
          setError(loadError.message)
        } else {
          setError('Unable to load the study group right now')
        }
      } finally {
        setIsLoading(false)
      }
    }

    void loadGroup()
  }, [currentUser, groupId, navigate])

  const refreshGroup = async () => {
    if (!currentUser || !Number.isFinite(groupId)) {
      return
    }

    const response = await fetchStudyGroup(groupId, currentUser.email)
    setGroup(response)
  }

  const handleJoin = async () => {
    if (!currentUser || !group) {
      return
    }

    setIsSubmitting(true)
    setError('')
    setMessage('')

    try {
      const response = await joinStudyGroup(group.id, { userEmail: currentUser.email })
      setMessage(response.message)
      await refreshGroup()
    } catch (joinError) {
      if (joinError instanceof Error) {
        setError(joinError.message)
      } else {
        setError('Unable to join the study group right now')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleLeave = async () => {
    if (!currentUser || !group) {
      return
    }

    setIsSubmitting(true)
    setError('')
    setMessage('')

    try {
      const response = await leaveStudyGroup(group.id, { userEmail: currentUser.email })
      setMessage(response.message)
      await refreshGroup()
    } catch (leaveError) {
      if (leaveError instanceof Error) {
        setError(leaveError.message)
      } else {
        setError('Unable to leave the study group right now')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleLogout = () => {
    clearCurrentUser()
    navigate('/', { replace: true })
  }

  return (
    <AppShell
      title={group ? group.subject : 'Study Group Details'}
      subtitle={group ? 'Review the session information and manage your membership from here.' : 'Inspect the selected study group.'}
      userName={currentUser?.fullName ?? 'Student'}
      actions={
        <>
          <Link className="shell-link-button secondary" to="/groups">
            Back to Dashboard
          </Link>
          <button className="shell-link-button secondary" type="button" onClick={handleLogout}>
            Logout
          </button>
        </>
      }
    >
      {isLoading ? (
        <div className="detail-card loading-card">Loading study group...</div>
      ) : error ? (
        <div className="detail-card error-card">{error}</div>
      ) : group ? (
        <div className="detail-layout">
          <section className="detail-card group-detail-main">
            <div className="detail-heading-row">
              <div>
                <span className={group.joinable ? 'status-pill open' : 'status-pill full'}>{group.status}</span>
                <h2>{group.subject}</h2>
                <p>{group.description}</p>
              </div>
            </div>

            <div className="detail-grid">
              <div>
                <span>Day</span>
                <strong>{group.day}</strong>
              </div>
              <div>
                <span>Time</span>
                <strong>{group.meetingTime}</strong>
              </div>
              <div>
                <span>Location</span>
                <strong>{group.location}</strong>
              </div>
              <div>
                <span>Members</span>
                <strong>
                  {group.currentMembers}/{group.maxMembers}
                </strong>
              </div>
            </div>

            <div className="detail-meta">
              <span>Hosted by {group.createdByName}</span>
              <span>{group.ownedByCurrentUser ? 'You created this group' : group.createdByEmail}</span>
            </div>
          </section>

          <aside className="detail-card detail-action-panel">
            <h3>Membership</h3>
            <p>
              {group.joined
                ? 'You are part of this study group. Leave only if you no longer need the session.'
                : group.joinable
                  ? 'Join now to start collaborating with the group.'
                  : 'This study group is already full.'}
            </p>

            {message ? <p className="feedback success">{message}</p> : null}
            {error ? <p className="feedback error">{error}</p> : null}

            {group.joined ? (
              <button className="action-button danger block" type="button" onClick={handleLeave} disabled={isSubmitting}>
                {isSubmitting ? 'Leaving...' : 'Leave Group'}
              </button>
            ) : (
              <button
                className="action-button block"
                type="button"
                onClick={handleJoin}
                disabled={isSubmitting || !group.joinable}
              >
                {group.joinable ? (isSubmitting ? 'Joining...' : 'Join Group') : 'Group Full'}
              </button>
            )}
          </aside>
        </div>
      ) : null}
    </AppShell>
  )
}

export default StudyGroupDetailsPage
