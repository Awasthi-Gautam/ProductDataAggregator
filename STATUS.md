# TrendIN — Project Status

Last updated: 2026-03-01

---

## What This Project Is

India-focused real-time product trend aggregation platform.
Collects buying signals from e-commerce + social platforms, scores products by velocity (rate of change) and popularity (cumulative), and surfaces them in a live dashboard.

---

## Current State: MVP is live and working locally

### Running right now
```bash
cd /Users/gautamawasthi/Documents/data-aggregation
docker compose up -d           # start all services
open http://localhost:5173     # frontend (dev server — run separately)
```

To start the frontend dev server:
```bash
cd frontend
/opt/homebrew/bin/npm run dev
```

To trigger a collection manually (don't wait for hourly cron):
```bash
curl -X POST http://localhost:8080/api/admin/collect/all
```

---

## What's Built

### Backend (Spring Boot 4 / Java 25 / Gradle)

**Discovery collectors** — find new products:
| Collector | Status | Notes |
|---|---|---|
| AmazonInCollector | ✅ Working | Scrapes bestseller pages across 15 categories |
| GoogleTrendsCollector | ✅ Working | RSS feed, no auth needed |
| FlipkartCollector | ⏳ Needs credentials | Affiliate API implemented, awaiting account |
| Meesho/Myntra/Nykaa/Ajio/BigBasket/Blinkit/SwiggyInstamart | ❌ Skeleton | React SPAs — need Playwright or affiliate APIs |

**Enrichment collectors** — add context to discovered products:
| Collector | Status | Notes |
|---|---|---|
| YouTubeCollector | ✅ Working | API key configured |
| GoogleDiscussionCollector | ✅ Working | CSE key configured, searches Reddit/Quora/forums |
| RedditCollector | ⚠️ Public fallback | OAuth blocked by Reddit policy; public API works locally, rate-limited on AWS |
| QuoraCollector | ⚠️ Best-effort | Works locally; 403 on AWS/cloud IPs |
| Twitter/Instagram/TikTok | ❌ Skeleton | Need paid API (Twitter $100/mo) or approval |

**Pipeline schedule** (runs automatically every hour):
- `:00` — Discovery (find products)
- `:10` — Enrichment (add YouTube/Reddit/forum posts)
- `:20` — Scoring (velocity + popularity, update Redis leaderboards)

**Scoring system:**
- `popularityScore` = cumulative price-weighted score (Redis ZSet `trends:IN`)
- `velocityScore` = score delta vs previous cycle (Redis ZSet `trends:IN:velocity`)
- Price multiplier: `1 + log10(price/100 + 1)` — higher-priced products score more

**REST API endpoints:**
```
GET  /api/trending                    velocity leaderboard (top 20)
GET  /api/trending/category/{cat}     filtered by category
GET  /api/popular                     popularity leaderboard
GET  /api/popular/category/{cat}
GET  /api/popular/state/{state}
GET  /api/popular/city/{city}
GET  /api/products?page=0&size=20     paginated product list
GET  /api/products/search?q=          name search
GET  /api/products/{id}               product detail
GET  /api/products/{id}/posts         source posts for a product
POST /api/admin/collect/all           trigger full pipeline
POST /api/admin/collect/discovery
POST /api/admin/collect/enrichment
POST /api/admin/collect/scoring
GET  /actuator/health                 all service health
```

### Frontend (React + Vite + Tailwind + TanStack Query)

**Pages:**
- `/trending` — velocity leaderboard with category tabs, auto-refreshes every 60s
- `/popular` — popularity leaderboard with category tabs
- `/products/:id` — product detail with source posts, Amazon link
- `/search?q=` — search results

**Affiliate link support:**
- Amazon URLs automatically get `?tag={VITE_AMAZON_AFFILIATE_TAG}` appended
- Set `VITE_AMAZON_AFFILIATE_TAG` in `frontend/.env.local` to activate

### Infrastructure (Docker Compose)
- PostgreSQL 16 — products, source posts, trend snapshots
- Elasticsearch 9 — full-text search (index exists, SearchController not yet built)
- Redis 7 — leaderboard ZSets
- Spring Boot app — port 8080
- Nginx + React — port 3000 (production build via `docker compose up --build frontend`)

---

## API Keys Configured (in .env)

| Key | Status |
|---|---|
| `YOUTUBE_API_KEY` | ✅ Set |
| `GOOGLE_CSE_API_KEY` | ✅ Set |
| `GOOGLE_CSE_CX` | ✅ Set |
| `FLIPKART_AFFILIATE_ID` | ❌ Empty — register at affiliate.flipkart.com |
| `FLIPKART_AFFILIATE_TOKEN` | ❌ Empty — register at affiliate.flipkart.com |
| `AMAZON_AFFILIATE_TAG` | ❌ Empty — register at affiliate-program.amazon.in |
| `REDDIT_CLIENT_ID` | ❌ Empty — blocked by Reddit policy for now |

---

## Next Steps (in priority order)

### Immediate (to unlock more data)
1. **Flipkart affiliate account** — affiliate.flipkart.com (free, ~10 min)
   - After sign-up, get ID + Token from "API Access" tab
   - Add to `.env`: `FLIPKART_AFFILIATE_ID=` and `FLIPKART_AFFILIATE_TOKEN=`
   - Restart app: `docker compose up --build -d app`

2. **Amazon Associates account** — affiliate-program.amazon.in (free, ~15 min)
   - Get your tracking ID (e.g. `yourname-21`)
   - Add to `.env`: `AMAZON_AFFILIATE_TAG=yourname-21`
   - Add to `frontend/.env.local`: `VITE_AMAZON_AFFILIATE_TAG=yourname-21`
   - Every Amazon click on the dashboard earns 1–10% commission

### Short-term (features)
3. **SearchController** — Elasticsearch full-text search endpoint (infrastructure ready, controller not built)
4. **Meesho/Myntra/Nykaa** — Add Playwright (headless browser) to Docker stack for JS-rendered sites
5. **Brand field** — Amazon scraper doesn't extract brand; add brand parsing to AmazonInCollector
6. **WebSocket** — Push live leaderboard updates to frontend without polling

### Monetization (when ready)
7. **B2B API** — Gate `/api/trending` behind auth + billing (Spring Security + Stripe)
8. **Seller dashboard** — Per-product trend history, competitor alerts
9. **Newsletter** — Weekly "Top 10 Trending in India" with affiliate links

---

## How to Resume Development

```bash
# 1. Start infrastructure
cd /Users/gautamawasthi/Documents/data-aggregation
docker compose up -d

# 2. Check all services are healthy
curl http://localhost:8080/actuator/health

# 3. Start frontend dev server
cd frontend && /opt/homebrew/bin/npm run dev

# 4. Trigger a fresh data collection
curl -X POST http://localhost:8080/api/admin/collect/all

# 5. Open dashboard
open http://localhost:5173
```

### Rebuilding after backend changes
```bash
./gradlew build -x test && docker compose up --build -d app
```

### Rebuilding after frontend changes
```bash
# Dev: just save the file — Vite hot-reloads automatically
# Production: docker compose up --build -d frontend
```

---

## Key Files Reference

```
src/main/java/com/aggregation/data_aggregation/
├── collector/
│   ├── AbstractCollector.java          base class (fetchHtml, fetchJson, sleep)
│   ├── CollectionOrchestrator.java     pipeline scheduler + scoring logic
│   ├── CollectionResult.java           result container
│   ├── discovery/
│   │   ├── AmazonInCollector.java      ← main data source
│   │   ├── GoogleTrendsCollector.java  ← RSS-based, working
│   │   └── FlipkartCollector.java      ← needs affiliate credentials
│   └── enrichment/
│       ├── YouTubeCollector.java
│       ├── GoogleDiscussionCollector.java
│       └── RedditCollector.java
├── controller/
│   ├── TrendController.java            /api/trending, /api/popular
│   ├── ProductController.java          /api/products
│   └── CollectionController.java       /api/admin/collect/*
├── model/entity/
│   ├── Product.java
│   ├── SourcePost.java
│   └── TrendSnapshot.java              score history for velocity calc
├── repository/
│   └── TrendLeaderboardRepository.java Redis ZSet operations
└── service/
    └── RedditAuthService.java          OAuth token cache

frontend/src/
├── api/index.ts        typed API calls
├── types.ts            TypeScript types
├── lib/affiliate.ts    Amazon affiliate URL helper
├── components/
│   ├── Navbar.tsx
│   ├── Leaderboard.tsx
│   ├── CategoryTabs.tsx
│   └── ScoreBadge.tsx
└── pages/
    ├── TrendingPage.tsx
    ├── PopularPage.tsx
    ├── ProductPage.tsx
    └── SearchPage.tsx
```
