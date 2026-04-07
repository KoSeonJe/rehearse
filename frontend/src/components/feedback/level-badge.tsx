interface LevelBadgeProps {
  label: string
  value: string | null
  bg?: 'white' | 'gray'
}

const LevelBadge = ({ label, value, bg = 'white' }: LevelBadgeProps) => {
  const bgClass = bg === 'white' ? 'bg-white' : 'bg-gray-50'
  return (
    <div className={`${bgClass} rounded-xl p-3 text-center`}>
      <p className="text-[12px] text-gray-400 mb-1">{label}</p>
      <p className="text-[15px] font-bold text-gray-900">{value ?? '—'}</p>
    </div>
  )
}

export default LevelBadge
