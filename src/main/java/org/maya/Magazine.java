package org.maya;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class Magazine {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getInventory() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Магазин</title>
                <style>
                    body { font-family: Arial; margin: 20px; }
                    .section { margin-bottom: 30px; padding: 20px; border: 1px solid #ddd; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 15px; }
                    th, td { border: 1px solid #ddd; padding: 8px; }
                    th { background-color: #f2f2f2; }
                    button, input, select { padding: 8px; margin: 5px; }
                    .notification { 
                        position: fixed; 
                        top: 20px; 
                        right: 20px; 
                        padding: 15px; 
                        background: #4CAF50; 
                        color: white; 
                        display: none;
                        border-radius: 5px;
                    }
                    .error { background: #f44336; }
                </style>
            </head>
            <body>
                <div id="notification" class="notification"></div>
                <div id="error-message" style="display:none; color:red; padding:10px; background:#ffecec;"></div>
                
                <div class="section">
                    <h2>Склад</h2>
                    <div>
                        <input type="text" id="itemName" placeholder="Название">
                        <input type="number" id="itemQuantity" placeholder="Количество" min="0">
                        <input type="number" id="itemPrice" placeholder="Цена" step="0.01" min="0">
                        <button onclick="addItem()">Добавить товар</button>
                    </div>
                    <table id="itemsTable">
                        <thead>
                            <tr><th>ID</th><th>Название</th><th>Кол-во</th><th>Цена</th><th>Действия</th></tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
                
                <div class="section">
                    <h2>Заказы</h2>
                    <div>
                        <select id="itemSelect"></select>
                        <input type="number" id="orderQuantity" placeholder="Количество" min="1" value="1">
                        <button onclick="addToOrder()">Добавить в заказ</button>
                        <button onclick="createOrder()">Создать заказ</button>
                    </div>
                    <table id="orderItemsTable">
                        <thead>
                            <tr><th>Товар</th><th>Кол-во</th><th>Действие</th></tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                    
                    <h3>Список заказов</h3>
                    <table id="ordersTable">
                        <thead>
                            <tr><th>ID</th><th>Дата</th><th>Товары</th><th>Действие</th></tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>

                <script>
                fetch('/api/items')
                        .then(res => res.text())
                        .then(text => console.log("Длина ответа:", text.length))
                        .catch(console.error);
                    // Глобальные переменные
                    let items = [];
                    let orders = [];
                    let currentOrderItems = [];
                    
                    // Утилиты
                    function showNotification(message, isError = false) {
                        const notification = document.getElementById('notification');
                        notification.textContent = message;
                        notification.className = isError ? 'notification error' : 'notification';
                        notification.style.display = 'block';
                        
                        setTimeout(() => {
                            notification.style.display = 'none';
                        }, 3000);
                    }
                    
                    // Обработчик ошибок
                    window.onerror = function(message, source, lineno, colno, error) {
                        console.error("Global error:", {message, source, lineno, colno, error});
                        const errorDiv = document.getElementById('error-message');
                        errorDiv.textContent = `Error: ${message}`;
                        errorDiv.style.display = 'block';
                        return true;
                    };
                    
                    // ========== Загрузка данных ==========
                    async function loadAllData() {
                             try {
                                 const itemsRes = await fetch('/api/items');
                                 const ordersRes = await fetch('/api/orders');
                                \s
                                 // Добавьте проверку Content-Type
                                 const contentType = itemsRes.headers.get('content-type');
                                 if (!contentType || !contentType.includes('application/json')) {
                                     throw new Error("Неверный формат ответа");
                                 }
                                \s
                                 const items = await itemsRes.json();
                                 const orders = await ordersRes.json();
                                \s
                                 console.log("Данные получены");
                                 renderItems(items);
                                 renderOrders(orders);
                                \s
                             } catch (error) {
                                 console.error("Ошибка загрузки:", error);
                                 showError("Ошибка загрузки данных");
                             }
                         }
                    
                    // ========== Товары ==========
                    function renderItems() {
                        const tbody = document.querySelector('#itemsTable tbody');
                        tbody.innerHTML = items.map(item => `
                            <tr>
                                <td>${item.id}</td>
                                <td>${item.name}</td>
                                <td>${item.quantity}</td>
                                <td>${item.price.toFixed(2)}</td>
                                <td>
                                    <button onclick="editItem(${item.id})">Изменить</button>
                                    <button onclick="deleteItem(${item.id})">Удалить</button>
                                </td>
                            </tr>
                        `).join('');
                    }
                    
                    async function addItem() {
                        const name = document.getElementById('itemName').value.trim();
                        const quantity = parseInt(document.getElementById('itemQuantity').value);
                        const price = parseFloat(document.getElementById('itemPrice').value);
                        
                        if (!name || isNaN(quantity)) {
                            showNotification("Заполните все поля корректно", true);
                            return;
                        }
                        
                        try {
                            const response = await fetch('/api/items', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ name: name, quantity: quantity, price: price })
                            });
                            
                            if (!response.ok) {
                                const error = await response.json();
                                throw new Error(error.error || 'Ошибка сервера');
                            }
                            
                            showNotification("Товар добавлен");
                            loadAllData();
                            
                            // Очищаем поля ввода
                            document.getElementById('itemName').value = '';
                            document.getElementById('itemQuantity').value = '';
                            document.getElementById('itemPrice').value = '';
                            
                        } catch (error) {
                            showNotification("Ошибка: " + error.message, true);
                        }
                    }
                    
                    async function editItem(id) {
                        const item = items.find(i => i.id === id);
                        if (!item) return;
                        
                        const newName = prompt('Название:', item.name);
                        const newQty = prompt('Количество:', item.quantity);
                        const newPrice = prompt('Цена:', item.price);
                        
                        if (newName && newQty && newPrice) {
                            try {
                                const response = await fetch('/api/items/' + id, {
                                    method: 'PUT',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({
                                        name: newName,
                                        quantity: newQty,
                                        price: newPrice
                                    })
                                });
                                
                                if (!response.ok) {
                                    const error = await response.json();
                                    throw new Error(error.error || 'Ошибка сервера');
                                }
                                
                                showNotification("Товар обновлен");
                                loadAllData();
                                
                            } catch (error) {
                                showNotification("Ошибка: " + error.message, true);
                            }
                        }
                    }
                    
                    async function deleteItem(id) {
                        if (confirm('Удалить товар?')) {
                            try {
                                const response = await fetch('/api/items/' + id, {
                                    method: 'DELETE'
                                });
                                
                                if (!response.ok) {
                                    throw new Error('Ошибка сервера');
                                }
                                
                                showNotification("Товар удален");
                                loadAllData();
                                
                            } catch (error) {
                                showNotification("Ошибка: " + error.message, true);
                            }
                        }
                    }
                    
                    // ========== Заказы ==========
                    function updateItemSelect() {
                        const select = document.getElementById('itemSelect');
                        select.innerHTML = items.map(item => `
                            <option value="${item.id}">${item.name} (${item.quantity} шт.)</option>
                        `).join('');
                    }
                    
                    function renderOrders() {
                        const tbody = document.querySelector('#ordersTable tbody');
                        tbody.innerHTML = orders.length ? orders.map(order => `
                            <tr>
                                <td>${order.id}</td>
                                <td>${new Date(order.orderDate).toLocaleString()}</td>
                                <td>
                                    <ul>${order.items.map(item => `
                                        <li>${item.item ? item.item.name : 'Товар'} - ${item.quantity} шт.</li>
                                    `).join('')}</ul>
                                </td>
                                <td>
                                    <button onclick="deleteOrder(${order.id})">Удалить</button>
                                </td>
                            </tr>
                        `).join('') : '<tr><td colspan="4">Нет заказов</td></tr>';
                    }
                    
                    function addToOrder() {
                        const select = document.getElementById('itemSelect');
                        const itemId = parseInt(select.value);
                        const quantity = parseInt(document.getElementById('orderQuantity').value);
                        const itemName = select.options[select.selectedIndex].text.split(' (')[0];
                        
                        if (isNaN(quantity)) {
                            showNotification("Введите корректное количество", true);
                            return;
                        }
                        
                        currentOrderItems.push({ itemId: itemId, quantity: quantity, itemName: itemName });
                        updateOrderTable();
                    }
                    
                    function updateOrderTable() {
                        const tbody = document.querySelector('#orderItemsTable tbody');
                        tbody.innerHTML = currentOrderItems.map((item, index) => `
                            <tr>
                                <td>${item.itemName}</td>
                                <td>${item.quantity}</td>
                                <td><button onclick="removeFromOrder(${index})">Удалить</button></td>
                            </tr>
                        `).join('');
                    }
                    
                    function removeFromOrder(index) {
                        currentOrderItems.splice(index, 1);
                        updateOrderTable();
                    }
                    
                    async function createOrder() {
                        if (currentOrderItems.length === 0) {
                            showNotification("Добавьте товары в заказ", true);
                            return;
                        }
                        
                        const orderRequest = {
                            items: currentOrderItems.map(item => ({
                                itemId: item.itemId,
                                quantity: item.quantity
                            }))
                        };
                        
                        try {
                            const response = await fetch('/api/orders', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify(orderRequest)
                            });
                            
                            if (!response.ok) {
                                const error = await response.json();
                                throw new Error(error.error || 'Ошибка сервера');
                            }
                            
                            showNotification("Заказ создан");
                            currentOrderItems = [];
                            updateOrderTable();
                            loadAllData();
                            
                        } catch (error) {
                            showNotification("Ошибка: " + error.message, true);
                        }
                    }
                    
                    async function deleteOrder(id) {
                        if (confirm('Удалить заказ?')) {
                            try {
                                const response = await fetch('/api/orders/' + id, {
                                    method: 'DELETE'
                                });
                                
                                if (!response.ok) {
                                    throw new Error('Ошибка сервера');
                                }
                                
                                showNotification("Заказ удален");
                                loadAllData();
                                
                            } catch (error) {
                                showNotification("Ошибка: " + error.message, true);
                            }
                        }
                    }
                    
                    // Инициализация при загрузке страницы
                    document.addEventListener('DOMContentLoaded', loadAllData);
                </script>
            </body>
            </html>
            """;
    }
}