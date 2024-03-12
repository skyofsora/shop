package com.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@Getter
@Setter
@ToString
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String email;
    private int number;

    public Email(String email, int number) {
        this.email = email;
        this.number = number;
    }

    public Email(String email) {
        this.email = email;
    }

    public Email() {

    }
}
