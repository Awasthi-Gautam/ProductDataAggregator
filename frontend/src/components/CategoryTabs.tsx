import { CATEGORIES } from '../types'

interface Props {
  selected: string
  onChange: (cat: string) => void
}

export default function CategoryTabs({ selected, onChange }: Props) {
  return (
    <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
      {CATEGORIES.map(cat => (
        <button
          key={cat.value}
          onClick={() => onChange(cat.value)}
          className={`shrink-0 px-3 py-1 rounded-full text-sm font-medium transition-colors ${
            selected === cat.value
              ? 'bg-orange-500 text-white'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          {cat.label}
        </button>
      ))}
    </div>
  )
}
