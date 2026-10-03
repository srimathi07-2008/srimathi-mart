-- =====================================================================
-- Srimathi Mart - MySQL schema
-- Run with:  mysql -u root -p < db/schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS srimathi_mart
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE srimathi_mart;

-- Drop in reverse dependency order so the script is re-runnable.
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;


-- ---------------------------------------------------------------------
-- users
-- role: BUYER | SELLER | ADMIN  (ADMIN is seed-only, never self-signup)
-- password_hash holds a bcrypt hash - plaintext is never stored.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(120) NOT NULL,
    email           VARCHAR(190) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(10)  NOT NULL,
    active          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role  CHECK (role IN ('BUYER', 'SELLER', 'ADMIN'))
) ENGINE = InnoDB;

CREATE INDEX idx_users_role ON users (role);


-- ---------------------------------------------------------------------
-- products
-- ---------------------------------------------------------------------
CREATE TABLE products (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    seller_id       BIGINT        NOT NULL,
    name            VARCHAR(160)  NOT NULL,
    description     VARCHAR(1000) NOT NULL DEFAULT '',
    price           DECIMAL(10,2) NOT NULL,
    stock_quantity  INT           NOT NULL DEFAULT 0,
    category        VARCHAR(60)   NOT NULL,
    image_url       VARCHAR(500)  NOT NULL DEFAULT '',
    active          TINYINT(1)    NOT NULL DEFAULT 1,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_products_seller
        FOREIGN KEY (seller_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_stock CHECK (stock_quantity >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_products_seller   ON products (seller_id);
CREATE INDEX idx_products_category ON products (category);
CREATE INDEX idx_products_name     ON products (name);


-- ---------------------------------------------------------------------
-- cart_items  (server-side cart, one row per buyer+product)
-- ---------------------------------------------------------------------
CREATE TABLE cart_items (
    id          BIGINT    NOT NULL AUTO_INCREMENT,
    buyer_id    BIGINT    NOT NULL,
    product_id  BIGINT    NOT NULL,
    quantity    INT       NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uq_cart_buyer_product UNIQUE (buyer_id, product_id),
    CONSTRAINT fk_cart_buyer
        FOREIGN KEY (buyer_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_cart_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_cart_quantity CHECK (quantity > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_cart_buyer   ON cart_items (buyer_id);
CREATE INDEX idx_cart_product ON cart_items (product_id);


-- ---------------------------------------------------------------------
-- orders
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    buyer_id      BIGINT        NOT NULL,
    total_amount  DECIMAL(10,2) NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PLACED',
    payment_ref   VARCHAR(60)   NOT NULL DEFAULT '',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_orders_buyer
        FOREIGN KEY (buyer_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_orders_total CHECK (total_amount >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_orders_buyer  ON orders (buyer_id);
CREATE INDEX idx_orders_status ON orders (status);


-- ---------------------------------------------------------------------
-- order_items
-- unit_price is copied at purchase time so history survives price edits.
-- ---------------------------------------------------------------------
CREATE TABLE order_items (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    product_name VARCHAR(160)  NOT NULL,
    unit_price   DECIMAL(10,2) NOT NULL,
    quantity     INT           NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_order_items_qty CHECK (quantity > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_order_items_order   ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);


-- ---------------------------------------------------------------------
-- reviews
-- ---------------------------------------------------------------------
CREATE TABLE reviews (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    product_id  BIGINT       NOT NULL,
    buyer_id    BIGINT       NOT NULL,
    rating      INT          NOT NULL,
    comment     VARCHAR(800) NOT NULL DEFAULT '',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uq_reviews_product_buyer UNIQUE (product_id, buyer_id),
    CONSTRAINT fk_reviews_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_reviews_buyer
        FOREIGN KEY (buyer_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB;

CREATE INDEX idx_reviews_product ON reviews (product_id);
CREATE INDEX idx_reviews_buyer   ON reviews (buyer_id);
