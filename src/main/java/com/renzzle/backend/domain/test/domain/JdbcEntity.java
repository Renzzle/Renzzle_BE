package com.renzzle.backend.domain.test.domain;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "Jdbc_entity")
@Getter
@Setter
@AllArgsConstructor
@Entity
@NoArgsConstructor
@Builder
public class JdbcEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

}
