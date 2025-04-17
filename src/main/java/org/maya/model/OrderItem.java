package org.maya.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
public class OrderItem extends PanacheEntity {
    @ManyToOne
    public Item item;

    @ManyToOne
    public Order order;

    public int quantity;
}