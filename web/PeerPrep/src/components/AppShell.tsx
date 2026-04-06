import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import './AppShell.css'

type AppShellProps = {
  title: string
  subtitle: string
  userName: string
  actions?: ReactNode
  children: ReactNode
}

function AppShell({ title, subtitle, userName, actions, children }: AppShellProps) {
  return (
    <div className="app-shell">
      <header className="app-header">
        <Link className="app-brand" to="/groups">
          <span className="app-brand-mark">P</span>
          <span>PeerPrep</span>
        </Link>

        <div className="app-header-actions">
          <span className="app-user-chip">{userName}</span>
          {actions}
        </div>
      </header>

      <main className="app-main">
        <section className="app-hero">
          <div>
            <p className="app-kicker">Collaborative study planning</p>
            <h1>{title}</h1>
            <p>{subtitle}</p>
          </div>
        </section>

        {children}
      </main>
    </div>
  )
}

export default AppShell
