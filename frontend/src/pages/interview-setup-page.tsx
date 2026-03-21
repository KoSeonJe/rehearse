import { Logo } from '@/components/ui/logo'
import { BackLink } from '@/components/ui/back-link'
import { useInterviewSetup } from '@/hooks/use-interview-setup'
import { SetupProgressBar } from '@/components/setup/setup-progress-bar'
import { StepPosition } from '@/components/setup/step-position'
import { StepTechStack } from '@/components/setup/step-tech-stack'
import { StepLevel } from '@/components/setup/step-level'
import { StepDuration } from '@/components/setup/step-duration'
import { StepInterviewType } from '@/components/setup/step-interview-type'
import { SetupNavigation } from '@/components/setup/setup-navigation'

export const InterviewSetupPage = () => {
  const setup = useInterviewSetup()

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="flex items-center justify-between px-5 pt-8 md:px-8">
        <div className="flex items-center gap-2">
          <Logo size={80} />
          <span className="text-xl font-extrabold tracking-tight text-text-primary">
            리허설
          </span>
        </div>
        <BackLink to="/" />
      </header>

      <main className="mx-auto max-w-2xl px-5 pb-32 pt-12 md:px-8">
        <SetupProgressBar currentStep={setup.currentStep} totalSteps={setup.totalSteps} />

        {setup.currentStep === 1 && (
          <StepPosition
            position={setup.position}
            isLoading={setup.isLoading}
            onSelect={setup.handlePositionSelect}
          />
        )}

        {setup.currentStep === 2 && setup.position && (
          <StepTechStack
            position={setup.position}
            techStack={setup.techStack}
            onSelect={setup.handleTechStackSelect}
          />
        )}

        {setup.currentStep === 3 && (
          <StepLevel
            level={setup.level}
            isLoading={setup.isLoading}
            onSelect={setup.handleLevelSelect}
          />
        )}

        {setup.currentStep === 4 && (
          <StepDuration
            durationMinutes={setup.durationMinutes}
            isLoading={setup.isLoading}
            onSelect={setup.handleDurationSelect}
          />
        )}

        {setup.currentStep === 5 && setup.position && (
          <StepInterviewType
            position={setup.position}
            interviewTypes={setup.interviewTypes}
            csSubTopics={setup.csSubTopics}
            resumeFile={setup.resumeFile}
            dragOver={setup.dragOver}
            isLoading={setup.isLoading}
            fileInputRef={setup.fileInputRef}
            onTypeToggle={setup.handleTypeToggle}
            onCsSubTopicToggle={setup.handleCsSubTopicToggle}
            onFileSelect={setup.handleFileSelect}
            onFileRemove={setup.handleFileRemove}
            onDrop={setup.handleDrop}
            onDragOver={setup.handleDragOver}
            onDragLeave={setup.handleDragLeave}
          />
        )}

        <SetupNavigation
          currentStep={setup.currentStep}
          isSubmitStep={setup.isSubmitStep}
          canNext={setup.canNext(setup.currentStep)}
          isLoading={setup.isLoading}
          serverError={setup.serverError}
          onNext={setup.handleNext}
          onPrev={setup.handlePrev}
          onSubmit={setup.handleSubmit}
        />
      </main>
    </div>
  )
}
