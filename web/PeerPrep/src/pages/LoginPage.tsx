import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import AuthCard from '../components/AuthCard'
import { getGoogleAuthUrl, login } from '../services/authService'
import { getCurrentUser, setCurrentUser } from '../services/sessionService'
import './LoginPage.css'

function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (getCurrentUser()) {
      navigate('/groups', { replace: true })
      return
    }

    const registered = searchParams.get('registered')
    const google = searchParams.get('google')
    const googleEmail = searchParams.get('email')
    const googleFullName = searchParams.get('fullName')
    const googleProfile = searchParams.get('profile')

    if (registered === '1') {
      setMessage('Account created successfully. Please sign in.')
    }

    if (google === 'success') {
      if (googleEmail) {
        setCurrentUser({
          email: googleEmail,
          fullName: googleFullName && googleFullName.trim().length > 0 ? googleFullName : googleEmail,
        })
        navigate(googleProfile === 'required' ? '/groups?profile=required' : '/groups', { replace: true })
        return
      }
      setMessage('Google authentication successful.')
    }

    if (google === 'error') {
      setError('Google authentication failed. Please try again.')
    }

    if (google === 'not-configured') {
      setError('Google auth is not configured yet. Add GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in backend.')
    }
  }, [navigate, searchParams])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setMessage('')
    setIsSubmitting(true)

    try {
      const response = await login({ email, password })
      setCurrentUser({
        fullName: response.fullName ?? email,
        email: response.email ?? email,
      })
      navigate('/groups', { replace: true })
    } catch (submitError) {
      if (submitError instanceof Error) {
        setError(submitError.message)
      } else {
        setError('Unable to login right now')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthCard
      title="Welcome back"
      subtitle="Sign in to continue your study sessions"
      footer={
        <>
          Don&apos;t have an account? <Link to="/register">Sign up</Link>
        </>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        <label htmlFor="login-email">Email</label>
        <input
          id="login-email"
          type="email"
          placeholder="you@email.com"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />

        <label htmlFor="login-password">Password</label>
        <input
          id="login-password"
          type="password"
          placeholder="Enter your password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Signing In...' : 'Sign In'}
        </button>

        <a className="google-button" href={getGoogleAuthUrl()}>
          Continue with Google
        </a>

        {error ? <p className="auth-error">{error}</p> : null}
        {message ? <p className="auth-success">{message}</p> : null}
      </form>
    </AuthCard>
  )
}

export default LoginPage
