import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import AppShell from '../../../shared/components/AppShell'
import { clearCurrentUser, getCurrentUser, setCurrentUser } from '../../../shared/session/sessionService'
import { fetchUserProfile, updateUserProfile } from '../api/userProfileService'
import './ProfilePage.css'

type ProfileFormState = {
  fullName: string
  university: string
  major: string
}

const defaultForm: ProfileFormState = {
  fullName: '',
  university: '',
  major: '',
}

const SUGGESTED_MAJORS = [
  'Computer Science',
  'Information Technology',
  'Software Engineering',
  'Data Science',
  'Artificial Intelligence',
  'Cybersecurity',
  'Engineering',
  'Electrical Engineering',
  'Mechanical Engineering',
  'Civil Engineering',
  'Business',
  'Finance',
  'Economics',
  'Psychology',
  'Biology',
  'Chemistry',
  'Physics',
  'Mathematics',
  'Education',
  'Nursing',
  'Liberal Arts',
]

function ProfilePage() {
  const navigate = useNavigate()
  const majorInputRef = useRef<HTMLInputElement>(null)
  const [currentUser] = useState(() => getCurrentUser())
  const [form, setForm] = useState<ProfileFormState>(defaultForm)
  const [email, setEmail] = useState('')
  const [isGoogleAuth, setIsGoogleAuth] = useState(false)
  const [majorFocus, setMajorFocus] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const majorSuggestions =
    form.major.trim().length === 0
      ? []
      : SUGGESTED_MAJORS.filter((m) => m.toLowerCase().includes(form.major.trim().toLowerCase()))

  useEffect(() => {
    if (!currentUser) {
      navigate('/login', { replace: true })
      return
    }

    const loadProfile = async () => {
      setIsLoading(true)
      setError('')

      try {
        const profile = await fetchUserProfile(currentUser.email)
        setForm({
          fullName: profile.fullName,
          university: profile.university,
          major: profile.major,
        })
        setEmail(profile.email)
        setIsGoogleAuth(profile.googleAuth)
      } catch (loadError) {
        if (loadError instanceof Error) {
          setError(loadError.message)
        } else {
          setError('Unable to load your profile at the moment')
        }
      } finally {
        setIsLoading(false)
      }
    }

    void loadProfile()
  }, [currentUser, navigate])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!currentUser) {
      navigate('/login', { replace: true })
      return
    }

    setMessage('')
    setError('')

    const payload = {
      email: currentUser.email,
      fullName: form.fullName.trim(),
      university: form.university.trim(),
      major: form.major.trim(),
    }

    if (!payload.fullName || !payload.university || !payload.major) {
      setError('Please complete all fields before saving your profile')
      return
    }

    setIsSaving(true)

    try {
      const updatedProfile = await updateUserProfile(payload)
      setForm({
        fullName: updatedProfile.fullName,
        university: updatedProfile.university,
        major: updatedProfile.major,
      })
      setCurrentUser({
        email: updatedProfile.email,
        fullName: updatedProfile.fullName,
      })
      setMessage('Profile updated successfully.')
    } catch (saveError) {
      if (saveError instanceof Error) {
        setError(saveError.message)
      } else {
        setError('Unable to save your profile right now')
      }
    } finally {
      setIsSaving(false)
    }
  }

  const handleLogout = () => {
    clearCurrentUser()
    navigate('/', { replace: true })
  }

  return (
    <AppShell
      title="Your Profile"
      subtitle="Keep your study identity up to date so groups and partners can find you faster."
      userName={form.fullName || currentUser?.fullName || 'Student'}
      actions={
        <>
          <button className="shell-link-button secondary" type="button" onClick={() => navigate('/groups')}>
            Back to Home
          </button>
          <button className="shell-link-button secondary" type="button" onClick={handleLogout}>
            Logout
          </button>
        </>
      }
    >
      <section className="profile-layout">
        <article className="profile-card">
          <h2>Edit profile information</h2>
          <p>Update your full name, university, and major.</p>

          {isLoading ? <div className="empty-state">Loading your profile...</div> : null}
          {error ? <p className="feedback error">{error}</p> : null}
          {message ? <p className="feedback success">{message}</p> : null}

          {!isLoading ? (
            <form className="profile-form" onSubmit={handleSubmit}>
              <label htmlFor="profile-email">Email</label>
              <input id="profile-email" type="email" value={email} disabled />

              <label htmlFor="profile-name">Full Name</label>
              <input
                id="profile-name"
                type="text"
                value={form.fullName}
                onChange={(event) => setForm({ ...form, fullName: event.target.value })}
                required
              />

              <label htmlFor="profile-university">University</label>
              <input
                id="profile-university"
                type="text"
                value={form.university}
                onChange={(event) => setForm({ ...form, university: event.target.value })}
                required
              />

              <label htmlFor="profile-major">Major</label>
              <div className="major-autocomplete-container">
                <input
                  ref={majorInputRef}
                  id="profile-major"
                  type="text"
                  placeholder="e.g. Computer Science, Business..."
                  value={form.major}
                  onChange={(event) => setForm({ ...form, major: event.target.value })}
                  onFocus={() => setMajorFocus(true)}
                  onBlur={() => setTimeout(() => setMajorFocus(false), 150)}
                  required
                />
                {majorFocus && majorSuggestions.length > 0 ? (
                  <ul className="major-suggestions-dropdown">
                    {majorSuggestions.map((suggestion) => (
                      <li
                        key={suggestion}
                        onClick={() => {
                          setForm({ ...form, major: suggestion })
                          setMajorFocus(false)
                          majorInputRef.current?.blur()
                        }}
                      >
                        {suggestion}
                      </li>
                    ))}
                  </ul>
                ) : null}
              </div>

              <button className="action-button" type="submit" disabled={isSaving}>
                {isSaving ? 'Saving...' : 'Save Changes'}
              </button>
            </form>
          ) : null}
        </article>

        <aside className="profile-card profile-side-card">
          <h3>Account details</h3>
          <p>
            Authentication method:{' '}
            <strong>{isGoogleAuth ? 'Google Sign-In' : 'Email + Password'}</strong>
          </p>
          {isGoogleAuth ? (
            <p>
              Google-authenticated users should complete profile fields so study partners can see your school and
              major.
            </p>
          ) : (
            <p>Keep your profile updated so your study group details stay accurate.</p>
          )}
        </aside>
      </section>
    </AppShell>
  )
}

export default ProfilePage
