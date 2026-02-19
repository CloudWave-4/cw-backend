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
    private Long price;

    @Column
    private Long remaining_count;

    @PrePersist
    public void prePersist(){
        this.remaining_count=0L;
    }

    // 임시 서비스의 상품 increase
    public void increase(Long quantity){
        if(remaining_count==null) remaining_count=0L;
        this.remaining_count+=quantity;
    }
}
