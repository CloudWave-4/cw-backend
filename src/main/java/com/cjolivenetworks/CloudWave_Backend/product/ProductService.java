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

    // 대기열 처리
    @Transactional
    public void increaseStock(Long productId,int quantity){
        Product product=productRepository.findById(productId)
                .orElseThrow(()->new IllegalArgumentException("Product not found: "+productId));
        int q=Math.max(quantity,1);
        product.increase(1);
    }
}
