package com.cjolivenetworks.CloudWave_Backend.product;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 상품 등록
    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productService.saveProduct(product);
    }

    // 상품 전체 목록 조회
    @GetMapping
    public List<Product> getProducts() {
        return productService.getAllProducts();
    }
}