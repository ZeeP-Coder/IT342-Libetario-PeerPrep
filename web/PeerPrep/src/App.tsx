import { Navigate, Route, Routes } from 'react-router-dom'
import LandingPage from './pages/LandingPage'
import LoginPage from './features/auth/pages/LoginPage'
import RegisterPage from './features/auth/pages/RegisterPage'
import ProfilePage from './features/profile/pages/ProfilePage'
import StudyGroupDetailsPage from './features/study-groups/pages/StudyGroupDetailsPage'
import StudyGroupsPage from './features/study-groups/pages/StudyGroupsPage'

function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/groups" element={<StudyGroupsPage />} />
      <Route path="/profile" element={<ProfilePage />} />
      <Route path="/groups/:groupId" element={<StudyGroupDetailsPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
