import { Character } from '@/components/ui/character'
import { GUIDE_SLIDES } from './constants'

interface StepGuideProps {
  slideIndex: number
  onSlideChange: (index: number) => void
}

export const StepGuide = ({ slideIndex, onSlideChange }: StepGuideProps) => {
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
