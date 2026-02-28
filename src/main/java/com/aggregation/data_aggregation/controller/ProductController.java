package com.aggregation.data_aggregation.controller;

import com.aggregation.data_aggregation.dto.ProductDto;
import com.aggregation.data_aggregation.dto.SourcePostDto;
import com.aggregation.data_aggregation.model.Region;
import com.aggregation.data_aggregation.model.entity.Product;
import com.aggregation.data_aggregation.model.entity.SourcePost;
import com.aggregation.data_aggregation.repository.ProductRepository;
import com.aggregation.data_aggregation.repository.SourcePostRepository;
import com.aggregation.data_aggregation.repository.TrendLeaderboardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Product catalog and source post endpoints.
 *
 * GET /api/products?page=0&size=20         — paginated product list, newest first
 * GET /api/products/search?q={term}        — search by name
 * GET /api/products/{id}                   — single product with trend score
 * GET /api/products/{id}/posts             — all source posts for a product
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final SourcePostRepository sourcePostRepository;
    private final TrendLeaderboardRepository leaderboard;

    public ProductController(
            ProductRepository productRepository,
            SourcePostRepository sourcePostRepository,
            TrendLeaderboardRepository leaderboard) {
        this.productRepository = productRepository;
        this.sourcePostRepository = sourcePostRepository;
        this.leaderboard = leaderboard;
    }

    @GetMapping
    public Page<ProductDto> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productRepository
            .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")))
            .map(this::toDto);
    }

    @GetMapping("/search")
    public List<ProductDto> searchProducts(@RequestParam String q) {
        return productRepository.findByNameContainingIgnoreCase(q)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable String id) {
        return productRepository.findById(id)
            .map(p -> ResponseEntity.ok(toDto(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<List<SourcePostDto>> getPosts(@PathVariable String id) {
        if (!productRepository.existsById(id)) return ResponseEntity.notFound().build();
        List<SourcePostDto> posts = sourcePostRepository.findByProductId(id)
            .stream()
            .map(this::toDto)
            .toList();
        return ResponseEntity.ok(posts);
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private ProductDto toDto(Product p) {
        Map<String, String> extIds = p.getExternalIds().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
        Double score = leaderboard.getScore(p.getId());
        return new ProductDto(
            p.getId(),
            p.getName(),
            p.getBrand(),
            p.getCategory() != null ? p.getCategory().name() : null,
            p.getImageUrl(),
            extIds,
            score,
            p.getFirstSeenAt(),
            p.getUpdatedAt()
        );
    }

    private SourcePostDto toDto(SourcePost sp) {
        Region r = sp.getRegion();
        return new SourcePostDto(
            sp.getId(),
            sp.getPlatform() != null ? sp.getPlatform().name() : null,
            sp.getType() != null ? sp.getType().name() : null,
            sp.getTitle(),
            sp.getContent(),
            sp.getUrl(),
            sp.getAuthorName(),
            sp.getRating(),
            r != null ? r.getState() : null,
            r != null ? r.getCity() : null,
            sp.getCollectedAt()
        );
    }
}
