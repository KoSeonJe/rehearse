interface ImprovementListProps {
  title: string
  items: string[]
  variant: 'strength' | 'improvement'
}

const ImprovementList = ({ title, items, variant }: ImprovementListProps) => {
  const isStrength = variant === 'strength'

  return (
    <div className={`rounded-card border p-6 ${
      isStrength ? 'border-success/20 bg-success-light' : 'border-warning/20 bg-warning-light'
    }`}>
      <h3 className={`mb-4 text-sm font-semibold ${
        isStrength ? 'text-success' : 'text-warning'
      }`}>
        {title}
      </h3>
      <ul className="space-y-3">
        {items.map((item, index) => (
          <li key={index} className="flex items-start gap-3">
            <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-xs font-bold text-white ${
              isStrength ? 'bg-success' : 'bg-warning'
            }`}>
              {index + 1}
            </span>
            <span className="text-sm leading-relaxed text-text-primary">
              {item}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default ImprovementList
