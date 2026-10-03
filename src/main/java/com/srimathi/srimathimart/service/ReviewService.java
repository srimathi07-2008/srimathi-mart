package com.srimathi.srimathimart.service;

import com.srimathi.srimathimart.dao.JdbcReviewDao;
import com.srimathi.srimathimart.dao.ReviewDao;
import com.srimathi.srimathimart.exception.ValidationException;
import com.srimathi.srimathimart.model.Review;
import java.util.List;

public class ReviewService {

    private final ReviewDao reviewDao;

    public ReviewService() {
        this(new JdbcReviewDao());
    }

    public ReviewService(final ReviewDao reviewDaoValue) {
        this.reviewDao = reviewDaoValue;
    }

    public Review addReview(final long buyerId,
                            final long productId,
                            final int rating,
                            final String comment) {

        if (productId <= 0) {
            throw new ValidationException("Invalid product.");
        }

        if (rating < 1 || rating > 5) {
            throw new ValidationException("Rating must be between 1 and 5.");
        }

        String cleanComment = comment == null ? "" : comment.trim();

        if (cleanComment.length() > 1000) {
            throw new ValidationException(
                    "Review must be 1000 characters or fewer.");
        }

        Review review = new Review();
        review.setProductId(productId);
        review.setBuyerId(buyerId);
        review.setRating(rating);
        review.setComment(cleanComment);

        TransactionTemplate.Work<Review> work =
        connection -> reviewDao.insert(connection, review);

return TransactionTemplate.inTransaction(work);
    }

    public List<Review> listForProduct(final long productId) {
        return TransactionTemplate.read(
                connection -> reviewDao.findByProduct(connection, productId));
    }

    public double averageRating(final long productId) {
        return TransactionTemplate.read(
                connection -> reviewDao.averageRating(connection, productId));
    }
}