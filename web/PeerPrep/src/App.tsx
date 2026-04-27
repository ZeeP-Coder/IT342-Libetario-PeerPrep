import { Navigate, Route, Routes } from 'react-router-dom'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import ProfilePage from './pages/ProfilePage'
import RegisterPage from './pages/RegisterPage'
import StudyGroupDetailsPage from './pages/StudyGroupDetailsPage'
import StudyGroupsPage from './pages/StudyGroupsPage'

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
