import { useState, useRef, useCallback } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { StepJobField } from '@/components/onboarding/step-job-field'
import { StepDeviceTest } from '@/components/onboarding/step-device-test'
import { ProgressBar } from '@/components/onboarding/progress-bar'
import { TOTAL_STEPS } from '@/components/onboarding/constants'
import { useDeviceTest } from '@/hooks/use-device-test'
import type { JobField } from '@/components/onboarding/types'

export const OnboardingPage = () => {
  const navigate = useNavigate()
  const [step, setStep] = useState(0)
  const [selectedJob, setSelectedJob] = useState<JobField | null>(null)
  const [contentVisible, setContentVisible] = useState(true)
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const { permissions, micLevel, videoRef } = useDeviceTest(step === 1)

  const changeStep = useCallback((newStep: number) => {
    setContentVisible(false)
    if (timerRef.current) clearTimeout(timerRef.current)
    timerRef.current = setTimeout(() => {
      setStep(newStep)
      setContentVisible(true)
    }, 150)
  }, [])

  const canProceed = (): boolean => {
    switch (step) {
      case 0:
        return selectedJob !== null
      case 1:
        return true
      default:
        return false
    }
  }

  const handleNext = () => {
    if (step === TOTAL_STEPS - 1) {
      navigate('/interview/setup')
      return
    }
    changeStep(step + 1)
  }

  const handlePrev = () => {
    if (step > 0) {
      changeStep(step - 1)
    }
  }

  const isLastStep = step === TOTAL_STEPS - 1
  const nextLabel = isLastStep ? '시작하기' : '다음'

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Top bar */}
      <header className="flex items-center justify-between px-4 pt-4 sm:px-6">
        <div className="text-sm font-medium text-text-tertiary">
          {step + 1} / {TOTAL_STEPS}
        </div>
        <Link
          to="/interview/setup"
          className="text-sm text-text-tertiary transition-colors hover:text-text-secondary"
        >
          건너뛰기
        </Link>
      </header>

      <ProgressBar step={step} />

      {/* Content */}
      <main className="flex flex-1 flex-col items-center justify-center px-4 pb-32 sm:px-6">
        <div
          className="w-full transition-opacity duration-300 ease-out"
          style={{ opacity: contentVisible ? 1 : 0 }}
        >
          {step === 0 && (
            <StepJobField selected={selectedJob} onSelect={setSelectedJob} />
          )}
          {step === 1 && (
            <StepDeviceTest
              permissions={permissions}
              micLevel={micLevel}
              videoRef={videoRef}
            />
          )}
        </div>
      </main>

      {/* Bottom buttons */}
      <footer className="fixed bottom-0 left-0 right-0 border-t border-border bg-surface px-4 py-4 sm:px-6">
        <div className="mx-auto flex max-w-lg items-center gap-3">
          {step > 0 && (
            <Button variant="secondary" onClick={handlePrev} className="flex-1">
              이전
            </Button>
          )}
          <Button
            variant="primary"
            onClick={handleNext}
            disabled={!canProceed()}
            className="flex-1"
          >
            {nextLabel}
          </Button>
        </div>
      </footer>
    </div>
  )
}
