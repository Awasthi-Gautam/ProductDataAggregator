package com.aggregation.data_aggregation.collector;

import com.aggregation.data_aggregation.model.entity.Product;
import com.aggregation.data_aggregation.model.entity.SourcePost;
import com.aggregation.data_aggregation.model.enums.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectionResult {

    private final Platform platform;
    private final List<ProductEntry> entries = new ArrayList<>();
    private final List<SourcePost> standalonePosts = new ArrayList<>(); // enrichment posts (productId already set)
    private String errorMessage;

    public CollectionResult(Platform platform) {
        this.platform = platform;
    }

    // ── Discovery: pair a product with its associated posts ───────────────

    public ProductEntry addProduct(Product product) {
        ProductEntry entry = new ProductEntry(product);
        entries.add(entry);
        return entry;
    }

    // ── Enrichment: posts linked to an existing product ───────────────────

    public void addPost(SourcePost post) {
        standalonePosts.add(post);
    }

    public void setError(String message) {
        this.errorMessage = message;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public Platform getPlatform() { return platform; }
    public List<ProductEntry> getEntries() { return Collections.unmodifiableList(entries); }
    public List<SourcePost> getStandalonePosts() { return Collections.unmodifiableList(standalonePosts); }
    public String getErrorMessage() { return errorMessage; }

    public int productCount() { return entries.size(); }
    public int postCount() {
        int n = standalonePosts.size();
        for (ProductEntry e : entries) n += e.posts.size();
        return n;
    }

    // ── Inner class ───────────────────────────────────────────────────────

    public static class ProductEntry {
        private final Product product;
        private final List<SourcePost> posts = new ArrayList<>();

        ProductEntry(Product product) {
            this.product = product;
        }

        public ProductEntry addPost(SourcePost post) {
            posts.add(post);
            return this;
        }

        public Product getProduct() { return product; }
        public List<SourcePost> getPosts() { return Collections.unmodifiableList(posts); }
    }
}
