import { createElement } from 'react'
import { ServerIcon } from '@/components/icons/server-icon'
import { CodeIcon } from '@/components/icons/code-icon'
import { LayersIcon } from '@/components/icons/layers-icon'
import { GlobeIcon } from '@/components/icons/globe-icon'
import { NetworkIcon } from '@/components/icons/network-icon'
import { SmartphoneIcon } from '@/components/icons/smartphone-icon'
import type { JobOption } from './types'

export const TOTAL_STEPS = 3

export const JOB_OPTIONS: JobOption[] = [
  {
    id: 'backend',
    label: '백엔드',
    icon: createElement(ServerIcon),
  },
  {
    id: 'frontend',
    label: '프론트엔드',
    icon: createElement(CodeIcon),
  },
  {
    id: 'fullstack',
    label: '풀스택',
    icon: createElement(LayersIcon),
  },
  {
    id: 'devops',
    label: 'DevOps',
    icon: createElement(GlobeIcon),
  },
  {
    id: 'data-ai',
    label: '데이터/AI',
    icon: createElement(NetworkIcon),
  },
  {
    id: 'mobile',
    label: '모바일',
    icon: createElement(SmartphoneIcon),
  },
]

export const GUIDE_SLIDES = [
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
