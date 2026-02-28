package com.aggregation.data_aggregation.model.entity;

import com.aggregation.data_aggregation.model.Region;
import com.aggregation.data_aggregation.model.enums.Platform;
import com.aggregation.data_aggregation.model.enums.PostType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "source_posts", indexes = {
    @Index(columnList = "productId"),
    @Index(columnList = "platform"),
    @Index(columnList = "collectedAt")
})
public class SourcePost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Nullable — set after product deduplication/matching
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String url;
    private String authorName;

    // For reviews: 1.0–5.0; null for non-review types
    private Double rating;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "country", column = @Column(name = "region_country")),
        @AttributeOverride(name = "state",   column = @Column(name = "region_state")),
        @AttributeOverride(name = "city",    column = @Column(name = "region_city"))
    })
    private Region region = new Region();

    private LocalDateTime postedAt;
    private LocalDateTime collectedAt;

    @PrePersist
    protected void onCreate() {
        collectedAt = LocalDateTime.now();
    }

    public String getId() { return id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }

    public PostType getType() { return type; }
    public void setType(PostType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }

    public LocalDateTime getCollectedAt() { return collectedAt; }
}
