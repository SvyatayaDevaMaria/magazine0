package org.maya.controller;

import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid; // Импорт для валидации DTO
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.maya.dto.OrderItemRequest;
import org.maya.dto.OrderRequest;
import org.maya.dto.OrderResponseDTO;
import org.maya.dto.OrderItemResponseDTO;
import org.maya.model.*;

import java.util.List;
import java.util.stream.Collectors;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
// Убираем @Consumes с класса
public class OrderController {

    // Метод GET /api/orders/items не логичен здесь, убираем или переносим в ItemController

    @GET
    public List<OrderResponseDTO> getAllOrders() {
        List<Order> orders = Order.listAll(Sort.by("id"));
        return orders.stream().map(this::mapOrderToDto).collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        Order order = Order.findById(id);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Заказ с ID " + id + " не найден\"}")
                    .build();
        }
        return Response.ok(mapOrderToDto(order)).build();
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON) // Добавляем к методу
    // Добавляем @Valid для автоматической валидации запроса на основе аннотаций в DTO
    public Response create(@Valid OrderRequest request) {
        // Валидация запроса (например, @Valid) уже должна была отработать благодаря аннотациям в DTO
        // Если нет - Quarkus вернет 400 Bad Request автоматически

        Order order = new Order();
        order.address = request.address; // Устанавливаем адрес

        // Проверяем наличие товаров ДО изменения количества
        for (OrderItemRequest itemReq : request.items) {
            Item item = Item.findById(itemReq.itemId);
            if (item == null) {
                return Response.status(Response.Status.NOT_FOUND) // 404 Not Found лучше, чем 400
                        .entity("{\"error\":\"Товар с ID " + itemReq.itemId + " не найден\"}")
                        .build();
            }
            // Проверяем доступное количество
            if (itemReq.quantity > item.quantity) {
                return Response.status(Response.Status.CONFLICT) // 409 Conflict - конфликт состояния (нет товара)
                        .entity("{\"error\":\"Недостаточно товара '" + item.name + "' на складе (запрошено: " + itemReq.quantity + ", в наличии: " + item.quantity + ")\"}")
                        .build();
            }
        }

        // Если все проверки пройдены, создаем OrderItem и обновляем количество Item
        for (OrderItemRequest itemReq : request.items) {
            Item item = Item.findById(itemReq.itemId); // Находим снова (в рамках транзакции)
            // Здесь уже не должно быть ошибок наличия или количества, т.к. проверили выше

            OrderItem orderItem = new OrderItem();
            orderItem.item = item;
            // orderItem.order = order; // Связь установится через order.addOrderItem
            orderItem.quantity = itemReq.quantity;
            order.addOrderItem(orderItem); // Используем вспомогательный метод

            // Уменьшаем количество на складе
            item.quantity -= itemReq.quantity;
            // item.persist(); // Не нужно, Hibernate отследит изменения
        }

        try {
            order.persist();
            // Возвращаем созданный заказ через DTO
            return Response.status(Response.Status.CREATED).entity(mapOrderToDto(order)).build();
        } catch (Exception e) {
            System.err.println("Ошибка при создании заказа: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при создании заказа\"}")
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON) // Добавляем к методу
    public Response update(@PathParam("id") Long id, @Valid OrderRequest request) {
        // @Valid также валидирует входной DTO

        Order order = Order.findById(id);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Заказ с ID " + id + " не найден\"}")
                    .build();
        }

        // --- Логика возврата старых товаров на склад ---
        for (OrderItem oldOrderItem : order.items) {
            if (oldOrderItem.item != null) {
                oldOrderItem.item.quantity += oldOrderItem.quantity;
            }
        }
        // Очищаем старые позиции (Hibernate удалит их благодаря orphanRemoval)
        order.items.clear();
        // Важно: Flush для применения возврата на склад ПЕРЕД проверкой наличия для новых
        Order.flush();


        // --- Проверяем наличие и количество для НОВЫХ позиций ---
        for (OrderItemRequest itemReq : request.items) {
            Item item = Item.findById(itemReq.itemId);
            if (item == null) {
                // Нужно откатить изменения (возврат на склад), если товар не найден?
                // Транзакция откатится автоматически при выбросе исключения или возврате ошибки.
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Товар с ID " + itemReq.itemId + " не найден для обновления заказа\"}")
                        .build();
            }
            // Проверяем доступное количество ПОСЛЕ возврата старых и flush()
            if (itemReq.quantity > item.quantity) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\":\"Недостаточно товара '" + item.name + "' на складе для обновления (запрошено: " + itemReq.quantity + ", в наличии: " + item.quantity + ")\"}")
                        .build();
            }
        }

        // --- Если все проверки пройдены, добавляем новые позиции и списываем со склада ---
        order.address = request.address; // Обновляем адрес
        for (OrderItemRequest itemReq : request.items) {
            Item item = Item.findById(itemReq.itemId);
            // Ошибок уже быть не должно

            OrderItem newOrderItem = new OrderItem();
            newOrderItem.item = item;
            // newOrderItem.order = order;
            newOrderItem.quantity = itemReq.quantity;
            order.addOrderItem(newOrderItem); // Добавляем в обновленный список заказа

            // Уменьшаем количество на складе
            item.quantity -= itemReq.quantity;
        }

        try {
            // persist не нужен, так как order управляется EntityManager'ом
            return Response.ok(mapOrderToDto(order)).build();
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении заказа: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при обновлении заказа\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        Order order = Order.findById(id);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Заказ с ID " + id + " не найден\"}")
                    .build();
        }

        try {
            // Возвращаем товары на склад перед удалением заказа
            for (OrderItem orderItem : order.items) {
                if (orderItem.item != null) {
                    orderItem.item.quantity += orderItem.quantity;
                }
            }
            // Удаляем заказ (OrderItem удалятся каскадно благодаря orphanRemoval=true)
            order.delete();
            return Response.noContent().build();
        } catch (Exception e) {
            System.err.println("Ошибка при удалении заказа: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при удалении заказа\"}")
                    .build();
        }
    }

    // Вспомогательный метод для маппинга Order -> OrderResponseDTO
    private OrderResponseDTO mapOrderToDto(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.id = order.id;
        dto.orderDate = order.orderDate;
        dto.address = order.address; // Маппим адрес
        dto.items = order.items.stream()
                // Загружаем данные перед маппингом, если FetchType.LAZY
                // В данном случае Hibernate/Panache должны справиться при доступе к полям
                .map(oi -> {
                    OrderItemResponseDTO itemDto = new OrderItemResponseDTO();
                    itemDto.quantity = oi.quantity;
                    if (oi.item != null) {
                        itemDto.itemId = oi.item.id;
                        itemDto.itemName = oi.item.name;
                        itemDto.price = oi.item.price;
                    } else {
                        itemDto.itemName = "[Товар удален]";
                    }
                    return itemDto;
                })
                .collect(Collectors.toList());
        return dto;
    }
}