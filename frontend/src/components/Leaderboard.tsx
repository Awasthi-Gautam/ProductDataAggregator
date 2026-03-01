import { Link } from 'react-router-dom'
import type { TrendingProduct } from '../types'
import ScoreBadge from './ScoreBadge'

interface Props {
  products: TrendingProduct[]
}

const RANK_COLORS = ['text-yellow-500', 'text-gray-400', 'text-amber-600']

function categoryLabel(cat: string | null) {
  if (!cat) return null
  return cat.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

export default function Leaderboard({ products }: Props) {
  if (products.length === 0) {
    return (
      <div className="text-center py-20 text-gray-400">
        No products found. Try a different category or run the collector.
      </div>
    )
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
      {/* Header row */}
      <div className="grid grid-cols-[3rem_3rem_1fr_8rem_6rem_6rem] gap-3 px-4 py-2 bg-gray-50 border-b border-gray-100 text-xs font-semibold text-gray-500 uppercase tracking-wide">
        <div>#</div>
        <div></div>
        <div>Product</div>
        <div>Category</div>
        <div className="text-right">Popularity</div>
        <div className="text-right">Velocity</div>
      </div>

      {products.map((p, i) => (
        <Link
          key={p.productId}
          to={`/products/${p.productId}`}
          className="grid grid-cols-[3rem_3rem_1fr_8rem_6rem_6rem] gap-3 px-4 py-3 items-center hover:bg-orange-50 transition-colors border-b border-gray-50 last:border-0 group"
        >
          {/* Rank */}
          <div className={`font-bold text-base ${RANK_COLORS[i] ?? 'text-gray-400'}`}>
            {p.rank}
          </div>

          {/* Thumbnail */}
          <div className="w-10 h-10 rounded-lg overflow-hidden bg-gray-100 shrink-0">
            {p.imageUrl ? (
              <img src={p.imageUrl} alt="" className="w-full h-full object-contain" />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-gray-300 text-lg">📦</div>
            )}
          </div>

          {/* Name + brand */}
          <div className="min-w-0">
            <p className="text-sm font-medium text-gray-900 truncate group-hover:text-orange-600 transition-colors">
              {p.name}
            </p>
            {p.brand && <p className="text-xs text-gray-400 truncate">{p.brand}</p>}
          </div>

          {/* Category */}
          <div>
            {p.category && (
              <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full truncate block w-fit max-w-full">
                {categoryLabel(p.category)}
              </span>
            )}
          </div>

          {/* Popularity */}
          <div className="text-right">
            <ScoreBadge value={p.popularityScore} type="popularity" />
          </div>

          {/* Velocity */}
          <div className="text-right">
            <ScoreBadge value={p.velocityScore} type="velocity" />
          </div>
        </Link>
      ))}
    </div>
  )
}
