package com.cjolivenetworks.CloudWave_Backend.product;

import com.cjolivenetworks.CloudWave_Backend.global.queue.QueueConstants;
import com.cjolivenetworks.CloudWave_Backend.global.queue.dto.QueueEnterRequest;
import com.cjolivenetworks.CloudWave_Backend.global.queue.dto.QueueEnterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // DB 즉각 접근 상품 구매
    @PostMapping("/buy")
    public void increaseRemaining(@RequestParam Long productId){
        productService.increaseStock(productId,1);
    }
}