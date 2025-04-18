package org.maya.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order extends PanacheEntity {

    @Column(name = "order_date", nullable = false) // Сделаем дату non-null
    public LocalDateTime orderDate = LocalDateTime.now();

    @Column(length = 500) // Добавляем столбец для адреса, можно настроить длину
    public String address; // Поле для адреса

    // Оставляем каскад и orphanRemoval
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // LAZY - стандарт, но явно укажем
    public List<OrderItem> items = new ArrayList<>();

    // Можно добавить вспомогательный метод для добавления OrderItem
    public void addOrderItem(OrderItem item) {
        items.add(item);
        item.order = this;
    }
}