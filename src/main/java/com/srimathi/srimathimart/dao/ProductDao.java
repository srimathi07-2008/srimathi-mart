package com.srimathi.srimathimart.dao;

import com.srimathi.srimathimart.dto.ProductSearch;
import com.srimathi.srimathimart.model.Product;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Persistence for {@code products}. */
public interface ProductDao {

    /**
     * Inserts a product and returns it with the generated id populated.
     *
     * @param connection an open connection
     * @param product    the product to insert
     * @return the inserted product
     */
    Product insert(Connection connection, Product product);

    /**
     * Updates the editable fields of a product.
     *
     * @param connection an open connection
     * @param product    the product carrying the new values
     * @return true when a row was updated
     */
    boolean update(Connection connection, Product product);

    /**
     * Deletes a product.
     *
     * @param connection an open connection
     * @param productId  the product id
     * @return true when a row was deleted
     */
    boolean delete(Connection connection, long productId);

    /**
     * Finds a product by id.
     *
     * @param connection an open connection
     * @param productId  the product id
     * @return the product, if present
     */
    Optional<Product> findById(Connection connection, long productId);

    /**
     * Lists every product belonging to one seller, newest first.
     *
     * @param connection an open connection
     * @param sellerId   the seller id
     * @return the seller's products
     */
    List<Product> findBySeller(Connection connection, long sellerId);

    /**
     * Searches the active catalogue. Both filter fields are optional and are
     * always bound as parameters, never concatenated into the SQL text.
     *
     * @param connection an open connection
     * @param search     the keyword and category filter
     * @return matching products
     */
    List<Product> search(Connection connection, ProductSearch search);

    /**
     * Lists the distinct categories currently in use.
     *
     * @param connection an open connection
     * @return sorted category names
     */
    List<String> findCategories(Connection connection);
/**
 * Counts all products.
 *
 * @param connection an open connection
 * @return total number of products
 */
long countAll(Connection connection);
    /**
     * Reduces stock, but only when enough is available. The WHERE clause
     * carries the stock check so concurrent checkouts cannot oversell.
     *
     * @param connection an open connection
     * @param productId  the product id
     * @param quantity   the amount to deduct
     * @return true when stock was deducted
     */
    boolean decrementStock(Connection connection, long productId, int quantity);
}
