import { useSearchParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { searchProducts } from '../api'
import { ExternalLink } from 'lucide-react'

function categoryLabel(cat: string | null) {
  if (!cat) return null
  return cat.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

export default function SearchPage() {
  const [searchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''

  const { data, isLoading, isError } = useQuery({
    queryKey: ['search', q],
    queryFn: () => searchProducts(q),
    enabled: q.length > 0,
  })

  return (
    <div className="max-w-4xl mx-auto px-4 py-6 space-y-4">
      <div>
        <h1 className="text-xl font-bold text-gray-900">
          Search results for <span className="text-orange-500">"{q}"</span>
        </h1>
        {data && <p className="text-sm text-gray-400 mt-0.5">{data.length} products found</p>}
      </div>

      {isLoading && (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="bg-white rounded-xl border border-gray-100 p-4 animate-pulse flex gap-3">
              <div className="w-12 h-12 bg-gray-200 rounded-lg shrink-0" />
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-gray-200 rounded w-3/4" />
                <div className="h-3 bg-gray-100 rounded w-1/3" />
              </div>
            </div>
          ))}
        </div>
      )}

      {isError && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700">
          Search failed. Make sure the backend is running.
        </div>
      )}

      {data && data.length === 0 && (
        <div className="text-center py-16 text-gray-400">
          No products match "<strong>{q}</strong>". Try a shorter keyword.
        </div>
      )}

      {data && data.length > 0 && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          {data.map(product => (
            <Link
              key={product.id}
              to={`/products/${product.id}`}
              className="flex items-center gap-3 px-4 py-3 border-b border-gray-50 last:border-0 hover:bg-orange-50 transition-colors group"
            >
              <div className="w-12 h-12 rounded-lg overflow-hidden bg-gray-100 shrink-0">
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt="" className="w-full h-full object-contain" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-gray-300 text-xl">📦</div>
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate group-hover:text-orange-600 transition-colors">
                  {product.name}
                </p>
                <div className="flex items-center gap-2 mt-0.5">
                  {product.brand && <span className="text-xs text-gray-400">{product.brand}</span>}
                  {product.category && (
                    <span className="text-xs bg-gray-100 text-gray-500 px-1.5 py-0.5 rounded-full">
                      {categoryLabel(product.category)}
                    </span>
                  )}
                </div>
              </div>
              <ExternalLink size={14} className="text-gray-300 group-hover:text-orange-400 transition-colors shrink-0" />
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
