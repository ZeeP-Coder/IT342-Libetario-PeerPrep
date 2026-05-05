import type { ReactNode } from 'react'
import './AuthCard.css'

type AuthCardProps = {
  title: string
  subtitle: string
  children: ReactNode
  footer: ReactNode
}

function AuthCard({ title, subtitle, children, footer }: AuthCardProps) {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1 className="brand">PeerPrep</h1>
        <h2 className="auth-title">{title}</h2>
        <p className="auth-subtitle">{subtitle}</p>

        {children}

        <p className="auth-link-text">{footer}</p>
      </div>
    </div>
  )
}

export default AuthCard