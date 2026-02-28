package com.aggregation.data_aggregation.repository;

import com.aggregation.data_aggregation.model.entity.SourcePost;
import com.aggregation.data_aggregation.model.enums.Platform;
import com.aggregation.data_aggregation.model.enums.PostType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SourcePostRepository extends JpaRepository<SourcePost, String> {

    List<SourcePost> findByProductId(String productId);

    List<SourcePost> findByPlatform(Platform platform);

    List<SourcePost> findByProductIdAndType(String productId, PostType type);

    List<SourcePost> findByCollectedAtAfter(LocalDateTime since);

    boolean existsByUrlAndPlatform(String url, Platform platform);
}
