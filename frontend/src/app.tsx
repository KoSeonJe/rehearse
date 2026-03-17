import { Routes, Route } from 'react-router-dom'
import { HomePage } from '@/pages/home-page'
import { InterviewSetupPage } from '@/pages/interview-setup-page'
import { InterviewReadyPage } from '@/pages/interview-ready-page'
import { InterviewPage } from '@/pages/interview-page'
import { InterviewCompletePage } from '@/pages/interview-complete-page'
import { InterviewReportPage } from '@/pages/interview-report-page'
export const App = () => {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/interview/setup" element={<InterviewSetupPage />} />
      <Route path="/interview/:id/ready" element={<InterviewReadyPage />} />
      <Route path="/interview/:id/conduct" element={<InterviewPage />} />
      <Route path="/interview/:id/complete" element={<InterviewCompletePage />} />
      <Route path="/interview/:id/report" element={<InterviewReportPage />} />
    </Routes>
  )
}
