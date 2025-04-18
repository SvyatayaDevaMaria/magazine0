package org.maya.dto;

import jakarta.validation.Valid; // Для валидации вложенных объектов
import jakarta.validation.constraints.*;
import java.util.List;

public class OrderRequest {

    @Size(min = 1, message = "Заказ должен содержать хотя бы один товар") // Список не должен быть пустым
    @Valid // Включаем валидацию для элементов списка (OrderItemRequest)
    public List<OrderItemRequest> items;

    @NotBlank(message = "Адрес доставки не может быть пустым") // Добавляем валидацию адреса
    @Size(max = 500, message = "Адрес слишком длинный")
    public String address; // Добавляем поле адреса в запрос
}

// --- OrderItemRequest остается без изменений ---
// package org.maya.dto;
// import jakarta.validation.constraints.*;
// public class OrderItemRequest {
//     @NotNull(message = "ID товара не может быть null")
//     public Long itemId;
//     @Min(value = 1, message = "Количество должно быть не менее 1")
//     public int quantity;
// }
