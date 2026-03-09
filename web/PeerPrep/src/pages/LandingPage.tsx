import { Link } from 'react-router-dom'
import './LandingPage.css'

function LandingPage() {
  return (
    <div className="landing-page">
      <div className="landing-card">
        <h1 className="landing-brand">PeerPrep</h1>
        <p className="landing-subtitle">Find your study partner and prep smarter.</p>
        <div className="landing-actions">
          <Link className="landing-button primary" to="/login">
            Login
          </Link>
          <Link className="landing-button secondary" to="/register">
            Register
          </Link>
        </div>
      </div>
    </div>
  )
}

export default LandingPage