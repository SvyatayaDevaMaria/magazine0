package org.maya.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Entity
public class Item extends PanacheEntity {

    @Column(unique = true, nullable = false)
    @NotNull(message = "Название не может быть пустым")
    public String name;

    @Column(nullable = false)
    @Min(value = 0, message = "Количество не может быть отрицательным")
    public int quantity;

    @Column(nullable = false)
    @Min(value = 0, message = "Цена не может быть отрицательной")
    public double price;
}