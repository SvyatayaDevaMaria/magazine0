package org.maya.controller;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.maya.model.Item;
import java.util.List;

@Path("/api/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ItemController {

    @GET
    public List<Item> getAll() {
        return Item.listAll();
    }

    @GET
    @Path("/{id}")
    public Item getById(@PathParam("id") Long id) {
        return Item.findById(id);
    }

    @POST
    @Transactional
    public Response create(Item item) {
        try {
            // Валидация данных
            if (item.name == null || item.name.trim().isEmpty()) {
                return Response.status(400)
                        .entity("{\"error\":\"Название товара обязательно\"}")
                        .build();
            }

            if (item.quantity < 0 || item.price < 0) {
                return Response.status(400)
                        .entity("{\"error\":\"Количество и цена должны быть положительными\"}")
                        .build();
            }

            // Проверка уникальности имени
            if (Item.count("name", item.name) > 0) {
                return Response.status(400)
                        .entity("{\"error\":\"Товар с таким именем уже существует\"}")
                        .build();
            }

            item.persist();
            return Response.status(201).entity(item).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"Ошибка сервера: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, Item updatedItem) {
        try {
            Item item = Item.findById(id);
            if (item == null) {
                return Response.status(404).build();
            }

            // Проверка уникальности имени (если имя изменилось)
            if (!item.name.equals(updatedItem.name) &&
                    Item.count("name", updatedItem.name) > 0) {
                return Response.status(400)
                        .entity("{\"error\":\"Товар с таким именем уже существует\"}")
                        .build();
            }

            item.name = updatedItem.name;
            item.quantity = updatedItem.quantity;
            item.price = updatedItem.price;

            return Response.ok(item).build();

        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"Ошибка сервера: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        try {
            boolean deleted = Item.deleteById(id);
            return deleted
                    ? Response.noContent().build()
                    : Response.status(404).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity("{\"error\":\"Ошибка сервера: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}