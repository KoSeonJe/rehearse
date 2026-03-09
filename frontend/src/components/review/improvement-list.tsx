interface ImprovementListProps {
  title: string
  items: string[]
  variant: 'strength' | 'improvement'
}

const ImprovementList = ({ title, items, variant }: ImprovementListProps) => {
  const isStrength = variant === 'strength'

  return (
    <div className={`rounded-2xl border p-6 ${
      isStrength ? 'border-green-100 bg-green-50' : 'border-amber-100 bg-amber-50'
    }`}>
      <h3 className={`mb-4 text-sm font-semibold ${
        isStrength ? 'text-green-800' : 'text-amber-800'
      }`}>
        {title}
      </h3>
      <ul className="space-y-3">
        {items.map((item, index) => (
          <li key={index} className="flex items-start gap-3">
            <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-xs font-bold text-white ${
              isStrength ? 'bg-green-500' : 'bg-amber-500'
            }`}>
              {index + 1}
            </span>
            <span className={`text-sm leading-relaxed ${
              isStrength ? 'text-green-900' : 'text-amber-900'
            }`}>
              {item}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default ImprovementList
