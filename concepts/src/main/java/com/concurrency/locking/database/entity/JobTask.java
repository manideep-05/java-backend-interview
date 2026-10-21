package com.concurrency.locking.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_task")
@Getter
@Setter
@NoArgsConstructor
public class JobTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.PENDING;

    private String lockedBy;

    private LocalDateTime lockedAt;

    private LocalDateTime completedAt;

    public enum JobStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public JobTask(String payload) {
        this.payload = payload;
        this.status = JobStatus.PENDING;
    }
}
