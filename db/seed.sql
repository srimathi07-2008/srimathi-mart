-- ---------------------------------------------------------------------
-- HOME DECORATION PRODUCTS
-- ---------------------------------------------------------------------

CREATE UNIQUE INDEX uq_products_seed ON products (seller_id, name);

INSERT IGNORE INTO products
    (seller_id, name, description, price, stock_quantity, category, image_url, active)
SELECT
    u.id,
    t.name,
    t.description,
    t.price,
    t.stock_quantity,
    t.category,
    t.image_url,
    1
FROM users u
CROSS JOIN (

    SELECT
        'Decorative Cushions' AS name,
        'Elegant cushions to give your living space a beautiful look.' AS description,
        599.00 AS price,
        30 AS stock_quantity,
        'Home Decor' AS category,
        'images/decorative-cushions.jpg' AS image_url

    UNION ALL

    SELECT
        'Indoor Plants & Planters',
        'Beautiful decorative plants for a fresh and stylish home.',
        699.00,
        30,
        'Home Decor',
        'images/indoor-plants-planters.jpg'

    UNION ALL

    SELECT
        'Decorative Lamps & Lanterns',
        'Warm decorative lighting for beautiful interiors and events.',
        899.00,
        25,
        'Lighting',
        'images/lamps-lanterns.jpg'

    UNION ALL

    SELECT
        'Wall Art Frames',
        'Stylish wall art to add character to your home.',
        799.00,
        25,
        'Wall Decor',
        'images/wall-art-frames.jpg'

    UNION ALL

    SELECT
        'Tabletop Decor',
        'Elegant decorative pieces for tables, shelves and corners.',
        499.00,
        30,
        'Table Decor',
        'images/tabletop-decor.jpg'

    UNION ALL

    SELECT
        'Decorative Mirrors',
        'Beautiful mirrors that add style and elegance to your space.',
        999.00,
        20,
        'Home Decor',
        'images/decorative-mirrors.jpg'

) AS t

WHERE u.email = 'seller@srimathimart.com';

DROP INDEX uq_products_seed ON products;