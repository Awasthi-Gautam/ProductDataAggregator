import { Link, NavLink, useNavigate } from 'react-router-dom'
import { Search, TrendingUp } from 'lucide-react'
import { useState } from 'react'

export default function Navbar() {
  const [q, setQ] = useState('')
  const navigate = useNavigate()

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    if (q.trim()) navigate(`/search?q=${encodeURIComponent(q.trim())}`)
  }

  return (
    <header className="bg-gray-900 text-white shadow-lg sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 h-14 flex items-center gap-6">
        {/* Logo */}
        <Link to="/trending" className="flex items-center gap-2 font-bold text-lg shrink-0">
          <TrendingUp className="text-orange-400" size={22} />
          <span>TrendIN</span>
        </Link>

        {/* Nav links */}
        <nav className="flex gap-1">
          <NavLink
            to="/trending"
            className={({ isActive }) =>
              `px-3 py-1.5 rounded text-sm font-medium transition-colors ${
                isActive ? 'bg-orange-500 text-white' : 'text-gray-300 hover:text-white hover:bg-gray-700'
              }`
            }
          >
            🔥 Trending
          </NavLink>
          <NavLink
            to="/popular"
            className={({ isActive }) =>
              `px-3 py-1.5 rounded text-sm font-medium transition-colors ${
                isActive ? 'bg-orange-500 text-white' : 'text-gray-300 hover:text-white hover:bg-gray-700'
              }`
            }
          >
            ⭐ Popular
          </NavLink>
        </nav>

        {/* Search */}
        <form onSubmit={handleSearch} className="ml-auto flex items-center bg-gray-800 rounded-lg overflow-hidden">
          <input
            value={q}
            onChange={e => setQ(e.target.value)}
            placeholder="Search products…"
            className="bg-transparent text-sm text-white placeholder-gray-400 px-3 py-1.5 w-56 outline-none"
          />
          <button type="submit" className="px-3 py-1.5 text-gray-400 hover:text-white transition-colors">
            <Search size={16} />
          </button>
        </form>
      </div>
    </header>
  )
}
