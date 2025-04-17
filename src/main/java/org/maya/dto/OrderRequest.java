package org.maya.dto;

import java.util.List;

public class OrderRequest {
    public List<OrderItemRequest> items;

    public OrderRequest() {}

    public OrderRequest(List<OrderItemRequest> items) {
        this.items = items;
    }
}