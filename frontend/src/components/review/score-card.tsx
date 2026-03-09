interface ScoreCardProps {
  score: number
}

const getScoreColor = (score: number): string => {
  if (score >= 80) return 'text-green-600'
  if (score >= 60) return 'text-blue-600'
  if (score >= 40) return 'text-amber-600'
  return 'text-red-600'
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
    <div className="flex flex-col items-center rounded-2xl border border-slate-200 bg-white p-8">
      <p className="mb-2 text-sm font-medium text-slate-500">종합 점수</p>
      <p className={`text-5xl font-bold ${color}`}>{score}</p>
      <p className={`mt-1 text-sm font-medium ${color}`}>{getScoreLabel(score)}</p>
      <div className="mt-4 h-2 w-full rounded-full bg-slate-100">
        <div
          className={`h-full rounded-full transition-all ${
            score >= 80 ? 'bg-green-500' : score >= 60 ? 'bg-blue-500' : score >= 40 ? 'bg-amber-500' : 'bg-red-500'
          }`}
          style={{ width: `${score}%` }}
        />
      </div>
    </div>
  )
}

export default ScoreCard
