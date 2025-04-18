
INSERT INTO items (id, name, quantity, price) VALUES
(1, 'Тестовый ноутбук', 10, 999.99),
(2, 'Тестовый телефон', 5, 499.50);

INSERT INTO orders (id, order_date) VALUES 
(1, CURRENT_TIMESTAMP),
(2, CURRENT_TIMESTAMP - INTERVAL '1 day');

INSERT INTO order_items (id, order_id, item_id, quantity) VALUES 
(1, 1, 1, 2),
(2, 1, 2, 1),
(3, 2, 1, 3);