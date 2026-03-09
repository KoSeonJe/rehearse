interface ScoreCardProps {
  score: number
}

const getScoreColor = (score: number): string => {
  if (score >= 80) return 'text-success'
  if (score >= 60) return 'text-info'
  if (score >= 40) return 'text-warning'
  return 'text-error'
}

const getScoreLabel = (score: number): string => {
  if (score >= 80) return '우수'
  if (score >= 60) return '양호'
  if (score >= 40) return '보통'
  return '개선 필요'
}

const ScoreCard = ({ score }: ScoreCardProps) => {
  const color = getScoreColor(score)

  return (
    <div className="flex flex-col items-center rounded-card border border-border bg-surface p-8">
      <p className="mb-2 text-sm font-medium text-text-secondary">종합 점수</p>
      <p className={`text-5xl font-bold ${color}`}>{score}</p>
      <p className={`mt-1 text-sm font-medium ${color}`}>{getScoreLabel(score)}</p>
      <div className="mt-4 h-2 w-full rounded-full bg-border">
        <div
          className={`h-full rounded-full transition-all ${
            score >= 80 ? 'bg-success' : score >= 60 ? 'bg-info' : score >= 40 ? 'bg-warning' : 'bg-error'
          }`}
          style={{ width: `${score}%` }}
        />
      </div>
    </div>
  )
}

export default ScoreCard
