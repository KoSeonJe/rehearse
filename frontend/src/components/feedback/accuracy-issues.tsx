import type { AccuracyIssue } from '@/types/interview'

interface AccuracyIssuesProps {
  issues: AccuracyIssue[]
}

const AccuracyIssues = ({ issues }: AccuracyIssuesProps) => {
  if (issues.length === 0) return null

  return (
    <div className="space-y-3">
      {issues.map((issue, idx) => (
        <div key={idx} className="rounded-xl bg-gray-50 p-4 space-y-3">
          <div>
            <p className="text-[13px] font-bold text-gray-400 mb-1">내가 한 말</p>
            <p className="text-[15px] text-gray-400 leading-[1.7] line-through decoration-gray-300">
              {issue.claim}
            </p>
          </div>
          <div className="border-t border-gray-200 pt-3">
            <p className="text-[13px] font-bold text-gray-900 mb-1">정확한 내용</p>
            <p className="text-[15px] text-gray-700 leading-[1.7]">{issue.correction}</p>
          </div>
        </div>
      ))}
    </div>
  )
}

export default AccuracyIssues
