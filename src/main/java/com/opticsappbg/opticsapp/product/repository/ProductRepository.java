package com.opticsappbg.opticsapp.product.repository;

import com.opticsappbg.opticsapp.product.model.Product;
import com.opticsappbg.opticsapp.product.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    @Query("""
            SELECT p
            FROM Product p
            WHERE p.category = :category
              AND p.brand = :brand
              AND p.model = :model
              AND (p.barcode = :barcode OR (p.barcode IS NULL AND :barcode IS NULL))
              AND (p.sizeOrDiameter = :sizeOrDiameter OR (p.sizeOrDiameter IS NULL AND :sizeOrDiameter IS NULL))
              AND p.sph = :sph
              AND p.cyl = :cyl
              AND p.addPower = :addPower
              AND p.prism = :prism
            """)
    Optional<Product> findExactProduct(
            @Param("category") ProductCategory category,
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("barcode") String barcode,
            @Param("sizeOrDiameter") String sizeOrDiameter,
            @Param("sph") BigDecimal sph,
            @Param("cyl") BigDecimal cyl,
            @Param("addPower") BigDecimal addPower,
            @Param("prism") BigDecimal prism
    );

    @Query("""
            SELECT p
            FROM Product p
            WHERE p.category = :category
              AND p.brand = :brand
              AND p.model = :model
              AND (p.sizeOrDiameter = :sizeOrDiameter OR (p.sizeOrDiameter IS NULL AND :sizeOrDiameter IS NULL))
              AND p.sph = :sph
              AND p.cyl = :cyl
              AND p.addPower = :addPower
              AND p.prism = :prism
            """)
    List<Product> findProductsByCharacteristics(
            @Param("category") ProductCategory category,
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("sizeOrDiameter") String sizeOrDiameter,
            @Param("sph") BigDecimal sph,
            @Param("cyl") BigDecimal cyl,
            @Param("addPower") BigDecimal addPower,
            @Param("prism") BigDecimal prism
    );

    // Тази заявка кара базата данни да върне само списък от уникални низове за милисекунди
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND TRIM(p.brand) != ''")
    List<String> findDistinctBrands();
}
