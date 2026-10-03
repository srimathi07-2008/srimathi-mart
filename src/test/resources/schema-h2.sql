-- H2 mirror of db/schema.sql, used only by the DAO and service tests.
-- Kept deliberately close to the MySQL DDL: same column names, same types
-- where H2 supports them, same UNIQUE constraints, so the production SQL in
-- the DAOs runs unchanged against it.

DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(120) NOT NULL,
    email           VARCHAR(190) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(10)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE products (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    seller_id       BIGINT        NOT NULL,
    name            VARCHAR(160)  NOT NULL,
    description     VARCHAR(1000) NOT NULL DEFAULT '',
    price           DECIMAL(10,2) NOT NULL,
    stock_quantity  INT           NOT NULL DEFAULT 0,
    category        VARCHAR(60)   NOT NULL,
    image_url       VARCHAR(500)  NOT NULL DEFAULT '',
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
    id          BIGINT    NOT NULL AUTO_INCREMENT,
    buyer_id    BIGINT    NOT NULL,
    product_id  BIGINT    NOT NULL,
    quantity    INT       NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_cart_buyer_product UNIQUE (buyer_id, product_id),
    CONSTRAINT fk_cart_buyer FOREIGN KEY (buyer_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE
);

CREATE TABLE orders (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    buyer_id      BIGINT        NOT NULL,
    total_amount  DECIMAL(10,2) NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PLACED',
    payment_ref   VARCHAR(60)   NOT NULL DEFAULT '',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE order_items (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    product_name VARCHAR(160)  NOT NULL,
    unit_price   DECIMAL(10,2) NOT NULL,
    quantity     INT           NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (id)
);

CREATE TABLE reviews (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    product_id  BIGINT       NOT NULL,
    buyer_id    BIGINT       NOT NULL,
    rating      INT          NOT NULL,
    comment     VARCHAR(800) NOT NULL DEFAULT '',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_reviews_product_buyer UNIQUE (product_id, buyer_id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_buyer FOREIGN KEY (buyer_id)
        REFERENCES users (id) ON DELETE CASCADE
);
