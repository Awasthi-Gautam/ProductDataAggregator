import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import Navbar from './components/Navbar'
import TrendingPage from './pages/TrendingPage'
import PopularPage from './pages/PopularPage'
import ProductPage from './pages/ProductPage'
import SearchPage from './pages/SearchPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: 1 },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <div className="min-h-screen bg-gray-50">
          <Navbar />
          <main className="pb-10">
            <Routes>
              <Route path="/" element={<Navigate to="/trending" replace />} />
              <Route path="/trending" element={<TrendingPage />} />
              <Route path="/popular" element={<PopularPage />} />
              <Route path="/products/:id" element={<ProductPage />} />
              <Route path="/search" element={<SearchPage />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
