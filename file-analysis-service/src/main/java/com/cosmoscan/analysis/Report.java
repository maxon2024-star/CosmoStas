package com.cosmoscan.analysis;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Data
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workId;
    private String status;
    private String issues;
    private String wordCloudUrl;
    private LocalDateTime checkedAt;
}