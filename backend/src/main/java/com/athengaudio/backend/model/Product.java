package com.athengaudio.backend.model;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "products")
public class Product {
    @Id
    private String id;

    private String name;
    private String description;
    private String category;
    private String brand;
    private Double price;
    private Double originalPrice; // Giá gốc để tính discount
    private Integer stock;
    private List<String> images;
    private List<String> colors;
    private List<String> sizes;

    // Specifications
    private String connectivity;
    private String batteryLife;
    private String weight;
    private String compatibility;
    private String warranty;

    // Ratings & Reviews
    private Double rating;
    private Integer reviewCount;

    // Features
    private List<String> features;
    private List<String> tags;

    // Status
    private Boolean isActive;
    private Boolean isFeatured;

    // Timestamps
    private Date createdAt;
    private Date updatedAt;
}
