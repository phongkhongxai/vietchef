package com.spring2025.vietchefs.models.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chef_id", nullable = false)
    private Chef chef;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    @Column(nullable = false)
    private boolean isDeleted;
}
