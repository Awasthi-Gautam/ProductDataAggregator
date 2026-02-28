package com.aggregation.data_aggregation.repository;

import com.aggregation.data_aggregation.model.entity.Product;
import com.aggregation.data_aggregation.model.enums.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findByNameAndBrand(String name, String brand);

    List<Product> findByCategory(ProductCategory category);

    List<Product> findByNameContainingIgnoreCase(String name);
}
