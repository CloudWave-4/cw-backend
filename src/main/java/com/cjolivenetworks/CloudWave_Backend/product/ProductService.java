package com.cjolivenetworks.CloudWave_Backend.product;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    // 상품 등록 (POST)
    @Transactional
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    // 전체 상품 조회 (GET)
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
