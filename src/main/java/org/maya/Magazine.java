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
                                           <title>Управление складом и заказами</title>
                                           <style>
                                               body { font-family: Arial, sans-serif; margin: 20px; }
                                               .section { margin-bottom: 30px; padding: 20px; border: 1px solid #ddd; border-radius: 5px; }
                                               table { width: 100%; border-collapse: collapse; margin-bottom: 15px; }
                                               th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                                               th { background-color: #f2f2f2; }
                                               input, button, select { margin: 5px; padding: 8px; }
                                               .form-group { margin-bottom: 15px; }
                                               .notification {
                                                   position: fixed; top: 20px; right: 20px;
                                                   padding: 15px; background-color: #4CAF50;
                                                   color: white; display: none; z-index: 1000;
                                               }
                                               .error { color: red; }
                                           </style>
                                       </head>
                                       <body>
                                           <div id="notification" class="notification"></div>
                                          
                                           <div class="section">
                                               <h2>Товары на складе</h2>
                                               <div class="form-group">
                                                   <input type="text" id="itemName" placeholder="Название" required>
                                                   <input type="number" id="itemQuantity" placeholder="Количество" min="0" required>
                                                   <input type="number" id="itemPrice" placeholder="Цена" step="0.01" min="0.01" required>
                                                   <button onclick="addItem()">Добавить товар</button>
                                               </div>
                                               <table id="itemsTable">
                                                   <thead>
                                                       <tr>
                                                           <th>ID</th>
                                                           <th>Название</th>
                                                           <th>Количество</th>
                                                           <th>Цена</th>
                                                           <th>Действия</th>
                                                       </tr>
                                                   </thead>
                                                   <tbody></tbody>
                                               </table>
                                           </div>
                
                                           <div class="section">
                                               <h2>Создать заказ</h2>
                                               <div class="form-group">
                                                   <select id="itemSelect"></select>
                                                   <input type="number" id="orderQuantity" placeholder="Количество" min="1" value="1">
                                                   <button onclick="addToOrder()">Добавить в заказ</button>
                                                   <button onclick="createOrder()">Создать заказ</button>
                                               </div>
                                               <table id="orderItemsTable">
                                                   <thead>
                                                       <tr>
                                                           <th>Товар</th>
                                                           <th>Количество</th>
                                                           <th>Действие</th>
                                                       </tr>
                                                   </thead>
                                                   <tbody></tbody>
                                               </table>
                                           </div>
                
                                           <div class="section">
                                               <h2>Список заказов</h2>
                                               <table id="ordersTable">
                                                   <thead>
                                                       <tr>
                                                           <th>ID</th>
                                                           <th>Товары</th>
                                                           <th>Дата</th>
                                                           <th>Действие</th>
                                                       </tr>
                                                   </thead>
                                                   <tbody></tbody>
                                               </table>
                                           </div>
                
                                           <script>
                                               let currentOrderItems = [];
                                              
                                               // Функция для показа уведомлений
                                               function showNotification(message, isSuccess = true) {
                                                   const notification = document.getElementById('notification');
                                                   notification.textContent = message;
                                                   notification.style.backgroundColor = isSuccess ? '#4CAF50' : '#f44336';
                                                   notification.style.display = 'block';
                                                   setTimeout(() => notification.style.display = 'none', 3000);
                                               }
                
                                               // Загрузка данных при старте
                                               document.addEventListener('DOMContentLoaded', () => {
                                                   loadItems();
                                                   loadOrders();
                                               });
                
                                               // ========== Товары ==========
                                               async function loadItems() {
                                                   try {
                                                       const response = await fetch('/api/items');
                                                       if (!response.ok) throw new Error('Ошибка загрузки товаров');
                                                      
                                                       const items = await response.json();
                                                       renderItems(items);
                                                       updateItemSelect(items);
                                                   } catch (error) {
                                                       console.error('Ошибка:', error);
                                                       showNotification(error.message, false);
                                                   }
                                               }
                
                                               function renderItems(items) {
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
                
                                               function updateItemSelect(items) {
                                                   const select = document.getElementById('itemSelect');
                                                   select.innerHTML = items.map(item =>
                                                       `<option value="${item.id}">${item.name} (${item.quantity} шт.)</option>`
                                                   ).join('');
                                               }
                
                                               async function addItem() {
                                                   const item = {
                                                       name: document.getElementById('itemName').value.trim(),
                                                       quantity: parseInt(document.getElementById('itemQuantity').value),
                                                       price: parseFloat(document.getElementById('itemPrice').value)
                                                   };
                
                                                   if (!item.name || isNaN(item.quantity) {
                                                       showNotification('Заполните все поля корректно', false);
                                                       return;
                                                   }
                
                                                   try {
                                                       const response = await fetch('/api/items', {
                                                           method: 'POST',
                                                           headers: {
                                                               'Content-Type': 'application/json',
                                                               'Accept': 'application/json'
                                                           },
                                                           body: JSON.stringify(item)
                                                       });
                
                                                       if (!response.ok) {
                                                           const error = await response.json();
                                                           throw new Error(error.error || 'Ошибка сервера');
                                                       }
                
                                                       showNotification('Товар успешно добавлен');
                                                       loadItems();
                                                      
                                                       // Очищаем форму
                                                       document.getElementById('itemName').value = '';
                                                       document.getElementById('itemQuantity').value = '';
                                                       document.getElementById('itemPrice').value = '';
                                                      
                                                   } catch (error) {
                                                       console.error('Ошибка:', error);
                                                       showNotification(error.message, false);
                                                   }
                                               }
                
                                               async function editItem(id) {
                                                   try {
                                                       const item = await fetch(`/api/items/${id}`).then(r => r.json());
                                                      
                                                       const newName = prompt("Название:", item.name);
                                                       const newQty = prompt("Количество:", item.quantity);
                                                       const newPrice = prompt("Цена:", item.price);
                                                      
                                                       if (newName && newQty && newPrice) {
                                                           const updatedItem = {
                                                               name: newName,
                                                               quantity: parseInt(newQty),
                                                               price: parseFloat(newPrice)
                                                           };
                                                          
                                                           const response = await fetch(`/api/items/${id}`, {
                                                               method: 'PUT',
                                                               headers: { 'Content-Type': 'application/json' },
                                                               body: JSON.stringify(updatedItem)
                                                           });
                                                          
                                                           if (response.ok) {
                                                               showNotification('Товар обновлен');
                                                               loadItems();
                                                           }
                                                       }
                                                   } catch (error) {
                                                       console.error('Ошибка:', error);
                                                       showNotification('Ошибка при редактировании', false);
                                                   }
                                               }
                
                                               async function deleteItem(id) {
                                                   if (!confirm('Удалить товар?')) return;
                                                  
                                                   try {
                                                       const response = await fetch(`/api/items/${id}`, { method: 'DELETE' });
                                                      
                                                       if (response.ok) {
                                                           showNotification('Товар удален');
                                                           loadItems();
                                                       }
                                                   } catch (error) {
                                                       console.error('Ошибка:', error);
                                                       showNotification('Ошибка при удалении', false);
                                                   }
                                               }
                
                                               // ========== Заказы ==========
                                               function addToOrder() {
                                                   const select = document.getElementById('itemSelect');
                                                   const itemId = parseInt(select.value);
                                                   const quantity = parseInt(document.getElementById('orderQuantity').value);
                                                   const itemName = select.options[select.selectedIndex].text.split(' (')[0];
                                                  
                                                   if (isNaN(quantity) {
                                                       showNotification('Введите корректное количество', false);
                                                       return;
                                                   }
                
                                                   // Проверяем, не добавлен ли уже этот товар
                                                   const existingItem = currentOrderItems.find(item => item.itemId === itemId);
                                                   if (existingItem) {
                                                       existingItem.quantity += quantity;
                                                   } else {
                                                       currentOrderItems.push({ itemId, quantity, itemName });
                                                   }
                                                  
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
                                                       showNotification('Добавьте товары в заказ', false);
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
                                                           headers: {
                                                               'Content-Type': 'application/json',
                                                               'Accept': 'application/json'
                                                           },
                                                           body: JSON.stringify(orderRequest)
                                                       });
                
                                                       if (!response.ok) {
                                                           const error = await response.json();
                                                           throw new Error(error.error || 'Ошибка сервера');
                                                       }
                
                                                       showNotification('Заказ успешно создан');
                                                       currentOrderItems = [];
                                                       updateOrderTable();
                                                       loadItems();
                                                       loadOrders();
                                                      
                                                   } catch (error) {
                                                       console.error('Ошибка:', error);
                                                       showNotification(error.message, false);
                                                   }
                                               }
                
                                               async function loadOrders() {
                                                   try {
                                                       const response = await fetch('/api/orders');
                                                       if (!response.ok) throw new Error('Ошибка загрузки заказов');
                                                      
                                                       const orders = await response.json();
                                                       renderOrders(orders);
                                                   } catch (error) {
                                                       console.error('Ошибка:', error);
                                                       showNotification(error.message, false);
                                                   }
                                               }
                
                                               function renderOrders(orders) {
                                                   const tbody = document.querySelector('#ordersTable tbody');
                                                   tbody.innerHTML = orders.map(order => `
                                                       <tr>
                                                           <td>${order.id}</td>
                                                           <td>
                                                               <ul>${order.items.map(item =>\s
                                                                   `<li>${item.item.name} - ${item.quantity} шт.</li>`
                                                               ).join('')}</ul>
                                                           </td>
                                                           <td>${new Date(order.orderDate).toLocaleString()}</td>
                                                           <td><button onclick="deleteOrder(${order.id})">Удалить</button></td>
                                                       </tr>
                                                   `).join('');
                                               }
                
                                               async function deleteOrder(id) {
                                                   if (!confirm('Удалить заказ?')) return;
                                                  
                                                   try {
                                                       const response = await fetch(`/api/orders/${id}`, { method: 'DELETE' });
                                                      
                                                       if (response.ok) {
                                                           showNotification('Заказ удален');
                                                           loadOrders();
                                                           loadItems(); // Обновляем количество товаров
                                                       }
                                                   } catch (error) {
                                                       console.error('Ошибка:', error);
                                                       showNotification('Ошибка при удалении заказа', false);
                                                   }
                                               }
                                           </script>
                                       </body>
                                       </html>
        """;
    }
}