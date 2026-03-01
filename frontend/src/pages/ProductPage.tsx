import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchProduct, fetchProductPosts } from '../api'
import { PLATFORM_COLORS, POST_TYPE_COLORS } from '../types'
import { ArrowLeft, ExternalLink, Star } from 'lucide-react'
import { amazonUrl } from '../lib/affiliate'

function timeAgo(iso: string) {
  const diff = Date.now() - new Date(iso).getTime()
  const h = Math.floor(diff / 3_600_000)
  if (h < 1) return 'just now'
  if (h < 24) return `${h}h ago`
  return `${Math.floor(h / 24)}d ago`
}

function categoryLabel(cat: string | null) {
  if (!cat) return null
  return cat.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

function platformLabel(p: string) {
  const map: Record<string, string> = {
    AMAZON_IN: 'Amazon.in',
    YOUTUBE: 'YouTube',
    REDDIT: 'Reddit',
    GOOGLE_CSE: 'Web Search',
    GOOGLE_TRENDS: 'Google Trends',
    QUORA: 'Quora',
  }
  return map[p] ?? p
}

export default function ProductPage() {
  const { id } = useParams<{ id: string }>()

  const { data: product, isLoading: loadingProduct } = useQuery({
    queryKey: ['product', id],
    queryFn: () => fetchProduct(id!),
    enabled: !!id,
  })

  const { data: posts, isLoading: loadingPosts } = useQuery({
    queryKey: ['posts', id],
    queryFn: () => fetchProductPosts(id!),
    enabled: !!id,
  })

  if (loadingProduct) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8 animate-pulse space-y-4">
        <div className="h-6 w-32 bg-gray-200 rounded" />
        <div className="h-8 w-2/3 bg-gray-200 rounded" />
        <div className="h-4 w-1/4 bg-gray-100 rounded" />
      </div>
    )
  }

  if (!product) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8 text-center text-gray-500">
        Product not found.
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-6 space-y-6">
      {/* Back */}
      <Link to="/trending" className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 transition-colors">
        <ArrowLeft size={14} /> Back to leaderboard
      </Link>

      {/* Product header */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex gap-5">
        {product.imageUrl && (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="w-28 h-28 object-contain rounded-lg bg-gray-50 shrink-0"
          />
        )}
        <div className="min-w-0 flex-1">
          <h1 className="text-xl font-bold text-gray-900 leading-snug">{product.name}</h1>
          <div className="flex flex-wrap items-center gap-2 mt-2">
            {product.brand && (
              <span className="text-sm text-gray-500">{product.brand}</span>
            )}
            {product.category && (
              <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
                {categoryLabel(product.category)}
              </span>
            )}
            {product.trendScore != null && (
              <span className="text-xs bg-orange-100 text-orange-700 px-2 py-0.5 rounded-full font-medium">
                Score: {product.trendScore.toFixed(1)}
              </span>
            )}
          </div>
          <div className="flex gap-3 mt-3">
            {product.externalIds?.['AMAZON_IN'] && (
              <a
                href={amazonUrl(product.externalIds['AMAZON_IN'])}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 text-sm bg-orange-500 text-white px-3 py-1.5 rounded-lg hover:bg-orange-600 transition-colors font-medium"
              >
                View on Amazon <ExternalLink size={13} />
              </a>
            )}
          </div>
          <p className="text-xs text-gray-400 mt-3">
            First seen {timeAgo(product.firstSeenAt)} · Updated {timeAgo(product.updatedAt)}
          </p>
        </div>
      </div>

      {/* Source posts */}
      <div>
        <h2 className="text-base font-semibold text-gray-800 mb-3">
          Sources <span className="text-gray-400 font-normal">({posts?.length ?? 0})</span>
        </h2>

        {loadingPosts ? (
          <div className="space-y-3">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="bg-white rounded-xl border border-gray-100 p-4 animate-pulse space-y-2">
                <div className="h-4 bg-gray-200 rounded w-3/4" />
                <div className="h-3 bg-gray-100 rounded w-full" />
                <div className="h-3 bg-gray-100 rounded w-2/3" />
              </div>
            ))}
          </div>
        ) : !posts || posts.length === 0 ? (
          <p className="text-sm text-gray-400 py-6 text-center">No source posts collected yet.</p>
        ) : (
          <div className="space-y-3">
            {posts.map(post => (
              <div key={post.id} className="bg-white rounded-xl border border-gray-100 p-4 space-y-2">
                {/* Badges row */}
                <div className="flex items-center gap-2 flex-wrap">
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${PLATFORM_COLORS[post.platform] ?? 'bg-gray-100 text-gray-600'}`}>
                    {platformLabel(post.platform)}
                  </span>
                  <span className={`text-xs px-2 py-0.5 rounded-full ${POST_TYPE_COLORS[post.type] ?? 'bg-gray-100 text-gray-600'}`}>
                    {post.type}
                  </span>
                  {post.rating != null && (
                    <span className="flex items-center gap-0.5 text-xs text-yellow-600">
                      <Star size={11} fill="currentColor" />
                      {post.rating.toFixed(1)}
                    </span>
                  )}
                  <span className="text-xs text-gray-400 ml-auto">{timeAgo(post.collectedAt)}</span>
                </div>

                {/* Title */}
                <p className="text-sm font-medium text-gray-900 leading-snug">{post.title}</p>

                {/* Content */}
                {post.content && (
                  <p className="text-xs text-gray-500 leading-relaxed line-clamp-3">{post.content}</p>
                )}

                {/* Link */}
                {post.url && (
                  <a
                    href={post.platform === 'AMAZON_IN' ? amazonUrl(post.url) : post.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1 text-xs text-blue-600 hover:underline"
                  >
                    View source <ExternalLink size={11} />
                  </a>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
