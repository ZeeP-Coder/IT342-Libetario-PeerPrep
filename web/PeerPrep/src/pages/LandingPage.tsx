import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getCurrentUser } from '../services/sessionService'
import './LandingPageV2.css'

function LandingPage() {
  const navigate = useNavigate()

  useEffect(() => {
    if (getCurrentUser()) {
      navigate('/groups', { replace: true })
    }
  }, [navigate])

  return (
    <div className="landing-page">
      <header className="landing-nav">
        <Link className="landing-brand" to="/">
          <span className="landing-brand-mark">P</span>
          <span>PeerPrep</span>
        </Link>
        <div className="landing-nav-actions">
          <Link className="landing-button ghost" to="/login">
            Login
          </Link>
          <Link className="landing-button primary" to="/register">
            Get Started
          </Link>
        </div>
      </header>

      <main className="landing-content">
        <section className="landing-hero">
          <div className="landing-copy">
            <p className="landing-kicker">Study better, together</p>
            <h1>Create and join study groups that actually stay organized.</h1>
            <p>
              PeerPrep connects students to focused study groups, shared schedules, and the people already working on the
              same subjects.
            </p>
            <div className="landing-actions">
              <Link className="landing-button primary" to="/register">
                Start a Group
              </Link>
              <Link className="landing-button secondary" to="/login">
                Sign In
              </Link>
            </div>
          </div>

          <div className="landing-preview">
            <article>
              <span>Data Structures</span>
              <strong>Wednesday</strong>
              <p>6:00 PM - 8:00 PM</p>
            </article>
            <article>
              <span>Organic Chemistry</span>
              <strong>Friday</strong>
              <p>2:00 PM - 4:00 PM</p>
            </article>
            <article>
              <span>Machine Learning</span>
              <strong>Open for joining</strong>
              <p>4 spots left</p>
            </article>
          </div>
        </section>
      </main>
    </div>
  )
}

export default LandingPage