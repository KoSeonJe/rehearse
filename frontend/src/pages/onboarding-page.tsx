import { useState, useEffect, useRef } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { StepJobField } from '@/components/onboarding/step-job-field'
import { StepDeviceTest } from '@/components/onboarding/step-device-test'
import { StepGuide } from '@/components/onboarding/step-guide'
import { ProgressBar } from '@/components/onboarding/progress-bar'
import { TOTAL_STEPS, GUIDE_SLIDES } from '@/components/onboarding/constants'
import { useDeviceTest } from '@/hooks/use-device-test'
import type { JobField } from '@/components/onboarding/types'

export const OnboardingPage = () => {
  const navigate = useNavigate()
  const [step, setStep] = useState(0)
  const [selectedJob, setSelectedJob] = useState<JobField | null>(null)
  const [slideIndex, setSlideIndex] = useState(0)
  const [contentVisible, setContentVisible] = useState(true)
  const prevStepRef = useRef(0)

  const { permissions, micLevel, videoRef } = useDeviceTest(step === 1)

  // 스텝 변경 시 fade-out → fade-in 트랜지션
  useEffect(() => {
    if (step !== prevStepRef.current) {
      setContentVisible(false)
      const timer = setTimeout(() => {
        setContentVisible(true)
      }, 150)
      prevStepRef.current = step
      return () => clearTimeout(timer)
    }
  }, [step])

  const canProceed = (): boolean => {
    switch (step) {
      case 0:
        return selectedJob !== null
      case 1:
        return true // 권한 없어도 진행 가능
      case 2:
        return true
      default:
        return false
    }
  }

  const handleNext = () => {
    if (step === TOTAL_STEPS - 1) {
      // 마지막 슬라이드가 아니면 다음 슬라이드
      if (slideIndex < GUIDE_SLIDES.length - 1) {
        setSlideIndex(slideIndex + 1)
        return
      }
      // 마지막 슬라이드 -> 시작하기
      navigate('/interview/setup')
      return
    }
    setStep(step + 1)
  }

  const handlePrev = () => {
    if (step === 2 && slideIndex > 0) {
      setSlideIndex(slideIndex - 1)
      return
    }
    if (step > 0) {
      setStep(step - 1)
    }
  }

  const isLastSlide = step === 2 && slideIndex === GUIDE_SLIDES.length - 1
  const nextLabel = isLastSlide ? '시작하기' : '다음'

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
          {step === 2 && (
            <StepGuide slideIndex={slideIndex} onSlideChange={setSlideIndex} />
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
