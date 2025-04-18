package org.maya.controller;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.maya.model.Item;
import org.maya.model.Order; // Импортируем Order
import org.maya.model.OrderItem; // Импортируем OrderItem

import java.util.List;
import java.util.Set; // Используем Set для уникальных ID заказов
import java.util.stream.Collectors; // Для сбора ID

@Path("/api/items")
@Produces(MediaType.APPLICATION_JSON)
// Убираем @Consumes с класса
public class ItemController {

    @GET
    public List<Item> getAll() {
        List<Item> items = Item.listAll();
        System.out.println("Отправляемые данные (товары):" + items);
        return items;
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON) // Добавляем к методу
    public Response create(Item item) {
        // Валидация входных данных
        if (item == null || item.name == null || item.name.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Название товара обязательно\"}")
                    .build();
        }
        if (item.quantity < 0) { // Количество не может быть отрицательным
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Количество товара не может быть отрицательным\"}")
                    .build();
        }
        if (item.price < 0) { // Цена не может быть отрицательной
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Цена товара не может быть отрицательной\"}")
                    .build();
        }


        // Проверка уникальности имени (без учета регистра для надежности)
        if (Item.count("LOWER(name) = LOWER(?1)", item.name.trim()) > 0) {
            return Response.status(Response.Status.CONFLICT) // 409 Conflict - более подходящий статус
                    .entity("{\"error\":\"Товар с таким названием уже существует\"}")
                    .build();
        }

        // Устанавливаем оттримованное имя
        item.name = item.name.trim();
        // Явно устанавливаем ID в null, чтобы Panache сгенерировал новый
        item.id = null;

        try {
            item.persist();
            return Response.status(Response.Status.CREATED).entity(item).build(); // 201 Created
        } catch (Exception e) {
            // Логируем ошибку на сервере для диагностики
            System.err.println("Ошибка при сохранении товара: " + e.getMessage());
            e.printStackTrace(); // Печатаем полный стектрейс в лог сервера
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при сохранении товара\"}")
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON) // Добавляем к методу
    public Response update(@PathParam("id") Long id, Item updatedItem) {
        // Валидация входных данных
        if (updatedItem == null || updatedItem.name == null || updatedItem.name.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Название товара обязательно\"}")
                    .build();
        }
        if (updatedItem.quantity < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Количество товара не может быть отрицательным\"}")
                    .build();
        }
        if (updatedItem.price < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Цена товара не может быть отрицательной\"}")
                    .build();
        }

        Item item = Item.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Товар с ID " + id + " не найден\"}")
                    .build();
        }

        // Проверка уникальности имени при изменении (если имя отличается от старого)
        String newNameTrimmed = updatedItem.name.trim();
        if (!item.name.equalsIgnoreCase(newNameTrimmed)) { // Сравниваем без учета регистра
            if (Item.count("LOWER(name) = LOWER(?1) AND id != ?2", newNameTrimmed, id) > 0) {
                return Response.status(Response.Status.CONFLICT) // 409 Conflict
                        .entity("{\"error\":\"Другой товар с названием '" + newNameTrimmed + "' уже существует\"}")
                        .build();
            }
            item.name = newNameTrimmed; // Обновляем имя, если проверка пройдена
        }


        // Обновляем остальные поля
        item.quantity = updatedItem.quantity;
        item.price = updatedItem.price;

        try {
            // persist() для управляемой сущности не нужен, изменения сохранятся при коммите
            return Response.ok(item).build();
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении товара: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при обновлении товара\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        Item item = Item.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Товар с ID " + id + " не найден\"}")
                    .build();
        }

        try {
            // 1. Найти все OrderItem, связанные с этим Item
            List<OrderItem> relatedOrderItems = OrderItem.list("item", item);

            // 2. Собрать уникальные ID заказов, которые будут затронуты
            Set<Long> affectedOrderIds = relatedOrderItems.stream()
                    .map(orderItem -> orderItem.order.id)
                    .collect(Collectors.toSet());

            // 3. Удалить все связанные OrderItem
            // Можно сделать циклом или одним запросом
            OrderItem.delete("item", item);
            // Явно вызовем flush, чтобы изменения применились до проверки заказов
            OrderItem.flush();


            // 4. Удалить сам товар
            item.delete();

            // 5. Проверить затронутые заказы и удалить пустые
            for (Long orderId : affectedOrderIds) {
                // Проверяем, остался ли заказ и есть ли у него еще позиции
                Order order = Order.findById(orderId);
                if (order != null) { // Заказ еще существует
                    // Пересчитываем количество позиций в заказе после удаления
                    long remainingItemsCount = OrderItem.count("order", order);
                    if (remainingItemsCount == 0) {
                        System.out.println("Удаляем пустой заказ ID: " + orderId);
                        order.delete();
                    }
                }
            }

            return Response.noContent().build(); // 204 No Content

        } catch (Exception e) {
            System.err.println("Ошибка при удалении товара и связанных данных: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при удалении товара\"}")
                    .build();
        }
    }
}