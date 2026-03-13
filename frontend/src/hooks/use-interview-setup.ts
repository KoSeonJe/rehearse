import { useState, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCreateInterview } from '@/hooks/use-interviews'
import { ApiError } from '@/lib/api-client'
import type {
  Position,
  Level,
  InterviewType,
  CsSubTopic,
} from '@/types/interview'
import { POSITION_INTERVIEW_TYPES } from '@/types/interview'

export const POSITIONS: Position[] = ['BACKEND', 'FRONTEND', 'DEVOPS', 'DATA_ENGINEER', 'FULLSTACK']
export const LEVELS: Level[] = ['JUNIOR', 'MID', 'SENIOR']
export const CS_SUB_TOPICS: CsSubTopic[] = ['DATA_STRUCTURE', 'OS', 'NETWORK', 'DATABASE']
export const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB

export const DURATION_PRESETS = [
  { minutes: 15, label: '15분', description: '빠른 연습' },
  { minutes: 30, label: '30분', description: '기본 면접' },
  { minutes: 45, label: '45분', description: '심화 면접' },
  { minutes: 60, label: '60분', description: '풀 면접' },
] as const

export type Step = 1 | 2 | 3 | 4

const TOTAL_STEPS = 4

export const useInterviewSetup = () => {
  const navigate = useNavigate()
  const createInterview = useCreateInterview()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [currentStep, setCurrentStep] = useState<Step>(1)
  const [position, setPosition] = useState<Position | null>(null)
  const [level, setLevel] = useState<Level | null>(null)
  const [durationMinutes, setDurationMinutes] = useState(30)
  const [interviewTypes, setInterviewTypes] = useState<InterviewType[]>([])
  const [csSubTopics, setCsSubTopics] = useState<CsSubTopic[]>([])
  const [resumeFile, setResumeFile] = useState<File | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)
  const [dragOver, setDragOver] = useState(false)

  const isLoading = createInterview.isPending
  const totalSteps = TOTAL_STEPS

  const canNext = (step: Step): boolean => {
    switch (step) {
      case 1:
        return position !== null
      case 2:
        return level !== null
      case 3:
        return durationMinutes >= 5 && durationMinutes <= 120
      case 4:
        return interviewTypes.length > 0
    }
  }

  const isSubmitStep = currentStep === totalSteps

  const handleNext = () => {
    if (!canNext(currentStep)) return
    if (currentStep < totalSteps) {
      setCurrentStep((currentStep + 1) as Step)
    }
  }

  const handlePrev = () => {
    if (currentStep > 1) {
      setCurrentStep((currentStep - 1) as Step)
    }
  }

  const handlePositionSelect = (p: Position) => {
    if (position !== p) {
      setPosition(p)
      const availableTypes = POSITION_INTERVIEW_TYPES[p]
      setInterviewTypes((prev) => prev.filter((t) => availableTypes.includes(t)))
    }
  }

  const handleTypeToggle = (type: InterviewType) => {
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
      return [...prev, type]
    })
  }

  const handleCsSubTopicToggle = (topic: CsSubTopic) => {
    setCsSubTopics((prev) =>
      prev.includes(topic) ? prev.filter((t) => t !== topic) : [...prev, topic],
    )
  }

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

  const handleFileRemove = () => {
    setResumeFile(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      setDragOver(false)
      const file = e.dataTransfer.files[0]
      if (file) handleFileSelect(file)
    },
    [handleFileSelect],
  )

  const handleSubmit = () => {
    if (!position || !level || interviewTypes.length === 0 || isLoading) return
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
        },
        resumeFile,
      },
      {
        onSuccess: (response) => {
          navigate(`/interview/${response.data.id}/ready`)
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
  }

  return {
    currentStep,
    position,
    level,
    durationMinutes,
    interviewTypes,
    csSubTopics,
    resumeFile,
    serverError,
    dragOver,
    isLoading,
    isSubmitStep,
    totalSteps,
    canNext,
    handleNext,
    handlePrev,
    handlePositionSelect,
    handleTypeToggle,
    handleCsSubTopicToggle,
    handleFileSelect,
    handleFileRemove,
    handleDrop,
    handleSubmit,
    setLevel,
    setDurationMinutes,
    setDragOver,
    fileInputRef,
  }
}
