package com.srimathi.srimathimart.dao;

import com.srimathi.srimathimart.model.Review;
import java.sql.Connection;
import java.util.List;

/** Persistence for {@code reviews}. */
public interface ReviewDao {

    /**
     * Inserts a review and returns it with the generated id populated.
     *
     * @param connection an open connection
     * @param review     the review to insert
     * @return the inserted review
     */
    Review insert(Connection connection, Review review);

    /**
     * Lists reviews for one product, newest first.
     *
     * @param connection an open connection
     * @param productId  the product id
     * @return the product's reviews
     */
    List<Review> findByProduct(Connection connection, long productId);

    /**
     * Average rating for a product.
     *
     * @param connection an open connection
     * @param productId  the product id
     * @return the average, or 0 when there are no reviews
     */
    double averageRating(Connection connection, long productId);
}
