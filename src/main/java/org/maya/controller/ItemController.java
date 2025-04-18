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
        List<Item> items = Item.listAll();
        System.out.println("Отправляемые данные:" + items);
        return items;
    }

    @POST
    @Transactional
    public Response create(Item item) {
        if (item.name == null || item.name.trim().isEmpty()) {
            return Response.status(400).entity("{\"error\":\"Название обязательно\"}").build();
        }

        if (Item.count("name", item.name) > 0) {
            return Response.status(400).entity("{\"error\":\"Товар уже существует\"}").build();
        }

        item.persist();
        return Response.status(201).entity(item).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, Item updatedItem) {
        Item item = Item.findById(id);
        if (item == null) {
            return Response.status(404).build();
        }

        item.name = updatedItem.name;
        item.quantity = updatedItem.quantity;
        item.price = updatedItem.price;

        return Response.ok(item).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        Item item = Item.findById(id);
        if (item != null) {
            item.delete();
            return Response.noContent().build();
        }
        return Response.status(404).build();
    }
}