package com.srimathi.srimathimart.controller;

import com.srimathi.srimathimart.dto.ProductSearch;
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
 * The public catalogue: browse, search by keyword, filter by category.
 *
 * <p>Open to signed-out visitors, matching the original site where anyone
 * could look at the product grid.</p>
 */
@WebServlet(name = "productServlet", urlPatterns = {"/api/products", "/api/categories"})
public class ProductServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient ProductService productService = new ProductService();

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        handle(request, response, () -> {
            String path = request.getServletPath();

            if ("/api/categories".equals(path)) {
                writeCategories(response);
                return;
            }

            ProductSearch search = new ProductSearch(
                    HttpUtil.param(request, "q"),
                    HttpUtil.param(request, "category"));

            List<Product> products = productService.browse(search);

            Json json = new Json();
            json.beginObject()
                .put("ok", true)
                .put("count", products.size())
                .name("products").beginArray();

            for (Product product : products) {
                writeProduct(json, product);
            }

            json.endArray().endObject();
            HttpUtil.writeJson(response, HttpServletResponse.SC_OK, json);
        });
    }

    private void writeCategories(final HttpServletResponse response) throws IOException {
        List<String> categories = productService.categories();

        Json json = new Json();
        json.beginObject().put("ok", true).name("categories").beginArray();
        for (String category : categories) {
            json.value(category);
        }
        json.endArray().endObject();

        HttpUtil.writeJson(response, HttpServletResponse.SC_OK, json);
    }

    /**
     * Serialises one product. Shared with the seller endpoint so both sides of
     * the app agree on field names.
     *
     * @param json    the writer
     * @param product the product
     */
    static void writeProduct(final Json json, final Product product) {
        json.beginObject()
            .put("id", product.getId())
            .put("name", product.getName())
            .put("description", product.getDescription())
            .put("price", product.getPrice())
            .put("stockQuantity", product.getStockQuantity())
            .put("category", product.getCategory())
            .put("imageUrl", product.getImageUrl())
            .put("active", product.isActive())
            .endObject();
    }
}
