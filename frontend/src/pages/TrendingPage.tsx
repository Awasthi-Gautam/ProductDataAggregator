import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchTrending } from '../api'
import Leaderboard from '../components/Leaderboard'
import CategoryTabs from '../components/CategoryTabs'
import { RefreshCw } from 'lucide-react'

export default function TrendingPage() {
  const [category, setCategory] = useState('')

  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ['trending', category],
    queryFn: () => fetchTrending(50, category || undefined),
    refetchInterval: 60_000,
  })

  return (
    <div className="max-w-7xl mx-auto px-4 py-6 space-y-4">
      {/* Page header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">🔥 Trending Now</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Products rising fastest this cycle — sorted by velocity (rate of change)
          </p>
        </div>
        <button
          onClick={() => refetch()}
          disabled={isFetching}
          className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 transition-colors disabled:opacity-40"
        >
          <RefreshCw size={14} className={isFetching ? 'animate-spin' : ''} />
          Refresh
        </button>
      </div>

      {/* Category filter */}
      <CategoryTabs selected={category} onChange={setCategory} />

      {/* Leaderboard */}
      {isLoading ? (
        <LoadingSkeleton />
      ) : isError ? (
        <ErrorCard />
      ) : (
        <Leaderboard products={data ?? []} />
      )}
    </div>
  )
}

function LoadingSkeleton() {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
      {Array.from({ length: 10 }).map((_, i) => (
        <div key={i} className="flex items-center gap-3 px-4 py-3 border-b border-gray-50 last:border-0 animate-pulse">
          <div className="w-8 h-4 bg-gray-200 rounded" />
          <div className="w-10 h-10 bg-gray-200 rounded-lg" />
          <div className="flex-1 space-y-2">
            <div className="h-3.5 bg-gray-200 rounded w-3/4" />
            <div className="h-2.5 bg-gray-100 rounded w-1/3" />
          </div>
          <div className="w-16 h-4 bg-gray-200 rounded" />
          <div className="w-14 h-4 bg-gray-200 rounded" />
        </div>
      ))}
    </div>
  )
}

function ErrorCard() {
  return (
    <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center text-red-700">
      <p className="font-medium">Failed to load data</p>
      <p className="text-sm mt-1">Make sure the Spring Boot app is running on port 8080.</p>
    </div>
  )
}
