import type { AccuracyIssue } from '@/types/interview'

interface AccuracyIssuesProps {
  issues: AccuracyIssue[]
}

const AccuracyIssues = ({ issues }: AccuracyIssuesProps) => {
  if (issues.length === 0) return null

  return (
    <div className="space-y-2">
      <span className="text-[10px] font-bold uppercase tracking-widest text-orange-500">
        ⚠ 기술 오류
      </span>
      <div className="space-y-2">
        {issues.map((issue, idx) => (
          <div key={idx} className="rounded-xl bg-orange-50 border border-orange-100 p-3 space-y-1">
            <p className="text-[10px] font-semibold text-orange-400 uppercase tracking-wide">발화 내용</p>
            <p className="text-xs text-text-secondary leading-relaxed">{issue.claim}</p>
            <p className="text-[10px] font-semibold text-green-600 uppercase tracking-wide mt-2">정확한 내용</p>
            <p className="text-xs text-green-700 leading-relaxed">{issue.correction}</p>
          </div>
        ))}
      </div>
    </div>
  )
}

export default AccuracyIssues
