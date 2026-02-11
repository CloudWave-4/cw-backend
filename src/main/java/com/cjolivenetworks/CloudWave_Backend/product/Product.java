package com.cjolivenetworks.CloudWave_Backend.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="product_id")
    private Long id;

    @Column
    private Integer price;

    @Column
    private Integer remaining_count;

    @PrePersist
    public void prePersist(){
        this.remaining_count=0;
    }

    // 임시 서비스의 상품 increase
    public void increase(int quantity){
        if(remaining_count==null) remaining_count=0;
        this.remaining_count+=quantity;
    }
}
