package org.maya.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
public class Item extends PanacheEntity {
    @Column(nullable = false, unique = true)
    public String name;

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false)
    public double price;
}