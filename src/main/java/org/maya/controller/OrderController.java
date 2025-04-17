package org.maya.controller;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.maya.dto.OrderItemRequest;
import org.maya.dto.OrderRequest;
import org.maya.model.*;
import java.util.List;

@Path("/api/orders")
@Produces("application/json")
@Consumes("application/json")
public class OrderController {

    @GET
    public List<Order> getAll() {
        return Order.listAll();
    }

    @POST
    @Transactional
    public Response create(OrderRequest request) {
        if (request.items == null || request.items.isEmpty()) {
            return Response.status(400).entity("{\"error\":\"Товары не выбраны\"}").build();
        }

        Order order = new Order();

        for (OrderItemRequest itemReq : request.items) {
            Item item = Item.findById(itemReq.itemId);
            if (item == null) {
                return Response.status(404).entity("{\"error\":\"Товар не найден\"}").build();
            }
            if (itemReq.quantity <= 0 || itemReq.quantity > item.quantity) {
                return Response.status(400)
                        .entity("{\"error\":\"Некорректное количество для товара " + item.name + "\"}")
                        .build();
            }

            OrderItem orderItem = new OrderItem();
            orderItem.item = item;
            orderItem.order = order;
            orderItem.quantity = itemReq.quantity;
            order.items.add(orderItem);

            item.quantity -= itemReq.quantity;
            item.persist();
        }

        order.persist();
        return Response.ok(order).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        Order order = Order.findById(id);
        if (order == null) {
            return Response.status(404).build();
        }

        // Возвращаем товары на склад
        for (OrderItem orderItem : order.items) {
            Item item = orderItem.item;
            item.quantity += orderItem.quantity;
            item.persist();
        }

        order.delete();
        return Response.noContent().build();
    }
}