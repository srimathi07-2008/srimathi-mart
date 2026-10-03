package com.srimathi.srimathimart.controller;
import javax.servlet.annotation.WebServlet;
import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.model.Review;
import com.srimathi.srimathimart.service.ReviewService;
import com.srimathi.srimathimart.util.HttpUtil;
import com.srimathi.srimathimart.util.Json;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
@WebServlet(name="reviewServlet",urlPatterns="/api/reviews")
public class ReviewServlet extends HttpServlet {

    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response)
            throws IOException {

        String productIdText = request.getParameter("productId");

        try {
            long productId = Long.parseLong(productIdText);

            List<Review> reviews =
                    reviewService.listForProduct(productId);

            double average =
                    reviewService.averageRating(productId);

            Json json = new Json();
            json.beginObject()
                    .put("ok", true)
                    .put("averageRating",BigDecimal.valueOf(average))
                    .put("count", reviews.size())
                    .name("reviews")
                    .beginArray();

            for (Review review : reviews) {
                json.beginObject()
                        .put("id", review.getId())
                        .put("productId", review.getProductId())
                        .put("rating", review.getRating())
                        .put("comment", review.getComment())
                        .put("createdAt",
                                review.getCreatedAt() == null
                                        ? ""
                                        : review.getCreatedAt().toString())
                        .endObject();
            }

            json.endArray().endObject();

            HttpUtil.writeJson(
                    response,
                    HttpServletResponse.SC_OK,
                    json);

        } catch (NumberFormatException ex) {
            HttpUtil.writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid product ID.");
        }
    }

    @Override
    protected void doPost(final HttpServletRequest request,
                          final HttpServletResponse response)
            throws IOException {

        try {
            SessionUser buyer = HttpUtil.currentUser(request);

            if (buyer == null) {
                HttpUtil.writeError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Please sign in to submit a review.");
                return;
            }

            long productId =
                    Long.parseLong(request.getParameter("productId"));

            int rating =
                    Integer.parseInt(request.getParameter("rating"));

            String comment =
                    request.getParameter("comment");

            Review review = reviewService.addReview(
                    buyer.getId(),
                    productId,
                    rating,
                    comment);

            Json json = new Json();

            json.beginObject()
                    .put("ok", true)
                    .name("review")
                    .beginObject()
                    .put("id", review.getId())
                    .put("productId", review.getProductId())
                    .put("rating", review.getRating())
                    .put("comment", review.getComment())
                    .endObject()
                    .endObject();

            HttpUtil.writeJson(
                    response,
                    HttpServletResponse.SC_CREATED,
                    json);

        } catch (NumberFormatException ex) {

            HttpUtil.writeError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid product ID or rating.");
        }
    }
}