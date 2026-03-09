import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthCard from '../components/AuthCard'
import { register } from '../services/authService'
import './RegisterPage.css'

function RegisterPage() {
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [university, setUniversity] = useState('')
  const [major, setMajor] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

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
        navigate('/login?registered=1', { replace: true })
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
        <select
          id="register-major"
          value={major}
          onChange={(event) => setMajor(event.target.value)}
          required
        >
          <option value="" disabled>
            Select your major
          </option>
          <option value="computer-science">Computer Science</option>
          <option value="information-technology">Information Technology</option>
          <option value="engineering">Engineering</option>
          <option value="business">Business</option>
          <option value="education">Education</option>
        </select>

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
