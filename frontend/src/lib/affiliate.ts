const AMAZON_TAG = import.meta.env.VITE_AMAZON_AFFILIATE_TAG as string | undefined

/**
 * Appends your Amazon Associates tracking tag to any amazon.in URL.
 * If no tag is configured, returns the URL unchanged.
 */
export function amazonUrl(url: string | null | undefined): string | undefined {
  if (!url) return undefined
  if (!AMAZON_TAG || !url.includes('amazon.in')) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}tag=${AMAZON_TAG}`
}

/**
 * Appends affiliate tag to a Flipkart URL if it comes from their affiliate API
 * (affiliate URLs already contain the tracking ID, so this is a no-op for them).
 */
export function flipkartUrl(url: string | null | undefined): string | undefined {
  if (!url) return undefined
  return url
}
