package com.aggregation.data_aggregation.repository;

import com.aggregation.data_aggregation.model.document.SourcePostDocument;
import com.aggregation.data_aggregation.model.enums.Platform;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SourcePostSearchRepository extends ElasticsearchRepository<SourcePostDocument, String> {

    List<SourcePostDocument> findByProductId(String productId);

    Page<SourcePostDocument> findByContentContainingOrTitleContaining(
        String contentKeyword, String titleKeyword, Pageable pageable
    );

    Page<SourcePostDocument> findByProductIdAndPlatform(
        String productId, Platform platform, Pageable pageable
    );

    Page<SourcePostDocument> findByStateAndCountry(String state, String country, Pageable pageable);
}
