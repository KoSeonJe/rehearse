import { Routes, Route, Navigate } from 'react-router-dom'
import { HomePage } from '@/pages/home-page'
import { InterviewSetupPage } from '@/pages/interview-setup-page'
import { InterviewReadyPage } from '@/pages/interview-ready-page'
import { InterviewPage } from '@/pages/interview-page'
import { InterviewAnalysisPage } from '@/pages/interview-analysis-page'
import { InterviewFeedbackPage } from '@/pages/interview-feedback-page'
import { ProtectedRoute } from '@/components/ui/protected-route'
import { LoginModal } from '@/components/ui/login-modal'

export const App = () => {
  return (
    <>
      <Routes>
        <Route path="/" element={<HomePage />} />

        <Route element={<ProtectedRoute />}>
          <Route path="/interview/setup" element={<InterviewSetupPage />} />
          <Route path="/interview/:id/ready" element={<InterviewReadyPage />} />
          <Route path="/interview/:id/conduct" element={<InterviewPage />} />
          <Route path="/interview/:publicId/analysis" element={<InterviewAnalysisPage />} />
          <Route path="/interview/:publicId/feedback" element={<InterviewFeedbackPage />} />
          <Route path="/interview/:publicId/review" element={<InterviewFeedbackPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <LoginModal />
    </>
  )
}
