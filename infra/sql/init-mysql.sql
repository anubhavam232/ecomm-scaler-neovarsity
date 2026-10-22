CREATE DATABASE IF NOT EXISTS ecommerce_user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_product_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON ecommerce_user_db.* TO 'ecommerce'@'%';
GRANT ALL PRIVILEGES ON ecommerce_product_db.* TO 'ecommerce'@'%';
GRANT ALL PRIVILEGES ON ecommerce_order_db.* TO 'ecommerce'@'%';
GRANT ALL PRIVILEGES ON ecommerce_payment_db.* TO 'ecommerce'@'%';
FLUSH PRIVILEGES;
