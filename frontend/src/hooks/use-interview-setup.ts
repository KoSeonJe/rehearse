import { useState, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCreateInterview } from '@/hooks/use-interviews'
import { ApiError } from '@/lib/api-client'
import { POSITION_INTERVIEW_TYPES, POSITION_TECH_STACKS } from '@/constants/interview-labels'
import { TOTAL_STEPS, MAX_FILE_SIZE } from '@/constants/setup'
import type { Step } from '@/constants/setup'
import type {
  Position,
  Level,
  InterviewType,
  CsSubTopic,
  TechStack,
} from '@/types/interview'

const toStep = (n: number): Step | null => {
  if (n >= 1 && n <= TOTAL_STEPS) return n as Step
  return null
}

export const useInterviewSetup = () => {
  const navigate = useNavigate()
  const createInterview = useCreateInterview()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [currentStep, setCurrentStep] = useState<Step>(1)
  const [position, setPosition] = useState<Position | null>(null)
  const [techStack, setTechStack] = useState<TechStack | null>(null)
  const [level, setLevel] = useState<Level | null>('JUNIOR')
  const [durationMinutes, setDurationMinutes] = useState(30)
  const [interviewTypes, setInterviewTypes] = useState<InterviewType[]>([])
  const [csSubTopics, setCsSubTopics] = useState<CsSubTopic[]>([])
  const [resumeFile, setResumeFile] = useState<File | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)
  const [dragOver, setDragOver] = useState(false)

  const isLoading = createInterview.isPending

  const canNext = useCallback((step: Step): boolean => {
    switch (step) {
      case 1:
        return position !== null
      case 2:
        return true
      case 3:
        return level !== null
      case 4:
        return durationMinutes >= 5 && durationMinutes <= 120
      case 5:
        if (interviewTypes.length === 0) return false
        if (interviewTypes.includes('RESUME_BASED') && !resumeFile) return false
        return true
    }
  }, [position, level, durationMinutes, interviewTypes, resumeFile])

  const isSubmitStep = currentStep === TOTAL_STEPS

  const handleNext = useCallback(() => {
    if (!canNext(currentStep)) return
    const next = toStep(currentStep + 1)
    if (next) setCurrentStep(next)
  }, [canNext, currentStep])

  const handlePrev = useCallback(() => {
    const prev = toStep(currentStep - 1)
    if (prev) setCurrentStep(prev)
  }, [currentStep])

  const handlePositionSelect = useCallback((p: Position) => {
    if (position !== p) {
      setPosition(p)
      const stacks = POSITION_TECH_STACKS[p]
      setTechStack(stacks.length > 0 ? stacks[0] : null)
      const availableTypes = POSITION_INTERVIEW_TYPES[p]
      setInterviewTypes((prev) => prev.filter((t) => availableTypes.includes(t)))
    }
  }, [position])

  const handleLevelSelect = useCallback((l: Level) => {
    setLevel(l)
  }, [])

  const handleDurationSelect = useCallback((minutes: number) => {
    setDurationMinutes(minutes)
  }, [])

  const isOtherTypesDisabled = interviewTypes.includes('RESUME_BASED')

  const handleTypeToggle = useCallback((type: InterviewType) => {
    setInterviewTypes((prev) => {
      if (prev.includes(type)) {
        const next = prev.filter((t) => t !== type)
        if (type === 'CS_FUNDAMENTAL') setCsSubTopics([])
        if (type === 'RESUME_BASED') {
          setResumeFile(null)
          if (fileInputRef.current) fileInputRef.current.value = ''
        }
        return next
      }

      // RESUME_BASED 활성화: 기존 선택 모두 해제하고 단독 선택
      if (type === 'RESUME_BASED') {
        setCsSubTopics([])
        return ['RESUME_BASED']
      }

      // 다른 타입 선택 시 RESUME_BASED가 이미 활성화면 토글 차단
      if (prev.includes('RESUME_BASED')) {
        return prev
      }

      return [...prev, type]
    })
  }, [])

  const handleCsSubTopicToggle = useCallback((topic: CsSubTopic) => {
    setCsSubTopics((prev) =>
      prev.includes(topic) ? prev.filter((t) => t !== topic) : [...prev, topic],
    )
  }, [])

  const handleFileSelect = useCallback((file: File) => {
    if (file.type !== 'application/pdf') {
      setServerError('PDF 파일만 업로드할 수 있습니다.')
      return
    }
    if (file.size > MAX_FILE_SIZE) {
      setServerError('파일 크기는 10MB 이하여야 합니다.')
      return
    }
    setServerError(null)
    setResumeFile(file)
  }, [])

  const handleFileRemove = useCallback(() => {
    setResumeFile(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }, [])

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      setDragOver(false)
      const file = e.dataTransfer.files[0]
      if (file) handleFileSelect(file)
    },
    [handleFileSelect],
  )

  const handleDragOver = useCallback(() => {
    setDragOver(true)
  }, [])

  const handleDragLeave = useCallback(() => {
    setDragOver(false)
  }, [])

  const handleTechStackSelect = useCallback((stack: TechStack | null) => {
    setTechStack(stack)
  }, [])

  const handleSubmit = useCallback(() => {
    if (!position || !level || interviewTypes.length === 0 || isLoading) return
    if (interviewTypes.includes('RESUME_BASED') && !resumeFile) return
    setServerError(null)

    createInterview.mutate(
      {
        request: {
          position,
          level,
          interviewTypes,
          durationMinutes,
          csSubTopics: interviewTypes.includes('CS_FUNDAMENTAL') && csSubTopics.length > 0
            ? csSubTopics
            : undefined,
          techStack: techStack ?? undefined,
        },
        resumeFile,
      },
      {
        onSuccess: (response) => {
          navigate(`/interview/${response.data.id}/ready`, { replace: true })
        },
        onError: (error) => {
          if (error instanceof ApiError) {
            setServerError(error.message || '오류가 발생했습니다.')
          } else {
            setServerError('오류가 발생했습니다.')
          }
        },
      },
    )
  }, [position, level, interviewTypes, durationMinutes, csSubTopics, techStack, resumeFile, isLoading, createInterview, navigate])

  return {
    currentStep,
    position,
    techStack,
    level,
    durationMinutes,
    interviewTypes,
    csSubTopics,
    resumeFile,
    serverError,
    dragOver,
    isLoading,
    isSubmitStep,
    isOtherTypesDisabled,
    totalSteps: TOTAL_STEPS,
    canNext,
    handleNext,
    handlePrev,
    handlePositionSelect,
    handleTechStackSelect,
    handleLevelSelect,
    handleDurationSelect,
    handleTypeToggle,
    handleCsSubTopicToggle,
    handleFileSelect,
    handleFileRemove,
    handleDrop,
    handleDragOver,
    handleDragLeave,
    handleSubmit,
    fileInputRef,
  }
}
