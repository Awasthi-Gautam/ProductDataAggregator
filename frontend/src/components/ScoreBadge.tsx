interface Props {
  value: number | null
  type: 'velocity' | 'popularity'
}

export default function ScoreBadge({ value, type }: Props) {
  if (value == null) return <span className="text-gray-400 text-xs">—</span>

  if (type === 'velocity') {
    const color = value > 5 ? 'text-green-600' : value > 0 ? 'text-emerald-500' : 'text-red-500'
    const arrow = value > 0 ? '▲' : '▼'
    return (
      <span className={`font-semibold text-sm ${color}`}>
        {arrow} {Math.abs(value).toFixed(1)}
      </span>
    )
  }

  return (
    <span className="text-gray-700 font-medium text-sm">
      {value.toFixed(1)}
    </span>
  )
}
