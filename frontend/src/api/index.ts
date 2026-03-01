import type { Page, Product, SourcePost, TrendingProduct } from '../types'

async function get<T>(path: string): Promise<T> {
  const res = await fetch(path)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json() as Promise<T>
}

// ── Leaderboard ────────────────────────────────────────────────────────────

export const fetchTrending = (limit = 20, category?: string) => {
  const url = category
    ? `/api/trending/category/${encodeURIComponent(category)}?limit=${limit}`
    : `/api/trending?limit=${limit}`
  return get<TrendingProduct[]>(url)
}

export const fetchPopular = (limit = 20, category?: string) => {
  const url = category
    ? `/api/popular/category/${encodeURIComponent(category)}?limit=${limit}`
    : `/api/popular?limit=${limit}`
  return get<TrendingProduct[]>(url)
}

// ── Products ───────────────────────────────────────────────────────────────

export const fetchProducts = (page = 0, size = 20) =>
  get<Page<Product>>(`/api/products?page=${page}&size=${size}`)

export const fetchProduct = (id: string) =>
  get<Product>(`/api/products/${id}`)

export const fetchProductPosts = (id: string) =>
  get<SourcePost[]>(`/api/products/${id}/posts`)

export const searchProducts = (q: string) =>
  get<Product[]>(`/api/products/search?q=${encodeURIComponent(q)}`)
