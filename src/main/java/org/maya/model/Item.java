package org.maya.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
// Явно указываем SequenceGenerator с allocationSize = 1
@SequenceGenerator(
        name = "items_seq_gen",         // Имя генератора (любое уникальное)
        sequenceName = "items_SEQ",    // Имя sequence в БД (как у Panache по умолчанию)
        allocationSize = 1             // <-- Устанавливаем размер блока = 1
)
public class Item extends PanacheEntity {
    @Column(nullable = false, unique = true)
    public String name;

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false)
    public double price;
}