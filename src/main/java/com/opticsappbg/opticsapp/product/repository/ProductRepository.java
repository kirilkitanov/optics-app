package com.opticsappbg.opticsapp.product.repository;

import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    Optional<Product> findByBrandAndModelAndCategory(
            String brand,
            String model,
            ProductCategory category
    );

    // Тази заявка кара базата данни да върне само списък от уникални низове за милисекунди
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND TRIM(p.brand) != ''")
    List<String> findDistinctBrands();
}
