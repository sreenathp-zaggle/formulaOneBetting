package org.example.formulaone.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drivers")
public class Driver {
    @Id
    private Integer id;
    private String fullName;
    private String country;
    private String team;
}
