interface LevelBadgeProps {
  label: string
  value: string | null
  bg?: 'white' | 'gray'
}

const LevelBadge = ({ label, value, bg = 'white' }: LevelBadgeProps) => {
  const bgClass = bg === 'white' ? 'bg-card' : 'bg-muted'
  return (
    <div className={`${bgClass} rounded-xl p-3 text-center`}>
      <p className="text-xs text-text-tertiary mb-1">{label}</p>
      <p className="text-sm font-bold text-text-primary">{value ?? '—'}</p>
    </div>
  )
}

export default LevelBadge
