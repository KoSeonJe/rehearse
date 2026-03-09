import { Routes, Route } from 'react-router-dom'
import { HomePage } from '@/pages/home-page'
import { InterviewSetupPage } from '@/pages/interview-setup-page'
import { InterviewReadyPage } from '@/pages/interview-ready-page'

export const App = () => {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/interview/setup" element={<InterviewSetupPage />} />
      <Route path="/interview/:id/ready" element={<InterviewReadyPage />} />
    </Routes>
  )
}
