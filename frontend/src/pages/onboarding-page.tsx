import { useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Character } from '@/components/ui/character'

// ─── Types ───────────────────────────────────────────────────────────────────

type JobField =
  | 'backend'
  | 'frontend'
  | 'fullstack'
  | 'devops'
  | 'data-ai'
  | 'mobile'

interface JobOption {
  id: JobField
  label: string
  icon: React.ReactNode
}

type PermissionStatus = 'idle' | 'granted' | 'denied'

interface DevicePermissions {
  camera: PermissionStatus
  microphone: PermissionStatus
}

// ─── Constants ───────────────────────────────────────────────────────────────

const TOTAL_STEPS = 3

const JOB_OPTIONS: JobOption[] = [
  {
    id: 'backend',
    label: '백엔드',
    icon: (
      <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="4" y="6" width="24" height="6" rx="1.5" stroke="currentColor" strokeWidth="1.8" />
        <rect x="4" y="14" width="24" height="6" rx="1.5" stroke="currentColor" strokeWidth="1.8" />
        <rect x="4" y="22" width="24" height="6" rx="1.5" stroke="currentColor" strokeWidth="1.8" />
        <circle cx="8" cy="9" r="1.2" fill="currentColor" />
        <circle cx="8" cy="17" r="1.2" fill="currentColor" />
        <circle cx="8" cy="25" r="1.2" fill="currentColor" />
      </svg>
    ),
  },
  {
    id: 'frontend',
    label: '프론트엔드',
    icon: (
      <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="3" y="5" width="26" height="20" rx="2" stroke="currentColor" strokeWidth="1.8" />
        <path d="M10 14L7 17L10 20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M22 14L25 17L22 20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M18 12L14 22" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      </svg>
    ),
  },
  {
    id: 'fullstack',
    label: '풀스택',
    icon: (
      <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="4" y="4" width="24" height="10" rx="2" stroke="currentColor" strokeWidth="1.8" />
        <rect x="4" y="18" width="24" height="10" rx="2" stroke="currentColor" strokeWidth="1.8" />
        <path d="M16 14V18" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
        <circle cx="8" cy="9" r="1.2" fill="currentColor" />
        <path d="M12 22L10 24L12 26" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M20 22L22 24L20 26" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
  },
  {
    id: 'devops',
    label: 'DevOps',
    icon: (
      <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="16" cy="16" r="11" stroke="currentColor" strokeWidth="1.8" />
        <path d="M16 5C20 9 22 12.5 22 16C22 19.5 20 23 16 27" stroke="currentColor" strokeWidth="1.8" />
        <path d="M16 5C12 9 10 12.5 10 16C10 19.5 12 23 16 27" stroke="currentColor" strokeWidth="1.8" />
        <path d="M5 16H27" stroke="currentColor" strokeWidth="1.8" />
      </svg>
    ),
  },
  {
    id: 'data-ai',
    label: '데이터/AI',
    icon: (
      <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="16" cy="16" r="4" stroke="currentColor" strokeWidth="1.8" />
        <circle cx="8" cy="8" r="2.5" stroke="currentColor" strokeWidth="1.5" />
        <circle cx="24" cy="8" r="2.5" stroke="currentColor" strokeWidth="1.5" />
        <circle cx="8" cy="24" r="2.5" stroke="currentColor" strokeWidth="1.5" />
        <circle cx="24" cy="24" r="2.5" stroke="currentColor" strokeWidth="1.5" />
        <path d="M10 10L13 13" stroke="currentColor" strokeWidth="1.5" />
        <path d="M22 10L19 13" stroke="currentColor" strokeWidth="1.5" />
        <path d="M10 22L13 19" stroke="currentColor" strokeWidth="1.5" />
        <path d="M22 22L19 19" stroke="currentColor" strokeWidth="1.5" />
      </svg>
    ),
  },
  {
    id: 'mobile',
    label: '모바일',
    icon: (
      <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="9" y="3" width="14" height="26" rx="3" stroke="currentColor" strokeWidth="1.8" />
        <path d="M9 7H23" stroke="currentColor" strokeWidth="1.5" />
        <path d="M9 25H23" stroke="currentColor" strokeWidth="1.5" />
        <circle cx="16" cy="27.5" r="1" fill="currentColor" />
      </svg>
    ),
  },
]

const GUIDE_SLIDES = [
  {
    mood: 'thinking' as const,
    title: 'AI가 맞춤 질문을 생성해요',
    description: '이력서와 직무에 맞는 면접 질문을 AI가 실시간으로 만들어드려요.',
  },
  {
    mood: 'default' as const,
    title: '자연스럽게 답변하세요',
    description: '실제 면접처럼 카메라를 보며 편안하게 답변하면 돼요.',
  },
  {
    mood: 'happy' as const,
    title: '영상과 함께 피드백을 확인하세요',
    description: '녹화된 영상의 타임스탬프에 맞춰 상세한 피드백을 확인할 수 있어요.',
  },
]

// ─── Step 1: Job Field Selection ─────────────────────────────────────────────

interface StepJobFieldProps {
  selected: JobField | null
  onSelect: (field: JobField) => void
}

const StepJobField = ({ selected, onSelect }: StepJobFieldProps) => {
  return (
    <div className="flex flex-col items-center">
      <h1 className="text-2xl font-semibold text-text-primary">
        어떤 면접을 준비하세요?
      </h1>
      <p className="mt-2 text-sm text-text-secondary">
        직무 분야를 선택해주세요
      </p>

      <div
        role="radiogroup"
        aria-label="직무 분야 선택"
        className="mt-8 grid w-full max-w-md grid-cols-2 gap-3 sm:grid-cols-3"
      >
        {JOB_OPTIONS.map((option) => (
          <button
            key={option.id}
            type="button"
            role="radio"
            aria-checked={selected === option.id}
            onClick={() => onSelect(option.id)}
            className={[
              'flex flex-col items-center gap-2 rounded-card border p-4 transition-all duration-150',
              'hover:border-accent/40 hover:bg-accent-light/30',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2',
              selected === option.id
                ? 'border-accent bg-accent-light/20 text-accent'
                : 'border-border bg-surface text-text-secondary',
            ].join(' ')}
          >
            <span className="flex h-10 w-10 items-center justify-center">
              {option.icon}
            </span>
            <span className="text-sm font-medium">{option.label}</span>
          </button>
        ))}
      </div>
    </div>
  )
}

// ─── Step 2: Device Permission Test ──────────────────────────────────────────

interface StepDeviceTestProps {
  permissions: DevicePermissions
  micLevel: number
  videoRef: React.RefObject<HTMLVideoElement | null>
}

const PermissionBadge = ({ status, label }: { status: PermissionStatus; label: string }) => {
  const icon = status === 'granted' ? '\u2705' : status === 'denied' ? '\u274C' : '\u23F3'
  const style =
    status === 'granted'
      ? 'bg-success-light text-success border-success/30'
      : status === 'denied'
        ? 'bg-error-light text-error border-error/30'
        : 'bg-background text-text-tertiary border-border'

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-badge border px-3 py-1 text-xs font-medium ${style}`}
    >
      <span aria-hidden="true">{icon}</span>
      {label}
    </span>
  )
}

const StepDeviceTest = ({ permissions, micLevel, videoRef }: StepDeviceTestProps) => {
  return (
    <div className="flex flex-col items-center">
      <h1 className="text-2xl font-semibold text-text-primary">
        카메라 &amp; 마이크 테스트
      </h1>
      <p className="mt-2 text-sm text-text-secondary">
        면접 녹화를 위해 카메라와 마이크 권한이 필요해요
      </p>

      <div className="mt-6 flex items-center gap-3">
        <PermissionBadge status={permissions.camera} label="카메라" />
        <PermissionBadge status={permissions.microphone} label="마이크" />
      </div>

      {/* Camera Preview */}
      <div className="mt-6 w-full max-w-sm overflow-hidden rounded-card border border-border bg-black">
        <div className="relative aspect-video w-full">
          <video
            ref={videoRef as React.RefObject<HTMLVideoElement>}
            autoPlay
            playsInline
            muted
            className="h-full w-full object-cover"
          />
          {permissions.camera === 'idle' && (
            <div className="absolute inset-0 flex items-center justify-center bg-black/80">
              <p className="text-sm text-text-tertiary">카메라 권한을 허용해주세요</p>
            </div>
          )}
          {permissions.camera === 'denied' && (
            <div className="absolute inset-0 flex items-center justify-center bg-black/80">
              <p className="text-sm text-error">카메라 접근이 거부되었습니다</p>
            </div>
          )}
        </div>
      </div>

      {/* Mic Level */}
      <div className="mt-4 w-full max-w-sm">
        <div className="mb-1 flex items-center justify-between">
          <span className="text-xs font-medium text-text-secondary">마이크 레벨</span>
          <span className="text-xs text-text-tertiary">
            {permissions.microphone === 'granted' ? '말해보세요' : '권한 필요'}
          </span>
        </div>
        <div className="h-2 w-full overflow-hidden rounded-badge bg-border">
          <div
            className="h-full rounded-badge bg-accent transition-all duration-75"
            style={{ width: `${Math.min(micLevel, 100)}%` }}
          />
        </div>
      </div>

      {permissions.camera === 'denied' || permissions.microphone === 'denied' ? (
        <p className="mt-4 max-w-sm text-center text-xs text-text-tertiary">
          브라우저 설정에서 카메라/마이크 권한을 허용한 뒤 페이지를 새로고침해주세요.
        </p>
      ) : null}
    </div>
  )
}

// ─── Step 3: Guide Carousel ──────────────────────────────────────────────────

interface StepGuideProps {
  slideIndex: number
  onSlideChange: (index: number) => void
}

const StepGuide = ({ slideIndex, onSlideChange }: StepGuideProps) => {
  const slide = GUIDE_SLIDES[slideIndex]

  return (
    <div className="flex flex-col items-center">
      <h1 className="text-2xl font-semibold text-text-primary">
        시작하기 전에
      </h1>

      <div className="mt-8 flex flex-col items-center">
        <Character mood={slide.mood} size={140} />
        <h2 className="mt-6 text-lg font-semibold text-text-primary">
          {slide.title}
        </h2>
        <p className="mt-2 max-w-xs text-center text-sm text-text-secondary">
          {slide.description}
        </p>
      </div>

      {/* Dot Indicator */}
      <div className="mt-8 flex items-center gap-2" role="tablist" aria-label="가이드 슬라이드">
        {GUIDE_SLIDES.map((_, i) => (
          <button
            key={i}
            type="button"
            role="tab"
            aria-selected={slideIndex === i}
            aria-label={`슬라이드 ${i + 1}`}
            onClick={() => onSlideChange(i)}
            className={[
              'h-2 rounded-badge transition-all duration-200',
              slideIndex === i
                ? 'w-6 bg-accent'
                : 'w-2 bg-border hover:bg-text-tertiary',
            ].join(' ')}
          />
        ))}
      </div>
    </div>
  )
}

// ─── Progress Bar ────────────────────────────────────────────────────────────

const ProgressBar = ({ step }: { step: number }) => {
  const progress = ((step + 1) / TOTAL_STEPS) * 100

  return (
    <div className="h-1 w-full bg-border">
      <div
        className="h-full bg-accent transition-all duration-300 ease-out"
        style={{ width: `${progress}%` }}
      />
    </div>
  )
}

// ─── useDeviceTest Hook ──────────────────────────────────────────────────────

const useDeviceTest = (active: boolean) => {
  const [permissions, setPermissions] = useState<DevicePermissions>({
    camera: 'idle',
    microphone: 'idle',
  })
  const [micLevel, setMicLevel] = useState(0)
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const audioCtxRef = useRef<AudioContext | null>(null)
  const animFrameRef = useRef<number>(0)

  const cleanup = useCallback(() => {
    if (animFrameRef.current) {
      cancelAnimationFrame(animFrameRef.current)
      animFrameRef.current = 0
    }
    if (audioCtxRef.current) {
      audioCtxRef.current.close().catch(() => {})
      audioCtxRef.current = null
    }
    if (streamRef.current) {
      for (const track of streamRef.current.getTracks()) {
        track.stop()
      }
      streamRef.current = null
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null
    }
    setMicLevel(0)
  }, [])

  useEffect(() => {
    if (!active) {
      cleanup()
      return
    }

    let cancelled = false

    const init = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: true,
          audio: true,
        })

        if (cancelled) {
          for (const track of stream.getTracks()) track.stop()
          return
        }

        streamRef.current = stream

        // Camera
        if (videoRef.current) {
          videoRef.current.srcObject = stream
        }
        setPermissions((prev) => ({ ...prev, camera: 'granted' }))

        // Microphone level
        const audioCtx = new AudioContext()
        audioCtxRef.current = audioCtx
        const source = audioCtx.createMediaStreamSource(stream)
        const analyser = audioCtx.createAnalyser()
        analyser.fftSize = 256
        source.connect(analyser)

        const dataArray = new Uint8Array(analyser.frequencyBinCount)
        let prevLevel = 0

        const tick = () => {
          if (cancelled) return
          analyser.getByteFrequencyData(dataArray)
          let sum = 0
          for (let i = 0; i < dataArray.length; i++) {
            sum += dataArray[i]
          }
          const avg = sum / dataArray.length
          const newLevel = Math.round((avg / 255) * 100)
          if (Math.abs(newLevel - prevLevel) >= 2) {
            prevLevel = newLevel
            setMicLevel(newLevel)
          }
          animFrameRef.current = requestAnimationFrame(tick)
        }
        tick()

        setPermissions((prev) => ({ ...prev, microphone: 'granted' }))
      } catch {
        if (cancelled) return
        setPermissions({ camera: 'denied', microphone: 'denied' })
      }
    }

    init()

    return () => {
      cancelled = true
      cleanup()
    }
  }, [active, cleanup])

  return { permissions, micLevel, videoRef }
}

// ─── Main Page ───────────────────────────────────────────────────────────────

const OnboardingPage = () => {
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

export default OnboardingPage
