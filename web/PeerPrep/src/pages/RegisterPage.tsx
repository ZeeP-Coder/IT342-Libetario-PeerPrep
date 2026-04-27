import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthCard from '../components/AuthCard'
import { register } from '../services/authService'
import { getCurrentUser, setCurrentUser } from '../services/sessionService'
import './RegisterPage.css'

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

function RegisterPage() {
  const navigate = useNavigate()
  const majorInputRef = useRef<HTMLInputElement>(null)
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [university, setUniversity] = useState('')
  const [major, setMajor] = useState('')
  const [majorFocus, setMajorFocus] = useState(false)
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const majorSuggestions =
    major.trim().length === 0
      ? []
      : SUGGESTED_MAJORS.filter((m) => m.toLowerCase().includes(major.trim().toLowerCase()))

  useEffect(() => {
    if (getCurrentUser()) {
      navigate('/groups', { replace: true })
    }
  }, [navigate])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setMessage('')

    if (password !== confirmPassword) {
      setError('Passwords do not match')
      return
    }

    setIsSubmitting(true)
    try {
      const response = await register({
        fullName,
        email,
        university,
        major,
        password,
      })
      if (response.success) {
        setCurrentUser({
          fullName: response.fullName ?? fullName,
          email: response.email ?? email,
        })
        navigate('/groups', { replace: true })
        return
      }

      setMessage(response.message)
    } catch (submitError) {
      if (submitError instanceof Error) {
        setError(submitError.message)
      } else {
        setError('Unable to register right now')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthCard
      title="Create an account"
      subtitle="Join thousands of students finding study partners"
      footer={
        <>
          Already have an account? <Link to="/login">Sign in</Link>
        </>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label htmlFor="register-name">Full Name</label>
        <input
          id="register-name"
          type="text"
          placeholder="John Doe"
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          required
        />

        <label htmlFor="register-email">Email</label>
        <input
          id="register-email"
          type="email"
          placeholder="you@email.com"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />

        <label htmlFor="register-school">University Name</label>
        <input
          id="register-school"
          type="text"
          placeholder="Your University Name"
          value={university}
          onChange={(event) => setUniversity(event.target.value)}
          required
        />

        <label htmlFor="register-major">Major</label>
        <div className="major-autocomplete-container">
          <input
            ref={majorInputRef}
            id="register-major"
            type="text"
            placeholder="e.g. Computer Science, Business..."
            value={major}
            onChange={(event) => setMajor(event.target.value)}
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
                    setMajor(suggestion)
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

        <label htmlFor="register-password">Password</label>
        <input
          id="register-password"
          type="password"
          placeholder="••••••••"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
          minLength={8}
        />

        <label htmlFor="register-confirm-password">Confirm Password</label>
        <input
          id="register-confirm-password"
          type="password"
          placeholder="••••••••"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          required
          minLength={8}
        />

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Creating Account...' : 'Create Account'}
        </button>

        {error ? <p className="auth-error">{error}</p> : null}
        {message ? <p className="auth-success">{message}</p> : null}
      </form>
    </AuthCard>
  )
}

export default RegisterPage
