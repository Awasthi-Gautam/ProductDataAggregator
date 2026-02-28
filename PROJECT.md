# Data Aggregation — Project Reference

## Goal

Build a real-time product trend aggregation platform **focused on India** that:
- Collects buying activity and product data from Indian and global sources that ship to India
- Identifies high-selling / trending products in real time across categories and Indian regions
- Surfaces user problems, complaints, reviews, and discussions about any product
- Presents everything in a unified dashboard with search, category filters, and region filters
- All infrastructure runs inside Docker (local dev and production)

---

## User Stories

1. **As a user**, I want to see a live feed of trending/high-selling products across all platforms so I know what is being bought right now.
2. **As a user**, I want to search for any product and see aggregated reviews, Q&As, Reddit discussions, tweets, and complaints from all platforms in one place.
3. **As a user**, I want to understand the top problems users are reporting about any product category or specific product.

---

## Data Sources

All sources are filtered to India (`geo=IN`) or are India-native platforms.
Only products available / shippable to India are tracked.

### Indian E-Commerce (Primary — Direct Sales Data)

| Source | Data Collected | Access Method | Notes |
|---|---|---|---|
| Amazon India (amazon.in) | Bestsellers by category, reviews, Q&A, deals | Scraping (Jsoup) | Largest e-commerce in India; category-level bestseller pages |
| Flipkart | Bestsellers, reviews, trending products | Scraping (Jsoup) | #2 Indian e-commerce; has daily/weekly trending |
| Myntra | Trending fashion, bestsellers, new arrivals | Scraping (Jsoup) | India's largest fashion platform |
| Meesho | Resale trending items, popular listings | Scraping (Jsoup) | Social commerce; tier-2/3 city buying trends |
| Nykaa | Trending beauty & wellness products, reviews | Scraping (Jsoup) | India's top beauty platform |
| Ajio | Fashion trends, bestsellers | Scraping (Jsoup) | Reliance-owned; fast fashion trends |
| Snapdeal | General e-commerce trends | Scraping (Jsoup) | Strong in budget/value segment |
| JioMart | Grocery & general trends | Scraping (Jsoup) | Reliance-owned; rapid grocery growth |
| BigBasket | Grocery bestsellers, trending FMCG | Scraping (Jsoup) | Tata-owned; largest online grocery |
| Tata Cliq | Premium product trends | Scraping (Jsoup) | Tata group; premium segment |
| Blinkit | Quick-commerce trending items | Scraping (Jsoup) | 10-min delivery; impulse buy trends |
| Swiggy Instamart | Grocery quick-commerce trends | Scraping (Jsoup) | Real-time demand signal |

### Social & Community (India-filtered)

| Source | Data Collected | Access Method | Notes |
|---|---|---|---|
| Reddit | r/india, r/IndiaShops, r/indianbusiness, r/mildlyinfuriating | Reddit API (free) | India-specific subreddits; complaints & recommendations |
| Twitter/X | India product mentions (geocode filter) | X API Basic ($100/mo) | Filter by `geocode=20.5937,78.9629,2000km` (India) |
| Instagram | Indian influencer posts, Shopping tags, `#MadeInIndia` | Instagram Graph API | Strong influencer-driven buying signal in India |
| TikTok / Moj | Product trends; `#TikTokMadeMeBuyIt` India | TikTok Research API | Also scrape Moj (India's TikTok alternative) |
| YouTube India | Hindi/English product reviews, unboxing, complaints | YouTube Data API v3 (free) | Filter by `regionCode=IN` |
| Pinterest | Indian shopping boards and pins | Pinterest API (free) | Filter by India region |
| Quora India | Product Q&As, `Which is the best...` questions | Scraping (Jsoup) | High-intent buying research signals |
| LinkedIn | B2B tech product discussions, India professionals | LinkedIn API (restricted) | Limited; B2B/enterprise products only |

### Search & Discovery (India-filtered)

| Source | Data Collected | Access Method | Notes |
|---|---|---|---|
| Google Trends India | Real-time trending searches (`geo=IN`) | Unofficial HTTP / SerpAPI | Best real-time signal; drill down to state level |
| Product Hunt | New products shipping to India | Product Hunt API (GraphQL) | Filter for India-available products |
| eBay India (ebay.in) | Trending / completed listings | eBay Browse API (free) | Smaller market but unique product set |

---

## Product Categories

All collected products will be tagged to one of these categories (auto-classified via NLP on product name/description):

| Category | Examples |
|---|---|
| Electronics & Gadgets | Phones, laptops, earbuds, smartwatches, cameras |
| Fashion & Apparel | Clothing, footwear, accessories, jewellery |
| Home & Kitchen | Appliances, furniture, cookware, décor |
| Beauty & Personal Care | Skincare, haircare, grooming, makeup |
| Sports & Fitness | Gym equipment, activewear, outdoor gear |
| Health & Wellness | Supplements, medical devices, yoga, mental health |
| Books & Media | Books, courses, streaming, games |
| Toys & Games | Kids toys, board games, video games, collectibles |
| Food & Grocery | Packaged food, beverages, organic, snacks |
| Automotive | Car accessories, tools, EV gear |
| Baby & Kids | Baby gear, clothing, education |
| Pet Supplies | Food, accessories, grooming |
| Travel & Luggage | Bags, travel accessories, booking trends |
| Office & Stationery | Work-from-home gear, stationery, office tech |

### Category Tagging Strategy
- **Primary**: Match product name/title against a keyword dictionary per category
- **Secondary**: NLP classifier (Apache OpenNLP) trained on Amazon category taxonomy
- **Fallback**: `Uncategorized` — reviewed and re-tagged in batches

---

## Region Support

**Primary focus: India.** All data is filtered to India by default.
Region data enables filtering trends by Indian state and city.

### Indian Region Sources

| Source | India Region Granularity | How |
|---|---|---|
| Google Trends | State + city level | `geo=IN-MH` (Maharashtra), `geo=IN-DL` (Delhi) etc. |
| Amazon India | Pin-code / city level in delivery data | Scrape city-specific bestsellers where exposed |
| Meesho | State + city (delivery coverage) | Listings show delivery regions |
| Twitter/X | City level | Geocode filter: `lat,lng,radius` per major city |
| Blinkit / Swiggy Instamart | City level (hyperlocal) | City-specific trending items |
| Reddit | State/city subreddits (r/mumbai, r/bangalore, r/delhi) | Subreddit-based |
| Instagram | Location tags on posts | City/neighbourhood level |
| YouTube | Viewer region via API | `regionCode=IN` |

### Supported Indian Regions

**States (Phase 1):** Maharashtra, Delhi, Karnataka, Tamil Nadu, Telangana, Gujarat, Rajasthan, West Bengal, Uttar Pradesh, Kerala

**Major Cities:** Mumbai, Delhi, Bengaluru, Hyderabad, Chennai, Pune, Kolkata, Ahmedabad, Jaipur, Surat, Lucknow, Kochi

### Region in Data Model

Each `SourcePost` and `TrendScore` will carry:
```
region.country = "IN"            # always IN for this project
region.state   = "Maharashtra"   # optional, ISO 3166-2:IN state
region.city    = "Mumbai"        # optional
```

Dashboard will support:
- National leaderboard (all India)
- Per-state leaderboard
- Per-city leaderboard
- State/city comparison view ("Realme phone trending more in UP or Karnataka?")

---

## Architecture

```
[Data Sources]
     │
     ▼
[Collectors Layer]  ──── Spring @Scheduled jobs, one per source
     │
     ▼ (raw events)
[Apache Kafka]  ──────── Decouples ingestion from processing
     │
     ▼ (consumed)
[Processing Layer]  ───── Normalize → Deduplicate → Score → NLP
     │
     ├──▶ [PostgreSQL]  ── Products catalog, trend scores, source metadata
     ├──▶ [Elasticsearch] ─ Full-text search: reviews, discussions, Q&As
     └──▶ [Redis]  ──────── Real-time trending leaderboard (sorted sets)
     │
     ▼
[API Layer]  ─────────── Spring Boot REST + WebSocket
     │
     ▼
[Dashboard]  ─────────── React frontend
                          - Live trending feed (WebSocket)
                          - Product search → aggregated reviews/discussions
                          - Charts: trend over time, platform breakdown
```

---

## Project Package Structure

```
com.aggregation.data_aggregation/
├── collector/           # One class per data source (ScheduledCollector interface)
│   ├── RedditCollector.java
│   ├── AmazonCollector.java
│   ├── EbayCollector.java
│   ├── GoogleTrendsCollector.java
│   ├── TwitterCollector.java
│   ├── YouTubeCollector.java
│   ├── WalmartCollector.java
│   ├── ProductHuntCollector.java
│   ├── InstagramCollector.java
│   ├── TikTokCollector.java
│   ├── PinterestCollector.java
│   ├── MeeshoCollector.java
│   ├── LinkedInCollector.java
│   └── QuoraCollector.java
├── kafka/
│   ├── producer/        # RawEventProducer.java
│   └── consumer/        # RawEventConsumer.java
├── processing/
│   ├── Normalizer.java          # Map source-specific data → common model
│   ├── Deduplicator.java        # Detect same product across sources
│   ├── TrendScorer.java         # Volume × recency × sentiment scoring
│   └── NlpExtractor.java        # Extract product mentions, complaints
├── repository/
│   ├── ProductRepository.java         # PostgreSQL via Spring Data JPA
│   ├── SourcePostRepository.java      # PostgreSQL
│   ├── ProductSearchRepository.java   # Elasticsearch
│   └── TrendLeaderboardRepository.java # Redis
├── api/
│   ├── TrendController.java     # GET /api/trends
│   ├── SearchController.java    # GET /api/search?q=
│   └── WebSocketHandler.java    # Push real-time updates
└── model/
    ├── Product.java             # id, name, category, sources, trendScore
    ├── TrendScore.java          # productId, score, timestamp, breakdown
    ├── SourcePost.java          # platform, type (review/qa/discussion), text, url
    └── RawEvent.java            # Kafka message envelope
```

---

## Key Dependencies (to add to build.gradle)

| Dependency | Purpose |
|---|---|
| spring-boot-starter-web | REST API |
| spring-boot-starter-websocket | Real-time dashboard push |
| spring-boot-starter-data-jpa | PostgreSQL ORM |
| spring-boot-starter-data-elasticsearch | Elasticsearch integration |
| spring-boot-starter-data-redis | Redis leaderboard |
| spring-kafka | Kafka producer/consumer |
| postgresql | JDBC driver |
| jsoup | HTML scraping for Walmart, BestBuy, etc. |
| apache-opennlp | NLP: product mention extraction |
| spring-boot-starter-scheduling | @Scheduled collectors |

---

## Build Order (Phases)

### Phase 1 — Foundation
- [ ] Add all dependencies to `build.gradle`
- [ ] Define core models: `Product`, `TrendScore`, `SourcePost`, `RawEvent`
- [ ] Set up PostgreSQL schema via JPA
- [ ] Configure Kafka topics
- [ ] Configure Elasticsearch index
- [ ] Configure Redis connection

### Phase 2 — Collectors (one per source)
- [ ] `RedditCollector` — Reddit API (free, no key needed for public data)
- [ ] `GoogleTrendsCollector` — Trending search terms
- [ ] `EbayCollector` — eBay Browse API
- [ ] `AmazonCollector` — Scraping bestsellers + reviews
- [ ] `WalmartCollector` — Scraping bestsellers
- [ ] `YouTubeCollector` — YouTube Data API
- [ ] `TwitterCollector` — X API
- [ ] `ProductHuntCollector` — GraphQL API
- [ ] `InstagramCollector` — Instagram Graph API (Shopping tags, hashtags, influencer posts)
- [ ] `TikTokCollector` — TikTok Research API (trending product videos, TikTok Shop)
- [ ] `PinterestCollector` — Pinterest API (trending shopping boards and pins)
- [ ] `MeeshoCollector` — Scraping (trending resale listings, Indian market)
- [ ] `LinkedInCollector` — LinkedIn API, restricted; B2B product discussions only
- [ ] `QuoraCollector` — Scraping (product Q&As, recommendations, complaints)

### Phase 3 — Processing Pipeline
- [ ] `Normalizer` — Map all source data to common models
- [ ] `Deduplicator` — Match same product across sources
- [ ] `TrendScorer` — Compute and update trend scores
- [ ] `NlpExtractor` — Extract problems/complaints from text

### Phase 4 — API Layer
- [ ] `TrendController` — List trending products, filter by category/time
- [ ] `SearchController` — Search product, return aggregated posts
- [ ] `WebSocketHandler` — Stream live updates to dashboard

### Phase 5 — Dashboard
- [ ] React app scaffold
- [ ] Live trending feed (WebSocket)
- [ ] Product search page
- [ ] Trend charts

---

## Architecture Decisions

| Decision | Choice | Reason |
|---|---|---|
| Language/Framework | Java 25 + Spring Boot 4 | Project was initialized with this stack |
| Message queue | Apache Kafka | Handles high-throughput ingestion; decouples collectors from processing |
| Primary DB | PostgreSQL | Structured product/trend data with complex queries |
| Search/reviews DB | Elasticsearch | Full-text search across heterogeneous text from multiple platforms |
| Real-time leaderboard | Redis sorted sets | O(log n) ranked reads; TTL-based score decay for recency |
| Frontend | React (separate) | Needs WebSocket + charting; better than Thymeleaf for interactive UI |
| Scraping library | Jsoup | Lightweight, Java-native HTML parser |
| NLP | Apache OpenNLP | Java-native; sufficient for entity/complaint extraction |
| Collector scheduling | Spring @Scheduled | Simple, no extra infra needed; can migrate to Quartz if needed |

---

## API Design (planned)

```
GET  /api/trends                        # Top trending products right now
GET  /api/trends?category=electronics  # Filtered by category
GET  /api/trends?period=24h            # Filtered by time window
GET  /api/search?q=iphone+15           # Search product → all posts/reviews
GET  /api/product/{id}                 # Product detail + trend breakdown
GET  /api/product/{id}/problems        # Top reported issues for a product
WS   /ws/trends                        # WebSocket: live trending updates
```

---

## Environment Variables (required at runtime)

```
# PostgreSQL
POSTGRES_URL=jdbc:postgresql://localhost:5432/data_aggregation
POSTGRES_USER=
POSTGRES_PASSWORD=

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Elasticsearch
ELASTICSEARCH_URI=http://localhost:9200

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# External APIs
REDDIT_CLIENT_ID=
REDDIT_CLIENT_SECRET=
EBAY_APP_ID=
YOUTUBE_API_KEY=
TWITTER_BEARER_TOKEN=
PRODUCTHUNT_TOKEN=
INSTAGRAM_ACCESS_TOKEN=
TIKTOK_CLIENT_KEY=
TIKTOK_CLIENT_SECRET=
PINTEREST_ACCESS_TOKEN=
LINKEDIN_CLIENT_ID=
LINKEDIN_CLIENT_SECRET=
```

---

## API Cost Analysis

### Free Tier / No Cost
| Source | Free Allowance | Constraint |
|---|---|---|
| Reddit API | Unlimited public read | Rate limit: 60 req/min |
| eBay Browse API | 5,000 calls/day | Per app ID |
| YouTube Data API v3 | 10,000 units/day | Search = 100 units; list = 1 unit |
| Google Trends (unofficial HTTP) | Unlimited | Rate-limited; delay needed between calls |
| Product Hunt API | Unlimited (GraphQL) | Rate limit per token |
| Amazon PA API | Free | Must have active affiliate account |
| Pinterest API | Free | 1,000 req/hour |
| TikTok Research API | Free (approved researchers) | Requires academic/researcher application |

### Paid APIs
| Source | Plan | Cost | What You Get |
|---|---|---|---|
| Twitter/X API | Basic | $100/month | 10,000 tweets read/month |
| Twitter/X API | Pro | $5,000/month | 1M tweets/month — skip unless high scale |
| SerpAPI (Google Trends enhanced) | Hobby | $50/month | 5,000 searches/month |
| Instagram Graph API | Free | $0 | Rate limits apply; needs FB Developer approval |
| LinkedIn API | Free (basic) | $0 | Very restricted; partner program needed for more |

### Total Estimated API Cost
| Scenario | Monthly API Cost |
|---|---|
| MVP (free APIs only, no Twitter) | $0 |
| Standard (Twitter Basic + SerpAPI) | ~$150/month |
| Full scale (Twitter Pro + SerpAPI Pro) | ~$5,200/month |

**Recommendation**: Start with $0 (free APIs only). Add Twitter Basic ($100) once the platform has users and proven value.

---

## Deployment Options

### Option A — AWS (Fully Managed, Production-Grade)

**Best for**: Reliability, auto-scaling, compliance

| Service | AWS Equivalent | Est. Cost/month |
|---|---|---|
| Spring Boot app | ECS Fargate (1 vCPU, 2GB) | ~$30 |
| PostgreSQL | RDS db.t3.medium (Multi-AZ) | ~$100 |
| Elasticsearch | OpenSearch t3.medium.search | ~$70 |
| Redis | ElastiCache cache.t3.micro | ~$15 |
| Kafka | Amazon MSK (2x kafka.t3.small) | ~$150 |
| Load Balancer | ALB | ~$20 |
| Storage (S3 for backups) | S3 Standard | ~$5 |
| **Total** | | **~$390/month** |

Pros: Fully managed, auto-scaling, high availability, strong ecosystem
Cons: Most expensive option; MSK (Kafka) is the biggest cost driver

---

### Option B — GCP (Cost-Effective, Serverless-Friendly)

**Best for**: Cost savings, pay-per-use model, less traffic initially

| Service | GCP Equivalent | Est. Cost/month |
|---|---|---|
| Spring Boot app | Cloud Run (serverless containers) | ~$10–20 (scales to zero) |
| PostgreSQL | Cloud SQL db-n1-standard-1 | ~$50 |
| Elasticsearch | Elastic Cloud (GCP-hosted, 4GB) | ~$70 |
| Redis | Memorystore for Redis (1GB) | ~$35 |
| Kafka | Confluent Cloud (Basic, 1 cluster) | ~$0–60 (free tier available) |
| Load Balancer | Cloud Load Balancing | ~$10 |
| **Total** | | **~$175–245/month** |

Pros: Cloud Run scales to zero — very cheap at low traffic; Pub/Sub can replace Kafka cheaper
Cons: Managed Kafka (Confluent) costs more if volume is high

---

### Option C — Self-Hosted VPS (Cheapest, Dev/Staging)

**Best for**: Development, staging, or bootstrapped MVP

| Provider | Spec | Cost/month |
|---|---|---|
| Hetzner CX31 | 2 vCPU, 8GB RAM, 80GB SSD | ~$10 |
| Hetzner CPX41 (recommended) | 4 vCPU, 16GB RAM, 240GB SSD | ~$28 |
| DigitalOcean General (4GB) | 2 vCPU, 4GB RAM | ~$24 |
| DigitalOcean General (8GB) | 2 vCPU, 8GB RAM | ~$48 |

All services (Spring Boot, Postgres, Elasticsearch, Redis, Kafka) run on the same VPS using Docker Compose.

**Total**: ~$28–48/month (Hetzner recommended for best value)

Pros: Cheapest by far; full control; great for MVP
Cons: No auto-scaling; you manage uptime, backups, updates

---

### Option D — Hybrid Managed Services (Recommended for Early Stage)

**Best for**: Easiest setup, managed infra, reasonable cost — best for starting out

| Component | Service | Cost/month |
|---|---|---|
| Spring Boot app | Railway / Render (1GB container) | ~$7–20 |
| PostgreSQL | Supabase (Pro plan) | ~$25 |
| Elasticsearch | Elastic Cloud (4GB, GCP-hosted) | ~$70 |
| Redis | Upstash Redis (pay-per-request) | ~$0–10 |
| Kafka | Confluent Cloud (free tier, 1 cluster) | ~$0–30 |
| **Total** | | **~$102–155/month** |

Pros: Fully managed, easy to set up, generous free tiers to start
Cons: Vendor lock-in risk; less control than VPS

---

### Deployment Recommendation by Stage

| Stage | Recommended Option | Monthly Cost |
|---|---|---|
| Development / MVP | Option C (Hetzner VPS + Docker Compose) | ~$28 |
| Early users (< 10k/day) | Option D (Hybrid managed services) | ~$100–155 |
| Growth (10k–100k/day) | Option B (GCP Cloud Run + managed services) | ~$200–300 |
| Scale (100k+/day) | Option A (AWS ECS + MSK + RDS Multi-AZ) | ~$400–800 |

---

### Docker Compose Setup (for Option C / local dev)

All services will be wired via `docker-compose.yml` in the project root:
```yaml
services:
  postgres, elasticsearch, redis, kafka, zookeeper
```
The Spring Boot app connects to these via environment variables.

**Deployment files to create (in Phase 1):**
- `docker-compose.yml` — local dev + VPS deployment
- `Dockerfile` — containerise the Spring Boot app
- `.env.example` — template for all environment variables
