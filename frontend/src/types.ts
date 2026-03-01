export interface TrendingProduct {
  rank: number
  productId: string
  name: string
  brand: string | null
  category: string | null
  imageUrl: string | null
  popularityScore: number
  velocityScore: number | null
}

export interface Product {
  id: string
  name: string
  brand: string | null
  category: string | null
  imageUrl: string | null
  externalIds: Record<string, string>
  trendScore: number | null
  firstSeenAt: string
  updatedAt: string
}

export interface SourcePost {
  id: string
  platform: string
  type: string
  title: string
  content: string | null
  url: string | null
  authorName: string | null
  rating: number | null
  regionState: string | null
  regionCity: string | null
  collectedAt: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const CATEGORIES: { value: string; label: string }[] = [
  { value: '', label: 'All' },
  { value: 'ELECTRONICS_GADGETS', label: 'Electronics' },
  { value: 'FASHION_APPAREL', label: 'Fashion' },
  { value: 'HOME_KITCHEN', label: 'Home & Kitchen' },
  { value: 'BEAUTY_PERSONAL_CARE', label: 'Beauty' },
  { value: 'SPORTS_FITNESS', label: 'Sports' },
  { value: 'HEALTH_WELLNESS', label: 'Health' },
  { value: 'BOOKS_MEDIA', label: 'Books' },
  { value: 'TOYS_GAMES', label: 'Toys' },
  { value: 'FOOD_GROCERY', label: 'Grocery' },
  { value: 'AUTOMOTIVE', label: 'Automotive' },
  { value: 'BABY_KIDS', label: 'Baby & Kids' },
]

export const PLATFORM_COLORS: Record<string, string> = {
  AMAZON_IN: 'bg-orange-100 text-orange-800',
  YOUTUBE: 'bg-red-100 text-red-800',
  REDDIT: 'bg-orange-100 text-orange-700',
  GOOGLE_CSE: 'bg-blue-100 text-blue-800',
  GOOGLE_TRENDS: 'bg-green-100 text-green-800',
  QUORA: 'bg-red-100 text-red-900',
}

export const POST_TYPE_COLORS: Record<string, string> = {
  REVIEW: 'bg-green-100 text-green-800',
  COMPLAINT: 'bg-red-100 text-red-800',
  QA: 'bg-purple-100 text-purple-800',
  DISCUSSION: 'bg-gray-100 text-gray-700',
  BESTSELLER_RANK: 'bg-yellow-100 text-yellow-800',
  MENTION: 'bg-sky-100 text-sky-800',
}
