package com.srimathi.srimathimart.controller;

import com.srimathi.srimathimart.dto.ProductForm;
import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.exception.ValidationException;
import com.srimathi.srimathimart.model.Product;
import com.srimathi.srimathimart.service.ProductService;
import com.srimathi.srimathimart.util.HttpUtil;
import com.srimathi.srimathimart.util.Json;
import java.io.IOException;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Seller product management: create, edit, delete, and list own products.
 *
 * <p>Update and delete arrive as POSTs carrying an {@code action} field rather
 * than as PUT and DELETE, because the existing pages submit with plain forms
 * and fetch() calls that browsers handle most simply as POST. The service
 * still verifies ownership on every one of them.</p>
 */
@WebServlet(name = "sellerProductServlet", urlPatterns = "/api/seller/products")
public class SellerProductServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient ProductService productService = new ProductService();

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        handle(request, response, () -> {
            SessionUser seller = requireUser(request);
            List<Product> products = productService.listForSeller(seller.getId());

            Json json = new Json();
            json.beginObject()
                .put("ok", true)
                .put("count", products.size())
                .name("products").beginArray();

            for (Product product : products) {
                ProductServlet.writeProduct(json, product);
            }

            json.endArray().endObject();
            HttpUtil.writeJson(response, HttpServletResponse.SC_OK, json);
        });
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        handle(request, response, () -> {
            SessionUser seller = requireUser(request);
            String action = HttpUtil.param(request, "action").toLowerCase();

            switch (action) {
                case "":
                case "create":
                    create(request, response, seller);
                    break;
                case "update":
                    update(request, response, seller);
                    break;
                case "delete":
                    delete(request, response, seller);
                    break;
                default:
                    throw new ValidationException("Unknown action: " + action);
            }
        });
    }

    private void create(final HttpServletRequest request,
                        final HttpServletResponse response,
                        final SessionUser seller) throws IOException {

        Product saved = productService.create(seller.getId(), readForm(request, null));
        writeProductResponse(response, HttpServletResponse.SC_CREATED,
                "Product added successfully.", saved);
    }

    private void update(final HttpServletRequest request,
                        final HttpServletResponse response,
                        final SessionUser seller) throws IOException {

        long productId = HttpUtil.longParam(request, "id", -1L);
        if (productId <= 0) {
            throw new ValidationException("A product id is required.");
        }

        Product saved = productService.update(
                seller.getId(), productId, readForm(request, productId));

        writeProductResponse(response, HttpServletResponse.SC_OK,
                "Product updated.", saved);
    }

    private void delete(final HttpServletRequest request,
                        final HttpServletResponse response,
                        final SessionUser seller) throws IOException {

        long productId = HttpUtil.longParam(request, "id", -1L);
        if (productId <= 0) {
            throw new ValidationException("A product id is required.");
        }

        productService.delete(seller.getId(), productId);
        writeOk(response, "Product deleted.");
    }

    private static ProductForm readForm(final HttpServletRequest request, final Long id) {
        return new ProductForm(
                id,
                HttpUtil.param(request, "name"),
                HttpUtil.param(request, "description"),
                HttpUtil.param(request, "price"),
                HttpUtil.param(request, "stockQuantity"),
                HttpUtil.param(request, "category"),
                HttpUtil.param(request, "imageUrl"));
    }

    private void writeProductResponse(final HttpServletResponse response,
                                      final int status,
                                      final String message,
                                      final Product product) throws IOException {
        Json json = new Json();
        json.beginObject()
            .put("ok", true)
            .put("message", message)
            .name("product");
        ProductServlet.writeProduct(json, product);
        json.endObject();

        HttpUtil.writeJson(response, status, json);
    }
}
